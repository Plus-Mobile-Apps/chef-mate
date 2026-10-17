-- Family recipe books — a recipe book can belong to a family instead of only its creator.
--
-- A book with family_id = NULL is personal, exactly as before: its owner manages it and can invite
-- collaborators through recipe_book_members. A book with a family_id belongs to that family:
--   * every accepted family member can view it and every recipe filed in it, and can file recipes
--     they can already read into it (or take them out);
--   * a recipe filed in a family book stays its author's: other members can read and cook it, but
--     only the author edits it (can_edit_recipe ignores family books);
--   * only family owners and admins create, rename and delete family books, and only through the
--     SECURITY DEFINER RPCs below (direct INSERT/UPDATE/DELETE on family rows is refused by RLS);
--   * family books don't take per-book collaborators — the family is the membership — and are
--     never anyone's default "My Recipes" book.
--
-- owner_id on a family book records who created it but grants nothing: leaving the family revokes
-- access even for the creator. A family can have any number of books.
--
-- If a family is deleted, its books fall back to their creators' personal books (ON DELETE SET
-- NULL), matching family meal plans and grocery lists. Clients prune books they can no longer see
-- on the next sync.
--
-- Filing a recipe into a family book grants every member read access to it, so inserting a
-- recipe_book_recipes row requires that the caller can already read the recipe (can_plan_recipe:
-- owns it or reaches it through a book or a family meal) — otherwise a member could file someone
-- else's private recipe UUID into the family and read it. The same check now applies to personal
-- books' editors, who previously could file any recipe UUID.
--
-- NOTE: the recipe_books / recipe_book_recipes policies in this folder are owner-based ("Users can
-- view own recipe books", ...), and prod may carry more (see the drift notes). An owner_id-based
-- policy would let a family book's creator keep access after leaving the family, so this migration
-- drops EVERY policy on both tables and recreates the full set below. Check what's there first with
-- supabase/preflight/family_recipe_books_check.sql.
--
-- Requires 20260916_add_families.sql (is_family_member, is_family_admin, family_forbidden),
-- 20260608_add_recipe_book_collaboration.sql / 20260610_* (recipe_book_members and the
-- can_*_recipe_book helpers) and 20261013_family_meal_plans.sql (can_access_recipe_via_book,
-- can_plan_recipe).
--
-- Safe to re-run (idempotent).

ALTER TABLE public.recipe_books
    ADD COLUMN IF NOT EXISTS family_id UUID REFERENCES public.families(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_recipe_books_family ON public.recipe_books(family_id)
    WHERE family_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Access helpers — a family book is reachable through family membership only.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION can_access_recipe_book(p_book_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT EXISTS (
        SELECT 1 FROM recipe_books b
        WHERE b.id = p_book_id
          AND CASE WHEN b.family_id IS NOT NULL THEN is_family_member(b.family_id)
                   ELSE b.owner_id = auth.uid() END
    ) OR EXISTS (
        SELECT 1 FROM recipe_book_members m
        JOIN recipe_books b ON b.id = m.recipe_book_id
        WHERE m.recipe_book_id = p_book_id
          AND b.family_id IS NULL
          AND m.user_id = auth.uid()
          AND m.status = 'accepted'
    );
$$;

-- "Edit" a book = file recipes into it and take them out. Every member of a family book may.
CREATE OR REPLACE FUNCTION can_edit_recipe_book(p_book_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT EXISTS (
        SELECT 1 FROM recipe_books b
        WHERE b.id = p_book_id
          AND CASE WHEN b.family_id IS NOT NULL THEN is_family_member(b.family_id)
                   ELSE b.owner_id = auth.uid() END
    ) OR EXISTS (
        SELECT 1 FROM recipe_book_members m
        JOIN recipe_books b ON b.id = m.recipe_book_id
        WHERE m.recipe_book_id = p_book_id
          AND b.family_id IS NULL
          AND m.user_id = auth.uid()
          AND m.status = 'accepted'
          AND m.role IN ('owner', 'editor')
    );
$$;

-- Owner-only collaborator management (inviting, removing, changing roles) applies to personal
-- books only.
CREATE OR REPLACE FUNCTION is_recipe_book_owner(p_book_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT EXISTS (
        SELECT 1 FROM recipe_books b
        WHERE b.id = p_book_id AND b.owner_id = auth.uid() AND b.family_id IS NULL
    );
$$;

-- Editing a recipe's content through a book is an editor-collaborator grant on personal books.
-- Family books share recipes read-only: only the author (via the recipes owner policies) edits.
CREATE OR REPLACE FUNCTION can_edit_recipe(p_recipe_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT EXISTS (
        SELECT 1 FROM recipe_book_recipes rbr
        JOIN recipe_books b ON b.id = rbr.recipe_book_id
        WHERE rbr.recipe_id = p_recipe_id
          AND b.family_id IS NULL
          AND can_edit_recipe_book(rbr.recipe_book_id)
    );
$$;

-- can_access_recipe / can_access_recipe_via_book / get_accessible_recipes go through
-- can_access_recipe_book, so recipes in a family book become readable to its members without
-- changing them.

-- ---------------------------------------------------------------------------
-- recipe_books / recipe_book_recipes RLS — the full set, replacing whatever is there.
-- ---------------------------------------------------------------------------
ALTER TABLE public.recipe_books ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.recipe_book_recipes ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE
    pol record;
BEGIN
    FOR pol IN
        SELECT tablename, policyname FROM pg_policies
        WHERE schemaname = 'public' AND tablename IN ('recipe_books', 'recipe_book_recipes')
    LOOP
        EXECUTE format('DROP POLICY %I ON public.%I', pol.policyname, pol.tablename);
    END LOOP;
END $$;

-- The row's own columns are checked inline (not only through can_access_recipe_book) so the
-- creator can read back a book in the same INSERT ... RETURNING that creates it.
CREATE POLICY "recipe_books_select" ON recipe_books FOR SELECT
    USING (
        CASE WHEN family_id IS NOT NULL THEN is_family_member(family_id)
             ELSE owner_id = auth.uid() END
        OR can_access_recipe_book(id)
    );

-- A pending invitee reads the invited book's name for the invite banner (from 20260610).
CREATE POLICY "recipe_books_invitees_select" ON recipe_books FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM recipe_book_members m
            WHERE m.recipe_book_id = recipe_books.id
              AND lower(m.invited_email) = lower(current_user_email())
        )
    );

-- Direct writes are for personal books; family books go through the RPCs below.
CREATE POLICY "recipe_books_insert" ON recipe_books FOR INSERT
    WITH CHECK (owner_id = auth.uid() AND family_id IS NULL);

-- Owner or editor collaborator renames a personal book. WITH CHECK stops anyone moving a personal
-- book into a family (or anywhere else).
CREATE POLICY "recipe_books_update" ON recipe_books FOR UPDATE
    USING (family_id IS NULL AND can_edit_recipe_book(id))
    WITH CHECK (family_id IS NULL);

CREATE POLICY "recipe_books_delete" ON recipe_books FOR DELETE
    USING (owner_id = auth.uid() AND family_id IS NULL);

-- Links: a recipe's author sees where it's filed; anyone who can access a book sees what's in it.
CREATE POLICY "recipe_book_recipes_select" ON recipe_book_recipes FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM recipes r
            WHERE r.id = recipe_book_recipes.recipe_id AND r.owner_id = auth.uid()
        )
        OR can_access_recipe_book(recipe_book_id)
    );

-- Filing needs edit rights on the book AND read access to the recipe (see the header). The
-- subquery inside can_plan_recipe runs on the statement's snapshot, so the row being inserted can't
-- vouch for itself.
CREATE POLICY "recipe_book_recipes_insert" ON recipe_book_recipes FOR INSERT
    WITH CHECK (can_edit_recipe_book(recipe_book_id) AND can_plan_recipe(recipe_id));

-- Unfiling: anyone who can edit the book, or the recipe's author from a personal book. An author
-- who has left a family can't pull their recipe out of its book (and the client's replace-all
-- link push after leaving silently skips it rather than doing so) — they can still delete it.
CREATE POLICY "recipe_book_recipes_delete" ON recipe_book_recipes FOR DELETE
    USING (
        can_edit_recipe_book(recipe_book_id)
        OR EXISTS (
            SELECT 1 FROM recipes r
            JOIN recipe_books b ON b.id = recipe_book_recipes.recipe_book_id
            WHERE r.id = recipe_book_recipes.recipe_id
              AND r.owner_id = auth.uid()
              AND b.family_id IS NULL
        )
    );

-- ---------------------------------------------------------------------------
-- Family book RPCs — owners and admins manage; any member reads.
-- ---------------------------------------------------------------------------

-- The family's books, oldest first. Empty for callers who aren't members.
DROP FUNCTION IF EXISTS family_recipe_books(uuid);
CREATE OR REPLACE FUNCTION family_recipe_books(p_family_id uuid)
RETURNS TABLE(id uuid, name text, recipe_count integer)
LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT b.id, b.name::text,
           (SELECT count(*)::integer FROM recipe_book_recipes rbr WHERE rbr.recipe_book_id = b.id)
    FROM recipe_books b
    WHERE b.family_id = p_family_id AND is_family_member(p_family_id)
    ORDER BY b.created_at, b.id;
$$;

CREATE OR REPLACE FUNCTION create_family_recipe_book(p_family_id uuid, p_name text)
RETURNS uuid LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_id uuid;
BEGIN
    IF NOT is_family_admin(p_family_id) THEN
        PERFORM family_forbidden('only owners and admins can create family recipe books');
    END IF;
    IF p_name IS NULL OR length(trim(p_name)) = 0 THEN
        RAISE EXCEPTION 'family: name is required' USING ERRCODE = '22023';
    END IF;
    INSERT INTO recipe_books (name, owner_id, family_id, is_default)
    VALUES (trim(p_name), auth.uid(), p_family_id, false)
    RETURNING id INTO v_id;
    RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION rename_family_recipe_book(p_book_id uuid, p_name text)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_family_id uuid := (SELECT family_id FROM recipe_books WHERE id = p_book_id);
BEGIN
    IF v_family_id IS NULL OR NOT is_family_admin(v_family_id) THEN
        PERFORM family_forbidden('only owners and admins can rename family recipe books');
    END IF;
    IF p_name IS NULL OR length(trim(p_name)) = 0 THEN
        RAISE EXCEPTION 'family: name is required' USING ERRCODE = '22023';
    END IF;
    UPDATE recipe_books SET name = trim(p_name), updated_at = now() WHERE id = p_book_id;
END;
$$;

-- Links cascade with the book; the recipes themselves stay with their authors.
CREATE OR REPLACE FUNCTION delete_family_recipe_book(p_book_id uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_family_id uuid := (SELECT family_id FROM recipe_books WHERE id = p_book_id);
BEGIN
    IF v_family_id IS NULL OR NOT is_family_admin(v_family_id) THEN
        PERFORM family_forbidden('only owners and admins can delete family recipe books');
    END IF;
    DELETE FROM recipe_books WHERE id = p_book_id;
END;
$$;

REVOKE EXECUTE ON FUNCTION
    family_recipe_books(uuid), create_family_recipe_book(uuid, text),
    rename_family_recipe_book(uuid, text), delete_family_recipe_book(uuid)
    FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION
    family_recipe_books(uuid), create_family_recipe_book(uuid, text),
    rename_family_recipe_book(uuid, text), delete_family_recipe_book(uuid)
    TO authenticated;

-- Off for everyone until the rollout (or user_ids) is set, like manage_family.
INSERT INTO feature_flags (key, value_type, value, enabled, rollout_percent, description)
VALUES ('family_recipe_books', 'bool', 'true', true, 0,
        'Let families share recipe books, managed from Manage Family.')
ON CONFLICT (key) DO NOTHING;

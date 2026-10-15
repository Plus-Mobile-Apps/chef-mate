-- Family grocery lists — a grocery list can belong to a family instead of only its creator.
--
-- A list with family_id = NULL is personal, exactly as before: its owner manages it and can invite
-- collaborators through grocery_list_members. A list with a family_id belongs to that family:
--   * every accepted family member can view it and add, check off, edit and delete its items;
--   * only family owners and admins create, rename and delete family lists, and only through the
--     SECURITY DEFINER RPCs below (direct INSERT/UPDATE/DELETE on family rows is refused by RLS);
--   * family lists don't take per-list collaborators — the family is the membership.
--
-- owner_id on a family list records who created it but grants nothing: leaving the family revokes
-- access even for the creator. A family can have any number of lists.
--
-- If a family is deleted, its lists fall back to their creators' personal lists (ON DELETE SET
-- NULL), matching family meal plans. Clients prune lists they can no longer see on the next sync.
--
-- NOTE: grocery_lists and grocery_items were created by hand in prod (see the drift notes) and
-- may carry policies beyond the ones in this folder — e.g. the client's INSERT ... RETURNING on a
-- new personal list only works because something besides grocery_lists_select (whose helper can't
-- see the row being inserted) lets the owner read it. Any extra owner_id-based policy would let a
-- family list's creator keep access after leaving the family, so this migration drops EVERY policy
-- on both tables and recreates the full set below. Check what's there first with
-- supabase/preflight/family_grocery_lists_check.sql.
--
-- Requires 20260916_add_families.sql (is_family_member, is_family_admin, family_forbidden) and
-- 20260425_add_collaboration.sql (grocery_list_members and the can_*_grocery_list helpers).
--
-- Safe to re-run (idempotent).

ALTER TABLE public.grocery_lists
    ADD COLUMN IF NOT EXISTS family_id UUID REFERENCES public.families(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_grocery_lists_family ON public.grocery_lists(family_id)
    WHERE family_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Access helpers — a family list is reachable through family membership only.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION can_access_grocery_list(p_list_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
  SELECT EXISTS (
    SELECT 1 FROM grocery_lists l
    WHERE l.id = p_list_id
      AND CASE WHEN l.family_id IS NOT NULL THEN is_family_member(l.family_id)
               ELSE l.owner_id = auth.uid() END
  ) OR EXISTS (
    SELECT 1 FROM grocery_list_members m
    JOIN grocery_lists l ON l.id = m.list_id
    WHERE m.list_id = p_list_id
      AND l.family_id IS NULL
      AND m.user_id = auth.uid()
      AND m.status = 'accepted'
  );
$$;

CREATE OR REPLACE FUNCTION can_edit_grocery_list(p_list_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
  SELECT EXISTS (
    SELECT 1 FROM grocery_lists l
    WHERE l.id = p_list_id
      AND CASE WHEN l.family_id IS NOT NULL THEN is_family_member(l.family_id)
               ELSE l.owner_id = auth.uid() END
  ) OR EXISTS (
    SELECT 1 FROM grocery_list_members m
    JOIN grocery_lists l ON l.id = m.list_id
    WHERE m.list_id = p_list_id
      AND l.family_id IS NULL
      AND m.user_id = auth.uid()
      AND m.status = 'accepted'
      AND m.role IN ('owner', 'editor')
  );
$$;

-- Owner-only collaborator management applies to personal lists only.
CREATE OR REPLACE FUNCTION is_grocery_list_owner(p_list_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
  SELECT EXISTS (
    SELECT 1 FROM grocery_lists l
    WHERE l.id = p_list_id AND l.owner_id = auth.uid() AND l.family_id IS NULL
  );
$$;

-- The creator of a family list isn't its sole owner, so don't give them an owner member row.
CREATE OR REPLACE FUNCTION auto_add_grocery_list_owner()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO grocery_list_members (list_id, user_id, role, invited_email, status)
  SELECT NEW.id, NEW.owner_id, 'owner',
    COALESCE((SELECT email FROM auth.users WHERE id = NEW.owner_id), ''),
    'accepted'
  WHERE NEW.owner_id IS NOT NULL AND NEW.family_id IS NULL;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public;

-- ---------------------------------------------------------------------------
-- grocery_lists / grocery_items RLS — the full set, replacing whatever is there.
-- ---------------------------------------------------------------------------
ALTER TABLE public.grocery_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.grocery_items ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE
    pol record;
BEGIN
    FOR pol IN
        SELECT tablename, policyname FROM pg_policies
        WHERE schemaname = 'public' AND tablename IN ('grocery_lists', 'grocery_items')
    LOOP
        EXECUTE format('DROP POLICY %I ON public.%I', pol.policyname, pol.tablename);
    END LOOP;
END $$;

-- The row's own columns are checked inline (not only through can_access_grocery_list) so the
-- creator can read back a list in the same INSERT ... RETURNING that creates it.
CREATE POLICY "grocery_lists_select" ON grocery_lists FOR SELECT
  USING (
    CASE WHEN family_id IS NOT NULL THEN is_family_member(family_id)
         ELSE owner_id = auth.uid() END
    OR can_access_grocery_list(id)
  );

-- Pending invitees read the invited list's name for the invite banner (from 20260612).
CREATE POLICY "grocery_lists_invitees_select" ON grocery_lists FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM grocery_list_members m
      WHERE m.list_id = grocery_lists.id
        AND m.status = 'pending'
        AND lower(m.invited_email) = lower(current_user_email())
    )
  );

-- Direct writes are for personal lists; family lists go through the RPCs below.
CREATE POLICY "grocery_lists_insert" ON grocery_lists FOR INSERT
  WITH CHECK (owner_id = auth.uid() AND family_id IS NULL);

-- WITH CHECK stops a collaborator from moving a personal list into a family (or anywhere else).
CREATE POLICY "grocery_lists_update" ON grocery_lists FOR UPDATE
  USING (family_id IS NULL AND can_edit_grocery_list(id))
  WITH CHECK (family_id IS NULL);

CREATE POLICY "grocery_lists_delete" ON grocery_lists FOR DELETE
  USING (owner_id = auth.uid() AND family_id IS NULL);

-- Items: anyone who can access the list reads them; anyone who can edit it writes them (every
-- member of a family list, per can_edit_grocery_list).
CREATE POLICY "grocery_items_select" ON grocery_items FOR SELECT
  USING (can_access_grocery_list(list_id));

CREATE POLICY "grocery_items_insert" ON grocery_items FOR INSERT
  WITH CHECK (can_edit_grocery_list(list_id));

CREATE POLICY "grocery_items_update" ON grocery_items FOR UPDATE
  USING (can_edit_grocery_list(list_id))
  WITH CHECK (can_edit_grocery_list(list_id));

CREATE POLICY "grocery_items_delete" ON grocery_items FOR DELETE
  USING (can_edit_grocery_list(list_id));

-- ---------------------------------------------------------------------------
-- Family list RPCs — owners and admins manage; any member reads.
-- ---------------------------------------------------------------------------

-- The family's lists, oldest first. Empty for callers who aren't members.
DROP FUNCTION IF EXISTS family_grocery_lists(uuid);
CREATE OR REPLACE FUNCTION family_grocery_lists(p_family_id uuid)
RETURNS TABLE(id uuid, name text, item_count integer)
LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT l.id, l.name::text,
           (SELECT count(*)::integer FROM grocery_items i WHERE i.list_id = l.id)
    FROM grocery_lists l
    WHERE l.family_id = p_family_id AND is_family_member(p_family_id)
    ORDER BY l.created_at, l.id;
$$;

CREATE OR REPLACE FUNCTION create_family_grocery_list(p_family_id uuid, p_name text)
RETURNS uuid LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_id uuid;
BEGIN
    IF NOT is_family_admin(p_family_id) THEN
        PERFORM family_forbidden('only owners and admins can create family grocery lists');
    END IF;
    IF p_name IS NULL OR length(trim(p_name)) = 0 THEN
        RAISE EXCEPTION 'family: name is required' USING ERRCODE = '22023';
    END IF;
    INSERT INTO grocery_lists (name, owner_id, family_id)
    VALUES (trim(p_name), auth.uid(), p_family_id)
    RETURNING id INTO v_id;
    RETURN v_id;
END;
$$;

CREATE OR REPLACE FUNCTION rename_family_grocery_list(p_list_id uuid, p_name text)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_family_id uuid := (SELECT family_id FROM grocery_lists WHERE id = p_list_id);
BEGIN
    IF v_family_id IS NULL OR NOT is_family_admin(v_family_id) THEN
        PERFORM family_forbidden('only owners and admins can rename family grocery lists');
    END IF;
    IF p_name IS NULL OR length(trim(p_name)) = 0 THEN
        RAISE EXCEPTION 'family: name is required' USING ERRCODE = '22023';
    END IF;
    UPDATE grocery_lists SET name = trim(p_name), updated_at = now() WHERE id = p_list_id;
END;
$$;

-- Items cascade with the list.
CREATE OR REPLACE FUNCTION delete_family_grocery_list(p_list_id uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_family_id uuid := (SELECT family_id FROM grocery_lists WHERE id = p_list_id);
BEGIN
    IF v_family_id IS NULL OR NOT is_family_admin(v_family_id) THEN
        PERFORM family_forbidden('only owners and admins can delete family grocery lists');
    END IF;
    DELETE FROM grocery_lists WHERE id = p_list_id;
END;
$$;

REVOKE EXECUTE ON FUNCTION
    family_grocery_lists(uuid), create_family_grocery_list(uuid, text),
    rename_family_grocery_list(uuid, text), delete_family_grocery_list(uuid)
    FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION
    family_grocery_lists(uuid), create_family_grocery_list(uuid, text),
    rename_family_grocery_list(uuid, text), delete_family_grocery_list(uuid)
    TO authenticated;

-- Off for everyone until the rollout (or user_ids) is set, like manage_family.
INSERT INTO feature_flags (key, value_type, value, enabled, rollout_percent, description)
VALUES ('family_grocery_lists', 'bool', 'true', true, 0,
        'Let families share grocery lists, managed from Manage Family.')
ON CONFLICT (key) DO NOTHING;

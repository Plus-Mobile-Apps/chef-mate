-- ============================================================
-- Families (Phase 1)
--
-- A family is a small group of accounts that share content across grocery lists, recipe books,
-- and the meal plan. This migration adds only the group + membership model and its invite flow;
-- the `family_id` columns that scope the three domains land in later phases.
--
-- Families sit ALONGSIDE the existing per-entity sharing (`grocery_list_members`,
-- `recipe_book_members`) rather than replacing it — nothing here touches those tables or their
-- policies, so one-off sharing outside the family keeps working.
--
-- Structure mirrors 20260425_add_collaboration.sql (helpers, triggers, policies, realtime).
-- Run this migration in the Supabase SQL Editor. Safe to re-run.
-- ============================================================

-- ============================================================
-- 1. Tables
-- ============================================================

CREATE TABLE IF NOT EXISTS families (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  client_id TEXT UNIQUE,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_families_owner_id ON families(owner_id);

-- Two roles only, unlike grocery/recipe books' three. A family implies trust: every member can
-- edit all family-scoped content; only the owner invites, removes, renames, and deletes.
CREATE TABLE IF NOT EXISTS family_members (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
  user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
  invited_email TEXT NOT NULL,
  invited_by UUID REFERENCES auth.users(id),
  role TEXT NOT NULL DEFAULT 'member' CHECK (role IN ('owner', 'member')),
  status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected')),
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_fm_family_id ON family_members(family_id);
CREATE INDEX IF NOT EXISTS idx_fm_user_id ON family_members(user_id);
CREATE INDEX IF NOT EXISTS idx_fm_invited_email ON family_members(invited_email);

-- One invite per email per family, case-insensitively. A functional index rather than a UNIQUE
-- constraint because the RLS checks compare lower(...) and a plain UNIQUE(family_id,
-- invited_email) would let "A@x.com" and "a@x.com" both in.
CREATE UNIQUE INDEX IF NOT EXISTS idx_fm_family_email_unique
  ON family_members (family_id, lower(invited_email));

-- THE "exactly one family" RULE. A user may hold any number of *pending* invites, but at most one
-- accepted membership. Accepting a second invite fails here at the DB rather than silently
-- corrupting current_family_id(); the client surfaces "leave your current family first".
CREATE UNIQUE INDEX IF NOT EXISTS idx_fm_one_accepted_family_per_user
  ON family_members (user_id)
  WHERE status = 'accepted' AND user_id IS NOT NULL;

-- ============================================================
-- 2. Triggers
-- ============================================================

-- Auto-insert the owner's accepted member row on family creation, so the owner shows up in
-- membership queries without the client having to write a second row. Mirrors
-- auto_add_grocery_list_owner.
CREATE OR REPLACE FUNCTION auto_add_family_owner()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  INSERT INTO family_members (family_id, user_id, role, invited_email, invited_by, status)
  SELECT NEW.id, NEW.owner_id, 'owner',
    COALESCE((SELECT email FROM auth.users WHERE id = NEW.owner_id), ''),
    NEW.owner_id,
    'accepted'
  WHERE NEW.owner_id IS NOT NULL;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_auto_add_family_owner ON families;
CREATE TRIGGER trg_auto_add_family_owner
  AFTER INSERT ON families
  FOR EACH ROW EXECUTE FUNCTION auto_add_family_owner();

-- Link email-keyed invites to the account once the invitee signs up. Grocery has the equivalent
-- trigger; recipe books deliberately match by email at read time instead. We take grocery's
-- approach because current_family_id() resolves by user_id and needs the link to exist.
CREATE OR REPLACE FUNCTION migrate_pending_family_invitations()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
  UPDATE family_members
  SET user_id = NEW.id
  WHERE lower(invited_email) = lower(NEW.email) AND user_id IS NULL;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_migrate_pending_family_invitations ON auth.users;
CREATE TRIGGER trg_migrate_pending_family_invitations
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION migrate_pending_family_invitations();

-- ============================================================
-- 3. RLS helper functions
--
-- SECURITY DEFINER so they bypass RLS and can't recurse — the families policies check membership
-- and the family_members policies check family ownership, so without these they'd reference each
-- other's RLS-protected tables and loop forever (Postgres 42P17). Same failure that
-- 20260610_fix_recipe_rls_recursion.sql was written to fix.
-- ============================================================

-- The caller's family, or NULL when they aren't in one. This is the workhorse: every future
-- family_id policy on grocery_lists / recipe_books / meal_plans compares against it.
-- LIMIT 1 is belt-and-braces — idx_fm_one_accepted_family_per_user already guarantees at most one.
CREATE OR REPLACE FUNCTION current_family_id()
RETURNS uuid LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
  SELECT m.family_id
  FROM family_members m
  WHERE m.user_id = auth.uid() AND m.status = 'accepted'
  LIMIT 1;
$$;

-- The caller's family row, or no rows when they aren't in one.
--
-- The client MUST use this rather than a blanket `select()` on `families`: the
-- families_invitees_select policy below deliberately widens SELECT to families the caller has only
-- been *invited* to, so an unfiltered select would pull an unjoined family into the local cache.
-- That's the same shape as the over-fetch fixed in 20260725_recipes_sync_excludes_public.sql.
DROP FUNCTION IF EXISTS current_family();
CREATE OR REPLACE FUNCTION current_family()
RETURNS TABLE(id uuid, name text, owner_id uuid, created_at timestamptz, updated_at timestamptz)
LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
  SELECT f.id, f.name::text, f.owner_id, f.created_at, f.updated_at
  FROM families f
  WHERE f.id = current_family_id();
$$;

CREATE OR REPLACE FUNCTION can_access_family(p_family_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
  SELECT EXISTS (
    SELECT 1 FROM families f
    WHERE f.id = p_family_id AND f.owner_id = auth.uid()
  ) OR EXISTS (
    SELECT 1 FROM family_members m
    WHERE m.family_id = p_family_id
      AND m.user_id = auth.uid()
      AND m.status = 'accepted'
  );
$$;

CREATE OR REPLACE FUNCTION is_family_owner(p_family_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
  SELECT EXISTS (
    SELECT 1 FROM families f
    WHERE f.id = p_family_id AND f.owner_id = auth.uid()
  );
$$;

-- Full member list for a family — owner plus every invited member — readable by anyone on the
-- family. SECURITY DEFINER so it can resolve names/avatars from auth.users, which the
-- authenticated role can't read. Clone of recipe_book_collaborators / grocery_list_collaborators;
-- the client reuses the same 7-column shape.
-- DROP first: CREATE OR REPLACE can't change a function's return type.
DROP FUNCTION IF EXISTS family_members_with_profiles(uuid);
CREATE OR REPLACE FUNCTION family_members_with_profiles(p_family_id uuid)
RETURNS TABLE(
  member_id uuid, email text, name text, role text, status text, is_owner boolean, avatar_url text
)
LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
  -- Sort outside the UNION: a set-operation ORDER BY can only reference the union's output
  -- columns, not expressions over them.
  SELECT c.member_id, c.email, c.name, c.role, c.status, c.is_owner, c.avatar_url
  FROM (
    SELECT NULL::uuid AS member_id, u.email::text AS email,
           (u.raw_user_meta_data ->> 'name')::text AS name,
           'owner'::text AS role, 'accepted'::text AS status, true AS is_owner,
           (u.raw_user_meta_data ->> 'avatar_url')::text AS avatar_url
    FROM families f
    JOIN auth.users u ON u.id = f.owner_id
    WHERE f.id = p_family_id AND can_access_family(p_family_id)
    UNION ALL
    -- Skip the auto-inserted owner row so the owner isn't listed twice; the synthesized row above
    -- already covers them.
    SELECT m.id, m.invited_email, (mu.raw_user_meta_data ->> 'name')::text,
           m.role::text, m.status::text, false,
           (mu.raw_user_meta_data ->> 'avatar_url')::text
    FROM family_members m
    LEFT JOIN auth.users mu ON mu.id = m.user_id
    JOIN families f ON f.id = m.family_id
    WHERE m.family_id = p_family_id
      AND can_access_family(p_family_id)
      AND NOT (m.role = 'owner' AND m.user_id = f.owner_id)
  ) c
  ORDER BY c.is_owner DESC, (c.status = 'accepted') DESC, c.email ASC;
$$;

-- Pending family invites addressed to the current user, with the family name for the invite card.
-- SECURITY DEFINER avoids an embed across RLS-protected tables. Clone of
-- grocery_list_pending_invites.
DROP FUNCTION IF EXISTS family_pending_invites();
CREATE OR REPLACE FUNCTION family_pending_invites()
RETURNS TABLE(member_id uuid, family_id uuid, family_name text, role text, status text)
LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
  SELECT m.id, m.family_id, f.name::text, m.role::text, m.status::text
  FROM family_members m
  JOIN families f ON f.id = m.family_id
  WHERE m.status = 'pending'
    AND lower(m.invited_email) = lower(current_user_email())
  ORDER BY f.name ASC, m.created_at ASC;
$$;

-- ============================================================
-- 4. RLS policies
-- ============================================================

ALTER TABLE families ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_members ENABLE ROW LEVEL SECURITY;

-- === families ===
DROP POLICY IF EXISTS "families_select" ON families;
DROP POLICY IF EXISTS "families_invitees_select" ON families;
DROP POLICY IF EXISTS "families_insert" ON families;
DROP POLICY IF EXISTS "families_update" ON families;
DROP POLICY IF EXISTS "families_delete" ON families;

CREATE POLICY "families_select" ON families FOR SELECT
  USING (can_access_family(id));

-- Pending invitees need to read the family row to show its name on the invite card, before they
-- have accepted and can_access_family() starts returning true. Mirrors
-- grocery_lists_invitees_select.
CREATE POLICY "families_invitees_select" ON families FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM family_members m
      WHERE m.family_id = families.id
        AND m.status = 'pending'
        AND lower(m.invited_email) = lower(current_user_email())
    )
  );

CREATE POLICY "families_insert" ON families FOR INSERT
  WITH CHECK (owner_id = auth.uid());

CREATE POLICY "families_update" ON families FOR UPDATE
  USING (is_family_owner(id));

CREATE POLICY "families_delete" ON families FOR DELETE
  USING (is_family_owner(id));

-- === family_members ===
DROP POLICY IF EXISTS "fm_select" ON family_members;
DROP POLICY IF EXISTS "fm_insert" ON family_members;
DROP POLICY IF EXISTS "fm_update" ON family_members;
DROP POLICY IF EXISTS "fm_delete" ON family_members;

-- See member rows for a family you're on, plus invites addressed to you.
CREATE POLICY "fm_select" ON family_members FOR SELECT USING (
  can_access_family(family_id)
  OR user_id = auth.uid()
  OR lower(invited_email) = lower(current_user_email())
);

-- Only the family owner can invite.
CREATE POLICY "fm_insert" ON family_members FOR INSERT WITH CHECK (
  is_family_owner(family_id)
);

-- Owner can change roles; the invitee can accept/reject their own invite.
CREATE POLICY "fm_update" ON family_members FOR UPDATE USING (
  is_family_owner(family_id)
  OR user_id = auth.uid()
  OR lower(invited_email) = lower(current_user_email())
);

-- Owner can remove anyone; a member can leave / decline.
CREATE POLICY "fm_delete" ON family_members FOR DELETE USING (
  is_family_owner(family_id)
  OR user_id = auth.uid()
  OR lower(invited_email) = lower(current_user_email())
);

-- ============================================================
-- 5. Invite email
--
-- Extends notify_invite_email() (20260707) with a 'family' kind. Replacing the function rather
-- than adding a second one keeps all three invite kinds on one code path; the grocery and
-- recipe-book triggers keep calling this same function and are unaffected.
-- ============================================================

CREATE OR REPLACE FUNCTION notify_invite_email()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  v_kind text := TG_ARGV[0];
  v_base_url text := invite_email_config('project_url');
  v_secret text := invite_email_config('invite_hook_secret');
  v_parent_id uuid;
  v_invited_by uuid;
BEGIN
  -- Only a genuine new invite: pending, not yet linked to a user (skips the accepted owner
  -- auto-row and any pre-linked self-add).
  IF NEW.status <> 'pending' OR NEW.user_id IS NOT NULL THEN
    RETURN NEW;
  END IF;

  -- Missing config: don't fail the invite insert — just skip the email.
  IF v_base_url IS NULL OR v_secret IS NULL THEN
    RETURN NEW;
  END IF;

  -- The parent id column differs per table, so read it dynamically from the NEW row.
  -- grocery_list_members and family_members carry invited_by; recipe_book_members does not, so we
  -- default it to NULL there (the edge function falls back to the parent owner).
  IF v_kind = 'grocery' THEN
    v_parent_id := NEW.list_id;
    v_invited_by := NEW.invited_by;
  ELSIF v_kind = 'family' THEN
    v_parent_id := NEW.family_id;
    v_invited_by := NEW.invited_by;
  ELSE
    v_parent_id := NEW.recipe_book_id;
    v_invited_by := NULL;
  END IF;

  -- Async: net.http_post queues into net.http_request_queue and returns immediately, so a slow or
  -- failing email never blocks or rolls back the invite insert.
  PERFORM net.http_post(
    url := rtrim(v_base_url, '/') || '/functions/v1/send-invite-email',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer ' || v_secret
    ),
    body := jsonb_build_object(
      'kind', v_kind,
      'memberId', NEW.id,
      'parentId', v_parent_id,
      'invitedEmail', NEW.invited_email,
      'invitedBy', v_invited_by,
      'role', NEW.role
    )
  );

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notify_family_invite_email ON family_members;
CREATE TRIGGER trg_notify_family_invite_email
  AFTER INSERT ON family_members
  FOR EACH ROW EXECUTE FUNCTION notify_invite_email('family');

-- ============================================================
-- 6. Realtime (idempotent)
--
-- REPLICA IDENTITY FULL so UPDATE/DELETE events still carry enough of the old row to survive RLS
-- evaluation — without it those events are silently dropped for subscribers
-- (see 20260711_grocery_items_replica_identity_full.sql).
-- ============================================================

ALTER TABLE families REPLICA IDENTITY FULL;
ALTER TABLE family_members REPLICA IDENTITY FULL;

DO $$ BEGIN
  ALTER PUBLICATION supabase_realtime ADD TABLE families;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
  ALTER PUBLICATION supabase_realtime ADD TABLE family_members;
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

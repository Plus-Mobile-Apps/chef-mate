-- Families — a user can belong to many families, each with an owner, admins, and members.
--
-- This first phase is membership only: families don't share any data yet. Members are added by
-- email invite (status='pending'); the invitee accepts or declines from the in-app Notifications
-- screen, same as recipe-book collaboration.
--
-- Permission rules:
--   * owner  — invites (as admin or member), promotes/demotes, removes anyone but themselves,
--              renames and deletes the family. Can't leave (deletes instead) and can't be removed.
--   * admin  — invites members, removes members (and member invites), renames the family, leaves.
--   * member — views the family and leaves.
--
-- RLS only grants SELECT. Every write goes through a SECURITY DEFINER RPC below that enforces the
-- rules above and raises on violation, so the per-column checks (e.g. "an admin may not change a
-- role") don't have to be expressed as RLS policies. The owner is a regular member row with
-- role='owner', so member lists need no synthesized entry.
--
-- Safe to re-run (idempotent).

DO $$ BEGIN
    CREATE TYPE family_role AS ENUM ('owner', 'admin', 'member');
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS families (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    owner_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS family_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    family_id UUID NOT NULL REFERENCES families(id) ON DELETE CASCADE,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    invited_email TEXT NOT NULL,
    role family_role NOT NULL DEFAULT 'member',
    status TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected')),
    invited_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One row per email per family (a re-invite after a decline reuses the row).
CREATE UNIQUE INDEX IF NOT EXISTS idx_family_members_family_email
    ON family_members(family_id, lower(invited_email));
-- Exactly one owner row per family.
CREATE UNIQUE INDEX IF NOT EXISTS idx_family_members_one_owner
    ON family_members(family_id) WHERE role = 'owner';
CREATE INDEX IF NOT EXISTS idx_family_members_user ON family_members(user_id);
CREATE INDEX IF NOT EXISTS idx_family_members_email ON family_members(lower(invited_email));

-- ---------------------------------------------------------------------------
-- Helpers (SECURITY DEFINER so they bypass RLS and can't recurse).
-- ---------------------------------------------------------------------------

-- The caller's accepted role in the family, or NULL when they aren't a member.
CREATE OR REPLACE FUNCTION family_role_of(p_family_id uuid)
RETURNS family_role LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT m.role FROM family_members m
    WHERE m.family_id = p_family_id AND m.user_id = auth.uid() AND m.status = 'accepted'
    LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION is_family_member(p_family_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT family_role_of(p_family_id) IS NOT NULL;
$$;

CREATE OR REPLACE FUNCTION is_family_admin(p_family_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT coalesce(family_role_of(p_family_id) IN ('owner', 'admin'), false);
$$;

-- Raises the error every RPC uses for a rule violation.
CREATE OR REPLACE FUNCTION family_forbidden(p_reason text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'family: %', p_reason USING ERRCODE = '42501';
END;
$$;

-- ---------------------------------------------------------------------------
-- RLS — read-only; writes go through the RPCs below.
-- ---------------------------------------------------------------------------
ALTER TABLE families ENABLE ROW LEVEL SECURITY;
ALTER TABLE family_members ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Members and invitees can view families" ON families;
CREATE POLICY "Members and invitees can view families" ON families
    FOR SELECT USING (
        is_family_member(id)
        OR EXISTS (
            SELECT 1 FROM family_members m
            WHERE m.family_id = families.id
              AND m.status = 'pending'
              AND lower(m.invited_email) = lower(current_user_email())
        )
    );

DROP POLICY IF EXISTS "Members can view family members or own invites" ON family_members;
CREATE POLICY "Members can view family members or own invites" ON family_members
    FOR SELECT USING (
        is_family_member(family_id)
        OR user_id = auth.uid()
        OR lower(invited_email) = lower(current_user_email())
    );

-- ---------------------------------------------------------------------------
-- Read RPCs
-- ---------------------------------------------------------------------------

-- Families the caller has joined, with their role and the number of accepted members.
DROP FUNCTION IF EXISTS my_families();
CREATE OR REPLACE FUNCTION my_families()
RETURNS TABLE(id uuid, name text, my_role text, member_count integer)
LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT f.id, f.name, m.role::text,
           (SELECT count(*)::integer FROM family_members c
            WHERE c.family_id = f.id AND c.status = 'accepted')
    FROM families f
    JOIN family_members m ON m.family_id = f.id
    WHERE m.user_id = auth.uid() AND m.status = 'accepted'
    ORDER BY lower(f.name), f.created_at;
$$;

-- Every member row of a family (owner first, then admins, then members; accepted before pending),
-- with profile name/avatar from auth.users. Empty for callers who aren't members.
DROP FUNCTION IF EXISTS family_member_list(uuid);
CREATE OR REPLACE FUNCTION family_member_list(p_family_id uuid)
RETURNS TABLE(
    member_id uuid, email text, name text, avatar_url text, role text, status text, is_self boolean
)
LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT m.id,
           coalesce(u.email::text, m.invited_email),
           (u.raw_user_meta_data ->> 'name')::text,
           (u.raw_user_meta_data ->> 'avatar_url')::text,
           m.role::text,
           m.status,
           m.user_id IS NOT DISTINCT FROM auth.uid() AND m.user_id IS NOT NULL
    FROM family_members m
    LEFT JOIN auth.users u ON u.id = m.user_id
    WHERE m.family_id = p_family_id AND is_family_member(p_family_id)
    ORDER BY
        CASE m.role WHEN 'owner' THEN 0 WHEN 'admin' THEN 1 ELSE 2 END,
        (m.status = 'accepted') DESC,
        lower(coalesce(u.email::text, m.invited_email));
$$;

-- Pending invites addressed to the caller's email, with the family's name.
DROP FUNCTION IF EXISTS my_pending_family_invites();
CREATE OR REPLACE FUNCTION my_pending_family_invites()
RETURNS TABLE(member_id uuid, family_id uuid, family_name text, role text)
LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT m.id, f.id, f.name, m.role::text
    FROM family_members m
    JOIN families f ON f.id = m.family_id
    WHERE m.status = 'pending'
      AND lower(m.invited_email) = lower(current_user_email())
    ORDER BY m.created_at;
$$;

-- ---------------------------------------------------------------------------
-- Write RPCs
-- ---------------------------------------------------------------------------

-- Creates a family owned by the caller and their accepted owner row, atomically.
CREATE OR REPLACE FUNCTION create_family(p_name text)
RETURNS uuid LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_family_id uuid;
    v_email text := current_user_email();
BEGIN
    IF auth.uid() IS NULL OR v_email IS NULL OR v_email = '' THEN
        PERFORM family_forbidden('a signed-in account is required');
    END IF;
    IF p_name IS NULL OR length(trim(p_name)) = 0 THEN
        RAISE EXCEPTION 'family: name is required' USING ERRCODE = '22023';
    END IF;

    INSERT INTO families (name, owner_id) VALUES (trim(p_name), auth.uid())
    RETURNING id INTO v_family_id;

    INSERT INTO family_members (family_id, user_id, invited_email, role, status, invited_by)
    VALUES (v_family_id, auth.uid(), v_email, 'owner', 'accepted', auth.uid());

    RETURN v_family_id;
END;
$$;

-- Owner or admin renames the family.
CREATE OR REPLACE FUNCTION rename_family(p_family_id uuid, p_name text)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF NOT is_family_admin(p_family_id) THEN
        PERFORM family_forbidden('only owners and admins can rename a family');
    END IF;
    IF p_name IS NULL OR length(trim(p_name)) = 0 THEN
        RAISE EXCEPTION 'family: name is required' USING ERRCODE = '22023';
    END IF;
    UPDATE families SET name = trim(p_name), updated_at = now() WHERE id = p_family_id;
END;
$$;

-- Owner deletes the family (member rows cascade).
CREATE OR REPLACE FUNCTION delete_family(p_family_id uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
BEGIN
    IF family_role_of(p_family_id) IS DISTINCT FROM 'owner' THEN
        PERFORM family_forbidden('only the owner can delete a family');
    END IF;
    DELETE FROM families WHERE id = p_family_id;
END;
$$;

-- Owner invites as admin or member; admin invites as member only. A previously declined invite
-- for the same email is reset to pending; an existing pending/accepted row is an error.
CREATE OR REPLACE FUNCTION invite_family_member(p_family_id uuid, p_email text, p_role text)
RETURNS uuid LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_actor family_role := family_role_of(p_family_id);
    v_role family_role;
    v_email text := lower(trim(p_email));
    v_existing family_members%ROWTYPE;
    v_id uuid;
BEGIN
    IF v_actor IS NULL OR v_actor = 'member' THEN
        PERFORM family_forbidden('only owners and admins can invite');
    END IF;
    IF p_role NOT IN ('admin', 'member') THEN
        RAISE EXCEPTION 'family: invalid role %', p_role USING ERRCODE = '22023';
    END IF;
    v_role := p_role::family_role;
    IF v_role = 'admin' AND v_actor <> 'owner' THEN
        PERFORM family_forbidden('only the owner can invite admins');
    END IF;
    IF v_email IS NULL OR v_email = '' OR position('@' in v_email) = 0 THEN
        RAISE EXCEPTION 'family: invalid email' USING ERRCODE = '22023';
    END IF;

    SELECT * INTO v_existing FROM family_members
    WHERE family_id = p_family_id AND lower(invited_email) = v_email;

    IF FOUND THEN
        IF v_existing.status <> 'rejected' THEN
            RAISE EXCEPTION 'family: % is already invited or a member', v_email
                USING ERRCODE = '23505';
        END IF;
        UPDATE family_members
        SET status = 'pending', role = v_role, user_id = NULL, invited_by = auth.uid(),
            updated_at = now()
        WHERE id = v_existing.id
        RETURNING id INTO v_id;
    ELSE
        INSERT INTO family_members (family_id, invited_email, role, status, invited_by)
        VALUES (p_family_id, v_email, v_role, 'pending', auth.uid())
        RETURNING id INTO v_id;
    END IF;
    RETURN v_id;
END;
$$;

-- Removes a member or cancels an invite. The owner row is never removable; only the owner can
-- remove an admin; admins can remove members. Leaving yourself goes through leave_family.
CREATE OR REPLACE FUNCTION remove_family_member(p_member_id uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_target family_members%ROWTYPE;
    v_actor family_role;
BEGIN
    SELECT * INTO v_target FROM family_members WHERE id = p_member_id;
    IF NOT FOUND THEN
        RETURN;
    END IF;
    v_actor := family_role_of(v_target.family_id);

    IF v_target.role = 'owner' THEN
        PERFORM family_forbidden('the owner can''t be removed');
    ELSIF v_target.user_id IS NOT NULL AND v_target.user_id = auth.uid() THEN
        PERFORM family_forbidden('use leave_family to leave');
    ELSIF v_target.role = 'admin' AND v_actor IS DISTINCT FROM 'owner' THEN
        PERFORM family_forbidden('only the owner can remove an admin');
    ELSIF v_target.role = 'member' AND (v_actor IS NULL OR v_actor = 'member') THEN
        PERFORM family_forbidden('only owners and admins can remove members');
    END IF;

    DELETE FROM family_members WHERE id = p_member_id;
END;
$$;

-- Owner promotes a member to admin or demotes an admin to member.
CREATE OR REPLACE FUNCTION set_family_member_role(p_member_id uuid, p_role text)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_target family_members%ROWTYPE;
BEGIN
    SELECT * INTO v_target FROM family_members WHERE id = p_member_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'family: member not found' USING ERRCODE = 'P0002';
    END IF;
    IF family_role_of(v_target.family_id) IS DISTINCT FROM 'owner' THEN
        PERFORM family_forbidden('only the owner can change roles');
    END IF;
    IF v_target.role = 'owner' THEN
        PERFORM family_forbidden('the owner''s role can''t be changed');
    END IF;
    IF p_role NOT IN ('admin', 'member') THEN
        RAISE EXCEPTION 'family: invalid role %', p_role USING ERRCODE = '22023';
    END IF;
    UPDATE family_members SET role = p_role::family_role, updated_at = now()
    WHERE id = p_member_id;
END;
$$;

-- An admin or member removes their own membership. The owner deletes the family instead.
CREATE OR REPLACE FUNCTION leave_family(p_family_id uuid)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_actor family_role := family_role_of(p_family_id);
BEGIN
    IF v_actor IS NULL THEN
        RETURN;
    END IF;
    IF v_actor = 'owner' THEN
        PERFORM family_forbidden('the owner can''t leave; delete the family instead');
    END IF;
    DELETE FROM family_members
    WHERE family_id = p_family_id AND user_id = auth.uid();
END;
$$;

-- The invitee accepts (stamping their user id) or declines (kept as 'rejected' so admins can see
-- the outcome) a pending invite addressed to their email.
CREATE OR REPLACE FUNCTION respond_family_invite(p_member_id uuid, p_accept boolean)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
    v_target family_members%ROWTYPE;
BEGIN
    SELECT * INTO v_target FROM family_members WHERE id = p_member_id;
    IF NOT FOUND
        OR v_target.status <> 'pending'
        OR lower(v_target.invited_email) IS DISTINCT FROM lower(current_user_email()) THEN
        PERFORM family_forbidden('no pending invite for this account');
    END IF;

    IF p_accept THEN
        UPDATE family_members
        SET status = 'accepted', user_id = auth.uid(), updated_at = now()
        WHERE id = p_member_id;
    ELSE
        UPDATE family_members SET status = 'rejected', updated_at = now()
        WHERE id = p_member_id;
    END IF;
END;
$$;

-- RPCs are for signed-in users only.
REVOKE EXECUTE ON FUNCTION
    my_families(), family_member_list(uuid), my_pending_family_invites(),
    create_family(text), rename_family(uuid, text), delete_family(uuid),
    invite_family_member(uuid, text, text), remove_family_member(uuid),
    set_family_member_role(uuid, text), leave_family(uuid), respond_family_invite(uuid, boolean)
    FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION
    my_families(), family_member_list(uuid), my_pending_family_invites(),
    create_family(text), rename_family(uuid, text), delete_family(uuid),
    invite_family_member(uuid, text, text), remove_family_member(uuid),
    set_family_member_role(uuid, text), leave_family(uuid), respond_family_invite(uuid, boolean)
    TO authenticated;

-- Feature flag gating the More-tab entry point. `value` is what users inside the rollout get, so
-- it's 'true' with rollout_percent = 0: off for everyone until the rollout (or user_ids) is set.
INSERT INTO feature_flags (key, value_type, value, enabled, rollout_percent, description)
VALUES ('manage_family', 'bool', 'true', true, 0, 'Show the Manage Family row in the More tab.')
ON CONFLICT (key) DO NOTHING;

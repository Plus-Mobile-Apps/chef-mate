-- ============================================================
-- Email notification on family invite
--
-- Extends the existing invite-email trigger (20260707_invite_email_notification.sql) to families.
-- Two things differ from grocery lists and recipe books:
--
--   1. A family invite is created by the `invite_family_member` RPC, which INSERTs a new row *or*
--      flips a previously rejected row back to 'pending'. So this trigger fires on INSERT OR
--      UPDATE and, on update, only when the row actually becomes a fresh pending invite.
--   2. `families` names its owner column `owner_id`, matching what the edge function already reads
--      — but family invites always carry `invited_by`, so the function uses that and never needs
--      the owner fallback.
--
-- The edge function must be redeployed alongside this migration; it learns 'family' as a third
-- `kind`. Operator setup (the `project_url` / `invite_hook_secret` Vault secrets) is unchanged and
-- is documented in 20260707_invite_email_notification.sql.
--
-- Safe to re-run (idempotent).
-- ============================================================

-- Fires for a family invite that is pending and not yet linked to an account. On UPDATE it must
-- also have just *become* pending, otherwise every later edit of the row (accept, role change)
-- would re-send the email.
CREATE OR REPLACE FUNCTION notify_family_invite_email()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  v_base_url text := invite_email_config('project_url');
  v_secret text := invite_email_config('invite_hook_secret');
BEGIN
  IF NEW.status <> 'pending' OR NEW.user_id IS NOT NULL THEN
    RETURN NEW;
  END IF;

  -- A re-invite after a decline is an UPDATE; anything else that touches an already-pending row
  -- is not a new invite and must not mail again.
  IF TG_OP = 'UPDATE' AND OLD.status = 'pending' THEN
    RETURN NEW;
  END IF;

  -- Missing config: skip the email rather than failing the invite. See the 20260707 migration.
  IF v_base_url IS NULL OR v_secret IS NULL THEN
    RETURN NEW;
  END IF;

  -- Async, exactly as the other kinds: a slow or failing send never blocks the invite.
  PERFORM net.http_post(
    url := rtrim(v_base_url, '/') || '/functions/v1/send-invite-email',
    headers := jsonb_build_object(
      'Content-Type', 'application/json',
      'Authorization', 'Bearer ' || v_secret
    ),
    body := jsonb_build_object(
      'kind', 'family',
      'memberId', NEW.id,
      'parentId', NEW.family_id,
      'invitedEmail', NEW.invited_email,
      'invitedBy', NEW.invited_by,
      'role', NEW.role
    )
  );

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_notify_family_invite_email ON family_members;
CREATE TRIGGER trg_notify_family_invite_email
  AFTER INSERT OR UPDATE OF status ON family_members
  FOR EACH ROW EXECUTE FUNCTION notify_family_invite_email();

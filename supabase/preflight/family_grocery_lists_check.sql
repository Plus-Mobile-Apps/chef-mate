-- Read-only preflight for 20261014_family_grocery_lists.sql. Run in the dashboard SQL editor before
-- applying: the migration drops EVERY policy on grocery_lists and grocery_items, so review what's
-- there and make sure nothing below needs to survive beyond the set the migration recreates
-- (grocery_lists_select / _invitees_select / _insert / _update / _delete, grocery_items_select /
-- _insert / _update / _delete).

-- 1. Prerequisites: the families migration (is_family_member, is_family_admin) and the
--    collaboration helpers.
SELECT p.proname
FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE n.nspname = 'public'
  AND p.proname IN ('is_family_member', 'is_family_admin', 'family_forbidden',
                    'can_access_grocery_list', 'can_edit_grocery_list', 'is_grocery_list_owner',
                    'current_user_email', 'auto_add_grocery_list_owner')
ORDER BY 1;

-- 2. Policies the migration will drop and replace.
SELECT tablename, policyname, cmd, qual, with_check
FROM pg_policies
WHERE schemaname = 'public' AND tablename IN ('grocery_lists', 'grocery_items')
ORDER BY tablename, policyname;

-- 3. Columns — family_id should not exist yet (or be a nullable uuid if re-running).
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name IN ('grocery_lists', 'grocery_items')
ORDER BY table_name, ordinal_position;

-- 4. Triggers on grocery_lists (the owner-member trigger is replaced to skip family lists).
SELECT tgname, pg_get_triggerdef(t.oid)
FROM pg_trigger t
WHERE t.tgrelid = 'public.grocery_lists'::regclass AND NOT t.tgisinternal;

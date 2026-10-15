-- Read-only preflight for 20261015_family_recipe_books.sql. Run in the dashboard SQL editor before
-- applying: the migration drops EVERY policy on recipe_books and recipe_book_recipes, so review
-- what's there and make sure nothing below needs to survive beyond the set the migration recreates
-- (recipe_books_select / _invitees_select / _insert / _update / _delete, recipe_book_recipes_select
-- / _insert / _delete).
--
-- The dashboard SQL editor only shows the LAST statement's result, so everything is one UNION.

-- 1. prereq: helper functions the migration calls or replaces (all should be present).
-- 2. policy: the policies the migration will drop and replace.
-- 3. column: recipe_books columns — family_id should not exist yet (or be a nullable uuid).
-- 4. trigger: non-internal triggers on recipe_books (none are expected).
SELECT 'prereq' AS kind, p.proname::text AS name, NULL::text AS detail, NULL::text AS detail2
FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE n.nspname = 'public'
  AND p.proname IN ('is_family_member', 'is_family_admin', 'family_forbidden',
                    'can_access_recipe_book', 'can_edit_recipe_book', 'is_recipe_book_owner',
                    'can_access_recipe', 'can_edit_recipe', 'can_access_recipe_via_book',
                    'can_plan_recipe', 'current_user_email', 'get_accessible_recipes')
UNION ALL
SELECT 'policy', tablename || '.' || policyname, cmd, coalesce(qual, '') || ' | ' || coalesce(with_check, '')
FROM pg_policies
WHERE schemaname = 'public' AND tablename IN ('recipe_books', 'recipe_book_recipes')
UNION ALL
SELECT 'column', column_name::text, data_type::text, is_nullable::text
FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'recipe_books'
UNION ALL
SELECT 'trigger', tgname::text, pg_get_triggerdef(t.oid), NULL
FROM pg_trigger t
WHERE t.tgrelid = 'public.recipe_books'::regclass AND NOT t.tgisinternal
ORDER BY 1, 2;

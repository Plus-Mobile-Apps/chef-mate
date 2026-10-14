-- Family meal plans — a meal can belong to a family instead of only its creator.
--
-- A meal with family_id = NULL is personal (visible to its owner only, as before). A meal with a
-- family_id is on that family's shared plan: every accepted member can see it and remove it, and
-- any member can add one. Members who aren't the creator can't edit it — the client never edits
-- meals in place (it adds and removes), and owner-only updates stop a member from moving someone
-- else's meal out of the family into that person's personal plan.
--
-- Recipes: meals reference their recipe by UUID and the client only shows a meal whose recipe it
-- has synced locally, so family members must be able to read the recipe behind a family meal.
-- can_access_recipe() (used by the recipes/recipe_categories SELECT policies and by
-- get_accessible_recipes()) now also grants read access to a recipe planned in one of the caller's
-- families. Read only — can_edit_recipe() is untouched.
--
-- Because a family meal grants read access to its recipe, INSERT/UPDATE require that the caller
-- can already read the recipe (owns it, or reaches it through a shared book or an existing family
-- meal). Without that check a member could plan someone else's private recipe UUID into the family
-- and gain read access to it. The check's subquery runs on the statement's snapshot, so the row
-- being inserted can't vouch for itself.
--
-- If a family is deleted, its meals fall back to their creators' personal plans (ON DELETE SET
-- NULL). Leaving a family revokes access to the family's meals and their recipes; clients prune
-- them on the next sync.
--
-- NOTE: meal_plans was created by hand in prod and has no migration (see the drift notes), so its
-- existing policy names are unknown. This migration drops EVERY policy on meal_plans and recreates
-- the full set below. Requires 20260916_add_families.sql (is_family_member).
--
-- Safe to re-run (idempotent).

ALTER TABLE public.meal_plans
    ADD COLUMN IF NOT EXISTS family_id UUID REFERENCES public.families(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_meal_plans_family ON public.meal_plans(family_id)
    WHERE family_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_meal_plans_family_recipe ON public.meal_plans(recipe_id, family_id)
    WHERE family_id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- Recipe access helpers
-- ---------------------------------------------------------------------------

-- The original book-only rule (20260610), kept as its own helper.
CREATE OR REPLACE FUNCTION can_access_recipe_via_book(p_recipe_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT EXISTS (
        SELECT 1 FROM recipe_book_recipes rbr
        WHERE rbr.recipe_id = p_recipe_id
          AND can_access_recipe_book(rbr.recipe_book_id)
    );
$$;

-- True when the recipe is planned as a meal in one of the caller's families.
CREATE OR REPLACE FUNCTION can_access_recipe_via_family_meal(p_recipe_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT EXISTS (
        SELECT 1 FROM meal_plans mp
        WHERE mp.recipe_id = p_recipe_id
          AND mp.family_id IS NOT NULL
          AND is_family_member(mp.family_id)
    );
$$;

-- Shared (non-owned) read access: a shared book or a family meal.
CREATE OR REPLACE FUNCTION can_access_recipe(p_recipe_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT can_access_recipe_via_book(p_recipe_id)
        OR can_access_recipe_via_family_meal(p_recipe_id);
$$;

-- Whether the caller may reference the recipe from a meal: they own it or can already read it.
CREATE OR REPLACE FUNCTION can_plan_recipe(p_recipe_id uuid)
RETURNS boolean LANGUAGE sql SECURITY DEFINER STABLE SET search_path = public AS $$
    SELECT EXISTS (SELECT 1 FROM recipes r WHERE r.id = p_recipe_id AND r.owner_id = auth.uid())
        OR can_access_recipe(p_recipe_id);
$$;

-- ---------------------------------------------------------------------------
-- meal_plans RLS
-- ---------------------------------------------------------------------------
ALTER TABLE public.meal_plans ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE
    pol record;
BEGIN
    FOR pol IN
        SELECT policyname FROM pg_policies WHERE schemaname = 'public' AND tablename = 'meal_plans'
    LOOP
        EXECUTE format('DROP POLICY %I ON public.meal_plans', pol.policyname);
    END LOOP;
END $$;

CREATE POLICY "Owners and family members can view meal plans" ON public.meal_plans
    FOR SELECT USING (
        owner_id = auth.uid()
        OR (family_id IS NOT NULL AND is_family_member(family_id))
    );

CREATE POLICY "Users can add their own or family meal plans" ON public.meal_plans
    FOR INSERT WITH CHECK (
        owner_id = auth.uid()
        AND (family_id IS NULL OR is_family_member(family_id))
        AND can_plan_recipe(recipe_id)
    );

CREATE POLICY "Owners can update their meal plans" ON public.meal_plans
    FOR UPDATE USING (owner_id = auth.uid())
    WITH CHECK (
        owner_id = auth.uid()
        AND (family_id IS NULL OR is_family_member(family_id))
        AND can_plan_recipe(recipe_id)
    );

CREATE POLICY "Owners and family members can delete meal plans" ON public.meal_plans
    FOR DELETE USING (
        owner_id = auth.uid()
        OR (family_id IS NOT NULL AND is_family_member(family_id))
    );

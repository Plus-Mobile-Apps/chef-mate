package com.plusmobileapps.chefmate.meal.data

/**
 * Remembers the meal plan the user last picked so the planner reopens on it next launch. Only
 * stores the choice — [MealPlanScopeRepository] decides whether it's still available.
 */
interface MealPlanSelectionRepository {
    /** The plan the user last picked; [MealPlanScope.Personal] when they haven't picked one. */
    val lastSelectedScope: MealPlanScope

    fun select(scope: MealPlanScope)

    fun clear()
}

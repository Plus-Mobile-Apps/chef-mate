package com.plusmobileapps.chefmate.meal.data

import kotlinx.coroutines.flow.StateFlow

/**
 * The meal plan the user is working in — personal or one of their families' shared plans — shared
 * by the planner and the add-meal flow so a meal is always added to the plan on screen. The choice
 * is remembered across launches.
 */
interface MealPlanScopeRepository {
    /**
     * Families whose shared plans the user can switch to. Empty when the user belongs to no family,
     * isn't signed in, or family meal plans are switched off.
     */
    val familyPlans: StateFlow<List<FamilyPlan>>

    /** The selected plan. Falls back to [MealPlanScope.Personal] when its family goes away. */
    val selectedScope: StateFlow<MealPlanScope>

    fun select(scope: MealPlanScope)

    /** Reloads [familyPlans]. Keeps the last known list when the user is offline. */
    suspend fun refresh()
}

data class FamilyPlan(val familyId: String, val name: String)

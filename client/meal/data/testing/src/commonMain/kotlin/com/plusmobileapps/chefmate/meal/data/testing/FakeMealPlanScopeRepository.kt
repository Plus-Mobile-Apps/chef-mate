package com.plusmobileapps.chefmate.meal.data.testing

import com.plusmobileapps.chefmate.meal.data.FamilyPlan
import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.plusmobileapps.chefmate.meal.data.MealPlanScopeRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMealPlanScopeRepository(
    familyPlans: List<FamilyPlan> = emptyList(),
    selected: MealPlanScope = MealPlanScope.Personal,
) : MealPlanScopeRepository {
    override val familyPlans = MutableStateFlow(familyPlans)
    override val selectedScope = MutableStateFlow(selected)

    /** Number of [refresh] calls, to assert reloads. */
    var refreshCount: Int = 0
        private set

    override fun select(scope: MealPlanScope) {
        selectedScope.value = scope
    }

    override suspend fun refresh() {
        refreshCount++
    }
}

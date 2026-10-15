package com.plusmobileapps.chefmate.meal.data.testing

import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.plusmobileapps.chefmate.meal.data.MealPlanSelectionRepository

class FakeMealPlanSelectionRepository(
    override var lastSelectedScope: MealPlanScope = MealPlanScope.Personal
) : MealPlanSelectionRepository {

    override fun select(scope: MealPlanScope) {
        lastSelectedScope = scope
    }

    override fun clear() {
        lastSelectedScope = MealPlanScope.Personal
    }
}

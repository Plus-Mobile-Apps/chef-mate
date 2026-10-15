@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.meal.data.impl

import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MealPlanSelectionRepositoryImplTest {

    private val settings = MapSettings()

    @Test
    fun defaults_to_the_personal_plan() {
        MealPlanSelectionRepositoryImpl(settings).lastSelectedScope shouldBe MealPlanScope.Personal
    }

    @Test
    fun remembers_the_selected_family_plan_across_launches() {
        MealPlanSelectionRepositoryImpl(settings).select(MealPlanScope.Family("family-1"))

        MealPlanSelectionRepositoryImpl(settings).lastSelectedScope shouldBe
            MealPlanScope.Family("family-1")
    }

    @Test
    fun selecting_personal_replaces_a_remembered_family_plan() {
        val repo = MealPlanSelectionRepositoryImpl(settings)
        repo.select(MealPlanScope.Family("family-1"))

        repo.select(MealPlanScope.Personal)

        MealPlanSelectionRepositoryImpl(settings).lastSelectedScope shouldBe MealPlanScope.Personal
    }

    @Test
    fun reads_the_empty_value_older_builds_stored_for_personal() {
        settings.putString("meal_plan_selected_family_id", "")

        MealPlanSelectionRepositoryImpl(settings).lastSelectedScope shouldBe MealPlanScope.Personal
    }
}

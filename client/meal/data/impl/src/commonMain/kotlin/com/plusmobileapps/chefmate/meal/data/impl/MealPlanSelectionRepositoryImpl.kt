package com.plusmobileapps.chefmate.meal.data.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.plusmobileapps.chefmate.meal.data.MealPlanSelectionRepository
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class MealPlanSelectionRepositoryImpl(private val settings: Settings) :
    MealPlanSelectionRepository {

    override val lastSelectedScope: MealPlanScope
        get() =
            settings
                .getStringOrNull(KEY_SELECTED_FAMILY_ID)
                ?.ifEmpty { null }
                ?.let {
                    MealPlanScope.Family(it)
                } ?: MealPlanScope.Personal

    override fun select(scope: MealPlanScope) {
        val familyId = scope.familyId
        if (familyId == null) clear() else settings.putString(KEY_SELECTED_FAMILY_ID, familyId)
    }

    override fun clear() {
        settings.remove(KEY_SELECTED_FAMILY_ID)
    }

    private companion object {
        // Kept from when the scope repository stored this itself, so existing choices survive.
        const val KEY_SELECTED_FAMILY_ID = "meal_plan_selected_family_id"
    }
}

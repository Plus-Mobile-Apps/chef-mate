package com.plusmobileapps.chefmate.meal.data

/** Which meal plan a meal belongs to: the user's own, or a family's shared plan. */
sealed interface MealPlanScope {
    /** The family's remote id for a family plan; null for the personal plan. */
    val familyId: String?

    data object Personal : MealPlanScope {
        override val familyId: String? = null
    }

    data class Family(override val familyId: String) : MealPlanScope
}

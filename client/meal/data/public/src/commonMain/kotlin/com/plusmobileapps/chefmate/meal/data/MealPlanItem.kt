package com.plusmobileapps.chefmate.meal.data

data class MealPlanItem(
    val id: Long,
    val recipeId: Long,
    val recipeTitle: String,
    val recipeImageUrl: String?,
    val date: String,
    val mealType: MealType,
    /** The family's remote id when the meal is on a family's shared plan; null when personal. */
    val familyId: String? = null,
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED,
)

enum class SyncStatus {
    NOT_SYNCED,
    SYNCING,
    SYNCED,
}

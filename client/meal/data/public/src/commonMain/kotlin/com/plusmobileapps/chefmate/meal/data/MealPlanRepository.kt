package com.plusmobileapps.chefmate.meal.data

import kotlinx.coroutines.flow.Flow

interface MealPlanRepository {
    fun getMealsByDate(
        date: String,
        scope: MealPlanScope = MealPlanScope.Personal,
    ): Flow<List<MealPlanItem>>

    fun getMealsByDateRange(
        startDate: String,
        endDate: String,
        scope: MealPlanScope = MealPlanScope.Personal,
    ): Flow<List<MealPlanItem>>

    suspend fun addMeal(
        recipeId: Long,
        date: String,
        mealType: MealType,
        scope: MealPlanScope = MealPlanScope.Personal,
    )

    suspend fun removeMeal(id: Long)

    suspend fun syncAllUnsynced()

    suspend fun clearLocalData()
}

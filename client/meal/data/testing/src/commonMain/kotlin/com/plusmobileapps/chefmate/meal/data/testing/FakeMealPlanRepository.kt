package com.plusmobileapps.chefmate.meal.data.testing

import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.plusmobileapps.chefmate.meal.data.MealType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeMealPlanRepository : MealPlanRepository {
    private val _meals = MutableStateFlow<List<MealPlanItem>>(emptyList())
    private var nextId = 1L

    override fun getMealsByDate(date: String, scope: MealPlanScope): Flow<List<MealPlanItem>> =
        _meals.map { items ->
            items.filter { it.date == date && it.familyId == scope.familyId }
        }

    override fun getMealsByDateRange(
        startDate: String,
        endDate: String,
        scope: MealPlanScope,
    ): Flow<List<MealPlanItem>> = _meals.map { items ->
        items.filter { it.date in startDate..endDate && it.familyId == scope.familyId }
    }

    override suspend fun addMeal(
        recipeId: Long,
        date: String,
        mealType: MealType,
        scope: MealPlanScope,
    ) {
        val item =
            MealPlanItem(
                id = nextId++,
                recipeId = recipeId,
                recipeTitle = "Recipe $recipeId",
                recipeImageUrl = null,
                date = date,
                mealType = mealType,
                familyId = scope.familyId,
            )
        _meals.update { it + item }
    }

    override suspend fun removeMeal(id: Long) {
        _meals.update { items -> items.filter { it.id != id } }
    }

    override suspend fun syncAllUnsynced() {
        // No-op in fake
    }

    override suspend fun clearLocalData() {
        _meals.value = emptyList()
        nextId = 1L
    }
}

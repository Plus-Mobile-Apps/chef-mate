package com.plusmobileapps.chefmate.meal.data.impl.remote

interface MealPlanRemoteDataSource {
    suspend fun upsertMealPlan(mealPlan: RemoteMealPlan): RemoteMealPlan

    suspend fun deleteMealPlan(remoteId: String)

    /**
     * Every meal the current user can see — their own plus those on their families' shared plans.
     */
    suspend fun fetchAccessibleMealPlans(): List<RemoteMealPlan>
}

package com.plusmobileapps.chefmate.meal.data.impl.remote

import com.plusmobileapps.chefmate.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class SupabaseMealPlanRemoteDataSource(private val supabaseClient: SupabaseClient) :
    MealPlanRemoteDataSource {

    override suspend fun upsertMealPlan(mealPlan: RemoteMealPlan): RemoteMealPlan =
        supabaseClient
            .from("meal_plans")
            .upsert(mealPlan) {
                select()
                if (mealPlan.id == null && mealPlan.clientId != null) {
                    onConflict = "client_id"
                }
            }
            .decodeSingle<RemoteMealPlan>()

    override suspend fun deleteMealPlan(remoteId: String) {
        supabaseClient.from("meal_plans").delete { filter { eq("id", remoteId) } }
    }

    override suspend fun fetchAccessibleMealPlans(): List<RemoteMealPlan> =
        // RLS scopes the rows to owned meals plus meals on the caller's families' plans. Unlike
        // recipes there's no public-read policy on meal_plans, so a blanket select is exact.
        supabaseClient.from("meal_plans").select().decodeList<RemoteMealPlan>()
}

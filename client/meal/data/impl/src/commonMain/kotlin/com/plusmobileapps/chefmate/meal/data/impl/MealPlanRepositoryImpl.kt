package com.plusmobileapps.chefmate.meal.data.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.database.MealPlanQueries
import com.plusmobileapps.chefmate.database.RecipeQueries
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.meal.data.SyncStatus
import com.plusmobileapps.chefmate.meal.data.impl.remote.MealPlanRemoteDataSource
import com.plusmobileapps.chefmate.meal.data.impl.remote.RemoteMealPlan
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.util.DateTimeUtil
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalUuidApi::class)
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class MealPlanRepositoryImpl(
    private val queries: MealPlanQueries,
    private val recipeQueries: RecipeQueries,
    @IO private val ioContext: CoroutineContext,
    private val dateTimeUtil: DateTimeUtil,
    private val remoteDataSource: MealPlanRemoteDataSource,
    private val authRepository: AuthenticationRepository,
    private val recipeRepository: RecipeRepository,
) : MealPlanRepository {

    private val scope = CoroutineScope(ioContext + SupervisorJob())
    private val syncMutex = Mutex()

    init {
        scope.launch {
            authRepository.state.collect { state ->
                if (state is AuthState.Authenticated) {
                    syncWithRemote(state.user.userId)
                }
            }
        }
    }

    override fun getMealsByDate(date: String, scope: MealPlanScope): Flow<List<MealPlanItem>> =
        queries.getByDate(date, scope.familyId).asFlow().mapToList(ioContext).map { rows ->
            rows.map { it.toMealPlanItem() }
        }

    override fun getMealsByDateRange(
        startDate: String,
        endDate: String,
        scope: MealPlanScope,
    ): Flow<List<MealPlanItem>> =
        queries
            .getByDateRange(startDate, endDate, scope.familyId)
            .asFlow()
            .mapToList(ioContext)
            .map { rows -> rows.map { it.toMealPlanItem() } }

    override suspend fun addMeal(
        recipeId: Long,
        date: String,
        mealType: MealType,
        scope: MealPlanScope,
    ) {
        val clientId = Uuid.random().toString()
        val now = dateTimeUtil.now.toString()
        val localId =
            withContext(ioContext) {
                queries.transactionWithResult {
                    queries.insert(
                        recipeId = recipeId,
                        date = date,
                        mealType = mealType.name,
                        clientId = clientId,
                        createdAt = now,
                        updatedAt = now,
                        familyId = scope.familyId,
                    )
                    queries.lastInsertId().executeAsOne().MAX
                        ?: error("Failed to get last insert id")
                }
            }
        pushAddToRemote(localId)
    }

    override suspend fun removeMeal(id: Long) {
        val entity =
            withContext(ioContext) {
                val entity = queries.getById(id).executeAsOneOrNull()
                queries.delete(id)
                entity
            }
        entity?.remoteId?.let { remoteId ->
            scope.launch {
                try {
                    remoteDataSource.deleteMealPlan(remoteId)
                } catch (_: Exception) {}
            }
        }
    }

    override suspend fun syncAllUnsynced() {
        val authState = authRepository.state.value
        if (authState is AuthState.Authenticated) {
            syncWithRemote(authState.user.userId)
        }
    }

    override suspend fun clearLocalData() {
        withContext(ioContext) { queries.deleteAll() }
    }

    private fun pushAddToRemote(localId: Long) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        scope.launch {
            try {
                val entity =
                    withContext(ioContext) { queries.getById(localId).executeAsOneOrNull() }
                        ?: return@launch
                // The remote references the recipe by its UUID, not the device-local id. If the
                // recipe hasn't synced yet the meal stays unsynced and is retried on the next sync.
                val recipeRemoteId = recipeRemoteId(entity.recipeId) ?: return@launch
                val clientId =
                    entity.clientId
                        ?: Uuid.random().toString().also { newId ->
                            withContext(ioContext) {
                                queries.updateClientId(clientId = newId, id = localId)
                            }
                        }
                val remote =
                    remoteDataSource.upsertMealPlan(
                        RemoteMealPlan(
                            ownerId = authState.user.userId,
                            recipeId = recipeRemoteId,
                            date = entity.date,
                            mealType = entity.mealType,
                            createdAt = entity.createdAt,
                            updatedAt = entity.updatedAt,
                            clientId = clientId,
                            familyId = entity.familyId,
                        )
                    )
                withContext(ioContext) {
                    queries.updateRemoteId(remoteId = remote.id, id = localId)
                }
            } catch (_: Exception) {}
        }
    }

    private suspend fun syncWithRemote(userId: String) = syncMutex.withLock {
        try {
            // Meal plans reference their recipe by the recipe's remote UUID, so the recipes have
            // to be present locally before we pull meals — otherwise every remote meal is skipped
            // (its recipe can't be resolved to a local id) and isn't retried until the next sync.
            // On sign-in the recipe and meal syncs both react to the same auth-state emission and
            // race, so ensure recipes have synced first. Recipe sync is guarded by its own mutex
            // and is idempotent, so this coalesces with any in-flight recipe sync.
            recipeRepository.syncAllUnsynced()

            // Snapshot the prune candidates before this pass can assign any remote ids — see
            // the same guard in RecipeRepositoryImpl.
            val pruneCandidates =
                withContext(ioContext) { queries.getSyncedRemoteIds().executeAsList() }

            // Push unsynced meal plans (no remoteId yet)
            val unsynced = withContext(ioContext) { queries.getUnsynced().executeAsList() }
            for (meal in unsynced) {
                try {
                    // Skip meals whose recipe hasn't synced yet — retried once the recipe has a
                    // remote UUID to reference.
                    val recipeRemoteId = recipeRemoteId(meal.recipeId) ?: continue
                    val clientId =
                        meal.clientId
                            ?: Uuid.random().toString().also { newId ->
                                withContext(ioContext) {
                                    queries.updateClientId(clientId = newId, id = meal.id)
                                }
                            }
                    val remote =
                        remoteDataSource.upsertMealPlan(
                            RemoteMealPlan(
                                ownerId = userId,
                                recipeId = recipeRemoteId,
                                date = meal.date,
                                mealType = meal.mealType,
                                createdAt = meal.createdAt,
                                updatedAt = meal.updatedAt,
                                clientId = clientId,
                                familyId = meal.familyId,
                            )
                        )
                    withContext(ioContext) {
                        queries.updateRemoteId(remoteId = remote.id, id = meal.id)
                    }
                } catch (_: Exception) {}
            }

            // Pull every meal the user can see: their own plus their families' shared plans.
            val remoteMeals = remoteDataSource.fetchAccessibleMealPlans()
            withContext(ioContext) {
                for (remote in remoteMeals) {
                    val remoteId = remote.id ?: continue
                    val existing = queries.getByRemoteId(remoteId).executeAsOneOrNull()
                    if (existing != null) {
                        // Meals are never edited in place, but deleting a family drops its meals
                        // back onto their creators' personal plans server-side.
                        if (existing.familyId != remote.familyId) {
                            queries.updateFamilyId(familyId = remote.familyId, id = existing.id)
                        }
                        continue
                    }

                    val matchedByClientId =
                        remote.clientId?.let { clientId ->
                            queries.getByClientId(clientId).executeAsOneOrNull()
                        }
                    if (matchedByClientId != null) {
                        queries.updateRemoteId(remoteId = remoteId, id = matchedByClientId.id)
                    } else {
                        // Resolve the recipe UUID to its local id. If the recipe isn't present
                        // locally yet the meal is skipped and picked up on a later sync.
                        val localRecipeId =
                            recipeQueries.getByRemoteId(remote.recipeId).executeAsOneOrNull()?.id
                                ?: continue
                        queries.insertWithRemoteId(
                            recipeId = localRecipeId,
                            date = remote.date,
                            mealType = remote.mealType,
                            clientId = remote.clientId,
                            createdAt = remote.createdAt ?: dateTimeUtil.now.toString(),
                            updatedAt = remote.updatedAt ?: dateTimeUtil.now.toString(),
                            remoteId = remoteId,
                            // Keep the real creator: a family meal may belong to another member.
                            ownerId = remote.ownerId.ifBlank { userId },
                            familyId = remote.familyId,
                        )
                    }
                }

                // Prune meals the remote no longer lists: deleted on another device or by another
                // family member, or on the plan of a family the user has left. The fetch returns
                // every meal the user can see, so anything synced that's missing is gone for them.
                // Meals without a remoteId are still pending their first push and are preserved.
                val remoteIds = remoteMeals.mapNotNull { it.id }.toSet()
                for (row in pruneCandidates) {
                    if (row.remoteId in remoteIds) continue
                    val current = queries.getById(row.id).executeAsOneOrNull() ?: continue
                    if (current.remoteId != row.remoteId) continue
                    queries.delete(row.id)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Resolves a device-local recipe id to the recipe's remote UUID, or null when the recipe has
     * not been synced yet (no remoteId). Meal plans reference recipes by UUID on the backend so
     * they resolve to the correct recipe on every device.
     */
    private suspend fun recipeRemoteId(localRecipeId: Long): String? =
        withContext(ioContext) {
            recipeQueries.getById(localRecipeId).executeAsOneOrNull()?.remoteId
        }
}

private fun com.plusmobileapps.chefmate.database.GetByDate.toMealPlanItem(): MealPlanItem =
    MealPlanItem(
        id = id,
        recipeId = recipeId,
        recipeTitle = recipeTitle,
        recipeImageUrl = recipeImageUrl,
        date = date,
        mealType = MealType.valueOf(mealType),
        familyId = familyId,
        syncStatus = if (remoteId != null) SyncStatus.SYNCED else SyncStatus.NOT_SYNCED,
    )

private fun com.plusmobileapps.chefmate.database.GetByDateRange.toMealPlanItem(): MealPlanItem =
    MealPlanItem(
        id = id,
        recipeId = recipeId,
        recipeTitle = recipeTitle,
        recipeImageUrl = recipeImageUrl,
        date = date,
        mealType = MealType.valueOf(mealType),
        familyId = familyId,
        syncStatus = if (remoteId != null) SyncStatus.SYNCED else SyncStatus.NOT_SYNCED,
    )

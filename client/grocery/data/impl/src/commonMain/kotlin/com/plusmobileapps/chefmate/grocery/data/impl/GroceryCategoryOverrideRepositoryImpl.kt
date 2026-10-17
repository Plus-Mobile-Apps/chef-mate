package com.plusmobileapps.chefmate.grocery.data.impl

import app.cash.sqldelight.coroutines.asFlow
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.database.GroceryCategoryOverride as DbOverride
import com.plusmobileapps.chefmate.database.GroceryCategoryOverrideQueries
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryCategoryOverride
import com.plusmobileapps.chefmate.grocery.data.GroceryCategoryOverrideRepository
import com.plusmobileapps.chefmate.util.Unique
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Local-only store of name→aisle rules. The `remoteId`/`clientId`/`isDirty`/`ownerId` columns are
 * populated so a future Supabase sync is a data-only follow-up, but no remote data source is wired
 * yet — writes stay on-device.
 */
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
class GroceryCategoryOverrideRepositoryImpl(
    private val db: GroceryCategoryOverrideQueries,
    @IO private val ioContext: CoroutineContext,
    private val unique: Unique,
    private val authRepository: AuthenticationRepository,
) : GroceryCategoryOverrideRepository {

    override fun observeOverrides(): Flow<List<GroceryCategoryOverride>> =
        db.getAll()
            .asFlow()
            .map { query -> query.executeAsList().mapNotNull { it.toModel() } }
            .flowOn(ioContext)

    override fun observeOverrideMap(): Flow<Map<String, GroceryCategory>> =
        observeOverrides().map { overrides ->
            overrides.associate { it.name.lowercase() to it.category }
        }

    override suspend fun setOverride(name: String, category: GroceryCategory) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val ownerId = authRepository.state.value.userIdOrNull()
        val clientId = unique.generate()
        withContext(ioContext) {
            db.upsert(
                name = trimmed,
                categoryKey = category.name,
                clientId = clientId,
                ownerId = ownerId,
            )
        }
    }

    override suspend fun removeOverride(id: Long) {
        withContext(ioContext) { db.deleteById(id) }
    }

    override suspend fun removeOverrideByName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        withContext(ioContext) { db.deleteByName(trimmed) }
    }

    override suspend fun clearLocalData() {
        withContext(ioContext) { db.deleteAll() }
    }

    private fun AuthState.userIdOrNull(): String? = (this as? AuthState.Authenticated)?.user?.userId

    // Rows with an unrecognized categoryKey (e.g. a Phase 2 custom aisle synced from another
    // device that this build doesn't know) are dropped rather than crashing.
    private fun DbOverride.toModel(): GroceryCategoryOverride? {
        val category =
            runCatching { GroceryCategory.valueOf(categoryKey) }.getOrNull() ?: return null
        return GroceryCategoryOverride(id = id, name = name, category = category)
    }
}

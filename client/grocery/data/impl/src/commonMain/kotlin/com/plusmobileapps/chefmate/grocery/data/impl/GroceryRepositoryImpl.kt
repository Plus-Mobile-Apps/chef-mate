package com.plusmobileapps.chefmate.grocery.data.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import co.touchlab.kermit.Logger
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.database.Grocery
import com.plusmobileapps.chefmate.database.GroceryListMemberQueries
import com.plusmobileapps.chefmate.database.GroceryListQueries
import com.plusmobileapps.chefmate.database.GroceryQueries
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.grocery.data.CollaborationStatus
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryCategoryOverrideRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryListInvite
import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.grocery.data.IngredientParser
import com.plusmobileapps.chefmate.grocery.data.IngredientQuantityMerger
import com.plusmobileapps.chefmate.grocery.data.ListCollaborator
import com.plusmobileapps.chefmate.grocery.data.ListRole
import com.plusmobileapps.chefmate.grocery.data.SyncStatus
import com.plusmobileapps.chefmate.grocery.data.remote.GroceryRemoteDataSource
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryItem
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryList
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryListCollaborator
import com.plusmobileapps.chefmate.grocery.data.remote.RemoteGroceryListMember
import com.plusmobileapps.chefmate.util.DateTimeUtil
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.collections.map
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@OptIn(ExperimentalUuidApi::class)
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class GroceryRepositoryImpl(
    private val queries: GroceryQueries,
    private val listQueries: GroceryListQueries,
    private val memberQueries: GroceryListMemberQueries,
    @IO private val ioContext: CoroutineContext,
    private val dateTimeUtil: DateTimeUtil,
    private val remoteDataSource: GroceryRemoteDataSource,
    private val authRepository: AuthenticationRepository,
    private val categoryOverrideRepository: GroceryCategoryOverrideRepository,
) : GroceryRepository {

    private val scope = CoroutineScope(ioContext + SupervisorJob())
    private val syncMutex = Mutex()
    private val syncingIds = MutableStateFlow<Set<Long>>(emptySet())

    // Latest name→aisle rules, cached so the synchronous [getGrocery] read can apply them without
    // re-querying. The [getGroceries] flows below re-read this via [combine] so rule changes
    // re-emit.
    private val categoryOverrides = MutableStateFlow<Map<String, GroceryCategory>>(emptyMap())

    private var realtimeJob: Job? = null
    private var realtimeUserId: String? = null

    init {
        scope.launch {
            authRepository.state.collect { state ->
                if (state is AuthState.Authenticated) {
                    syncWithRemote(state.user.userId)
                    startRealtimeSync(state.user.userId)
                } else {
                    stopRealtimeSync()
                }
            }
        }
        scope.launch {
            categoryOverrideRepository.observeOverrideMap().collect { categoryOverrides.value = it }
        }
    }

    /**
     * Subscribes to remote grocery changes so edits made on another device reconcile into the local
     * cache without waiting for the next sign-in or manual sync. Emissions are debounced to
     * coalesce bursts (e.g. clearing a whole list), and each one re-runs the full [syncWithRemote]
     * reconcile. The realtime stream auto-reconnects after transient failures.
     */
    @OptIn(FlowPreview::class)
    private fun startRealtimeSync(userId: String) {
        if (realtimeUserId == userId && realtimeJob?.isActive == true) return
        stopRealtimeSync()
        realtimeUserId = userId
        realtimeJob = scope.launch {
            remoteDataSource
                .observeChanges()
                .debounce(REALTIME_DEBOUNCE_MS)
                .retry { cause ->
                    // Keep the subscription alive across transient failures, but let
                    // structured cancellation (sign-out / scope teardown) stop the loop.
                    if (cause is CancellationException) throw cause
                    delay(REALTIME_RETRY_DELAY_MS)
                    true
                }
                .catch {}
                .collect { syncWithRemote(userId) }
        }
    }

    private fun stopRealtimeSync() {
        realtimeJob?.cancel()
        realtimeJob = null
        realtimeUserId = null
    }

    override fun getGroceries(): Flow<List<GroceryItem>> =
        combine(
                queries.readAll().asFlow().map { it.executeAsList() },
                syncingIds,
                categoryOverrides,
            ) { items, syncing, overrides ->
                items.map { fromEntity(it, syncing, overrides) }
            }
            .flowOn(ioContext)

    override fun getGroceries(listId: Long): Flow<List<GroceryItem>> =
        combine(
                queries.readByListId(listId).asFlow().map { it.executeAsList() },
                syncingIds,
                categoryOverrides,
            ) { items, syncing, overrides ->
                items.map { fromEntity(it, syncing, overrides) }
            }
            .flowOn(ioContext)

    override fun getGroceryLists(): Flow<List<GroceryListModel>> =
        listQueries
            .getAll()
            .asFlow()
            .mapToList(ioContext)
            .map { query ->
                query.map { entity ->
                    GroceryListModel(
                        id = entity.id,
                        name = entity.name,
                        syncStatus =
                            when {
                                entity.isDirty -> SyncStatus.NOT_SYNCED
                                entity.remoteId != null -> SyncStatus.SYNCED
                                else -> SyncStatus.NOT_SYNCED
                            },
                        role = entity.role.toListRole(),
                        isShared = entity.isShared,
                    )
                }
            }
            .flowOn(ioContext)

    override suspend fun addGrocery(name: String) {
        val result =
            withContext(ioContext) {
                val defaultListId = ensureDefaultList()
                insertOrMergeGroceries(defaultListId, listOf(name), recipeName = null).first()
            }
        pushMergeResultToRemote(result)
    }

    override suspend fun addGrocery(listId: Long, name: String) {
        val result =
            withContext(ioContext) {
                insertOrMergeGroceries(listId, listOf(name), recipeName = null).first()
            }
        pushMergeResultToRemote(result)
    }

    override suspend fun addGroceries(names: List<String>) {
        val results =
            withContext(ioContext) {
                val defaultListId = ensureDefaultList()
                insertOrMergeGroceries(defaultListId, names, recipeName = null)
            }
        results.forEach { pushMergeResultToRemote(it) }
    }

    override suspend fun addGroceries(listId: Long, names: List<String>) {
        val results =
            withContext(ioContext) { insertOrMergeGroceries(listId, names, recipeName = null) }
        results.forEach { pushMergeResultToRemote(it) }
    }

    override suspend fun addGroceries(listId: Long, names: List<String>, recipeName: String?) {
        val results = withContext(ioContext) { insertOrMergeGroceries(listId, names, recipeName) }
        results.forEach { pushMergeResultToRemote(it) }
    }

    /**
     * Inserts each name as a new row unless an existing row in [listId] resolves to the same
     * [IngredientParser] name, in which case the two lines are combined via
     * [IngredientQuantityMerger] and the existing row is updated instead — this is what prevents
     * adding a recipe's ingredients from creating duplicate rows for something already on the list.
     * Matching is scoped to a single call's [names] plus what's already in the list, so two
     * matching lines within the same recipe also merge with each other.
     */
    private fun insertOrMergeGroceries(
        listId: Long,
        names: List<String>,
        recipeName: String?,
    ): List<MergeResult> {
        val now = dateTimeUtil.now.toString()
        return queries.transactionWithResult {
            val existingByKey =
                queries.readByListId(listId).executeAsList().associateByTo(mutableMapOf()) {
                    mergeKey(it.name)
                }
            names.map { name ->
                val key = mergeKey(name)
                val existing = existingByKey[key]
                if (existing == null) {
                    queries.create(
                        name = name,
                        isChecked = false,
                        createdAt = now,
                        updatedAt = now,
                        clientId = Uuid.random().toString(),
                        listId = listId,
                        recipeName = recipeName,
                    )
                    val newId = queries.lastId().executeAsOne().MAX!!
                    existingByKey[key] = queries.getGroceryById(newId).executeAsOne()
                    MergeResult(id = newId, isNew = true)
                } else {
                    val mergedName = IngredientQuantityMerger.merge(existing.name, name)
                    queries.update(
                        name = mergedName,
                        isChecked = false,
                        aisle = existing.aisle,
                        updatedAt = now,
                        id = existing.id,
                    )
                    existingByKey[key] =
                        existing.copy(name = mergedName, isChecked = false, updatedAt = now)
                    MergeResult(id = existing.id, isNew = false)
                }
            }
        }
    }

    private fun mergeKey(rawName: String): String =
        IngredientParser.parse(rawName).name.trim().lowercase()

    private data class MergeResult(val id: Long, val isNew: Boolean)

    private fun pushMergeResultToRemote(result: MergeResult) {
        if (result.isNew) pushAddToRemote(result.id) else pushUpdateToRemote(result.id)
    }

    override suspend fun updateChecked(item: GroceryItem, isChecked: Boolean) {
        withContext(ioContext) {
            queries.updateChecked(
                isChecked = isChecked,
                updatedAt = dateTimeUtil.now.toString(),
                id = item.id,
            )
        }
        pushUpdateToRemote(item.id)
    }

    override suspend fun deleteGrocery(item: GroceryItem) {
        val entity =
            withContext(ioContext) {
                val entity = queries.getGroceryById(item.id).executeAsOneOrNull()
                queries.delete(item.id)
                entity
            }
        entity?.remoteId?.let { remoteId ->
            scope.launch {
                try {
                    remoteDataSource.deleteGroceryItem(remoteId)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
                }
            }
        }
    }

    override suspend fun getGrocery(id: Long): GroceryItem? =
        withContext(ioContext) {
            queries.getGroceryById(id).executeAsOneOrNull()?.let {
                fromEntity(it, syncingIds.value, categoryOverrides.value)
            }
        }

    override suspend fun updateGrocery(item: GroceryItem) {
        withContext(ioContext) {
            queries.update(
                name = item.name,
                isChecked = item.isChecked,
                aisle = item.category.name,
                updatedAt = dateTimeUtil.now.toString(),
                id = item.id,
            )
        }
        pushUpdateToRemote(item.id)
    }

    override suspend fun syncAllUnsynced() {
        val authState = authRepository.state.value
        if (authState is AuthState.Authenticated) {
            syncWithRemote(authState.user.userId)
        }
    }

    override suspend fun createGroceryList(name: String): Long =
        withContext(ioContext) {
            val clientId = Uuid.random().toString()
            val id = listQueries.transactionWithResult {
                listQueries.create(name = name, clientId = clientId)
                listQueries.lastId().executeAsOne().MAX!!
            }
            pushListToRemote(id)
            id
        }

    override suspend fun deleteGroceryList(id: Long) {
        val entity =
            withContext(ioContext) {
                val entity = listQueries.getById(id).executeAsOneOrNull()
                listQueries.delete(id)
                entity
            }
        entity?.remoteId?.let { remoteId ->
            scope.launch {
                try {
                    remoteDataSource.deleteGroceryList(remoteId)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
                }
            }
        }
    }

    override suspend fun renameGroceryList(id: Long, name: String) {
        withContext(ioContext) {
            listQueries.update(name = name, updatedAt = dateTimeUtil.now.toString(), id = id)
        }
        pushListUpdateToRemote(id)
    }

    override suspend fun clearLocalData() {
        withContext(ioContext) {
            queries.deleteAll()
            listQueries.deleteAll()
        }
    }

    override suspend fun ensureDefaultList(): Long =
        withContext(ioContext) {
            val existing = listQueries.getAll().executeAsList()
            if (existing.isNotEmpty()) {
                existing.first().id
            } else {
                listQueries.transactionWithResult {
                    listQueries.create(
                        name = "My Grocery List",
                        clientId = Uuid.random().toString(),
                    )
                    listQueries.lastId().executeAsOne().MAX!!
                }
            }
        }

    private fun pushListToRemote(localId: Long) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        scope.launch {
            try {
                val entity = listQueries.getById(localId).executeAsOneOrNull() ?: return@launch
                val remoteList =
                    remoteDataSource.createGroceryList(
                        RemoteGroceryList(name = entity.name, ownerId = authState.user.userId)
                    )
                listQueries.updateRemoteId(remoteId = remoteList.id!!, id = localId)
                // Record ownership locally right away. Without this the local ownerId
                // stays NULL until a later pull, and syncListMembers would flag our own
                // freshly-pushed list as "shared with you".
                listQueries.updateOwnership(
                    ownerId = authState.user.userId,
                    role = "owner",
                    isShared = false,
                    id = localId,
                )
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
            }
        }
    }

    private fun pushListUpdateToRemote(localId: Long) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        scope.launch {
            try {
                val entity = listQueries.getById(localId).executeAsOneOrNull() ?: return@launch
                val remoteId = entity.remoteId ?: return@launch
                remoteDataSource.updateGroceryList(
                    RemoteGroceryList(
                        id = remoteId,
                        name = entity.name,
                        ownerId = authState.user.userId,
                    )
                )
                listQueries.clearDirty(localId)
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
            }
        }
    }

    private fun pushAddToRemote(id: Long) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        scope.launch {
            try {
                val match = queries.getGroceryById(id).executeAsOneOrNull() ?: return@launch
                if (match.remoteId != null) return@launch
                val clientId =
                    match.clientId
                        ?: Uuid.random().toString().also { newId ->
                            queries.updateClientId(clientId = newId, id = match.id)
                        }
                // Resolve the list's remoteId
                val listRemoteId =
                    match.listId?.let { localListId ->
                        listQueries.getById(localListId).executeAsOneOrNull()?.remoteId
                    } ?: remoteDataSource.ensureDefaultList(authState.user.userId)

                syncingIds.update { it + match.id }
                try {
                    val remoteItem =
                        remoteDataSource.upsertGroceryItem(
                            RemoteGroceryItem(
                                listId = listRemoteId,
                                name = match.name,
                                isChecked = match.isChecked,
                                createdAt = match.createdAt,
                                updatedAt = match.updatedAt,
                                clientId = clientId,
                                recipeName = match.recipeName,
                                aisle = match.aisle,
                            )
                        )
                    queries.updateRemoteId(
                        remoteId = remoteItem.id,
                        listRemoteId = listRemoteId,
                        id = match.id,
                    )
                } finally {
                    syncingIds.update { it - match.id }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
            }
        }
    }

    private fun pushUpdateToRemote(localId: Long) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        scope.launch {
            try {
                val entity = queries.getGroceryById(localId).executeAsOneOrNull() ?: return@launch
                val remoteId = entity.remoteId ?: return@launch
                val listId = entity.listRemoteId ?: return@launch
                syncingIds.update { it + localId }
                try {
                    remoteDataSource.updateGroceryItem(
                        RemoteGroceryItem(
                            id = remoteId,
                            listId = listId,
                            name = entity.name,
                            isChecked = entity.isChecked,
                            updatedAt = entity.updatedAt,
                            clientId = entity.clientId,
                            recipeName = entity.recipeName,
                            aisle = entity.aisle,
                        )
                    )
                    queries.clearDirty(localId)
                } finally {
                    syncingIds.update { it - localId }
                }
            } catch (t: Throwable) {
                Logger.e(throwable = t, tag = TAG) {
                    "pushUpdateToRemote failed (localId=$localId)"
                }
            }
        }
    }

    private suspend fun syncWithRemote(userId: String) = syncMutex.withLock {
        try {
            // --- Sync lists first ---

            // Pull all accessible lists FIRST (RLS handles filtering, includes shared lists).
            // Pulling before pushing lets us link a local default list to an existing remote
            // entry rather than pushing a duplicate when signing in on a new device.
            val remoteLists = remoteDataSource.fetchAccessibleGroceryLists()
            val remoteListIds = remoteLists.mapNotNull { it.id }.toSet()
            withContext(ioContext) {
                for (remoteList in remoteLists) {
                    val remoteId = remoteList.id ?: continue
                    val isOwned = remoteList.ownerId == userId
                    val role = if (isOwned) "owner" else "editor"
                    val isShared = !isOwned

                    val existing = listQueries.getByRemoteId(remoteId).executeAsOneOrNull()
                    if (existing != null) {
                        listQueries.updateOwnership(
                            ownerId = remoteList.ownerId,
                            role = role,
                            isShared = isShared,
                            id = existing.id,
                        )
                        continue
                    }

                    // Match a local unsynced list by name so we don't duplicate the local
                    // default list when signing in on a new device.
                    val localLists = listQueries.getAll().executeAsList()
                    val matchedByName = localLists.firstOrNull {
                        it.remoteId == null && it.name == remoteList.name
                    }
                    if (matchedByName != null) {
                        listQueries.updateRemoteId(remoteId = remoteId, id = matchedByName.id)
                        listQueries.updateOwnership(
                            ownerId = remoteList.ownerId,
                            role = role,
                            isShared = isShared,
                            id = matchedByName.id,
                        )
                    } else {
                        val newId = listQueries.transactionWithResult {
                            listQueries.create(name = remoteList.name, clientId = null)
                            listQueries.lastId().executeAsOne().MAX!!
                        }
                        listQueries.updateRemoteId(remoteId = remoteId, id = newId)
                        listQueries.updateOwnership(
                            ownerId = remoteList.ownerId,
                            role = role,
                            isShared = isShared,
                            id = newId,
                        )
                    }
                }

                // Prune local lists that were deleted on another device.
                // A local list with a remoteId that no longer appears in the
                // fetched remote lists has been deleted remotely — drop it and
                // its items locally so the deletion is reflected on this device.
                val pruneLists =
                    listQueries.getAll().executeAsList().filter {
                        it.remoteId != null && it.remoteId !in remoteListIds
                    }
                for (list in pruneLists) {
                    queries.deleteByListId(list.id)
                    listQueries.delete(list.id)
                }
            }

            // Push unsynced owned lists only (those that weren't linked above)
            val unsyncedLists = withContext(ioContext) { listQueries.getUnsynced().executeAsList() }
            for (list in unsyncedLists) {
                if (list.role != "owner") continue
                try {
                    val remoteList =
                        remoteDataSource.createGroceryList(
                            RemoteGroceryList(name = list.name, ownerId = userId)
                        )
                    withContext(ioContext) {
                        listQueries.updateRemoteId(remoteId = remoteList.id!!, id = list.id)
                        // Record ownership locally so this just-pushed list isn't later
                        // mistaken for a shared list when its local ownerId is still NULL.
                        listQueries.updateOwnership(
                            ownerId = userId,
                            role = "owner",
                            isShared = false,
                            id = list.id,
                        )
                    }
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
                }
            }

            // Push dirty owned lists only
            val dirtyLists = withContext(ioContext) { listQueries.getDirty().executeAsList() }
            for (list in dirtyLists) {
                if (list.role != "owner") continue
                try {
                    val remoteId = list.remoteId
                    remoteDataSource.updateGroceryList(
                        RemoteGroceryList(id = remoteId, name = list.name, ownerId = userId)
                    )
                    withContext(ioContext) { listQueries.clearDirty(list.id) }
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
                }
            }

            // Sync members for shared lists
            syncListMembers(userId)

            // --- Sync items per list ---
            val allLists = withContext(ioContext) { listQueries.getAll().executeAsList() }

            for (list in allLists) {
                val listRemoteId = list.remoteId ?: continue

                // Push unsynced items for this list
                val unsynced =
                    withContext(ioContext) {
                        queries.getUnsynced().executeAsList().filter { it.listId == list.id }
                    }
                for (item in unsynced) {
                    try {
                        val clientId =
                            item.clientId
                                ?: Uuid.random().toString().also { newId ->
                                    withContext(ioContext) {
                                        queries.updateClientId(clientId = newId, id = item.id)
                                    }
                                }
                        syncingIds.update { it + item.id }
                        try {
                            val remoteItem =
                                remoteDataSource.upsertGroceryItem(
                                    RemoteGroceryItem(
                                        listId = listRemoteId,
                                        name = item.name,
                                        isChecked = item.isChecked,
                                        createdAt = item.createdAt,
                                        updatedAt = item.updatedAt,
                                        clientId = clientId,
                                        recipeName = item.recipeName,
                                        aisle = item.aisle,
                                    )
                                )
                            withContext(ioContext) {
                                queries.updateRemoteId(
                                    remoteId = remoteItem.id,
                                    listRemoteId = listRemoteId,
                                    id = item.id,
                                )
                            }
                        } finally {
                            syncingIds.update { it - item.id }
                        }
                    } catch (t: Throwable) {
                        if (t is CancellationException) throw t
                        Logger.e(throwable = t, tag = TAG) {
                            "grocery remote sync operation failed"
                        }
                    }
                }

                // Push dirty items for this list
                val dirty =
                    withContext(ioContext) {
                        queries.getDirty().executeAsList().filter { it.listId == list.id }
                    }
                for (item in dirty) {
                    try {
                        val remoteId = item.remoteId
                        val itemListId = item.listRemoteId ?: listRemoteId
                        syncingIds.update { it + item.id }
                        try {
                            remoteDataSource.updateGroceryItem(
                                RemoteGroceryItem(
                                    id = remoteId,
                                    listId = itemListId,
                                    name = item.name,
                                    isChecked = item.isChecked,
                                    updatedAt = item.updatedAt,
                                    clientId = item.clientId,
                                    recipeName = item.recipeName,
                                    aisle = item.aisle,
                                )
                            )
                            withContext(ioContext) { queries.clearDirty(item.id) }
                        } finally {
                            syncingIds.update { it - item.id }
                        }
                    } catch (t: Throwable) {
                        Logger.e(throwable = t, tag = TAG) {
                            "syncWithRemote: push dirty item failed (localId=${item.id})"
                        }
                    }
                }

                // Pull remote items for this list
                val remoteItems = remoteDataSource.fetchAllGroceryItems(listRemoteId)
                val remoteItemIds = remoteItems.mapNotNull { it.id }.toSet()
                withContext(ioContext) {
                    for (remoteItem in remoteItems) {
                        val remoteId = remoteItem.id ?: continue
                        val existing = queries.getByRemoteId(remoteId).executeAsOneOrNull()
                        if (existing != null) {
                            // Item already known locally — reconcile remote edits (e.g. a
                            // "purchased" toggle made on another device) into the local row.
                            // Skip rows with unpushed local changes: their dirty edit is pushed
                            // earlier in this reconcile, and overwriting here would drop a change
                            // made concurrently while we were fetching.
                            if (!existing.isDirty) {
                                queries.updateFromRemote(
                                    name = remoteItem.name,
                                    isChecked = remoteItem.isChecked,
                                    updatedAt = remoteItem.updatedAt ?: existing.updatedAt,
                                    listRemoteId = listRemoteId,
                                    listId = list.id,
                                    recipeName = remoteItem.recipeName,
                                    aisle = remoteItem.aisle,
                                    id = existing.id,
                                )
                            }
                            continue
                        }

                        val matchedByClientId =
                            remoteItem.clientId?.let { clientId ->
                                queries.getByClientId(clientId).executeAsOneOrNull()
                            }
                        if (matchedByClientId != null) {
                            queries.updateRemoteId(
                                remoteId = remoteId,
                                listRemoteId = listRemoteId,
                                id = matchedByClientId.id,
                            )
                        } else {
                            queries.createWithRemoteId(
                                name = remoteItem.name,
                                isChecked = remoteItem.isChecked,
                                createdAt = remoteItem.createdAt ?: dateTimeUtil.now.toString(),
                                updatedAt = remoteItem.updatedAt ?: dateTimeUtil.now.toString(),
                                remoteId = remoteId,
                                listRemoteId = listRemoteId,
                                clientId = remoteItem.clientId,
                                listId = list.id,
                                recipeName = remoteItem.recipeName,
                                aisle = remoteItem.aisle,
                            )
                        }
                    }

                    // Prune local items that were deleted on another device.
                    // An item with a remoteId that no longer appears in the
                    // fetched remote items has been deleted remotely. Items
                    // without a remoteId (still pending initial push) are
                    // preserved.
                    val pruneItems =
                        queries.readByListId(list.id).executeAsList().filter {
                            it.remoteId != null && it.remoteId !in remoteItemIds
                        }
                    for (item in pruneItems) {
                        queries.delete(item.id)
                    }
                }
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
        }
    }

    override suspend fun deleteAllGroceries(listId: Long) {
        val remoteIds =
            withContext(ioContext) {
                val items = queries.readByListId(listId).executeAsList()
                queries.deleteByListId(listId)
                items.mapNotNull { it.remoteId }
            }
        deleteRemoteItems(remoteIds)
    }

    override suspend fun deletePurchasedGroceries(listId: Long) {
        val remoteIds =
            withContext(ioContext) {
                val items = queries.readCheckedByListId(listId).executeAsList()
                queries.deleteCheckedByListId(listId)
                items.mapNotNull { it.remoteId }
            }
        deleteRemoteItems(remoteIds)
    }

    private fun deleteRemoteItems(remoteIds: List<String>) {
        if (remoteIds.isEmpty()) return
        scope.launch {
            for (remoteId in remoteIds) {
                try {
                    remoteDataSource.deleteGroceryItem(remoteId)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
                }
            }
        }
    }

    override fun getListCollaborators(listId: Long): Flow<List<ListCollaborator>> =
        memberQueries
            .getByListId(listId)
            .asFlow()
            .mapToList(ioContext)
            .map { members ->
                members.map { member ->
                    ListCollaborator(
                        id = member.id,
                        email = member.userEmail,
                        displayName = member.displayName,
                        role = member.role.toListRole(),
                        status =
                            when (member.status) {
                                "accepted" -> CollaborationStatus.ACCEPTED
                                "rejected" -> CollaborationStatus.REJECTED
                                else -> CollaborationStatus.PENDING
                            },
                        avatarUrl = member.avatarUrl,
                    )
                }
            }
            .flowOn(ioContext)

    override suspend fun refreshListMembers(listId: Long) {
        val list =
            withContext(ioContext) { listQueries.getById(listId).executeAsOneOrNull() } ?: return
        val remoteListId = list.remoteId ?: return
        runCatching {
            val collaborators = fetchCollaboratorsForCache(remoteListId)
            cacheListCollaborators(listId = listId, collaborators = collaborators)
        }
    }

    override suspend fun inviteCollaborator(listId: Long, email: String, role: ListRole) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        val list =
            withContext(ioContext) { listQueries.getById(listId).executeAsOneOrNull() } ?: return
        val remoteListId = list.remoteId ?: return

        val remoteMember =
            remoteDataSource.inviteToList(
                RemoteGroceryListMember(
                    listId = remoteListId,
                    invitedEmail = email,
                    role = role.name.lowercase(),
                    invitedBy = authState.user.userId,
                )
            )
        withContext(ioContext) {
            memberQueries.insert(
                listLocalId = listId,
                remoteId = remoteMember.id,
                userId = remoteMember.userId,
                userEmail = email,
                role = role.name.lowercase(),
                status = "pending",
                displayName = null,
                avatarUrl = null,
            )
        }
    }

    override suspend fun removeCollaborator(listId: Long, collaboratorId: Long) {
        val member =
            withContext(ioContext) {
                memberQueries.getByListId(listId).executeAsList().firstOrNull {
                    it.id == collaboratorId
                }
            } ?: return
        member.remoteId?.let { remoteDataSource.removeFromList(it) }
        withContext(ioContext) { memberQueries.deleteById(collaboratorId) }
    }

    override suspend fun acceptInvitation(memberId: String) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        remoteDataSource.respondToInvitation(
            memberId = memberId,
            userId = authState.user.userId,
            accept = true,
        )
    }

    override suspend fun rejectInvitation(memberId: String) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        remoteDataSource.respondToInvitation(
            memberId = memberId,
            userId = authState.user.userId,
            accept = false,
        )
    }

    override fun getPendingInvitations(): Flow<List<GroceryListInvite>> =
        authRepository.state
            .map { state ->
                val email =
                    (state as? AuthState.Authenticated)?.user?.userEmail?.trim()?.lowercase()
                        ?: return@map emptyList()
                runCatching {
                        remoteDataSource.fetchPendingInvitations(email).map {
                            GroceryListInvite(
                                memberId = it.id,
                                listName = it.listName,
                                role = it.role.toListRole(),
                            )
                        }
                    }
                    .getOrDefault(emptyList())
            }
            .flowOn(ioContext)

    private suspend fun syncListMembers(userId: String) {
        val allLists = withContext(ioContext) { listQueries.getAll().executeAsList() }
        for (list in allLists) {
            val remoteListId = list.remoteId ?: continue
            try {
                val remoteMembers = remoteDataSource.fetchListMembers(remoteListId)
                cacheListCollaborators(
                    listId = list.id,
                    collaborators = fetchCollaboratorsForCache(remoteListId, remoteMembers),
                )
                withContext(ioContext) {
                    val myMember = remoteMembers.firstOrNull { it.userId == userId }
                    if (myMember != null) {
                        // Trust the member role rather than the local ownerId, which may
                        // still be NULL right after we pushed the list (see pushListToRemote).
                        // Deriving isShared from a NULL ownerId would flag our own list as
                        // "shared with you".
                        val isOwner = myMember.role == "owner"
                        listQueries.updateOwnership(
                            ownerId = if (isOwner) userId else list.ownerId,
                            role = myMember.role,
                            isShared = !isOwner,
                            id = list.id,
                        )
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                Logger.e(throwable = t, tag = TAG) { "grocery remote sync operation failed" }
            }
        }
    }

    private suspend fun fetchCollaboratorsForCache(
        remoteListId: String,
        remoteMembers: List<RemoteGroceryListMember>? = null,
    ): List<RemoteGroceryListCollaborator> {
        val collaborators = runCatching { remoteDataSource.fetchListCollaborators(remoteListId) }
        if (collaborators.isSuccess && collaborators.getOrThrow().isNotEmpty()) {
            return collaborators.getOrThrow()
        }

        // Fallback for environments that have the table policies but not the collaborator RPC yet.
        // It cannot synthesize the owner's email, but it keeps accepted/pending invited members
        // visible and prevents a refresh from wiping the local collaborator cache.
        val fallbackMembers = remoteMembers ?: remoteDataSource.fetchListMembers(remoteListId)
        return fallbackMembers.map { member ->
            RemoteGroceryListCollaborator(
                memberId = member.id,
                email = member.invitedEmail,
                name = null,
                role = member.role,
                status = member.status,
                isOwner = member.role == "owner",
                avatarUrl = null,
            )
        }
    }

    private suspend fun cacheListCollaborators(
        listId: Long,
        collaborators: List<RemoteGroceryListCollaborator>,
    ) {
        if (collaborators.isEmpty()) {
            return
        }
        withContext(ioContext) {
            memberQueries.transaction {
                memberQueries.deleteByListId(listId)
                for (collaborator in collaborators) {
                    collaborator.memberId?.let { remoteMemberId ->
                        memberQueries.getByRemoteId(remoteMemberId).executeAsOneOrNull()?.let {
                            staleMember ->
                            memberQueries.deleteById(staleMember.id)
                        }
                    }
                    memberQueries.insert(
                        listLocalId = listId,
                        remoteId = collaborator.memberId,
                        userId = null,
                        userEmail = collaborator.email,
                        role = collaborator.role,
                        status = collaborator.status,
                        displayName = collaborator.name,
                        avatarUrl = collaborator.avatarUrl,
                    )
                }
            }
        }
    }

    private fun String.toListRole(): ListRole =
        when (this) {
            "editor" -> ListRole.EDITOR
            "viewer" -> ListRole.VIEWER
            else -> ListRole.OWNER
        }

    private fun fromEntity(
        entity: Grocery,
        syncing: Set<Long>,
        overrides: Map<String, GroceryCategory>,
    ): GroceryItem {
        val syncStatus =
            when {
                entity.id in syncing -> SyncStatus.SYNCING
                entity.isDirty -> SyncStatus.NOT_SYNCED
                entity.remoteId != null -> SyncStatus.SYNCED
                else -> SyncStatus.NOT_SYNCED
            }
        val parsed = IngredientParser.parse(entity.name)
        val storedAisle =
            entity.aisle?.let { runCatching { GroceryCategory.valueOf(it) }.getOrNull() }
        // Resolution order: a per-item stored aisle wins, then the user's name→aisle rule, then the
        // parser's guess.
        val category = storedAisle ?: overrides[parsed.name.lowercase()] ?: parsed.category
        return GroceryItem(
            id = entity.id,
            name = entity.name,
            displayName = parsed.name,
            quantity = parsed.quantity,
            category = category,
            isChecked = entity.isChecked,
            syncStatus = syncStatus,
            recipeName = entity.recipeName,
        )
    }

    private companion object {
        const val TAG = "GroceryRepositoryImpl"
        const val REALTIME_DEBOUNCE_MS = 300L
        const val REALTIME_RETRY_DELAY_MS = 3_000L
    }
}

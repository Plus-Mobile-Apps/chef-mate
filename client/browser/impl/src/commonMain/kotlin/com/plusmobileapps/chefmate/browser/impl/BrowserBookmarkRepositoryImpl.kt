package com.plusmobileapps.chefmate.browser.impl

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.browser.BrowserBookmark
import com.plusmobileapps.chefmate.browser.BrowserBookmarkRepository
import com.plusmobileapps.chefmate.browser.impl.remote.BrowserBookmarkRemoteDataSource
import com.plusmobileapps.chefmate.browser.impl.remote.RemoteBrowserBookmark
import com.plusmobileapps.chefmate.database.BrowserBookmark as DbBookmark
import com.plusmobileapps.chefmate.database.BrowserBookmarkQueries
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.util.DateTimeUtil
import com.plusmobileapps.chefmate.util.Unique
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class BrowserBookmarkRepositoryImpl(
    private val queries: BrowserBookmarkQueries,
    @IO private val ioContext: CoroutineContext,
    private val unique: Unique,
    private val dateTimeUtil: DateTimeUtil,
    private val remoteDataSource: BrowserBookmarkRemoteDataSource,
    private val authRepository: AuthenticationRepository,
) : BrowserBookmarkRepository {

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

    override fun observeBookmarks(): Flow<List<BrowserBookmark>> =
        queries.getAll().asFlow().mapToList(ioContext).map { rows -> rows.map { it.toBookmark() } }

    override suspend fun addBookmark(url: String, title: String?) {
        val normalized = BookmarkUrl.normalize(url) ?: return
        val resolvedTitle =
            title?.trim()?.takeIf { it.isNotEmpty() } ?: BookmarkUrl.host(normalized)
        val ownerId = (authRepository.state.value as? AuthState.Authenticated)?.user?.userId
        // The unique url index + INSERT OR IGNORE dedups, so read the row back to learn whether
        // this was a fresh insert (worth pushing) or the page was already bookmarked.
        val row =
            withContext(ioContext) {
                queries.transactionWithResult {
                    queries.insertOrIgnore(
                        url = normalized,
                        title = resolvedTitle,
                        createdAt = dateTimeUtil.now.toString(),
                        clientId = unique.generate(),
                        ownerId = ownerId,
                    )
                    queries.getByUrl(normalized).executeAsOneOrNull()
                }
            }
        if (row != null && row.remoteId == null) pushAddToRemote(row.id)
    }

    override suspend fun removeBookmark(id: Long) {
        val entity =
            withContext(ioContext) {
                queries.transactionWithResult {
                    val entity = queries.getById(id).executeAsOneOrNull()
                    queries.deleteById(id)
                    entity
                }
            }
        entity?.remoteId?.let(::pushDeleteToRemote)
    }

    override suspend fun removeBookmarkByUrl(url: String) {
        val normalized = BookmarkUrl.normalize(url) ?: return
        val entity =
            withContext(ioContext) {
                queries.transactionWithResult {
                    val entity = queries.getByUrl(normalized).executeAsOneOrNull()
                    entity?.let { queries.deleteById(it.id) }
                    entity
                }
            }
        entity?.remoteId?.let(::pushDeleteToRemote)
    }

    override suspend fun clearLocalData() {
        withContext(ioContext) { queries.deleteAll() }
    }

    private fun pushDeleteToRemote(remoteId: String) {
        scope.launch {
            try {
                remoteDataSource.deleteBookmark(remoteId)
            } catch (_: Exception) {}
        }
    }

    private fun pushAddToRemote(localId: Long) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        scope.launch {
            try {
                val entity =
                    withContext(ioContext) { queries.getById(localId).executeAsOneOrNull() }
                        ?: return@launch
                if (entity.remoteId != null) return@launch
                pushBookmark(entity, authState.user.userId)
            } catch (_: Exception) {}
        }
    }

    private suspend fun pushBookmark(entity: DbBookmark, userId: String) {
        val clientId =
            entity.clientId
                ?: unique.generate().also { newId ->
                    withContext(ioContext) {
                        queries.updateClientId(clientId = newId, id = entity.id)
                    }
                }
        val remote =
            remoteDataSource.upsertBookmark(
                RemoteBrowserBookmark(
                    ownerId = userId,
                    url = entity.url,
                    title = entity.title,
                    clientId = clientId,
                    createdAt = entity.createdAt,
                )
            )
        remote.id?.let { remoteId ->
            withContext(ioContext) { queries.updateRemoteId(remoteId = remoteId, id = entity.id) }
        }
    }

    private suspend fun syncWithRemote(userId: String) = syncMutex.withLock {
        try {
            // Snapshot the prune candidates before this pass can assign any remote ids — a row
            // pushed below is absent from the remote list fetched afterwards only if the fetch
            // raced the push, and must not be pruned as "deleted elsewhere".
            val pruneCandidates =
                withContext(ioContext) { queries.getSyncedRemoteIds().executeAsList() }

            // Push unsynced bookmarks (no remoteId yet). Bookmarks are never edited, so there's no
            // dirty set to reconcile.
            val unsynced = withContext(ioContext) { queries.getUnsynced().executeAsList() }
            for (bookmark in unsynced) {
                try {
                    pushBookmark(bookmark, userId)
                } catch (_: Exception) {}
            }

            // Pull remote bookmarks not yet local.
            val remoteBookmarks = remoteDataSource.fetchAllBookmarks(userId)
            withContext(ioContext) {
                queries.transaction {
                    for (remote in remoteBookmarks) {
                        val remoteId = remote.id ?: continue
                        if (queries.getByRemoteId(remoteId).executeAsOneOrNull() != null) continue
                        // Adopt the remoteId onto an existing local row matched by clientId, or —
                        // since URLs are locally unique — by URL, so the same page starred on two
                        // devices doesn't collide with the unique-url index.
                        val matched =
                            remote.clientId?.let { queries.getByClientId(it).executeAsOneOrNull() }
                                ?: queries.getByUrl(remote.url).executeAsOneOrNull()
                        if (matched != null) {
                            if (matched.remoteId == null) {
                                queries.updateRemoteId(remoteId = remoteId, id = matched.id)
                            }
                        } else {
                            queries.createWithRemoteId(
                                url = remote.url,
                                title = remote.title,
                                createdAt = remote.createdAt ?: dateTimeUtil.now.toString(),
                                remoteId = remoteId,
                                clientId = remote.clientId,
                                ownerId = userId,
                            )
                        }
                    }

                    // Prune bookmarks the user deleted on another device. The fetch is already
                    // filtered to owner_id = userId and bookmarks are never shared, so anything
                    // synced that the remote no longer lists was deleted there. Rows without a
                    // remoteId are still pending their first push and are preserved.
                    val remoteIds = remoteBookmarks.mapNotNull { it.id }.toSet()
                    for (row in pruneCandidates) {
                        if (row.remoteId in remoteIds) continue
                        val current = queries.getById(row.id).executeAsOneOrNull() ?: continue
                        if (current.remoteId != row.remoteId) continue
                        queries.deleteById(row.id)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    private fun DbBookmark.toBookmark(): BrowserBookmark =
        BrowserBookmark(id = id, url = url, title = title)
}

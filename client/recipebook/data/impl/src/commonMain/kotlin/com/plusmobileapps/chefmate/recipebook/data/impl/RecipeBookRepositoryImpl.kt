package com.plusmobileapps.chefmate.recipebook.data.impl

import app.cash.sqldelight.coroutines.asFlow
import co.touchlab.kermit.Logger
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.database.RecipeBook as DbRecipeBook
import com.plusmobileapps.chefmate.database.RecipeBookQueries
import com.plusmobileapps.chefmate.database.RecipeBookRecipeQueries
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.recipe.data.SyncStatus
import com.plusmobileapps.chefmate.recipebook.data.RecipeBook
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRepository
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookSelection
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookSelectionRepository
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.FamilyRecipeBookRemoteDataSource
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RecipeBookMemberRemoteDataSource
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RecipeBookRemoteDataSource
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteRecipeBook
import com.plusmobileapps.chefmate.util.DateTimeUtil
import com.plusmobileapps.chefmate.util.Unique
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
class RecipeBookRepositoryImpl(
    private val db: RecipeBookQueries,
    private val bookRecipeDb: RecipeBookRecipeQueries,
    @IO private val ioContext: CoroutineContext,
    private val dateTimeUtil: DateTimeUtil,
    private val unique: Unique,
    private val remoteDataSource: RecipeBookRemoteDataSource,
    private val memberRemoteDataSource: RecipeBookMemberRemoteDataSource,
    private val familyRemoteDataSource: FamilyRecipeBookRemoteDataSource,
    private val authRepository: AuthenticationRepository,
    private val selectionRepository: RecipeBookSelectionRepository,
) : RecipeBookRepository {

    private val scope = CoroutineScope(ioContext + SupervisorJob())
    private val syncMutex = Mutex()
    private val _activeBookId = MutableStateFlow<Long?>(null)
    override val activeBookId: StateFlow<Long?> = _activeBookId.asStateFlow()

    init {
        scope.launch {
            val defaultId = ensureDefaultBookId()
            _activeBookId.value =
                when (val stored = selectionRepository.lastSelection) {
                    // The user explicitly picked the cross-book "All recipes" view.
                    RecipeBookSelection.AllRecipes -> null
                    is RecipeBookSelection.Book ->
                        if (bookExists(stored.id)) stored.id else defaultId
                    null -> defaultId
                }
        }
        scope.launch {
            authRepository.state.collect { state ->
                if (state is AuthState.Authenticated) {
                    syncWithRemote(state.user.userId)
                }
            }
        }
    }

    override fun getRecipeBooks(): Flow<List<RecipeBook>> =
        db.getAll()
            .asFlow()
            .map { query -> query.executeAsList().map { it.toRecipeBook() } }
            .flowOn(ioContext)

    override fun getRecipeBook(id: Long): Flow<RecipeBook?> =
        db.getById(id).asFlow().map { it.executeAsOneOrNull()?.toRecipeBook() }.flowOn(ioContext)

    override suspend fun getDefaultBookId(): Long = ensureDefaultBookId()

    override suspend fun setActiveBook(id: Long) {
        selectionRepository.select(RecipeBookSelection.Book(id))
        _activeBookId.value = id
    }

    override suspend fun selectAllRecipes() {
        selectionRepository.select(RecipeBookSelection.AllRecipes)
        _activeBookId.value = null
    }

    override suspend fun createBook(name: String): RecipeBook {
        val clientId = unique.generate()
        val book =
            withContext(ioContext) {
                db.transactionWithResult {
                    db.create(
                        name = name,
                        isDefault = false,
                        createdAt = dateTimeUtil.now.toString(),
                        updatedAt = dateTimeUtil.now.toString(),
                        clientId = clientId,
                        ownerId = null,
                    )
                    val id = db.lastInsertId().executeAsOne().MAX ?: error("No last insert id")
                    db.getById(id).executeAsOne().toRecipeBook()
                }
            }
        pushAddToRemote(book.id)
        return book
    }

    override suspend fun renameBook(id: Long, name: String): RecipeBook {
        val entity = withContext(ioContext) { db.getById(id).executeAsOne() }
        // A family book is renamed through Manage Family; a local rename would never push.
        if (entity.familyId != null) return entity.toRecipeBook()
        val book =
            withContext(ioContext) {
                db.transactionWithResult {
                    db.rename(name = name, updatedAt = dateTimeUtil.now.toString(), id = id)
                    db.getById(id).executeAsOne().toRecipeBook()
                }
            }
        pushUpdateToRemote(id)
        return book
    }

    override suspend fun deleteBook(id: Long) {
        val entity = withContext(ioContext) { db.getById(id).executeAsOneOrNull() } ?: return
        // "My Recipes" is the fallback book new recipes file under — it always has to exist. Family
        // books are deleted for the whole family through Manage Family.
        if (entity.isDefault || entity.familyId != null) return
        val remoteId = entity.remoteId

        if (remoteId == null) {
            // Never synced, so there's nothing on the server to tombstone for.
            withContext(ioContext) { db.delete(id) }
            resetActiveBookIfRemoved(id)
            return
        }

        withContext(ioContext) { db.markPendingDelete(id) }
        resetActiveBookIfRemoved(id)

        if (authRepository.state.value !is AuthState.Authenticated) return

        try {
            remoteDataSource.deleteRecipeBook(remoteId)
            withContext(ioContext) { db.delete(id) }
        } catch (t: Throwable) {
            // Leave the tombstone for syncWithRemote to retry the remote delete.
            Logger.e(throwable = t, tag = TAG) { "deleteBook failed remotely (localId=$id)" }
        }
    }

    override suspend fun removeLocalBook(id: Long) {
        withContext(ioContext) { db.delete(id) }
        resetActiveBookIfRemoved(id)
    }

    /** Falls the selection back to "My Recipes" when the book that just went away was active. */
    private suspend fun resetActiveBookIfRemoved(id: Long) {
        if (_activeBookId.value != id) return
        setActiveBook(ensureDefaultBookId())
    }

    override suspend fun syncAllUnsynced() {
        val authState = authRepository.state.value
        if (authState is AuthState.Authenticated) {
            syncWithRemote(authState.user.userId)
        }
    }

    override suspend fun clearLocalData() {
        withContext(ioContext) { db.deleteAll() }
        // Drop the persisted active-book selection — it points at a now-deleted row.
        selectionRepository.clear()
        // Recreate the baseline "My Recipes" book so a signed-out/fresh session stays valid.
        _activeBookId.value = ensureDefaultBookId()
    }

    /** Creates the default book if absent and returns its local id. Idempotent. */
    private suspend fun ensureDefaultBookId(): Long =
        withContext(ioContext) {
            db.getDefault().executeAsOneOrNull()?.id
                ?: db.transactionWithResult {
                    db.create(
                        name = DEFAULT_BOOK_NAME,
                        isDefault = true,
                        createdAt = dateTimeUtil.now.toString(),
                        updatedAt = dateTimeUtil.now.toString(),
                        clientId = unique.generate(),
                        ownerId = null,
                    )
                    db.lastInsertId().executeAsOne().MAX ?: error("No last insert id")
                }
        }

    private suspend fun bookExists(id: Long): Boolean =
        withContext(ioContext) { db.getById(id).executeAsOneOrNull() != null }

    private suspend fun pushAddToRemote(localId: Long) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        try {
            val entity =
                withContext(ioContext) { db.getById(localId).executeAsOneOrNull() } ?: return
            val clientId =
                entity.clientId
                    ?: unique.generate().also { newId ->
                        withContext(ioContext) { db.updateClientId(clientId = newId, id = localId) }
                    }
            val remote =
                remoteDataSource.upsertRecipeBook(
                    RemoteRecipeBook(
                        ownerId = authState.user.userId,
                        name = entity.name,
                        isDefault = entity.isDefault,
                        clientId = clientId,
                        createdAt = entity.createdAt,
                        updatedAt = entity.updatedAt,
                    )
                )
            withContext(ioContext) { db.updateRemoteId(remoteId = remote.id, id = localId) }
        } catch (t: Throwable) {
            Logger.e(throwable = t, tag = TAG) { "pushAddToRemote failed (localId=$localId)" }
        }
    }

    private suspend fun pushUpdateToRemote(localId: Long) {
        val authState = authRepository.state.value
        if (authState !is AuthState.Authenticated) return
        try {
            val entity =
                withContext(ioContext) { db.getById(localId).executeAsOneOrNull() } ?: return
            val remoteId = entity.remoteId ?: return
            remoteDataSource.upsertRecipeBook(
                RemoteRecipeBook(
                    id = remoteId,
                    ownerId = authState.user.userId,
                    name = entity.name,
                    isDefault = entity.isDefault,
                    clientId = entity.clientId,
                    updatedAt = entity.updatedAt,
                )
            )
            withContext(ioContext) { db.clearDirty(localId) }
        } catch (t: Throwable) {
            Logger.e(throwable = t, tag = TAG) { "pushUpdateToRemote failed (localId=$localId)" }
        }
    }

    private suspend fun syncWithRemote(userId: String) = syncMutex.withLock {
        try {
            // Retry remote deletes for any locally tombstoned books.
            val pendingDeletes = withContext(ioContext) { db.getPendingDeletes().executeAsList() }
            for (book in pendingDeletes) {
                val remoteId = book.remoteId
                try {
                    remoteDataSource.deleteRecipeBook(remoteId)
                    withContext(ioContext) { db.delete(book.id) }
                } catch (_: Exception) {}
            }

            // Push unsynced books (no remoteId yet).
            val unsynced = withContext(ioContext) { db.getUnsynced().executeAsList() }
            for (book in unsynced) {
                try {
                    val clientId =
                        book.clientId
                            ?: unique.generate().also { newId ->
                                withContext(ioContext) {
                                    db.updateClientId(clientId = newId, id = book.id)
                                }
                            }
                    val remote =
                        remoteDataSource.upsertRecipeBook(
                            RemoteRecipeBook(
                                ownerId = userId,
                                name = book.name,
                                isDefault = book.isDefault,
                                clientId = clientId,
                                createdAt = book.createdAt,
                                updatedAt = book.updatedAt,
                            )
                        )
                    withContext(ioContext) { db.updateRemoteId(remoteId = remote.id, id = book.id) }
                } catch (_: Exception) {}
            }

            // Push dirty books (already have a remoteId).
            val dirty = withContext(ioContext) { db.getDirty().executeAsList() }
            for (book in dirty) {
                try {
                    val remoteId = book.remoteId
                    remoteDataSource.upsertRecipeBook(
                        RemoteRecipeBook(
                            id = remoteId,
                            ownerId = userId,
                            name = book.name,
                            isDefault = book.isDefault,
                            clientId = book.clientId,
                            updatedAt = book.updatedAt,
                        )
                    )
                    withContext(ioContext) { db.clearDirty(book.id) }
                } catch (_: Exception) {}
            }

            // Pull every accessible book — owned plus books shared with the user (RLS-scoped).
            // RLS also lets a *pending* invitee read the invited book's row (so the banner can name
            // it), so exclude those here: a book the user hasn't accepted yet must not appear in
            // their list until they accept it from the recipe-list banner.
            val pendingBookIds: Set<String> =
                try {
                    val email =
                        (authRepository.state.value as? AuthState.Authenticated)
                            ?.user
                            ?.userEmail
                            ?.trim()
                            ?.lowercase()
                    if (email.isNullOrEmpty()) emptySet()
                    else
                        memberRemoteDataSource
                            .fetchPendingInvites(email)
                            .map { it.recipeBookId }
                            .toSet()
                } catch (_: Exception) {
                    emptySet()
                }
            val remoteBooks = remoteDataSource.fetchAccessibleRecipeBooks()
            val familyNames = fetchFamilyNames(remoteBooks.mapNotNull { it.familyId }.toSet())
            withContext(ioContext) {
                for (remote in remoteBooks) {
                    val remoteId = remote.id ?: continue
                    // Skip books the user only has a pending (un-accepted) invite to.
                    if (remote.ownerId != userId && remoteId in pendingBookIds) continue
                    val familyId = remote.familyId
                    val existing = db.getByRemoteId(remoteId).executeAsOneOrNull()
                    if (existing != null) {
                        // Already known locally — fold in a rename made on another device. Books
                        // with an unpushed local rename are skipped: their dirty push ran earlier
                        // in this pass, and overwriting here would drop a rename made while we
                        // were fetching. isDefault is deliberately left alone; it's a local
                        // sentinel for "My Recipes" that other code (e.g. the orphaned-recipe
                        // fallback in recipe sync) depends on exactly one book carrying.
                        if (!existing.isDirty && !existing.isPendingDelete) {
                            db.updateFromRemote(
                                name = remote.name,
                                ownerId = remote.ownerId,
                                updatedAt = remote.updatedAt ?: existing.updatedAt,
                                id = existing.id,
                            )
                        }
                        db.updateFamily(
                            familyId = familyId,
                            familyName = familyId?.let { familyNames[it] ?: existing.familyName },
                            id = existing.id,
                        )
                        continue
                    }

                    // Adopt the existing remote id onto the matching local book rather than
                    // inserting a duplicate. Match by clientId first, then — for your own default
                    // book — by the isDefault sentinel so two devices' "My Recipes" converge.
                    val clientId = remote.clientId
                    val matchedByClientId: DbRecipeBook? =
                        if (clientId != null) {
                            db.getByClientId(clientId).executeAsOneOrNull()
                        } else {
                            null
                        }
                    // A family book belongs to the family, not its creator: it's never anyone's
                    // default and is managed from Manage Family.
                    val isOwn = familyId == null && remote.ownerId == userId
                    val matched: DbRecipeBook? =
                        when {
                            matchedByClientId != null -> matchedByClientId
                            remote.isDefault && isOwn ->
                                db.getDefault().executeAsOneOrNull()?.takeIf { it.remoteId == null }
                            else -> null
                        }
                    if (matched != null) {
                        db.updateRemoteId(remoteId = remoteId, id = matched.id)
                    } else {
                        db.createWithRemoteId(
                            // A shared book is never the local "default"; that sentinel is reserved
                            // for the user's own My Recipes book.
                            name = remote.name,
                            isDefault = remote.isDefault && isOwn,
                            createdAt = remote.createdAt ?: dateTimeUtil.now.toString(),
                            updatedAt = remote.updatedAt ?: dateTimeUtil.now.toString(),
                            remoteId = remoteId,
                            clientId = remote.clientId,
                            ownerId = remote.ownerId,
                        )
                        db.getByRemoteId(remoteId).executeAsOneOrNull()?.let { created ->
                            db.updateFamily(
                                familyId = familyId,
                                familyName = familyId?.let { familyNames[it] },
                                id = created.id,
                            )
                        }
                    }
                }
            }

            pruneFamilyBooks(
                userId = userId,
                accessibleRemoteIds = remoteBooks.mapNotNull { it.id }.toSet(),
            )
        } catch (_: Exception) {}
    }

    /**
     * Drops family books the server no longer returns — the user left the family, was removed, or
     * the book was deleted — along with the other members' recipes that only reached the user
     * through them. The user's own recipes stay, refiled under "My Recipes". Only reachable when
     * the pull succeeded, since a failed fetch aborts the sync.
     */
    private suspend fun pruneFamilyBooks(userId: String, accessibleRemoteIds: Set<String>) {
        val removed =
            withContext(ioContext) {
                val gone =
                    db.getFamilyBooks().executeAsList().filter {
                        it.remoteId !in accessibleRemoteIds
                    }
                if (gone.isEmpty()) return@withContext emptyList()
                val defaultId = db.getDefault().executeAsOneOrNull()?.id
                db.transaction {
                    for (book in gone) {
                        // Foreign recipes first, so the refile below only moves the user's own.
                        bookRecipeDb.deleteForeignRecipesOnlyInBook(
                            recipeBookId = book.id,
                            userId = userId,
                        )
                        if (defaultId != null) {
                            bookRecipeDb.refileRecipesOnlyInBook(
                                targetBookId = defaultId,
                                recipeBookId = book.id,
                            )
                        }
                        db.delete(book.id)
                    }
                }
                gone.map { it.id }
            }
        removed.forEach { resetActiveBookIfRemoved(it) }
    }

    /**
     * Names for the families that own [familyIds], keyed by id. Empty on failure — callers keep the
     * last known name rather than failing the whole sync over a label.
     */
    private suspend fun fetchFamilyNames(familyIds: Set<String>): Map<String, String> {
        if (familyIds.isEmpty()) return emptyMap()
        return try {
            familyRemoteDataSource.fetchFamilyNames(familyIds).associate { it.id to it.name }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Logger.e(throwable = t, tag = TAG) { "fetching family names failed" }
            emptyMap()
        }
    }

    private fun DbRecipeBook.toRecipeBook(): RecipeBook {
        val syncStatus =
            when {
                isDirty -> SyncStatus.NOT_SYNCED
                remoteId != null -> SyncStatus.SYNCED
                else -> SyncStatus.NOT_SYNCED
            }
        return RecipeBook(
            id = id,
            name = name,
            isDefault = isDefault,
            // No ownerId yet (locally created, unsynced) means the creator owns it. Nobody owns a
            // family book, its creator included.
            isOwnedByCurrentUser =
                familyId == null && (ownerId == null || ownerId == currentUserId),
            syncStatus = syncStatus,
            createdAt = parseTimestamp(createdAt),
            updatedAt = parseTimestamp(updatedAt),
            familyId = familyId,
            familyName = familyName,
        )
    }

    /**
     * Books seeded by an upgrade migration's column default carry a SQLite datetime (`"2026-06-08
     * 22:11:24"` — space-separated, UTC, no offset), which [Instant.parse] rejects. Normalise that
     * shape to ISO-8601 before parsing so those rows don't crash on read.
     */
    private fun parseTimestamp(value: String): Instant =
        Instant.parse(if (value.contains('T')) value else "${value.replace(' ', 'T')}Z")

    private val currentUserId: String?
        get() = (authRepository.state.value as? AuthState.Authenticated)?.user?.userId

    private companion object {
        const val TAG = "RecipeBookRepositoryImpl"
        const val DEFAULT_BOOK_NAME = "My Recipes"
    }
}

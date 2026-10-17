package com.plusmobileapps.chefmate.recipe.data.impl

import app.cash.sqldelight.coroutines.asFlow
import co.touchlab.kermit.Logger
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.database.CategoryQueries
import com.plusmobileapps.chefmate.database.Recipe as DbRecipe
import com.plusmobileapps.chefmate.database.RecipeBookQueries
import com.plusmobileapps.chefmate.database.RecipeBookRecipeQueries
import com.plusmobileapps.chefmate.database.RecipeCategoryQueries
import com.plusmobileapps.chefmate.database.RecipeQueries
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.RecipePhotoStorage
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.recipe.data.SyncStatus
import com.plusmobileapps.chefmate.recipe.data.impl.remote.RecipeRemoteDataSource
import com.plusmobileapps.chefmate.recipe.data.impl.remote.RemoteRecipe
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRepository
import com.plusmobileapps.chefmate.util.DateTimeUtil
import com.plusmobileapps.chefmate.util.Unique
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@Inject
@ContributesBinding(AppScope::class)
class RecipeRepositoryImpl(
    private val db: RecipeQueries,
    private val joinDb: RecipeCategoryQueries,
    private val categoryDb: CategoryQueries,
    private val recipeBookDb: RecipeBookQueries,
    private val bookJoinDb: RecipeBookRecipeQueries,
    private val recipeBookRepository: RecipeBookRepository,
    @IO private val ioContext: CoroutineContext,
    private val dateTimeUtil: DateTimeUtil,
    private val unique: Unique,
    private val remoteDataSource: RecipeRemoteDataSource,
    private val authRepository: AuthenticationRepository,
    private val photoStorage: RecipePhotoStorage,
) : RecipeRepository {

    private val scope = CoroutineScope(ioContext + SupervisorJob())
    private val syncMutex = Mutex()
    private val syncingIds = MutableStateFlow<Set<Long>>(emptySet())

    init {
        scope.launch {
            authRepository.state.collect { state ->
                if (state is AuthState.Authenticated) {
                    syncWithRemote(state.user.userId)
                }
            }
        }
    }

    override fun getRecipes(): Flow<List<Recipe>> =
        combine(db.getAll().asFlow().map { it.executeAsList() }, syncingIds) { items, syncing ->
                items.map { it.toRecipe(syncing) }
            }
            .flowOn(ioContext)

    override fun getRecipes(presets: Set<BuiltinCategory>?): Flow<List<Recipe>> =
        if (presets.isNullOrEmpty()) {
            getRecipes()
        } else {
            getRecipes().map { recipes -> recipes.filter { it.matchesPresetFilter(presets) } }
        }

    override fun getRecipes(recipeBookId: Long): Flow<List<Recipe>> =
        combine(db.getAllForBook(recipeBookId).asFlow().map { it.executeAsList() }, syncingIds) {
                items,
                syncing ->
                items.map { it.toRecipe(syncing) }
            }
            .flowOn(ioContext)

    override suspend fun createRecipe(recipe: Recipe): Recipe {
        val clientId = unique.generate()
        val bookIds = resolveBookIds(recipe)
        val result =
            withContext(ioContext) {
                db.transactionWithResult {
                    db.create(
                        title = recipe.title,
                        description = recipe.description,
                        ingredients = recipe.ingredients,
                        directions = recipe.directions,
                        imageUrl = recipe.imageUrl,
                        sourceUrl = recipe.sourceUrl,
                        servings = recipe.servings?.toLong(),
                        prepTime = recipe.prepTime?.toLong(),
                        cookTime = recipe.cookTime?.toLong(),
                        totalTime = recipe.totalTime?.toLong(),
                        calories = recipe.calories?.toLong(),
                        starRating = recipe.starRating?.toLong(),
                        isFavorite = recipe.isFavorite,
                        createdAt = dateTimeUtil.now.toString(),
                        updatedAt = dateTimeUtil.now.toString(),
                        clientId = clientId,
                        ownerId = null,
                    )
                    val id =
                        db.lastInsertId().executeAsOne().MAX
                            ?: error("Failed to get last insert id")
                    for (category in recipe.categories) {
                        joinDb.attach(recipeId = id, categoryId = category.id)
                    }
                    for (bookId in bookIds) {
                        bookJoinDb.attach(recipeBookId = bookId, recipeId = id)
                    }
                    db.getById(id).executeAsOne().toRecipe()
                }
            }
        pushAddToRemote(result.id)
        return result
    }

    override suspend fun updateRecipe(recipe: Recipe): Recipe {
        val (result, previousImageUrl) =
            withContext(ioContext) {
                val now = dateTimeUtil.now
                val previous = db.getById(recipe.id).executeAsOneOrNull()
                val updated = db.transactionWithResult {
                    db.update(
                        id = recipe.id,
                        title = recipe.title,
                        description = recipe.description,
                        ingredients = recipe.ingredients,
                        directions = recipe.directions,
                        imageUrl = recipe.imageUrl,
                        sourceUrl = recipe.sourceUrl,
                        servings = recipe.servings?.toLong(),
                        prepTime = recipe.prepTime?.toLong(),
                        cookTime = recipe.cookTime?.toLong(),
                        totalTime = recipe.totalTime?.toLong(),
                        calories = recipe.calories?.toLong(),
                        starRating = recipe.starRating?.toLong(),
                        isFavorite = recipe.isFavorite,
                        updatedAt = now.toString(),
                    )
                    syncJoinRowsForRecipe(recipe.id, recipe.categories)
                    syncBookJoinRowsForRecipe(recipe.id, recipe.recipeBookIds)
                    recipe.copy(updatedAt = now)
                }
                updated to previous?.imageUrl
            }
        if (!previousImageUrl.isNullOrBlank() && previousImageUrl != recipe.imageUrl) {
            scope.launch { photoStorage.deletePhoto(previousImageUrl) }
        }
        pushUpdateToRemote(recipe.id)
        return result
    }

    override suspend fun getRecipe(id: Long): Flow<Recipe?> =
        db.getById(id)
            .asFlow()
            .map { it.executeAsOneOrNull() }
            .map { row -> row?.takeUnless { it.isPendingDelete }?.toRecipe() }
            .flowOn(ioContext)

    override suspend fun getRecipeByRemoteId(remoteId: String): Recipe? =
        withContext(ioContext) {
            db.getByRemoteId(remoteId)
                .executeAsOneOrNull()
                ?.takeUnless { it.isPendingDelete }
                ?.toRecipe()
        }

    override suspend fun getRecipeByClientId(clientId: String): Recipe? =
        withContext(ioContext) {
            db.getByClientId(clientId)
                .executeAsOneOrNull()
                ?.takeUnless { it.isPendingDelete }
                ?.toRecipe()
        }

    override suspend fun fetchPublicRecipe(remoteId: String): Result<Recipe> = runCatching {
        val remote =
            remoteDataSource.fetchPublicRecipe(remoteId)
                ?: throw NoSuchElementException("No public recipe with id $remoteId")
        remote.toPublicPreview()
    }

    override suspend fun setRecipePublic(id: Long, isPublic: Boolean): String? {
        // Publishing requires a remote row (the share link embeds the remote id), which requires
        // auth. Bail early so the caller can prompt the user to sign in.
        if (authRepository.state.value !is AuthState.Authenticated) return null
        withContext(ioContext) {
            db.setPublic(isPublic = isPublic, updatedAt = dateTimeUtil.now.toString(), id = id)
        }
        val existingRemoteId =
            withContext(ioContext) { db.getById(id).executeAsOneOrNull()?.remoteId }
        // Create the remote row on first publish, otherwise push the flag change.
        if (existingRemoteId == null) pushAddToRemote(id) else pushUpdateToRemote(id)
        return withContext(ioContext) { db.getById(id).executeAsOneOrNull()?.remoteId }
    }

    override suspend fun addRecipesToBook(recipeIds: Set<Long>, bookId: Long) {
        if (recipeIds.isEmpty()) return
        withContext(ioContext) {
            db.transaction {
                val now = dateTimeUtil.now.toString()
                for (recipeId in recipeIds) {
                    // attach is INSERT OR IGNORE, so a recipe already in the book is untouched.
                    bookJoinDb.attach(recipeBookId = bookId, recipeId = recipeId)
                    db.markDirty(id = recipeId, updatedAt = now)
                }
            }
        }
        // Push each in the background so the bulk action returns as soon as the local write lands;
        // the dirty flag guarantees a retry on the next full sync if a push fails.
        for (recipeId in recipeIds) scope.launch { pushUpdateToRemote(recipeId) }
    }

    override suspend fun addRecipesToCategory(recipeIds: Set<Long>, category: Category) {
        if (recipeIds.isEmpty()) return
        withContext(ioContext) {
            db.transaction {
                val now = dateTimeUtil.now.toString()
                for (recipeId in recipeIds) {
                    joinDb.attach(recipeId = recipeId, categoryId = category.id)
                    db.markDirty(id = recipeId, updatedAt = now)
                }
            }
        }
        for (recipeId in recipeIds) scope.launch { pushUpdateToRemote(recipeId) }
    }

    override suspend fun deleteRecipe(id: Long) {
        val entity = withContext(ioContext) { db.getById(id).executeAsOneOrNull() } ?: return
        val remoteId = entity.remoteId
        val imageUrl = entity.imageUrl

        if (remoteId == null) {
            withContext(ioContext) { db.delete(id) }
            if (!imageUrl.isNullOrBlank()) {
                scope.launch { photoStorage.deletePhoto(imageUrl) }
            }
            return
        }

        withContext(ioContext) { db.markPendingDelete(id) }

        if (authRepository.state.value !is AuthState.Authenticated) return

        try {
            remoteDataSource.deleteRecipe(remoteId)
            withContext(ioContext) { db.delete(id) }
        } catch (_: Exception) {
            // Leave the tombstone for pushPendingDeletes to retry on the next sync.
        }
    }

    override suspend fun deleteLocalRecipesInBook(recipeBookId: Long) {
        withContext(ioContext) {
            db.transaction {
                for (recipeId in bookJoinDb.getRecipeIdsForBook(recipeBookId).executeAsList()) {
                    val books = bookJoinDb.getBookIdsForRecipe(recipeId).executeAsList()
                    if (books.size <= 1) {
                        // The join row goes with it via ON DELETE CASCADE. No remote delete and no
                        // tombstone: the recipe still exists for whoever owns the book.
                        db.delete(recipeId)
                    } else {
                        bookJoinDb.detach(recipeBookId = recipeBookId, recipeId = recipeId)
                    }
                }
            }
        }
    }

    override suspend fun clearLocalData() {
        withContext(ioContext) { db.deleteAll() }
    }

    override suspend fun syncAllUnsynced() {
        val authState = authRepository.state.value
        if (authState is AuthState.Authenticated) {
            syncWithRemote(authState.user.userId)
        }
    }

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
            syncingIds.update { it + localId }
            try {
                val remoteRecipe =
                    remoteDataSource.upsertRecipe(
                        RemoteRecipe(
                            ownerId = authState.user.userId,
                            title = entity.title,
                            description = entity.description,
                            ingredients = entity.ingredients,
                            directions = entity.directions,
                            imageUrl = entity.imageUrl,
                            sourceUrl = entity.sourceUrl,
                            servings = entity.servings?.toInt(),
                            prepTime = entity.prepTime?.toInt(),
                            cookTime = entity.cookTime?.toInt(),
                            totalTime = entity.totalTime?.toInt(),
                            calories = entity.calories?.toInt(),
                            starRating = entity.starRating?.toInt(),
                            isFavorite = entity.isFavorite,
                            isPublic = entity.isPublic,
                            createdAt = entity.createdAt,
                            updatedAt = entity.updatedAt,
                            clientId = clientId,
                        )
                    )
                withContext(ioContext) {
                    db.updateRemoteId(remoteId = remoteRecipe.id, id = localId)
                }
                remoteRecipe.id?.let { recipeRemoteId ->
                    remoteDataSource.setRecipeCategories(
                        recipeRemoteId,
                        attachedCategoryRemoteIds(localId),
                    )
                    remoteDataSource.setRecipeBooks(recipeRemoteId, attachedBookRemoteIds(localId))
                }
            } finally {
                syncingIds.update { it - localId }
            }
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
            syncingIds.update { it + localId }
            try {
                remoteDataSource.upsertRecipe(
                    RemoteRecipe(
                        id = remoteId,
                        ownerId = authState.user.userId,
                        title = entity.title,
                        description = entity.description,
                        ingredients = entity.ingredients,
                        directions = entity.directions,
                        imageUrl = entity.imageUrl,
                        sourceUrl = entity.sourceUrl,
                        servings = entity.servings?.toInt(),
                        prepTime = entity.prepTime?.toInt(),
                        cookTime = entity.cookTime?.toInt(),
                        totalTime = entity.totalTime?.toInt(),
                        calories = entity.calories?.toInt(),
                        starRating = entity.starRating?.toInt(),
                        isFavorite = entity.isFavorite,
                        isPublic = entity.isPublic,
                        updatedAt = entity.updatedAt,
                        clientId = entity.clientId,
                    )
                )
                remoteDataSource.setRecipeCategories(remoteId, attachedCategoryRemoteIds(localId))
                remoteDataSource.setRecipeBooks(remoteId, attachedBookRemoteIds(localId))
                withContext(ioContext) { db.clearDirty(localId) }
            } finally {
                syncingIds.update { it - localId }
            }
        } catch (t: Throwable) {
            Logger.e(throwable = t, tag = TAG) { "pushUpdateToRemote failed (localId=$localId)" }
        }
    }

    private suspend fun syncWithRemote(userId: String) = syncMutex.withLock {
        try {
            // Retry remote deletes for any locally tombstoned recipes. Each is independent — a
            // failure on one leaves the tombstone in place and continues with the rest of sync.
            val pendingDeletes = withContext(ioContext) { db.getPendingDeletes().executeAsList() }
            for (recipe in pendingDeletes) {
                val remoteId = recipe.remoteId
                try {
                    remoteDataSource.deleteRecipe(remoteId)
                    withContext(ioContext) { db.delete(recipe.id) }
                } catch (_: Exception) {}
            }

            // Snapshot the prune candidates before this pass can assign any remote ids, so a
            // recipe pushed (or created on this device) during the sync can never be mistaken for
            // one deleted elsewhere: it either isn't in this list, or it still has no remoteId.
            val pruneCandidates =
                withContext(ioContext) { db.getOwnedSyncedRemoteIds(userId).executeAsList() }

            // Push unsynced recipes (no remoteId yet)
            val unsynced = withContext(ioContext) { db.getUnsynced().executeAsList() }
            for (recipe in unsynced) {
                try {
                    val clientId =
                        recipe.clientId
                            ?: unique.generate().also { newId ->
                                withContext(ioContext) {
                                    db.updateClientId(clientId = newId, id = recipe.id)
                                }
                            }
                    val remoteRecipe =
                        remoteDataSource.upsertRecipe(
                            RemoteRecipe(
                                ownerId = userId,
                                title = recipe.title,
                                description = recipe.description,
                                ingredients = recipe.ingredients,
                                directions = recipe.directions,
                                imageUrl = recipe.imageUrl,
                                sourceUrl = recipe.sourceUrl,
                                servings = recipe.servings?.toInt(),
                                prepTime = recipe.prepTime?.toInt(),
                                cookTime = recipe.cookTime?.toInt(),
                                totalTime = recipe.totalTime?.toInt(),
                                calories = recipe.calories?.toInt(),
                                starRating = recipe.starRating?.toInt(),
                                isFavorite = recipe.isFavorite,
                                isPublic = recipe.isPublic,
                                createdAt = recipe.createdAt,
                                updatedAt = recipe.updatedAt,
                                clientId = clientId,
                            )
                        )
                    withContext(ioContext) {
                        db.updateRemoteId(remoteId = remoteRecipe.id, id = recipe.id)
                    }
                    remoteRecipe.id?.let { recipeRemoteId ->
                        remoteDataSource.setRecipeCategories(
                            recipeRemoteId,
                            attachedCategoryRemoteIds(recipe.id),
                        )
                        remoteDataSource.setRecipeBooks(
                            recipeRemoteId,
                            attachedBookRemoteIds(recipe.id),
                        )
                    }
                } catch (_: Exception) {}
            }

            // Push dirty recipes (modified locally, already have remoteId)
            val dirty = withContext(ioContext) { db.getDirty().executeAsList() }
            for (recipe in dirty) {
                try {
                    val remoteId = recipe.remoteId
                    remoteDataSource.upsertRecipe(
                        RemoteRecipe(
                            id = remoteId,
                            ownerId = userId,
                            title = recipe.title,
                            description = recipe.description,
                            ingredients = recipe.ingredients,
                            directions = recipe.directions,
                            imageUrl = recipe.imageUrl,
                            sourceUrl = recipe.sourceUrl,
                            servings = recipe.servings?.toInt(),
                            prepTime = recipe.prepTime?.toInt(),
                            cookTime = recipe.cookTime?.toInt(),
                            totalTime = recipe.totalTime?.toInt(),
                            calories = recipe.calories?.toInt(),
                            starRating = recipe.starRating?.toInt(),
                            isFavorite = recipe.isFavorite,
                            isPublic = recipe.isPublic,
                            updatedAt = recipe.updatedAt,
                            clientId = recipe.clientId,
                        )
                    )
                    remoteDataSource.setRecipeCategories(
                        remoteId,
                        attachedCategoryRemoteIds(recipe.id),
                    )
                    remoteDataSource.setRecipeBooks(remoteId, attachedBookRemoteIds(recipe.id))
                    withContext(ioContext) { db.clearDirty(recipe.id) }
                } catch (_: Exception) {}
            }

            // Pull every accessible recipe — owned plus recipes in books shared with the user.
            val remoteRecipes = remoteDataSource.fetchAccessibleRecipes()
            withContext(ioContext) {
                for (remote in remoteRecipes) {
                    val remoteId = remote.id ?: continue
                    val existing = db.getByRemoteId(remoteId).executeAsOneOrNull()
                    if (existing != null) {
                        // Already known locally — fold in edits made on another device. Rows with
                        // unpushed local changes are skipped: their dirty push ran earlier in this
                        // pass, and overwriting here would drop an edit made while we were
                        // fetching. Tombstoned rows keep their tombstone until the delete lands.
                        if (!existing.isDirty && !existing.isPendingDelete) {
                            db.updateFromRemote(
                                title = remote.title,
                                description = remote.description,
                                ingredients = remote.ingredients,
                                directions = remote.directions,
                                imageUrl = remote.imageUrl,
                                sourceUrl = remote.sourceUrl,
                                servings = remote.servings?.toLong(),
                                prepTime = remote.prepTime?.toLong(),
                                cookTime = remote.cookTime?.toLong(),
                                totalTime = remote.totalTime?.toLong(),
                                calories = remote.calories?.toLong(),
                                starRating = remote.starRating?.toLong(),
                                isFavorite = remote.isFavorite,
                                isPublic = remote.isPublic,
                                ownerId = remote.ownerId.ifBlank { userId },
                                updatedAt = remote.updatedAt ?: existing.updatedAt,
                                id = existing.id,
                            )
                        }
                        continue
                    }

                    val matchedByClientId =
                        remote.clientId?.let { clientId ->
                            db.getByClientId(clientId).executeAsOneOrNull()
                        }
                    if (matchedByClientId != null) {
                        db.updateRemoteId(remoteId = remoteId, id = matchedByClientId.id)
                    } else {
                        db.createWithRemoteId(
                            title = remote.title,
                            description = remote.description,
                            ingredients = remote.ingredients,
                            directions = remote.directions,
                            imageUrl = remote.imageUrl,
                            sourceUrl = remote.sourceUrl,
                            servings = remote.servings?.toLong(),
                            prepTime = remote.prepTime?.toLong(),
                            cookTime = remote.cookTime?.toLong(),
                            totalTime = remote.totalTime?.toLong(),
                            calories = remote.calories?.toLong(),
                            starRating = remote.starRating?.toLong(),
                            isFavorite = remote.isFavorite,
                            createdAt = remote.createdAt ?: dateTimeUtil.now.toString(),
                            updatedAt = remote.updatedAt ?: dateTimeUtil.now.toString(),
                            remoteId = remoteId,
                            clientId = remote.clientId,
                            // Preserve the real owner so shared recipes aren't mistaken for the
                            // current user's own; defensively fall back to the syncing user.
                            ownerId = remote.ownerId.ifBlank { userId },
                            isPublic = remote.isPublic,
                        )
                    }
                }
            }

            // Prune recipes the user deleted on another device. Scoped to rows the user owns
            // (or created here and hasn't re-pulled): a recipe shared through a book always
            // carries the sharer's ownerId, so losing access to a book can never delete a local
            // copy here — deleteLocalRecipesInBook handles that case explicitly. Only reachable
            // when fetchAccessibleRecipes succeeded, since a failure aborts the whole sync.
            withContext(ioContext) {
                val remoteIds = remoteRecipes.mapNotNull { it.id }.toSet()
                for (row in pruneCandidates) {
                    if (row.remoteId in remoteIds) continue
                    // Re-read: the user may have edited or deleted the recipe while we were
                    // fetching, which makes it no longer ours to drop.
                    val current = db.getById(row.id).executeAsOneOrNull() ?: continue
                    if (current.isDirty || current.isPendingDelete) continue
                    if (current.remoteId != row.remoteId) continue
                    db.delete(row.id)
                }
            }

            // Pull remote category attachments and rebuild local join rows. Requires both the
            // recipe and its categories to be present locally — if either is missing (e.g. category
            // sync hasn't run yet), the row is skipped and picked up on the next sync.
            val categoryAttachments = remoteDataSource.fetchRecipeCategoryAttachments()
            withContext(ioContext) {
                for ((recipeRemoteId, categoryRemoteIds) in categoryAttachments) {
                    val recipeLocalId =
                        db.getByRemoteId(recipeRemoteId).executeAsOneOrNull()?.id ?: continue
                    val desiredLocalIds =
                        categoryRemoteIds
                            .mapNotNull { categoryDb.getByRemoteId(it).executeAsOneOrNull()?.id }
                            .toSet()
                    val current =
                        joinDb
                            .getCategoriesForRecipe(recipeLocalId)
                            .executeAsList()
                            .map { it.id }
                            .toSet()
                    for (toAttach in desiredLocalIds - current) {
                        joinDb.attach(recipeId = recipeLocalId, categoryId = toAttach)
                    }
                    for (toDetach in current - desiredLocalIds) {
                        joinDb.detach(recipeId = recipeLocalId, categoryId = toDetach)
                    }
                }
            }

            // Pull remote book attachments and rebuild the local many-to-many join rows.
            val bookAttachments = remoteDataSource.fetchRecipeBookAttachments()
            withContext(ioContext) {
                for ((recipeRemoteId, bookRemoteIds) in bookAttachments) {
                    val recipeLocalId =
                        db.getByRemoteId(recipeRemoteId).executeAsOneOrNull()?.id ?: continue
                    val desiredBookIds =
                        bookRemoteIds
                            .mapNotNull { recipeBookDb.getByRemoteId(it).executeAsOneOrNull()?.id }
                            .toSet()
                    val current =
                        bookJoinDb.getBookIdsForRecipe(recipeLocalId).executeAsList().toSet()
                    for (toAttach in desiredBookIds - current) {
                        bookJoinDb.attach(recipeBookId = toAttach, recipeId = recipeLocalId)
                    }
                    for (toDetach in current - desiredBookIds) {
                        bookJoinDb.detach(recipeBookId = toDetach, recipeId = recipeLocalId)
                    }
                }

                // A recipe must live in at least one book. Any that ended up orphaned (older server
                // rows, or a book whose own sync hasn't landed yet) is filed under the default book
                // so it stays visible in the list.
                recipeBookDb.getDefault().executeAsOneOrNull()?.let { defaultBook ->
                    for (recipeId in db.getRecipeIdsWithoutBook().executeAsList()) {
                        bookJoinDb.attach(recipeBookId = defaultBook.id, recipeId = recipeId)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * The book ids a new recipe should be filed under: the recipe's own set when non-empty,
     * otherwise the active book (falling back to the default) so every recipe lands in at least one
     * book.
     */
    private suspend fun resolveBookIds(recipe: Recipe): Set<Long> {
        if (recipe.recipeBookIds.isNotEmpty()) return recipe.recipeBookIds
        return withContext(ioContext) {
            val fallback =
                recipeBookRepository.activeBookId.value
                    ?: recipeBookDb.getDefault().executeAsOneOrNull()?.id
            setOfNotNull(fallback)
        }
    }

    /**
     * Looks up the remote IDs of the books currently attached to [recipeLocalId]. Books that
     * haven't been remote-synced yet (no [remoteId]) are skipped — they're picked up on a future
     * sync once their own push completes, mirroring how attached category remote ids are resolved.
     */
    private suspend fun attachedBookRemoteIds(recipeLocalId: Long): Set<String> =
        withContext(ioContext) {
            bookJoinDb
                .getBookIdsForRecipe(recipeLocalId)
                .executeAsList()
                .mapNotNull { recipeBookDb.getById(it).executeAsOneOrNull()?.remoteId }
                .toSet()
        }

    /**
     * Looks up the remote IDs of the categories currently attached to [recipeLocalId]. Categories
     * that haven't been remote-synced yet (no [remoteId]) are skipped — they'll be picked up on a
     * future sync once their own push completes.
     */
    private suspend fun attachedCategoryRemoteIds(recipeLocalId: Long): Set<String> =
        withContext(ioContext) {
            joinDb
                .getCategoriesForRecipe(recipeLocalId)
                .executeAsList()
                .mapNotNull { categoryDb.getById(it.id).executeAsOneOrNull()?.remoteId }
                .toSet()
        }

    /**
     * Diffs the desired [desired] category set against the current join rows for [recipeId] and
     * applies the minimum number of attach/detach operations. Must be called inside a SQLDelight
     * transaction so the diff can't see a partial mutation.
     */
    private fun syncJoinRowsForRecipe(recipeId: Long, desired: Set<Category>) {
        val current = joinDb.getCategoriesForRecipe(recipeId).executeAsList().map { it.id }.toSet()
        val want = desired.map { it.id }.toSet()
        for (toAttach in want - current) {
            joinDb.attach(recipeId = recipeId, categoryId = toAttach)
        }
        for (toDetach in current - want) {
            joinDb.detach(recipeId = recipeId, categoryId = toDetach)
        }
    }

    /**
     * Diffs the desired book-membership [want] against the current join rows for [recipeId] and
     * applies the minimum number of attach/detach operations. Must be called inside a SQLDelight
     * transaction so the diff can't see a partial mutation.
     */
    private fun syncBookJoinRowsForRecipe(recipeId: Long, want: Set<Long>) {
        val current = bookJoinDb.getBookIdsForRecipe(recipeId).executeAsList().toSet()
        for (toAttach in want - current) {
            bookJoinDb.attach(recipeBookId = toAttach, recipeId = recipeId)
        }
        for (toDetach in current - want) {
            bookJoinDb.detach(recipeBookId = toDetach, recipeId = recipeId)
        }
    }

    private fun DbRecipe.toRecipe(syncing: Set<Long> = emptySet()): Recipe {
        val syncStatus =
            when {
                id in syncing -> SyncStatus.SYNCING
                isDirty -> SyncStatus.NOT_SYNCED
                remoteId != null -> SyncStatus.SYNCED
                else -> SyncStatus.NOT_SYNCED
            }
        val attachedCategories =
            joinDb.getCategoriesForRecipe(id).executeAsList().map {
                Category(
                    id = it.id,
                    name = it.name,
                    builtinId = it.builtinId,
                    syncStatus =
                        when {
                            it.isDirty -> SyncStatus.NOT_SYNCED
                            it.remoteId != null -> SyncStatus.SYNCED
                            else -> SyncStatus.NOT_SYNCED
                        },
                )
            }
        return Recipe(
            id = id,
            title = title,
            description = description,
            ingredients = ingredients.orEmpty(),
            directions = directions.orEmpty(),
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            servings = servings?.toInt(),
            prepTime = prepTime?.toInt(),
            cookTime = cookTime?.toInt(),
            totalTime = totalTime?.toInt(),
            calories = calories?.toInt(),
            starRating = starRating?.toInt(),
            isFavorite = isFavorite,
            categories = attachedCategories.toSet(),
            recipeBookIds = bookJoinDb.getBookIdsForRecipe(id).executeAsList().toSet(),
            syncStatus = syncStatus,
            remoteId = remoteId,
            clientId = clientId,
            isPublic = isPublic,
            createdAt = Instant.parse(createdAt),
            updatedAt = Instant.parse(updatedAt),
        )
    }

    /** Maps a remotely-fetched public recipe into a transient (unsaved) domain [Recipe]. */
    private fun RemoteRecipe.toPublicPreview(): Recipe =
        Recipe(
            id = -1,
            title = title,
            description = description,
            ingredients = ingredients.orEmpty(),
            directions = directions.orEmpty(),
            imageUrl = imageUrl,
            sourceUrl = sourceUrl,
            servings = servings,
            prepTime = prepTime,
            cookTime = cookTime,
            totalTime = totalTime,
            calories = calories,
            starRating = starRating,
            isFavorite = false,
            categories = emptySet(),
            recipeBookIds = emptySet(),
            syncStatus = SyncStatus.SYNCED,
            remoteId = id,
            isPublic = isPublic,
            createdAt = createdAt?.let { Instant.parse(it) } ?: Instant.DISTANT_PAST,
            updatedAt = updatedAt?.let { Instant.parse(it) } ?: Instant.DISTANT_PAST,
        )

    private companion object {
        const val TAG = "RecipeRepositoryImpl"
    }
}

internal fun Recipe.matchesPresetFilter(presets: Set<BuiltinCategory>): Boolean {
    val recipeBuiltins = categories.mapNotNull { BuiltinCategory.fromId(it.builtinId) }.toSet()
    if (recipeBuiltins.isEmpty()) return BuiltinCategory.OTHER in presets
    return recipeBuiltins.any { it in presets }
}

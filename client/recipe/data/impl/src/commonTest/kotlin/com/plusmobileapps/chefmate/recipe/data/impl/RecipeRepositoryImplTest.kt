@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class, ExperimentalTime::class)

package com.plusmobileapps.chefmate.recipe.data.impl

import app.cash.turbine.test
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.database.Database
import com.plusmobileapps.chefmate.database.testing.createTestDatabase
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.SyncStatus
import com.plusmobileapps.chefmate.recipe.data.impl.remote.CategoryRemoteDataSource
import com.plusmobileapps.chefmate.recipe.data.impl.remote.RecipeRemoteDataSource
import com.plusmobileapps.chefmate.recipe.data.impl.remote.RemoteCategory
import com.plusmobileapps.chefmate.recipe.data.impl.remote.RemoteRecipe
import com.plusmobileapps.chefmate.recipe.data.testing.FakeRecipePhotoStorage
import com.plusmobileapps.chefmate.recipebook.data.testing.FakeRecipeBookRepository
import com.plusmobileapps.chefmate.util.testing.FakeDateTimeUtil
import com.plusmobileapps.chefmate.util.testing.FakeUnique
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class RecipeRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val db: Database = createTestDatabase()
    private val fakeAuth = FakeAuthenticationRepository()
    private val dateTimeUtil = FakeDateTimeUtil()

    private val recipeRemote = RecordingRecipeRemote()

    private val fakeBooks = FakeRecipeBookRepository(MutableStateFlow(emptyList()))

    private val recipeRepository =
        RecipeRepositoryImpl(
            db = db.recipeQueries,
            joinDb = db.recipeCategoryQueries,
            categoryDb = db.categoryQueries,
            recipeBookDb = db.recipeBookQueries,
            bookJoinDb = db.recipeBookRecipeQueries,
            recipeBookRepository = fakeBooks,
            ioContext = testDispatcher,
            dateTimeUtil = dateTimeUtil,
            unique = FakeUnique(),
            remoteDataSource = recipeRemote,
            authRepository = fakeAuth,
            photoStorage = FakeRecipePhotoStorage(),
        )

    private val categoryRepository =
        CategoryRepositoryImpl(
            db = db.categoryQueries,
            recipeCategoryQueries = db.recipeCategoryQueries,
            ioContext = testDispatcher,
            unique = FakeUnique(),
            remoteDataSource = NoopCategoryRemote(),
            authRepository = fakeAuth,
        )

    @Test
    fun createRecipe_with_categories_attaches_them_via_the_join_table() =
        runTest(testDispatcher) {
            val breakfast = categoryRepository.materializeBuiltin(BuiltinCategory.BREAKFAST)

            recipeRepository.createRecipe(
                blankRecipe(title = "Pancakes", categories = setOf(breakfast))
            )

            recipeRepository.getRecipes().test {
                val recipes = awaitItem()
                recipes.size shouldBe 1
                recipes.first().categories.singleOrNull()?.builtinId shouldBe
                    BuiltinCategory.BREAKFAST.id
            }
        }

    @Test
    fun updateRecipe_diff_syncs_attaches_and_detaches_in_one_call() =
        runTest(testDispatcher) {
            val breakfast = categoryRepository.materializeBuiltin(BuiltinCategory.BREAKFAST)
            val dinner = categoryRepository.materializeBuiltin(BuiltinCategory.DINNER)
            val custom = categoryRepository.createUserCategory("Quick")

            val created =
                recipeRepository.createRecipe(
                    blankRecipe(title = "Tacos", categories = setOf(breakfast, dinner))
                )

            // Replace categories: drop breakfast, add custom. Dinner stays put.
            recipeRepository.updateRecipe(created.copy(categories = setOf(dinner, custom)))

            recipeRepository.getRecipes().test {
                val recipes = awaitItem()
                recipes.first().categories.map { it.id }.toSet() shouldBe
                    setOf(dinner.id, custom.id)
            }
        }

    @Test
    fun addRecipesToCategory_attaches_the_category_to_every_selected_recipe() =
        runTest(testDispatcher) {
            val breakfast = categoryRepository.materializeBuiltin(BuiltinCategory.BREAKFAST)
            val one = recipeRepository.createRecipe(blankRecipe(title = "One"))
            val two = recipeRepository.createRecipe(blankRecipe(title = "Two"))

            recipeRepository.addRecipesToCategory(setOf(one.id, two.id), breakfast)

            recipeRepository.getRecipes().test {
                val recipes = awaitItem()
                recipes.forEach { recipe ->
                    recipe.categories.map { it.builtinId } shouldBe
                        listOf(BuiltinCategory.BREAKFAST.id)
                }
            }
        }

    @Test
    fun addRecipesToBook_files_every_selected_recipe_under_the_book() =
        runTest(testDispatcher) {
            db.recipeBookQueries.create(
                name = "Weeknight",
                isDefault = false,
                createdAt = dateTimeUtil.now.toString(),
                updatedAt = dateTimeUtil.now.toString(),
                clientId = "book-client-1",
                ownerId = null,
            )
            val bookId = db.recipeBookQueries.lastInsertId().executeAsOne().MAX!!
            val one = recipeRepository.createRecipe(blankRecipe(title = "One"))
            val two = recipeRepository.createRecipe(blankRecipe(title = "Two"))

            recipeRepository.addRecipesToBook(setOf(one.id, two.id), bookId)

            recipeRepository.getRecipes().test {
                awaitItem().forEach { recipe -> (bookId in recipe.recipeBookIds) shouldBe true }
            }
        }

    @Test
    fun getRecipes_filters_by_preset_using_attached_categories() =
        runTest(testDispatcher) {
            val breakfast = categoryRepository.materializeBuiltin(BuiltinCategory.BREAKFAST)
            val dinner = categoryRepository.materializeBuiltin(BuiltinCategory.DINNER)
            recipeRepository.createRecipe(
                blankRecipe(title = "Pancakes", categories = setOf(breakfast))
            )
            recipeRepository.createRecipe(blankRecipe(title = "Steak", categories = setOf(dinner)))

            recipeRepository.getRecipes(presets = setOf(BuiltinCategory.BREAKFAST)).test {
                val filtered = awaitItem()
                filtered.map { it.title } shouldBe listOf("Pancakes")
            }
        }

    @Test
    fun createRecipe_when_authenticated_Then_pushes_join_rows_to_remote() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val custom = categoryRepository.createUserCategory("Weeknight")
            // Stamp a remote id directly to simulate that the category's own remote push has
            // already completed (otherwise the join row is filtered out as unsynced).
            db.categoryQueries.updateRemoteId(remoteId = "cat-remote-1", id = custom.id)

            recipeRepository.createRecipe(blankRecipe(title = "Tacos", categories = setOf(custom)))

            val (_, categoryRemoteIds) = recipeRemote.attachmentCalls.last()
            categoryRemoteIds shouldBe setOf("cat-remote-1")
        }

    @Test
    fun updateRecipe_when_authenticated_Then_pushes_replaced_join_rows() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val a = categoryRepository.createUserCategory("A")
            val b = categoryRepository.createUserCategory("B")
            db.categoryQueries.updateRemoteId(remoteId = "cat-a", id = a.id)
            db.categoryQueries.updateRemoteId(remoteId = "cat-b", id = b.id)

            val created =
                recipeRepository.createRecipe(blankRecipe(title = "Tacos", categories = setOf(a)))
            recipeRepository.updateRecipe(created.copy(categories = setOf(b)))

            recipeRemote.attachmentCalls.last().second shouldBe setOf("cat-b")
        }

    @Test
    fun getRecipes_with_OTHER_preset_includes_recipes_with_no_categories() =
        runTest(testDispatcher) {
            val breakfast = categoryRepository.materializeBuiltin(BuiltinCategory.BREAKFAST)
            recipeRepository.createRecipe(
                blankRecipe(title = "Pancakes", categories = setOf(breakfast))
            )
            recipeRepository.createRecipe(blankRecipe(title = "Mystery", categories = emptySet()))

            recipeRepository.getRecipes(presets = setOf(BuiltinCategory.OTHER)).test {
                awaitItem().map { it.title } shouldBe listOf("Mystery")
            }
        }

    @Test
    fun deleteRecipe_when_never_synced_Then_hard_deletes_locally_and_skips_remote() =
        runTest(testDispatcher) {
            val created = recipeRepository.createRecipe(blankRecipe(title = "Toast"))

            recipeRepository.deleteRecipe(created.id)

            db.recipeQueries.getById(created.id).executeAsOneOrNull() shouldBe null
            recipeRemote.deleteCalls shouldBe emptyList()
        }

    @Test
    fun deleteRecipe_when_remote_delete_fails_Then_tombstones_and_hides_from_queries() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val created = recipeRepository.createRecipe(blankRecipe(title = "Soup"))
            val remoteId = db.recipeQueries.getById(created.id).executeAsOne().remoteId
            checkNotNull(remoteId) {
                "createRecipe should have stamped a remoteId when authenticated"
            }
            recipeRemote.deleteFailure = { RuntimeException("network") }

            recipeRepository.deleteRecipe(created.id)

            recipeRemote.deleteCalls shouldBe listOf(remoteId)
            val row = db.recipeQueries.getById(created.id).executeAsOneOrNull()
            row?.isPendingDelete shouldBe true
            recipeRepository.getRecipes().test { awaitItem() shouldBe emptyList() }
            recipeRepository.getRecipe(created.id).test { awaitItem() shouldBe null }
        }

    @Test
    fun deleteRecipe_when_remote_delete_succeeds_Then_hard_deletes_locally() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val created = recipeRepository.createRecipe(blankRecipe(title = "Stew"))

            recipeRepository.deleteRecipe(created.id)

            db.recipeQueries.getById(created.id).executeAsOneOrNull() shouldBe null
            recipeRemote.deleteCalls.size shouldBe 1
        }

    @Test
    fun syncWithRemote_retries_pending_delete_and_clears_tombstone_when_remote_succeeds() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val created = recipeRepository.createRecipe(blankRecipe(title = "Chili"))
            recipeRemote.deleteFailure = { RuntimeException("network") }
            recipeRepository.deleteRecipe(created.id)
            recipeRemote.deleteCalls.size shouldBe 1

            recipeRemote.deleteFailure = null
            recipeRepository.syncAllUnsynced()

            db.recipeQueries.getById(created.id).executeAsOneOrNull() shouldBe null
            recipeRemote.deleteCalls.size shouldBe 2
        }

    @Test
    fun syncWithRemote_when_pending_delete_keeps_failing_Then_other_recipes_still_sync() =
        runTest(testDispatcher) {
            // Create a tombstoned row directly so we can keep the repo unauthenticated until
            // we're ready to fire a single sync pass.
            db.recipeQueries.create(
                title = "Tombstoned",
                description = null,
                ingredients = null,
                directions = null,
                imageUrl = null,
                sourceUrl = null,
                servings = null,
                prepTime = null,
                cookTime = null,
                totalTime = null,
                calories = null,
                starRating = null,
                isFavorite = false,
                createdAt = "now",
                updatedAt = "now",
                clientId = "tombstone-client",
                ownerId = null,
            )
            val tombstoneId = db.recipeQueries.lastInsertId().executeAsOne().MAX!!
            db.recipeQueries.updateRemoteId(remoteId = "remote-tombstone", id = tombstoneId)
            db.recipeQueries.markPendingDelete(tombstoneId)

            // And an unsynced recipe that should get pushed despite the failing tombstone.
            db.recipeQueries.create(
                title = "Fresh",
                description = null,
                ingredients = null,
                directions = null,
                imageUrl = null,
                sourceUrl = null,
                servings = null,
                prepTime = null,
                cookTime = null,
                totalTime = null,
                calories = null,
                starRating = null,
                isFavorite = false,
                createdAt = "now",
                updatedAt = "now",
                clientId = "fresh-client",
                ownerId = null,
            )
            val freshId = db.recipeQueries.lastInsertId().executeAsOne().MAX!!
            recipeRemote.deleteFailure = { RuntimeException("network") }

            fakeAuth.setAuthenticated() // triggers syncWithRemote via the init collector

            db.recipeQueries.getById(tombstoneId).executeAsOne().isPendingDelete shouldBe true
            recipeRemote.deleteCalls shouldBe listOf("remote-tombstone")
            db.recipeQueries.getById(freshId).executeAsOne().remoteId shouldBe "remote-fresh-client"
        }

    @Test
    fun syncWithRemote_pull_applies_remote_edits_to_an_already_synced_recipe() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val created = recipeRepository.createRecipe(blankRecipe(title = "Chili"))
            val remoteId = db.recipeQueries.getById(created.id).executeAsOne().remoteId!!

            // Another device edited the same recipe and pushed it.
            recipeRemote.fetchResult =
                listOf(
                    RemoteRecipe(
                        id = remoteId,
                        ownerId = "test-id",
                        title = "Chili con Carne",
                        directions = "Simmer for an hour",
                        servings = 6,
                        isFavorite = true,
                        updatedAt = "2026-01-01T00:00:00Z",
                    )
                )

            recipeRepository.syncAllUnsynced()

            val row = db.recipeQueries.getById(created.id).executeAsOne()
            row.title shouldBe "Chili con Carne"
            row.directions shouldBe "Simmer for an hour"
            row.servings shouldBe 6L
            row.isFavorite shouldBe true
            row.updatedAt shouldBe "2026-01-01T00:00:00Z"
        }

    @Test
    fun syncWithRemote_pull_does_not_clobber_a_recipe_with_unpushed_local_edits() =
        runTest(testDispatcher) {
            val created = recipeRepository.createRecipe(blankRecipe(title = "Chili"))
            // Previously synced, then edited locally — update() leaves the row dirty.
            db.recipeQueries.updateRemoteId(remoteId = "remote-chili", id = created.id)
            db.recipeQueries.update(
                title = "My local title",
                description = null,
                ingredients = null,
                directions = null,
                imageUrl = null,
                sourceUrl = null,
                servings = null,
                prepTime = null,
                cookTime = null,
                totalTime = null,
                calories = null,
                starRating = null,
                isFavorite = false,
                updatedAt = "2026-02-02T00:00:00Z",
                id = created.id,
            )
            // The dirty push fails, so the remote still holds the pre-edit copy when we pull.
            recipeRemote.upsertFailure = { RuntimeException("network") }
            recipeRemote.fetchResult =
                listOf(
                    RemoteRecipe(
                        id = "remote-chili",
                        ownerId = "test-id",
                        title = "Stale remote title",
                        updatedAt = "2026-01-01T00:00:00Z",
                    )
                )

            fakeAuth.setAuthenticated()

            val row = db.recipeQueries.getById(created.id).executeAsOne()
            row.title shouldBe "My local title"
            row.isDirty shouldBe true
        }

    @Test
    fun syncWithRemote_prunes_an_owned_recipe_deleted_on_another_device() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val kept = recipeRepository.createRecipe(blankRecipe(title = "Kept"))
            val deletedElsewhere = recipeRepository.createRecipe(blankRecipe(title = "Gone"))
            val keptRemoteId = db.recipeQueries.getById(kept.id).executeAsOne().remoteId!!

            // The remote no longer lists "Gone" — it was deleted from another device.
            recipeRemote.fetchResult =
                listOf(RemoteRecipe(id = keptRemoteId, ownerId = "test-id", title = "Kept"))

            recipeRepository.syncAllUnsynced()

            db.recipeQueries.getById(kept.id).executeAsOneOrNull() shouldNotBe null
            db.recipeQueries.getById(deletedElsewhere.id).executeAsOneOrNull() shouldBe null
        }

    @Test
    fun syncWithRemote_prune_leaves_recipes_shared_by_someone_else_alone() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            // A recipe shared through someone else's book, cached locally on a previous sync.
            db.recipeQueries.createWithRemoteId(
                title = "Theirs",
                description = null,
                ingredients = null,
                directions = null,
                imageUrl = null,
                sourceUrl = null,
                servings = null,
                prepTime = null,
                cookTime = null,
                totalTime = null,
                calories = null,
                starRating = null,
                isFavorite = false,
                createdAt = "now",
                updatedAt = "now",
                remoteId = "remote-theirs",
                clientId = "theirs-client",
                ownerId = "someone-else",
                isPublic = false,
            )
            val sharedId = db.recipeQueries.getByRemoteId("remote-theirs").executeAsOne().id

            // Access was revoked, so the RPC no longer returns it. Losing access must not delete
            // the local copy here — deleteLocalRecipesInBook owns that decision.
            recipeRemote.fetchResult = emptyList()

            recipeRepository.syncAllUnsynced()

            db.recipeQueries.getById(sharedId).executeAsOneOrNull() shouldNotBe null
        }

    @Test
    fun syncWithRemote_prune_leaves_a_recipe_with_unpushed_local_edits_alone() =
        runTest(testDispatcher) {
            val created = recipeRepository.createRecipe(blankRecipe(title = "Chili"))
            db.recipeQueries.updateRemoteId(remoteId = "remote-chili", id = created.id)
            db.recipeQueries.markDirty(id = created.id, updatedAt = "2026-02-02T00:00:00Z")
            recipeRemote.upsertFailure = { RuntimeException("network") }
            recipeRemote.fetchResult = emptyList()

            fakeAuth.setAuthenticated()

            db.recipeQueries.getById(created.id).executeAsOneOrNull() shouldNotBe null
        }

    @Test
    fun syncWithRemote_pull_does_not_resurrect_tombstoned_recipe() =
        runTest(testDispatcher) {
            db.recipeQueries.create(
                title = "Tombstoned",
                description = null,
                ingredients = null,
                directions = null,
                imageUrl = null,
                sourceUrl = null,
                servings = null,
                prepTime = null,
                cookTime = null,
                totalTime = null,
                calories = null,
                starRating = null,
                isFavorite = false,
                createdAt = "now",
                updatedAt = "now",
                clientId = "tombstone-client",
                ownerId = null,
            )
            val id = db.recipeQueries.lastInsertId().executeAsOne().MAX!!
            db.recipeQueries.updateRemoteId(remoteId = "remote-1", id = id)
            db.recipeQueries.markPendingDelete(id)
            recipeRemote.deleteFailure = { RuntimeException("network") }
            recipeRemote.fetchResult =
                listOf(
                    RemoteRecipe(
                        id = "remote-1",
                        ownerId = "test-id",
                        title = "Tombstoned",
                        clientId = "tombstone-client",
                    )
                )

            fakeAuth.setAuthenticated()

            // Pull saw the matching remote row, found the tombstoned local row via
            // getByRemoteId, and skipped re-creation. Tombstone state is preserved.
            db.recipeQueries.getById(id).executeAsOne().isPendingDelete shouldBe true
            recipeRepository.getRecipes().test { awaitItem() shouldBe emptyList() }
        }

    @Test
    fun deleteRecipe_when_unauthenticated_Then_tombstones_and_syncs_on_next_authentication() =
        runTest(testDispatcher) {
            val created = recipeRepository.createRecipe(blankRecipe(title = "Curry"))
            // Pretend the recipe was previously synced even though we're now offline.
            db.recipeQueries.updateRemoteId(remoteId = "remote-curry", id = created.id)

            recipeRepository.deleteRecipe(created.id)

            db.recipeQueries.getById(created.id).executeAsOne().isPendingDelete shouldBe true
            recipeRemote.deleteCalls shouldBe emptyList()

            fakeAuth.setAuthenticated() // init collector fires syncWithRemote → pushPendingDeletes

            db.recipeQueries.getById(created.id).executeAsOneOrNull() shouldBe null
            recipeRemote.deleteCalls shouldBe listOf("remote-curry")
        }

    @Test
    fun getRecipes_by_book_only_returns_that_books_recipes() =
        runTest(testDispatcher) {
            val bookA = createBook("a")
            val bookB = createBook("b")

            fakeBooks.setActiveBook(bookA)
            recipeRepository.createRecipe(blankRecipe(title = "In A"))
            fakeBooks.setActiveBook(bookB)
            recipeRepository.createRecipe(blankRecipe(title = "In B"))

            recipeRepository.getRecipes(bookA).test {
                awaitItem().map { it.title } shouldBe listOf("In A")
            }
        }

    @Test
    fun createRecipe_files_under_the_active_book_when_none_specified() =
        runTest(testDispatcher) {
            val book = createBook("active")
            fakeBooks.setActiveBook(book)

            val created = recipeRepository.createRecipe(blankRecipe(title = "Stamped"))

            created.recipeBookIds shouldBe setOf(book)
        }

    @Test
    fun createRecipe_can_file_under_multiple_books() =
        runTest(testDispatcher) {
            val bookA = createBook("a")
            val bookB = createBook("b")

            val created =
                recipeRepository.createRecipe(
                    blankRecipe(title = "Shared").copy(recipeBookIds = setOf(bookA, bookB))
                )

            created.recipeBookIds shouldBe setOf(bookA, bookB)
            recipeRepository.getRecipes(bookA).test {
                awaitItem().map { it.title } shouldBe listOf("Shared")
            }
            recipeRepository.getRecipes(bookB).test {
                awaitItem().map { it.title } shouldBe listOf("Shared")
            }
        }

    private fun createBook(clientId: String): Long {
        db.recipeBookQueries.create(
            name = clientId,
            isDefault = false,
            createdAt = "now",
            updatedAt = "now",
            clientId = clientId,
            ownerId = null,
        )
        return db.recipeBookQueries.lastInsertId().executeAsOne().MAX!!
    }

    @Test
    fun setRecipePublic_when_authenticated_flags_public_and_returns_remote_id() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val created = recipeRepository.createRecipe(blankRecipe("Shared Stew"))

            val remoteId = recipeRepository.setRecipePublic(created.id, isPublic = true)

            remoteId shouldBe recipeRepository.getRecipe(created.id).first()?.remoteId
            (remoteId != null) shouldBe true
            recipeRepository.getRecipe(created.id).first()?.isPublic shouldBe true
        }

    @Test
    fun setRecipePublic_when_unauthenticated_returns_null() =
        runTest(testDispatcher) {
            val created = recipeRepository.createRecipe(blankRecipe("Private Pie"))
            recipeRepository.setRecipePublic(created.id, isPublic = true) shouldBe null
        }

    @Test
    fun fetchPublicRecipe_maps_a_public_remote_recipe() =
        runTest(testDispatcher) {
            recipeRemote.publicRecipe =
                RemoteRecipe(
                    id = "pub-1",
                    ownerId = "another-user",
                    title = "Public Pancakes",
                    ingredients = "flour",
                    directions = "mix",
                    isPublic = true,
                )

            val result = recipeRepository.fetchPublicRecipe("pub-1")

            result.isSuccess shouldBe true
            result.getOrNull()?.title shouldBe "Public Pancakes"
            result.getOrNull()?.remoteId shouldBe "pub-1"
            result.getOrNull()?.isPublic shouldBe true
        }

    @Test
    fun fetchPublicRecipe_fails_when_recipe_is_not_public() =
        runTest(testDispatcher) {
            recipeRepository.fetchPublicRecipe("missing").isFailure shouldBe true
        }

    @Test
    fun getRecipeByRemoteId_returns_the_local_recipe() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val created = recipeRepository.createRecipe(blankRecipe("Findable"))
            val remoteId = recipeRepository.getRecipe(created.id).first()?.remoteId

            recipeRepository.getRecipeByRemoteId(remoteId!!)?.title shouldBe "Findable"
        }

    @Test
    fun getRecipeByClientId_returns_the_local_recipe() =
        runTest(testDispatcher) {
            val created = recipeRepository.createRecipe(blankRecipe("ByClient"))
            val clientId = recipeRepository.getRecipe(created.id).first()?.clientId

            clientId shouldNotBe null
            recipeRepository.getRecipeByClientId(clientId!!)?.title shouldBe "ByClient"
        }

    @Test
    fun getRecipeByClientId_returns_null_when_absent() =
        runTest(testDispatcher) {
            recipeRepository.getRecipeByClientId("no-such-client-id") shouldBe null
        }

    private fun blankRecipe(title: String, categories: Set<Category> = emptySet()) =
        Recipe(
            id = -1,
            title = title,
            description = null,
            ingredients = "",
            directions = "",
            imageUrl = null,
            sourceUrl = null,
            servings = null,
            prepTime = null,
            cookTime = null,
            totalTime = null,
            calories = null,
            starRating = null,
            isFavorite = false,
            categories = categories,
            syncStatus = SyncStatus.NOT_SYNCED,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = Instant.DISTANT_PAST,
        )

    /**
     * Records every join-row push the repo makes so tests can assert that recipe ↔ category
     * attachments are synced to the remote alongside the recipe row itself.
     */
    private class RecordingRecipeRemote : RecipeRemoteDataSource {
        val attachmentCalls: MutableList<Pair<String, Set<String>>> = mutableListOf()
        val deleteCalls: MutableList<String> = mutableListOf()
        var deleteFailure: (() -> Throwable)? = null
        var fetchResult: List<RemoteRecipe> = emptyList()

        var upsertFailure: (() -> Throwable)? = null

        override suspend fun upsertRecipe(recipe: RemoteRecipe): RemoteRecipe {
            upsertFailure?.invoke()?.let { throw it }
            // Stamp a stable remote id derived from the client id so tests can correlate.
            return recipe.copy(id = recipe.id ?: "remote-${recipe.clientId.orEmpty()}")
        }

        override suspend fun deleteRecipe(remoteId: String) {
            deleteCalls += remoteId
            deleteFailure?.invoke()?.let { throw it }
        }

        override suspend fun fetchAccessibleRecipes(): List<RemoteRecipe> = fetchResult

        var publicRecipe: RemoteRecipe? = null

        override suspend fun fetchPublicRecipe(remoteId: String): RemoteRecipe? =
            publicRecipe?.takeIf {
                it.id == remoteId
            }

        override suspend fun setRecipeCategories(
            recipeRemoteId: String,
            categoryRemoteIds: Set<String>,
        ) {
            attachmentCalls += recipeRemoteId to categoryRemoteIds
        }

        override suspend fun fetchRecipeCategoryAttachments(): Map<String, Set<String>> = emptyMap()

        val bookAttachmentCalls: MutableList<Pair<String, Set<String>>> = mutableListOf()

        override suspend fun setRecipeBooks(recipeRemoteId: String, bookRemoteIds: Set<String>) {
            bookAttachmentCalls += recipeRemoteId to bookRemoteIds
        }

        override suspend fun fetchRecipeBookAttachments(): Map<String, Set<String>> = emptyMap()
    }

    private class NoopCategoryRemote : CategoryRemoteDataSource {
        override suspend fun upsertCategory(category: RemoteCategory): RemoteCategory = category

        override suspend fun deleteCategory(remoteId: String) = Unit

        override suspend fun fetchAllCategories(ownerId: String): List<RemoteCategory> = emptyList()
    }
}

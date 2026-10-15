@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.recipebook.data.impl

import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.database.Database
import com.plusmobileapps.chefmate.database.testing.createTestDatabase
import com.plusmobileapps.chefmate.recipe.data.testing.FakeRecipeRepository
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBook
import com.plusmobileapps.chefmate.recipebook.data.RecipeBook
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RecipeBookMemberRemoteDataSource
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteCollaborator
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteRecipeBook
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteRecipeBookInvite
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteRecipeBookMember
import com.plusmobileapps.chefmate.util.testing.FakeDateTimeUtil
import com.plusmobileapps.chefmate.util.testing.FakeUnique
import com.russhwolf.settings.MapSettings
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/** Family recipe books: the RPC-backed management repository and how sync caches family books. */
class FamilyRecipeBookRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val db: Database = createTestDatabase()
    private val fakeAuth = FakeAuthenticationRepository()
    private val server = FakeRecipeBookServer()
    private val familyRemote =
        FakeFamilyRecipeBookRemoteDataSource(server).apply { familyNames[FAMILY_ID] = "The Smiths" }

    private val bookRepository =
        RecipeBookRepositoryImpl(
            db = db.recipeBookQueries,
            bookRecipeDb = db.recipeBookRecipeQueries,
            ioContext = testDispatcher,
            dateTimeUtil = FakeDateTimeUtil(),
            unique = FakeUnique(),
            remoteDataSource = server,
            memberRemoteDataSource = NoopMemberRemote(),
            familyRemoteDataSource = familyRemote,
            authRepository = fakeAuth,
            selectionRepository = RecipeBookSelectionRepositoryImpl(MapSettings()),
        )

    private val repository =
        FamilyRecipeBookRepositoryImpl(
            ioContext = testDispatcher,
            remoteDataSource = familyRemote,
            recipeBookRepository = bookRepository,
            recipeRepository = FakeRecipeRepository(),
        )

    @Test
    fun createBook_creates_the_book_remotely_and_syncs_it_into_the_local_cache() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()

            repository.createBook(FAMILY_ID, "  Sunday dinners ")

            repository.getBooks(FAMILY_ID).map { it.name } shouldBe listOf("Sunday dinners")
            val local = localBooks().single { it.isFamily }
            local.name shouldBe "Sunday dinners"
            local.familyId shouldBe FAMILY_ID
            local.familyName shouldBe "The Smiths"
            local.isDefault shouldBe false
        }

    @Test
    fun renameBook_updates_the_local_name_from_the_remote() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            repository.createBook(FAMILY_ID, "Dinners")
            val bookId = repository.getBooks(FAMILY_ID).single().id

            repository.renameBook(bookId, "Sunday dinners")

            localBooks().single { it.isFamily }.name shouldBe "Sunday dinners"
        }

    @Test
    fun deleteBook_removes_the_book_and_prunes_it_locally() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            repository.createBook(FAMILY_ID, "Dinners")
            val bookId = repository.getBooks(FAMILY_ID).single().id

            repository.deleteBook(bookId)

            repository.getBooks(FAMILY_ID) shouldBe emptyList<FamilyRecipeBook>()
            localBooks().none { it.isFamily } shouldBe true
        }

    @Test
    fun rpc_failures_propagate_to_the_caller() =
        runTest(testDispatcher) {
            familyRemote.error = IllegalStateException("family: forbidden")

            shouldThrow<IllegalStateException> { repository.createBook(FAMILY_ID, "Dinners") }
            shouldThrow<IllegalStateException> { repository.getBooks(FAMILY_ID) }
        }

    @Test
    fun a_family_book_the_user_created_is_still_not_owned_by_them() =
        runTest(testDispatcher) {
            server.addFamilyBook("family-book", "Dinners", FAMILY_ID, creator = USER_ID)

            fakeAuth.setAuthenticated()

            val book = localBooks().single { it.isFamily }
            book.isOwnedByCurrentUser shouldBe false
            book.familyName shouldBe "The Smiths"
        }

    @Test
    fun sync_never_adopts_a_family_book_as_the_local_default() =
        runTest(testDispatcher) {
            server.books +=
                RemoteRecipeBook(
                    id = "family-book",
                    ownerId = USER_ID,
                    name = "My Recipes",
                    isDefault = true,
                    familyId = FAMILY_ID,
                )

            fakeAuth.setAuthenticated()

            val books = localBooks()
            books.single { it.isDefault }.isFamily shouldBe false
            books.single { it.isFamily }.isDefault shouldBe false
        }

    @Test
    fun family_books_cant_be_renamed_or_deleted_locally() =
        runTest(testDispatcher) {
            server.addFamilyBook("family-book", "Dinners", FAMILY_ID, creator = "user-2")
            fakeAuth.setAuthenticated()
            val book = localBooks().single { it.isFamily }

            bookRepository.renameBook(book.id, "Mine now")
            bookRepository.deleteBook(book.id)

            val after = localBooks().single { it.isFamily }
            after.name shouldBe "Dinners"
            db.recipeBookQueries.getById(book.id).executeAsOne().isDirty shouldBe false
            server.books.any { it.id == "family-book" } shouldBe true
        }

    @Test
    fun leaving_the_family_prunes_its_books_and_the_other_members_recipes_in_them() =
        runTest(testDispatcher) {
            server.addFamilyBook("family-book", "Dinners", FAMILY_ID, creator = "user-2")
            fakeAuth.setAuthenticated()
            val familyBook = localBooks().single { it.isFamily }
            val defaultBookId = bookRepository.getDefaultBookId()
            val theirs = insertRecipe("Their lasagna", owner = "user-2", familyBook.id)
            val mine = insertRecipe("My soup", owner = USER_ID, familyBook.id)
            val theirsAlsoInMine =
                insertRecipe("Shared stew", owner = "user-3", familyBook.id, defaultBookId)
            bookRepository.setActiveBook(familyBook.id)

            // RLS stops returning the family's books once the user has left.
            server.books.removeAll { it.familyId == FAMILY_ID }
            repository.refreshLocalBooks()

            localBooks().none { it.isFamily } shouldBe true
            recipeExists(theirs) shouldBe false
            recipeExists(mine) shouldBe true
            recipeExists(theirsAlsoInMine) shouldBe true
            bookIdsFor(mine) shouldBe listOf(defaultBookId)
            bookRepository.activeBookId.value shouldBe defaultBookId
        }

    @Test
    fun a_deleted_family_hands_the_book_back_to_its_creator_as_a_personal_book() =
        runTest(testDispatcher) {
            server.addFamilyBook("family-book", "Dinners", FAMILY_ID, creator = USER_ID)
            fakeAuth.setAuthenticated()

            // families.id ON DELETE SET NULL
            val index = server.books.indexOfFirst { it.id == "family-book" }
            server.books[index] = server.books[index].copy(familyId = null)
            repository.refreshLocalBooks()

            val book = localBooks().single { it.name == "Dinners" }
            book.isFamily shouldBe false
            book.familyName shouldBe null
            book.isOwnedByCurrentUser shouldBe true
        }

    private suspend fun localBooks(): List<RecipeBook> = bookRepository.getRecipeBooks().first()

    private fun insertRecipe(title: String, owner: String, vararg bookIds: Long): Long {
        db.recipeQueries.create(
            title = title,
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
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            clientId = null,
            ownerId = owner,
        )
        val id = db.recipeQueries.lastInsertId().executeAsOne().MAX!!
        bookIds.forEach { db.recipeBookRecipeQueries.attach(recipeBookId = it, recipeId = id) }
        return id
    }

    private fun recipeExists(id: Long): Boolean =
        db.recipeQueries.getById(id).executeAsOneOrNull() != null

    private fun bookIdsFor(recipeId: Long): List<Long> =
        db.recipeBookRecipeQueries.getBookIdsForRecipe(recipeId).executeAsList()

    private class NoopMemberRemote : RecipeBookMemberRemoteDataSource {
        override suspend fun fetchMembers(bookRemoteId: String): List<RemoteRecipeBookMember> =
            emptyList()

        override suspend fun fetchCollaborators(bookRemoteId: String): List<RemoteCollaborator> =
            emptyList()

        override suspend fun invite(bookRemoteId: String, email: String, role: String) = Unit

        override suspend fun deleteMember(memberId: String) = Unit

        override suspend fun fetchPendingInvites(email: String): List<RemoteRecipeBookInvite> =
            emptyList()

        override suspend fun acceptInvite(memberId: String, userId: String) = Unit

        override suspend fun rejectInvite(memberId: String) = Unit
    }

    private companion object {
        // FakeAuthenticationRepository.fakeUser()'s id.
        const val USER_ID = "test-id"
        const val FAMILY_ID = "family-1"
    }
}

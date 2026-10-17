@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.recipebook.data.impl

import app.cash.turbine.test
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.database.Database
import com.plusmobileapps.chefmate.database.testing.createTestDatabase
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RecipeBookMemberRemoteDataSource
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RecipeBookRemoteDataSource
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteCollaborator
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteInviteBook
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteRecipeBook
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteRecipeBookInvite
import com.plusmobileapps.chefmate.recipebook.data.impl.remote.RemoteRecipeBookMember
import com.plusmobileapps.chefmate.util.testing.FakeDateTimeUtil
import com.plusmobileapps.chefmate.util.testing.FakeUnique
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class RecipeBookRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val db: Database = createTestDatabase()
    private val fakeAuth = FakeAuthenticationRepository()
    private val dateTimeUtil = FakeDateTimeUtil()
    private val settings = MapSettings()

    private fun repository(
        remoteDataSource: RecipeBookRemoteDataSource = NoopRecipeBookRemote(),
        memberRemoteDataSource: RecipeBookMemberRemoteDataSource = NoopMemberRemote(),
    ) =
        RecipeBookRepositoryImpl(
            db = db.recipeBookQueries,
            bookRecipeDb = db.recipeBookRecipeQueries,
            ioContext = testDispatcher,
            dateTimeUtil = dateTimeUtil,
            unique = FakeUnique(),
            remoteDataSource = remoteDataSource,
            memberRemoteDataSource = memberRemoteDataSource,
            familyRemoteDataSource = FakeFamilyRecipeBookRemoteDataSource(),
            authRepository = fakeAuth,
            selectionRepository = RecipeBookSelectionRepositoryImpl(settings),
        )

    @Test
    fun ensures_exactly_one_default_book_on_first_use() =
        runTest(testDispatcher) {
            val repo = repository()

            repo.getRecipeBooks().test {
                val books = awaitItem()
                books.size shouldBe 1
                books.single().isDefault shouldBe true
                books.single().name shouldBe "My Recipes"
            }
        }

    @Test
    fun activeBookId_resolves_to_the_default_book_initially() =
        runTest(testDispatcher) {
            val repo = repository()
            val defaultId = repo.getDefaultBookId()

            repo.activeBookId.value shouldBe defaultId
        }

    @Test
    fun clearLocalData_wipes_books_and_resets_to_a_fresh_default() =
        runTest(testDispatcher) {
            val repo = repository()
            val shared = repo.createBook("Shared with me")
            repo.setActiveBook(shared.id)

            repo.clearLocalData()

            repo.getRecipeBooks().test {
                val books = awaitItem()
                // Only the recreated baseline "My Recipes" default survives.
                books.single().isDefault shouldBe true
                books.single().name shouldBe "My Recipes"
                // The active selection no longer points at the deleted book.
                repo.activeBookId.value shouldBe books.single().id
            }
        }

    @Test
    fun reads_books_seeded_with_sqlite_datetime_timestamps() =
        runTest(testDispatcher) {
            // A book seeded by the upgrade migration's column default carries a SQLite datetime
            // ("2026-06-08 22:11:24"), which Instant.parse rejects. Reading it must not crash.
            db.recipeBookQueries.create(
                name = "Imported",
                isDefault = false,
                createdAt = "2026-06-08 22:11:24",
                updatedAt = "2026-06-08 22:11:24",
                clientId = "client-1",
                ownerId = null,
            )
            val repo = repository()

            repo.getRecipeBooks().test {
                val books = awaitItem()
                books.map { it.name } shouldContain "Imported"
            }
        }

    @Test
    fun createBook_adds_a_non_default_book() =
        runTest(testDispatcher) {
            val repo = repository()

            val created = repo.createBook("Weeknight Dinners")

            created.name shouldBe "Weeknight Dinners"
            created.isDefault shouldBe false
            repo.getRecipeBooks().test { awaitItem().size shouldBe 2 }
        }

    @Test
    fun renameBook_changes_the_name() =
        runTest(testDispatcher) {
            val repo = repository()
            val created = repo.createBook("Old name")

            repo.renameBook(created.id, "New name")

            repo.getRecipeBook(created.id).test { awaitItem()?.name shouldBe "New name" }
        }

    @Test
    fun setActiveBook_updates_active_id_and_persists() =
        runTest(testDispatcher) {
            val repo = repository()
            val created = repo.createBook("Holiday Baking")

            repo.setActiveBook(created.id)

            repo.activeBookId.value shouldBe created.id
            // A fresh repository reading the same settings restores the selection.
            repository().activeBookId.value shouldBe created.id
        }

    @Test
    fun selectAllRecipes_clears_active_book_and_persists_across_reload() =
        runTest(testDispatcher) {
            val repo = repository()
            val created = repo.createBook("Holiday Baking")
            repo.setActiveBook(created.id)

            repo.selectAllRecipes()

            // "All recipes" is modelled as a null active id spanning every book.
            repo.activeBookId.value shouldBe null
            // A fresh repository reading the same settings restores the "All recipes" selection
            // rather than falling back to the default book.
            repository().activeBookId.value shouldBe null
        }

    @Test
    fun deleteBook_removes_an_unsynced_book_outright() =
        runTest(testDispatcher) {
            val repo = repository()
            val created = repo.createBook("Weeknight Dinners")

            repo.deleteBook(created.id)

            repo.getRecipeBooks().test {
                awaitItem().map { it.name } shouldNotContain "Weeknight Dinners"
            }
        }

    @Test
    fun deleteBook_falls_the_active_selection_back_to_the_default_book() =
        runTest(testDispatcher) {
            val repo = repository()
            val created = repo.createBook("Weeknight Dinners")
            repo.setActiveBook(created.id)

            repo.deleteBook(created.id)

            repo.activeBookId.value shouldBe repo.getDefaultBookId()
        }

    @Test
    fun deleteBook_ignores_the_default_book() =
        runTest(testDispatcher) {
            val repo = repository()
            val defaultId = repo.getDefaultBookId()

            repo.deleteBook(defaultId)

            repo.getRecipeBooks().test { awaitItem().map { it.id } shouldContain defaultId }
        }

    @Test
    fun deleteBook_tombstones_a_synced_book_when_the_remote_delete_fails() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val failingRemote =
                object : NoopRecipeBookRemote() {
                    // Stamp a distinct remote id on each push so books count as synced.
                    override suspend fun upsertRecipeBook(book: RemoteRecipeBook) =
                        book.copy(id = "remote-${book.name}")

                    override suspend fun deleteRecipeBook(remoteId: String) {
                        error("offline")
                    }
                }
            val repo = repository(remoteDataSource = failingRemote)
            val created = repo.createBook("Weeknight Dinners")

            repo.deleteBook(created.id)

            // Hidden from the list right away, but the row survives so sync can retry the remote
            // delete.
            repo.getRecipeBooks().test { awaitItem().map { it.id } shouldNotContain created.id }
            db.recipeBookQueries.getPendingDeletes().executeAsList().map { it.id } shouldContain
                created.id
        }

    @Test
    fun removeLocalBook_drops_the_row_without_deleting_it_remotely() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val deletedRemotely = mutableListOf<String>()
            val remote =
                object : NoopRecipeBookRemote() {
                    override suspend fun upsertRecipeBook(book: RemoteRecipeBook) =
                        book.copy(id = "remote-${book.name}")

                    override suspend fun deleteRecipeBook(remoteId: String) {
                        deletedRemotely += remoteId
                    }
                }
            val repo = repository(remoteDataSource = remote)
            val created = repo.createBook("Shared with me")

            repo.removeLocalBook(created.id)

            repo.getRecipeBooks().test { awaitItem().map { it.id } shouldNotContain created.id }
            deletedRemotely.isEmpty() shouldBe true
            db.recipeBookQueries.getPendingDeletes().executeAsList().isEmpty() shouldBe true
        }

    @Test
    fun pull_applies_a_remote_rename_to_a_book_already_synced_locally() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val bookRemote = StubRecipeBookRemote()
            val repo = repository(remoteDataSource = bookRemote)
            val created = repo.createBook("Weeknights")
            db.recipeBookQueries.getById(created.id).executeAsOne().remoteId shouldBe
                "remote-Weeknights"

            // The book was renamed on another device after this one had already cached it.
            bookRemote.fetchResult =
                listOf(
                    RemoteRecipeBook(
                        id = "remote-Weeknights",
                        ownerId = "test-id",
                        name = "Weeknight Dinners",
                    )
                )
            repo.syncAllUnsynced()

            db.recipeBookQueries.getById(created.id).executeAsOne().name shouldBe
                "Weeknight Dinners"
        }

    @Test
    fun pull_does_not_clobber_a_book_with_an_unpushed_local_rename() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            val bookRemote = StubRecipeBookRemote()
            val repo = repository(remoteDataSource = bookRemote)
            val created = repo.createBook("Weeknights")

            // Rename locally, but the push fails — the book is still dirty when the pull runs,
            // and the remote still holds the pre-rename name.
            bookRemote.upsertFailure = { RuntimeException("network") }
            repo.renameBook(created.id, "My local name")
            bookRemote.fetchResult =
                listOf(
                    RemoteRecipeBook(
                        id = "remote-Weeknights",
                        ownerId = "test-id",
                        name = "Weeknights",
                    )
                )

            repo.syncAllUnsynced()

            val row = db.recipeBookQueries.getById(created.id).executeAsOne()
            row.name shouldBe "My local name"
            row.isDirty shouldBe true
        }

    @Test
    fun pull_skips_books_with_only_a_pending_invite() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            // Two shared books owned by someone else: one accepted, one still a pending invite.
            val accepted =
                RemoteRecipeBook(id = "remote-accepted", ownerId = "owner", name = "Shared")
            val pending =
                RemoteRecipeBook(id = "remote-pending", ownerId = "owner", name = "Invited")
            val bookRemote =
                object : RecipeBookRemoteDataSource {
                    override suspend fun upsertRecipeBook(book: RemoteRecipeBook) = book

                    override suspend fun deleteRecipeBook(remoteId: String) = Unit

                    override suspend fun fetchAccessibleRecipeBooks() = listOf(accepted, pending)
                }
            val memberRemote =
                object : NoopMemberRemote() {
                    override suspend fun fetchPendingInvites(email: String) =
                        listOf(
                            RemoteRecipeBookInvite(
                                id = "member-1",
                                recipeBookId = "remote-pending",
                                role = "editor",
                                status = "pending",
                                book = RemoteInviteBook(name = "Invited"),
                            )
                        )
                }

            val repo =
                repository(remoteDataSource = bookRemote, memberRemoteDataSource = memberRemote)

            repo.getRecipeBooks().test {
                val names = awaitItem().map { it.name }
                names shouldContain "Shared" // accepted book is cached
                names shouldNotContain "Invited" // pending-invite book stays hidden
            }
        }

    /**
     * Stamps a stable remote id derived from the book name so tests can correlate, and lets a test
     * fail the push to leave a book dirty when the pull runs.
     */
    private class StubRecipeBookRemote : RecipeBookRemoteDataSource {
        var fetchResult: List<RemoteRecipeBook> = emptyList()
        var upsertFailure: (() -> Throwable)? = null

        override suspend fun upsertRecipeBook(book: RemoteRecipeBook): RemoteRecipeBook {
            upsertFailure?.invoke()?.let { throw it }
            return book.copy(id = book.id ?: "remote-${book.name}")
        }

        override suspend fun deleteRecipeBook(remoteId: String) = Unit

        override suspend fun fetchAccessibleRecipeBooks(): List<RemoteRecipeBook> = fetchResult
    }

    private open class NoopRecipeBookRemote : RecipeBookRemoteDataSource {
        override suspend fun upsertRecipeBook(book: RemoteRecipeBook): RemoteRecipeBook = book

        override suspend fun deleteRecipeBook(remoteId: String) = Unit

        override suspend fun fetchAccessibleRecipeBooks(): List<RemoteRecipeBook> = emptyList()
    }

    private open class NoopMemberRemote : RecipeBookMemberRemoteDataSource {
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
}

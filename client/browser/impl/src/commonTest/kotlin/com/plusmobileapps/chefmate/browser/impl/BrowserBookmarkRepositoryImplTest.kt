@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.browser.impl

import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.browser.BrowserBookmark
import com.plusmobileapps.chefmate.browser.impl.remote.BrowserBookmarkRemoteDataSource
import com.plusmobileapps.chefmate.browser.impl.remote.RemoteBrowserBookmark
import com.plusmobileapps.chefmate.database.Database
import com.plusmobileapps.chefmate.database.testing.createTestDatabase
import com.plusmobileapps.chefmate.util.testing.FakeDateTimeUtil
import com.plusmobileapps.chefmate.util.testing.FakeUnique
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class BrowserBookmarkRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val db: Database = createTestDatabase()
    private val fakeAuth = FakeAuthenticationRepository()
    private val fakeRemote = RecordingRemote()
    private val dateTimeUtil = FakeDateTimeUtil()

    private val repository =
        BrowserBookmarkRepositoryImpl(
            queries = db.browserBookmarkQueries,
            ioContext = testDispatcher,
            unique = FakeUnique(),
            dateTimeUtil = dateTimeUtil,
            remoteDataSource = fakeRemote,
            authRepository = fakeAuth,
        )

    private suspend fun bookmarks(): List<BrowserBookmark> = repository.observeBookmarks().first()

    private fun row(url: String) = db.browserBookmarkQueries.getByUrl(url).executeAsOne()

    @Test
    fun addBookmark_stores_normalized_url_and_title() =
        runTest(testDispatcher) {
            repository.addBookmark("  https://example.com/recipe#step-2 ", "Banana Bread")

            bookmarks().single().let {
                it.url shouldBe "https://example.com/recipe"
                it.title shouldBe "Banana Bread"
            }
        }

    @Test
    fun addBookmark_without_title_falls_back_to_host() =
        runTest(testDispatcher) {
            repository.addBookmark("https://www.seriouseats.com/banana-bread", null)
            repository.addBookmark("https://food52.com/", "   ")

            bookmarks().map { it.title } shouldBe listOf("seriouseats.com", "food52.com")
        }

    @Test
    fun addBookmark_twice_for_same_page_is_a_no_op() =
        runTest(testDispatcher) {
            repository.addBookmark("https://example.com/", "First")
            repository.addBookmark("https://example.com/#top", "Second")

            bookmarks().single().title shouldBe "First"
        }

    @Test
    fun bookmarks_are_listed_oldest_first() =
        runTest(testDispatcher) {
            dateTimeUtil.fakeNow = Instant.parse("2026-01-02T00:00:00Z")
            repository.addBookmark("https://b.com/", "B")
            dateTimeUtil.fakeNow = Instant.parse("2026-01-01T00:00:00Z")
            fakeRemote.remoteBookmarks =
                listOf(
                    RemoteBrowserBookmark(
                        id = "remote-a",
                        ownerId = "test-id",
                        url = "https://a.com/",
                        title = "A",
                        createdAt = "2025-12-31T00:00:00Z",
                    )
                )
            fakeAuth.setAuthenticated()

            bookmarks().map { it.title } shouldBe listOf("A", "B")
        }

    @Test
    fun removeBookmarkByUrl_matches_normalized_url() =
        runTest(testDispatcher) {
            repository.addBookmark("https://example.com/recipe", "Recipe")

            repository.removeBookmarkByUrl("https://example.com/recipe#comments")

            bookmarks() shouldBe emptyList()
        }

    @Test
    fun addBookmark_while_signed_out_does_not_push() =
        runTest(testDispatcher) {
            repository.addBookmark("https://example.com/", "Example")

            fakeRemote.upserts shouldBe emptyList()
            row("https://example.com/").remoteId shouldBe null
        }

    @Test
    fun addBookmark_while_authenticated_pushes_and_stores_remoteId() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()

            repository.addBookmark("https://example.com/", "Example")

            fakeRemote.upserts.single().let {
                it.url shouldBe "https://example.com/"
                it.title shouldBe "Example"
                it.ownerId shouldBe "test-id"
            }
            row("https://example.com/").remoteId shouldBe "remote-uid-1"
        }

    @Test
    fun authenticating_pushes_bookmarks_added_while_signed_out() =
        runTest(testDispatcher) {
            repository.addBookmark("https://example.com/", "Example")

            fakeAuth.setAuthenticated()

            fakeRemote.upserts.size shouldBe 1
            row("https://example.com/").remoteId shouldBe "remote-uid-1"
        }

    @Test
    fun authenticating_pulls_remote_bookmarks() =
        runTest(testDispatcher) {
            fakeRemote.remoteBookmarks =
                listOf(
                    RemoteBrowserBookmark(
                        id = "remote-1",
                        ownerId = "test-id",
                        url = "https://food.com/",
                        title = "Food",
                        clientId = "client-1",
                    )
                )

            fakeAuth.setAuthenticated()

            bookmarks().single().title shouldBe "Food"
            row("https://food.com/").remoteId shouldBe "remote-1"
        }

    @Test
    fun pull_adopts_remoteId_onto_local_bookmark_for_same_url() =
        runTest(testDispatcher) {
            // Starred offline on this device and on another device: one bookmark, not two.
            repository.addBookmark("https://food.com/", "Food")
            fakeRemote.remoteBookmarks =
                listOf(
                    RemoteBrowserBookmark(
                        id = "remote-9",
                        ownerId = "test-id",
                        url = "https://food.com/",
                        title = "Food",
                        clientId = "other-device-client-id",
                    )
                )
            // Fail the push so the local row is still unsynced when the pull runs.
            fakeRemote.failUpserts = true

            fakeAuth.setAuthenticated()

            bookmarks().size shouldBe 1
            row("https://food.com/").remoteId shouldBe "remote-9"
        }

    @Test
    fun sync_prunes_bookmarks_deleted_on_another_device() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            repository.addBookmark("https://keep.com/", "Keep")
            repository.addBookmark("https://gone.com/", "Gone")
            fakeRemote.remoteBookmarks = fakeRemote.upserts.filter { it.url == "https://keep.com/" }

            // Re-authenticating triggers another sync pass.
            fakeAuth.setState(AuthState.Unauthenticated)
            fakeAuth.setAuthenticated()

            bookmarks().map { it.title } shouldBe listOf("Keep")
        }

    @Test
    fun removeBookmark_with_remoteId_deletes_on_remote() =
        runTest(testDispatcher) {
            fakeAuth.setAuthenticated()
            repository.addBookmark("https://example.com/", "Example")

            repository.removeBookmark(bookmarks().single().id)

            bookmarks() shouldBe emptyList()
            fakeRemote.deletes shouldBe listOf("remote-uid-1")
        }

    @Test
    fun clearLocalData_removes_everything() =
        runTest(testDispatcher) {
            repository.addBookmark("https://a.com/", "A")
            repository.addBookmark("https://b.com/", "B")

            repository.clearLocalData()

            bookmarks() shouldBe emptyList()
        }

    /**
     * Records upserts/deletes and echoes back a synthesized remote id (`remote-<clientId>`), the
     * way Supabase would after an insert. [remoteBookmarks] seeds what a pull returns.
     */
    private class RecordingRemote : BrowserBookmarkRemoteDataSource {
        val upserts = mutableListOf<RemoteBrowserBookmark>()
        val deletes = mutableListOf<String>()
        var remoteBookmarks: List<RemoteBrowserBookmark> = emptyList()
        var failUpserts = false

        override suspend fun upsertBookmark(
            bookmark: RemoteBrowserBookmark
        ): RemoteBrowserBookmark {
            if (failUpserts) error("offline")
            return bookmark.copy(id = bookmark.id ?: "remote-${bookmark.clientId}").also {
                upserts.add(it)
            }
        }

        override suspend fun deleteBookmark(remoteId: String) {
            deletes.add(remoteId)
        }

        override suspend fun fetchAllBookmarks(ownerId: String): List<RemoteBrowserBookmark> =
            remoteBookmarks
    }
}

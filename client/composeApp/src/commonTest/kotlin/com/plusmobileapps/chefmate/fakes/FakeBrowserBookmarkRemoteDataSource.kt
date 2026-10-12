package com.plusmobileapps.chefmate.fakes

import com.plusmobileapps.chefmate.browser.impl.remote.BrowserBookmarkRemoteDataSource
import com.plusmobileapps.chefmate.browser.impl.remote.RemoteBrowserBookmark
import com.plusmobileapps.chefmate.browser.impl.remote.SupabaseBrowserBookmarkRemoteDataSource
import com.plusmobileapps.chefmate.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/** In-memory stand-in for the `browser_bookmarks` table, keyed by remote id. */
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(
    scope = AppScope::class,
    replaces = [SupabaseBrowserBookmarkRemoteDataSource::class],
)
class FakeBrowserBookmarkRemoteDataSource : BrowserBookmarkRemoteDataSource {
    private val rows = linkedMapOf<String, RemoteBrowserBookmark>()

    override suspend fun upsertBookmark(bookmark: RemoteBrowserBookmark): RemoteBrowserBookmark {
        val stored = bookmark.copy(id = bookmark.id ?: "remote-${bookmark.clientId}")
        rows[stored.id!!] = stored
        return stored
    }

    override suspend fun deleteBookmark(remoteId: String) {
        rows.remove(remoteId)
    }

    override suspend fun fetchAllBookmarks(ownerId: String): List<RemoteBrowserBookmark> =
        rows.values.filter { it.ownerId == ownerId }
}

package com.plusmobileapps.chefmate.browser.impl.remote

interface BrowserBookmarkRemoteDataSource {
    suspend fun upsertBookmark(bookmark: RemoteBrowserBookmark): RemoteBrowserBookmark

    suspend fun deleteBookmark(remoteId: String)

    suspend fun fetchAllBookmarks(ownerId: String): List<RemoteBrowserBookmark>
}

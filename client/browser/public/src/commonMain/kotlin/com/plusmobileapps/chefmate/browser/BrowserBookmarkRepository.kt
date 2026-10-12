package com.plusmobileapps.chefmate.browser

import kotlinx.coroutines.flow.Flow

/** A site the user starred from the in-app browser. */
data class BrowserBookmark(val id: Long, val url: String, val title: String)

/**
 * The user's browser bookmarks. Offline-first: every change lands in the local database first, and
 * is pushed to the user's account in the background when they're signed in.
 */
interface BrowserBookmarkRepository {
    /** All bookmarks, oldest first so new ones append to the end of the landing grid. */
    fun observeBookmarks(): Flow<List<BrowserBookmark>>

    /**
     * Bookmarks [url]. A no-op when the page is already bookmarked. [title] falls back to the URL's
     * host when blank.
     */
    suspend fun addBookmark(url: String, title: String?)

    suspend fun removeBookmark(id: Long)

    /** Removes the bookmark for [url], if any — the browser's star toggle works by page, not id. */
    suspend fun removeBookmarkByUrl(url: String)

    suspend fun clearLocalData()
}

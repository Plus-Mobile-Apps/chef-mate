package com.plusmobileapps.chefmate.browser.testing

import com.plusmobileapps.chefmate.browser.BrowserBookmark
import com.plusmobileapps.chefmate.browser.BrowserBookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [BrowserBookmarkRepository]. URLs are matched verbatim — no normalization. */
class FakeBrowserBookmarkRepository(initial: List<BrowserBookmark> = emptyList()) :
    BrowserBookmarkRepository {
    val bookmarks = MutableStateFlow(initial)
    var clearLocalDataCount = 0
        private set

    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override fun observeBookmarks(): Flow<List<BrowserBookmark>> = bookmarks

    override suspend fun addBookmark(url: String, title: String?) {
        if (bookmarks.value.any { it.url == url }) return
        bookmarks.value += BrowserBookmark(id = nextId++, url = url, title = title ?: url)
    }

    override suspend fun removeBookmark(id: Long) {
        bookmarks.value = bookmarks.value.filterNot { it.id == id }
    }

    override suspend fun removeBookmarkByUrl(url: String) {
        bookmarks.value = bookmarks.value.filterNot { it.url == url }
    }

    override suspend fun clearLocalData() {
        clearLocalDataCount++
        bookmarks.value = emptyList()
    }
}

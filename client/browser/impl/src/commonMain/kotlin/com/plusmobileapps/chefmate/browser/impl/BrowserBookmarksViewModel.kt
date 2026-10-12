package com.plusmobileapps.chefmate.browser.impl

import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.browser.BrowserBookmark
import com.plusmobileapps.chefmate.browser.BrowserBookmarkRepository
import com.plusmobileapps.chefmate.browser.BrowserBookmarksBloc.Model
import com.plusmobileapps.chefmate.di.Main
import dev.zacsweers.metro.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
class BrowserBookmarksViewModel(
    @Main mainContext: CoroutineContext,
    private val bookmarkRepository: BrowserBookmarkRepository,
) : ViewModel(mainContext) {

    private val _state = MutableStateFlow(Model())
    val state: StateFlow<Model> = _state.asStateFlow()

    init {
        scope.launch {
            bookmarkRepository.observeBookmarks().collect { bookmarks ->
                _state.update { current ->
                    // Drop selections for bookmarks that vanished, e.g. deleted by a sync.
                    val survivingIds = bookmarks.mapTo(mutableSetOf()) { it.id }
                    current.copy(
                        bookmarks = bookmarks,
                        isLoading = false,
                        selectedIds = current.selectedIds intersect survivingIds,
                    )
                }
            }
        }
    }

    /**
     * Flips [bookmark]'s selection. Outside selection mode a tap would otherwise do nothing on a
     * screen whose only job is removal, so it starts selecting with that row instead.
     */
    fun toggleSelection(bookmark: BrowserBookmark) {
        _state.update { current ->
            val next =
                if (bookmark.id in current.selectedIds) current.selectedIds - bookmark.id
                else current.selectedIds + bookmark.id
            // Stay in selection mode at zero selected; the user leaves it via the close icon.
            current.copy(selectionMode = true, selectedIds = next)
        }
    }

    fun enterSelectionWith(bookmark: BrowserBookmark) {
        _state.update { it.copy(selectionMode = true, selectedIds = it.selectedIds + bookmark.id) }
    }

    fun enterSelectionMode() {
        _state.update { it.copy(selectionMode = true) }
    }

    fun toggleSelectAll() {
        _state.update { current ->
            current.copy(
                selectedIds =
                    if (current.allSelected) emptySet()
                    else current.bookmarks.mapTo(mutableSetOf()) { it.id }
            )
        }
    }

    fun cancelSelection() {
        _state.update { it.copy(selectionMode = false, selectedIds = emptySet()) }
    }

    fun requestDeleteSelected() {
        _state.update { current ->
            if (current.selectedIds.isEmpty()) current
            else current.copy(pendingDeleteCount = current.selectedIds.size)
        }
    }

    fun dismissDelete() {
        _state.update { it.copy(pendingDeleteCount = null) }
    }

    fun confirmDeleteSelected() {
        val ids = _state.value.selectedIds.toList()
        _state.update {
            it.copy(pendingDeleteCount = null, selectionMode = false, selectedIds = emptySet())
        }
        if (ids.isEmpty()) return
        scope.launch { ids.forEach { bookmarkRepository.removeBookmark(it) } }
    }
}

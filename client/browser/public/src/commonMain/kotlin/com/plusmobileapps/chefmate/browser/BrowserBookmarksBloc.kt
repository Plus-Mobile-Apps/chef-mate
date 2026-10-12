package com.plusmobileapps.chefmate.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.coroutines.flow.StateFlow

/** Settings screen for reviewing the user's browser bookmarks and removing them in bulk. */
interface BrowserBookmarksBloc : BackHandlerOwner, BackClickBloc, ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        BrowserBookmarksScreen(bloc = this, modifier = modifier)
    }

    /** Toggles [bookmark]'s selection, entering selection mode first if needed. */
    fun onBookmarkClicked(bookmark: BrowserBookmark)

    /** Enters selection mode with [bookmark] already selected. */
    fun onBookmarkLongClicked(bookmark: BrowserBookmark)

    fun onSelectModeClicked()

    fun onSelectAllClicked()

    fun onCancelSelection()

    fun onDeleteSelectedClicked()

    fun onDeleteSelectedConfirmed()

    fun onDeleteSelectedDismissed()

    data class Model(
        val bookmarks: List<BrowserBookmark> = emptyList(),
        val isLoading: Boolean = true,
        val selectionMode: Boolean = false,
        val selectedIds: Set<Long> = emptySet(),
        /** Number of bookmarks the open confirmation dialog will delete, or null when closed. */
        val pendingDeleteCount: Int? = null,
    ) {
        val selectedCount: Int
            get() = selectedIds.size

        val allSelected: Boolean
            get() = bookmarks.isNotEmpty() && selectedIds.size == bookmarks.size
    }

    sealed class Output {
        data object Back : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): BrowserBookmarksBloc
    }
}

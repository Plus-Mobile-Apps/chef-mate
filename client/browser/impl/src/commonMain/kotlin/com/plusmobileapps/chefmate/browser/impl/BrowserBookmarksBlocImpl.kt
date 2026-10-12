package com.plusmobileapps.chefmate.browser.impl

import com.arkivanov.essenty.backhandler.BackCallback
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.browser.BrowserBookmark
import com.plusmobileapps.chefmate.browser.BrowserBookmarksBloc
import com.plusmobileapps.chefmate.browser.BrowserBookmarksBloc.Output
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = BrowserBookmarksBloc.Factory::class,
)
class BrowserBookmarksBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: Provider<BrowserBookmarksViewModel>,
) : BrowserBookmarksBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<BrowserBookmarksBloc.Model> = viewModel.state

    // System back leaves selection mode first; only enabled while selecting so the settings stack
    // handles back otherwise.
    private val selectionBackCallback =
        BackCallback(isEnabled = false) { viewModel.cancelSelection() }

    init {
        backHandler.register(selectionBackCallback)
        viewModel.state
            .map { it.selectionMode }
            .distinctUntilChanged()
            .onEach { selectionBackCallback.isEnabled = it }
            .launchIn(createScope())
    }

    override fun onBackClicked() {
        if (state.value.selectionMode) {
            viewModel.cancelSelection()
        } else {
            output.onNext(Output.Back)
        }
    }

    override fun onBookmarkClicked(bookmark: BrowserBookmark) {
        viewModel.toggleSelection(bookmark)
    }

    override fun onBookmarkLongClicked(bookmark: BrowserBookmark) {
        viewModel.enterSelectionWith(bookmark)
    }

    override fun onSelectModeClicked() {
        viewModel.enterSelectionMode()
    }

    override fun onSelectAllClicked() {
        viewModel.toggleSelectAll()
    }

    override fun onCancelSelection() {
        viewModel.cancelSelection()
    }

    override fun onDeleteSelectedClicked() {
        viewModel.requestDeleteSelected()
    }

    override fun onDeleteSelectedConfirmed() {
        viewModel.confirmDeleteSelected()
    }

    override fun onDeleteSelectedDismissed() {
        viewModel.dismissDelete()
    }
}

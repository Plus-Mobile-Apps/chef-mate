package com.plusmobileapps.chefmate.browser.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.browser.BrowserEditQueryBloc
import com.plusmobileapps.chefmate.browser.BrowserHistoryEntry
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = BrowserEditQueryBloc.Factory::class,
)
class BrowserEditQueryBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<BrowserEditQueryBloc.Output>,
    @Assisted initialText: String,
    viewModelFactory: () -> BrowserEditQueryViewModel,
) : BrowserEditQueryBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel {
        viewModelFactory().also {
            if (initialText.isNotEmpty()) it.onSearchTextChanged(initialText)
        }
    }

    override val state: StateFlow<BrowserEditQueryBloc.Model> = viewModel.state

    override fun onSearchTextChanged(text: String) {
        viewModel.onSearchTextChanged(text)
    }

    override fun onNavigate() {
        val url = viewModel.state.value.searchText
        if (url.isBlank()) return
        output.onNext(
            BrowserEditQueryBloc.Output.Navigate(url.toNavigationUrl(viewModel.currentSearchEngine))
        )
    }

    override fun onCancel() {
        output.onNext(BrowserEditQueryBloc.Output.Cancel)
    }

    override fun onHistoryItemClicked(entry: BrowserHistoryEntry) {
        output.onNext(BrowserEditQueryBloc.Output.Navigate(entry.url))
    }

    override fun onHistoryItemDeleteClicked(entry: BrowserHistoryEntry) {
        viewModel.deleteHistoryEntry(entry)
    }
}

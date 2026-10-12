package com.plusmobileapps.chefmate.grocery.autocomplete.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.grocery.autocomplete.GroceryAutocompleteBloc
import com.plusmobileapps.chefmate.grocery.autocomplete.GroceryAutocompleteBloc.Item
import com.plusmobileapps.chefmate.grocery.autocomplete.GroceryAutocompleteBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = GroceryAutocompleteBloc.Factory::class,
)
class GroceryAutocompleteBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: () -> GroceryAutocompleteViewModel,
) : GroceryAutocompleteBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<GroceryAutocompleteBloc.Model> = viewModel.state

    override fun onBackClicked() {
        output.onNext(Output.Back)
    }

    override fun onCreateClicked() {
        viewModel.openCreateField()
    }

    override fun onCreateCancelled() {
        viewModel.closeCreateField()
    }

    override fun onCreateTextChanged(text: String) {
        viewModel.updateCreateText(text)
    }

    override fun onCreateSubmitted() {
        viewModel.submitCreate()
    }

    override fun onDeleteRequested(item: Item) {
        viewModel.showDeleteDialog(item)
    }

    override fun onDeleteConfirmed() {
        viewModel.confirmDelete()
    }

    override fun onDeleteDismissed() {
        viewModel.dismissDialog()
    }
}

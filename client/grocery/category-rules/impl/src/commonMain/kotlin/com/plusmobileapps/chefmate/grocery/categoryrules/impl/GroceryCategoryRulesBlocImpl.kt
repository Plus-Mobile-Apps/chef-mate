package com.plusmobileapps.chefmate.grocery.categoryrules.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.Output
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.Rule
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = GroceryCategoryRulesBloc.Factory::class,
)
class GroceryCategoryRulesBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: Provider<GroceryCategoryRulesViewModel>,
) : GroceryCategoryRulesBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<GroceryCategoryRulesBloc.Model> = viewModel.state

    override fun onBackClicked() {
        output.onNext(Output.Back)
    }

    override fun onCreateClicked() {
        viewModel.openCreateField()
    }

    override fun onCreateCancelled() {
        viewModel.closeCreateField()
    }

    override fun onCreateNameChanged(name: String) {
        viewModel.updateCreateName(name)
    }

    override fun onCreateCategorySelected(category: GroceryCategory) {
        viewModel.updateCreateCategory(category)
    }

    override fun onCreateSubmitted() {
        viewModel.submitCreate()
    }

    override fun onDeleteRequested(rule: Rule) {
        viewModel.showDeleteDialog(rule)
    }

    override fun onDeleteConfirmed() {
        viewModel.confirmDelete()
    }

    override fun onDeleteDismissed() {
        viewModel.dismissDialog()
    }
}

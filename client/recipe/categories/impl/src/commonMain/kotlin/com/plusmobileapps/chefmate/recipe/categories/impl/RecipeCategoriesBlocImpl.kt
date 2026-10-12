package com.plusmobileapps.chefmate.recipe.categories.impl

import com.arkivanov.essenty.backhandler.BackCallback
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.recipe.categories.RecipeCategoriesBloc
import com.plusmobileapps.chefmate.recipe.categories.RecipeCategoriesBloc.CategoryItem
import com.plusmobileapps.chefmate.recipe.categories.RecipeCategoriesBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = RecipeCategoriesBloc.Factory::class,
)
class RecipeCategoriesBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: () -> RecipeCategoriesViewModel,
) : RecipeCategoriesBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<RecipeCategoriesBloc.Model> = viewModel.state

    // Consumes system back during selection mode so the user exits selection instead of popping
    // the whole screen. Disabled when selection is empty so Decompose's nav stack handles back.
    private val selectionBackCallback =
        BackCallback(isEnabled = false) { viewModel.cancelSelection() }

    init {
        backHandler.register(selectionBackCallback)
        val scope = createScope()
        viewModel.state
            .map { it.selectionMode }
            .distinctUntilChanged()
            .onEach { selectionBackCallback.isEnabled = it }
            .launchIn(scope)
    }

    override fun onBackClicked() {
        if (state.value.selectionMode) {
            viewModel.cancelSelection()
        } else {
            output.onNext(Output.Back)
        }
    }

    override fun onCategoryClicked(item: CategoryItem) {
        if (state.value.selectionMode) {
            viewModel.toggleSelection(item)
        }
        // Outside selection mode, single-tap is currently a no-op (no per-category detail screen).
    }

    override fun onCategoryLongClicked(item: CategoryItem) {
        viewModel.enterSelectionWith(item)
    }

    override fun onSelectModeClicked() {
        viewModel.enterSelectionMode()
    }

    override fun onCancelSelection() {
        viewModel.cancelSelection()
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

    override fun onRenameRequested(item: CategoryItem) {
        viewModel.showRenameDialog(item)
    }

    override fun onRenameSubmitted(id: Long, newName: String) {
        viewModel.submitRename(id, newName)
    }

    override fun onRenameDismissed() {
        viewModel.dismissDialog()
    }

    override fun onDeleteRequested(item: CategoryItem) {
        viewModel.showDeleteDialog(item)
    }

    override fun onDeleteConfirmed() {
        viewModel.confirmDelete()
    }

    override fun onDeleteDismissed() {
        viewModel.dismissDialog()
    }

    override fun onBulkDeleteRequested() {
        viewModel.showBulkDeleteDialog()
    }

    override fun onBulkDeleteConfirmed() {
        viewModel.confirmBulkDelete()
    }

    override fun onBulkDeleteDismissed() {
        viewModel.dismissDialog()
    }
}

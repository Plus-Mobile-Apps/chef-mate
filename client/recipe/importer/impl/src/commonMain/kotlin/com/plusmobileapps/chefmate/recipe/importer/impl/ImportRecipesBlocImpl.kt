package com.plusmobileapps.chefmate.recipe.importer.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.recipe.importer.ImportRecipesBloc
import com.plusmobileapps.chefmate.recipe.importer.ImportRecipesBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = ImportRecipesBloc.Factory::class,
)
class ImportRecipesBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: () -> ImportRecipesViewModel,
) : ImportRecipesBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<ImportRecipesBloc.Model> = viewModel.state

    override val recipeBooks = viewModel.recipeBooks

    override val selectedBookIds = viewModel.selectedBookIds

    override fun onArchiveSelected(bytes: ByteArray, fileName: String) =
        viewModel.onArchiveSelected(bytes, fileName)

    override fun onRecipeToggled(id: String) = viewModel.onRecipeToggled(id)

    override fun onToggleSelectAll() = viewModel.onToggleSelectAll()

    override fun onToggleBook(bookId: Long) = viewModel.onToggleBook(bookId)

    override fun onImportClicked() = viewModel.onImportClicked()

    override fun onStartOver() = viewModel.onStartOver()

    override fun onBack() {
        output.onNext(Output.Back)
    }
}

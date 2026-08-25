package com.plusmobileapps.chefmate.settings.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.settings.AppSettingsBloc
import com.plusmobileapps.chefmate.settings.AppSettingsBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.Provider
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = AppSettingsBloc.Factory::class,
)
class AppSettingsBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: Provider<AppSettingsViewModel>,
) : AppSettingsBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<AppSettingsBloc.Model> = viewModel.state

    override fun onBack() {
        output.onNext(Output.Back)
    }

    override fun onHistoryEnabledChanged(enabled: Boolean) {
        viewModel.setHistoryEnabled(enabled)
    }

    override fun onDefaultSearchEngineClicked() {
        viewModel.showSearchEnginePicker()
    }

    override fun onSearchEngineSelected(engine: SearchEngine) {
        viewModel.setDefaultSearchEngine(engine)
    }

    override fun onSearchEnginePickerDismissed() {
        viewModel.dismissSearchEnginePicker()
    }

    override fun onClearHistoryClicked() {
        viewModel.showClearHistoryDialog()
    }

    override fun onClearHistoryConfirmed() {
        viewModel.clearHistory()
    }

    override fun onClearHistoryDismissed() {
        viewModel.dismissClearHistoryDialog()
    }

    override fun onBottomNavOrderClicked() {
        output.onNext(Output.OpenBottomNavOrder)
    }

    override fun onImportRecipesClicked() {
        output.onNext(Output.OpenImportRecipes)
    }

    override fun onExportRecipesClicked() {
        output.onNext(Output.OpenExportRecipes)
    }

    override fun onRecipeCategoriesClicked() {
        output.onNext(Output.OpenRecipeCategories)
    }

    override fun onGroceryAutocompleteClicked() {
        output.onNext(Output.OpenGroceryAutocomplete)
    }

    override fun onGroceryCategoryRulesClicked() {
        output.onNext(Output.OpenGroceryCategoryRules)
    }
}

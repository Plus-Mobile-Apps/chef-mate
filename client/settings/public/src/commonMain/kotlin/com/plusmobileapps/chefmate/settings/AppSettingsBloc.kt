package com.plusmobileapps.chefmate.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.settings.ui.AppSettingsScreen
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.coroutines.flow.StateFlow

interface AppSettingsBloc : BackHandlerOwner, ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        AppSettingsScreen(bloc = this, modifier = modifier)
    }

    fun onBack()

    fun onHistoryEnabledChanged(enabled: Boolean)

    fun onDefaultSearchEngineClicked()

    fun onSearchEngineSelected(engine: SearchEngine)

    fun onSearchEnginePickerDismissed()

    fun onClearHistoryClicked()

    fun onClearHistoryConfirmed()

    fun onClearHistoryDismissed()

    fun onBottomNavOrderClicked()

    fun onImportRecipesClicked()

    fun onExportRecipesClicked()

    fun onRecipeCategoriesClicked()

    fun onGroceryAutocompleteClicked()

    fun onGroceryCategoryRulesClicked()

    data class Model(
        val isHistoryEnabled: Boolean = true,
        val showClearHistoryDialog: Boolean = false,
        /** The current default search engine, or null until the user picks one. */
        val selectedSearchEngine: SearchEngine? = null,
        val showSearchEnginePicker: Boolean = false,
    )

    sealed class Output {
        data object Back : Output()

        data object OpenBottomNavOrder : Output()

        data object OpenImportRecipes : Output()

        data object OpenExportRecipes : Output()

        data object OpenRecipeCategories : Output()

        data object OpenGroceryAutocomplete : Output()

        data object OpenGroceryCategoryRules : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): AppSettingsBloc
    }
}

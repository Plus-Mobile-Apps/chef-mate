package com.plusmobileapps.chefmate.settings.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackHandler
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.settings.AppSettingsBloc
import com.plusmobileapps.chefmate.settings.ui.AppSettingsScreen
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.coroutines.flow.MutableStateFlow

private fun appSettingsBloc(model: AppSettingsBloc.Model): AppSettingsBloc =
    object : AppSettingsBloc {
        override val backHandler: BackHandler = BackDispatcher()
        override val state = MutableStateFlow(model)

        override fun onBack() = Unit

        override fun onHistoryEnabledChanged(enabled: Boolean) = Unit

        override fun onDefaultSearchEngineClicked() = Unit

        override fun onSearchEngineSelected(engine: SearchEngine) = Unit

        override fun onSearchEnginePickerDismissed() = Unit

        override fun onClearHistoryClicked() = Unit

        override fun onClearHistoryConfirmed() = Unit

        override fun onClearHistoryDismissed() = Unit

        override fun onBottomNavOrderClicked() = Unit

        override fun onImportRecipesClicked() = Unit

        override fun onExportRecipesClicked() = Unit

        override fun onRecipeCategoriesClicked() = Unit

        override fun onGroceryAutocompleteClicked() = Unit

        override fun onGroceryCategoryRulesClicked() = Unit
    }

val previewAppSettingsBloc: AppSettingsBloc =
    appSettingsBloc(
        AppSettingsBloc.Model(
            isHistoryEnabled = true,
            showClearHistoryDialog = false,
            selectedSearchEngine = SearchEngine.GOOGLE,
        )
    )

val previewAppSettingsEnginePickerBloc: AppSettingsBloc =
    appSettingsBloc(
        AppSettingsBloc.Model(
            isHistoryEnabled = true,
            selectedSearchEngine = SearchEngine.DUCK_DUCK_GO,
            showSearchEnginePicker = true,
        )
    )

@Preview(showBackground = true)
@Composable
internal fun AppSettingsScreenPreview() {
    ChefMateTheme { AppSettingsScreen(bloc = previewAppSettingsBloc) }
}

@Preview(showBackground = true)
@Composable
internal fun AppSettingsScreenDarkPreview() {
    ChefMateTheme(darkTheme = true) { AppSettingsScreen(bloc = previewAppSettingsBloc) }
}

@Preview(showBackground = true)
@Composable
internal fun AppSettingsScreenEnginePickerPreview() {
    ChefMateTheme { AppSettingsScreen(bloc = previewAppSettingsEnginePickerBloc) }
}

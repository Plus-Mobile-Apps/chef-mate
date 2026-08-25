@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.settings.impl

import com.plusmobileapps.chefmate.browser.BrowserHistoryEntry
import com.plusmobileapps.chefmate.browser.BrowserHistoryRepository
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.browser.testing.FakeBrowserPreferences
import com.plusmobileapps.chefmate.settings.AppSettingsBloc
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class AppSettingsBlocImplTest {

    private val context = TestBlocContext.create()
    private val output = TestConsumer<AppSettingsBloc.Output>()
    private val browserPreferences = FakeBrowserPreferences()
    private val historyRepository: BrowserHistoryRepository =
        mock(
            block = {
                everySuspend { observeRecent(any()) } returns
                    MutableStateFlow<List<BrowserHistoryEntry>>(emptyList())
                everySuspend { clearAll() } returns Unit
            }
        )

    private fun createBloc() =
        AppSettingsBlocImpl(
            context = context,
            output = output,
            viewModelFactory = {
                AppSettingsViewModel(
                    mainContext = context.mainContext,
                    browserPreferences = browserPreferences,
                    historyRepository = historyRepository,
                )
            },
        )

    @Test
    fun initial_state_reflects_history_enabled_preference() {
        browserPreferences.setHistoryEnabled(false)
        val bloc = createBloc()
        bloc.state.value.isHistoryEnabled shouldBe false
    }

    @Test
    fun When_history_toggled_off_Then_preferences_updated() {
        val bloc = createBloc()
        bloc.onHistoryEnabledChanged(false)
        browserPreferences.isHistoryEnabled.value shouldBe false
        bloc.state.value.isHistoryEnabled shouldBe false
    }

    @Test
    fun When_clear_history_clicked_Then_dialog_shown() {
        val bloc = createBloc()
        bloc.onClearHistoryClicked()
        bloc.state.value.showClearHistoryDialog shouldBe true
    }

    @Test
    fun When_clear_history_dismissed_Then_dialog_hidden() {
        val bloc = createBloc()
        bloc.onClearHistoryClicked()
        bloc.onClearHistoryDismissed()
        bloc.state.value.showClearHistoryDialog shouldBe false
    }

    @Test
    fun When_clear_history_confirmed_Then_repository_clearAll_called() = runTest {
        val bloc = createBloc()
        bloc.onClearHistoryClicked()
        bloc.onClearHistoryConfirmed()
        bloc.state.value.showClearHistoryDialog shouldBe false
        verifySuspend { historyRepository.clearAll() }
    }

    @Test
    fun initial_state_reflects_default_search_engine_preference() {
        browserPreferences.setDefaultSearchEngine(SearchEngine.BING)
        val bloc = createBloc()
        bloc.state.value.selectedSearchEngine shouldBe SearchEngine.BING
    }

    @Test
    fun When_default_search_engine_clicked_Then_picker_shown() {
        val bloc = createBloc()
        bloc.onDefaultSearchEngineClicked()
        bloc.state.value.showSearchEnginePicker shouldBe true
    }

    @Test
    fun When_search_engine_selected_Then_preference_updated_and_picker_hidden() {
        val bloc = createBloc()
        bloc.onDefaultSearchEngineClicked()
        bloc.onSearchEngineSelected(SearchEngine.BRAVE)
        browserPreferences.defaultSearchEngine.value shouldBe SearchEngine.BRAVE
        bloc.state.value.selectedSearchEngine shouldBe SearchEngine.BRAVE
        bloc.state.value.showSearchEnginePicker shouldBe false
    }

    @Test
    fun When_search_engine_picker_dismissed_Then_picker_hidden() {
        val bloc = createBloc()
        bloc.onDefaultSearchEngineClicked()
        bloc.onSearchEnginePickerDismissed()
        bloc.state.value.showSearchEnginePicker shouldBe false
    }

    @Test
    fun When_back_clicked_Then_back_output_emitted() {
        val bloc = createBloc()
        bloc.onBack()
        output.lastValue shouldBe AppSettingsBloc.Output.Back
    }

    @Test
    fun When_bottom_nav_order_clicked_Then_open_bottom_nav_order_output_emitted() {
        val bloc = createBloc()
        bloc.onBottomNavOrderClicked()
        output.lastValue shouldBe AppSettingsBloc.Output.OpenBottomNavOrder
    }

    @Test
    fun When_import_recipes_clicked_Then_open_import_recipes_output_emitted() {
        val bloc = createBloc()
        bloc.onImportRecipesClicked()
        output.lastValue shouldBe AppSettingsBloc.Output.OpenImportRecipes
    }

    @Test
    fun When_export_recipes_clicked_Then_open_export_recipes_output_emitted() {
        val bloc = createBloc()
        bloc.onExportRecipesClicked()
        output.lastValue shouldBe AppSettingsBloc.Output.OpenExportRecipes
    }

    @Test
    fun When_grocery_category_rules_clicked_Then_open_grocery_category_rules_output_emitted() {
        val bloc = createBloc()
        bloc.onGroceryCategoryRulesClicked()
        output.lastValue shouldBe AppSettingsBloc.Output.OpenGroceryCategoryRules
    }
}

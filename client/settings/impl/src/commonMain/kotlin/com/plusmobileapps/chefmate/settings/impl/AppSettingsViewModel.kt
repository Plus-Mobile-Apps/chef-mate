package com.plusmobileapps.chefmate.settings.impl

import com.plusmobileapps.chefmate.Platform
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.browser.BrowserHistoryRepository
import com.plusmobileapps.chefmate.browser.BrowserPreferences
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.currentPlatform
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.settings.AppSettingsBloc
import com.plusmobileapps.chefmate.settings.BetaPreferences
import dev.zacsweers.metro.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
class AppSettingsViewModel(
    @Main mainContext: CoroutineContext,
    private val browserPreferences: BrowserPreferences,
    private val historyRepository: BrowserHistoryRepository,
    private val betaPreferences: BetaPreferences,
    platform: Platform = currentPlatform,
) : ViewModel(mainContext) {

    private val _state =
        MutableStateFlow(AppSettingsBloc.Model(showBetaSection = platform == Platform.IOS))
    val state: StateFlow<AppSettingsBloc.Model> = _state.asStateFlow()

    init {
        browserPreferences.isHistoryEnabled
            .onEach { enabled -> _state.update { it.copy(isHistoryEnabled = enabled) } }
            .launchIn(scope)
        browserPreferences.defaultSearchEngine
            .onEach { engine -> _state.update { it.copy(selectedSearchEngine = engine) } }
            .launchIn(scope)
        betaPreferences.isSwiftUiRedesignEnabled
            .onEach { enabled -> _state.update { it.copy(isSwiftUiRedesignEnabled = enabled) } }
            .launchIn(scope)
    }

    fun setSwiftUiRedesignEnabled(enabled: Boolean) {
        betaPreferences.setSwiftUiRedesignEnabled(enabled)
    }

    fun setHistoryEnabled(enabled: Boolean) {
        browserPreferences.setHistoryEnabled(enabled)
    }

    fun showSearchEnginePicker() {
        _state.update { it.copy(showSearchEnginePicker = true) }
    }

    fun dismissSearchEnginePicker() {
        _state.update { it.copy(showSearchEnginePicker = false) }
    }

    fun setDefaultSearchEngine(engine: SearchEngine) {
        browserPreferences.setDefaultSearchEngine(engine)
        _state.update { it.copy(showSearchEnginePicker = false) }
    }

    fun showClearHistoryDialog() {
        _state.update { it.copy(showClearHistoryDialog = true) }
    }

    fun dismissClearHistoryDialog() {
        _state.update { it.copy(showClearHistoryDialog = false) }
    }

    fun clearHistory() {
        _state.update { it.copy(showClearHistoryDialog = false) }
        scope.launch { historyRepository.clearAll() }
    }
}

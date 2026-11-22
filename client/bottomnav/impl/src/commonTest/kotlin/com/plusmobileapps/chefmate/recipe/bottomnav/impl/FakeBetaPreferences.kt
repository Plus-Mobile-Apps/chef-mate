package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import com.plusmobileapps.chefmate.settings.BetaPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeBetaPreferences(
    swiftUiRedesignEnabled: Boolean = false,
    promptSeen: Boolean = false,
) : BetaPreferences {
    private val _isSwiftUiRedesignEnabled = MutableStateFlow(swiftUiRedesignEnabled)
    override val isSwiftUiRedesignEnabled: StateFlow<Boolean> =
        _isSwiftUiRedesignEnabled.asStateFlow()

    override fun setSwiftUiRedesignEnabled(enabled: Boolean) {
        _isSwiftUiRedesignEnabled.value = enabled
    }

    override var hasSeenSwiftUiRedesignPrompt: Boolean = promptSeen
        private set

    override fun markSwiftUiRedesignPromptSeen() {
        hasSeenSwiftUiRedesignPrompt = true
    }
}

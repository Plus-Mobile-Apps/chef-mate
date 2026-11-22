package com.plusmobileapps.chefmate.settings.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.settings.BetaPreferences
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val BETA_SWIFTUI_REDESIGN_KEY = "beta.swiftui_redesign"
internal const val BETA_SWIFTUI_REDESIGN_PROMPT_SEEN_KEY = "beta.swiftui_redesign_prompt_seen"

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class BetaPreferencesImpl(private val settings: Settings) : BetaPreferences {

    private val _isSwiftUiRedesignEnabled =
        MutableStateFlow(settings.getBoolean(BETA_SWIFTUI_REDESIGN_KEY, defaultValue = false))

    override val isSwiftUiRedesignEnabled: StateFlow<Boolean> =
        _isSwiftUiRedesignEnabled.asStateFlow()

    override fun setSwiftUiRedesignEnabled(enabled: Boolean) {
        settings.putBoolean(BETA_SWIFTUI_REDESIGN_KEY, enabled)
        _isSwiftUiRedesignEnabled.value = enabled
    }

    override val hasSeenSwiftUiRedesignPrompt: Boolean
        get() = settings.getBoolean(BETA_SWIFTUI_REDESIGN_PROMPT_SEEN_KEY, defaultValue = false)

    override fun markSwiftUiRedesignPromptSeen() {
        settings.putBoolean(BETA_SWIFTUI_REDESIGN_PROMPT_SEEN_KEY, true)
    }
}

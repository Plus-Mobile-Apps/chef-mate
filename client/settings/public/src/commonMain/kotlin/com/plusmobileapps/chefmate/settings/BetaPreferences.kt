package com.plusmobileapps.chefmate.settings

import kotlinx.coroutines.flow.StateFlow

/** Opt-in beta features the user toggles from Settings → Beta. Stored on this device only. */
interface BetaPreferences {
    /**
     * iOS only: launch the native SwiftUI shell instead of the Compose UI. Read once at launch, so
     * a change takes effect on the next cold start.
     */
    val isSwiftUiRedesignEnabled: StateFlow<Boolean>

    fun setSwiftUiRedesignEnabled(enabled: Boolean)

    /**
     * Whether the one-time "try the new iOS design" sheet on the home screen has been answered —
     * accepted or dismissed. Once true, the sheet never shows again.
     */
    val hasSeenSwiftUiRedesignPrompt: Boolean

    fun markSwiftUiRedesignPromptSeen()
}

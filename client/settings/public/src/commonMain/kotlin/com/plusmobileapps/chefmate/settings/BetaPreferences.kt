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
}

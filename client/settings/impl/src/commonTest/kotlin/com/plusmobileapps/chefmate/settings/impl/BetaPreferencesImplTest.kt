@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.settings.impl

import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BetaPreferencesImplTest {

    @Test
    fun swiftUiRedesign_defaults_to_false() {
        BetaPreferencesImpl(MapSettings()).isSwiftUiRedesignEnabled.value shouldBe false
    }

    @Test
    fun swiftUiRedesign_reads_existing_value_from_settings() {
        val settings = MapSettings()
        settings.putBoolean(BETA_SWIFTUI_REDESIGN_KEY, true)
        BetaPreferencesImpl(settings).isSwiftUiRedesignEnabled.value shouldBe true
    }

    @Test
    fun setSwiftUiRedesignEnabled_updates_state_flow_and_settings() {
        val settings = MapSettings()
        val preferences = BetaPreferencesImpl(settings)

        preferences.setSwiftUiRedesignEnabled(true)

        preferences.isSwiftUiRedesignEnabled.value shouldBe true
        settings.getBoolean(BETA_SWIFTUI_REDESIGN_KEY, defaultValue = false) shouldBe true
    }

    @Test
    fun swiftUiRedesignPrompt_is_unseen_by_default() {
        BetaPreferencesImpl(MapSettings()).hasSeenSwiftUiRedesignPrompt shouldBe false
    }

    @Test
    fun markSwiftUiRedesignPromptSeen_persists_across_instances() {
        val settings = MapSettings()
        BetaPreferencesImpl(settings).markSwiftUiRedesignPromptSeen()

        BetaPreferencesImpl(settings).hasSeenSwiftUiRedesignPrompt shouldBe true
    }
}

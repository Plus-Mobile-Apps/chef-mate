@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import com.plusmobileapps.chefmate.recipe.bottomnav.NAV_RAIL_COLLAPSED_KEY
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NavRailPreferencesImplTest {

    @Test
    fun defaults_to_expanded_when_no_persisted_value() {
        val prefs = NavRailPreferencesImpl(MapSettings())
        prefs.railCollapsed.value shouldBe false
    }

    @Test
    fun reads_persisted_value_from_settings() {
        val settings = MapSettings()
        settings.putBoolean(NAV_RAIL_COLLAPSED_KEY, true)
        NavRailPreferencesImpl(settings).railCollapsed.value shouldBe true
    }

    @Test
    fun set_rail_collapsed_persists_and_emits() {
        val settings = MapSettings()
        val prefs = NavRailPreferencesImpl(settings)

        prefs.setRailCollapsed(true)

        prefs.railCollapsed.value shouldBe true
        settings.getBoolean(NAV_RAIL_COLLAPSED_KEY, defaultValue = false) shouldBe true
        // A fresh instance — the next app launch — sees the saved value.
        NavRailPreferencesImpl(settings).railCollapsed.value shouldBe true
    }
}

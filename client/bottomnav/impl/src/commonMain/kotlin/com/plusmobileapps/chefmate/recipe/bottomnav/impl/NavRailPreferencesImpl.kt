package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.recipe.bottomnav.NAV_RAIL_COLLAPSED_KEY
import com.plusmobileapps.chefmate.recipe.bottomnav.NavRailPreferences
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NavRailPreferencesImpl(private val settings: Settings) : NavRailPreferences {

    private val _railCollapsed =
        MutableStateFlow(settings.getBoolean(NAV_RAIL_COLLAPSED_KEY, defaultValue = false))

    override val railCollapsed: StateFlow<Boolean> = _railCollapsed.asStateFlow()

    override fun setRailCollapsed(collapsed: Boolean) {
        settings.putBoolean(NAV_RAIL_COLLAPSED_KEY, collapsed)
        _railCollapsed.value = collapsed
    }
}

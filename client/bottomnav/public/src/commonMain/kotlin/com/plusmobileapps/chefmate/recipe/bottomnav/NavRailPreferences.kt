package com.plusmobileapps.chefmate.recipe.bottomnav

import kotlinx.coroutines.flow.StateFlow

const val NAV_RAIL_COLLAPSED_KEY = "bottomnav.rail.collapsed"

/** Persists whether the user has collapsed the expanded (wide-window) side rail. */
interface NavRailPreferences {
    val railCollapsed: StateFlow<Boolean>

    fun setRailCollapsed(collapsed: Boolean)
}

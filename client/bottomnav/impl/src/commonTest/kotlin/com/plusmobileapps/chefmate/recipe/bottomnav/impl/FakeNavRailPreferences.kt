package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import com.plusmobileapps.chefmate.recipe.bottomnav.NavRailPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeNavRailPreferences(initial: Boolean = false) : NavRailPreferences {
    private val _railCollapsed = MutableStateFlow(initial)
    override val railCollapsed: StateFlow<Boolean> = _railCollapsed.asStateFlow()

    override fun setRailCollapsed(collapsed: Boolean) {
        _railCollapsed.value = collapsed
    }
}

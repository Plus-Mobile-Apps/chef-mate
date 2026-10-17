package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.notifications.data.NotificationsRepository
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Tab.RECIPES
import com.plusmobileapps.chefmate.recipe.bottomnav.NavRailPreferences
import com.plusmobileapps.chefmate.recipe.bottomnav.TabOrderPreferences
import dev.zacsweers.metro.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

@Inject
class BottomNavViewModel(
    @Main mainContext: CoroutineContext,
    tabOrderPreferences: TabOrderPreferences,
    private val navRailPreferences: NavRailPreferences,
    notificationsRepository: NotificationsRepository,
) : ViewModel(mainContext) {
    private val _state =
        MutableStateFlow(
            State(
                tabs = tabOrderPreferences.tabOrder.value,
                railCollapsed = navRailPreferences.railCollapsed.value,
            )
        )
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        tabOrderPreferences.tabOrder
            .onEach { latest -> _state.update { it.copy(tabs = latest) } }
            .launchIn(scope)

        navRailPreferences.railCollapsed
            .onEach { collapsed -> _state.update { it.copy(railCollapsed = collapsed) } }
            .launchIn(scope)

        notificationsRepository.notifications
            .map { it.size }
            .onEach { count -> _state.update { it.copy(notificationCount = count) } }
            .launchIn(scope)
    }

    fun selectTab(tab: BottomNavBloc.Tab) {
        _state.value = _state.value.copy(selectedTab = tab)
    }

    fun toggleRailCollapsed() {
        navRailPreferences.setRailCollapsed(!_state.value.railCollapsed)
    }

    data class State(
        val selectedTab: BottomNavBloc.Tab = RECIPES,
        val tabs: List<BottomNavBloc.Tab>,
        val notificationCount: Int = 0,
        val railCollapsed: Boolean = false,
    )
}

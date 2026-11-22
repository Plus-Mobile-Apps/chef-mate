package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import androidx.compose.material3.SnackbarDuration
import chefmate.client.bottomnav.public.generated.resources.Res
import chefmate.client.bottomnav.public.generated.resources.swiftui_redesign_prompt_enabled_toast
import com.plusmobileapps.chefmate.Platform
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.currentPlatform
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.notifications.data.NotificationsRepository
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Tab.RECIPES
import com.plusmobileapps.chefmate.recipe.bottomnav.NavRailPreferences
import com.plusmobileapps.chefmate.recipe.bottomnav.TabOrderPreferences
import com.plusmobileapps.chefmate.settings.BetaPreferences
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.toast.ToastService
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
    private val betaPreferences: BetaPreferences,
    private val toastService: ToastService,
    platform: Platform = currentPlatform,
) : ViewModel(mainContext) {
    private val _state =
        MutableStateFlow(
            State(
                tabs = tabOrderPreferences.tabOrder.value,
                railCollapsed = navRailPreferences.railCollapsed.value,
                // iOS only, asked once: offer the native SwiftUI redesign to anyone still on the
                // Compose UI who hasn't already accepted or dismissed the offer.
                showSwiftUiRedesignPrompt =
                    platform == Platform.IOS &&
                        !betaPreferences.isSwiftUiRedesignEnabled.value &&
                        !betaPreferences.hasSeenSwiftUiRedesignPrompt,
            ),
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

    /** Turns the redesign on (it lands on the next cold start) and retires the prompt for good. */
    fun acceptSwiftUiRedesignPrompt() {
        betaPreferences.setSwiftUiRedesignEnabled(true)
        retireSwiftUiRedesignPrompt()
        toastService.show(
            message = Res.string.swiftui_redesign_prompt_enabled_toast.asTextData(),
            duration = SnackbarDuration.Long,
        )
    }

    /** Any dismissal — "Not now", swipe down, or tapping outside — means never ask again. */
    fun dismissSwiftUiRedesignPrompt() {
        retireSwiftUiRedesignPrompt()
    }

    private fun retireSwiftUiRedesignPrompt() {
        betaPreferences.markSwiftUiRedesignPromptSeen()
        _state.update { it.copy(showSwiftUiRedesignPrompt = false) }
    }

    data class State(
        val selectedTab: BottomNavBloc.Tab = RECIPES,
        val tabs: List<BottomNavBloc.Tab>,
        val notificationCount: Int = 0,
        val railCollapsed: Boolean = false,
        val showSwiftUiRedesignPrompt: Boolean = false,
    )
}

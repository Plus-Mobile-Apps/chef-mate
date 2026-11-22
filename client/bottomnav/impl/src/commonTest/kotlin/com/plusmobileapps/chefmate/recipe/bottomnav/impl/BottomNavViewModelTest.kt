@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import chefmate.client.bottomnav.public.generated.resources.Res
import chefmate.client.bottomnav.public.generated.resources.swiftui_redesign_prompt_enabled_toast
import com.plusmobileapps.chefmate.Platform
import com.plusmobileapps.chefmate.notifications.data.AppNotification
import com.plusmobileapps.chefmate.notifications.data.testing.FakeNotificationsRepository
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.DEFAULT_TAB_ORDER
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRole
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class BottomNavViewModelTest {

    private val mainContext = UnconfinedTestDispatcher()
    private val notificationsRepository = FakeNotificationsRepository()

    @Test
    fun initial_tabs_are_seeded_from_preferences() {
        val customOrder =
            listOf(
                BottomNavBloc.Tab.SETTINGS,
                BottomNavBloc.Tab.RECIPES,
                BottomNavBloc.Tab.GROCERIES,
                BottomNavBloc.Tab.MEALS,
                BottomNavBloc.Tab.BROWSER,
            )
        val prefs = FakeTabOrderPreferences(initial = customOrder)
        val vm = createViewModel(tabOrderPreferences = prefs)
        vm.state.value.tabs shouldBe customOrder
    }

    @Test
    fun preferences_updates_propagate_to_state() {
        val prefs = FakeTabOrderPreferences()
        val vm = createViewModel(tabOrderPreferences = prefs)
        vm.state.value.tabs shouldBe DEFAULT_TAB_ORDER

        val newOrder =
            listOf(
                BottomNavBloc.Tab.MEALS,
                BottomNavBloc.Tab.RECIPES,
                BottomNavBloc.Tab.GROCERIES,
                BottomNavBloc.Tab.BROWSER,
                BottomNavBloc.Tab.SETTINGS,
            )
        prefs.setTabOrder(newOrder)
        vm.state.value.tabs shouldBe newOrder
    }

    @Test
    fun notification_count_reflects_pending_notifications() {
        val prefs = FakeTabOrderPreferences()
        val vm = createViewModel(tabOrderPreferences = prefs)
        vm.state.value.notificationCount shouldBe 0

        notificationsRepository.notificationsState.value =
            listOf(
                AppNotification.RecipeBookInvite("b1", "Desserts", RecipeBookRole.EDITOR),
                AppNotification.RecipeBookInvite("b2", "Dinners", RecipeBookRole.VIEWER),
            )
        vm.state.value.notificationCount shouldBe 2
    }

    @Test
    fun rail_collapsed_is_seeded_from_preferences() {
        val vm = createViewModel(navRailPreferences = FakeNavRailPreferences(initial = true))
        vm.state.value.railCollapsed shouldBe true
    }

    @Test
    fun toggle_rail_collapsed_persists_and_updates_state() {
        val railPrefs = FakeNavRailPreferences()
        val vm = createViewModel(navRailPreferences = railPrefs)

        vm.toggleRailCollapsed()
        railPrefs.railCollapsed.value shouldBe true
        vm.state.value.railCollapsed shouldBe true

        vm.toggleRailCollapsed()
        railPrefs.railCollapsed.value shouldBe false
        vm.state.value.railCollapsed shouldBe false
    }

    @Test
    fun swiftUiRedesign_prompt_shows_on_ios_when_unseen_and_not_enabled() {
        createViewModel(platform = Platform.IOS).state.value.showSwiftUiRedesignPrompt shouldBe true
    }

    @Test
    fun swiftUiRedesign_prompt_hidden_off_ios() {
        Platform.entries
            .filter { it != Platform.IOS }
            .forEach { platform ->
                createViewModel(platform = platform).state.value.showSwiftUiRedesignPrompt shouldBe
                    false
            }
    }

    @Test
    fun swiftUiRedesign_prompt_hidden_once_seen() {
        val vm =
            createViewModel(
                platform = Platform.IOS,
                betaPreferences = FakeBetaPreferences(promptSeen = true),
            )
        vm.state.value.showSwiftUiRedesignPrompt shouldBe false
    }

    @Test
    fun swiftUiRedesign_prompt_hidden_when_already_enabled() {
        val vm =
            createViewModel(
                platform = Platform.IOS,
                betaPreferences = FakeBetaPreferences(swiftUiRedesignEnabled = true),
            )
        vm.state.value.showSwiftUiRedesignPrompt shouldBe false
    }

    @Test
    fun accepting_swiftUiRedesign_prompt_enables_redesign_and_never_asks_again() {
        val beta = FakeBetaPreferences()
        val toasts = FakeToastService()
        val vm =
            createViewModel(platform = Platform.IOS, betaPreferences = beta, toastService = toasts)

        vm.acceptSwiftUiRedesignPrompt()

        beta.isSwiftUiRedesignEnabled.value shouldBe true
        beta.hasSeenSwiftUiRedesignPrompt shouldBe true
        vm.state.value.showSwiftUiRedesignPrompt shouldBe false
        toasts.shown shouldBe listOf(Res.string.swiftui_redesign_prompt_enabled_toast.asTextData())
    }

    @Test
    fun dismissing_swiftUiRedesign_prompt_leaves_redesign_off_and_never_asks_again() {
        val beta = FakeBetaPreferences()
        val toasts = FakeToastService()
        val vm =
            createViewModel(platform = Platform.IOS, betaPreferences = beta, toastService = toasts)

        vm.dismissSwiftUiRedesignPrompt()

        beta.isSwiftUiRedesignEnabled.value shouldBe false
        beta.hasSeenSwiftUiRedesignPrompt shouldBe true
        vm.state.value.showSwiftUiRedesignPrompt shouldBe false
        toasts.shown shouldBe emptyList()
        createViewModel(platform = Platform.IOS, betaPreferences = beta)
            .state
            .value
            .showSwiftUiRedesignPrompt shouldBe false
    }

    private fun createViewModel(
        tabOrderPreferences: FakeTabOrderPreferences = FakeTabOrderPreferences(),
        navRailPreferences: FakeNavRailPreferences = FakeNavRailPreferences(),
        betaPreferences: FakeBetaPreferences = FakeBetaPreferences(),
        toastService: FakeToastService = FakeToastService(),
        platform: Platform = Platform.ANDROID,
    ) =
        BottomNavViewModel(
            mainContext = mainContext,
            tabOrderPreferences = tabOrderPreferences,
            navRailPreferences = navRailPreferences,
            notificationsRepository = notificationsRepository,
            betaPreferences = betaPreferences,
            toastService = toastService,
            platform = platform,
        )
}

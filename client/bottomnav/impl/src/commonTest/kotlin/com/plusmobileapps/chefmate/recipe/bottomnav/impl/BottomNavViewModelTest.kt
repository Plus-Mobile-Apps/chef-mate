@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import com.plusmobileapps.chefmate.notifications.data.AppNotification
import com.plusmobileapps.chefmate.notifications.data.testing.FakeNotificationsRepository
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.DEFAULT_TAB_ORDER
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRole
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
        val vm =
            BottomNavViewModel(
                mainContext = mainContext,
                tabOrderPreferences = prefs,
                navRailPreferences = FakeNavRailPreferences(),
                notificationsRepository = notificationsRepository,
            )
        vm.state.value.tabs shouldBe customOrder
    }

    @Test
    fun preferences_updates_propagate_to_state() {
        val prefs = FakeTabOrderPreferences()
        val vm =
            BottomNavViewModel(
                mainContext = mainContext,
                tabOrderPreferences = prefs,
                navRailPreferences = FakeNavRailPreferences(),
                notificationsRepository = notificationsRepository,
            )
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
        val vm =
            BottomNavViewModel(
                mainContext = mainContext,
                tabOrderPreferences = prefs,
                navRailPreferences = FakeNavRailPreferences(),
                notificationsRepository = notificationsRepository,
            )
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

    private fun createViewModel(navRailPreferences: FakeNavRailPreferences) =
        BottomNavViewModel(
            mainContext = mainContext,
            tabOrderPreferences = FakeTabOrderPreferences(),
            navRailPreferences = navRailPreferences,
            notificationsRepository = notificationsRepository,
        )
}

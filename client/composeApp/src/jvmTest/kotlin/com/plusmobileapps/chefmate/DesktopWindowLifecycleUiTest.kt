@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.window.WindowState
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.plusmobileapps.chefmate.di.createTestApplicationComponent
import com.plusmobileapps.chefmate.family.manage.robots.familyList
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.fixtures.TestRecipes
import com.plusmobileapps.chefmate.harness.TestUserState
import com.plusmobileapps.chefmate.harness.applyUserState
import com.plusmobileapps.chefmate.harness.createRootBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.robots.bottomNav
import com.plusmobileapps.chefmate.settings.robots.more
import kotlin.test.Test

/**
 * `runRootBlocTest` hands the root a lifecycle it has already resumed, which hid that the desktop
 * main window never moved its own past INITIALIZED — so every `doOnResume` (the family list's load
 * among them) silently never ran there. These tests boot the root the way `main.kt` does instead: a
 * bare [LifecycleRegistry] driven by [LifecycleController] from the window's state.
 */
class DesktopWindowLifecycleUiTest {

    @Test
    fun manage_family_loads_when_the_window_drives_the_lifecycle() = runComposeUiTest {
        val app = createTestApplicationComponent()
        app.settings.clear()
        app.applyUserState(
            TestUserState.Authenticated(recipes = listOf(TestRecipes.fullyPopulated)),
        )
        app.onboardingRepository.setOnboardingCompleted()
        app.testFeatureFlags.set(FeatureFlagRegistry.ManageFamily, true)
        app.testFamilyRepository.seedFamily("The Smiths")

        val lifecycle = LifecycleRegistry()
        val windowState = WindowState()
        setContent {
            LifecycleController(lifecycle, windowState)
            App(
                rootBloc = app.createRootBloc(lifecycle = lifecycle),
                toastService = app.toastService,
                inAppReviewService = app.inAppReviewService,
            )
        }

        bottomNav().clickMoreTab()
        more().awaitDisplayed().clickManageFamilyRow()

        familyList().awaitDisplayed().awaitFamily("The Smiths")
    }
}

@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.harness

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.plusmobileapps.chefmate.App
import com.plusmobileapps.chefmate.DefaultBlocContext
import com.plusmobileapps.chefmate.di.TestApplicationComponent
import com.plusmobileapps.chefmate.di.createTestApplicationComponent
import com.plusmobileapps.chefmate.fixtures.TestRecipes
import com.plusmobileapps.chefmate.root.DeepLink
import com.plusmobileapps.chefmate.root.RootBloc
import kotlinx.coroutines.test.TestResult

/**
 * Build a root BLoC tree for UI tests using production-default coroutine dispatchers (Main, IO,
 * Default, Unconfined as set by [DefaultBlocContext]). The lifecycle is started in the resumed
 * state so child blocs and view models initialise immediately. Returns a fully-wired [RootBloc]
 * ready to be passed into `App(rootBloc = …)`.
 */
fun TestApplicationComponent.createRootBloc(
    lifecycle: LifecycleRegistry = LifecycleRegistry().apply { resume() },
    deepLink: DeepLink = DeepLink.None,
): RootBloc =
    rootBlocFactory.create(
        context =
            DefaultBlocContext(componentContext = DefaultComponentContext(lifecycle = lifecycle)),
        deepLink = deepLink,
    )

/**
 * Wraps [runComposeUiTest] with the standard root-bloc setup: builds a [TestApplicationComponent],
 * applies [userState], and renders `App` against a fresh [RootBloc]. The component is passed to
 * [block] so robots/assertions can reach back into the graph (e.g. flipping auth state mid-test).
 *
 * Returns the [TestResult] from [runComposeUiTest] unchanged — required so K/N test runners await
 * the suspending body before reporting the test as finished.
 *
 * [beforeContent] runs against the component *before* the UI is composed — use it to seed feature
 * flags (or other state) that the initial screen's view model reads at construction, so the first
 * frame already reflects them. Flipping such a flag inside [block] would race the first
 * composition.
 */
fun runRootBlocTest(
    userState: TestUserState =
        TestUserState.Authenticated(recipes = listOf(TestRecipes.fullyPopulated)),
    showOnboarding: Boolean = false,
    beforeContent: (TestApplicationComponent) -> Unit = {},
    block: suspend ComposeUiTest.(TestApplicationComponent) -> Unit,
): TestResult = runComposeUiTest {
    val app = createTestApplicationComponent()
    // The production Settings() is backed by a process-global store (JVM Preferences), so persisted
    // flags leak across tests. Clear it up front for deterministic, isolated runs.
    app.settings.clear()
    app.applyUserState(userState)
    if (!showOnboarding) {
        // Default to a returning user so tests boot straight into the main app. Leaving onboarding
        // incomplete (the showOnboarding = true case) is enough to boot into the onboarding flow.
        app.onboardingRepository.setOnboardingCompleted()
    }
    beforeContent(app)
    setContent {
        App(
            rootBloc = app.createRootBloc(),
            toastService = app.toastService,
            inAppReviewService = app.inAppReviewService,
        )
    }
    block(app)
}

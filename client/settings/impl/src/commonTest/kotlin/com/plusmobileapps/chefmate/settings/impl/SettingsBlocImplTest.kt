@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.settings.impl

import app.cash.turbine.test
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.auth.usecase.SignOutUseCase
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.featureflag.testing.FakeFeatureFlags
import com.plusmobileapps.chefmate.settings.SettingsBloc
import com.plusmobileapps.chefmate.subscription.testing.FakeSubscriptionRepository
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest

class SettingsBlocImplTest {

    private val context = TestBlocContext.create()
    private val output = TestConsumer<SettingsBloc.Output>()
    private val authRepository = FakeAuthenticationRepository()
    private val signOutUseCase = SignOutUseCase { authRepository.signOut() }
    private val featureFlags = FakeFeatureFlags()
    private val subscriptionRepository = FakeSubscriptionRepository()

    private val bloc by lazy {
        SettingsBlocImpl(
            context = context,
            output = output,
            viewModelFactory = {
                SettingsViewModel(
                    mainContext = context.mainContext,
                    authenticationRepository = authRepository,
                    signOutUseCase = signOutUseCase,
                    featureFlags = featureFlags,
                    subscriptionRepository = subscriptionRepository,
                )
            },
        )
    }

    @Test
    fun When_authenticated_Then_profile_image_and_name_are_exposed() = runTest {
        authRepository.setAuthenticated(
            FakeAuthenticationRepository.fakeUser()
                .copy(userProfileImageUrl = "https://example.com/avatar.png")
        )

        bloc.state.value.profileImageUrl shouldBe "https://example.com/avatar.png"
        bloc.state.value.profileName shouldBe "Test User"
    }

    @Test
    fun When_anonymous_Then_profile_image_and_name_are_hidden() = runTest {
        authRepository.setAnonymous()

        bloc.state.value.profileImageUrl shouldBe null
        bloc.state.value.profileName shouldBe null
    }

    @Test
    fun When_signed_out_Then_profile_image_is_cleared() = runTest {
        authRepository.setAuthenticated(
            FakeAuthenticationRepository.fakeUser()
                .copy(userProfileImageUrl = "https://example.com/avatar.png")
        )
        bloc.state.value.profileImageUrl shouldBe "https://example.com/avatar.png"

        authRepository.signOut()

        bloc.state.value.profileImageUrl shouldBe null
    }

    @Test
    fun When_sign_out_clicked_Then_confirmation_dialog_is_shown() = runTest {
        authRepository.setAuthenticated()

        bloc.state.test {
            awaitItem() // initial authenticated state

            bloc.onSignOutClicked()

            awaitItem().showSignOutConfirmationDialog shouldBe true
        }
    }

    @Test
    fun When_sign_out_dismissed_Then_confirmation_dialog_is_hidden() = runTest {
        authRepository.setAuthenticated()

        bloc.state.test {
            awaitItem()

            bloc.onSignOutClicked()
            awaitItem().showSignOutConfirmationDialog shouldBe true

            bloc.onSignOutDismissed()
            awaitItem().showSignOutConfirmationDialog shouldBe false
        }
    }

    @Test
    fun When_sign_out_confirmed_Then_user_is_signed_out() = runTest {
        authRepository.setAuthenticated()
        bloc.onSignOutClicked()
        bloc.onSignOutConfirmed()

        bloc.state.value.isAuthenticated shouldBe false
        bloc.state.value.showSignOutConfirmationDialog shouldBe false
    }

    @Test
    fun When_sign_out_confirmed_Then_dialog_is_hidden() = runTest {
        authRepository.setAuthenticated()

        bloc.state.test {
            awaitItem()
            bloc.onSignOutClicked()
            awaitItem()

            bloc.onSignOutConfirmed()
            awaitItem().showSignOutConfirmationDialog shouldBe false
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun When_sign_in_clicked_Then_OpenSignIn_output_emitted() {
        bloc.onSignInClicked()
        output.lastValue shouldBe SettingsBloc.Output.OpenSignIn
    }

    @Test
    fun When_sign_up_clicked_Then_OpenSignUp_output_emitted() {
        bloc.onSignUpClicked()
        output.lastValue shouldBe SettingsBloc.Output.OpenSignUp
    }

    @Test
    fun When_url_clicked_Then_OpenUrl_output_emitted() {
        val url = "https://example.com"
        bloc.onUrlClicked(url)
        output.lastValue shouldBe SettingsBloc.Output.OpenUrl(url)
    }

    @Test
    fun When_app_settings_clicked_Then_OpenAppSettings_output_emitted() {
        bloc.onAppSettingsClicked()
        output.lastValue shouldBe SettingsBloc.Output.OpenAppSettings
    }

    @Test
    fun When_subscribed_and_ai_chat_clicked_Then_OpenAiChat_output_emitted() {
        subscriptionRepository.setSubscribed(true)

        bloc.onAiChatClicked()

        output.lastValue shouldBe SettingsBloc.Output.OpenAiChat
    }

    @Test
    fun When_not_subscribed_and_ai_chat_clicked_Then_upsell_shown_and_no_output() {
        bloc.onAiChatClicked()

        bloc.state.value.showPremiumRequiredDialog shouldBe true
        output.values.shouldBeEmpty()
    }

    @Test
    fun When_upsell_dismissed_Then_dialog_hidden() {
        bloc.onAiChatClicked()
        bloc.state.value.showPremiumRequiredDialog shouldBe true

        bloc.onPremiumRequiredDismissed()

        bloc.state.value.showPremiumRequiredDialog shouldBe false
    }

    @Test
    fun When_subscription_starts_Then_state_reflects_it_without_reopening() = runTest {
        bloc.state.test {
            awaitItem().isSubscribed shouldBe false

            subscriptionRepository.setSubscribed(true)

            awaitItem().isSubscribed shouldBe true
        }
    }

    @Test
    fun When_auth_state_changes_Then_ai_chat_flag_and_subscription_survive() = runTest {
        featureFlags.set(FeatureFlagRegistry.AiChat, true)
        subscriptionRepository.setSubscribed(true)

        bloc.state.test {
            skipItems(1)
            // Both stream in independently of auth; an auth emission used to rebuild State from
            // scratch and silently reset them.
            authRepository.setAuthenticated()

            val state = expectMostRecentItem()
            state.isAuthenticated shouldBe true
            state.isAiChatEnabled shouldBe true
            state.isSubscribed shouldBe true
        }
    }

    @Test
    fun When_replay_onboarding_clicked_Then_OpenOnboarding_output_emitted() {
        bloc.onReplayOnboardingClicked()
        output.lastValue shouldBe SettingsBloc.Output.OpenOnboarding
    }

    @Test
    fun When_authenticated_Then_state_reflects_authenticated() = runTest {
        bloc.state.test {
            awaitItem().isAuthenticated shouldBe false

            authRepository.setAuthenticated()
            awaitItem().isAuthenticated shouldBe true
        }
    }

    @Test
    fun When_ai_chat_flag_off_Then_isAiChatEnabled_is_false() = runTest {
        // Flag defaults to off — the row should be hidden.
        bloc.state.value.isAiChatEnabled shouldBe false
    }

    @Test
    fun When_ai_chat_flag_on_Then_isAiChatEnabled_is_true() = runTest {
        featureFlags.set(FeatureFlagRegistry.AiChat, true)

        bloc.state.test {
            // The flag observer pushes the initial value during the first emission; if it's not
            // captured by the time the bloc is first observed, await the next emission.
            val initial = awaitItem()
            if (initial.isAiChatEnabled) {
                // already reflects the flag
            } else {
                awaitItem().isAiChatEnabled shouldBe true
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun When_ai_chat_flag_flips_off_Then_isAiChatEnabled_flips_off() = runTest {
        featureFlags.set(FeatureFlagRegistry.AiChat, true)

        bloc.state.test {
            // Drain the initial true emission(s).
            var enabled = awaitItem().isAiChatEnabled
            while (!enabled) {
                enabled = awaitItem().isAiChatEnabled
            }
            enabled shouldBe true

            featureFlags.set(FeatureFlagRegistry.AiChat, false)

            awaitItem().isAiChatEnabled shouldBe false
            cancelAndIgnoreRemainingEvents()
        }
    }
}

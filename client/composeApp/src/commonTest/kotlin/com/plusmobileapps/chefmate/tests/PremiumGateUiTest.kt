@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import com.plusmobileapps.chefmate.aichat.robots.aiChat
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.recipe.bottomnav.robots.bottomNav
import com.plusmobileapps.chefmate.settings.robots.more
import com.plusmobileapps.chefmate.subscription.robots.premium
import kotlin.test.Test

// Test graphs never configure RevenueCat, so "Subscribe" reaches the paywall-unavailable fallback
// instead of the store paywall — which still proves the gate routes into the purchase flow.
class PremiumGateUiTest {

    @Test
    fun non_subscriber_tapping_ai_chat_can_dismiss_the_upsell() = runRootBlocTest { component ->
        component.testFeatureFlags.set(FeatureFlagRegistry.AiChat, true)

        bottomNav().clickMoreTab()
        more().awaitDisplayed().clickAiChatRow()

        premium().awaitGateDialog().tapNotNow().assertNothingShown()
    }

    @Test
    fun subscribe_opens_the_paywall_and_closing_it_returns_to_the_screen() =
        runRootBlocTest { component ->
            component.testFeatureFlags.set(FeatureFlagRegistry.AiChat, true)

            bottomNav().clickMoreTab()
            more().awaitDisplayed().clickAiChatRow()

            premium()
                .awaitGateDialog()
                .tapSubscribe()
                .awaitPaywallUnavailable()
                .dismissPaywallUnavailable()
                .assertNothingShown()
            more().awaitDisplayed()
        }

    @Test
    fun becoming_a_subscriber_unlocks_ai_chat() = runRootBlocTest { component ->
        component.testFeatureFlags.set(FeatureFlagRegistry.AiChat, true)

        bottomNav().clickMoreTab()
        more().awaitDisplayed().clickAiChatRow()
        premium().awaitGateDialog().tapNotNow().assertNothingShown()

        // What a completed store purchase looks like to the app: the entitlement flips live.
        component.testSubscriptionRepository.setSubscribed(true)
        more().clickAiChatRow()

        aiChat().awaitShown()
    }
}

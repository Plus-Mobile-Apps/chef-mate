@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.subscription.robots

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.plusmobileapps.chefmate.subscription.SubscriptionTestTags

/**
 * Robot for the ChefMate Premium upsell: the gate dialog a non-subscriber sees and what "Subscribe"
 * opens. Test graphs never configure RevenueCat, so "Subscribe" always lands on the
 * paywall-unavailable fallback rather than the real store paywall.
 */
class PremiumRobot(private val test: ComposeUiTest) {

    private val onGateDialog =
        hasAnyAncestor(hasTestTag(SubscriptionTestTags.PREMIUM_REQUIRED_DIALOG))
    private val onUnavailableDialog =
        hasAnyAncestor(hasTestTag(SubscriptionTestTags.PAYWALL_UNAVAILABLE_DIALOG))

    fun awaitGateDialog(): PremiumRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(SubscriptionTestTags.PREMIUM_REQUIRED_DIALOG))
    }

    fun tapSubscribe(): PremiumRobot = apply {
        test.onNode(hasText("Subscribe") and onGateDialog).performClick()
    }

    fun tapNotNow(): PremiumRobot = apply {
        test.onNode(hasText("Not now") and onGateDialog).performClick()
    }

    fun awaitPaywallUnavailable(): PremiumRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(SubscriptionTestTags.PAYWALL_UNAVAILABLE_DIALOG))
    }

    fun dismissPaywallUnavailable(): PremiumRobot = apply {
        test.onNode(hasText("Got it") and onUnavailableDialog).performClick()
    }

    fun assertNothingShown(): PremiumRobot = apply {
        test.waitUntilDoesNotExist(hasTestTag(SubscriptionTestTags.PREMIUM_REQUIRED_DIALOG))
        test.waitUntilDoesNotExist(hasTestTag(SubscriptionTestTags.PAYWALL_UNAVAILABLE_DIALOG))
    }
}

fun ComposeUiTest.premium(): PremiumRobot = PremiumRobot(this)

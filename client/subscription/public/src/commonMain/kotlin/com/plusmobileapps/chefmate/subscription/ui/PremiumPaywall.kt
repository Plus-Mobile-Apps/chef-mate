package com.plusmobileapps.chefmate.subscription.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import chefmate.client.subscription.public.generated.resources.Res
import chefmate.client.subscription.public.generated.resources.premium_paywall_unavailable_confirm
import chefmate.client.subscription.public.generated.resources.premium_paywall_unavailable_message
import chefmate.client.subscription.public.generated.resources.premium_paywall_unavailable_title
import com.plusmobileapps.chefmate.subscription.SubscriptionTestTags
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusDialog

/**
 * Full-screen purchase flow for ChefMate Premium, shown over the current screen in its own dialog
 * window. On Android/iOS this is RevenueCat's server-driven Paywall (designed in the RevenueCat
 * dashboard, current offering); [onDismiss] fires when the user closes it or completes a purchase.
 * A successful purchase needs no handling here — the SDK's customer-info update flips
 * `SubscriptionRepository.isSubscribed` and every gate reacts on its own.
 *
 * Where nothing can be bought (desktop, or a build without a RevenueCat API key) it falls back to
 * [PaywallUnavailableDialog].
 */
@Composable
internal fun PremiumPaywall(onDismiss: () -> Unit) {
    if (!isPaywallAvailable) {
        PaywallUnavailableDialog(onDismiss)
        return
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        RevenueCatPaywall(onDismiss, Modifier.fillMaxSize())
    }
}

/**
 * [PremiumPaywall] for a host that already presents it full-screen — the SwiftUI redesign, which
 * shows it in a native `fullScreenCover` — so it renders in place rather than in a dialog window.
 */
@Composable
fun PremiumPaywallScreen(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    if (!isPaywallAvailable) {
        PaywallUnavailableDialog(onDismiss)
        return
    }
    RevenueCatPaywall(onDismiss, modifier)
}

@Composable
private fun RevenueCatPaywall(onDismiss: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.testTag(SubscriptionTestTags.PAYWALL)) { StorePaywall(onDismiss) }
}

/**
 * Whether the store paywall can render. False on desktop and in a build without a RevenueCat API
 * key, which never configures the SDK — the Paywall would crash reaching for its shared instance.
 */
internal expect val isPaywallAvailable: Boolean

/** RevenueCat's Paywall. Only called when [isPaywallAvailable]. */
@Composable internal expect fun StorePaywall(onDismiss: () -> Unit)

@Composable
internal fun PaywallUnavailableDialog(onDismiss: () -> Unit) {
    PlusDialog(
        title = Res.string.premium_paywall_unavailable_title.asTextData(),
        message = Res.string.premium_paywall_unavailable_message.asTextData(),
        confirmButtonText = Res.string.premium_paywall_unavailable_confirm.asTextData(),
        onConfirmClick = onDismiss,
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(SubscriptionTestTags.PAYWALL_UNAVAILABLE_DIALOG),
    )
}

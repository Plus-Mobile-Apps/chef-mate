package com.plusmobileapps.chefmate.subscription.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import chefmate.client.subscription.public.generated.resources.Res
import chefmate.client.subscription.public.generated.resources.premium_paywall_unavailable_confirm
import chefmate.client.subscription.public.generated.resources.premium_paywall_unavailable_message
import chefmate.client.subscription.public.generated.resources.premium_paywall_unavailable_title
import com.plusmobileapps.chefmate.subscription.SubscriptionTestTags
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusDialog

/**
 * Full-screen purchase flow for ChefMate Premium. On Android/iOS this is RevenueCat's server-driven
 * Paywall (designed in the RevenueCat dashboard, current offering); [onDismiss] fires when the user
 * closes it or completes a purchase. A successful purchase needs no handling here — the SDK's
 * customer-info update flips `SubscriptionRepository.isSubscribed` and every gate reacts on its
 * own.
 *
 * Where nothing can be bought (desktop, or a build without a RevenueCat API key) it falls back to
 * [PaywallUnavailableDialog].
 */
@Composable internal expect fun PremiumPaywall(onDismiss: () -> Unit)

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

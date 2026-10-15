package com.plusmobileapps.chefmate.subscription.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.plusmobileapps.chefmate.subscription.SubscriptionTestTags
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

// NOTE: identical to the iosMain twin (PremiumPaywall.ios.kt). purchases-kmp-ui exposes one
// common API for Android + iOS, but the default source-set hierarchy has no shared parent for the
// two, so the code is duplicated. Keep both files in sync.
@Composable
internal actual fun PremiumPaywall(onDismiss: () -> Unit) {
    // A build without a RevenueCat API key never configures the SDK, and the Paywall would crash
    // reaching for Purchases.sharedInstance.
    if (!Purchases.isConfigured) {
        PaywallUnavailableDialog(onDismiss)
        return
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(modifier = Modifier.fillMaxSize().testTag(SubscriptionTestTags.PAYWALL)) {
            Paywall(
                PaywallOptions(dismissRequest = onDismiss) {
                    // Honored by legacy template paywalls only; V2 paywalls get their close
                    // button from the RevenueCat paywall editor.
                    shouldDisplayDismissButton = true
                }
            )
        }
    }
}

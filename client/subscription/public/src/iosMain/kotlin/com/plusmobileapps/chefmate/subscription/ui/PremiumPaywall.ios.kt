package com.plusmobileapps.chefmate.subscription.ui

import androidx.compose.runtime.Composable
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions

// NOTE: identical to the androidMain twin (PremiumPaywall.android.kt). purchases-kmp-ui exposes one
// common API for Android + iOS, but the default source-set hierarchy has no shared parent for the
// two, so the code is duplicated. Keep both files in sync.
internal actual val isPaywallAvailable: Boolean
    get() = Purchases.isConfigured

@Composable
internal actual fun StorePaywall(onDismiss: () -> Unit) {
    Paywall(
        PaywallOptions(dismissRequest = onDismiss) {
            // Honored by legacy template paywalls only; V2 paywalls get their close button from
            // the RevenueCat paywall editor.
            shouldDisplayDismissButton = true
        },
    )
}

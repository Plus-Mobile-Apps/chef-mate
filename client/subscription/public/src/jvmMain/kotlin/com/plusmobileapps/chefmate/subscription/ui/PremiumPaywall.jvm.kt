package com.plusmobileapps.chefmate.subscription.ui

import androidx.compose.runtime.Composable

/** Desktop has no app store and purchases-kmp has no JVM artifact, so there is nothing to buy. */
@Composable
internal actual fun PremiumPaywall(onDismiss: () -> Unit) {
    PaywallUnavailableDialog(onDismiss)
}

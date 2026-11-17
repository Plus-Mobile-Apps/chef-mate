package com.plusmobileapps.chefmate.subscription.ui

import androidx.compose.runtime.Composable

/** Desktop has no app store and purchases-kmp has no JVM artifact, so there is nothing to buy. */
internal actual val isPaywallAvailable: Boolean = false

@Composable internal actual fun StorePaywall(onDismiss: () -> Unit) = Unit

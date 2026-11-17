package com.plusmobileapps.chefmate.redesign

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.plusmobileapps.chefmate.subscription.ui.PremiumPaywallScreen
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import platform.UIKit.UIViewController

/**
 * RevenueCat's paywall for the SwiftUI redesign, which presents it in a native `fullScreenCover`
 * when "Subscribe" is tapped on a premium alert (Swift `PremiumPaywallCover`). [onDismiss] fires
 * when the user closes the paywall or completes a purchase.
 */
object PremiumPaywallViewController {
    fun create(onDismiss: () -> Unit): UIViewController = ComposeUIViewController {
        ChefMateTheme { PremiumPaywallScreen(onDismiss, Modifier.fillMaxSize()) }
    }
}

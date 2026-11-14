package com.plusmobileapps.chefmate.subscription.impl

import com.plusmobileapps.chefmate.buildconfig.BuildConfig
import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration

actual object SubscriptionInitializer {
    actual fun initialize() {
        configureRevenueCat(BuildConfig.REVENUECAT_IOS_API_KEY)
    }
}

// Twin of configureRevenueCat in SubscriptionInitializer.android.kt — keep in sync.
private fun configureRevenueCat(apiKey: String) {
    if (apiKey.isBlank() || Purchases.isConfigured) return
    Purchases.logLevel = if (BuildConfig.IS_DEBUG) LogLevel.DEBUG else LogLevel.ERROR
    Purchases.configure(PurchasesConfiguration(apiKey))
}

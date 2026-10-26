package com.plusmobileapps.chefmate

import com.plusmobileapps.chefmate.subscription.impl.SubscriptionInitializer

/** Called from `iOSApp.swift` at launch to configure RevenueCat with the iOS API key. */
fun initializeSubscriptions() {
    SubscriptionInitializer.initialize()
}

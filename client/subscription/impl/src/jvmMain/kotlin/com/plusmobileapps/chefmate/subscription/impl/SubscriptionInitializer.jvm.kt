package com.plusmobileapps.chefmate.subscription.impl

/** Desktop has no app store, so there is nothing to configure. */
actual object SubscriptionInitializer {
    actual fun initialize() = Unit
}

package com.plusmobileapps.chefmate.subscription.impl

/**
 * Configures the store SDK at app startup. Android/iOS configure RevenueCat with that platform's
 * BuildKonfig API key; JVM no-ops. Call exactly once, as early as possible (before the DI graph
 * builds a `SubscriptionRepository`), from each platform's entry point.
 *
 * A blank key is treated as "not configured" and skips setup, keeping local builds runnable without
 * RevenueCat credentials — the gateway then reports "never entitled" and the paywall shows its
 * unavailable state.
 */
expect object SubscriptionInitializer {
    fun initialize()
}

package com.plusmobileapps.chefmate.subscription

import kotlinx.coroutines.flow.StateFlow

/**
 * Source of truth for whether the user has an active ChefMate Premium subscription.
 *
 * Deliberately read-only: entitlement is granted by the store (App Store / Play), never by the app.
 * The only writer today is Developer Settings, which flips the underlying override — see
 * `StubSubscriptionRepository`. When a real billing SDK lands it replaces that implementation and
 * every caller here stays unchanged.
 */
interface SubscriptionRepository {
    /**
     * `true` while the user is entitled to premium features. Callers should treat this as live — it
     * can flip at any time (purchase, restore, expiry, or a developer toggling the override).
     */
    val isSubscribed: StateFlow<Boolean>
}

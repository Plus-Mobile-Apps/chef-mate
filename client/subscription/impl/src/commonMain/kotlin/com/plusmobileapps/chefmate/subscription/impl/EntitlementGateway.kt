package com.plusmobileapps.chefmate.subscription.impl

import kotlinx.coroutines.flow.Flow

/**
 * Thin platform boundary over the store SDK. The Android/iOS actual talks to RevenueCat; the JVM
 * actual (and an unconfigured SDK — blank API key) reports "never entitled". Kept as an interface
 * rather than an `expect class` so [RevenueCatSubscriptionRepository] can take a fake in
 * `commonTest`.
 */
internal interface EntitlementGateway {
    /** Entitlement state pushed by the SDK on every customer-info change (purchase, expiry, …). */
    fun entitlementUpdates(): Flow<Boolean>

    /**
     * Associates purchases with the signed-in ChefMate user, so the entitlement follows the account
     * across devices and platforms. Returns whether premium is active for that user.
     */
    suspend fun identify(userId: String): Boolean

    /** Detaches the store SDK from any signed-in user. Returns whether premium is still active. */
    suspend fun reset(): Boolean
}

/** Platform factory: RevenueCat-backed on Android/iOS, a no-op on JVM. */
internal expect fun createEntitlementGateway(): EntitlementGateway

/**
 * RevenueCat entitlement identifier that unlocks ChefMate Premium. Must match the entitlement
 * configured in the RevenueCat dashboard.
 */
internal const val PREMIUM_ENTITLEMENT_ID = "premium"

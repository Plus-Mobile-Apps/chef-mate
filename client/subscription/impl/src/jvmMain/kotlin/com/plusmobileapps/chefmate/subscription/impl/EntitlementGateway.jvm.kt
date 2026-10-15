package com.plusmobileapps.chefmate.subscription.impl

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Desktop has no app store and purchases-kmp has no JVM artifact, so desktop users are never
 * entitled through the store (the debug Developer Settings override still applies).
 */
internal actual fun createEntitlementGateway(): EntitlementGateway = NoOpEntitlementGateway

private object NoOpEntitlementGateway : EntitlementGateway {
    override fun entitlementUpdates(): Flow<Boolean> = emptyFlow()

    override suspend fun identify(userId: String): Boolean = false

    override suspend fun reset(): Boolean = false
}

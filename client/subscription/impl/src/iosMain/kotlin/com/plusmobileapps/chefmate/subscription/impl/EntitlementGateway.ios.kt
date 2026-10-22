package com.plusmobileapps.chefmate.subscription.impl

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction
import com.revenuecat.purchases.kmp.result.awaitCustomerInfoResult
import com.revenuecat.purchases.kmp.result.awaitLogInResult
import com.revenuecat.purchases.kmp.result.awaitLogOutResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow

// NOTE: identical to the androidMain twin (EntitlementGateway.android.kt). purchases-kmp exposes
// one
// common API for Android + iOS, but the default source-set hierarchy has no shared parent for the
// two, so the code is duplicated. Keep both files in sync.
internal actual fun createEntitlementGateway(): EntitlementGateway =
    if (Purchases.isConfigured) RevenueCatEntitlementGateway(Purchases.sharedInstance)
    else UnconfiguredEntitlementGateway

private class RevenueCatEntitlementGateway(private val purchases: Purchases) : EntitlementGateway {

    override fun entitlementUpdates(): Flow<Boolean> = callbackFlow {
        purchases.delegate =
            object : PurchasesDelegate {
                override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) {
                    trySend(customerInfo.isPremium())
                }

                override fun onPurchasePromoProduct(
                    product: StoreProduct,
                    startPurchase:
                        (
                            onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                            onSuccess: (StoreTransaction, CustomerInfo) -> Unit,
                        ) -> Unit,
                ) {
                    // App Store promoted purchases: let them go through; the resulting
                    // customer-info update flips the entitlement like any other purchase.
                    startPurchase({ _, _ -> }, { _, _ -> })
                }
            }
        awaitClose { purchases.delegate = null }
    }

    override suspend fun identify(userId: String): Boolean =
        if (purchases.appUserID == userId) {
            purchases.awaitCustomerInfoResult().getOrThrow().isPremium()
        } else {
            purchases.awaitLogInResult(newAppUserID = userId).getOrThrow().customerInfo.isPremium()
        }

    override suspend fun reset(): Boolean =
        // logOut() errors for an already-anonymous user, so only call it when someone is signed in.
        if (purchases.isAnonymous) {
            purchases.awaitCustomerInfoResult().getOrThrow().isPremium()
        } else {
            purchases.awaitLogOutResult().getOrThrow().isPremium()
        }
}

/** No API key for this build: nothing to query, never entitled. */
private object UnconfiguredEntitlementGateway : EntitlementGateway {
    override fun entitlementUpdates(): Flow<Boolean> = emptyFlow()

    override suspend fun identify(userId: String): Boolean = false

    override suspend fun reset(): Boolean = false
}

private fun CustomerInfo.isPremium(): Boolean =
    entitlements[PREMIUM_ENTITLEMENT_ID]?.isActive == true

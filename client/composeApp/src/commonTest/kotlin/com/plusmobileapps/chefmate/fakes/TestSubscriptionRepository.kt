package com.plusmobileapps.chefmate.fakes

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.subscription.SubscriptionRepository
import com.plusmobileapps.chefmate.subscription.impl.RevenueCatSubscriptionRepository
import com.plusmobileapps.chefmate.subscription.testing.FakeSubscriptionRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.StateFlow

/**
 * Replaces the production [RevenueCatSubscriptionRepository] in tests so entitlement doesn't depend
 * on a store SDK or the Developer Settings preference. Defaults to **not** subscribed; a UI test
 * that needs to reach the AI chat must call [setSubscribed] with `true` before the screen renders.
 */
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(scope = AppScope::class, replaces = [RevenueCatSubscriptionRepository::class])
class TestSubscriptionRepository(
    private val fake: FakeSubscriptionRepository = FakeSubscriptionRepository()
) : SubscriptionRepository {

    override val isSubscribed: StateFlow<Boolean> = fake.isSubscribed

    fun setSubscribed(subscribed: Boolean) = fake.setSubscribed(subscribed)
}

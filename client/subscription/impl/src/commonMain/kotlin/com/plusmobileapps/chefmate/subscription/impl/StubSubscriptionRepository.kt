package com.plusmobileapps.chefmate.subscription.impl

import com.plusmobileapps.chefmate.devsettings.DeveloperPreferences
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.subscription.SubscriptionRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.StateFlow

/**
 * Stand-in for a real billing integration: entitlement comes straight from the Developer Settings
 * override rather than from a store.
 *
 * The override defaults to `false`, so a build with no one touching Developer Settings behaves like
 * a free user — which is the state the gates need to be correct in.
 *
 * TODO: replace with a store-backed implementation (App Store / Play via a billing SDK). The
 *   developer override is worth keeping even then, OR-ed over the real entitlement, so QA can reach
 *   premium surfaces without a sandbox purchase.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class StubSubscriptionRepository(preferences: DeveloperPreferences) : SubscriptionRepository {
    override val isSubscribed: StateFlow<Boolean> = preferences.isSubscribed
}

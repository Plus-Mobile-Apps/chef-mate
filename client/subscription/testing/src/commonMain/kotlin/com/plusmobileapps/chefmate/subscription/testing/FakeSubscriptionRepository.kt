package com.plusmobileapps.chefmate.subscription.testing

import com.plusmobileapps.chefmate.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Defaults to **not** subscribed so a test that forgets to opt in exercises the gated path — the
 * state most gate regressions hide in. Call [setSubscribed] to reach premium surfaces.
 */
class FakeSubscriptionRepository(initiallySubscribed: Boolean = false) : SubscriptionRepository {

    private val _isSubscribed = MutableStateFlow(initiallySubscribed)

    override val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    fun setSubscribed(subscribed: Boolean) {
        _isSubscribed.value = subscribed
    }
}

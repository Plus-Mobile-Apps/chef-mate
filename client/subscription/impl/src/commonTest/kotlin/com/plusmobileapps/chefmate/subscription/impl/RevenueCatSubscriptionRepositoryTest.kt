@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.subscription.impl

import com.plusmobileapps.chefmate.Environment
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.devsettings.DeveloperPreferences
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class RevenueCatSubscriptionRepositoryTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val gateway = FakeGateway()
    private val auth = FakeAuthenticationRepository()
    private val preferences = FakePreferences()

    private fun repository(includeDeveloperOverride: Boolean = false) =
        RevenueCatSubscriptionRepository(
            gateway = gateway,
            authenticationRepository = auth,
            preferences = preferences,
            ioContext = dispatcher,
            includeDeveloperOverride = includeDeveloperOverride,
        )

    @Test
    fun signed_out_user_without_entitlement_is_not_subscribed() {
        val repo = repository()

        repo.isSubscribed.value shouldBe false
        gateway.resetCalls shouldBe 1
    }

    @Test
    fun signing_in_identifies_the_user_with_the_store() {
        repository()

        auth.setAnonymous(userId = "user-1")

        gateway.identifiedUserIds shouldBe listOf("user-1")
    }

    @Test
    fun entitled_user_is_subscribed_after_sign_in() {
        gateway.premiumUserIds += "user-1"
        val repo = repository()

        auth.setAnonymous(userId = "user-1")

        repo.isSubscribed.value shouldBe true
    }

    @Test
    fun switching_to_an_unentitled_user_drops_premium() {
        gateway.premiumUserIds += "user-1"
        val repo = repository()
        auth.setAnonymous(userId = "user-1")

        auth.setAnonymous(userId = "user-2")

        repo.isSubscribed.value shouldBe false
    }

    @Test
    fun customer_info_updates_flip_entitlement_live() {
        val repo = repository()

        gateway.updates.tryEmit(true)
        repo.isSubscribed.value shouldBe true

        gateway.updates.tryEmit(false)
        repo.isSubscribed.value shouldBe false
    }

    @Test
    fun identify_failure_keeps_last_known_entitlement() {
        val repo = repository()
        gateway.updates.tryEmit(true)
        gateway.identifyError = IllegalStateException("offline")

        auth.setAnonymous(userId = "user-1")

        repo.isSubscribed.value shouldBe true
    }

    @Test
    fun developer_override_unlocks_premium_when_allowed() {
        val repo = repository(includeDeveloperOverride = true)

        preferences.setSubscribed(true)

        repo.isSubscribed.value shouldBe true
    }

    @Test
    fun developer_override_is_ignored_in_release() {
        preferences.setSubscribed(true)
        val repo = repository(includeDeveloperOverride = false)

        repo.isSubscribed.value shouldBe false
    }

    private class FakeGateway : EntitlementGateway {
        val updates = MutableSharedFlow<Boolean>(extraBufferCapacity = 8)
        val premiumUserIds = mutableSetOf<String>()
        val identifiedUserIds = mutableListOf<String>()
        var resetCalls = 0
        var identifyError: Throwable? = null

        override fun entitlementUpdates(): Flow<Boolean> = updates

        override suspend fun identify(userId: String): Boolean {
            identifyError?.let { throw it }
            identifiedUserIds += userId
            return userId in premiumUserIds
        }

        override suspend fun reset(): Boolean {
            resetCalls += 1
            return false
        }
    }

    private class FakePreferences : DeveloperPreferences {
        override val environment: StateFlow<Environment> = MutableStateFlow(Environment.PROD)
        override val selectedUserIndex: StateFlow<Int?> = MutableStateFlow(null)
        private val _isSubscribed = MutableStateFlow(false)
        override val isSubscribed: StateFlow<Boolean> = _isSubscribed

        override fun setEnvironment(environment: Environment) = Unit

        override fun setSelectedUserIndex(index: Int?) = Unit

        override fun setSubscribed(subscribed: Boolean) {
            _isSubscribed.value = subscribed
        }
    }
}

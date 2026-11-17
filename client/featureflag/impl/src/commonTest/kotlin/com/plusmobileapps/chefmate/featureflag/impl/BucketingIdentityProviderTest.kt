@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.featureflag.impl

import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.ChefMateUser
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers

class BucketingIdentityProviderTest {

    private val settings = MapSettings()

    @Test
    fun When_never_signed_in_Then_device_id_is_used_and_stable() {
        val first = provider(FakeAuthenticationRepository()).identity.value
        val second = provider(FakeAuthenticationRepository()).identity.value

        first shouldNotBe USER_ID
        second shouldBe first
    }

    @Test
    fun When_signed_in_Then_user_id_is_used() {
        val auth = FakeAuthenticationRepository()
        val provider = provider(auth)

        auth.setAuthenticated(user(USER_ID))

        provider.identity.value shouldBe USER_ID
    }

    @Test
    fun When_cold_start_before_session_loads_Then_last_signed_in_user_is_used() {
        // Previous launch: signed in.
        FakeAuthenticationRepository().let { auth ->
            provider(auth)
            auth.setAuthenticated(user(USER_ID))
        }

        // Next launch: auth starts Unauthenticated until Supabase restores the session, which is
        // when launch-time flags (the iOS UI shell) are snapshotted.
        val coldStart = provider(FakeAuthenticationRepository())

        coldStart.identity.value shouldBe USER_ID
    }

    @Test
    fun When_user_signs_out_Then_device_id_is_used_again_on_this_and_next_launch() {
        val auth = FakeAuthenticationRepository()
        val provider = provider(auth)
        val deviceId = provider.identity.value
        auth.setAuthenticated(user(USER_ID))

        auth.setState(AuthState.Unauthenticated)

        provider.identity.value shouldBe deviceId
        provider(FakeAuthenticationRepository()).identity.value shouldBe deviceId
    }

    @Test
    fun When_a_different_user_signs_in_Then_they_replace_the_remembered_user() {
        val auth = FakeAuthenticationRepository()
        provider(auth)
        auth.setAuthenticated(user(USER_ID))
        auth.setAuthenticated(user(OTHER_USER_ID))

        provider(FakeAuthenticationRepository()).identity.value shouldBe OTHER_USER_ID
    }

    private fun provider(auth: FakeAuthenticationRepository) =
        BucketingIdentityProvider(auth, settings, Dispatchers.Unconfined)

    private fun user(id: String) =
        ChefMateUser(
            userId = id,
            userName = "Test",
            userEmail = "test@example.com",
            userProfileImageUrl = null,
        )

    private companion object {
        const val USER_ID = "57bf5203-a8f6-4dca-9f55-5d0f9f3933bb"
        const val OTHER_USER_ID = "dfb70c78-cd9e-478a-911c-5876edd5495e"
    }
}

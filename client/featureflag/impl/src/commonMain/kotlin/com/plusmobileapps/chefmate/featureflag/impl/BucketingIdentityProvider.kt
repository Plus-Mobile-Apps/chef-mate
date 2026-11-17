@file:OptIn(ExperimentalUuidApi::class)

package com.plusmobileapps.chefmate.featureflag.impl

import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.mapState
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * The id flags bucket and allowlist against: the signed-in user's id, or a per-device UUID when
 * signed out.
 *
 * Auth starts as [AuthState.Unauthenticated] on every cold start and only flips to
 * [AuthState.Authenticated] once Supabase has loaded the saved session in the background. Flags
 * read synchronously at launch (e.g. `ios_swiftui_redesign`, snapshotted once to pick the UI shell)
 * would therefore always see the device id and miss the user allowlist. To avoid that, the last
 * signed-in user id is remembered and used until auth settles, and forgotten on an observed
 * sign-out.
 */
@Inject
@SingleIn(AppScope::class)
class BucketingIdentityProvider(
    authRepository: AuthenticationRepository,
    private val settings: Settings,
    @Main mainContext: CoroutineContext,
) {
    private val deviceId: String = ensureDeviceId()

    val identity: StateFlow<String> =
        authRepository.state.mapState { state ->
            when (state) {
                is AuthState.Authenticated -> state.user.userId
                is AuthState.AwaitingEmailVerification,
                AuthState.Unauthenticated -> settings.getStringOrNull(LAST_USER_KEY) ?: deviceId
            }
        }

    init {
        CoroutineScope(mainContext).launch {
            var wasAuthenticated = false
            authRepository.state.collect { state ->
                if (state is AuthState.Authenticated) {
                    wasAuthenticated = true
                    settings.putString(LAST_USER_KEY, state.user.userId)
                } else if (wasAuthenticated) {
                    // A sign-out this session (not the Unauthenticated placeholder auth starts
                    // in before the session loads): stop targeting the previous user.
                    wasAuthenticated = false
                    settings.remove(LAST_USER_KEY)
                }
            }
        }
    }

    private fun ensureDeviceId(): String {
        settings.getStringOrNull(DEVICE_KEY)?.let {
            return it
        }
        val generated = Uuid.random().toString()
        settings.putString(DEVICE_KEY, generated)
        return generated
    }

    private companion object {
        const val DEVICE_KEY = "flags.bucket_id"
        const val LAST_USER_KEY = "flags.last_user_id"
    }
}

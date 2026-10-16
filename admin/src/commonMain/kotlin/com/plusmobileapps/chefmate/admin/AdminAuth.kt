package com.plusmobileapps.chefmate.admin

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow

/**
 * Thin wrapper over Supabase email-OTP auth for the admin app. Sign-in mirrors the shipping app's
 * passwordless flow: request a one-time code by email, then verify it. Authorization is enforced
 * server-side by RLS; [isAdmin] only lets the UI turn non-admins away instead of showing them a
 * dashboard whose writes would all fail.
 */
class AdminAuth(private val client: SupabaseClient) {

    val sessionStatus: Flow<SessionStatus> = client.auth.sessionStatus

    suspend fun sendOtp(email: String): Result<Unit> = runCatching {
        client.auth.signInWith(OTP) { this.email = email }
    }

    suspend fun verifyOtp(email: String, token: String): Result<Unit> = runCatching {
        client.auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = token)
    }

    /** Asks the server whether the signed-in user is in the `admins` allow-list. */
    suspend fun isAdmin(): Result<Boolean> = runCatching {
        client.postgrest.rpc("is_admin").decodeAs<Boolean>()
    }

    suspend fun signOut(): Result<Unit> = runCatching { client.auth.signOut() }
}

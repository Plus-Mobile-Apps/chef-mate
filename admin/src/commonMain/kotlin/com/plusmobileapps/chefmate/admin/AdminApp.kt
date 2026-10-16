package com.plusmobileapps.chefmate.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.jan.supabase.auth.status.SessionStatus

/** No-arg entry point used by both the wasmJs and JVM `main()`. Builds dependencies once. */
@Composable
fun AdminApp() {
    val client = remember { createAdminSupabaseClient() }
    val auth = remember { AdminAuth(client) }
    val repo = remember { SupabaseFeatureFlagAdminRepository(client) }
    AdminRoot(auth = auth, repo = repo)
}

@Composable
fun AdminRoot(auth: AdminAuth, repo: FeatureFlagAdminRepository) {
    AdminTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val status by auth.sessionStatus.collectAsState(initial = SessionStatus.Initializing)
            when (val current = status) {
                is SessionStatus.Authenticated ->
                    AdminAccessGate(auth = auth, userId = current.session.user?.id) {
                        DashboardScreen(repo = repo, auth = auth)
                    }
                is SessionStatus.Initializing ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                else -> SignInScreen(auth = auth)
            }
        }
    }
}

@Composable
private fun AdminTheme(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

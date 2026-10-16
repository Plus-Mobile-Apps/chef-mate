package com.plusmobileapps.chefmate.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private sealed interface AccessState {
    data object Checking : AccessState

    data object Granted : AccessState

    data object Denied : AccessState

    data class Failed(val message: String) : AccessState
}

/**
 * Shows [content] only once the server confirms the signed-in user is an admin. Non-admins get a
 * "no permission" message and a sign-out button instead of the dashboard. This is a UX gate — the
 * real boundary is RLS — so a failed check is shown as an error with retry, never as access.
 */
@Composable
fun AdminAccessGate(auth: AdminAuth, userId: String?, content: @Composable () -> Unit) {
    var state by remember(userId) { mutableStateOf<AccessState>(AccessState.Checking) }
    var attempt by remember(userId) { mutableIntStateOf(0) }

    LaunchedEffect(userId, attempt) {
        state = AccessState.Checking
        state =
            auth
                .isAdmin()
                .fold(
                    onSuccess = { if (it) AccessState.Granted else AccessState.Denied },
                    onFailure = {
                        AccessState.Failed(it.message ?: "Could not verify admin access")
                    },
                )
    }

    when (val current = state) {
        AccessState.Checking ->
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        AccessState.Granted -> content()
        AccessState.Denied ->
            AccessMessage(
                title = "No permission",
                message =
                    "This account isn't an admin. Sign in with an admin account, or ask an " +
                        "existing admin to add you.",
                auth = auth,
            )
        is AccessState.Failed ->
            AccessMessage(
                title = "Couldn't verify access",
                message = current.message,
                auth = auth,
                onRetry = { attempt++ },
            )
    }
}

@Composable
private fun AccessMessage(
    title: String,
    message: String,
    auth: AdminAuth,
    onRetry: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            onRetry?.let {
                Button(onClick = it, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
            }
            TextButton(onClick = { scope.launch { auth.signOut() } }) { Text("Sign out") }
        }
    }
}

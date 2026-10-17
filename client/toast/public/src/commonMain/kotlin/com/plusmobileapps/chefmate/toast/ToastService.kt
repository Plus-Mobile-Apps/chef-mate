package com.plusmobileapps.chefmate.toast

import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.staticCompositionLocalOf
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.components.SnackbarQueue
import kotlinx.coroutines.flow.StateFlow

/**
 * App-scoped service for enqueuing snackbar ("toast") messages from anywhere — a BLoC, a ViewModel,
 * or a composable. A single [ToastServiceHost] near the app root drains [queue] and renders the
 * messages over every screen.
 *
 * BLoCs and ViewModels obtain this by constructor injection (it is bound as a singleton in
 * `AppScope`). Composables that have no BLoC can reach the same instance through
 * [LocalToastService].
 */
interface ToastService {
    /** The pending messages. Observed by [ToastServiceHost]; not meant for general consumption. */
    val queue: StateFlow<SnackbarQueue>

    /**
     * Enqueue a message to be shown. Messages are shown one at a time, in order, none dropped.
     *
     * Pass [actionLabel] to add a button; [onAction] runs when it is tapped (never on plain
     * dismissal). [onAction] is held only until the message is shown and dequeued — for a
     * navigation action, route through something app-scoped rather than capturing a short-lived
     * screen object.
     *
     * [onDismiss] runs when the message goes away *without* its action being tapped (timeout,
     * swipe, or the host leaving composition). Exactly one of [onAction] / [onDismiss] runs per
     * message — useful for deferring a destructive change until an "Undo" window has passed.
     */
    fun show(
        message: TextData,
        actionLabel: TextData? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onAction: (() -> Unit)? = null,
        onDismiss: (() -> Unit)? = null,
    )

    /** Called by the host once a message has been displayed and dismissed, so it can be removed. */
    fun onShown(id: Long)
}

/**
 * Hands composables the same [ToastService] singleton DI provides to BLoCs. Provided once at the
 * app root via `CompositionLocalProvider`. BLoCs/ViewModels should inject [ToastService] directly
 * rather than reach for this — a CompositionLocal is unreachable from those layers anyway.
 */
val LocalToastService =
    staticCompositionLocalOf<ToastService> {
        error(
            "LocalToastService not provided — wrap content in CompositionLocalProvider at App root"
        )
    }

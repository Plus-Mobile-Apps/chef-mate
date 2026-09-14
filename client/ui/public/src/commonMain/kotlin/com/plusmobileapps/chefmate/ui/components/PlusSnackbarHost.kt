package com.plusmobileapps.chefmate.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.text.TextData

/**
 * The vertical space the currently-shown snackbar occupies at the bottom of the screen, or `0.dp`
 * when none is showing. Provided by the app-root toast scaffold and animated as snackbars come and
 * go.
 *
 * Apply it as bottom padding on any element that would otherwise sit *under* a snackbar — a
 * bottom-aligned FAB, a bottom toolbar/nav bar — so it rides up while a snackbar is visible:
 * `Modifier.padding(bottom = LocalSnackbarInset.current)`. [PlusFloatingActionButton] already does
 * this; raw FABs and bottom bars must opt in.
 */
val LocalSnackbarInset: ProvidableCompositionLocal<Dp> = compositionLocalOf { 0.dp }

/**
 * A holder for the live height of the block the app's bottom navigation occupies — the floating
 * pill plus the margin between it and the bottom of the screen — or `0.dp` when no bottom bar is
 * showing. Provided by the app-root toast scaffold so the global snackbar host can float its
 * snackbars *above* the bar instead of rendering on top of it.
 *
 * The bottom bar reports its measured height by applying [reportBottomNavInset] to itself; nothing
 * else should write to this. Read it only inside the toast scaffold. For clearing screen content of
 * the floating bar, read [LocalBottomNavBarInset] instead.
 */
val LocalBottomNavInset: ProvidableCompositionLocal<MutableState<Dp>> = compositionLocalOf {
    mutableStateOf(0.dp)
}

/**
 * Reports this composable's height into [LocalBottomNavInset] so the global snackbar host can sit
 * above it, and resets the reported height to `0.dp` when the composable leaves composition (e.g.
 * navigating away from a screen that shows a bottom bar). Apply it to the app's bottom navigation
 * bar.
 *
 * Place it *before* the bar's outer margins in the modifier chain: `onSizeChanged` measures the
 * node as it stands at that point, and the toast host needs the distance from the bottom of the
 * screen, not the bare height of the pill.
 */
@Composable
fun Modifier.reportBottomNavInset(): Modifier {
    val holder = LocalBottomNavInset.current
    val density = LocalDensity.current
    DisposableEffect(holder) { onDispose { holder.value = 0.dp } }
    return onSizeChanged { holder.value = with(density) { it.height.toDp() } }
}

/**
 * A single queued snackbar. [id] is stable so the UI can dequeue exactly what it showed.
 *
 * Optionally carries an action: when [actionLabel] is non-null the snackbar shows a button, and
 * tapping it invokes [onAction]. [onAction] runs only on tap, never on plain dismissal. The lambda
 * is held for the message's lifetime only (until shown + dequeued), so keep what it captures cheap.
 */
@Immutable
data class SnackbarMessage(
    val id: Long,
    val text: TextData,
    val actionLabel: TextData? = null,
    val duration: SnackbarDuration = SnackbarDuration.Short,
    val onAction: (() -> Unit)? = null,
)

/**
 * Immutable snackbar queue intended to live in a BLoC `Model`. Holds pending messages plus an
 * internal monotonic id counter, so enqueue/dequeue stay correct even when the queue empties and
 * refills — two concurrently-queued messages can never collide on an id.
 *
 * BLoC/ViewModel side:
 * ```
 * _state.update { it.copy(snackbars = it.snackbars.enqueue(message)) }   // emit
 * fun onSnackbarShown(id: Long) =
 *     _state.update { it.copy(snackbars = it.snackbars.dequeue(id)) }     // consume
 * ```
 *
 * Screen side: drop [PlusSnackbarHost] into a `snackbarHost` slot (see [PlusNavContainer]).
 */
@Immutable
data class SnackbarQueue(
    val messages: List<SnackbarMessage> = emptyList(),
    private val nextId: Long = 0L,
) {
    /** The message currently due to be shown, or null when the queue is empty. */
    val head: SnackbarMessage?
        get() = messages.firstOrNull()

    fun enqueue(
        text: TextData,
        actionLabel: TextData? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onAction: (() -> Unit)? = null,
    ): SnackbarQueue =
        copy(
            messages = messages + SnackbarMessage(nextId, text, actionLabel, duration, onAction),
            nextId = nextId + 1,
        )

    fun dequeue(id: Long): SnackbarQueue = copy(messages = messages.filterNot { it.id == id })
}

/**
 * Drains a [SnackbarQueue] through a single Material [SnackbarHost]. Shows the
 * [SnackbarQueue.head], waits for dismissal (Material already serializes display), then calls
 * [onSnackbarShown] so the BLoC can dequeue and the next message plays. No messages are dropped.
 *
 * If the head carries an [SnackbarMessage.actionLabel], its button is shown and tapping it runs
 * [SnackbarMessage.onAction] before the message is dequeued.
 *
 * @param queue the queue from BLoC state.
 * @param onSnackbarShown called with the shown message's id once it is dismissed.
 */
@Composable
fun PlusSnackbarHost(
    queue: SnackbarQueue,
    onSnackbarShown: (id: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hostState = remember { SnackbarHostState() }
    val head = queue.head
    // localized() must resolve in composition; capture results for the effect below.
    val text = head?.text?.localized()
    val actionLabel = head?.actionLabel?.localized()

    LaunchedEffect(head?.id) {
        if (head != null && text != null) {
            try {
                val result =
                    hostState.showSnackbar(
                        message = text,
                        actionLabel = actionLabel,
                        duration = head.duration,
                    )
                if (result == SnackbarResult.ActionPerformed) head.onAction?.invoke()
            } finally {
                onSnackbarShown(head.id)
            }
        }
    }

    SnackbarHost(hostState = hostState, modifier = modifier)
}

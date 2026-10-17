package com.plusmobileapps.chefmate.toast.testing

import androidx.compose.material3.SnackbarDuration
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.toast.ToastService
import com.plusmobileapps.chefmate.ui.components.SnackbarQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory [ToastService] for tests. Behaves like the real one (a working queue) and additionally
 * records every message passed to [show] in [shown] so BLoC/ViewModel tests can assert what was
 * enqueued without driving the UI host.
 */
class FakeToastService : ToastService {

    private val _queue = MutableStateFlow(SnackbarQueue())
    override val queue: StateFlow<SnackbarQueue> = _queue.asStateFlow()

    /** Every message passed to [show], in order. */
    val shown: List<TextData>
        get() = _shown

    private val _shown = mutableListOf<TextData>()

    /** Every action label passed to [show], in order (null when a message had no action). */
    val shownActionLabels: List<TextData?>
        get() = _shownActionLabels

    private val _shownActionLabels = mutableListOf<TextData?>()

    /** The action callback from the most recent [show], so tests can invoke a toast's action. */
    var lastOnAction: (() -> Unit)? = null
        private set

    /** The dismiss callback from the most recent [show], so tests can simulate a timeout. */
    var lastOnDismiss: (() -> Unit)? = null
        private set

    override fun show(
        message: TextData,
        actionLabel: TextData?,
        duration: SnackbarDuration,
        onAction: (() -> Unit)?,
        onDismiss: (() -> Unit)?,
    ) {
        _shown += message
        _shownActionLabels += actionLabel
        lastOnAction = onAction
        lastOnDismiss = onDismiss
        _queue.update { it.enqueue(message, actionLabel, duration, onAction, onDismiss) }
    }

    override fun onShown(id: Long) {
        _queue.update { it.dequeue(id) }
    }
}

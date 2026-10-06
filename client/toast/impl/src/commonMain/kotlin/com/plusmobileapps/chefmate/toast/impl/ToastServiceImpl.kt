package com.plusmobileapps.chefmate.toast.impl

import androidx.compose.material3.SnackbarDuration
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.toast.ToastService
import com.plusmobileapps.chefmate.ui.components.SnackbarQueue
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ToastServiceImpl : ToastService {

    private val _queue = MutableStateFlow(SnackbarQueue())
    override val queue: StateFlow<SnackbarQueue> = _queue.asStateFlow()

    override fun show(
        message: TextData,
        actionLabel: TextData?,
        duration: SnackbarDuration,
        onAction: (() -> Unit)?,
        onDismiss: (() -> Unit)?,
    ) {
        _queue.update { it.enqueue(message, actionLabel, duration, onAction, onDismiss) }
    }

    override fun onShown(id: Long) {
        _queue.update { it.dequeue(id) }
    }
}

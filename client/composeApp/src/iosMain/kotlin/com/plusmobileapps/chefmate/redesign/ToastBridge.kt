package com.plusmobileapps.chefmate.redesign

import androidx.compose.material3.SnackbarDuration
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.toast.ToastService
import com.plusmobileapps.chefmate.ui.components.SnackbarMessage
import com.plusmobileapps.chefmate.ui.components.SnackbarQueue

/**
 * One toast as the SwiftUI toast host sees it. The message's `onAction` / `onDismiss` callbacks
 * stay on the Kotlin side; Swift reports the outcome through [ToastBridge.finish].
 */
class NativeToast internal constructor(internal val message: SnackbarMessage) {
    val id: Long
        get() = message.id

    val text: TextData
        get() = message.text

    val actionLabel: TextData?
        get() = message.actionLabel

    /**
     * Seconds the toast stays up, or `null` for [SnackbarDuration.Indefinite]. Matches Material's
     * timings: short 4s, long 10s.
     */
    val durationSeconds: Double?
        get() =
            when (message.duration) {
                SnackbarDuration.Short -> 4.0
                SnackbarDuration.Long -> 10.0
                SnackbarDuration.Indefinite -> null
            }
}

/**
 * Drives the native toast host in the SwiftUI redesign from the same [ToastService] the Compose
 * `ToastScaffold` drains, keeping its contract: messages play one at a time, in order, and exactly
 * one of `onAction` / `onDismiss` runs per message.
 */
object ToastBridge {
    /**
     * The message due to be shown in [queue] (the value of [ToastService.queue], typed `Any` so
     * Swift needn't name [SnackbarQueue]), or `null` when it's empty.
     */
    fun head(queue: Any): NativeToast? = (queue as? SnackbarQueue)?.head?.let(::NativeToast)

    /**
     * Ends [toast]: runs its action when [actionTapped], otherwise its dismiss callback, then
     * dequeues it so the next message plays. Call exactly once per toast.
     */
    fun finish(service: ToastService, toast: NativeToast, actionTapped: Boolean) {
        try {
            if (actionTapped) toast.message.onAction?.invoke()
            else toast.message.onDismiss?.invoke()
        } finally {
            service.onShown(toast.id)
        }
    }
}

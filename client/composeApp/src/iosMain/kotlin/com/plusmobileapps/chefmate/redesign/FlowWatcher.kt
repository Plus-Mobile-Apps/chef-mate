package com.plusmobileapps.chefmate.redesign

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Cancels a subscription started from Swift. */
fun interface Cancellable {
    fun cancel()
}

/**
 * Lets the SwiftUI redesign observe a BLoC's [StateFlow] without a Flow interop plugin. Swift wraps
 * this in `ObservedFlow`, which republishes each value to SwiftUI.
 */
class FlowWatcher<T : Any>(private val flow: StateFlow<T>) {
    val value: T
        get() = flow.value

    /** Delivers every value (the current one first) on the main thread until cancelled. */
    fun watch(onEach: (T) -> Unit): Cancellable {
        val job = CoroutineScope(Dispatchers.Main.immediate).launch { flow.collect { onEach(it) } }
        return Cancellable { job.cancel() }
    }
}

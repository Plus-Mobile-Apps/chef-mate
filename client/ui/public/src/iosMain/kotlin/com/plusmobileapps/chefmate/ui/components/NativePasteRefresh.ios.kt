@file:Suppress("ktlint:standard:filename")

package com.plusmobileapps.chefmate.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardChangedNotification

@Composable
internal actual fun rememberNativePasteRefreshKey(isFocused: Boolean): Int {
    var key by remember { mutableIntStateOf(0) }
    // The field checked the clipboard when it was composed, so start from the current count.
    // Reading changeCount never triggers iOS's paste-permission prompt.
    var seenChangeCount by remember { mutableLongStateOf(currentChangeCount()) }
    val currentIsFocused by rememberUpdatedState(isFocused)

    fun refreshIfStale(allowWhileFocused: Boolean) {
        val changeCount = currentChangeCount()
        if (changeCount == seenChangeCount) return
        if (currentIsFocused && !allowWhileFocused) return
        seenChangeCount = changeCount
        key++
    }

    LaunchedEffect(isFocused) { if (!isFocused) refreshIfStale(allowWhileFocused = false) }

    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val queue = NSOperationQueue.mainQueue
        val pasteboardObserver =
            center.addObserverForName(UIPasteboardChangedNotification, null, queue) { _ ->
                refreshIfStale(allowWhileFocused = false)
            }
        val activeObserver =
            center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, queue) { _ ->
                refreshIfStale(allowWhileFocused = true)
            }
        onDispose {
            center.removeObserver(pasteboardObserver)
            center.removeObserver(activeObserver)
        }
    }
    return key
}

private fun currentChangeCount(): Long = UIPasteboard.generalPasteboard.changeCount

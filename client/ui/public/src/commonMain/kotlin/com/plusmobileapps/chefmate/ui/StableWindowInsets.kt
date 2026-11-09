package com.plusmobileapps.chefmate.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * These insets as an instance that compares equal for as long as their values don't change. Pass
 * this, not a raw `WindowInsets.systemBars` (or any other platform inset), to
 * `Modifier.windowInsetsPadding` on content that Decompose's stack animations move around.
 *
 * Outside Android, Compose Multiplatform builds each platform inset as a fresh object on every
 * read, and those objects only compare by identity. So `windowInsetsPadding` sees "new" insets on
 * every recomposition and walks its subtree to push them down. If that recomposition lands while
 * `movableContentOf` is re-inserting the node (which `predictiveBackAnimation` does on pop), the
 * node isn't attached yet and Compose crashes with "visitSubtreeIf called on an unattached node".
 * Android's insets are already long-lived objects, so it returns them untouched there.
 */
@Composable expect fun WindowInsets.stableWindowInsets(): WindowInsets

/** Snapshots the current values into a fixed, value-equal [WindowInsets]. */
@Composable
internal fun WindowInsets.snapshotWindowInsets(): WindowInsets {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val left = getLeft(density, layoutDirection)
    val top = getTop(density)
    val right = getRight(density, layoutDirection)
    val bottom = getBottom(density)
    return remember(left, top, right, bottom) { WindowInsets(left, top, right, bottom) }
}

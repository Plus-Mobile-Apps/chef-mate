package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The vertical space the app's floating bottom navigation bar occupies at the bottom of the tab
 * content area — its height plus the margin below it — or `0.dp` when no bar is showing (larger
 * window sizes, or while the keyboard is up and the bar has slid away).
 *
 * Unlike the old `Scaffold(bottomBar = …)` layout, tab content now fills the screen and the bar
 * floats *over* it, so that content can be seen through the glass. Anything that must stay
 * reachable has to account for the bar itself:
 * - scrollable content adds it to its `contentPadding` (or a trailing spacer), so the last item can
 *   still be scrolled clear of the bar;
 * - bottom-anchored UI (FABs, input rows, toolbars) adds it as real bottom padding via
 *   [bottomNavBarPadding].
 *
 * It animates to `0.dp` as the bar hides for the keyboard, so consumers glide rather than jump.
 */
val LocalBottomNavBarInset: ProvidableCompositionLocal<Dp> = compositionLocalOf { 0.dp }

/**
 * Pads this composable clear of the floating bottom navigation bar. For bottom-anchored UI that
 * must never sit under the bar — FABs, input rows, pinned toolbars.
 */
@Composable
fun Modifier.bottomNavBarPadding(): Modifier = padding(bottom = LocalBottomNavBarInset.current)

/**
 * `contentPadding` for a scrollable tab root: adds [LocalBottomNavBarInset] underneath whatever
 * padding the screen already needed (FAB clearance, its own spacing), so the list scrolls under the
 * floating bar instead of ending at it.
 */
@Composable
fun bottomNavContentPadding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
): PaddingValues =
    PaddingValues(
        start = start,
        top = top,
        end = end,
        bottom = bottom + LocalBottomNavBarInset.current,
    )

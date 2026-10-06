package com.plusmobileapps.chefmate.toast

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.ui.components.LocalBottomNavInset
import com.plusmobileapps.chefmate.ui.components.LocalSnackbarInset

/**
 * App-root wrapper that renders [content] with a single global [ToastServiceHost] overlaid at the
 * bottom, and makes the toast reachable to everything below it:
 * - [LocalToastService] so no-BLoC composables can call `show(...)`.
 * - [LocalSnackbarInset] — the live height of the shown snackbar (animated, `0.dp` when none) — so
 *   bottom-aligned FABs and toolbars can ride up instead of being covered. See [LocalSnackbarInset]
 *   for how consumers apply it.
 * - [LocalBottomNavInset] — a holder the app's bottom navigation bar reports its height into (via
 *   `Modifier.reportBottomNavInset()`), so snackbars float *above* the bar instead of covering it.
 *
 * With no bottom bar present the host still clears the system navigation-bar inset, matching where
 * bottom-anchored toolbars/FABs sit (they already lift above it), so the gap between them and a
 * snackbar stays tight instead of leaving a nav-bar-sized space.
 *
 * Place this once, at the app root, around the root navigation.
 */
@Composable
fun ToastScaffold(
    toastService: ToastService,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var hostHeight by remember { mutableStateOf(0.dp) }
    val bottomNavInset = remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    // Animate so FABs/toolbars glide up and back rather than jumping as snackbars come and go.
    val inset by animateDpAsState(hostHeight, label = "snackbarInset")
    // Lift the snackbar over the bottom nav bar when one is showing; otherwise fall back to the
    // system navigation-bar inset so the host never renders under it. The nav bar reports the whole
    // block it occupies — the floating pill plus the margin below it, i.e. the distance up from the
    // bottom of the screen — so that figure already spans the system inset. Take the larger of the
    // two to avoid double counting. Animated so it glides as bars/screens come and go.
    val navBarInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val bottomBarInset by
        animateDpAsState(maxOf(bottomNavInset.value, navBarInset), label = "bottomNavInset")

    CompositionLocalProvider(
        LocalToastService provides toastService,
        LocalSnackbarInset provides inset,
        LocalBottomNavInset provides bottomNavInset,
    ) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
            ToastServiceHost(
                service = toastService,
                // Pad the host up by the bottom-bar / nav-bar inset (outside onSizeChanged) so it
                // floats above them, while hostHeight stays the bare snackbar height — keeping the
                // FAB-to-snackbar gap constant whether or not a bottom bar is present.
                modifier =
                    Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = bottomBarInset)
                        .onSizeChanged { hostHeight = with(density) { it.height.toDp() } },
            )
        }
    }
}

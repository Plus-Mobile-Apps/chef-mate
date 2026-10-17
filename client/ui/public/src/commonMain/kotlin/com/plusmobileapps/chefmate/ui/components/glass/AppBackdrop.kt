package com.plusmobileapps.chefmate.ui.components.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * A handle on the content that the app's glass navigation surfaces blur behind themselves.
 *
 * This is a thin wrapper around Haze's `HazeState`. It exists so that Haze stays an
 * `implementation` dependency of this module: no Haze type appears in a public signature, so
 * consumers (bottomnav, recipe, grocery, meal, browser, settings) don't inherit it on their compile
 * classpath and their ABI stays free of a third-party UI library.
 *
 * Create one with [rememberAppBackdrop], mark the content to be blurred with [appBackdropSource],
 * and hand the same instance to the glass surface drawn over it (see [GlassSurface]).
 */
@Immutable class AppBackdrop internal constructor(internal val state: HazeState)

/** Remembers an [AppBackdrop] for the lifetime of the calling composable. */
@Composable
fun rememberAppBackdrop(): AppBackdrop {
    val state = remember { HazeState() }
    return remember(state) { AppBackdrop(state) }
}

/**
 * The nearest enclosing [AppBackdrop], or `null` where nothing is capturing a backdrop (screenshot
 * tests, previews, and any screen rendered outside the navigation shells). A `null` backdrop is a
 * supported state everywhere — [GlassSurface] falls back to an opaque-ish tint.
 */
val LocalAppBackdrop: ProvidableCompositionLocal<AppBackdrop?> = compositionLocalOf { null }

/**
 * Marks this composable's content as the source that [backdrop]'s glass surfaces blur. Apply it to
 * the full-bleed content behind a floating navigation bar or rail.
 *
 * Passing `null` is a no-op, so callers don't need to branch.
 *
 * Note: the subtree below this modifier is rendered into an offscreen graphics layer every frame,
 * so keep it to the screen content rather than wrapping the whole app.
 */
fun Modifier.appBackdropSource(backdrop: AppBackdrop?): Modifier =
    if (backdrop == null) this else hazeSource(backdrop.state)

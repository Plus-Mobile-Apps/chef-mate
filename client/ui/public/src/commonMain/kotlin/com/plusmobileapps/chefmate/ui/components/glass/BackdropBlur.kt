package com.plusmobileapps.chefmate.ui.components.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

private val BackdropBlurRadius = 24.dp
private const val BackdropNoiseFactor = 0.04f
// Where the platform can't blur (older Android), the content behind would otherwise show through a
// plain translucent tint and fight with the chrome's labels, so fall back to a near-opaque scrim.
private const val FallbackAlpha = 0.95f

/**
 * Remembers an [AppBackdrop] for frosted chrome (app bars, floating toolbars) drawn over a screen's
 * own content, or `null` in previews and screenshot tests. Those renderers have no blur pipeline,
 * so callers fall back to their opaque colors there and goldens stay deterministic.
 */
@Composable
internal fun rememberChromeBackdrop(): AppBackdrop? =
    if (LocalInspectionMode.current) null else rememberAppBackdrop()

/**
 * A frosted, non-refracting blur of [backdrop] behind this element, tinted with [tint] over an
 * opaque [background]. A lighter treatment than [GlassSurface] — no refraction or specular — meant
 * for chrome that spans the screen edge, where lens optics would read as distortion. The blur fills
 * the element's bounds, so clip before applying it for a non-rectangular shape.
 *
 * Passing a `null` [backdrop] is a no-op.
 */
@Composable
internal fun Modifier.backdropBlur(
    backdrop: AppBackdrop?,
    tint: Color,
    background: Color,
): Modifier {
    if (backdrop == null) return this
    // Remembered so a recomposition doesn't hand the node a new style instance every frame.
    val style =
        remember(tint, background) {
            HazeBlurStyle {
                blurRadius(BackdropBlurRadius)
                noiseFactor(BackdropNoiseFactor)
                backgroundColor(background)
                colorEffects(listOf(HazeColorEffect.tint(tint)))
                fallbackColorEffect(HazeColorEffect.tint(tint.copy(alpha = FallbackAlpha)))
            }
        }
    return hazeBlur(input = HazeInput.Sources(backdrop.state), style = style)
}

package com.plusmobileapps.chefmate.ui.components.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.blurEffect
import dev.chrisbanes.haze.hazeEffect

/** Shared look of the app's floating "glass" navigation surfaces. */
object GlassDefaults {
    /** A true pill — the floating bottom bar and nav rail are both fully rounded. */
    val shape: Shape = RoundedCornerShape(percent = 50)

    /**
     * Minimum gap between a floating surface and the bottom of the screen. On devices with a system
     * navigation affordance the surface sits directly on top of it instead — see the clamp in
     * `PlusBottomBar`. Adding to the inset rather than clamping strands the bar ~46-60dp up.
     */
    val minEdgeGap: Dp = 12.dp
}

private val BlurRadiusLight = 28.dp
private val BlurRadiusDark = 32.dp
private const val NoiseFactorLight = 0.04f
private const val NoiseFactorDark = 0.06f
private const val TintAlphaLight = 0.55f
private const val TintAlphaDark = 0.42f
private const val FallbackTintAlphaLight = 0.94f
private const val FallbackTintAlphaDark = 0.92f
private val BorderWidth = 1.dp
private val ShadowElevationLight = 10.dp
private val ShadowElevationDark = 6.dp
private const val ShadowAlphaLight = 0.30f
private const val ShadowAlphaDark = 0.45f

/**
 * A translucent, blurred, rounded container — the "liquid glass" surface the floating navigation
 * bar and nav rail are built from.
 *
 * [backdrop] is the content rendered behind this surface (see [appBackdropSource]). When it is
 * `null` — screenshot tests and previews, where the platform has no blur pipeline — the surface
 * renders a near-opaque tint of the same shape, margins and border instead, so layout still reads
 * correctly and goldens stay deterministic. On Android below API 31 Haze itself falls back to a
 * scrim using the same tint, so that case needs no branch here.
 *
 * Sizing is the caller's job: pass the margins and any inset on [modifier], and let [content]
 * (typically a navigation bar with a transparent container) establish the height.
 */
@Composable
fun GlassSurface(
    backdrop: AppBackdrop?,
    modifier: Modifier = Modifier,
    shape: Shape = GlassDefaults.shape,
    content: @Composable BoxScope.() -> Unit,
) {
    val colorScheme = ChefMateTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f

    val tint =
        colorScheme.surfaceContainer.copy(alpha = if (isDark) TintAlphaDark else TintAlphaLight)
    val fallback =
        colorScheme.surfaceContainer.copy(
            alpha = if (isDark) FallbackTintAlphaDark else FallbackTintAlphaLight
        )
    val borderBrush =
        Brush.verticalGradient(
            if (isDark) {
                listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.04f))
            } else {
                listOf(
                    colorScheme.onSurface.copy(alpha = 0.14f),
                    colorScheme.onSurface.copy(alpha = 0.04f),
                )
            }
        )
    val shadowColor = Color.Black.copy(alpha = if (isDark) ShadowAlphaDark else ShadowAlphaLight)

    val glass =
        if (backdrop == null) {
            Modifier.background(fallback)
        } else {
            Modifier.hazeEffect(backdrop.state) {
                blurEffect {
                    blurRadius = if (isDark) BlurRadiusDark else BlurRadiusLight
                    noiseFactor = if (isDark) NoiseFactorDark else NoiseFactorLight
                    // Drawn behind the blurred content so a translucent window background can
                    // never show through the pill.
                    backgroundColor = colorScheme.surface
                    colorEffects = listOf(HazeColorEffect.tint(tint))
                    // Used verbatim when the platform can't blur (Android < 12). Note this is
                    // HazeColorEffect.tint, not HazeBlurDefaults.tint — the latter silently
                    // multiplies alpha by 0.7.
                    fallbackTint = HazeColorEffect.tint(fallback)
                }
            }
        }

    Box(
        modifier =
            modifier
                .shadow(
                    elevation = if (isDark) ShadowElevationDark else ShadowElevationLight,
                    shape = shape,
                    clip = false,
                    ambientColor = shadowColor,
                    spotColor = shadowColor,
                )
                .clip(shape)
                .then(glass)
                .border(width = BorderWidth, brush = borderBrush, shape = shape),
        content = content,
    )
}

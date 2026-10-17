package com.plusmobileapps.chefmate.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * The widest an onboarding preview (static image or gif) is allowed to grow. Capped at roughly a
 * phone screen's width so the previews stay phone-sized and don't stretch tall on wide windows
 * (desktop/tablet), where the step content can otherwise be up to 600dp across.
 */
private val PreviewMaxWidth: Dp = 560.dp

/**
 * A framed preview of a real feature screen shown at the top of an onboarding step so the user can
 * see what the feature looks like. [light]/[dark] are rendered from the real feature screens by
 * `OnboardingCapturesScreenshotTest` (copy them in with `scripts/update-onboarding-previews.sh`);
 * the variant is picked from the active theme (not the system setting) so it stays correct even
 * when the theme is forced. The frame wraps the whole capture — width is capped at
 * [PreviewMaxWidth] and the height follows the image's own aspect ratio, so nothing is cropped.
 */
@Composable
internal fun OnboardingPreviewImage(
    light: DrawableResource,
    dark: DrawableResource,
    modifier: Modifier = Modifier,
) {
    val isDark = ChefMateTheme.colorScheme.surface.luminance() < 0.5f

    Image(
        painter = painterResource(if (isDark) dark else light),
        contentDescription = null, // decorative; the step title and body describe the feature
        // Fill the (capped) width and let the height follow the capture's aspect ratio — the whole
        // screenshot shows, none of it is cropped off.
        contentScale = ContentScale.FillWidth,
        // widthIn must come before fillMaxWidth: fillMaxWidth fixes the width to the full available
        // width (min == max), which a later widthIn can no longer shrink. Capping the incoming
        // constraint first, then filling up to it, is what actually bounds the width on wide
        // windows.
        modifier = modifier.widthIn(max = PreviewMaxWidth).fillMaxWidth().onboardingPreviewFrame(),
    )
}

/** Shared framing (rounded card, shadow, outline) for an onboarding preview image. */
@Composable
internal fun Modifier.onboardingPreviewFrame(): Modifier {
    val shape = RoundedCornerShape(20.dp)
    return this.shadow(elevation = 8.dp, shape = shape)
        .clip(shape)
        .border(1.dp, ChefMateTheme.colorScheme.outlineVariant, shape)
}

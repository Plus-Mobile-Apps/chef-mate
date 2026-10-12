@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.ui.components.glass.AppBackdrop
import com.plusmobileapps.chefmate.ui.components.glass.LocalAppBackdrop
import com.plusmobileapps.chefmate.ui.components.glass.backdropBlur
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Lighter than the header's tint: the toolbar is a small pill of icons, and more of the content
// reading through it is what makes it feel like it floats over the page.
private const val ToolbarBlurTintAlpha = 0.6f

/**
 * A bottom-anchored horizontal floating toolbar that automatically rides up above a shown snackbar,
 * so it is never covered. Wraps Material's [HorizontalFloatingToolbar] and bakes in the
 * [LocalSnackbarInset] bottom padding — the toolbar equivalent of [PlusFloatingActionButton].
 * Prefer this over a raw `HorizontalFloatingToolbar` for a screen's bottom action toolbar.
 *
 * Inside a [PlusHeaderContainer]'s `floatingToolbar` slot the toolbar is frosted: the content
 * scrolling behind it shows through a blur (see [LocalAppBackdrop]). Elsewhere, and in previews and
 * screenshot tests, it keeps Material's opaque container.
 *
 * @param expanded whether the toolbar is expanded; forwarded to [HorizontalFloatingToolbar].
 * @param floatingActionButton optional FAB shown alongside the toolbar (rides up with it).
 * @param content the toolbar's action items, laid out in a `Row`.
 */
@Composable
fun PlusToolbar(
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
) {
    // Lift the toolbar above any shown snackbar.
    val toolbarModifier = modifier.padding(bottom = LocalSnackbarInset.current)
    val backdrop = LocalAppBackdrop.current
    if (backdrop != null) {
        FrostedToolbar(
            backdrop = backdrop,
            expanded = expanded,
            floatingActionButton = floatingActionButton,
            modifier = toolbarModifier,
            content = content,
        )
        return
    }
    // HorizontalFloatingToolbar exposes the FAB via a separate overload (non-null param), so route
    // to whichever matches.
    if (floatingActionButton != null) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            floatingActionButton = floatingActionButton,
            modifier = toolbarModifier,
            content = content,
        )
    } else {
        HorizontalFloatingToolbar(
            expanded = expanded,
            modifier = toolbarModifier,
            content = content,
        )
    }
}

/**
 * [HorizontalFloatingToolbar] with a blurred, translucent container. Material's FAB overload draws
 * the toolbar and FAB inside one node, which would put the (rectangular) blur behind the FAB too,
 * so the FAB is laid out beside a FAB-less toolbar instead, at the same gap Material uses.
 */
@Composable
private fun FrostedToolbar(
    backdrop: AppBackdrop,
    expanded: Boolean,
    floatingActionButton: (@Composable () -> Unit)?,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = FloatingToolbarDefaults.ContainerShape
    val defaultColors = FloatingToolbarDefaults.standardFloatingToolbarColors()
    val colors =
        FloatingToolbarDefaults.standardFloatingToolbarColors(
            toolbarContainerColor = Color.Transparent
        )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalFloatingToolbar(
            expanded = expanded,
            colors = colors,
            shape = shape,
            // A shadow under a translucent container shows through it as a dark smudge.
            expandedShadowElevation = 0.dp,
            collapsedShadowElevation = 0.dp,
            modifier =
                Modifier.clip(shape)
                    .backdropBlur(
                        backdrop = backdrop,
                        tint =
                            defaultColors.toolbarContainerColor.copy(alpha = ToolbarBlurTintAlpha),
                        background = ChefMateTheme.colorScheme.background,
                    ),
            content = content,
        )
        floatingActionButton?.invoke()
    }
}

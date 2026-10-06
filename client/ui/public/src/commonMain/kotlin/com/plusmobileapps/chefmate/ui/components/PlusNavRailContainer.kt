package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.components.glass.GlassDefaults
import com.plusmobileapps.chefmate.ui.components.glass.GlassSurface
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

data class NavRailItem(
    val label: TextData,
    val selected: Boolean,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
)

/**
 * The wide-window counterpart of the floating bottom navigation pill: a rounded rail floating at
 * the start edge, inset from the window edges and sharing the pill's shape, border and shadow.
 *
 * Unlike the bottom bar, screen content is inset past the rail rather than running behind it. It
 * would be prettier to blur real content through the rail, but screens measure themselves to pick a
 * layout — [PlusResponsiveContainer] window size classes, the recipe list's filter sidebar — and
 * full-bleed content makes every one of those breakpoints read a window ~250dp wider than the space
 * it actually has. So the rail floats over the app background, which is why it is drawn as a solid
 * surface rather than glass: there would be nothing behind it to see.
 */
@Composable
fun PlusNavRailHeaderContainer(
    modifier: Modifier = Modifier,
    navRail: List<NavRailItem>,
    expandedItems: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val density = LocalDensity.current
    var railBlockWidth by remember { mutableStateOf(0.dp) }

    val cutoutStart =
        with(density) { WindowInsets.displayCutout.getLeft(density, LayoutDirection.Ltr).toDp() }
    // Clamp rather than add, matching the bottom pill: on a notched device in landscape the rail
    // sits just clear of the cutout instead of a full margin beyond it.
    val startMargin = maxOf(cutoutStart, ChefMateTheme.dimens.paddingNormal)
    val verticalMargin = ChefMateTheme.dimens.paddingNormal

    val paddingValues: PaddingValues =
        with(density) {
            PaddingValues.Absolute(
                left = railBlockWidth,
                right = WindowInsets.displayCutout.getRight(density, LayoutDirection.Ltr).toDp(),
                top = WindowInsets.statusBars.getTop(density).toDp(),
                bottom = WindowInsets.navigationBars.getBottom(density).toDp(),
            )
        }

    Box(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ChefMateTheme.colorScheme.background,
            contentColor = ChefMateTheme.colorScheme.onBackground,
        ) {
            content(paddingValues)
        }

        GlassSurface(
            backdrop = null,
            // A stadium is right for the narrow icon rail but absurd on the 240dp expanded one,
            // where the corner radius would resolve against the width.
            shape = if (expandedItems) RoundedCornerShape(28.dp) else GlassDefaults.shape,
            modifier =
                Modifier.align(Alignment.CenterStart)
                    // Measured before the margins so the reported block is what content has to
                    // clear from the start edge, not the bare rail width.
                    .onSizeChanged { railBlockWidth = with(density) { it.width.toDp() } }
                    .padding(start = startMargin, top = verticalMargin, bottom = verticalMargin),
        ) {
            NavigationRail(
                containerColor = Color.Transparent,
                contentColor = ChefMateTheme.colorScheme.onSurface,
                windowInsets =
                    WindowInsets(bottom = WindowInsets.systemGestures.getBottom(density)),
            ) {
                navRail.forEach { item ->
                    if (expandedItems) {
                        PlusExpandedNavRailItem(
                            selected = item.selected,
                            onClick = item.onClick,
                            icon = { item.icon() },
                            label = { Text(item.label.localized()) },
                        )
                    } else {
                        NavigationRailItem(
                            selected = item.selected,
                            onClick = item.onClick,
                            icon = { item.icon() },
                            label = { Text(item.label.localized()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlusExpandedNavRailItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: @Composable () -> Unit,
) {
    // Unselected items are transparent rather than surfaceContainer: the rail is one rounded
    // surface now, and a second opaque rectangle per item would show its corners through it.
    val backgroundColor =
        if (selected) ChefMateTheme.colorScheme.secondaryContainer else Color.Transparent

    val contentColor =
        if (selected) {
            ChefMateTheme.colorScheme.onSecondaryContainer
        } else {
            ChefMateTheme.colorScheme.onSurface
        }

    Row(
        modifier =
            modifier
                .width(240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(backgroundColor)
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            icon()
            label()
        }
    }
}

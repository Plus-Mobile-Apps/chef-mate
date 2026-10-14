@file:OptIn(ExperimentalMaterial3Api::class)

package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.ui.components.glass.appBackdropSource
import com.plusmobileapps.chefmate.ui.components.glass.backdropBlur
import com.plusmobileapps.chefmate.ui.components.glass.rememberChromeBackdrop
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Matches PlusHeaderContainer's header: the content reads through the blur while the title and
// actions stay legible over whatever scrolls underneath.
private const val HeaderBlurTintAlpha = 0.72f

/**
 * A container to embed inside a navigation shell (a bottom-nav tab), which already applies the
 * status-bar inset — so unlike [PlusHeaderContainer], the header reserves none of its own.
 *
 * [headerContent] is stacked directly below the header — a search bar, banners, or a custom app bar
 * when [data] is [PlusHeaderData.None] — and is treated as part of it.
 *
 * When [drawContentBehindHeader] is true the header and [headerContent] float over [content] as one
 * frosted panel, so whatever scrolls up behind them shows through the blur. [content] is then laid
 * out full-height and must reserve [LocalPlusHeaderOverlap] at its top itself — typically as an
 * inner lazy list's top content padding, so the list scrolls up behind the header. With
 * [scrollEnabled] the container reserves it for you. In this mode the container also grows up under
 * the status bar the shell reserved ([LocalNavShellTopInset]), so the frosted panel reaches the top
 * of the screen.
 */
@Composable
fun PlusNavContainer(
    data: PlusHeaderData,
    scrollEnabled: Boolean = true,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    drawContentBehindHeader: Boolean = false,
    headerContent: @Composable ColumnScope.() -> Unit = {},
) {
    val scrollState = rememberScrollState()
    // The modifier goes ahead of the scroll so it applies to the viewport, not the scrolled column.
    fun contentModifier(before: Modifier = Modifier): Modifier =
        Modifier.fillMaxSize().then(before).let {
            if (scrollEnabled) it.verticalScroll(scrollState) else it
        }

    if (!drawContentBehindHeader) {
        Column(modifier = modifier.fillMaxSize()) {
            if (data !is PlusHeaderData.None) {
                PlusHeader(data = data, windowInsets = WindowInsets())
            }
            headerContent()

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(modifier = contentModifier(), verticalArrangement = verticalArrangement) {
                    content()
                }
            }
        }
        return
    }

    // Null in previews and screenshot tests, where the header keeps an opaque background instead.
    val backdrop = rememberChromeBackdrop()
    val background = ChefMateTheme.colorScheme.background
    val headerHeight = remember { mutableStateOf(0.dp) }
    val overlap = remember(headerHeight) { PlusHeaderOverlap(headerHeight) }
    // A navigation shell lays this out below the status bar. Grow up into that strip so content
    // scrolls under the status bar too and the frosted panel runs to the screen edge, reserving the
    // strip at the top of the panel instead.
    val shellTopInset = LocalNavShellTopInset.current
    Box(modifier = modifier.fillMaxSize().extendUpward(shellTopInset)) {
        HeaderOverlayLayout(
            header = {
                Column(
                    modifier =
                        Modifier.fillMaxWidth().let {
                            if (backdrop == null) {
                                it.background(background)
                            } else {
                                it.backdropBlur(
                                    backdrop = backdrop,
                                    tint = background.copy(alpha = HeaderBlurTintAlpha),
                                    background = background,
                                )
                            }
                        }
                ) {
                    Spacer(modifier = Modifier.height(shellTopInset))
                    if (data !is PlusHeaderData.None) {
                        PlusHeader(
                            data = data,
                            windowInsets = WindowInsets(),
                            containerAlpha = if (backdrop != null) 0f else 1f,
                        )
                    }
                    headerContent()
                }
            },
            onHeaderMeasured = { headerHeight.value = it },
        ) {
            CompositionLocalProvider(LocalPlusHeaderOverlap provides overlap) {
                Column(
                    modifier = contentModifier(Modifier.appBackdropSource(backdrop)),
                    verticalArrangement = verticalArrangement,
                ) {
                    if (scrollEnabled) {
                        Spacer(modifier = Modifier.padding(overlap.contentPadding()))
                    }
                    content()
                }
            }
        }
    }
}

/**
 * Lays this element out [amount] taller and shifted up by [amount], so it draws over the space its
 * parent reserved above it while still reporting its original size to the parent.
 */
private fun Modifier.extendUpward(amount: Dp): Modifier =
    if (amount <= 0.dp) {
        this
    } else {
        layout { measurable, constraints ->
            val extra = amount.roundToPx()
            if (!constraints.hasBoundedHeight) {
                val placeable = measurable.measure(constraints)
                return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            }
            val placeable =
                measurable.measure(
                    constraints.copy(
                        minHeight = constraints.minHeight + extra,
                        maxHeight = constraints.maxHeight + extra,
                    )
                )
            layout(placeable.width, placeable.height - extra) { placeable.place(0, -extra) }
        }
    }

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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
 * [scrollEnabled] the container reserves it for you.
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
    Box(modifier = modifier.fillMaxSize()) {
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

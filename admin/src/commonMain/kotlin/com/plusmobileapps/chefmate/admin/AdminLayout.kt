package com.plusmobileapps.chefmate.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Coarse window-width buckets, following the Material window size classes. The admin app has no
 * dependency on `material3-adaptive`, so this is computed from the root constraints instead.
 */
enum class WidthClass {
    /** Phones (< 600dp): single pane, full-screen editor, FAB. */
    Compact,

    /** Tablets / narrow browser windows (600–1039dp): single pane, centered and width-capped. */
    Medium,

    /** Desktop (>= 1040dp): list-detail with the editor beside the list. */
    Expanded;

    val isCompact: Boolean
        get() = this == Compact

    companion object {
        fun from(width: Dp): WidthClass =
            when {
                width < 600.dp -> Compact
                width < 1040.dp -> Medium
                else -> Expanded
            }
    }
}

val LocalWidthClass = staticCompositionLocalOf { WidthClass.Compact }

/** Measures the available width once at the root and exposes it as [LocalWidthClass]. */
@Composable
fun ProvideWidthClass(content: @Composable () -> Unit) {
    BoxWithConstraints {
        CompositionLocalProvider(
            LocalWidthClass provides WidthClass.from(maxWidth),
            content = content,
        )
    }
}

/** Centers [content] horizontally and caps its width so lines stay readable on wide screens. */
@Composable
fun CenteredContent(
    maxWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxWidth()) { content() }
    }
}

/** Cmd (macOS / web on Mac) or Ctrl (everywhere else) held together with [key]. */
fun KeyEvent.isShortcut(key: Key): Boolean = (isCtrlPressed || isMetaPressed) && this.key == key

/**
 * Frames the sign-in / access forms: bare on phones, an outlined card on larger windows so the form
 * doesn't float in an empty page.
 */
@Composable
fun AuthCard(content: @Composable () -> Unit) {
    val modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
    if (LocalWidthClass.current.isCompact) {
        Box(modifier) { content() }
    } else {
        OutlinedCard(modifier) { Box(Modifier.padding(32.dp)) { content() } }
    }
}

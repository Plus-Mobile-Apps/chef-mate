package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

@Composable
fun PlusResponsiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSizeClass) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val measuredSizeClass =
            when {
                maxWidth < 600.dp -> WindowSizeClass.COMPACT
                maxWidth < 840.dp -> WindowSizeClass.MEDIUM
                else -> WindowSizeClass.EXPANDED
            }
        // BoxWithConstraints recomposes this block inside the layout pass. If a breakpoint change
        // swapped content there, any Popup in the outgoing branch (coach marks, menus) would be
        // disposed mid-layout, which crashes Compose Desktop ("RootNodeOwner is already disposed")
        // while it is still measuring that popup's layer. So the first composition uses the
        // measured class directly, and later changes are committed via a state write that lands
        // in the next frame's regular composition pass instead.
        var sizeClass by remember { mutableStateOf(measuredSizeClass) }
        SideEffect { sizeClass = measuredSizeClass }
        content(sizeClass)
    }
}

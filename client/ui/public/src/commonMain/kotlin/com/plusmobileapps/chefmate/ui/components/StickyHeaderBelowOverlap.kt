package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex

/**
 * Pins a `stickyHeader` [overlap] below the top of its list's viewport instead of at the very top.
 *
 * A LazyColumn sticks its headers to the viewport edge and ignores the top content padding, so in a
 * list drawn behind a [PlusHeaderContainer]'s frosted header (see `drawContentBehindHeader` and
 * [LocalPlusHeaderOverlap]) a stuck header would slide underneath the app bar. Apply this to the
 * sticky header's root composable: it shifts the header down by however much the overlap would
 * cover it, and still lets the next sticky header (any visible item whose key is in [stickyKeys])
 * push it away at the shifted line. The shift is a draw-time translation, so it doesn't relayout
 * the list; the header is raised above the other items so the rows it's shifted over draw beneath
 * it.
 *
 * @param key the key this sticky header was given in the list.
 */
fun Modifier.stickyHeaderBelowOverlap(
    listState: LazyListState,
    key: Any,
    stickyKeys: Set<Any>,
    overlap: Dp,
): Modifier =
    zIndex(1f).graphicsLayer {
        val layoutInfo = listState.layoutInfo
        val items = layoutInfo.visibleItemsInfo
        val item = items.firstOrNull { it.key == key } ?: return@graphicsLayer
        val stickLine = layoutInfo.viewportStartOffset + overlap.toPx()
        val offset = item.offset.toFloat()
        // At or above the viewport edge the list has stuck (or is pushing) this header; below it,
        // the
        // header is in its natural spot and only needs moving if the overlap covers that spot.
        val stuck =
            if (item.offset > layoutInfo.viewportStartOffset) maxOf(stickLine, offset)
            else stickLine
        val next =
            items.filter { it.index > item.index && it.key in stickyKeys }.minByOrNull { it.index }
        val target = if (next != null) minOf(stuck, (next.offset - item.size).toFloat()) else stuck
        translationY = target - offset
    }

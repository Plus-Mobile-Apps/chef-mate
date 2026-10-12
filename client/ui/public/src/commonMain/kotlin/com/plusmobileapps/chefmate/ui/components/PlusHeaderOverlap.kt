package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * How far a [PlusHeaderContainer]'s frosted header overlaps the top of its content — the header's
 * current height, which shrinks as a collapsing title collapses. Content drawn behind the header
 * (see `drawContentBehindHeader`) reserves this at its top, typically with [contentPadding].
 *
 * The height is measured in the container's layout pass, so read [height] only from a layout or
 * draw phase (a `graphicsLayer` block, a measure policy, a [PaddingValues] a layout reads). Reading
 * it during composition would recompose the content every frame of a collapse — and lag a frame
 * behind the header.
 */
@Stable
class PlusHeaderOverlap internal constructor(private val heightState: State<Dp>) {
    /** The header's current height. See the class docs for where to read it. */
    val height: Dp
        get() = heightState.value

    /**
     * Padding with the header's height added on top of [top]. The header height is read whenever a
     * layout reads the padding, so it tracks a collapsing header without recomposing.
     */
    fun contentPadding(
        start: Dp = 0.dp,
        top: Dp = 0.dp,
        end: Dp = 0.dp,
        bottom: Dp = 0.dp,
    ): PaddingValues = OverlapPaddingValues(this, start, top, end, bottom)

    internal companion object {
        val None = PlusHeaderOverlap(mutableStateOf(0.dp))
    }
}

/** The enclosing [PlusHeaderContainer]'s [PlusHeaderOverlap]; zero outside such a container. */
val LocalPlusHeaderOverlap: ProvidableCompositionLocal<PlusHeaderOverlap> = compositionLocalOf {
    PlusHeaderOverlap.None
}

private data class OverlapPaddingValues(
    private val overlap: PlusHeaderOverlap,
    private val start: Dp,
    private val top: Dp,
    private val end: Dp,
    private val bottom: Dp,
) : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
        if (layoutDirection == LayoutDirection.Ltr) start else end

    override fun calculateTopPadding(): Dp = overlap.height + top

    override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
        if (layoutDirection == LayoutDirection.Ltr) end else start

    override fun calculateBottomPadding(): Dp = bottom
}

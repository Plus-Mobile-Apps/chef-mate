package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import chefmate.client.ui.public.generated.resources.Res
import chefmate.client.ui.public.generated.resources.coach_mark_dismiss
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

/** Which side of the anchor the tooltip bubble sits on. The caret always points at the anchor. */
enum class PlusTooltipPlacement {
    ABOVE,
    BELOW,
}

/**
 * An onboarding "coach mark": a small pop-up bubble that floats next to the composable it wraps and
 * points at it with a caret. Use it to draw a first-time user's attention to a specific control.
 *
 * The bubble is rendered in a [Popup] so it escapes the bounds of the anchor's parent (e.g. a
 * floating toolbar) and can overlay other content. The popup is non-focusable, so taps still reach
 * the underlying control — a user can tap the highlighted control directly.
 *
 * Visibility is fully caller-controlled. Pass [visible] = true to show it and handle [onDismiss] to
 * hide it. [onDismiss] fires only when the user taps the bubble's dismiss button — never when the
 * popup goes away for other reasons (outside interaction, or the anchor leaving composition as the
 * user navigates away). Callers that want it shown only once should persist that decision in
 * [onDismiss] — e.g. via the shared coach-mark controller — so that merely leaving the screen
 * mid-tooltip lets it reappear on the next visit rather than dismissing it forever.
 *
 * @param text the message shown in the bubble.
 * @param visible whether the bubble is currently shown.
 * @param onDismiss invoked only when the user taps the bubble's dismiss button.
 * @param placement which side of the anchor the bubble sits on. Defaults to [ABOVE].
 * @param anchor the control the tooltip points at; always laid out, tooltip or not.
 */
@Composable
fun PlusOnboardingTooltip(
    text: TextData,
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    placement: PlusTooltipPlacement = PlusTooltipPlacement.ABOVE,
    anchor: @Composable () -> Unit,
) {
    // Under a screenshot test's preview host, register during composition and report the anchor's
    // coordinates as it's placed, so the host draws this tooltip inline in the same frame — Popup
    // windows don't render there.
    val previewHost = LocalTooltipPreviewHost.current
    val previewEntry =
        if (previewHost != null && visible) {
            rememberPreviewEntry(previewHost, text, placement)
        } else {
            null
        }

    Box(
        modifier =
            modifier.then(
                if (previewEntry != null) Modifier.onPlaced { previewEntry.anchor = it }
                else Modifier
            )
    ) {
        anchor()

        if (visible && previewHost == null) {
            val density = LocalDensity.current
            val gapPx = with(density) { ChefMateTheme.dimens.paddingSmall.roundToPx() }
            val edgeMarginPx = with(density) { ChefMateTheme.dimens.paddingNormal.roundToPx() }

            // -1 = "not positioned yet" → the caret renders centered. The position provider
            // updates this to the anchor's center relative to the bubble's left edge once the
            // popup is laid out, keeping the caret pointing at the anchor even when the bubble is
            // clamped to a screen edge.
            var caretOffsetPx by remember { mutableIntStateOf(-1) }
            val positionProvider =
                remember(placement, gapPx, edgeMarginPx) {
                    TooltipPositionProvider(
                        placement = placement,
                        anchorGapPx = gapPx,
                        edgeMarginPx = edgeMarginPx,
                        onCaretOffset = { caretOffsetPx = it },
                    )
                }

            Popup(
                popupPositionProvider = positionProvider,
                // Deliberately a no-op: only the bubble's dismiss button should mark the coach mark
                // as seen. The popup auto-dismissing for any other reason (outside interaction, the
                // anchor leaving composition when navigating away) must NOT consume it — otherwise
                // leaving the screen mid-tooltip would hide it forever. Visibility is caller-driven
                // via [visible], so the tip simply reappears the next time the screen requests it.
                onDismissRequest = {},
                properties = PopupProperties(focusable = false, clippingEnabled = false),
            ) {
                PlusTooltipBubble(
                    text = text,
                    placement = placement,
                    caretOffsetPx = caretOffsetPx,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

private val CaretWidth: Dp = 16.dp
private val CaretHeight: Dp = 8.dp
private val MaxBubbleWidth: Dp = 260.dp

/**
 * The bare tooltip bubble visual — a rounded message surface with a caret on one edge. Exposed
 * separately from [PlusOnboardingTooltip] so it can be rendered directly (e.g. in snapshot tests)
 * without the surrounding [Popup].
 *
 * @param caretOffsetPx horizontal position of the caret tip, in pixels from the bubble's left edge.
 *   A negative value (the default) centers the caret.
 */
@Composable
fun PlusTooltipBubble(
    text: TextData,
    placement: PlusTooltipPlacement,
    modifier: Modifier = Modifier,
    caretOffsetPx: Int = -1,
    onDismiss: () -> Unit = {},
) {
    PlusTooltipBubble(text, placement, { caretOffsetPx }, modifier, onDismiss)
}

/** [PlusTooltipBubble] with the caret position read at draw time, for [PlusTooltipPreviewHost]. */
@Composable
private fun PlusTooltipBubble(
    text: TextData,
    placement: PlusTooltipPlacement,
    caretOffsetPx: () -> Int,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
) {
    val color = MaterialTheme.colorScheme.primaryContainer
    val caret: @Composable () -> Unit = {
        Caret(
            color = color,
            pointingUp = placement == PlusTooltipPlacement.BELOW,
            caretOffsetPx = caretOffsetPx,
        )
    }

    // IntrinsicSize.Max pins the column to the text surface's width so the full-width caret canvas
    // doesn't stretch the bubble to the popup window's width.
    Column(modifier = modifier.width(IntrinsicSize.Max)) {
        if (placement == PlusTooltipPlacement.BELOW) caret()
        Surface(
            color = color,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = ChefMateTheme.shapes.medium,
            shadowElevation = ChefMateTheme.dimens.paddingExtraSmall,
        ) {
            Column(
                modifier =
                    Modifier.widthIn(max = MaxBubbleWidth)
                        .padding(
                            horizontal = ChefMateTheme.dimens.paddingNormal,
                            vertical = ChefMateTheme.dimens.paddingSmall,
                        ),
                verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingExtraSmall),
            ) {
                Text(
                    text = text.localized(),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(Res.string.coach_mark_dismiss),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier =
                        Modifier.align(Alignment.End)
                            .clickable(onClick = onDismiss)
                            .padding(ChefMateTheme.dimens.paddingExtraSmall),
                )
            }
        }
        if (placement == PlusTooltipPlacement.ABOVE) caret()
    }
}

@Composable
private fun Caret(
    color: Color,
    pointingUp: Boolean,
    caretOffsetPx: () -> Int,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxWidth().height(CaretHeight)) {
        val halfWidth = CaretWidth.toPx() / 2f
        // Centre the caret when the popup hasn't been positioned yet (or in preview), otherwise
        // align it with the anchor while keeping it fully inside the bubble.
        val offsetPx = caretOffsetPx()
        val centerX =
            if (offsetPx < 0) size.width / 2f
            else offsetPx.toFloat().coerceIn(halfWidth, size.width - halfWidth)
        val path =
            Path().apply {
                if (pointingUp) {
                    moveTo(centerX, 0f)
                    lineTo(centerX - halfWidth, size.height)
                    lineTo(centerX + halfWidth, size.height)
                } else {
                    moveTo(centerX, size.height)
                    lineTo(centerX - halfWidth, 0f)
                    lineTo(centerX + halfWidth, 0f)
                }
                close()
            }
        drawPath(path = path, color = color)
    }
}

/**
 * Collects the coach marks shown under a [PlusTooltipPreviewHost] so it can draw them inline.
 * Screenshot tests only: the app never provides one, so tooltips there always use a [Popup].
 *
 * The screenshot renderer captures a single frame, so nothing here goes through snapshot state:
 * tooltips register while composing and report their anchor while being placed, and the host reads
 * both later in that same frame.
 */
class TooltipPreviewHost internal constructor() {
    internal class Entry(var text: TextData, var placement: PlusTooltipPlacement) {
        var anchor: LayoutCoordinates? = null
        var caretOffsetPx: Int = -1
    }

    internal val entries = mutableListOf<Entry>()
}

/** The active [TooltipPreviewHost], or null (the default, and always in the app). */
val LocalTooltipPreviewHost = staticCompositionLocalOf<TooltipPreviewHost?> { null }

@Composable
private fun rememberPreviewEntry(
    host: TooltipPreviewHost,
    text: TextData,
    placement: PlusTooltipPlacement,
): TooltipPreviewHost.Entry {
    val entry = remember(host) { TooltipPreviewHost.Entry(text, placement) }
    entry.text = text
    entry.placement = placement
    if (entry !in host.entries) host.entries += entry
    DisposableEffect(host, entry) { onDispose { host.entries -= entry } }
    return entry
}

/**
 * Draws every visible [PlusOnboardingTooltip] in [content] inline, positioned exactly as the popup
 * would be. For screenshot tests, where [Popup] windows aren't captured; the app never uses it.
 *
 * A [SubcomposeLayout] so the content is measured — which composes any lazily composed parts, and
 * the tooltips inside them — before the bubbles are composed, and placed before they're positioned.
 */
@Composable
fun PlusTooltipPreviewHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val host = remember { TooltipPreviewHost() }
    val gapPx = with(LocalDensity.current) { ChefMateTheme.dimens.paddingSmall.roundToPx() }
    val edgeMarginPx = with(LocalDensity.current) { ChefMateTheme.dimens.paddingNormal.roundToPx() }
    SubcomposeLayout(modifier) { constraints ->
        val contentPlaceables =
            subcompose("content") {
                    CompositionLocalProvider(
                        LocalTooltipPreviewHost provides host,
                        content = content,
                    )
                }
                .map { it.measure(constraints) }
        val width = contentPlaceables.maxOfOrNull { it.width } ?: constraints.minWidth
        val height = contentPlaceables.maxOfOrNull { it.height } ?: constraints.minHeight
        val entries = host.entries.toList()
        val bubbles =
            subcompose("tooltips") {
                    entries.forEach { entry ->
                        PlusTooltipBubble(
                            text = entry.text,
                            placement = entry.placement,
                            caretOffsetPx = { entry.caretOffsetPx },
                        )
                    }
                }
                .map { it.measure(Constraints()) }
        layout(width, height) {
            contentPlaceables.forEach { it.place(0, 0) }
            val hostCoordinates = coordinates ?: return@layout
            entries.zip(bubbles).forEach { (entry, bubble) ->
                val anchor = entry.anchor?.takeIf { it.isAttached } ?: return@forEach
                val topLeft = hostCoordinates.localPositionOf(anchor, Offset.Zero).round()
                val position =
                    TooltipPositionProvider(
                            placement = entry.placement,
                            anchorGapPx = gapPx,
                            edgeMarginPx = edgeMarginPx,
                            onCaretOffset = { entry.caretOffsetPx = it },
                        )
                        .calculatePosition(
                            anchorBounds = IntRect(topLeft, anchor.size),
                            windowSize = IntSize(width, height),
                            layoutDirection = layoutDirection,
                            popupContentSize = IntSize(bubble.width, bubble.height),
                        )
                bubble.place(position)
            }
        }
    }
}

private class TooltipPositionProvider(
    private val placement: PlusTooltipPlacement,
    private val anchorGapPx: Int,
    private val edgeMarginPx: Int,
    private val onCaretOffset: (Int) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val anchorCenterX = anchorBounds.left + anchorBounds.width / 2
        val maxX =
            (windowSize.width - popupContentSize.width - edgeMarginPx).coerceAtLeast(edgeMarginPx)
        val x = (anchorCenterX - popupContentSize.width / 2).coerceIn(edgeMarginPx, maxX)
        val y =
            when (placement) {
                PlusTooltipPlacement.ABOVE ->
                    anchorBounds.top - popupContentSize.height - anchorGapPx
                PlusTooltipPlacement.BELOW -> anchorBounds.bottom + anchorGapPx
            }
        onCaretOffset(anchorCenterX - x)
        return IntOffset(x, y)
    }
}

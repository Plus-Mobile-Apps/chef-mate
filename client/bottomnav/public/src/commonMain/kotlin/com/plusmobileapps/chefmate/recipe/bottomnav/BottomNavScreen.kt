@file:OptIn(ExperimentalDecomposeApi::class)

package com.plusmobileapps.chefmate.recipe.bottomnav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import chefmate.client.bottomnav.public.generated.resources.Res
import chefmate.client.bottomnav.public.generated.resources.tab_browser
import chefmate.client.bottomnav.public.generated.resources.tab_grocery
import chefmate.client.bottomnav.public.generated.resources.tab_meals
import chefmate.client.bottomnav.public.generated.resources.tab_more
import chefmate.client.bottomnav.public.generated.resources.tab_recipes
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Tab.BROWSER
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Tab.GROCERIES
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Tab.MEALS
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Tab.RECIPES
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBloc.Tab.SETTINGS
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.components.LocalBottomNavBarInset
import com.plusmobileapps.chefmate.ui.components.NavRailItem
import com.plusmobileapps.chefmate.ui.components.PlusNavRailHeaderContainer
import com.plusmobileapps.chefmate.ui.components.glass.AppBackdrop
import com.plusmobileapps.chefmate.ui.components.glass.GlassDefaults
import com.plusmobileapps.chefmate.ui.components.glass.GlassSurface
import com.plusmobileapps.chefmate.ui.components.glass.LocalAppBackdrop
import com.plusmobileapps.chefmate.ui.components.glass.appBackdropSource
import com.plusmobileapps.chefmate.ui.components.glass.rememberAppBackdrop
import com.plusmobileapps.chefmate.ui.components.reportBottomNavInset
import com.plusmobileapps.chefmate.ui.fadeScalePredictiveBackAnimatable
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private enum class NavigationLayout {
    BOTTOM,
    SIDE_COMPACT,
    SIDE_EXPANDED,
}

@Composable
fun BottomNavigationScreen(bloc: BottomNavBloc, modifier: Modifier = Modifier) {
    // One backdrop for both shells. The floating bar and rail blur the tab content behind
    // themselves, and hoisting the handle above BoxWithConstraints keeps the content's modifier
    // chain identical on both sides of the 600dp breakpoint — so crossing it stays a pure
    // movableContent move for Haze too, with no layer torn down and rebuilt.
    val backdrop = rememberAppBackdrop()
    // The active tab's Children stack is kept in movable content so that crossing the 600dp
    // breakpoint on rotation/resize *moves* the running composition between the bottom-bar and
    // nav-rail shells rather than disposing one and composing the other fresh. Without this, every
    // tab screen's in-composition state — most visibly the list scroll position — resets on
    // rotation. Android papers over it by restoring rememberSaveable values from the Activity's
    // saved-instance bundle; iOS and desktop have no such bundle, so there the position is simply
    // lost. Moving the content keeps the state alive on every platform.
    val tabContent =
        remember(bloc) {
            movableContentOf<Modifier> { contentModifier ->
                BottomNavContentContainer(modifier = contentModifier, bloc = bloc)
            }
        }
    BoxWithConstraints(modifier = modifier) {
        val layout =
            when {
                maxWidth < 600.dp -> NavigationLayout.BOTTOM
                maxHeight < 480.dp -> NavigationLayout.SIDE_COMPACT
                else -> NavigationLayout.SIDE_EXPANDED
            }
        when (layout) {
            NavigationLayout.BOTTOM ->
                MobileBottomNavContent(bloc = bloc, backdrop = backdrop, content = tabContent)
            NavigationLayout.SIDE_COMPACT ->
                SideNavContent(
                    modifier = Modifier.imePadding(),
                    bloc = bloc,
                    expandedItems = false,
                    content = tabContent,
                )
            NavigationLayout.SIDE_EXPANDED ->
                SideNavContent(
                    modifier = Modifier.imePadding(),
                    bloc = bloc,
                    expandedItems = true,
                    content = tabContent,
                )
        }
    }
}

@Composable
private fun SideNavContent(
    bloc: BottomNavBloc,
    expandedItems: Boolean,
    content: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = bloc.state.collectAsState()

    val navRailItems =
        remember(state.value) {
            val currentState = state.value
            currentState.tabs.map {
                NavRailItem(
                    label = it.getLabel().asTextData(),
                    selected = it == currentState.selectedTab,
                    icon = {
                        TabIcon(tab = it, notificationCount = currentState.notificationCount)
                    },
                    onClick = { bloc.onTabSelected(it) },
                )
            }
        }

    PlusNavRailHeaderContainer(
        modifier = modifier.fillMaxSize(),
        navRail = navRailItems,
        expandedItems = expandedItems,
        // The compact rail (a phone turned sideways) is the bottom bar on its side, lens and all.
        // The expanded rail's wide label rows keep the standard items.
        railContent =
            if (expandedItems) {
                null
            } else {
                { LensNavRail(state = state.value, onClick = bloc::onTabSelected) }
            },
        content = { content(Modifier.padding(it)) },
    )
}

@Composable
private fun MobileBottomNavContent(
    bloc: BottomNavBloc,
    backdrop: AppBackdrop,
    content: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = bloc.state.collectAsState()
    val density = LocalDensity.current
    // The pill is only ~16dp off the bottom edge, so an open keyboard covers it outright. Slide it
    // away instead of trying to float it above a rising keyboard: the tab content then owns the
    // whole area above the keyboard, which is what a bottom-anchored input (the grocery add row)
    // wants anyway.
    val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val imeVisible = imeBottom > 0.dp
    var barBlockHeight by remember { mutableStateOf(0.dp) }
    // The content is already imePadding()-ed, so the bar only needs to contribute whatever part of
    // its block the keyboard doesn't yet cover. Deriving this from the live IME inset (rather than
    // springing it to 0 when the keyboard appears) keeps a bottom-anchored input at
    // max(keyboard, bar) on every frame: it waits on top of the bar until the rising keyboard
    // overtakes it, then rides the keyboard — and on close it settles back onto the bar instead of
    // dropping to the screen edge and bouncing up once the keyboard is gone.
    val barInset = (barBlockHeight - imeBottom).coerceAtLeast(0.dp)

    Box(modifier = modifier.fillMaxSize()) {
        // No Scaffold here: the tab content fills the screen so it can be seen through the glass,
        // and the bar floats over it. That means this Box has to supply the insets the Scaffold
        // used to — no tab root applies a top inset of its own, so without this every screen would
        // run under the status bar. windowInsetsPadding consumes what it applies, so nested
        // consumers don't double-pad. The bottom is deliberately left unpadded; screens clear the
        // bar through LocalBottomNavBarInset instead.
        CompositionLocalProvider(
            LocalAppBackdrop provides backdrop,
            LocalBottomNavBarInset provides barInset,
        ) {
            content(
                Modifier.fillMaxSize()
                    .appBackdropSource(backdrop)
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(
                            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                        )
                    )
                    .imePadding()
            )
        }
        AnimatedVisibility(
            visible = !imeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            PlusBottomBar(
                state = state.value,
                backdrop = backdrop,
                onClick = bloc::onTabSelected,
                onBlockHeightChanged = { barBlockHeight = it },
            )
        }
    }
}

@Composable
private fun BottomNavContentContainer(bloc: BottomNavBloc, modifier: Modifier = Modifier) {
    Children(
        modifier = modifier.fillMaxSize(),
        stack = bloc.content,
        animation =
            predictiveBackAnimation(
                backHandler = bloc.backHandler,
                fallbackAnimation = stackAnimation(fade() + scale()),
                onBack = bloc::onBackClicked,
                selector = { backEvent, _, _ ->
                    fadeScalePredictiveBackAnimatable(initialEvent = backEvent)
                },
            ),
    ) { created ->
        created.instance.bloc.Content()
    }
}

/**
 * The floating navigation pill: a translucent, blurred capsule inset from the screen edges with the
 * tab content scrolling behind it.
 *
 * [backdrop] is the content it blurs; `null` renders the flat fallback tint (screenshot tests and
 * previews, where the platform has no blur pipeline). [onBlockHeightChanged] reports the height of
 * the bar *plus* its bottom margin, which is what screens need to clear via
 * [LocalBottomNavBarInset].
 */
@Composable
private fun PlusBottomBar(
    state: BottomNavBloc.Model,
    backdrop: AppBackdrop?,
    onClick: (BottomNavBloc.Tab) -> Unit,
    modifier: Modifier = Modifier,
    onBlockHeightChanged: (Dp) -> Unit = {},
) {
    val density = LocalDensity.current
    val navigationBarInset = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    // Clamp rather than add: stacking a 12dp gap on top of a 34pt iPhone home indicator or a 48dp
    // Android three-button bar leaves the pill stranded halfway up the screen. Sitting directly on
    // top of the system affordance is what reads as "floating".
    val bottomMargin = maxOf(navigationBarInset, GlassDefaults.minEdgeGap)

    GlassSurface(
        backdrop = backdrop,
        modifier =
            modifier
                // Both of these must come BEFORE the padding: onSizeChanged reports the node as
                // measured at its position in the chain, and what consumers need is the full block
                // — bar plus margin, i.e. the distance from the bottom of the screen. ToastScaffold
                // in particular takes maxOf(bottomNavInset, navigationBars) on the assumption that
                // the reported height spans the system inset.
                .reportBottomNavInset()
                .onSizeChanged { onBlockHeightChanged(with(density) { it.height.toDp() }) }
                .padding(
                    start = ChefMateTheme.dimens.paddingNormal,
                    end = ChefMateTheme.dimens.paddingNormal,
                    bottom = bottomMargin,
                ),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // ShortNavigationBar lays its items out with equal weight, so a cell is just an even
            // slice of the bar and the lens can be positioned arithmetically.
            LensTrack(
                state = state,
                onClick = onClick,
                orientation = Orientation.Horizontal,
                cellSize = maxWidth / state.tabs.size.coerceAtLeast(1),
                modifier = Modifier.fillMaxWidth(),
            ) { lensTint ->
                ShortNavigationBar(
                    containerColor = Color.Transparent,
                    contentColor = ChefMateTheme.colorScheme.onSurface,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                ) {
                    state.tabs.forEach { tab ->
                        ShortNavigationBarItem(
                            selected = tab == state.selectedTab,
                            onClick = { onClick(tab) },
                            icon = {
                                TabIcon(
                                    tab = tab,
                                    notificationCount = state.notificationCount,
                                    lensTint = lensTint,
                                )
                            },
                            label = { TabLabel(tab = tab, lensTint = lensTint) },
                            colors =
                                ShortNavigationBarItemDefaults.colors(
                                    // Selection is carried by the lens and its blue tint, so
                                    // selected and unselected items share one base color.
                                    // (Material's default selected label is `secondary` — teal200
                                    // here, near-invisible on the light pill.)
                                    selectedIconColor = ChefMateTheme.colorScheme.onSurface,
                                    selectedTextColor = ChefMateTheme.colorScheme.onSurface,
                                    // The lens is the indicator now, and it is one object that
                                    // travels rather than a per-item pill that cross-fades.
                                    selectedIndicatorColor = Color.Transparent,
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The compact side rail for a short, wide window (a phone in landscape): the bottom bar turned on
 * its side, with the same glass lens sliding — and dragging — vertically between equal-height
 * cells, and the same blue tint on whatever sits beneath it. Rendered inside the rail's glass pill
 * by [PlusNavRailHeaderContainer].
 */
@Composable
private fun LensNavRail(
    state: BottomNavBloc.Model,
    onClick: (BottomNavBloc.Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        // Equal cells so the lens can be positioned arithmetically, as on the bottom bar. Capped so
        // a taller window doesn't stretch them, and shrunk to fit a short one.
        val cellHeight = minOf(RailCellMaxHeight, maxHeight / state.tabs.size.coerceAtLeast(1))
        LensTrack(
            state = state,
            onClick = onClick,
            orientation = Orientation.Vertical,
            cellSize = cellHeight,
            modifier = Modifier.width(RailWidth),
        ) { lensTint ->
            CompositionLocalProvider(
                LocalContentColor provides ChefMateTheme.colorScheme.onSurface
            ) {
                Column {
                    state.tabs.forEach { tab ->
                        Column(
                            modifier =
                                Modifier.width(RailWidth)
                                    .height(cellHeight)
                                    .selectable(
                                        selected = tab == state.selectedTab,
                                        interactionSource = null,
                                        // The lens is the press feedback, as on the bottom bar.
                                        indication = null,
                                        role = Role.Tab,
                                        onClick = { onClick(tab) },
                                    ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            TabIcon(
                                tab = tab,
                                notificationCount = state.notificationCount,
                                lensTint = lensTint,
                            )
                            TabLabel(
                                tab = tab,
                                lensTint = lensTint,
                                style = ChefMateTheme.typography.labelMedium,
                                modifier =
                                    Modifier.padding(top = ChefMateTheme.dimens.paddingExtraSmall),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val RailWidth = 80.dp
private val RailCellMaxHeight = 64.dp

/**
 * The selection lens and the track of tab cells it slides along — shared by the bottom bar
 * ([Orientation.Horizontal]) and the compact side rail ([Orientation.Vertical]). [items] lays out
 * the cells, each exactly [cellSize] along [orientation] starting at the track's origin, and paints
 * their icons and labels through the [LensTint] it is handed.
 *
 * The lens springs onto the selected cell, and can be dragged: the selection commits as the lens
 * crosses into a cell, so the screen behind it changes under your finger the way the iOS tab bar
 * does. Item ripples are switched off — the lens is the press feedback, and a ripple blooming under
 * a piece of glass reads as a smudge rather than a response.
 */
@Composable
private fun LensTrack(
    state: BottomNavBloc.Model,
    onClick: (BottomNavBloc.Tab) -> Unit,
    orientation: Orientation,
    cellSize: Dp,
    modifier: Modifier = Modifier,
    items: @Composable (LensTint) -> Unit,
) {
    val density = LocalDensity.current
    val tabCount = state.tabs.size.coerceAtLeast(1)
    val selectedIndex = state.tabs.indexOf(state.selectedTab).coerceAtLeast(0)
    val horizontal = orientation == Orientation.Horizontal
    val cellPx = with(density) { cellSize.toPx() }

    // Two sources for the lens position, and only ever one of them is live: `settle` animates it
    // onto a cell, `dragOffset` tracks a finger. The drag must not go through the Animatable —
    // every delta would have to launch a coroutine, and Animatable's mutator mutex cancels
    // whichever of those is still in flight, so deltas get dropped and the lens lands on the wrong
    // tab.
    val settle = remember { Animatable(selectedIndex * cellPx) }
    var dragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(settle.value) }
    // Read through a lambda so the offset and the tint below pick it up in the layout/draw phases
    // — a moving lens then never recomposes the track.
    val lensPosition = { if (dragging) dragOffset else settle.value }
    val maxOffset = (tabCount - 1) * cellPx

    // Settle onto the selected cell whenever the selection or the geometry changes — a tap, a deep
    // link, a tab reorder, a rotation — but never while a finger is on the lens.
    LaunchedEffect(selectedIndex, cellPx, dragging) {
        if (!dragging) {
            settle.animateTo(
                targetValue = selectedIndex * cellPx,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
            )
        }
    }

    val dragState = rememberDraggableState { delta ->
        dragOffset = (dragOffset + delta).coerceIn(0f, maxOffset)
        val nearest = (dragOffset / cellPx).roundToInt().coerceIn(0, tabCount - 1)
        state.tabs.getOrNull(nearest)?.let { if (it != state.selectedTab) onClick(it) }
    }

    val insetPx = with(density) { LensInset.toPx() }
    val focusTint =
        if (ChefMateTheme.colorScheme.surface.luminance() < 0.5f) {
            LensFocusTintDark
        } else {
            LensFocusTintLight
        }
    val lensTint =
        remember(focusTint, cellPx, insetPx, horizontal) {
            LensTint(
                color = focusTint,
                lensRect = { trackSize ->
                    val start = lensPosition()
                    if (horizontal) {
                        Rect(
                            left = start + insetPx,
                            top = insetPx,
                            right = start + cellPx - insetPx,
                            bottom = trackSize.height - insetPx,
                        )
                    } else {
                        Rect(
                            left = insetPx,
                            top = start + insetPx,
                            right = trackSize.width - insetPx,
                            bottom = start + cellPx - insetPx,
                        )
                    }
                },
            )
        }

    Box(
        modifier =
            modifier
                // The lens is laid out relative to this Box, so the tint measures from it.
                .onPlaced { lensTint.barCoordinates = it }
                .draggable(
                    state = dragState,
                    orientation = orientation,
                    onDragStarted = {
                        dragOffset = settle.value
                        dragging = true
                    },
                    onDragStopped = {
                        // Hand the position back before switching source, so the lens doesn't jump
                        // on release; the effect above springs it home.
                        settle.snapTo(dragOffset)
                        dragging = false
                    },
                )
    ) {
        // Declared first so it draws beneath the icons and labels, and sized with matchParentSize
        // so the items still decide the track's size.
        Box(modifier = Modifier.matchParentSize()) {
            SelectionLens(
                modifier =
                    Modifier.offset {
                            val offset = lensPosition().roundToInt()
                            if (horizontal) IntOffset(offset, 0) else IntOffset(0, offset)
                        }
                        .then(
                            if (horizontal) {
                                Modifier.width(cellSize).fillMaxHeight()
                            } else {
                                Modifier.height(cellSize).fillMaxWidth()
                            }
                        )
                        .padding(LensInset)
            )
        }
        CompositionLocalProvider(LocalRippleConfiguration provides null) { items(lensTint) }
    }
}

// The lens frames the whole item — icon and label — so it runs nearly the full cell. Material lays
// the bar's label out low in the item, so anything shorter cuts across it.
private val LensInset = 3.dp

// The blue the lens lends the icon and label beneath it. A system-style blue rather than the
// theme's purple primary, so it reads as a property of the glass, not of the brand.
private val LensFocusTintLight = Color(0xFF007AFF)
private val LensFocusTintDark = Color(0xFF0A84FF)

// Not a full stadium: at this height a semicircular end would curve in across a wide label like
// "Browser". This radius still sits inside the bar's own rounded ends at the first and last tab.
private val LensShape = RoundedCornerShape(24.dp)

/**
 * Where the selection lens currently sits, for [LensTinted]. [lensRect] maps the bar's size to the
 * lens's bounds in the bar's coordinates, and is read at draw time so a sliding lens only redraws.
 */
@Stable
private class LensTint(val color: Color, val lensRect: (IntSize) -> Rect) {
    var barCoordinates: LayoutCoordinates? = null
}

/**
 * Paints the part of [content] that sits under the selection lens in the lens's focus color, as if
 * the glass were tinting what it magnifies — and only the overlapping slice, so an icon half under
 * a moving lens is half blue.
 *
 * [content] is composed twice and stacked: once in its own color clipped to outside the lens, once
 * in [LensTint.color] clipped to inside it. Splitting with pixel-snapped clips, rather than
 * recoloring through a blend mode in an offscreen layer, keeps every pixel identical to an ordinary
 * draw — blend arithmetic rounds differently across renderers, enough to flake screenshot goldens
 * between macOS and the Linux CI runner. The tinted copy is hidden from accessibility so the label
 * isn't announced twice.
 *
 * Applied per icon and label rather than once over the whole bar so the More tab's badge keeps its
 * own colors.
 */
@Composable
private fun LensTinted(
    tint: LensTint,
    content: @Composable (tintColor: Color?, modifier: Modifier) -> Unit,
) {
    Box {
        content(null, Modifier.lensClip(tint, inside = false))
        content(tint.color, Modifier.lensClip(tint, inside = true).clearAndSetSemantics {})
    }
}

private fun Modifier.lensClip(tint: LensTint, inside: Boolean): Modifier =
    this then LensClipElement(tint, inside)

private data class LensClipElement(val tint: LensTint, val inside: Boolean) :
    ModifierNodeElement<LensClipNode>() {
    override fun create() = LensClipNode(tint, inside)

    override fun update(node: LensClipNode) {
        node.tint = tint
        node.inside = inside
        node.invalidateDraw()
    }
}

private class LensClipNode(var tint: LensTint, var inside: Boolean) :
    Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        val bar = tint.barCoordinates
        val lens =
            if (bar != null && bar.isAttached) {
                tint.lensRect(bar.size).translate(requireLayoutCoordinates().localPositionOf(bar))
            } else {
                null
            }
        if (lens == null || !lens.overlaps(Rect(Offset.Zero, size))) {
            // Nothing under the lens: the plain copy draws whole, the tinted copy not at all.
            if (!inside) drawContent()
            return
        }
        // Whole pixels, so the clip edge is hard and identical on every renderer.
        clipRect(
            left = lens.left.roundToInt().toFloat(),
            top = lens.top.roundToInt().toFloat(),
            right = lens.right.roundToInt().toFloat(),
            bottom = lens.bottom.roundToInt().toFloat(),
            clipOp = if (inside) ClipOp.Intersect else ClipOp.Difference,
        ) {
            this@draw.drawContent()
        }
    }
}

/**
 * The draggable selection lens: a brighter piece of glass sitting on the bar, marking the active
 * tab. It replaces Material's per-item indicator so that selection reads as one object moving along
 * the bar — and so it can be dragged between tabs.
 */
@Composable
private fun SelectionLens(modifier: Modifier = Modifier) {
    val colorScheme = ChefMateTheme.colorScheme
    val isDark = colorScheme.surface.luminance() < 0.5f
    Box(
        modifier =
            modifier
                .clip(LensShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colorScheme.secondaryContainer.copy(
                                alpha = if (isDark) 0.55f else 0.80f
                            ),
                            colorScheme.secondaryContainer.copy(
                                alpha = if (isDark) 0.32f else 0.55f
                            ),
                        )
                    )
                )
                // A bright top edge is what sells a piece of glass as raised rather than painted
                // on.
                .border(
                    width = 1.dp,
                    brush =
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = if (isDark) 0.28f else 0.60f),
                                Color.White.copy(alpha = 0.04f),
                            )
                        ),
                    shape = LensShape,
                )
    )
}

/**
 * Renders just the bottom navigation bar for screenshot tests, with [notificationCount] driving the
 * More-tab badge. Not used in production — the app always renders the bar through
 * [BottomNavigationScreen].
 *
 * The backdrop is `null` here: layoutlib has no blur pipeline, so the bar renders its flat fallback
 * tint. Everything else — shape, margins, border, item layout — is identical to production, so the
 * goldens still catch layout regressions.
 */
@Composable
fun BottomNavBarPreview(notificationCount: Int) {
    PlusBottomBar(
        state = BottomNavBloc.Model(notificationCount = notificationCount),
        backdrop = null,
        onClick = {},
    )
}

/**
 * Renders just the compact side rail — the one a phone shows in landscape — for screenshot tests,
 * with [notificationCount] driving the More-tab badge. Not used in production; the app always
 * renders the rail through [BottomNavigationScreen].
 */
@Composable
fun BottomNavRailPreview(notificationCount: Int) {
    GlassSurface(backdrop = null) {
        LensNavRail(
            state = BottomNavBloc.Model(notificationCount = notificationCount),
            onClick = {},
        )
    }
}

/**
 * A tab's icon, wrapped in a count badge on the More ([SETTINGS]) tab when notifications are
 * pending. Shared by the bottom bar and the side nav rail so both surfaces stay in sync.
 */
@Composable
private fun TabIcon(
    tab: BottomNavBloc.Tab,
    notificationCount: Int,
    lensTint: LensTint? = null,
) {
    val icon: @Composable () -> Unit = {
        if (lensTint == null) {
            Icon(imageVector = tab.getIcon(), contentDescription = null)
        } else {
            LensTinted(lensTint) { tintColor, tintModifier ->
                Icon(
                    imageVector = tab.getIcon(),
                    contentDescription = null,
                    tint = tintColor ?: LocalContentColor.current,
                    modifier = tintModifier,
                )
            }
        }
    }
    if (tab == SETTINGS && notificationCount > 0) {
        BadgedBox(badge = { Badge { Text(notificationCount.toString()) } }) { icon() }
    } else {
        icon()
    }
}

/** A tab's label, tinted blue where the selection lens covers it. */
@Composable
private fun TabLabel(
    tab: BottomNavBloc.Tab,
    lensTint: LensTint,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    Box(modifier = modifier) {
        LensTinted(lensTint) { tintColor, tintModifier ->
            Text(
                text = stringResource(tab.getLabel()),
                color = tintColor ?: Color.Unspecified,
                style = style,
                modifier = tintModifier,
            )
        }
    }
}

internal fun BottomNavBloc.Tab.getLabel(): StringResource =
    when (this) {
        RECIPES -> Res.string.tab_recipes
        GROCERIES -> Res.string.tab_grocery
        MEALS -> Res.string.tab_meals
        BROWSER -> Res.string.tab_browser
        SETTINGS -> Res.string.tab_more
    }

internal fun BottomNavBloc.Tab.getIcon() =
    when (this) {
        RECIPES -> Icons.AutoMirrored.Filled.List
        GROCERIES -> Icons.Default.ShoppingCart
        MEALS -> Icons.Default.CalendarMonth
        BROWSER -> Icons.Default.Language
        SETTINGS -> Icons.Default.Menu
    }

@file:OptIn(ExperimentalDecomposeApi::class)

package com.plusmobileapps.chefmate.recipe.bottomnav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    var barBlockHeight by remember { mutableStateOf(0.dp) }
    val barInset by
        animateDpAsState(if (imeVisible) 0.dp else barBlockHeight, label = "bottomNavBarInset")

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
        val tabCount = state.tabs.size.coerceAtLeast(1)
        val selectedIndex = state.tabs.indexOf(state.selectedTab).coerceAtLeast(0)

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // ShortNavigationBar lays its items out with equal weight, so a cell is just an even
            // slice of the bar and the lens can be positioned arithmetically.
            val cellWidth = maxWidth / tabCount
            val cellWidthPx = with(density) { cellWidth.toPx() }
            // Two sources for the lens position, and only ever one of them is live: `settle`
            // animates it onto a cell, `dragOffset` tracks a finger. The drag must not go through
            // the Animatable — every delta would have to launch a coroutine, and Animatable's
            // mutator mutex cancels whichever of those is still in flight, so deltas get dropped
            // and the lens lands on the wrong tab.
            val settle = remember { Animatable(selectedIndex * cellWidthPx) }
            var dragging by remember { mutableStateOf(false) }
            var dragOffset by remember { mutableFloatStateOf(settle.value) }
            val lensX = if (dragging) dragOffset else settle.value
            val maxOffset = (tabCount - 1) * cellWidthPx

            // Settle onto the selected cell whenever the selection or the geometry changes — a tap,
            // a deep link, a tab reorder, a rotation — but never while a finger is on the lens.
            LaunchedEffect(selectedIndex, cellWidthPx, dragging) {
                if (!dragging) {
                    settle.animateTo(
                        targetValue = selectedIndex * cellWidthPx,
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
                // Commit as the lens crosses into a cell rather than on release, so the screen
                // behind it changes under your finger the way the iOS tab bar does.
                val nearest = (dragOffset / cellWidthPx).roundToInt().coerceIn(0, tabCount - 1)
                state.tabs.getOrNull(nearest)?.let { if (it != state.selectedTab) onClick(it) }
            }

            Box(
                modifier =
                    Modifier.fillMaxWidth()
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Horizontal,
                            onDragStarted = {
                                dragOffset = settle.value
                                dragging = true
                            },
                            onDragStopped = {
                                // Hand the position back before switching source, so the lens
                                // doesn't jump on release; the effect above springs it home.
                                settle.snapTo(dragOffset)
                                dragging = false
                            },
                        )
            ) {
                // Declared first so it draws beneath the icons and labels, and sized with
                // matchParentSize so the bar below still decides the height.
                Box(modifier = Modifier.matchParentSize()) {
                    SelectionLens(
                        modifier =
                            Modifier.offset { IntOffset(lensX.roundToInt(), 0) }
                                .width(cellWidth)
                                .fillMaxHeight()
                                .padding(
                                    horizontal = LensHorizontalInset,
                                    vertical = LensVerticalInset,
                                )
                    )
                }

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
                                TabIcon(tab = tab, notificationCount = state.notificationCount)
                            },
                            label = { Text(stringResource(tab.getLabel())) },
                            colors =
                                ShortNavigationBarItemDefaults.colors(
                                    // Material defaults the selected label to `secondary`, which in
                                    // this theme is teal200 — near-invisible on the light pill.
                                    // Match the selected icon's treatment instead.
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

private val LensHorizontalInset = 6.dp
private val LensVerticalInset = 8.dp

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
                .clip(GlassDefaults.shape)
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
                    shape = GlassDefaults.shape,
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
 * A tab's icon, wrapped in a count badge on the More ([SETTINGS]) tab when notifications are
 * pending. Shared by the bottom bar and the side nav rail so both surfaces stay in sync.
 */
@Composable
private fun TabIcon(tab: BottomNavBloc.Tab, notificationCount: Int) {
    if (tab == SETTINGS && notificationCount > 0) {
        BadgedBox(badge = { Badge { Text(notificationCount.toString()) } }) {
            Icon(imageVector = tab.getIcon(), contentDescription = null)
        }
    } else {
        Icon(imageVector = tab.getIcon(), contentDescription = null)
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

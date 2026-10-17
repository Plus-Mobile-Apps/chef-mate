@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import chefmate.client.ui.public.generated.resources.Res
import chefmate.client.ui.public.generated.resources.back
import chefmate.client.ui.public.generated.resources.close
import com.plusmobileapps.chefmate.ui.components.glass.AppBackdrop
import com.plusmobileapps.chefmate.ui.components.glass.LocalAppBackdrop
import com.plusmobileapps.chefmate.ui.components.glass.appBackdropSource
import com.plusmobileapps.chefmate.ui.components.glass.backdropBlur
import com.plusmobileapps.chefmate.ui.components.glass.rememberChromeBackdrop
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

object PlusHeaderContainerDefaults {
    /**
     * Default cap applied to the content column. Exposed so callers that opt out (passing
     * `Dp.Unspecified` to keep the scroll surface full-width on wide windows) can still cap
     * individual rows at the same width for visual consistency.
     */
    val MaxContentWidth: Dp = 600.dp
}

// The content still reads through the header's blur, but the title and actions stay legible over
// whatever bright imagery scrolls underneath.
private const val HeaderBlurTintAlpha = 0.72f

@Composable
fun PlusHeaderContainer(
    data: PlusHeaderData,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scrollEnabled: Boolean = true,
    maxContentWidth: Dp = PlusHeaderContainerDefaults.MaxContentWidth,
    floatingActionButton: @Composable () -> Unit = {},
    floatingToolbar: (@Composable () -> Unit)? = null,
    floatingHeader: Boolean = false,
    headerContainerAlpha: Float = 1f,
    centerAlignTitle: Boolean = false,
    // When true the header renders as a large, collapsing title: the full title shows on its own
    // row while the content is at the top, then shrinks to a single-line ellipsized app-bar title
    // as the content scrolls up. Wired via a Material [TopAppBarScrollBehavior] whose nested-scroll
    // connection is attached to the container root, so the content's own scroll (e.g. an inner
    // LazyColumn) drives the collapse. Only honored in the standard (non-floating) layout.
    largeCollapsingTitle: Boolean = false,
    // Insets applied to the header itself. Defaults to null, which lets [PlusHeader] reserve the
    // status bar at the top. Pass insets without a top component when the container is hosted
    // somewhere already below the status bar (e.g. inside a ModalBottomSheet, where the drag handle
    // owns that spacing) so the app bar doesn't add a second gap. Ignored when [floatingHeader].
    headerWindowInsets: WindowInsets? = null,
    // When true the container reserves status-bar + app-bar space at the top *and* applies
    // horizontal display-cutout padding to the content. Pass false when the content manages all
    // its own insets (e.g. Cook Mode applies horizontal insets per body region) so the cutout
    // side isn't padded twice.
    applyContentInsets: Boolean = true,
    // Padding applied once around the capped content column, inside the display-cutout safe area.
    // Use this for a screen's horizontal gutters instead of padding each child composable. Defaults
    // to none so screens that manage their own content padding are unaffected.
    contentPadding: PaddingValues = PaddingValues(),
    // When true, non-scrolling content ([scrollEnabled] = false) is laid out full-height behind the
    // frosted header instead of below it, and must reserve [LocalPlusHeaderOverlap] at its top
    // itself — typically as an inner LazyColumn's top content padding, so the list scrolls up
    // behind the header. Scrolling content always draws behind the header; the container reserves
    // the space for it. [verticalArrangement]'s spacing is not applied below the header in this
    // mode. Only honored in the standard (non-floating) layout.
    drawContentBehindHeader: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    // Frosts the header and floating toolbar over the content scrolling behind them. Null in
    // previews and screenshot tests, where both keep their opaque colors.
    val backdrop = rememberChromeBackdrop()
    val headerTint = ChefMateTheme.colorScheme.background.copy(alpha = HeaderBlurTintAlpha)

    if (floatingHeader) {
        val topPadding =
            if (applyContentInsets) {
                with(density) { WindowInsets.statusBars.getTop(density).toDp() } + 64.dp
            } else {
                0.dp
            }
        Box(modifier = modifier.fillMaxSize().background(ChefMateTheme.colorScheme.background)) {
            ContainerBody(
                backdrop = backdrop,
                scrollEnabled = scrollEnabled,
                scrollState = scrollState,
                maxContentWidth = maxContentWidth,
                topPadding = topPadding,
                applyHorizontalInsets = applyContentInsets,
                horizontalAlignment = horizontalAlignment,
                contentPadding = contentPadding,
                floatingActionButton = floatingActionButton,
                floatingToolbar = floatingToolbar,
                content = content,
            )
            if (data !is PlusHeaderData.None) {
                PlusHeader(
                    data = data,
                    containerAlpha = if (backdrop != null) 0f else headerContainerAlpha,
                    centerAlign = centerAlignTitle,
                    windowInsets =
                        WindowInsets.systemBars
                            .union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .backdropBlur(
                                backdrop = backdrop,
                                tint = headerTint,
                                background = ChefMateTheme.colorScheme.background,
                            ),
                )
            }
        }
    } else {
        val scrollBehavior =
            if (largeCollapsingTitle) TopAppBarDefaults.exitUntilCollapsedScrollBehavior() else null
        val rootModifier =
            if (scrollBehavior != null) {
                modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            } else {
                modifier
            }
        val header: @Composable () -> Unit = {
            if (data !is PlusHeaderData.None) {
                PlusHeader(
                    data = data,
                    windowInsets = headerWindowInsets,
                    scrollBehavior = scrollBehavior,
                    containerAlpha = if (backdrop != null) 0f else 1f,
                    modifier =
                        Modifier.backdropBlur(
                            backdrop = backdrop,
                            tint = headerTint,
                            background = ChefMateTheme.colorScheme.background,
                        ),
                )
            }
        }
        if (scrollEnabled || drawContentBehindHeader) {
            // The header floats over the content so whatever scrolls up behind it shows through
            // the frosted blur. A scrolling container reserves the header's height (plus the
            // arrangement's spacing, which used to sit between the two) as the first thing inside
            // its scroll, so the content still starts below the header at rest; a
            // [drawContentBehindHeader] screen reserves it itself via [LocalPlusHeaderOverlap].
            val headerHeight = remember { mutableStateOf(0.dp) }
            val overlap = remember(headerHeight) { PlusHeaderOverlap(headerHeight) }
            Box(
                modifier =
                    rootModifier.fillMaxSize().background(ChefMateTheme.colorScheme.background)
            ) {
                HeaderOverlayLayout(
                    header = header,
                    onHeaderMeasured = { headerHeight.value = it },
                ) {
                    CompositionLocalProvider(LocalPlusHeaderOverlap provides overlap) {
                        ContainerBody(
                            backdrop = backdrop,
                            scrollEnabled = scrollEnabled,
                            scrollState = scrollState,
                            maxContentWidth = maxContentWidth,
                            scrollTopPadding =
                                if (scrollEnabled) {
                                    overlap.contentPadding(top = verticalArrangement.spacing)
                                } else {
                                    PaddingValues()
                                },
                            horizontalAlignment = horizontalAlignment,
                            contentPadding = contentPadding,
                            floatingActionButton = floatingActionButton,
                            floatingToolbar = floatingToolbar,
                            content = content,
                        )
                    }
                }
            }
        } else {
            // Non-scrolling content (typically an inner LazyColumn) that hasn't opted into drawing
            // behind the header can't reserve the header's height inside its own scroll, so it
            // keeps sitting below an opaque header.
            Column(
                modifier =
                    rootModifier.fillMaxSize().background(ChefMateTheme.colorScheme.background),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
            ) {
                if (data !is PlusHeaderData.None) {
                    PlusHeader(
                        data = data,
                        windowInsets = headerWindowInsets,
                        scrollBehavior = scrollBehavior,
                    )
                }
                ContainerBody(
                    modifier = Modifier.weight(1f),
                    backdrop = backdrop,
                    scrollEnabled = false,
                    scrollState = scrollState,
                    maxContentWidth = maxContentWidth,
                    horizontalAlignment = horizontalAlignment,
                    contentPadding = contentPadding,
                    floatingActionButton = floatingActionButton,
                    floatingToolbar = floatingToolbar,
                    content = content,
                )
            }
        }
    }
}

/**
 * Lays [header] over the top of [content], reporting the header's measured height through
 * [onHeaderMeasured] before [content] is measured in the same pass. [content] reads that height at
 * layout time (see [PlusHeaderOverlap]), so it never renders a frame tucked under the header —
 * including while a collapsing title changes the header's height.
 *
 * Deliberately a plain multi-content [Layout] rather than a SubcomposeLayout: subcomposing the
 * content puts it in a separate composition, and when an enclosing movable content (Decompose's
 * stack, the bottom-nav shells) moved it in the same frame as a text field inside it changed, the
 * text field's modifier was updated while detached and the screen crashed on teardown.
 */
@Composable
internal fun HeaderOverlayLayout(
    header: @Composable () -> Unit,
    onHeaderMeasured: (Dp) -> Unit,
    content: @Composable () -> Unit,
) {
    Layout(contents = listOf(header, content), modifier = Modifier.fillMaxSize()) {
        (headerMeasurables, contentMeasurables),
        constraints ->
        val headerPlaceables = headerMeasurables.map { it.measure(constraints.copy(minHeight = 0)) }
        onHeaderMeasured((headerPlaceables.maxOfOrNull { it.height } ?: 0).toDp())
        val contentPlaceables = contentMeasurables.map { it.measure(constraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            contentPlaceables.forEach { it.place(0, 0) }
            headerPlaceables.forEach { it.place(0, 0) }
        }
    }
}

/**
 * The content surface: the (optionally scrolling) content column, the FAB and the floating toolbar.
 * The content column is the [backdrop]'s source, so the header and the floating toolbar blur what
 * scrolls behind them. The FAB and toolbar sit outside that source so they never blur themselves.
 */
@Composable
private fun ContainerBody(
    backdrop: AppBackdrop?,
    scrollEnabled: Boolean,
    scrollState: ScrollState,
    maxContentWidth: Dp,
    horizontalAlignment: Alignment.Horizontal,
    contentPadding: PaddingValues,
    floatingActionButton: @Composable () -> Unit,
    floatingToolbar: (@Composable () -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    topPadding: Dp = 0.dp,
    scrollTopPadding: PaddingValues = PaddingValues(),
    applyHorizontalInsets: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = ChefMateTheme.colorScheme.background,
        contentColor = ChefMateTheme.colorScheme.onBackground,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ScrollingContent(
                modifier = Modifier.appBackdropSource(backdrop),
                scrollEnabled = scrollEnabled,
                scrollState = scrollState,
                maxContentWidth = maxContentWidth,
                topPadding = topPadding,
                scrollTopPadding = scrollTopPadding,
                applyHorizontalInsets = applyHorizontalInsets,
                horizontalAlignment = horizontalAlignment,
                contentPadding = contentPadding,
                content = content,
            )
            BottomBarBox(
                density = LocalDensity.current,
                floatingActionButton = floatingActionButton,
            )
            floatingToolbar?.let {
                // Scoped to this container's backdrop (or to none) so a toolbar never blurs an
                // enclosing shell's backdrop, which would include the toolbar itself.
                CompositionLocalProvider(LocalAppBackdrop provides backdrop) {
                    Box(modifier = Modifier.align(BottomCenter).floatingToolbarPadding()) { it() }
                }
            }
        }
    }
}

@Composable
private fun BottomBarBox(
    density: Density,
    floatingActionButton: @Composable (() -> Unit),
) {
    Column {
        Spacer(modifier = Modifier.weight(1f))
        Row(
            modifier =
                Modifier.padding(
                    end =
                        with(density) {
                            WindowInsets.displayCutout.getRight(density, LayoutDirection.Ltr).toDp()
                        },
                    // Bottom breathing room the (now-removed) snackbar slot used to reserve below
                    // the FAB. Kept so the FAB sits exactly where it did before the toast
                    // migration.
                    bottom = ChefMateTheme.dimens.paddingNormal,
                )
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.padding(end = ChefMateTheme.dimens.paddingNormal)) {
                floatingActionButton()
            }
        }
        Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
}

@Composable
private fun ScrollingContent(
    modifier: Modifier = Modifier,
    scrollEnabled: Boolean,
    scrollState: ScrollState,
    maxContentWidth: Dp,
    topPadding: Dp = 0.dp,
    // Space reserved as the first thing *inside* the scroll, so content scrolls up behind whatever
    // covers it (the frosted header) rather than being clipped below it like [topPadding].
    scrollTopPadding: PaddingValues = PaddingValues(),
    applyHorizontalInsets: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable (ColumnScope.() -> Unit),
) {
    // The scroll surface fills the full width so the wheel/drag is captured everywhere — including
    // the gutters on wide windows (desktop), where the capped content column doesn't reach. The
    // visible content stays capped at [maxContentWidth] and centered inside it.
    val scrollModifier =
        modifier.fillMaxSize().padding(top = topPadding).let {
            if (scrollEnabled) it.verticalScroll(scrollState) else it
        }
    Column(modifier = scrollModifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.padding(scrollTopPadding))
        // Cap before filling: `fillMaxWidth()` first would pin the column to the full width and
        // leave `widthIn(max)` nothing to shrink.
        val baseModifier = Modifier.widthIn(max = maxContentWidth).fillMaxWidth()
        val insetModifier =
            if (applyHorizontalInsets) baseModifier.scaffoldContentInsetPadding() else baseModifier
        Column(
            modifier = insetModifier.padding(contentPadding),
            horizontalAlignment = horizontalAlignment,
        ) {
            content()
            if (scrollEnabled) {
                Spacer(modifier = Modifier.padding(WindowInsets.systemGestures.asPaddingValues()))
            }
        }
    }
}

@Composable
fun PlusHeader(
    data: PlusHeaderData,
    windowInsets: WindowInsets? = null,
    containerAlpha: Float = 1f,
    centerAlign: Boolean = false,
    // When non-null the header renders as a large, collapsing app bar driven by this behavior. The
    // title spans up to two lines while expanded and collapses to a single ellipsized line.
    scrollBehavior: TopAppBarScrollBehavior? = null,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val resolvedInsets =
        windowInsets
            ?: WindowInsets(
                left = WindowInsets.displayCutout.getLeft(density, LayoutDirection.Ltr),
                right = WindowInsets.displayCutout.getRight(density, LayoutDirection.Ltr),
                top = WindowInsets.statusBars.getTop(density),
                bottom = WindowInsets.statusBars.getBottom(density),
            )
    val colors =
        if (containerAlpha < 1f) {
            // Both colors, so a collapsing app bar doesn't turn opaque as it scrolls.
            ChefMateTheme.colorScheme.background.copy(alpha = containerAlpha).let {
                TopAppBarDefaults.topAppBarColors(containerColor = it, scrolledContainerColor = it)
            }
        } else {
            TopAppBarDefaults.topAppBarColors()
        }
    // A collapsing large title shows up to two lines while expanded, then shrinks to a single
    // ellipsized line once mostly collapsed. A regular app bar keeps the existing two-line title.
    val titleMaxLines =
        when {
            scrollBehavior == null -> 2
            scrollBehavior.state.collapsedFraction < 0.5f -> 2
            else -> 1
        }
    val titleText: @Composable () -> Unit = {
        Text(
            text = data.title.localized(),
            color = ChefMateTheme.colorScheme.onBackground,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
    val centerContent = (data as? PlusHeaderData.Parent)?.center
    // The title area is the only middle slot an app bar offers, so centered content is overlaid on
    // it: the title keeps its start alignment while the centered content sits in the middle of
    // whatever space is left between the leading content and the actions.
    val title: @Composable () -> Unit =
        if (centerContent == null) {
            titleText
        } else {
            {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.align(Alignment.CenterStart)) { titleText() }
                    Box(modifier = Modifier.align(Alignment.Center)) { centerContent() }
                }
            }
        }
    val navigationIcon: @Composable () -> Unit =
        when (data) {
            is PlusHeaderData.Child -> {
                {
                    PlusIconButton(
                        modifier = Modifier.padding(end = ChefMateTheme.dimens.paddingSmall),
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        onClick = data.onBackClick,
                    )
                }
            }
            is PlusHeaderData.Modal -> {
                {
                    PlusIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.close),
                        onClick = data.onCloseClick,
                    )
                }
            }
            is PlusHeaderData.Parent -> data.leading ?: {}
            PlusHeaderData.None -> {
                {}
            }
        }
    val actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        when (val trailingAccessory = data.trailingAccessory) {
            is PlusHeaderData.TrailingAccessory.Button -> {
                PlusButton(text = trailingAccessory.text, onClick = trailingAccessory.onClick)
            }
            is PlusHeaderData.TrailingAccessory.Custom -> trailingAccessory.content(this)
            null -> Unit
            is PlusHeaderData.TrailingAccessory.Icon -> {
                if (trailingAccessory.onClick != null) {
                    IconButton(onClick = trailingAccessory.onClick) {
                        Icon(
                            trailingAccessory.icon,
                            contentDescription = trailingAccessory.contentDesc.localized(),
                            tint = ChefMateTheme.colorScheme.onBackground,
                        )
                    }
                } else {
                    Icon(
                        trailingAccessory.icon,
                        contentDescription = trailingAccessory.contentDesc.localized(),
                        tint = ChefMateTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
    if (scrollBehavior != null) {
        LargeTopAppBar(
            modifier = modifier,
            windowInsets = resolvedInsets,
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            scrollBehavior = scrollBehavior,
        )
    } else if (centerAlign) {
        CenterAlignedTopAppBar(
            modifier = modifier,
            windowInsets = resolvedInsets,
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
        )
    } else {
        TopAppBar(
            modifier = modifier,
            windowInsets = resolvedInsets,
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
        )
    }
}

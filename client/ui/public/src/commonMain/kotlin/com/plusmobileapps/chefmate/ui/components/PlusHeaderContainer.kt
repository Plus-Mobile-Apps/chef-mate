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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import chefmate.client.ui.public.generated.resources.Res
import chefmate.client.ui.public.generated.resources.back
import chefmate.client.ui.public.generated.resources.close
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
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    if (floatingHeader) {
        val topPadding =
            if (applyContentInsets) {
                with(density) { WindowInsets.statusBars.getTop(density).toDp() } + 64.dp
            } else {
                0.dp
            }
        Box(modifier = modifier.fillMaxSize().background(ChefMateTheme.colorScheme.background)) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = ChefMateTheme.colorScheme.background,
                contentColor = ChefMateTheme.colorScheme.onBackground,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ScrollingContent(
                        scrollEnabled = scrollEnabled,
                        scrollState = scrollState,
                        maxContentWidth = maxContentWidth,
                        topPadding = topPadding,
                        applyHorizontalInsets = applyContentInsets,
                        horizontalAlignment = horizontalAlignment,
                        contentPadding = contentPadding,
                        content = content,
                    )
                    BottomBarBox(
                        density = density,
                        floatingActionButton = floatingActionButton,
                    )
                    floatingToolbar?.let {
                        Box(modifier = Modifier.align(BottomCenter).floatingToolbarPadding()) {
                            it()
                        }
                    }
                }
            }
            if (data !is PlusHeaderData.None) {
                PlusHeader(
                    data = data,
                    containerAlpha = headerContainerAlpha,
                    centerAlign = centerAlignTitle,
                    windowInsets =
                        WindowInsets.systemBars
                            .union(WindowInsets.displayCutout)
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                    modifier = Modifier.align(Alignment.TopCenter),
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
        Column(
            modifier = rootModifier.fillMaxSize().background(ChefMateTheme.colorScheme.background),
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

            Surface(
                modifier = Modifier.weight(1f),
                color = ChefMateTheme.colorScheme.background,
                contentColor = ChefMateTheme.colorScheme.onBackground,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ScrollingContent(
                        scrollEnabled = scrollEnabled,
                        scrollState = scrollState,
                        maxContentWidth = maxContentWidth,
                        applyHorizontalInsets = true,
                        horizontalAlignment = horizontalAlignment,
                        contentPadding = contentPadding,
                        content = content,
                    )
                    BottomBarBox(
                        density = density,
                        floatingActionButton = floatingActionButton,
                    )

                    floatingToolbar?.let {
                        Box(modifier = Modifier.align(BottomCenter).floatingToolbarPadding()) {
                            it()
                        }
                    }
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
        val baseModifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)
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
            TopAppBarDefaults.topAppBarColors(
                containerColor = ChefMateTheme.colorScheme.background.copy(alpha = containerAlpha)
            )
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

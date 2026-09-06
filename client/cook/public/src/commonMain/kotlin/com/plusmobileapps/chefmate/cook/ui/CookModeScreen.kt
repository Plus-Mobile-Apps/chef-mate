@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.plusmobileapps.chefmate.cook.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chefmate.client.cook.public.generated.resources.Res
import chefmate.client.cook.public.generated.resources.cook_mode_ai_chat
import chefmate.client.cook.public.generated.resources.cook_mode_ai_chat_premium
import chefmate.client.cook.public.generated.resources.cook_mode_allow_screen_off
import chefmate.client.cook.public.generated.resources.cook_mode_directions
import chefmate.client.cook.public.generated.resources.cook_mode_finish
import chefmate.client.cook.public.generated.resources.cook_mode_finish_cancel_button
import chefmate.client.cook.public.generated.resources.cook_mode_finish_confirm_button
import chefmate.client.cook.public.generated.resources.cook_mode_finish_confirm_message
import chefmate.client.cook.public.generated.resources.cook_mode_finish_confirm_title
import chefmate.client.cook.public.generated.resources.cook_mode_ingredients
import chefmate.client.cook.public.generated.resources.cook_mode_keep_screen_on
import chefmate.client.cook.public.generated.resources.cook_mode_keep_screen_on_onboarding
import chefmate.client.cook.public.generated.resources.cook_mode_layout_onboarding
import chefmate.client.cook.public.generated.resources.cook_mode_layout_split
import chefmate.client.cook.public.generated.resources.cook_mode_layout_stacked
import chefmate.client.cook.public.generated.resources.cook_mode_loading
import chefmate.client.cook.public.generated.resources.cook_mode_no_active_recipe
import chefmate.client.cook.public.generated.resources.cook_mode_scale_ingredients
import chefmate.client.cook.public.generated.resources.cook_mode_whats_cooking
import com.plusmobileapps.chefmate.cook.CookModeBloc
import com.plusmobileapps.chefmate.cook.WhatsCookingBloc
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.recipe.data.IngredientScaler
import com.plusmobileapps.chefmate.recipe.data.IngredientSection
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.subscription.ui.PremiumLockedIcon
import com.plusmobileapps.chefmate.subscription.ui.PremiumRequiredDialog
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.KeepScreenOn
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.components.PlusOnboardingTooltip
import com.plusmobileapps.chefmate.ui.components.PlusTooltipPlacement
import com.plusmobileapps.chefmate.ui.components.WindowSizeClass
import com.plusmobileapps.chefmate.ui.text.parseListLine
import com.plusmobileapps.chefmate.ui.text.toDisplayAnnotatedString
import com.plusmobileapps.chefmate.ui.text.withoutLineBreakTags
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun CookModeScreen(bloc: CookModeBloc, modifier: Modifier = Modifier) {
    val state by bloc.state.collectAsState()
    if (state.keepScreenOn) {
        KeepScreenOn()
    }
    // Upsell shown when a non-subscriber taps the AI chat button.
    if (state.showPremiumRequiredDialog) {
        PremiumRequiredDialog(onDismiss = bloc::onPremiumRequiredDismissed)
    }
    // Ingredient scale multiplier for the active recipe. Owned by the BLoC so it persists per
    // recipe
    // across Cook Mode / recipe detail; the header control and body both read it from state.
    val ingredientScale = state.ingredientScale

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val windowSizeClass =
                when {
                    maxWidth < 600.dp -> WindowSizeClass.COMPACT
                    maxWidth < 840.dp -> WindowSizeClass.MEDIUM
                    else -> WindowSizeClass.EXPANDED
                }
            val isCompactHeight = maxHeight < 480.dp
            if (windowSizeClass == WindowSizeClass.COMPACT) {
                CookModeMobileLayout(
                    bloc = bloc,
                    state = state,
                    windowSizeClass = windowSizeClass,
                    scale = ingredientScale,
                    onScaleChange = bloc::onScaleChanged,
                )
            } else {
                CookModeTabletLayout(
                    bloc = bloc,
                    state = state,
                    windowSizeClass = windowSizeClass,
                    isCompactHeight = isCompactHeight,
                    scale = ingredientScale,
                    onScaleChange = bloc::onScaleChanged,
                )
            }
        }
    }
}

/**
 * Header dropdown that multiplies every ingredient amount in the active recipe by the chosen factor
 * (½× through 4×). Placed in the top-bar so it applies uniformly across Cook Mode's stacked, split,
 * and tabbed layouts.
 */
@Composable
private fun CookModeScaleButton(scale: Double, onScaleChange: (Double) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) {
        Text(IngredientScaler.label(scale))
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = stringResource(Res.string.cook_mode_scale_ingredients),
        )
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        IngredientScaler.DEFAULT_FACTORS.forEach { option ->
            DropdownMenuItem(
                text = { Text(IngredientScaler.label(option)) },
                trailingIcon = {
                    if (option == scale) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    }
                },
                onClick = {
                    expanded = false
                    onScaleChange(option)
                },
            )
        }
    }
}

/**
 * Wraps a header control in a first-run coach mark that points up at it (the cook-mode header sits
 * at the top of the screen). The bubble only shows while [id] is the shared controller's active
 * mark.
 */
@Composable
private fun CookModeCoachMark(
    id: String,
    text: TextData,
    activeCoachMark: String?,
    onDismiss: (String) -> Unit,
    anchor: @Composable () -> Unit,
) {
    PlusOnboardingTooltip(
        text = text,
        visible = activeCoachMark == id,
        onDismiss = { onDismiss(id) },
        placement = PlusTooltipPlacement.BELOW,
        anchor = anchor,
    )
}

/**
 * Header control that opens the AI chat grounded in the active recipe. Rendered only when the AI
 * Chat feature flag is on (via [visible]); nothing is emitted otherwise.
 *
 * Without [subscribed] the control stays tappable but dimmed — the click opens the premium upsell
 * rather than the chat, so disabling it would be misleading.
 */
@Composable
private fun CookModeAiChatButton(visible: Boolean, subscribed: Boolean, onClick: () -> Unit) {
    if (!visible) return
    IconButton(onClick = onClick) {
        if (subscribed) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(Res.string.cook_mode_ai_chat),
            )
        } else {
            PremiumLockedIcon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = stringResource(Res.string.cook_mode_ai_chat_premium),
            )
        }
    }
}

/**
 * Check-mark control that ends cook mode. Tapping it opens a confirmation dialog first; confirming
 * runs [onFinish], which clears every recipe from cook mode and closes the screen.
 */
@Composable
private fun CookModeFinishButton(onFinish: () -> Unit) {
    var showConfirm by remember { mutableStateOf(false) }
    IconButton(onClick = { showConfirm = true }) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = stringResource(Res.string.cook_mode_finish),
        )
    }
    if (showConfirm) {
        PlusDialog(
            title = Res.string.cook_mode_finish_confirm_title.asTextData(),
            message = Res.string.cook_mode_finish_confirm_message.asTextData(),
            confirmButtonText = Res.string.cook_mode_finish_confirm_button.asTextData(),
            dismissButtonText = Res.string.cook_mode_finish_cancel_button.asTextData(),
            onConfirmClick = {
                showConfirm = false
                onFinish()
            },
            onDismissRequest = { showConfirm = false },
        )
    }
}

@Composable
private fun CookModeTabletLayout(
    bloc: CookModeBloc,
    state: CookModeBloc.Model,
    windowSizeClass: WindowSizeClass,
    isCompactHeight: Boolean,
    scale: Double,
    onScaleChange: (Double) -> Unit,
) {
    var showWhatsCooking by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        PlusHeaderContainer(
            data =
                PlusHeaderData.Modal(
                    title = FixedString(state.activeRecipe?.title.orEmpty()),
                    onCloseClick = bloc::onCloseClicked,
                    trailingAccessory =
                        PlusHeaderData.TrailingAccessory.Custom {
                            if (isCompactHeight) {
                                IconButton(onClick = { showWhatsCooking = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Restaurant,
                                        contentDescription =
                                            stringResource(Res.string.cook_mode_whats_cooking),
                                    )
                                }
                            }
                            CookModeAiChatButton(
                                visible = state.showAiChat,
                                subscribed = state.isSubscribed,
                                onClick = bloc::onAiChatClicked,
                            )
                            CookModeScaleButton(scale = scale, onScaleChange = onScaleChange)
                            CookModeCoachMark(
                                id = CoachMarkId.COOK_MODE_KEEP_SCREEN_ON,
                                text = Res.string.cook_mode_keep_screen_on_onboarding.asTextData(),
                                activeCoachMark = state.activeCoachMark,
                                onDismiss = bloc::onCoachMarkDismissed,
                            ) {
                                IconButton(onClick = bloc::onKeepScreenOnToggled) {
                                    Icon(
                                        imageVector =
                                            if (state.keepScreenOn) Icons.Default.Visibility
                                            else Icons.Default.VisibilityOff,
                                        contentDescription =
                                            stringResource(
                                                if (state.keepScreenOn)
                                                    Res.string.cook_mode_allow_screen_off
                                                else Res.string.cook_mode_keep_screen_on
                                            ),
                                    )
                                }
                            }
                            CookModeCoachMark(
                                id = CoachMarkId.COOK_MODE_LAYOUT,
                                text = Res.string.cook_mode_layout_onboarding.asTextData(),
                                activeCoachMark = state.activeCoachMark,
                                onDismiss = bloc::onCoachMarkDismissed,
                            ) {
                                IconButton(onClick = bloc::onLayoutToggled) {
                                    Icon(
                                        imageVector =
                                            if (state.layoutMode == CookModeBloc.LayoutMode.Stacked)
                                                Icons.Default.ViewWeek
                                            else Icons.Default.ViewAgenda,
                                        contentDescription =
                                            stringResource(
                                                if (
                                                    state.layoutMode ==
                                                        CookModeBloc.LayoutMode.Stacked
                                                )
                                                    Res.string.cook_mode_layout_split
                                                else Res.string.cook_mode_layout_stacked
                                            ),
                                    )
                                }
                            }
                            CookModeFinishButton(onFinish = bloc::onFinishClicked)
                        },
                ),
            modifier = Modifier.fillMaxSize(),
            floatingHeader = true,
            headerContainerAlpha = 0.85f,
            centerAlignTitle = true,
            applyContentInsets = false,
            scrollEnabled = false,
            maxContentWidth = Dp.Unspecified,
        ) {
            CookModeBody(
                isLoading = state.isLoading,
                recipe = state.activeRecipe,
                layoutMode = state.layoutMode,
                windowSizeClass = windowSizeClass,
                scale = scale,
                bottomReserve = if (isCompactHeight) 0.dp else BottomBarReserve,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (!isCompactHeight) {
            CookModeBottomBar(
                chips = state.activeSessions,
                onWhatsCookingClicked = { showWhatsCooking = true },
                onChipClicked = bloc::onRecipeChipClicked,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showWhatsCooking) {
        WhatsCookingModalSheet(
            bloc = bloc.whatsCookingBloc,
            onDismiss = { showWhatsCooking = false },
        )
    }
}

@Composable
private fun WhatsCookingModalSheet(bloc: WhatsCookingBloc, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        dragHandle = {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(statusBarTop))
                BottomSheetDefaults.DragHandle()
            }
        },
    ) {
        WhatsCookingScreen(
            bloc = bloc,
            onClose = onDismiss,
            onRecipeSelected = onDismiss,
            modifier = Modifier.padding(bottom = navBarBottom),
        )
    }
}

@Composable
private fun CookModeMobileLayout(
    bloc: CookModeBloc,
    state: CookModeBloc.Model,
    windowSizeClass: WindowSizeClass,
    scale: Double,
    onScaleChange: (Double) -> Unit,
) {
    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val scope = rememberCoroutineScope()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gestureBottom = WindowInsets.systemGestures.asPaddingValues().calculateBottomPadding()
    // Reserve enough at the bottom that interactive sheet content sits clear of the gesture-nav
    // strip on devices using gesture nav (where navigationBars inset is ~0).
    val safeBottom = maxOf(navBarBottom, gestureBottom)
    val peekHeight = MobilePeekHeight + safeBottom

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peekHeight,
        sheetContent = {
            CookingPeekRow(
                title = state.activeRecipe?.title,
                onIconClicked = { scope.launch { sheetState.expand() } },
            )
            HorizontalDivider()
            WhatsCookingScreen(
                bloc = bloc.whatsCookingBloc,
                onClose = { scope.launch { sheetState.partialExpand() } },
                onRecipeSelected = { scope.launch { sheetState.partialExpand() } },
                modifier = Modifier.padding(bottom = safeBottom),
            )
        },
    ) { bodyPadding ->
        // Don't shrink the body by the sheet peek — let the scrolling content run all the way down
        // behind the opaque sheet, and reserve the peek as scroll padding instead so the last item
        // clears the sheet without an empty band above it.
        Box(modifier = Modifier.fillMaxSize()) {
            PlusHeaderContainer(
                data =
                    PlusHeaderData.Modal(
                        title = FixedString(state.activeRecipe?.title.orEmpty()),
                        onCloseClick = bloc::onCloseClicked,
                        trailingAccessory =
                            PlusHeaderData.TrailingAccessory.Custom {
                                CookModeAiChatButton(
                                    visible = state.showAiChat,
                                    subscribed = state.isSubscribed,
                                    onClick = bloc::onAiChatClicked,
                                )
                                CookModeScaleButton(scale = scale, onScaleChange = onScaleChange)
                                CookModeCoachMark(
                                    id = CoachMarkId.COOK_MODE_KEEP_SCREEN_ON,
                                    text =
                                        Res.string.cook_mode_keep_screen_on_onboarding.asTextData(),
                                    activeCoachMark = state.activeCoachMark,
                                    onDismiss = bloc::onCoachMarkDismissed,
                                ) {
                                    IconButton(onClick = bloc::onKeepScreenOnToggled) {
                                        Icon(
                                            imageVector =
                                                if (state.keepScreenOn) Icons.Default.Visibility
                                                else Icons.Default.VisibilityOff,
                                            contentDescription =
                                                stringResource(
                                                    if (state.keepScreenOn)
                                                        Res.string.cook_mode_allow_screen_off
                                                    else Res.string.cook_mode_keep_screen_on
                                                ),
                                        )
                                    }
                                }
                                CookModeCoachMark(
                                    id = CoachMarkId.COOK_MODE_LAYOUT,
                                    text = Res.string.cook_mode_layout_onboarding.asTextData(),
                                    activeCoachMark = state.activeCoachMark,
                                    onDismiss = bloc::onCoachMarkDismissed,
                                ) {
                                    IconButton(onClick = bloc::onLayoutToggled) {
                                        Icon(
                                            imageVector =
                                                if (
                                                    state.layoutMode ==
                                                        CookModeBloc.LayoutMode.Stacked
                                                )
                                                    Icons.Default.ViewWeek
                                                else Icons.Default.ViewAgenda,
                                            contentDescription =
                                                stringResource(
                                                    if (
                                                        state.layoutMode ==
                                                            CookModeBloc.LayoutMode.Stacked
                                                    )
                                                        Res.string.cook_mode_layout_split
                                                    else Res.string.cook_mode_layout_stacked
                                                ),
                                        )
                                    }
                                }
                                CookModeFinishButton(onFinish = bloc::onFinishClicked)
                            },
                    ),
                modifier = Modifier.fillMaxSize(),
                floatingHeader = true,
                headerContainerAlpha = 0.85f,
                centerAlignTitle = true,
                applyContentInsets = false,
                scrollEnabled = false,
                maxContentWidth = Dp.Unspecified,
            ) {
                CookModeBody(
                    isLoading = state.isLoading,
                    recipe = state.activeRecipe,
                    layoutMode = state.layoutMode,
                    windowSizeClass = windowSizeClass,
                    scale = scale,
                    bottomReserve = bodyPadding.calculateBottomPadding(),
                    // The sheet peek already sits above the nav bar and covers it, so don't add the
                    // bottom system inset on top of the reserve — that's what left the clipped gap.
                    applyBottomInset = false,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun CookingPeekRow(
    title: String?,
    onIconClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onIconClicked) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = stringResource(Res.string.cook_mode_whats_cooking),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title ?: stringResource(Res.string.cook_mode_no_active_recipe),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CookModeBody(
    isLoading: Boolean,
    recipe: Recipe?,
    layoutMode: CookModeBloc.LayoutMode,
    windowSizeClass: WindowSizeClass,
    scale: Double,
    bottomReserve: Dp,
    modifier: Modifier = Modifier,
    applyBottomInset: Boolean = true,
) {
    when {
        isLoading -> LoadingState(modifier)
        recipe == null -> EmptyState(modifier)
        else -> {
            // Strikethrough + highlight state lives at this level so it survives layout-mode
            // toggles for the same recipe; reset when the active recipe changes.
            val ingredientLines = remember(recipe.ingredients) { splitLines(recipe.ingredients) }
            val crossedOut =
                remember(recipe.id, recipe.ingredients) {
                    mutableStateListOf(*BooleanArray(ingredientLines.size) { false }.toTypedArray())
                }
            val directionParagraphs = remember(recipe.directions) { splitLines(recipe.directions) }
            var highlightedDirectionIndex by
                remember(recipe.id, recipe.directions) { mutableStateOf(-1) }
            val onDirectionToggled: (Int) -> Unit = { index ->
                highlightedDirectionIndex = if (highlightedDirectionIndex == index) -1 else index
            }

            Box(modifier = modifier) {
                when (layoutMode) {
                    CookModeBloc.LayoutMode.Stacked ->
                        StackedLayout(
                            ingredientLines = ingredientLines,
                            crossedOut = crossedOut,
                            scale = scale,
                            directionParagraphs = directionParagraphs,
                            highlightedIndex = highlightedDirectionIndex,
                            onDirectionToggled = onDirectionToggled,
                            bottomReserve = bottomReserve,
                            applyBottomInset = applyBottomInset,
                        )
                    CookModeBloc.LayoutMode.Split ->
                        when (windowSizeClass) {
                            WindowSizeClass.COMPACT ->
                                SplitCompactLayout(
                                    ingredientLines = ingredientLines,
                                    crossedOut = crossedOut,
                                    scale = scale,
                                    directionParagraphs = directionParagraphs,
                                    highlightedIndex = highlightedDirectionIndex,
                                    onDirectionToggled = onDirectionToggled,
                                    bottomReserve = bottomReserve,
                                    applyBottomInset = applyBottomInset,
                                )
                            WindowSizeClass.MEDIUM,
                            WindowSizeClass.EXPANDED ->
                                SplitWideLayout(
                                    ingredientLines = ingredientLines,
                                    crossedOut = crossedOut,
                                    scale = scale,
                                    directionParagraphs = directionParagraphs,
                                    highlightedIndex = highlightedDirectionIndex,
                                    onDirectionToggled = onDirectionToggled,
                                    bottomReserve = bottomReserve,
                                    applyBottomInset = applyBottomInset,
                                )
                        }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    val padding = ChefMateTheme.dimens.paddingNormal
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PlusLoadingIndicator()
            Spacer(Modifier.height(padding))
            Text(stringResource(Res.string.cook_mode_loading))
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.cook_mode_no_active_recipe),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val AppBarHeight = 64.dp
private val BottomBarReserve = 96.dp

/**
 * Peek-state height of the persistent mobile sheet: enough room for the default drag handle (~48dp)
 * plus the IconButton + title row (~48dp). Anything larger leaves an awkward empty band between the
 * drag handle and the row content.
 */
private val MobilePeekHeight = 96.dp

/**
 * Cook Mode draws edge-to-edge with a floating top app bar (and on tablet a floating bottom bar),
 * so body padding must reserve room for those bars *plus* keep text out of any display cutout (e.g.
 * landscape camera hole on Pixel). On mobile the persistent bottom sheet handles its own spacing
 * via [BottomSheetScaffold]'s padding values, so the caller passes that peek as [bottomReserve] and
 * sets [applyBottomInset] = false (the sheet sits above and covers the bottom system inset).
 */
@Composable
private fun bodyContentPadding(bottomReserve: Dp, applyBottomInset: Boolean = true): PaddingValues {
    val safe = WindowInsets.systemBars.union(WindowInsets.displayCutout).asPaddingValues()
    val layoutDir = LocalLayoutDirection.current
    val bottomInset = if (applyBottomInset) safe.calculateBottomPadding() else 0.dp
    return PaddingValues(
        start = safe.calculateStartPadding(layoutDir),
        end = safe.calculateEndPadding(layoutDir),
        top = safe.calculateTopPadding() + AppBarHeight,
        bottom = bottomInset + bottomReserve,
    )
}

@Composable
private fun StackedLayout(
    ingredientLines: List<String>,
    crossedOut: SnapshotStateList<Boolean>,
    scale: Double,
    directionParagraphs: List<String>,
    highlightedIndex: Int,
    onDirectionToggled: (Int) -> Unit,
    bottomReserve: Dp,
    applyBottomInset: Boolean,
) {
    val padding = ChefMateTheme.dimens.paddingNormal
    val contentPadding = bodyContentPadding(bottomReserve, applyBottomInset)
    val layoutDir = LocalLayoutDirection.current
    val lazyListState = rememberLazyListState()

    val ingredientsTitle = stringResource(Res.string.cook_mode_ingredients)
    val directionsTitle = stringResource(Res.string.cook_mode_directions)

    // List layout (no sticky headers):
    //   indices 0..N-1     → ingredient items
    //   index  N           → directions section header (regular item, scrolls normally)
    //   indices N+1..      → direction items
    // Switch to "Directions" label when the directions header or a direction item is first visible.
    val currentSectionTitle by
        remember(ingredientLines.size) {
            derivedStateOf {
                if (lazyListState.firstVisibleItemIndex < ingredientLines.size) ingredientsTitle
                else directionsTitle
            }
        }

    // Measure the overlay header height so contentPadding reserves the same space below the
    // app bar. Starts at 0 on the first frame then stabilises immediately.
    var overlayHeaderHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = lazyListState,
            contentPadding =
                PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDir) + padding,
                    end = contentPadding.calculateEndPadding(layoutDir) + padding,
                    top =
                        contentPadding.calculateTopPadding() +
                            with(density) { overlayHeaderHeightPx.toDp() },
                    bottom = contentPadding.calculateBottomPadding(),
                ),
            verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingExtraSmall),
        ) {
            itemsIndexed(ingredientLines, key = { i, _ -> "ingredient_$i" }) { index, line ->
                IngredientRow(
                    text = line,
                    scale = scale,
                    crossedOut = crossedOut[index],
                    onClick = { crossedOut[index] = !crossedOut[index] },
                )
            }
            // Directions header as a regular scrollable item — acts as a visual section divider.
            item(key = "directions_header") { CookSectionHeader(directionsTitle) }
            itemsIndexed(directionParagraphs, key = { i, _ -> "direction_$i" }) { index, paragraph
                ->
                DirectionRow(
                    text = paragraph,
                    highlighted = highlightedIndex == index,
                    onClick = { onDirectionToggled(index) },
                )
            }
        }

        // Overlay sticky header pinned to the bottom edge of the floating app bar.
        // Items scroll freely through/behind the transparent app bar; only this header is anchored.
        CookSectionHeader(
            title = currentSectionTitle,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(top = contentPadding.calculateTopPadding())
                    .onSizeChanged { overlayHeaderHeightPx = it.height },
        )
    }
}

@Composable
private fun SplitCompactLayout(
    ingredientLines: List<String>,
    crossedOut: SnapshotStateList<Boolean>,
    scale: Double,
    directionParagraphs: List<String>,
    highlightedIndex: Int,
    onDirectionToggled: (Int) -> Unit,
    bottomReserve: Dp,
    applyBottomInset: Boolean,
) {
    val padding = ChefMateTheme.dimens.paddingNormal
    val contentPadding = bodyContentPadding(bottomReserve, applyBottomInset)
    val layoutDir = LocalLayoutDirection.current
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs =
        listOf(
            stringResource(Res.string.cook_mode_ingredients),
            stringResource(Res.string.cook_mode_directions),
        )

    Column(
        modifier =
            Modifier.fillMaxSize()
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDir),
                    end = contentPadding.calculateEndPadding(layoutDir),
                    top = contentPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding(),
                )
    ) {
        SecondaryTabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) },
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = 1,
        ) { page ->
            Column(
                modifier =
                    Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = padding),
                verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingExtraSmall),
            ) {
                when (page) {
                    0 ->
                        ingredientLines.forEachIndexed { index, line ->
                            IngredientRow(
                                text = line,
                                scale = scale,
                                crossedOut = crossedOut[index],
                                onClick = { crossedOut[index] = !crossedOut[index] },
                            )
                        }
                    1 ->
                        directionParagraphs.forEachIndexed { index, paragraph ->
                            DirectionRow(
                                text = paragraph,
                                highlighted = highlightedIndex == index,
                                onClick = { onDirectionToggled(index) },
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun SplitWideLayout(
    ingredientLines: List<String>,
    crossedOut: SnapshotStateList<Boolean>,
    scale: Double,
    directionParagraphs: List<String>,
    highlightedIndex: Int,
    onDirectionToggled: (Int) -> Unit,
    bottomReserve: Dp,
    applyBottomInset: Boolean,
) {
    val padding = ChefMateTheme.dimens.paddingNormal
    val contentPadding = bodyContentPadding(bottomReserve, applyBottomInset)
    val layoutDir = LocalLayoutDirection.current
    var ingredientsWeight by remember { mutableStateOf(0.5f) }

    // Each column scrolls behind the floating bottom bar: reserve the bottom inset as the columns'
    // scroll padding rather than shrinking the Row, so long content runs all the way down to the
    // bar
    // instead of being clipped short above it (matching StackedLayout).
    val columnContentPadding =
        PaddingValues(
            start = padding,
            end = padding,
            bottom = contentPadding.calculateBottomPadding(),
        )

    Row(
        modifier =
            Modifier.fillMaxSize()
                .padding(
                    start = contentPadding.calculateStartPadding(layoutDir),
                    end = contentPadding.calculateEndPadding(layoutDir),
                    top = contentPadding.calculateTopPadding(),
                )
    ) {
        LazyColumn(
            modifier = Modifier.weight(ingredientsWeight).fillMaxHeight(),
            contentPadding = columnContentPadding,
            verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingExtraSmall),
        ) {
            stickyHeader(key = "ingredients_header") {
                CookSectionHeader(stringResource(Res.string.cook_mode_ingredients))
            }
            itemsIndexed(ingredientLines, key = { i, _ -> "ingredient_$i" }) { index, line ->
                IngredientRow(
                    text = line,
                    scale = scale,
                    crossedOut = crossedOut[index],
                    onClick = { crossedOut[index] = !crossedOut[index] },
                )
            }
        }
        VerticalDragHandle(
            onDrag = { dx ->
                val deltaWeight = dx * 0.001f
                ingredientsWeight = (ingredientsWeight + deltaWeight).coerceIn(0.2f, 0.8f)
            }
        )
        LazyColumn(
            modifier = Modifier.weight(1f - ingredientsWeight).fillMaxHeight(),
            contentPadding = columnContentPadding,
            verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingExtraSmall),
        ) {
            stickyHeader(key = "directions_header") {
                CookSectionHeader(stringResource(Res.string.cook_mode_directions))
            }
            itemsIndexed(directionParagraphs, key = { i, _ -> "direction_$i" }) { index, paragraph
                ->
                DirectionRow(
                    text = paragraph,
                    highlighted = highlightedIndex == index,
                    onClick = { onDirectionToggled(index) },
                )
            }
        }
    }
}

@Composable
private fun VerticalDragHandle(onDrag: (Float) -> Unit) {
    Box(
        modifier =
            Modifier.width(16.dp).fillMaxHeight().pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier.width(4.dp)
                    .height(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.small,
                    )
        )
    }
}

@Composable
private fun CookSectionHeader(title: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
        )
        HorizontalDivider()
    }
}

@Composable
private fun IngredientRow(text: String, scale: Double, crossedOut: Boolean, onClick: () -> Unit) {
    if (IngredientSection.isHeader(text)) {
        // Sub-section header (e.g. "For the sauce:") — bold and not crossable.
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        top = ChefMateTheme.dimens.paddingSmall,
                        bottom = ChefMateTheme.dimens.paddingExtraSmall,
                    ),
        )
        return
    }
    // Scale the marker-stripped content so a leading bullet doesn't hide the amount.
    val rendered =
        remember(text, scale) {
            val parsed = parseListLine(text)
            parsed
                .copy(content = IngredientScaler.scale(parsed.content, scale))
                .toDisplayAnnotatedString()
        }
    Text(
        text = rendered,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
        textDecoration = if (crossedOut) TextDecoration.LineThrough else TextDecoration.None,
        color =
            if (crossedOut) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = ChefMateTheme.dimens.paddingExtraSmall),
    )
}

@Composable
private fun DirectionRow(text: String, highlighted: Boolean, onClick: () -> Unit) {
    val dimens = ChefMateTheme.dimens
    val rendered = remember(text) { parseListLine(text).toDisplayAnnotatedString() }
    Text(
        text = rendered,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .then(
                    if (highlighted) {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(dimens.paddingSmall),
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(
                    vertical = dimens.paddingExtraSmall,
                    horizontal = dimens.paddingExtraSmall,
                ),
    )
}

@Composable
private fun CookModeBottomBar(
    chips: List<CookModeBloc.Model.Chip>,
    onWhatsCookingClicked: () -> Unit,
    onChipClicked: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomAppBar(
        modifier = modifier.fillMaxWidth(),
        windowInsets =
            WindowInsets.systemBars
                .union(WindowInsets.displayCutout)
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    ) {
        IconButton(onClick = onWhatsCookingClicked) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = stringResource(Res.string.cook_mode_whats_cooking),
            )
        }
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 4.dp),
        ) {
            items(chips, key = { it.recipeId }) { chip ->
                FilterChip(
                    selected = chip.isActive,
                    onClick = { onChipClicked(chip.recipeId) },
                    label = { Text(chip.title, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(),
                )
            }
        }
    }
}

/**
 * Splits stored recipe text into displayable lines, dropping blanks and the stray `<br>` tags
 * recipes edited by an older build may still carry (see [withoutLineBreakTags]).
 */
private fun splitLines(text: String): List<String> =
    text.withoutLineBreakTags().split("\n").filter { it.isNotBlank() }

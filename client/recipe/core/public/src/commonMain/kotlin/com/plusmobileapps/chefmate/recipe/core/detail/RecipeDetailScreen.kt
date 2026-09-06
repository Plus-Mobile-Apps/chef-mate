@file:OptIn(
    ExperimentalTime::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
)

package com.plusmobileapps.chefmate.recipe.core.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chefmate.client.recipe.core.public.generated.resources.Res
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_add_favorite
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_add_to_grocery
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_add_to_grocery_onboarding
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_add_to_meal_plan
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_add_to_meal_plan_onboarding
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_ai_chat
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_ai_chat_premium
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_calories
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_cook_mode
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_cook_mode_onboarding
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_cook_time
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_copied_to_clipboard
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_created
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_delete
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_delete_cancel
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_delete_confirm
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_delete_message
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_delete_title
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_deleting_message
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_deleting_title
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_directions
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_edit
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_favorite_onboarding
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_ingredients
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_kcal
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_prep_time
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_remove_favorite
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_servings
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share_cancel
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share_confirm_body
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share_confirm_button
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share_confirm_title
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share_link
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share_text
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share_url
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_stop_sharing
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_total_time
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_updated
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.plusmobileapps.chefmate.ChefMateUrls
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.recipe.categories.pickerLabelRes
import com.plusmobileapps.chefmate.recipe.core.ingredients.IngredientScaleSelector
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.recipe.data.IngredientScaler
import com.plusmobileapps.chefmate.recipe.data.IngredientSection
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.subscription.ui.PremiumLockedIcon
import com.plusmobileapps.chefmate.subscription.ui.PremiumRequiredDialog
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.toast.LocalToastService
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.LocalAnimatedVisibilityScope
import com.plusmobileapps.chefmate.ui.LocalSecondaryAnimatedVisibilityScope
import com.plusmobileapps.chefmate.ui.LocalSharedTransitionScope
import com.plusmobileapps.chefmate.ui.PlusShareIcon
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.components.PlusOnboardingTooltip
import com.plusmobileapps.chefmate.ui.components.PlusResponsiveContainer
import com.plusmobileapps.chefmate.ui.components.PlusToolbar
import com.plusmobileapps.chefmate.ui.components.RecipeImage
import com.plusmobileapps.chefmate.ui.components.WindowSizeClass
import com.plusmobileapps.chefmate.ui.text.LineMarker
import com.plusmobileapps.chefmate.ui.text.ListLine
import com.plusmobileapps.chefmate.ui.text.parseListLine
import com.plusmobileapps.chefmate.ui.text.toDisplayAnnotatedString
import com.plusmobileapps.chefmate.ui.text.toInlineMarkdownAnnotatedString
import com.plusmobileapps.chefmate.ui.text.withoutLineBreakTags
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import com.plusmobileapps.chefmate.util.rememberShareLauncher
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(bloc: RecipeDetailBloc, modifier: Modifier = Modifier) {
    val state by bloc.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shareLauncher = rememberShareLauncher()
    val toastService = LocalToastService.current

    // The "added to grocery list" toast (with its View action) is fired from the BLoC through the
    // app-wide ToastService; the "copied to clipboard" toast below goes through the same service.

    // Hand each share-link URL emitted by the BLoC to the platform share sheet. On desktop the
    // launcher copies to the clipboard and returns true, so we show the same "copied" toast used by
    // the other share options.
    LaunchedEffect(bloc) {
        bloc.shareLink.collect { url ->
            if (shareLauncher(url)) {
                toastService.show(ResourceString(Res.string.recipe_detail_copied_to_clipboard))
            }
        }
    }

    // "Anyone with the link can view this" confirmation before a recipe is first made public.
    if (state.showShareConfirmation) {
        PlusDialog(
            title = ResourceString(Res.string.recipe_detail_share_confirm_title),
            message = ResourceString(Res.string.recipe_detail_share_confirm_body),
            confirmButtonText = ResourceString(Res.string.recipe_detail_share_confirm_button),
            dismissButtonText = ResourceString(Res.string.recipe_detail_share_cancel),
            onConfirmClick = bloc::onShareConfirmed,
            onDismissRequest = bloc::onShareDismissed,
        )
    }

    // Upsell shown when a non-subscriber taps the AI chat button.
    if (state.showPremiumRequiredDialog) {
        PremiumRequiredDialog(onDismiss = bloc::onPremiumRequiredDismissed)
    }

    // Delete confirmation dialog
    if (state.showDeleteConfirmationDialog) {
        DeleteConfirmationDialog(
            recipeName = state.recipe.title,
            onConfirm = bloc::onDeleteConfirmed,
            onDismiss = bloc::onDeleteDismissed,
        )
    }

    // Deleting progress dialog
    if (state.isDeleting) {
        DeletingDialog()
    }

    // Add to Grocery List Bottom Sheet
    RecipeDetailSheet(bloc = bloc, sheetState = sheetState)

    var showOverflowMenu by remember { mutableStateOf(false) }

    val fullImageSlot by bloc.fullImageSlot.subscribeAsState()
    val fullImageActive = fullImageSlot.child?.instance as? RecipeDetailBloc.FullImage.Active

    // Self-contained shared-element scope for the body image ↔ full-screen image morph. This is
    // local to this screen (not the root) so the root stack keeps its predictive-back animation.
    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
            AnimatedContent(
                targetState = fullImageActive,
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(250)) },
                label = "recipe-detail-full-image",
                modifier = Modifier.fillMaxSize(),
            ) { fullImage ->
                if (fullImage != null) {
                    // Full-screen image registers in this AnimatedContent's AVS — pairing with the
                    // body image's *secondary* registration (LocalSecondaryAnimatedVisibilityScope)
                    // for the morph.
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        RecipeImageFullScreenScreen(
                            imageUrl = fullImage.imageUrl,
                            recipeId = fullImage.recipeId,
                            contentDescription = fullImage.title,
                            onDismiss = bloc::onCloseFullImage,
                        )
                    }
                } else {
                    // Body image exposes this AnimatedContent's AVS as a *secondary* scope so it
                    // can
                    // morph into the full-screen image.
                    CompositionLocalProvider(LocalSecondaryAnimatedVisibilityScope provides this) {
                        RecipeDetailBody(
                            bloc = bloc,
                            state = state,
                            showOverflowMenu = showOverflowMenu,
                            onShowOverflowMenuChange = { showOverflowMenu = it },
                            shareLauncher = shareLauncher,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailBody(
    bloc: RecipeDetailBloc,
    state: RecipeDetailBloc.Model,
    showOverflowMenu: Boolean,
    onShowOverflowMenuChange: (Boolean) -> Unit,
    shareLauncher: (String) -> Boolean,
) {
    val toastService = LocalToastService.current
    val copiedMessage = ResourceString(Res.string.recipe_detail_copied_to_clipboard)
    // Links inside ingredients/directions: a chefmate://recipe/<clientId> link opens that recipe
    // in-app; any other URL is treated like the source link and opened externally.
    val onRecipeLinkClick: (String) -> Unit =
        remember(bloc) {
            { url ->
                ChefMateUrls.recipeLinkClientId(url)?.let(bloc::onRecipeLinkClicked)
                    ?: bloc.onSourceUrlClicked(url)
            }
        }

    // Scroll states are hoisted above [PlusResponsiveContainer] so they outlive the
    // compact <-> two-column layout swap that a rotation triggers when the window crosses the
    // 600dp breakpoint. If they lived inside each layout composable, rotating (or rotating away and
    // back) would dispose the active layout's LazyColumn and its position would reset to the top.
    // rememberLazyListState is itself rememberSaveable-backed, so this also restores across an
    // Android Activity recreation.
    val compactListState = rememberLazyListState()
    val ingredientsListState = rememberLazyListState()
    val directionsListState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize().testTag(RecipeDetailTestTags.SCREEN)) {
        PlusResponsiveContainer(modifier = Modifier.fillMaxSize()) { windowSizeClass ->
            val isCompact = windowSizeClass == WindowSizeClass.COMPACT
            PlusHeaderContainer(
                modifier = Modifier.fillMaxSize(),
                data =
                    PlusHeaderData.Child(
                        title = state.recipe.title.asTextData(),
                        onBackClick = bloc::onBackClicked,
                        trailingAccessory =
                            PlusHeaderData.TrailingAccessory.Custom {
                                RecipeDetailActions(
                                    recipe = state.recipe,
                                    // Compact collapses Edit/Delete into the three-dot overflow;
                                    // wider windows surface them directly. The Share button is
                                    // always its own dedicated control in both layouts.
                                    inlineActions = !isCompact,
                                    showOverflowMenu = showOverflowMenu,
                                    onShowOverflowMenuChange = onShowOverflowMenuChange,
                                    onEdit = bloc::onEditClicked,
                                    onDelete = bloc::onDeleteClicked,
                                    onShare = { content ->
                                        if (shareLauncher(content)) {
                                            toastService.show(copiedMessage)
                                        }
                                    },
                                    onShareLink = bloc::onShareLinkClicked,
                                    onStopSharing = bloc::onStopSharingClicked,
                                )
                            },
                    ),
                // Compact's app bar collapses as the inner LazyColumn scrolls, so the header and
                // content need to move as one continuous surface. A fixed arrangement gap here
                // would sit outside the scroll and stay put while the header shrinks underneath
                // it, leaving a seam that clips the first row of content — so compact carries its
                // breathing room as the list's own top content padding instead (see
                // RecipeDetailCompactContent) and this arrangement contributes none. The
                // two-column layout keeps a fixed header, so its gap can stay a static spacing.
                verticalArrangement =
                    if (isCompact) Arrangement.Top
                    else spacedBy(ChefMateTheme.dimens.paddingNormal),
                scrollEnabled = false,
                // Compact is a single linear scroll, so the title can grow large and collapse as
                // the
                // content scrolls. The two-column layout has two independent scrolls with no single
                // driver, so it keeps the standard fixed header.
                largeCollapsingTitle = isCompact,
                maxContentWidth = if (isCompact) 600.dp else Dp.Unspecified,
                floatingToolbar = {
                    PlusToolbar(
                        expanded = true,
                        floatingActionButton = {
                            RecipeDetailCoachMark(
                                id = CoachMarkId.RECIPE_DETAIL_COOK_MODE,
                                text = Res.string.recipe_detail_cook_mode_onboarding.asTextData(),
                                activeCoachMark = state.activeCoachMark,
                                isLoading = state.isLoading,
                                onDismiss = bloc::onCoachMarkDismissed,
                            ) {
                                FloatingActionButton(
                                    onClick = bloc::onCookModeClicked,
                                    shape = ChefMateTheme.shapes.large,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SoupKitchen,
                                        contentDescription =
                                            stringResource(Res.string.recipe_detail_cook_mode),
                                    )
                                }
                            }
                        },
                    ) {
                        RecipeDetailCoachMark(
                            id = CoachMarkId.RECIPE_DETAIL_ADD_TO_GROCERY,
                            text = Res.string.recipe_detail_add_to_grocery_onboarding.asTextData(),
                            activeCoachMark = state.activeCoachMark,
                            isLoading = state.isLoading,
                            onDismiss = bloc::onCoachMarkDismissed,
                        ) {
                            IconButton(
                                onClick = bloc::onAddToGroceryListClicked,
                                modifier =
                                    Modifier.testTag(RecipeDetailTestTags.ADD_TO_GROCERY_BUTTON),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription =
                                        stringResource(Res.string.recipe_detail_add_to_grocery),
                                )
                            }
                        }
                        RecipeDetailCoachMark(
                            id = CoachMarkId.RECIPE_DETAIL_FAVORITE,
                            text = Res.string.recipe_detail_favorite_onboarding.asTextData(),
                            activeCoachMark = state.activeCoachMark,
                            isLoading = state.isLoading,
                            onDismiss = bloc::onCoachMarkDismissed,
                        ) {
                            IconButton(onClick = { bloc.onFavoriteToggled() }) {
                                Icon(
                                    imageVector =
                                        if (state.recipe.isFavorite) {
                                            Icons.Default.Favorite
                                        } else {
                                            Icons.Default.FavoriteBorder
                                        },
                                    contentDescription =
                                        if (state.recipe.isFavorite) {
                                            stringResource(Res.string.recipe_detail_remove_favorite)
                                        } else {
                                            stringResource(Res.string.recipe_detail_add_favorite)
                                        },
                                )
                            }
                        }
                        RecipeDetailCoachMark(
                            id = CoachMarkId.RECIPE_DETAIL_ADD_TO_MEAL_PLAN,
                            text =
                                Res.string.recipe_detail_add_to_meal_plan_onboarding.asTextData(),
                            activeCoachMark = state.activeCoachMark,
                            isLoading = state.isLoading,
                            onDismiss = bloc::onCoachMarkDismissed,
                        ) {
                            IconButton(onClick = { bloc.onAddToMealPlanClicked() }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription =
                                        stringResource(Res.string.recipe_detail_add_to_meal_plan),
                                )
                            }
                        }
                        if (state.showAiChat) {
                            // Still tappable for a non-subscriber — the click opens the premium
                            // upsell instead of the chat, so this is dimmed rather than disabled.
                            IconButton(
                                onClick = bloc::onAiChatClicked,
                                modifier = Modifier.testTag(RecipeDetailTestTags.AI_CHAT_BUTTON),
                            ) {
                                if (state.isSubscribed) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription =
                                            stringResource(Res.string.recipe_detail_ai_chat),
                                    )
                                } else {
                                    PremiumLockedIcon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription =
                                            stringResource(
                                                Res.string.recipe_detail_ai_chat_premium
                                            ),
                                    )
                                }
                            }
                        }
                    }
                },
            ) {
                if (state.isLoading) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PlusLoadingIndicator()
                    }
                } else if (isCompact) {
                    RecipeDetailCompactContent(
                        recipe = state.recipe,
                        createdAt = state.createdAt,
                        updatedAt = state.updatedAt,
                        formattedPrepTime = state.formattedPrepTime,
                        formattedCookTime = state.formattedCookTime,
                        formattedTotalTime = state.formattedTotalTime,
                        onSourceUrlClicked = bloc::onSourceUrlClicked,
                        onImageClicked = bloc::onImageClicked,
                        onRecipeLinkClick = onRecipeLinkClick,
                        scale = state.ingredientScale,
                        onScaleChange = bloc::onScaleChanged,
                        listState = compactListState,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    RecipeDetailTwoColumnContent(
                        recipe = state.recipe,
                        createdAt = state.createdAt,
                        updatedAt = state.updatedAt,
                        formattedPrepTime = state.formattedPrepTime,
                        formattedCookTime = state.formattedCookTime,
                        formattedTotalTime = state.formattedTotalTime,
                        onSourceUrlClicked = bloc::onSourceUrlClicked,
                        onImageClicked = bloc::onImageClicked,
                        onRecipeLinkClick = onRecipeLinkClick,
                        scale = state.ingredientScale,
                        onScaleChange = bloc::onScaleChanged,
                        ingredientsListState = ingredientsListState,
                        directionsListState = directionsListState,
                    )
                }
            }
        }
    }
}

/**
 * Recipe-detail top-bar actions. On compact widths everything collapses into a single overflow
 * (three-dot) menu; on wider windows ([inlineActions]) the otherwise-empty app-bar space is used to
 * surface Edit / Share / Delete directly, with Share opening its own small menu to pick between the
 * source URL and the full recipe text.
 */
@Composable
private fun RecipeDetailActions(
    recipe: Recipe,
    inlineActions: Boolean,
    showOverflowMenu: Boolean,
    onShowOverflowMenuChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: (String) -> Unit,
    onShareLink: () -> Unit,
    onStopSharing: () -> Unit,
) {
    // A single Share button owns every share option in both layouts. Edit/Delete are surfaced
    // inline on wide windows and collapsed into a three-dot overflow on compact ones.
    if (inlineActions) {
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(Res.string.recipe_detail_edit),
            )
        }
        ShareActionButton(
            recipe = recipe,
            onShare = onShare,
            onShareLink = onShareLink,
            onStopSharing = onStopSharing,
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(Res.string.recipe_detail_delete),
            )
        }
    } else {
        ShareActionButton(
            recipe = recipe,
            onShare = onShare,
            onShareLink = onShareLink,
            onStopSharing = onStopSharing,
        )
        EditDeleteOverflow(
            expanded = showOverflowMenu,
            onExpandedChange = onShowOverflowMenuChange,
            onEdit = onEdit,
            onDelete = onDelete,
        )
    }
}

/**
 * The single Share affordance: an icon button whose dropdown offers every share option — a Chef
 * Mate link, the source URL (when present), the full recipe text, and, once shared, stop-sharing.
 */
@Composable
private fun ShareActionButton(
    recipe: Recipe,
    onShare: (String) -> Unit,
    onShareLink: () -> Unit,
    onStopSharing: () -> Unit,
) {
    Box {
        var showShareMenu by remember { mutableStateOf(false) }
        IconButton(onClick = { showShareMenu = true }) {
            Icon(
                imageVector = PlusShareIcon,
                contentDescription = stringResource(Res.string.recipe_detail_share),
            )
        }
        DropdownMenu(expanded = showShareMenu, onDismissRequest = { showShareMenu = false }) {
            RecipeShareMenuItems(
                recipe = recipe,
                onClose = { showShareMenu = false },
                onShare = onShare,
                onShareLink = onShareLink,
                onStopSharing = onStopSharing,
            )
        }
    }
}

/** Compact layout: Edit and Delete collapsed behind a single three-dot overflow menu. */
@Composable
private fun EditDeleteOverflow(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(true) }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.recipe_detail_edit),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_detail_edit)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_detail_delete)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    onExpandedChange(false)
                    onDelete()
                },
            )
        }
    }
}

/**
 * The share entries in the Share button's dropdown: a Chef Mate recipe link (always), the source
 * URL (only when present), the full recipe text, and — once the recipe is public — a stop-sharing
 * entry. The link and stop-sharing actions route through the BLoC (they toggle public visibility);
 * the URL and text actions share content directly via [onShare].
 */
@Composable
private fun RecipeShareMenuItems(
    recipe: Recipe,
    onClose: () -> Unit,
    onShare: (String) -> Unit,
    onShareLink: () -> Unit,
    onStopSharing: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(Res.string.recipe_detail_share_link)) },
        leadingIcon = { Icon(PlusShareIcon, contentDescription = null) },
        onClick = {
            onClose()
            onShareLink()
        },
    )
    recipe.sourceUrl?.let { url ->
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.recipe_detail_share_url)) },
            leadingIcon = { Icon(PlusShareIcon, contentDescription = null) },
            onClick = {
                onClose()
                onShare(url)
            },
        )
    }
    DropdownMenuItem(
        text = { Text(stringResource(Res.string.recipe_detail_share_text)) },
        leadingIcon = { Icon(PlusShareIcon, contentDescription = null) },
        onClick = {
            onClose()
            onShare(formatRecipeAsText(recipe))
        },
    )
    if (recipe.isPublic) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.recipe_detail_stop_sharing)) },
            leadingIcon = { Icon(Icons.Default.LinkOff, contentDescription = null) },
            onClick = {
                onClose()
                onStopSharing()
            },
        )
    }
}

/**
 * A recipe-detail toolbar coach mark: shows its tooltip above [anchor] only while [id] is the
 * active coach mark (and the recipe has loaded), and reports dismissal back by id.
 */
@Composable
private fun RecipeDetailCoachMark(
    id: String,
    text: TextData,
    activeCoachMark: String?,
    isLoading: Boolean,
    onDismiss: (String) -> Unit,
    anchor: @Composable () -> Unit,
) {
    PlusOnboardingTooltip(
        text = text,
        visible = !isLoading && activeCoachMark == id,
        onDismiss = { onDismiss(id) },
        anchor = anchor,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeDetailSheet(
    bloc: RecipeDetailBloc,
    sheetState: androidx.compose.material3.SheetState,
) {
    val slot = bloc.childSlot.subscribeAsState()
    val child = slot.value.child?.instance

    // Remember the last active child so the sheet stays in composition during dismiss animation
    var sheetChild by remember { mutableStateOf(child) }
    if (child != null) {
        sheetChild = child
    }

    // When the bloc dismisses programmatically, animate the sheet hide before removing it
    LaunchedEffect(child) {
        if (child == null && sheetChild != null) {
            sheetState.hide()
            sheetChild = null
        }
    }

    if (sheetChild != null) {
        ModalBottomSheet(
            onDismissRequest = {
                when (val current = sheetChild) {
                    is RecipeDetailBloc.Sheet.AddToGroceryList -> current.bloc.onBackClicked()
                    null -> {}
                }
            },
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
            sheetChild?.bloc?.Content()
        }
    }
}

/**
 * Compact layout: one linear scroll. Metadata scrolls away at the top, then ingredients and
 * directions read straight down the page under sticky section headers — no tabs, no pager, so
 * there's only ever one scroll position to keep track of while cooking.
 */
@Composable
private fun RecipeDetailCompactContent(
    recipe: Recipe,
    createdAt: TextData,
    updatedAt: TextData,
    formattedPrepTime: TextData?,
    formattedCookTime: TextData?,
    formattedTotalTime: TextData?,
    onSourceUrlClicked: (String) -> Unit,
    onImageClicked: () -> Unit,
    onRecipeLinkClick: (String) -> Unit,
    scale: Double,
    onScaleChange: (Double) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    val dimens = ChefMateTheme.dimens
    val padding = dimens.paddingNormal

    val ingredientLines = remember(recipe.ingredients) { splitLines(recipe.ingredients) }
    val crossedOut =
        remember(recipe.ingredients) {
            mutableStateListOf(*BooleanArray(ingredientLines.size) { false }.toTypedArray())
        }
    val directionParagraphs = remember(recipe.directions) { directionSteps(recipe.directions) }
    var highlightedDirectionIndex by remember(recipe.directions) { mutableStateOf(-1) }

    val navBarBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = spacedBy(dimens.paddingSmall),
        // The top inset lives here instead of as a fixed gap above the LazyColumn (see
        // RecipeDetailScreen's PlusHeaderContainer call) so it scrolls away with the rest of the
        // content as the collapsing app bar shrinks, instead of sitting still as a seam beneath it.
        contentPadding =
            PaddingValues(top = dimens.paddingNormal, bottom = dimens.fabClearance + navBarBottom),
    ) {
        // Hero section: image + key details side by side
        item(key = "hero") {
            RecipeHeroSection(
                recipe = recipe,
                createdAt = createdAt,
                updatedAt = updatedAt,
                formattedPrepTime = formattedPrepTime,
                formattedCookTime = formattedCookTime,
                formattedTotalTime = formattedTotalTime,
                onSourceUrlClicked = onSourceUrlClicked,
                onImageClicked = onImageClicked,
                modifier = Modifier.padding(horizontal = padding),
            )
        }

        if (recipe.categories.isNotEmpty()) {
            item(key = "categories") {
                RecipeCategoriesRow(
                    categories = recipe.categories,
                    modifier = Modifier.padding(horizontal = padding),
                )
            }
        }

        // Description below hero
        recipe.description?.let { description ->
            item(key = "description") {
                Text(
                    text = description.toInlineMarkdownAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = padding),
                )
            }
        }

        ingredientItems(
            lines = ingredientLines,
            crossedOut = crossedOut,
            scale = scale,
            onScaleChange = onScaleChange,
            onRecipeLinkClick = onRecipeLinkClick,
            itemPadding = padding,
        )

        directionItems(
            steps = directionParagraphs,
            highlightedIndex = highlightedDirectionIndex,
            onHighlightedIndexChanged = { highlightedDirectionIndex = it },
            onRecipeLinkClick = onRecipeLinkClick,
            itemPadding = padding,
        )
    }
}

/**
 * Tablet / desktop layout: the same linear reading order as compact, split across two independently
 * scrolling columns — metadata and ingredients on the left, directions on the right — so the steps
 * stay in view while scrolling the ingredient list. The divider between them is draggable to
 * rebalance the split.
 */
@Composable
private fun ColumnScope.RecipeDetailTwoColumnContent(
    recipe: Recipe,
    createdAt: TextData,
    updatedAt: TextData,
    formattedPrepTime: TextData?,
    formattedCookTime: TextData?,
    formattedTotalTime: TextData?,
    onSourceUrlClicked: (String) -> Unit,
    onImageClicked: () -> Unit,
    onRecipeLinkClick: (String) -> Unit,
    scale: Double,
    onScaleChange: (Double) -> Unit,
    ingredientsListState: LazyListState = rememberLazyListState(),
    directionsListState: LazyListState = rememberLazyListState(),
) {
    val dimens = ChefMateTheme.dimens
    val padding = dimens.paddingNormal
    val navBarBottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    val toolbarClearance = dimens.fabClearance + navBarBottom

    // Ratio for the ingredients column vs the directions column (0.0 = all directions).
    var ingredientsWeight by remember { mutableStateOf(0.5f) }

    val ingredientLines = remember(recipe.ingredients) { splitLines(recipe.ingredients) }
    val ingredientCrossedOut =
        remember(recipe.ingredients) {
            mutableStateListOf(*BooleanArray(ingredientLines.size) { false }.toTypedArray())
        }
    val directionParagraphs = remember(recipe.directions) { directionSteps(recipe.directions) }
    var directionHighlightedIndex by remember(recipe.directions) { mutableStateOf(-1) }

    Row(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = padding)) {
        // Column 1: metadata, then ingredients
        LazyColumn(
            modifier = Modifier.weight(ingredientsWeight),
            state = ingredientsListState,
            verticalArrangement = spacedBy(dimens.paddingSmall),
        ) {
            item(key = "hero") {
                RecipeHeroSection(
                    recipe = recipe,
                    createdAt = createdAt,
                    updatedAt = updatedAt,
                    formattedPrepTime = formattedPrepTime,
                    formattedCookTime = formattedCookTime,
                    formattedTotalTime = formattedTotalTime,
                    onSourceUrlClicked = onSourceUrlClicked,
                    onImageClicked = onImageClicked,
                    modifier = Modifier.padding(horizontal = padding),
                )
            }

            if (recipe.categories.isNotEmpty()) {
                item(key = "categories") {
                    RecipeCategoriesRow(
                        categories = recipe.categories,
                        modifier = Modifier.padding(horizontal = padding),
                    )
                }
            }

            recipe.description?.let { description ->
                item(key = "description") {
                    Text(
                        text = description.toInlineMarkdownAnnotatedString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = padding),
                    )
                }
            }

            ingredientItems(
                lines = ingredientLines,
                crossedOut = ingredientCrossedOut,
                scale = scale,
                onScaleChange = onScaleChange,
                onRecipeLinkClick = onRecipeLinkClick,
                itemPadding = padding,
            )

            item(key = "ingredients_spacer") {
                Spacer(modifier = Modifier.height(toolbarClearance))
            }
        }

        DragHandle(
            onDrag = { dragAmountX ->
                val delta = dragAmountX * 0.001f
                ingredientsWeight = (ingredientsWeight + delta).coerceIn(0.2f, 0.8f)
            }
        )

        // Column 2: directions
        LazyColumn(
            modifier = Modifier.weight(1f - ingredientsWeight),
            state = directionsListState,
            verticalArrangement = spacedBy(dimens.paddingSmall),
        ) {
            directionItems(
                steps = directionParagraphs,
                highlightedIndex = directionHighlightedIndex,
                onHighlightedIndexChanged = { directionHighlightedIndex = it },
                onRecipeLinkClick = onRecipeLinkClick,
                itemPadding = padding,
            )

            item(key = "directions_spacer") {
                Spacer(modifier = Modifier.height(toolbarClearance))
            }
        }
    }
}

/**
 * The ingredients section — sticky header plus one tappable (crossable) line per ingredient — as
 * `LazyListScope` items so both layouts share the same rendering and the section participates in
 * whichever scroll it's placed in.
 */
private fun LazyListScope.ingredientItems(
    lines: List<String>,
    crossedOut: SnapshotStateList<Boolean>,
    scale: Double,
    onScaleChange: (Double) -> Unit,
    onRecipeLinkClick: (String) -> Unit,
    itemPadding: Dp,
) {
    stickyHeader(key = "ingredients_header") {
        IngredientsStickyHeader(
            title = stringResource(Res.string.recipe_detail_ingredients),
            scale = scale,
            onScaleChange = onScaleChange,
        )
    }
    itemsIndexed(lines, key = { index, _ -> "ingredient_$index" }) { index, line ->
        IngredientLineItem(
            text = line,
            scale = scale,
            crossedOut = crossedOut[index],
            onClick = { crossedOut[index] = !crossedOut[index] },
            onRecipeLinkClick = onRecipeLinkClick,
            modifier = Modifier.padding(horizontal = itemPadding),
        )
    }
}

/**
 * The Ingredients section header: the title on the left and a scale dropdown on the right that
 * multiplies every ingredient amount by the chosen factor (½× through 4×). Matches
 * [StickyColumnHeader]'s surface so it reads as the same sticky bar the Directions section uses.
 */
@Composable
private fun IngredientsStickyHeader(
    title: String,
    scale: Double,
    onScaleChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).padding(vertical = 16.dp),
            )
            IngredientScaleSelector(
                scale = scale,
                onScaleChange = onScaleChange,
                buttonTestTag = RecipeDetailTestTags.INGREDIENT_SCALE_BUTTON,
            )
        }
    }
}

/** The directions section, as `LazyListScope` items. See [ingredientItems]. */
private fun LazyListScope.directionItems(
    steps: List<DirectionStep>,
    highlightedIndex: Int,
    onHighlightedIndexChanged: (Int) -> Unit,
    onRecipeLinkClick: (String) -> Unit,
    itemPadding: Dp,
) {
    stickyHeader(key = "directions_header") {
        StickyColumnHeader(title = stringResource(Res.string.recipe_detail_directions))
    }
    itemsIndexed(steps, key = { index, _ -> "direction_$index" }) { index, step ->
        DirectionLineItem(
            content = step.content,
            marker = step.marker,
            isHeader = step.isHeader,
            highlighted = highlightedIndex == index,
            onClick = { onHighlightedIndexChanged(if (highlightedIndex == index) -1 else index) },
            onRecipeLinkClick = onRecipeLinkClick,
            modifier = Modifier.padding(horizontal = itemPadding),
        )
    }
}

@Composable
private fun DragHandle(onDrag: (Float) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier.width(16.dp).fillMaxHeight().pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Vertical pill indicator
        Box(
            modifier =
                Modifier.width(4.dp)
                    .height(32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.small,
                    )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCategoriesRow(categories: Set<Category>, modifier: Modifier = Modifier) {
    if (categories.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            val label =
                BuiltinCategory.fromId(category.builtinId)?.let {
                    stringResource(it.pickerLabelRes())
                } ?: category.name
            CategoryChip(label = label)
        }
    }
}

@Composable
private fun CategoryChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun StarRating(rating: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            Icon(
                imageVector = if (index < rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint =
                    if (index < rating) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    },
            )
        }
    }
}

/** Compact hero: image on the left, key details stacked on the right. */
@Composable
private fun RecipeHeroSection(
    recipe: Recipe,
    createdAt: TextData,
    updatedAt: TextData,
    formattedPrepTime: TextData?,
    formattedCookTime: TextData?,
    formattedTotalTime: TextData?,
    onSourceUrlClicked: (String) -> Unit,
    onImageClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RecipeImage(
            imageUrl = recipe.imageUrl,
            contentDescription = recipe.title,
            modifier = Modifier.width(140.dp).height(140.dp),
            secondarySharedElementKey = "recipe-image-fullscreen-${recipe.id}",
            onClick = onImageClicked,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Star Rating
            recipe.starRating?.let { rating -> StarRating(rating = rating) }

            // Key details inline
            recipe.servings?.let { servings ->
                DetailRow(
                    icon = Icons.Default.Restaurant,
                    label = stringResource(Res.string.recipe_detail_servings),
                    value = "$servings",
                )
            }
            formattedPrepTime?.let {
                DetailRow(
                    icon = Icons.Default.Timer,
                    label = stringResource(Res.string.recipe_detail_prep_time),
                    value = it.localized(),
                )
            }
            formattedCookTime?.let {
                DetailRow(
                    icon = Icons.Default.Timer,
                    label = stringResource(Res.string.recipe_detail_cook_time),
                    value = it.localized(),
                )
            }
            formattedTotalTime?.let {
                DetailRow(
                    icon = Icons.Default.Timer,
                    label = stringResource(Res.string.recipe_detail_total_time),
                    value = it.localized(),
                )
            }
            recipe.calories?.let { calories ->
                DetailRow(
                    icon = Icons.Default.LocalFireDepartment,
                    label = stringResource(Res.string.recipe_detail_calories),
                    value =
                        PhraseModel(
                                Res.string.recipe_detail_kcal,
                                "calories" to FixedString(calories.toString()),
                            )
                            .localized(),
                )
            }

            // Source URL
            recipe.sourceUrl?.let { sourceUrl ->
                Text(
                    text = sourceUrl,
                    modifier = Modifier.clickable { onSourceUrlClicked(sourceUrl) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }

            // Timestamps inline
            Text(
                text =
                    stringResource(Res.string.recipe_detail_created) + " " + createdAt.localized(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    stringResource(Res.string.recipe_detail_updated) + " " + updatedAt.localized(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Splits stored recipe text into displayable lines, dropping blanks and the stray `<br>` tags
 * recipes edited by an older build may still carry (see [withoutLineBreakTags]).
 */
private fun splitLines(text: String): List<String> =
    text.withoutLineBreakTags().split("\n").filter { it.isNotBlank() }

private fun formatRecipeAsText(recipe: Recipe): String = buildString {
    appendLine(recipe.title)
    recipe.description?.let {
        appendLine()
        appendLine(it)
    }
    appendLine()
    appendLine("Ingredients:")
    appendLine(recipe.ingredients)
    appendLine()
    appendLine("Directions:")
    appendLine(recipe.directions)
    recipe.sourceUrl?.let {
        appendLine()
        append("Source: $it")
    }
}

/** The span style applied to recipe-to-recipe links inside ingredients/directions. */
@Composable
private fun recipeLinkStyle(): SpanStyle =
    SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
    )

@Composable
private fun IngredientLineItem(
    text: String,
    scale: Double,
    crossedOut: Boolean,
    onClick: () -> Unit,
    onRecipeLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (IngredientSection.isHeader(text)) {
        // Sub-section header (e.g. "For the sauce:") — bold and not crossable.
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(
                        top = ChefMateTheme.dimens.paddingSmall,
                        bottom = ChefMateTheme.dimens.paddingExtraSmall,
                    ),
        )
        return
    }
    val linkStyle = recipeLinkStyle()
    val rendered =
        remember(text, scale, linkStyle, onRecipeLinkClick) {
            // Scale the marker-stripped content so a leading "- "/"* " bullet doesn't hide the
            // amount from the scaler; the bullet is re-attached when the line is rendered.
            val parsed = parseListLine(text)
            val scaled = ListLine(parsed.marker, IngredientScaler.scale(parsed.content, scale))
            scaled.toDisplayAnnotatedString(linkStyle, onRecipeLinkClick)
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
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = ChefMateTheme.dimens.paddingExtraSmall),
    )
}

/**
 * One rendered directions line. When [isHeader] is set (a line ending in `:`) it renders bold and
 * is not tappable. Otherwise it's a step whose [marker] adds a bold `•`/`N.` prefix when the author
 * typed a bullet or enumerator — plain steps get no prefix.
 */
@Composable
private fun DirectionLineItem(
    content: String,
    marker: LineMarker,
    isHeader: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    onRecipeLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isHeader) {
        Text(
            text = content,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(
                        top = ChefMateTheme.dimens.paddingSmall,
                        bottom = ChefMateTheme.dimens.paddingExtraSmall,
                    ),
        )
        return
    }
    val dimens = ChefMateTheme.dimens
    val linkStyle = recipeLinkStyle()
    val rendered =
        remember(content, marker, linkStyle, onRecipeLinkClick) {
            ListLine(marker, content).toDisplayAnnotatedString(linkStyle, onRecipeLinkClick)
        }
    Text(
        text = rendered,
        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
        modifier =
            modifier
                .fillMaxWidth()
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

/**
 * A directions line with its display info. Section headers (lines ending in `:`) set [isHeader] and
 * render bold. Otherwise it's a step: [marker] carries any list prefix the author actually typed —
 * a `- ` bullet or a `1. ` enumerator — stripped from [content]. Numbers are never auto-generated,
 * so a plain step has a [LineMarker.None] marker and renders as a plain line, the same "only show
 * numbers when present" behavior as Cook Mode.
 */
internal data class DirectionStep(
    val content: String,
    val marker: LineMarker,
    val isHeader: Boolean,
)

internal fun directionSteps(directions: String): List<DirectionStep> =
    splitLines(directions).map { line ->
        if (IngredientSection.isHeader(line)) {
            DirectionStep(content = line, marker = LineMarker.None, isHeader = true)
        } else {
            val parsed = parseListLine(line)
            DirectionStep(content = parsed.content, marker = parsed.marker, isHeader = false)
        }
    }

@Composable
private fun StickyColumnHeader(title: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    recipeName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.recipe_detail_delete_title)) },
        text = {
            Text(
                PhraseModel(
                        Res.string.recipe_detail_delete_message,
                        "recipe_name" to FixedString(recipeName),
                    )
                    .localized()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.recipe_detail_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.recipe_detail_delete_cancel))
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun DeletingDialog(modifier: Modifier = Modifier) {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal while deleting */ },
        title = { Text(stringResource(Res.string.recipe_detail_deleting_title)) },
        text = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlusLoadingIndicator()
                Text(stringResource(Res.string.recipe_detail_deleting_message))
            }
        },
        confirmButton = {},
        modifier = modifier,
    )
}

val previewRecipeDetailBloc: RecipeDetailBloc =
    object : RecipeDetailBloc {
        override val state: StateFlow<RecipeDetailBloc.Model> =
            MutableStateFlow(
                RecipeDetailBloc.Model(
                    isLoading = false,
                    recipe =
                        Recipe(
                            id = 1L,
                            title = "Spaghetti Bolognese",
                            description = "A classic Italian pasta dish.",
                            imageUrl = null,
                            starRating = 4,
                            servings = 4,
                            prepTime = 15,
                            cookTime = 60,
                            totalTime = 75,
                            calories = 600,
                            sourceUrl = "https://example.com/spaghetti-bolognese",
                            ingredients =
                                """
                                - 400g **spaghetti**
                                - 2 tbsp olive oil
                                - 1 onion, _finely chopped_
                                - 2 garlic cloves, crushed
                                - 400g minced beef
                                - 800g canned tomatoes
                                - A batch of [Garlic Bread](chefmate://recipe/garlic-bread-id)
                                - Salt and **pepper** to taste
                                """
                                    .trimIndent(),
                            directions =
                                """
                                1. Cook the spaghetti until _al dente_.
                                2. Heat the olive oil in a pan and sauté the onion and garlic until soft.
                                3. Add the minced beef and cook until **browned**.
                                4. Stir in the canned tomatoes and simmer for 45 minutes.
                                5. Season with salt and pepper.
                                6. Serve the sauce over the cooked spaghetti.
                                """
                                    .trimIndent(),
                            categories =
                                setOf(
                                    Category(
                                        id = 1L,
                                        name = "Dinner",
                                        builtinId = BuiltinCategory.DINNER.id,
                                    ),
                                    Category(id = 100L, name = "Weeknight"),
                                ),
                            createdAt = Instant.parse("2023-01-01T12:00:00Z"),
                            updatedAt = Instant.parse("2023-02-01T12:00:00Z"),
                        ),
                    createdAt = FixedString("January 1, 2023"),
                    updatedAt = FixedString("February 1, 2023"),
                    isDeleting = false,
                    showDeleteConfirmationDialog = false,
                )
            )
        override val shareLink = emptyFlow<String>()

        override val childSlot: Value<ChildSlot<*, RecipeDetailBloc.Sheet>> =
            MutableValue(ChildSlot<Any, RecipeDetailBloc.Sheet>(null))

        override val fullImageSlot: Value<ChildSlot<*, RecipeDetailBloc.FullImage>> =
            MutableValue(ChildSlot<Any, RecipeDetailBloc.FullImage>(null))

        override fun onImageClicked() {
            // Preview no-op
        }

        override fun onCloseFullImage() {
            // Preview no-op
        }

        override fun onEditClicked() {
            TODO("Not yet implemented")
        }

        override fun onDeleteClicked() {
            TODO("Not yet implemented")
        }

        override fun onDeleteConfirmed() {
            TODO("Not yet implemented")
        }

        override fun onDeleteDismissed() {
            TODO("Not yet implemented")
        }

        override fun onFavoriteToggled() {
            TODO("Not yet implemented")
        }

        override fun onAddToGroceryListClicked() {
            TODO("Not yet implemented")
        }

        override fun onAddToMealPlanClicked() {
            TODO("Not yet implemented")
        }

        override fun onCookModeClicked() {
            TODO("Not yet implemented")
        }

        override fun onAiChatClicked() {
            TODO("Not yet implemented")
        }

        override fun onPremiumRequiredDismissed() {
            // Preview no-op
        }

        override fun onCoachMarkDismissed(id: String) {
            TODO("Not yet implemented")
        }

        override fun onSourceUrlClicked(url: String) {
            TODO("Not yet implemented")
        }

        override fun onRecipeLinkClicked(clientId: String) {
            // Preview no-op
        }

        override fun onShareLinkClicked() {
            // Preview no-op
        }

        override fun onShareConfirmed() {
            // Preview no-op
        }

        override fun onShareDismissed() {
            // Preview no-op
        }

        override fun onStopSharingClicked() {
            // Preview no-op
        }

        override fun onScaleChanged(scale: Double) {
            // Preview no-op
        }

        override fun onDismissSheet() {
            TODO("Not yet implemented")
        }

        override fun onBackClicked() {
            TODO("Not yet implemented")
        }

        @Composable override fun Content(modifier: Modifier) = RecipeDetailScreen(this, modifier)
    }

/** Same as [previewRecipeDetailBloc] but with the AI Chat toolbar button surfaced, unlocked. */
val previewRecipeDetailBlocWithAiChat: RecipeDetailBloc =
    object : RecipeDetailBloc by previewRecipeDetailBloc {
        override val state: StateFlow<RecipeDetailBloc.Model> =
            MutableStateFlow(
                previewRecipeDetailBloc.state.value.copy(showAiChat = true, isSubscribed = true)
            )

        @Composable override fun Content(modifier: Modifier) = RecipeDetailScreen(this, modifier)
    }

/** AI Chat surfaced for a non-subscriber: the button is dimmed and opens the upsell. */
val previewRecipeDetailBlocWithLockedAiChat: RecipeDetailBloc =
    object : RecipeDetailBloc by previewRecipeDetailBloc {
        override val state: StateFlow<RecipeDetailBloc.Model> =
            MutableStateFlow(
                previewRecipeDetailBloc.state.value.copy(showAiChat = true, isSubscribed = false)
            )

        @Composable override fun Content(modifier: Modifier) = RecipeDetailScreen(this, modifier)
    }

@Preview(heightDp = 1100)
@Composable
private fun RecipeDetailContentPreview() {
    ChefMateTheme { RecipeDetailScreen(bloc = previewRecipeDetailBloc) }
}

@Preview(heightDp = 1100)
@Composable
private fun RecipeDetailContentDarkPreview() {
    ChefMateTheme(darkTheme = true) { RecipeDetailScreen(bloc = previewRecipeDetailBloc) }
}

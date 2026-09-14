package com.plusmobileapps.chefmate.recipe.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chefmate.client.recipe.list.public.generated.resources.Res
import chefmate.client.recipe.list.public.generated.resources.recipe_list_add_onboarding
import chefmate.client.recipe.list.public.generated.resources.recipe_list_add_recipe
import chefmate.client.recipe.list.public.generated.resources.recipe_list_apply
import chefmate.client.recipe.list.public.generated.resources.recipe_list_book_all_recipes
import chefmate.client.recipe.list.public.generated.resources.recipe_list_book_selector
import chefmate.client.recipe.list.public.generated.resources.recipe_list_bulk_book_title
import chefmate.client.recipe.list.public.generated.resources.recipe_list_bulk_category_title
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_ai
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_appetizer
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_breakfast
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_dessert
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_dinner
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_drink
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_lunch
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_other
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_side
import chefmate.client.recipe.list.public.generated.resources.recipe_list_category_snack
import chefmate.client.recipe.list.public.generated.resources.recipe_list_clear_filters
import chefmate.client.recipe.list.public.generated.resources.recipe_list_continue_cooking
import chefmate.client.recipe.list.public.generated.resources.recipe_list_create_recipe
import chefmate.client.recipe.list.public.generated.resources.recipe_list_done_cooking
import chefmate.client.recipe.list.public.generated.resources.recipe_list_done_cooking_cancel
import chefmate.client.recipe.list.public.generated.resources.recipe_list_done_cooking_confirm
import chefmate.client.recipe.list.public.generated.resources.recipe_list_done_cooking_message
import chefmate.client.recipe.list.public.generated.resources.recipe_list_done_cooking_title
import chefmate.client.recipe.list.public.generated.resources.recipe_list_empty_browse
import chefmate.client.recipe.list.public.generated.resources.recipe_list_empty_create
import chefmate.client.recipe.list.public.generated.resources.recipe_list_empty_description
import chefmate.client.recipe.list.public.generated.resources.recipe_list_empty_title
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_by
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_by_category
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_empty_description
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_empty_title
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_favorites
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_onboarding
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_quick_recipes
import chefmate.client.recipe.list.public.generated.resources.recipe_list_filter_rated
import chefmate.client.recipe.list.public.generated.resources.recipe_list_invite_accept
import chefmate.client.recipe.list.public.generated.resources.recipe_list_invite_decline
import chefmate.client.recipe.list.public.generated.resources.recipe_list_invite_message
import chefmate.client.recipe.list.public.generated.resources.recipe_list_item_calories
import chefmate.client.recipe.list.public.generated.resources.recipe_list_item_servings
import chefmate.client.recipe.list.public.generated.resources.recipe_list_menu_collaborate
import chefmate.client.recipe.list.public.generated.resources.recipe_list_menu_export_all
import chefmate.client.recipe.list.public.generated.resources.recipe_list_menu_select
import chefmate.client.recipe.list.public.generated.resources.recipe_list_menu_sync
import chefmate.client.recipe.list.public.generated.resources.recipe_list_more_actions
import chefmate.client.recipe.list.public.generated.resources.recipe_list_open_in_new_window
import chefmate.client.recipe.list.public.generated.resources.recipe_list_scan_failed_title
import chefmate.client.recipe.list.public.generated.resources.recipe_list_scan_from_photo
import chefmate.client.recipe.list.public.generated.resources.recipe_list_scanning_message
import chefmate.client.recipe.list.public.generated.resources.recipe_list_scanning_title
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_all_recipes
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_clear
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_empty
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_empty_book_hint
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_placeholder
import chefmate.client.recipe.list.public.generated.resources.recipe_list_selection_add_to_book
import chefmate.client.recipe.list.public.generated.resources.recipe_list_selection_add_to_category
import chefmate.client.recipe.list.public.generated.resources.recipe_list_selection_count
import chefmate.client.recipe.list.public.generated.resources.recipe_list_selection_deselect_all
import chefmate.client.recipe.list.public.generated.resources.recipe_list_selection_exit
import chefmate.client.recipe.list.public.generated.resources.recipe_list_selection_export
import chefmate.client.recipe.list.public.generated.resources.recipe_list_selection_more
import chefmate.client.recipe.list.public.generated.resources.recipe_list_selection_select_all
import chefmate.client.recipe.list.public.generated.resources.recipe_list_sort
import chefmate.client.recipe.list.public.generated.resources.recipe_list_sort_a_to_z
import chefmate.client.recipe.list.public.generated.resources.recipe_list_sort_and_filter
import chefmate.client.recipe.list.public.generated.resources.recipe_list_sort_by
import chefmate.client.recipe.list.public.generated.resources.recipe_list_sort_oldest_first
import chefmate.client.recipe.list.public.generated.resources.recipe_list_sort_recently_added
import chefmate.client.recipe.list.public.generated.resources.recipe_list_sort_top_rated
import chefmate.client.recipe.list.public.generated.resources.recipe_list_sort_z_to_a
import chefmate.client.recipe.list.public.generated.resources.recipe_list_title
import chefmate.client.recipe.list.public.generated.resources.recipe_list_view_grid
import chefmate.client.recipe.list.public.generated.resources.recipe_list_view_list
import chefmate.client.recipe.list.public.generated.resources.recipe_list_view_mode_onboarding
import chefmate.client.recipe.list.public.generated.resources.recipe_sync_not_synced
import chefmate.client.recipe.list.public.generated.resources.recipe_sync_synced
import chefmate.client.recipe.list.public.generated.resources.recipe_sync_syncing
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.letIfTrue
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.recipe.data.SyncStatus
import com.plusmobileapps.chefmate.recipebook.data.RecipeBook
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.LocalRecipeWindowOpener
import com.plusmobileapps.chefmate.ui.components.LocalBottomNavBarInset
import com.plusmobileapps.chefmate.ui.components.LocalSnackbarInset
import com.plusmobileapps.chefmate.ui.components.PlusContextMenuArea
import com.plusmobileapps.chefmate.ui.components.PlusContextMenuItem
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusDialogScaffold
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.components.PlusNavContainer
import com.plusmobileapps.chefmate.ui.components.PlusOnboardingTooltip
import com.plusmobileapps.chefmate.ui.components.PlusResponsiveContainer
import com.plusmobileapps.chefmate.ui.components.PlusResponsiveModal
import com.plusmobileapps.chefmate.ui.components.PlusTextField
import com.plusmobileapps.chefmate.ui.components.PlusTooltipPlacement
import com.plusmobileapps.chefmate.ui.components.RecipeImage
import com.plusmobileapps.chefmate.ui.components.WindowSizeClass
import com.plusmobileapps.chefmate.ui.components.bottomNavContentPadding
import com.plusmobileapps.chefmate.ui.text.toInlineMarkdownAnnotatedString
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import com.plusmobileapps.chefmate.util.rememberImagePickerLauncher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(bloc: RecipeListBloc, modifier: Modifier = Modifier) {
    val state by bloc.state.collectAsState()
    var showSearchBar by remember { mutableStateOf(state.isSearchActive) }
    var showSortFilterSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(Unit) {
        bloc.state
            .map { Triple(it.currentSort, it.activeFilters, it.activeCategories) }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                listState.animateScrollToItem(0)
                gridState.animateScrollToItem(0)
            }
    }

    PlusResponsiveContainer { windowSizeClass ->
        // Wide windows get a permanent filter rail down the left edge; the same options are then
        // dropped from the sort & filter sheet so they only live in one place. Pointless before the
        // user has any recipes, so it waits for the library to be non-empty.
        val showFilterSidebar =
            windowSizeClass == WindowSizeClass.EXPANDED && state.totalRecipeCount > 0
        // The selector stands in for the title once any book exists — it names the active book, or
        // "All recipes" when the cross-book view is active.
        val showBookSelector = state.recipeBooks.isNotEmpty()
        val bookSelectorLabel =
            state.activeBook?.name ?: stringResource(Res.string.recipe_list_book_all_recipes)
        val headerData =
            if (state.isSelectionMode) {
                PlusHeaderData.Parent(
                    title =
                        PhraseModel(
                            Res.string.recipe_list_selection_count,
                            "count" to FixedString(state.selectedRecipeIds.size.toString()),
                        ),
                    trailingAccessory =
                        PlusHeaderData.TrailingAccessory.Custom {
                            val allSelected =
                                state.recipes.isNotEmpty() &&
                                    state.recipes.all { it.id in state.selectedRecipeIds }
                            IconButton(onClick = bloc::onToggleSelectAllVisible) {
                                Icon(
                                    imageVector =
                                        if (allSelected) Icons.Default.CheckCircle
                                        else Icons.Outlined.Circle,
                                    contentDescription =
                                        stringResource(
                                            if (allSelected) {
                                                Res.string.recipe_list_selection_deselect_all
                                            } else {
                                                Res.string.recipe_list_selection_select_all
                                            }
                                        ),
                                )
                            }
                            IconButton(
                                onClick = bloc::onExportClicked,
                                enabled = state.selectedRecipeIds.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription =
                                        stringResource(Res.string.recipe_list_selection_export),
                                )
                            }
                            SelectionOverflowMenu(
                                enabled = state.selectedRecipeIds.isNotEmpty(),
                                onAddToBookClicked = bloc::onAddToBookClicked,
                                onAddToCategoryClicked = bloc::onAddToCategoryClicked,
                            )
                            IconButton(onClick = bloc::onExitSelectionMode) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription =
                                        stringResource(Res.string.recipe_list_selection_exit),
                                )
                            }
                        },
                )
            } else {
                PlusHeaderData.Parent(
                    // The book selector stands in for the title when any book exists, so the static
                    // "Recipes" title is suppressed to avoid a cramped double-title in the app bar.
                    title =
                        if (showBookSelector) FixedString("")
                        else Res.string.recipe_list_title.asTextData(),
                    leading =
                        letIfTrue(showBookSelector) {
                            {
                                BookSelector(
                                    activeBookName = bookSelectorLabel,
                                    isPickerOpen = state.isBookPickerOpen,
                                    onClick = bloc::onBookSelectorClicked,
                                ) {
                                    // The dropdown is anchored to the selector on tablet/desktop
                                    // widths.
                                    if (windowSizeClass != WindowSizeClass.COMPACT) {
                                        RecipeBookPickerDropdown(
                                            expanded = state.isBookPickerOpen,
                                            books = state.recipeBooks,
                                            activeBookId = state.activeBook?.id,
                                            isAllRecipesSelected = state.isAllRecipesSelected,
                                            onDismiss = bloc::onBookPickerDismissed,
                                            onBookSelected = bloc::onBookSelected,
                                            onAllRecipesSelected = bloc::onAllRecipesSelected,
                                            onEditBook = bloc::onEditBookClicked,
                                            onCreateBook = bloc::onCreateBookClicked,
                                        )
                                    }
                                }
                            }
                        },
                    trailingAccessory =
                        PlusHeaderData.TrailingAccessory.Custom {
                            IconButton(
                                onClick = {
                                    showSearchBar = !showSearchBar
                                    if (!showSearchBar) bloc.onSearchQueryChanged("")
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription =
                                        stringResource(Res.string.recipe_list_search),
                                )
                            }
                            RecipeListCoachMark(
                                id = CoachMarkId.RECIPE_LIST_VIEW_MODE,
                                text = Res.string.recipe_list_view_mode_onboarding.asTextData(),
                                activeCoachMark = state.activeCoachMark,
                                onDismiss = bloc::onCoachMarkDismissed,
                            ) {
                                IconButton(onClick = bloc::onToggleViewMode) {
                                    Icon(
                                        imageVector =
                                            if (state.isGridView) Icons.AutoMirrored.Filled.ViewList
                                            else Icons.Default.GridView,
                                        contentDescription =
                                            stringResource(
                                                if (state.isGridView) {
                                                    Res.string.recipe_list_view_list
                                                } else {
                                                    Res.string.recipe_list_view_grid
                                                }
                                            ),
                                    )
                                }
                            }
                            RecipeListCoachMark(
                                id = CoachMarkId.RECIPE_LIST_FILTER,
                                text = Res.string.recipe_list_filter_onboarding.asTextData(),
                                activeCoachMark = state.activeCoachMark,
                                onDismiss = bloc::onCoachMarkDismissed,
                            ) {
                                IconButton(
                                    onClick = {
                                        bloc.onCoachMarkDismissed(CoachMarkId.RECIPE_LIST_FILTER)
                                        showSortFilterSheet = true
                                    }
                                ) {
                                    // With the rail visible the filters live there, so this action
                                    // narrows to sort-only and drops the active-filter badge.
                                    val icon =
                                        if (showFilterSidebar) Icons.AutoMirrored.Filled.Sort
                                        else Icons.Default.FilterList
                                    val label =
                                        stringResource(
                                            if (showFilterSidebar) Res.string.recipe_list_sort
                                            else Res.string.recipe_list_filter
                                        )
                                    val filterCount =
                                        if (showFilterSidebar) 0 else state.totalActiveFilterCount
                                    if (filterCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$filterCount") } }) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = label,
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    } else {
                                        Icon(imageVector = icon, contentDescription = label)
                                    }
                                }
                            }
                            RecipeListCoachMark(
                                id = CoachMarkId.RECIPE_LIST_ADD,
                                text = Res.string.recipe_list_add_onboarding.asTextData(),
                                activeCoachMark = state.activeCoachMark,
                                onDismiss = bloc::onCoachMarkDismissed,
                            ) {
                                AddRecipeMenu(
                                    scanEnabled = state.isScanFromPhotoEnabled,
                                    onCreateClicked = bloc::onAddRecipeClicked,
                                    onScanPicked = bloc::onScanRecipePhotoPicked,
                                )
                            }
                            OverflowMenu(
                                onSelectClicked = bloc::onEnterSelectionMode,
                                onExportAllClicked = bloc::onExportClicked,
                                onCollaborateClicked = bloc::onCollaborateClicked,
                                onSyncClicked = bloc::onSyncClicked,
                            )
                        },
                )
            }

        Box(modifier = modifier.fillMaxSize().testTag(RecipeListTestTags.SCREEN)) {
            PlusNavContainer(
                modifier = modifier.fillMaxSize(),
                data = headerData,
                scrollEnabled = false,
                content = {
                    AnimatedVisibility(
                        visible = showSearchBar,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        SearchBar(
                            query = state.searchQuery,
                            onQueryChanged = bloc::onSearchQueryChanged,
                            onClear = {
                                bloc.onSearchQueryChanged("")
                                showSearchBar = false
                            },
                        )
                    }
                    state.pendingInvites.forEach { invite ->
                        InviteBanner(
                            invite = invite,
                            onAccept = { bloc.onAcceptInvite(invite.memberId) },
                            onDecline = { bloc.onDeclineInvite(invite.memberId) },
                        )
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        if (showFilterSidebar) {
                            RecipeFilterSidebar(
                                activeFilters = state.activeFilters,
                                activeCategories = state.activeCategories,
                                activeUserCategoryIds = state.activeUserCategoryIds,
                                availableUserCategories = state.availableUserCategories,
                                onFilterToggled = bloc::onFilterToggled,
                                onCategoryToggled = { category ->
                                    bloc.onApplySortAndFilters(
                                        sort = state.currentSort,
                                        filters = state.activeFilters,
                                        categories = state.activeCategories.toggle(category),
                                        userCategoryIds = state.activeUserCategoryIds,
                                    )
                                },
                                onUserCategoryToggled = { categoryId ->
                                    bloc.onApplySortAndFilters(
                                        sort = state.currentSort,
                                        filters = state.activeFilters,
                                        categories = state.activeCategories,
                                        userCategoryIds =
                                            state.activeUserCategoryIds.toggle(categoryId),
                                    )
                                },
                                onClearFilters = bloc::onClearFilters,
                            )
                        }
                        PullToRefreshBox(
                            isRefreshing = state.isSyncing,
                            onRefresh = bloc::onSyncClicked,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        ) {
                            when {
                                !state.isLoading && state.totalRecipeCount == 0 -> {
                                    NoRecipesEmptyState(
                                        onBrowseClicked = bloc::onBrowseRecipesClicked,
                                        onCreateClicked = bloc::onAddRecipeClicked,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                state.recipes.isEmpty() && state.isSearchActive -> {
                                    SearchEmptyState(
                                        canSearchAllRecipes = !state.isAllRecipesSelected,
                                        onSearchAllRecipes = bloc::onAllRecipesSelected,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                state.recipes.isEmpty() && state.totalActiveFilterCount > 0 -> {
                                    FilterEmptyState(
                                        activeFilters = state.activeFilters,
                                        activeCategories = state.activeCategories,
                                        onClearFilters = bloc::onClearFilters,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                                state.isGridView -> {
                                    RecipeGrid(
                                        modifier = Modifier.fillMaxSize(),
                                        recipes = state.recipes,
                                        onRecipeClicked = bloc::onRecipeClicked,
                                        onRecipeLongClicked = bloc::onRecipeLongClicked,
                                        state = gridState,
                                        bottomContentPadding =
                                            if (state.cookingRecipeCount > 0) FabStackReserve
                                            else 0.dp,
                                        isSelectionMode = state.isSelectionMode,
                                        selectedRecipeIds = state.selectedRecipeIds,
                                    )
                                }
                                else -> {
                                    RecipeList(
                                        modifier = Modifier.fillMaxSize(),
                                        recipes = state.recipes,
                                        onRecipeClicked = bloc::onRecipeClicked,
                                        onRecipeLongClicked = bloc::onRecipeLongClicked,
                                        state = listState,
                                        bottomContentPadding =
                                            if (state.cookingRecipeCount > 0) FabStackReserve
                                            else 0.dp,
                                        isSelectionMode = state.isSelectionMode,
                                        selectedRecipeIds = state.selectedRecipeIds,
                                    )
                                }
                            }
                        }
                    }
                },
            )

            if (showSortFilterSheet) {
                SortFilterModal(
                    showFilters = !showFilterSidebar,
                    currentSort = state.currentSort,
                    activeFilters = state.activeFilters,
                    activeCategories = state.activeCategories,
                    activeUserCategoryIds = state.activeUserCategoryIds,
                    availableUserCategories = state.availableUserCategories,
                    onApply = { sort, filters, categories, userCategoryIds ->
                        bloc.onApplySortAndFilters(sort, filters, categories, userCategoryIds)
                        showSortFilterSheet = false
                    },
                    onDismiss = { showSortFilterSheet = false },
                )
            }

            if (state.cookingRecipeCount > 0) {
                CookingSessionFabStack(
                    onContinueClicked = bloc::onContinueCookingClicked,
                    onDoneCookingClicked = bloc::onDoneCookingClicked,
                    // Clear the floating nav pill, and ride up further so the app-wide cook-mode
                    // toast never covers the FAB stack (matching MealPlanScreen).
                    modifier =
                        Modifier.align(Alignment.BottomEnd)
                            .padding(
                                bottom = LocalBottomNavBarInset.current + LocalSnackbarInset.current
                            ),
                )
            }

            if (state.showDoneCookingDialog) {
                DoneCookingDialog(
                    onConfirm = bloc::onDoneCookingConfirmed,
                    onDismiss = bloc::onDoneCookingDismissed,
                )
            }

            if (state.isScanning) {
                ScanningDialog()
            }

            state.scanError?.let { error ->
                PlusDialog(
                    title = ResourceString(Res.string.recipe_list_scan_failed_title),
                    message = error,
                    onConfirmClick = bloc::onScanErrorDismissed,
                    onDismissRequest = bloc::onScanErrorDismissed,
                )
            }

            // On phone widths the book picker is a bottom sheet rather than an anchored dropdown.
            if (state.isBookPickerOpen && windowSizeClass == WindowSizeClass.COMPACT) {
                RecipeBookPickerSheet(
                    books = state.recipeBooks,
                    activeBookId = state.activeBook?.id,
                    isAllRecipesSelected = state.isAllRecipesSelected,
                    onDismiss = bloc::onBookPickerDismissed,
                    onBookSelected = bloc::onBookSelected,
                    onAllRecipesSelected = bloc::onAllRecipesSelected,
                    onEditBook = bloc::onEditBookClicked,
                    onCreateBook = bloc::onCreateBookClicked,
                )
            }

            if (state.isBulkBookPickerOpen) {
                BulkAddToBookSheet(
                    books = state.recipeBooks,
                    onDismiss = bloc::onBulkBookPickerDismissed,
                    onBookSelected = bloc::onAddSelectedToBook,
                )
            }

            if (state.isBulkCategoryPickerOpen) {
                BulkAddToCategorySheet(
                    userCategories = state.availableUserCategories,
                    onDismiss = bloc::onBulkCategoryPickerDismissed,
                    onBuiltinSelected = bloc::onAddSelectedToBuiltinCategory,
                    onUserCategorySelected = bloc::onAddSelectedToUserCategory,
                )
            }
        }
    }
}

/**
 * The "+" action. When [scanEnabled] it opens a chooser — create a recipe by hand, or scan one from
 * a photo via Gemini vision (the image picker launches directly from the scan item). When disabled
 * it opens the blank editor directly with no menu, preserving the original add-recipe behaviour.
 */
/**
 * Wraps a header control in a first-run coach mark that points up at it (the header sits at the top
 * of the screen). The bubble only shows while [id] is the shared controller's active mark.
 */
@Composable
private fun RecipeListCoachMark(
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

@Composable
private fun AddRecipeMenu(
    scanEnabled: Boolean,
    onCreateClicked: () -> Unit,
    onScanPicked: (ByteArray, String) -> Unit,
) {
    if (!scanEnabled) {
        IconButton(
            onClick = onCreateClicked,
            modifier = Modifier.testTag(RecipeListTestTags.ADD_RECIPE_BUTTON),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.recipe_list_add_recipe),
            )
        }
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val scanPicker = rememberImagePickerLauncher { picked ->
        picked?.let { onScanPicked(it.bytes, it.fileExtension) }
    }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(RecipeListTestTags.ADD_RECIPE_BUTTON),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.recipe_list_add_recipe),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_list_create_recipe)) },
                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                modifier = Modifier.testTag(RecipeListTestTags.ADD_MENU_CREATE),
                onClick = {
                    expanded = false
                    onCreateClicked()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_list_scan_from_photo)) },
                leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                modifier = Modifier.testTag(RecipeListTestTags.ADD_MENU_SCAN),
                onClick = {
                    expanded = false
                    scanPicker()
                },
            )
        }
    }
}

/** Non-cancellable progress dialog shown while a picked photo is being scanned into a recipe. */
@Composable
private fun ScanningDialog() {
    PlusDialogScaffold(
        onDismissRequest = {},
        header = { Text(stringResource(Res.string.recipe_list_scanning_title)) },
        content = {
            Row(
                modifier = Modifier.testTag(RecipeListTestTags.SCANNING_DIALOG),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PlusLoadingIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(stringResource(Res.string.recipe_list_scanning_message))
            }
        },
    )
}

@Composable
private fun InviteBanner(
    invite: com.plusmobileapps.chefmate.recipebook.data.RecipeBookInvite,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(ChefMateTheme.dimens.paddingNormal),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
    ) {
        Column(modifier = Modifier.padding(ChefMateTheme.dimens.paddingNormal)) {
            Text(
                text =
                    PhraseModel(
                            Res.string.recipe_list_invite_message,
                            "book" to FixedString(invite.bookName),
                        )
                        .localized(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = ChefMateTheme.dimens.paddingSmall),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDecline) {
                    Text(stringResource(Res.string.recipe_list_invite_decline))
                }
                TextButton(onClick = onAccept) {
                    Text(stringResource(Res.string.recipe_list_invite_accept))
                }
            }
        }
    }
}

@Composable
private fun CookingSessionFabStack(
    onContinueClicked: () -> Unit,
    onDoneCookingClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(end = 16.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ExtendedFloatingActionButton(
            onClick = onDoneCookingClicked,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.loweredElevation(),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(Res.string.recipe_list_done_cooking),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        ExtendedFloatingActionButton(
            onClick = onContinueClicked,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(imageVector = Icons.Default.SoupKitchen, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(Res.string.recipe_list_continue_cooking))
        }
    }
}

@Composable
private fun DoneCookingDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.recipe_list_done_cooking_title)) },
        text = { Text(stringResource(Res.string.recipe_list_done_cooking_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(Res.string.recipe_list_done_cooking_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.recipe_list_done_cooking_cancel))
            }
        },
    )
}

@Composable
private fun OverflowMenu(
    onSelectClicked: () -> Unit,
    onExportAllClicked: () -> Unit,
    onCollaborateClicked: () -> Unit,
    onSyncClicked: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.recipe_list_more_actions),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_list_menu_sync)) },
                leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSyncClicked()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_list_menu_select)) },
                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                onClick = {
                    expanded = false
                    onSelectClicked()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_list_menu_export_all)) },
                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                onClick = {
                    expanded = false
                    onExportAllClicked()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_list_menu_collaborate)) },
                leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                onClick = {
                    expanded = false
                    onCollaborateClicked()
                },
            )
        }
    }
}

/**
 * Overflow menu shown in the selection-mode app bar. Holds the bulk actions that don't warrant
 * their own icon — filing the selection under a recipe book or tagging it with a category. Disabled
 * while nothing is selected.
 */
@Composable
private fun SelectionOverflowMenu(
    enabled: Boolean,
    onAddToBookClicked: () -> Unit,
    onAddToCategoryClicked: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.testTag(RecipeListTestTags.SELECTION_OVERFLOW),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.recipe_list_selection_more),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_list_selection_add_to_book)) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
                },
                modifier = Modifier.testTag(RecipeListTestTags.SELECTION_ADD_TO_BOOK),
                onClick = {
                    expanded = false
                    onAddToBookClicked()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.recipe_list_selection_add_to_category)) },
                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                modifier = Modifier.testTag(RecipeListTestTags.SELECTION_ADD_TO_CATEGORY),
                onClick = {
                    expanded = false
                    onAddToCategoryClicked()
                },
            )
        }
    }
}

/** Bottom sheet listing the user's recipe books so the current selection can be filed under one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkAddToBookSheet(
    books: List<RecipeBook>,
    onDismiss: () -> Unit,
    onBookSelected: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.navigationBarsPadding().testTag(RecipeListTestTags.BULK_BOOK_SHEET)
        ) {
            Text(
                text = stringResource(Res.string.recipe_list_bulk_book_title),
                style = MaterialTheme.typography.titleLarge,
                modifier =
                    Modifier.padding(
                        horizontal = ChefMateTheme.dimens.paddingNormal,
                        vertical = ChefMateTheme.dimens.paddingSmall,
                    ),
            )
            HorizontalDivider()
            books.forEach { book ->
                ListItem(
                    headlineContent = { Text(book.name) },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    modifier = Modifier.fillMaxWidth().clickable { onBookSelected(book.id) },
                )
            }
        }
    }
}

/**
 * Bottom sheet of category chips — built-in presets followed by the user's own categories — so the
 * current selection can be tagged. Tapping a chip adds and closes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BulkAddToCategorySheet(
    userCategories: List<Category>,
    onDismiss: () -> Unit,
    onBuiltinSelected: (BuiltinCategory) -> Unit,
    onUserCategorySelected: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier =
                Modifier.padding(horizontal = ChefMateTheme.dimens.paddingNormal)
                    .navigationBarsPadding()
                    .testTag(RecipeListTestTags.BULK_CATEGORY_SHEET)
        ) {
            Text(
                text = stringResource(Res.string.recipe_list_bulk_category_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = ChefMateTheme.dimens.paddingSmall),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BuiltinCategory.entries.forEach { category ->
                    FilterChip(
                        selected = false,
                        onClick = { onBuiltinSelected(category) },
                        label = { Text(stringResource(category.labelRes())) },
                    )
                }
                // User categories that aren't just a preset in disguise (those render above).
                userCategories
                    .filter { it.builtinId == null }
                    .forEach { category ->
                        FilterChip(
                            selected = false,
                            onClick = { onUserCategorySelected(category.id) },
                            label = { Text(category.name) },
                        )
                    }
            }
            Spacer(Modifier.height(ChefMateTheme.dimens.paddingNormal))
        }
    }
}

@Composable
private fun BookSelector(
    activeBookName: String?,
    isPickerOpen: Boolean,
    onClick: () -> Unit,
    dropdown: @Composable () -> Unit,
) {
    Box {
        Row(
            modifier =
                Modifier.clickable(onClick = onClick)
                    .padding(
                        horizontal = ChefMateTheme.dimens.paddingSmall,
                        vertical = ChefMateTheme.dimens.paddingExtraSmall,
                    )
                    .testTag(RecipeListTestTags.BOOK_SELECTOR)
                    .semantics { contentDescription = activeBookName ?: "" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = activeBookName.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
            Icon(
                imageVector =
                    if (isPickerOpen) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = stringResource(Res.string.recipe_list_book_selector),
            )
        }
        dropdown()
    }
}

// region Sort & Filter Bottom Sheet

/**
 * Sort & filter controls, presented as a bottom sheet on phone/tablet widths and as a centered
 * dialog on expanded ones — [PlusResponsiveModal] picks the presentation.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SortFilterModal(
    showFilters: Boolean,
    currentSort: RecipeSortOption,
    activeFilters: Set<RecipeFilterOption>,
    activeCategories: Set<BuiltinCategory>,
    activeUserCategoryIds: Set<Long>,
    availableUserCategories: List<com.plusmobileapps.chefmate.recipe.data.Category>,
    onApply:
        (
            sort: RecipeSortOption,
            filters: Set<RecipeFilterOption>,
            categories: Set<BuiltinCategory>,
            userCategoryIds: Set<Long>,
        ) -> Unit,
    onDismiss: () -> Unit,
) {
    // The content renders its own "Sort & Filter" heading, so the modal chrome stays bare — a
    // second title/close row would only crowd it, and Apply already dismisses.
    PlusResponsiveModal(onDismissRequest = onDismiss) {
        SortFilterSheetContent(
            showFilters = showFilters,
            initialSort = currentSort,
            initialFilters = activeFilters,
            initialCategories = activeCategories,
            initialUserCategoryIds = activeUserCategoryIds,
            availableUserCategories = availableUserCategories,
            onApply = onApply,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SortFilterSheetContent(
    initialSort: RecipeSortOption,
    initialFilters: Set<RecipeFilterOption>,
    initialCategories: Set<BuiltinCategory>,
    initialUserCategoryIds: Set<Long> = emptySet(),
    availableUserCategories: List<com.plusmobileapps.chefmate.recipe.data.Category> = emptyList(),
    onApply:
        (
            sort: RecipeSortOption,
            filters: Set<RecipeFilterOption>,
            categories: Set<BuiltinCategory>,
            userCategoryIds: Set<Long>,
        ) -> Unit,
    modifier: Modifier = Modifier,
    /**
     * When false the "Filter by" and "Category" sections are omitted and only sort remains — used
     * at expanded widths where those filters live in [RecipeFilterSidebar] instead.
     */
    showFilters: Boolean = true,
) {
    var selectedSort by remember { mutableStateOf(initialSort) }
    var selectedFilters by remember { mutableStateOf(initialFilters) }
    var selectedCategories by remember { mutableStateOf(initialCategories) }
    var selectedUserCategoryIds by remember { mutableStateOf(initialUserCategoryIds) }
    val anyFilterActive =
        selectedFilters.isNotEmpty() ||
            selectedCategories.isNotEmpty() ||
            selectedUserCategoryIds.isNotEmpty()

    Column(modifier = modifier.padding(horizontal = 16.dp).navigationBarsPadding()) {
        Text(
            text =
                stringResource(
                    if (showFilters) Res.string.recipe_list_sort_and_filter
                    else Res.string.recipe_list_sort
                ),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(16.dp))

        // Sort by
        Text(
            text = stringResource(Res.string.recipe_list_sort_by),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecipeSortOption.entries.forEach { option ->
                FilterChip(
                    selected = option == selectedSort,
                    onClick = { selectedSort = option },
                    label = { Text(stringResource(option.labelRes())) },
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (showFilters) {
            // Filter by
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.recipe_list_filter_by),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (anyFilterActive) {
                    TextButton(
                        onClick = {
                            selectedFilters = emptySet()
                            selectedCategories = emptySet()
                            selectedUserCategoryIds = emptySet()
                        }
                    ) {
                        Text(stringResource(Res.string.recipe_list_clear_filters))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecipeFilterOption.entries.forEach { filter ->
                    FilterChip(
                        selected = filter in selectedFilters,
                        onClick = {
                            selectedFilters =
                                if (filter in selectedFilters) selectedFilters - filter
                                else selectedFilters + filter
                        },
                        label = { Text(stringResource(filter.labelRes())) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // Filter by Category — presets first (in enum order) followed by user-created
            // categories.
            Text(
                text = stringResource(Res.string.recipe_list_filter_by_category),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BuiltinCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category in selectedCategories,
                        onClick = {
                            selectedCategories =
                                if (category in selectedCategories) {
                                    selectedCategories - category
                                } else {
                                    selectedCategories + category
                                }
                        },
                        label = { Text(stringResource(category.labelRes())) },
                    )
                }
                // User-created categories: filter out any that masquerade as a preset (by
                // builtinId)
                // so we don't render a duplicate chip when both representations are present.
                availableUserCategories
                    .filter { it.builtinId == null }
                    .forEach { category ->
                        val isSelected = category.id in selectedUserCategoryIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedUserCategoryIds =
                                    if (isSelected) selectedUserCategoryIds - category.id
                                    else selectedUserCategoryIds + category.id
                            },
                            label = { Text(category.name) },
                        )
                    }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                onApply(selectedSort, selectedFilters, selectedCategories, selectedUserCategoryIds)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.recipe_list_apply))
        }
        Spacer(Modifier.height(16.dp))
    }
}

// endregion

/** Adds [value] to the set when absent and removes it when present. */
private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

private fun RecipeSortOption.labelRes(): StringResource =
    when (this) {
        RecipeSortOption.RECENTLY_ADDED -> Res.string.recipe_list_sort_recently_added
        RecipeSortOption.OLDEST_FIRST -> Res.string.recipe_list_sort_oldest_first
        RecipeSortOption.ALPHABETICAL_ASC -> Res.string.recipe_list_sort_a_to_z
        RecipeSortOption.ALPHABETICAL_DESC -> Res.string.recipe_list_sort_z_to_a
        RecipeSortOption.TOP_RATED -> Res.string.recipe_list_sort_top_rated
    }

internal fun RecipeFilterOption.labelRes(): StringResource =
    when (this) {
        RecipeFilterOption.FAVORITES -> Res.string.recipe_list_filter_favorites
        RecipeFilterOption.RATED -> Res.string.recipe_list_filter_rated
        RecipeFilterOption.QUICK_RECIPES -> Res.string.recipe_list_filter_quick_recipes
    }

internal fun BuiltinCategory.labelRes(): StringResource =
    when (this) {
        BuiltinCategory.BREAKFAST -> Res.string.recipe_list_category_breakfast
        BuiltinCategory.LUNCH -> Res.string.recipe_list_category_lunch
        BuiltinCategory.DINNER -> Res.string.recipe_list_category_dinner
        BuiltinCategory.APPETIZER -> Res.string.recipe_list_category_appetizer
        BuiltinCategory.SIDE -> Res.string.recipe_list_category_side
        BuiltinCategory.DESSERT -> Res.string.recipe_list_category_dessert
        BuiltinCategory.SNACK -> Res.string.recipe_list_category_snack
        BuiltinCategory.DRINK -> Res.string.recipe_list_category_drink
        BuiltinCategory.OTHER -> Res.string.recipe_list_category_other
        BuiltinCategory.AI -> Res.string.recipe_list_category_ai
    }

// region Search

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.weight(1f),
            focusRequester = focusRequester,
            placeholder = { Text(stringResource(Res.string.recipe_list_search_placeholder)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            singleLine = true,
        )
        // The clear/close control lives OUTSIDE PlusTextField rather than in its trailing slot. On
        // iOS the field renders through the native text input (usingNativeTextInput), whose UIView
        // covers the field's bounds and swallows taps landing on any control drawn inside it — so a
        // trailing IconButton there never fired and the search wouldn't close. As a sibling it sits
        // outside that native view and reliably clears the query and dismisses the search bar.
        IconButton(onClick = onClear) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.recipe_list_search_clear),
            )
        }
    }
}

@Composable
private fun SearchEmptyState(
    canSearchAllRecipes: Boolean,
    onSearchAllRecipes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(Res.string.recipe_list_search_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp),
        )
        // When the search was scoped to a single book, offer to broaden it across every book in
        // case the match lives elsewhere.
        if (canSearchAllRecipes) {
            Text(
                text = stringResource(Res.string.recipe_list_search_empty_book_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onSearchAllRecipes, modifier = Modifier.padding(top = 16.dp)) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(Res.string.recipe_list_search_all_recipes))
            }
        }
    }
}

// endregion

// region Empty States

@Composable
private fun NoRecipesEmptyState(
    onBrowseClicked: () -> Unit,
    onCreateClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Restaurant,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.recipe_list_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.recipe_list_empty_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onBrowseClicked, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.recipe_list_empty_browse))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCreateClicked, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(Res.string.recipe_list_empty_create))
        }
    }
}

@Composable
private fun FilterEmptyState(
    activeFilters: Set<RecipeFilterOption>,
    activeCategories: Set<BuiltinCategory>,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val filterLabels =
        (activeFilters.map { stringResource(it.labelRes()) } +
                activeCategories.map { stringResource(it.labelRes()) })
            .joinToString(", ")

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.recipe_list_filter_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text =
                PhraseModel(
                        Res.string.recipe_list_filter_empty_description,
                        "filters" to FixedString(filterLabels),
                    )
                    .localized(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onClearFilters) {
            Text(stringResource(Res.string.recipe_list_clear_filters))
        }
    }
}

// endregion

// region Context Menu

/**
 * Adds the desktop right-click menu to a recipe row or card. On Android and iOS
 * [LocalRecipeWindowOpener] is null — there is nothing to put in the menu and no mouse to open it
 * with — so the item renders exactly as it did before.
 */
@Composable
private fun RecipeContextMenu(recipe: RecipeListItem, content: @Composable () -> Unit) {
    val windowOpener = LocalRecipeWindowOpener.current
    if (windowOpener == null) {
        content()
        return
    }
    val label = stringResource(Res.string.recipe_list_open_in_new_window)
    PlusContextMenuArea(
        items = {
            listOf(PlusContextMenuItem(label) { windowOpener.open(recipe.id, recipe.title) })
        },
        content = content,
    )
}

// endregion

// region Grid View

@Composable
private fun RecipeGrid(
    recipes: List<RecipeListItem>,
    onRecipeClicked: (RecipeListItem) -> Unit,
    modifier: Modifier = Modifier,
    onRecipeLongClicked: (RecipeListItem) -> Unit = {},
    state: LazyGridState = rememberLazyGridState(),
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
    isSelectionMode: Boolean = false,
    selectedRecipeIds: Set<Long> = emptySet(),
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        state = state,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding =
            bottomNavContentPadding(
                start = 8.dp,
                end = 8.dp,
                top = 8.dp,
                bottom = 8.dp + bottomContentPadding,
            ),
    ) {
        items(recipes.size, key = { recipes[it].id }) { index ->
            val recipe = recipes[index]
            RecipeContextMenu(recipe) {
                RecipeGridItem(
                    recipe = recipe,
                    onClick = { onRecipeClicked(recipe) },
                    onLongClick = { onRecipeLongClicked(recipe) },
                    isSelectionMode = isSelectionMode,
                    isSelected = recipe.id in selectedRecipeIds,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecipeGridItem(
    recipe: RecipeListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelectionMode && isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
            ),
    ) {
        Box {
            RecipeImage(
                imageUrl = recipe.imageUrl,
                contentDescription = recipe.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.2f),
            )
            if (isSelectionMode) {
                SelectionBadge(
                    isSelected = isSelected,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                )
            }
        }
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StarRating(rating = recipe.starRating, starSize = 14.dp)
                SyncStatusIcon(syncStatus = recipe.syncStatus)
            }
        }
    }
}

/**
 * Filled-circle checkmark when selected, hollow circle when not — small enough to overlay a recipe
 * image without overpowering it.
 */
@Composable
private fun SelectionBadge(isSelected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(24.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color =
            if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.Check else Icons.Outlined.Circle,
            contentDescription = null,
            tint =
                if (isSelected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(2.dp),
        )
    }
}

// endregion

// region List View

@Composable
private fun RecipeList(
    recipes: List<RecipeListItem>,
    onRecipeClicked: (RecipeListItem) -> Unit,
    modifier: Modifier = Modifier,
    onRecipeLongClicked: (RecipeListItem) -> Unit = {},
    state: LazyListState = rememberLazyListState(),
    bottomContentPadding: androidx.compose.ui.unit.Dp = 0.dp,
    isSelectionMode: Boolean = false,
    selectedRecipeIds: Set<Long> = emptySet(),
) {
    LazyColumn(
        state = state,
        modifier = modifier.fillMaxWidth(),
        contentPadding = bottomNavContentPadding(bottom = bottomContentPadding),
    ) {
        items(recipes.size, key = { recipes[it].id }) { index ->
            val recipe = recipes[index]
            RecipeContextMenu(recipe) {
                RecipeListItemContent(
                    recipe = recipe,
                    onClick = { onRecipeClicked(recipe) },
                    onLongClick = { onRecipeLongClicked(recipe) },
                    isSelectionMode = isSelectionMode,
                    isSelected = recipe.id in selectedRecipeIds,
                )
            }
        }
    }
}

/** Approximate height of the Continue/Done Cooking FAB stack plus breathing room. */
private val FabStackReserve = 152.dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun RecipeListItemContent(
    recipe: RecipeListItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
) {
    val background =
        if (isSelectionMode && isSelected) MaterialTheme.colorScheme.primaryContainer
        else androidx.compose.ui.graphics.Color.Transparent
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .background(background)
                .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint =
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        RecipeImage(
            imageUrl = recipe.imageUrl,
            contentDescription = recipe.title,
            modifier = Modifier.size(80.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                SyncStatusIcon(syncStatus = recipe.syncStatus)
                if (recipe.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
            recipe.description?.let { description ->
                Text(
                    text = description.toInlineMarkdownAnnotatedString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StarRating(rating = recipe.starRating, starSize = 16.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                recipe.formattedTotalTime?.let { formattedTime ->
                    RecipeInfoChip(
                        icon = Icons.Outlined.AccessTime,
                        label = formattedTime.localized(),
                    )
                }
                recipe.servings?.let { servings ->
                    RecipeInfoChip(
                        icon = Icons.Outlined.Restaurant,
                        label =
                            PhraseModel(
                                    Res.string.recipe_list_item_servings,
                                    "servings" to FixedString(servings.toString()),
                                )
                                .localized(),
                    )
                }
                recipe.calories?.let { calories ->
                    RecipeInfoChip(
                        icon = Icons.Outlined.LocalFireDepartment,
                        label =
                            PhraseModel(
                                    Res.string.recipe_list_item_calories,
                                    "calories" to FixedString(calories.toString()),
                                )
                                .localized(),
                    )
                }
            }
        }
    }
}

// endregion

// region Shared Components

@Composable
private fun SyncStatusIcon(syncStatus: SyncStatus, modifier: Modifier = Modifier) {
    val syncingDescription = stringResource(Res.string.recipe_sync_syncing)
    when (syncStatus) {
        SyncStatus.NOT_SYNCED ->
            Icon(
                imageVector = Icons.Outlined.CloudOff,
                contentDescription = stringResource(Res.string.recipe_sync_not_synced),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.size(16.dp),
            )
        SyncStatus.SYNCING ->
            PlusLoadingIndicator(
                modifier = modifier.size(14.dp),
                contentDescription = syncingDescription,
                strokeWidth = 2.dp,
            )
        SyncStatus.SYNCED ->
            Icon(
                imageVector = Icons.Outlined.CloudDone,
                contentDescription = stringResource(Res.string.recipe_sync_synced),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = modifier.size(16.dp),
            )
    }
}

@Composable
private fun StarRating(
    rating: Int?,
    starSize: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val displayRating = rating ?: 0
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            val isFilled = index < displayRating
            Icon(
                imageVector = if (isFilled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                modifier = Modifier.size(starSize),
                tint =
                    if (isFilled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    },
            )
        }
    }
}

@Composable
private fun RecipeInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// endregion

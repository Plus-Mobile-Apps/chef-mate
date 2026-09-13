package com.plusmobileapps.chefmate.grocery.core.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chefmate.client.grocery.core.public.generated.resources.Res
import chefmate.client.grocery.core.public.generated.resources.grocery_accept
import chefmate.client.grocery.core.public.generated.resources.grocery_add_item_hint
import chefmate.client.grocery.core.public.generated.resources.grocery_apply
import chefmate.client.grocery.core.public.generated.resources.grocery_cancel
import chefmate.client.grocery.core.public.generated.resources.grocery_clear_filters
import chefmate.client.grocery.core.public.generated.resources.grocery_create_list_cancel
import chefmate.client.grocery.core.public.generated.resources.grocery_create_list_confirm
import chefmate.client.grocery.core.public.generated.resources.grocery_create_list_hint
import chefmate.client.grocery.core.public.generated.resources.grocery_create_list_title
import chefmate.client.grocery.core.public.generated.resources.grocery_create_new_list
import chefmate.client.grocery.core.public.generated.resources.grocery_delete_all
import chefmate.client.grocery.core.public.generated.resources.grocery_delete_item
import chefmate.client.grocery.core.public.generated.resources.grocery_delete_items
import chefmate.client.grocery.core.public.generated.resources.grocery_delete_items_message
import chefmate.client.grocery.core.public.generated.resources.grocery_delete_items_title
import chefmate.client.grocery.core.public.generated.resources.grocery_delete_purchased
import chefmate.client.grocery.core.public.generated.resources.grocery_detail
import chefmate.client.grocery.core.public.generated.resources.grocery_done
import chefmate.client.grocery.core.public.generated.resources.grocery_edit_list
import chefmate.client.grocery.core.public.generated.resources.grocery_filter
import chefmate.client.grocery.core.public.generated.resources.grocery_filter_all
import chefmate.client.grocery.core.public.generated.resources.grocery_filter_by
import chefmate.client.grocery.core.public.generated.resources.grocery_filter_by_recipe
import chefmate.client.grocery.core.public.generated.resources.grocery_filter_no_recipe
import chefmate.client.grocery.core.public.generated.resources.grocery_filter_purchased
import chefmate.client.grocery.core.public.generated.resources.grocery_filter_unpurchased
import chefmate.client.grocery.core.public.generated.resources.grocery_list
import chefmate.client.grocery.core.public.generated.resources.grocery_list_empty_browse_recipes
import chefmate.client.grocery.core.public.generated.resources.grocery_list_empty_description
import chefmate.client.grocery.core.public.generated.resources.grocery_list_empty_title
import chefmate.client.grocery.core.public.generated.resources.grocery_list_filtered_empty_clear_filters
import chefmate.client.grocery.core.public.generated.resources.grocery_list_filtered_empty_description
import chefmate.client.grocery.core.public.generated.resources.grocery_list_filtered_empty_title
import chefmate.client.grocery.core.public.generated.resources.grocery_my_lists
import chefmate.client.grocery.core.public.generated.resources.grocery_pending_invite_message
import chefmate.client.grocery.core.public.generated.resources.grocery_reject
import chefmate.client.grocery.core.public.generated.resources.grocery_save_autocomplete
import chefmate.client.grocery.core.public.generated.resources.grocery_shared_badge
import chefmate.client.grocery.core.public.generated.resources.grocery_shared_with_you
import chefmate.client.grocery.core.public.generated.resources.grocery_sort_aisle
import chefmate.client.grocery.core.public.generated.resources.grocery_sort_alphabetical
import chefmate.client.grocery.core.public.generated.resources.grocery_sort_and_filter
import chefmate.client.grocery.core.public.generated.resources.grocery_sort_by
import chefmate.client.grocery.core.public.generated.resources.grocery_sync_all
import chefmate.client.grocery.core.public.generated.resources.grocery_sync_not_synced
import chefmate.client.grocery.core.public.generated.resources.grocery_sync_onboarding
import chefmate.client.grocery.core.public.generated.resources.grocery_sync_synced
import chefmate.client.grocery.core.public.generated.resources.grocery_sync_syncing
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.plusmobileapps.chefmate.grocery.core.displayName
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryListInvite
import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.grocery.data.ListRole
import com.plusmobileapps.chefmate.grocery.data.SyncStatus
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.components.LocalBottomNavBarInset
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.components.PlusNavContainer
import com.plusmobileapps.chefmate.ui.components.PlusOnboardingTooltip
import com.plusmobileapps.chefmate.ui.components.PlusResponsiveContainer
import com.plusmobileapps.chefmate.ui.components.PlusResponsiveModal
import com.plusmobileapps.chefmate.ui.components.PlusTextField
import com.plusmobileapps.chefmate.ui.components.PlusTooltipPlacement
import com.plusmobileapps.chefmate.ui.components.WindowSizeClass
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryListScreen(
    bloc: GroceryListBloc,
    modifier: Modifier = Modifier,
    forceShowAutocompleteSuggestions: Boolean = false,
) {
    val state by bloc.state.collectAsState()
    var showSortFilterSheet by remember { mutableStateOf(false) }
    val hasActiveFilter = state.filter != GroceryListBloc.GroceryFilter.ALL
    val hasNonDefaultSort = state.sort != GroceryListBloc.GrocerySort.AISLE
    val hasRecipeFilter = state.recipeFilter != null
    val activeCount =
        (if (hasActiveFilter) 1 else 0) +
            (if (hasNonDefaultSort) 1 else 0) +
            (if (hasRecipeFilter) 1 else 0)
    val focusManager = LocalFocusManager.current
    // Scrolling the list dismisses the keyboard, mirroring the tap-to-dismiss gesture below. The
    // connection never consumes the scroll so pull-to-refresh and the list itself keep scrolling.
    val dismissKeyboardOnScroll =
        remember(focusManager) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y != 0f) focusManager.clearFocus()
                    return Offset.Zero
                }
            }
        }

    // When the user adds an item from the input, scroll it into view and briefly highlight it. We
    // can't know the new item's id up front, so we flag the pending add and, on the next state that
    // introduces an id we hadn't seen, treat that id as the freshly added item.
    val listState = rememberLazyListState()
    var highlightedItemKey by remember { mutableStateOf<Long?>(null) }
    var awaitingAddedItem by remember { mutableStateOf(false) }
    var knownItemIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

    LaunchedEffect(state.groupedItems) {
        val currentIds = state.groupedItems.flatMap { group -> group.items.map { it.id } }.toSet()
        if (awaitingAddedItem) {
            val addedId = (currentIds - knownItemIds).firstOrNull()
            if (addedId != null) {
                awaitingAddedItem = false
                highlightedItemKey = addedId
                val showHeaders = state.sort == GroceryListBloc.GrocerySort.AISLE
                val index = state.groupedItems.flatIndexOfItem(addedId, showHeaders)
                if (index >= 0) listState.animateScrollToItem(index)
            }
        }
        knownItemIds = currentIds
    }

    PlusResponsiveContainer(
        modifier = modifier.testTag(GroceryListTestTags.SCREEN).fillMaxSize()
    ) { windowSizeClass ->
        val useDropdownSelector = windowSizeClass == WindowSizeClass.EXPANDED
        PlusNavContainer(
            modifier = Modifier.fillMaxSize(),
            data = PlusHeaderData.None,
            scrollEnabled = false,
            content = {
                TopAppBar(
                    windowInsets = WindowInsets(0.dp),
                    title = {
                        Box {
                            Row(
                                modifier =
                                    Modifier.testTag(GroceryListTestTags.LIST_SELECTOR)
                                        .clickable(onClick = bloc::onListSelectorClicked),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text =
                                        state.selectedList?.name
                                            ?: stringResource(Res.string.grocery_list)
                                )
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = state.showListSelector
                                )
                            }
                            if (useDropdownSelector) {
                                GroceryListSelectorDropdown(
                                    expanded = state.showListSelector,
                                    state = state,
                                    onDismiss = bloc::onListSelectorDismissed,
                                    onListSelected = bloc::onListSelected,
                                    onCreateListClicked = bloc::onCreateListClicked,
                                    onEditListClicked = bloc::onEditListClicked,
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSortFilterSheet = true }) {
                            if (activeCount > 0) {
                                BadgedBox(badge = { Badge { Text("$activeCount") } }) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription =
                                            stringResource(Res.string.grocery_filter),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = stringResource(Res.string.grocery_filter),
                                )
                            }
                        }
                        IconButton(onClick = bloc::onDeleteClicked) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription =
                                    stringResource(Res.string.grocery_delete_items),
                            )
                        }
                        if (state.isSyncing) {
                            PlusLoadingIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 4.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            PlusOnboardingTooltip(
                                text = Res.string.grocery_sync_onboarding.asTextData(),
                                visible = state.showSyncTooltip,
                                onDismiss = bloc::onSyncTooltipDismissed,
                                placement = PlusTooltipPlacement.BELOW,
                            ) {
                                IconButton(onClick = bloc::onSyncClicked) {
                                    Icon(
                                        Icons.Default.Sync,
                                        contentDescription =
                                            stringResource(Res.string.grocery_sync_all),
                                    )
                                }
                            }
                        }
                    },
                )
                state.pendingInvitations.forEach { invite ->
                    GroceryInviteBanner(
                        invite = invite,
                        onAccept = { bloc.onAcceptInvitation(invite) },
                        onReject = { bloc.onRejectInvitation(invite) },
                    )
                }
                val itemLookup =
                    remember(state.groupedItems) {
                        state.groupedItems.flatMap { it.items }.associateBy { it.id }
                    }
                PullToRefreshBox(
                    isRefreshing = state.isSyncing,
                    onRefresh = bloc::onSyncClicked,
                    modifier = Modifier.weight(1f),
                ) {
                    val hasNoItems = state.groupedItems.isEmpty()
                    val filtersApplied =
                        state.filter != GroceryListBloc.GroceryFilter.ALL ||
                            state.recipeFilter != null
                    val showEmptyState = hasNoItems && !filtersApplied && !state.isSyncing
                    val showFilteredEmptyState = hasNoItems && filtersApplied && !state.isSyncing
                    if (showEmptyState) {
                        EmptyGroceryListState(
                            onBrowseRecipesClicked = bloc::onBrowseRecipesClicked,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (showFilteredEmptyState) {
                        FilteredEmptyGroceryListState(
                            onClearFiltersClicked = bloc::onClearFiltersClicked,
                            onBrowseRecipesClicked = bloc::onBrowseRecipesClicked,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        GroceryGroupedList(
                            groups =
                                state.groupedItems.map { group ->
                                    GroceryDisplayGroup(
                                        category = group.category,
                                        items =
                                            group.items.map { item ->
                                                GroceryDisplayItem(
                                                    key = item.id,
                                                    displayName = item.displayName,
                                                    quantity = item.quantity,
                                                    isChecked = item.isChecked,
                                                    recipeName = item.recipeName,
                                                )
                                            },
                                    )
                                },
                            onItemClick = { key ->
                                itemLookup[key as Long]?.let { bloc.onGroceryItemClicked(it) }
                            },
                            onCheckedChange = { key ->
                                itemLookup[key as Long]?.let {
                                    bloc.onGroceryItemCheckedChange(it, !it.isChecked)
                                }
                            },
                            modifier =
                                Modifier.fillMaxSize()
                                    .nestedScroll(dismissKeyboardOnScroll)
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = { focusManager.clearFocus() })
                                    },
                            state = listState,
                            showHeaders = state.sort == GroceryListBloc.GrocerySort.AISLE,
                            highlightedKey = highlightedItemKey,
                            trailingContent = { displayItem ->
                                val item = itemLookup[displayItem.key as Long]
                                if (item != null) {
                                    GroceryItemTrailingContent(
                                        item = item,
                                        onDeleteClick = { bloc.onGroceryItemDelete(item) },
                                        // Where the row can be swiped away, the always-visible
                                        // delete button sits right under a thumb and is too easy
                                        // to hit by accident; the swipe is the deliberate gesture.
                                        showDeleteButton = !supportsSwipeToDelete,
                                    )
                                }
                            },
                            swipeToDeleteEnabled = supportsSwipeToDelete,
                            onSwipeToDelete = { displayItem ->
                                itemLookup[displayItem.key as Long]?.let(bloc::onGroceryItemDelete)
                            },
                        )
                    }
                }
                if (state.currentUserRole != ListRole.VIEWER) {
                    GroceryListInput(
                        name = bloc.newGroceryItemName,
                        suggestions = state.autocompleteSuggestions,
                        queryMatchesSavedAutocomplete = state.queryMatchesSavedAutocomplete,
                        onNameChange = bloc::onNewGroceryItemNameChange,
                        onAddClick = {
                            awaitingAddedItem = true
                            bloc.saveGroceryItem()
                        },
                        onSaveAutocompleteItem = bloc::onSaveAutocompleteItem,
                        forceShowSuggestions = forceShowAutocompleteSuggestions,
                        // The add row is anchored at the bottom of the screen, where the floating
                        // nav pill now sits. It can't scroll under the pill like the list does, so
                        // it takes real padding. The pill slides away with the keyboard, so this
                        // animates to zero and the row lands directly on the keyboard.
                        modifier = Modifier.padding(bottom = LocalBottomNavBarInset.current),
                    )
                }
            },
        )

        if (showSortFilterSheet) {
            GrocerySortFilterBottomSheet(
                currentSort = state.sort,
                currentFilter = state.filter,
                currentRecipeFilter = state.recipeFilter,
                availableRecipes = state.availableRecipes,
                hasNoRecipeItems = state.hasNoRecipeItems,
                onApply = { sort, filter, recipeFilter ->
                    bloc.onApplySortAndFilter(sort, filter, recipeFilter)
                    showSortFilterSheet = false
                },
                onDismiss = { showSortFilterSheet = false },
            )
        }

        if (!useDropdownSelector && state.showListSelector) {
            GroceryListSelectorSheet(
                state = state,
                onDismiss = bloc::onListSelectorDismissed,
                onListSelected = bloc::onListSelected,
                onCreateListClicked = bloc::onCreateListClicked,
                onEditListClicked = bloc::onEditListClicked,
            )
        }

        if (state.showCreateListDialog) {
            CreateListDialog(
                onDismiss = bloc::onCreateListDismissed,
                onConfirm = bloc::onCreateListConfirmed,
            )
        }

        if (state.showDeleteDialog) {
            DeleteItemsDialog(
                onDismiss = bloc::onDeleteDismissed,
                onDeletePurchased = bloc::onDeletePurchasedConfirmed,
                onDeleteAll = bloc::onDeleteAllConfirmed,
            )
        }

        GroceryDetailSheet(bloc = bloc)
    }
}

/**
 * Flattened index of the item with [id] within the lazy list, accounting for the per-category
 * sticky headers when [showHeaders] is set. Mirrors the layout built in [GroceryGroupedList].
 * Returns -1 when the item isn't present.
 */
private fun List<GroceryListBloc.GroceryGroup>.flatIndexOfItem(
    id: Long,
    showHeaders: Boolean,
): Int {
    var index = 0
    forEach { group ->
        if (showHeaders) index++
        group.items.forEach { item ->
            if (item.id == id) return index
            index++
        }
    }
    return -1
}

@Composable
private fun GroceryInviteBanner(
    invite: GroceryListInvite,
    onAccept: () -> Unit,
    onReject: () -> Unit,
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
                            Res.string.grocery_pending_invite_message,
                            "list" to FixedString(invite.listName),
                        )
                        .localized(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = ChefMateTheme.dimens.paddingSmall),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onReject) { Text(stringResource(Res.string.grocery_reject)) }
                TextButton(onClick = onAccept) { Text(stringResource(Res.string.grocery_accept)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryDetailSheet(bloc: GroceryListBloc) {
    val slot = bloc.childSlot.subscribeAsState()
    val child = slot.value.child?.instance
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sheetChild by remember { mutableStateOf(child) }
    if (child != null) {
        sheetChild = child
    }

    LaunchedEffect(child) {
        if (child == null && sheetChild != null) {
            sheetState.hide()
            sheetChild = null
        }
    }

    val active = sheetChild
    if (active != null) {
        PlusResponsiveModal(
            onDismissRequest = bloc::onDismissSheet,
            sheetState = sheetState,
            title = Res.string.grocery_detail.asTextData(),
            onCloseClick = bloc::onDismissSheet,
        ) {
            active.bloc.Content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GrocerySortFilterBottomSheet(
    currentSort: GroceryListBloc.GrocerySort,
    currentFilter: GroceryListBloc.GroceryFilter,
    currentRecipeFilter: String?,
    availableRecipes: List<String>,
    hasNoRecipeItems: Boolean,
    onApply:
        (
            sort: GroceryListBloc.GrocerySort,
            filter: GroceryListBloc.GroceryFilter,
            recipeFilter: String?,
        ) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedSort by remember { mutableStateOf(currentSort) }
    var selectedFilter by remember { mutableStateOf(currentFilter) }
    var selectedRecipeFilter by remember { mutableStateOf(currentRecipeFilter) }

    val showRecipeSection = availableRecipes.isNotEmpty() || hasNoRecipeItems
    val hasActiveFilters =
        selectedSort != GroceryListBloc.GrocerySort.AISLE ||
            selectedFilter != GroceryListBloc.GroceryFilter.ALL ||
            selectedRecipeFilter != null

    val dimens = ChefMateTheme.dimens
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.padding(horizontal = dimens.paddingNormal).navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.grocery_sort_and_filter),
                    style = MaterialTheme.typography.titleLarge,
                )
                if (hasActiveFilters) {
                    TextButton(
                        onClick = {
                            selectedSort = GroceryListBloc.GrocerySort.AISLE
                            selectedFilter = GroceryListBloc.GroceryFilter.ALL
                            selectedRecipeFilter = null
                        }
                    ) {
                        Text(stringResource(Res.string.grocery_clear_filters))
                    }
                }
            }
            Spacer(Modifier.height(dimens.paddingNormal))

            Text(
                text = stringResource(Res.string.grocery_sort_by),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(dimens.paddingSmall))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall)) {
                GroceryListBloc.GrocerySort.entries.forEach { option ->
                    FilterChip(
                        selected = option == selectedSort,
                        onClick = { selectedSort = option },
                        label = {
                            Text(
                                when (option) {
                                    GroceryListBloc.GrocerySort.AISLE ->
                                        stringResource(Res.string.grocery_sort_aisle)
                                    GroceryListBloc.GrocerySort.ALPHABETICAL ->
                                        stringResource(Res.string.grocery_sort_alphabetical)
                                }
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(dimens.paddingNormal))

            Text(
                text = stringResource(Res.string.grocery_filter_by),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(dimens.paddingSmall))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall)) {
                GroceryListBloc.GroceryFilter.entries.forEach { filterOption ->
                    FilterChip(
                        selected = filterOption == selectedFilter,
                        onClick = { selectedFilter = filterOption },
                        label = {
                            Text(
                                when (filterOption) {
                                    GroceryListBloc.GroceryFilter.ALL ->
                                        stringResource(Res.string.grocery_filter_all)
                                    GroceryListBloc.GroceryFilter.UNPURCHASED ->
                                        stringResource(Res.string.grocery_filter_unpurchased)
                                    GroceryListBloc.GroceryFilter.PURCHASED ->
                                        stringResource(Res.string.grocery_filter_purchased)
                                }
                            )
                        },
                    )
                }
            }

            if (showRecipeSection) {
                Spacer(Modifier.height(dimens.paddingNormal))
                Text(
                    text = stringResource(Res.string.grocery_filter_by_recipe),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(dimens.paddingSmall))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall)) {
                    availableRecipes.forEach { recipe ->
                        FilterChip(
                            selected = selectedRecipeFilter == recipe,
                            onClick = {
                                selectedRecipeFilter =
                                    if (selectedRecipeFilter == recipe) null else recipe
                            },
                            label = { Text(recipe) },
                        )
                    }
                    if (hasNoRecipeItems) {
                        FilterChip(
                            selected = selectedRecipeFilter == GroceryListBloc.NO_RECIPE_FILTER,
                            onClick = {
                                selectedRecipeFilter =
                                    if (selectedRecipeFilter == GroceryListBloc.NO_RECIPE_FILTER) {
                                        null
                                    } else {
                                        GroceryListBloc.NO_RECIPE_FILTER
                                    }
                            },
                            label = { Text(stringResource(Res.string.grocery_filter_no_recipe)) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(dimens.paddingNormal))
            Button(
                onClick = { onApply(selectedSort, selectedFilter, selectedRecipeFilter) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.grocery_apply))
            }
            Spacer(Modifier.height(dimens.paddingNormal))
        }
    }
}

/** Phone/medium presentation of the list selector: a modal bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryListSelectorSheet(
    state: GroceryListBloc.Model,
    onDismiss: () -> Unit,
    onListSelected: (GroceryListModel) -> Unit,
    onCreateListClicked: () -> Unit,
    onEditListClicked: (GroceryListModel) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        GroceryListSelectorContent(
            state = state,
            onDismiss = onDismiss,
            onListSelected = onListSelected,
            onCreateListClicked = onCreateListClicked,
            onEditListClicked = onEditListClicked,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** Tablet/desktop presentation: a dropdown anchored to the title in the top bar. */
@Composable
private fun GroceryListSelectorDropdown(
    expanded: Boolean,
    state: GroceryListBloc.Model,
    onDismiss: () -> Unit,
    onListSelected: (GroceryListModel) -> Unit,
    onCreateListClicked: () -> Unit,
    onEditListClicked: (GroceryListModel) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        GroceryListSelectorContent(
            state = state,
            onDismiss = onDismiss,
            onListSelected = onListSelected,
            onCreateListClicked = onCreateListClicked,
            onEditListClicked = onEditListClicked,
        )
    }
}

/** Shared rows for both selector presentations (My Lists / Shared with me + create). */
@Composable
private fun ColumnScope.GroceryListSelectorContent(
    state: GroceryListBloc.Model,
    onDismiss: () -> Unit,
    onListSelected: (GroceryListModel) -> Unit,
    onCreateListClicked: () -> Unit,
    onEditListClicked: (GroceryListModel) -> Unit,
) {
    val myLists = state.lists.filter { !it.isShared }
    val sharedLists = state.lists.filter { it.isShared }

    Text(
        text = stringResource(Res.string.grocery_my_lists),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
    myLists.forEach { list ->
        GroceryListSelectorItem(
            list = list,
            isSelected = list.id == state.selectedList?.id,
            onListSelected = onListSelected,
            onEditListClicked = {
                onDismiss()
                onEditListClicked(it)
            },
        )
    }
    HorizontalDivider()
    ListItem(
        headlineContent = { Text(stringResource(Res.string.grocery_create_new_list)) },
        modifier =
            Modifier.clickable {
                onDismiss()
                onCreateListClicked()
            },
        leadingContent = {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        },
    )
    if (sharedLists.isNotEmpty()) {
        HorizontalDivider()
        Text(
            text = stringResource(Res.string.grocery_shared_with_you),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        sharedLists.forEach { list ->
            GroceryListSelectorItem(
                list = list,
                isSelected = list.id == state.selectedList?.id,
                onListSelected = onListSelected,
                onEditListClicked = {
                    onDismiss()
                    onEditListClicked(it)
                },
            )
        }
    }
}

@Composable
private fun CreateListDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var listName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.grocery_create_list_title)) },
        text = {
            PlusTextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text(stringResource(Res.string.grocery_create_list_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(listName) }, enabled = listName.isNotBlank()) {
                Text(stringResource(Res.string.grocery_create_list_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.grocery_create_list_cancel))
            }
        },
    )
}

@Composable
private fun DeleteItemsDialog(
    onDismiss: () -> Unit,
    onDeletePurchased: () -> Unit,
    onDeleteAll: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.grocery_delete_items_title)) },
        text = { Text(stringResource(Res.string.grocery_delete_items_message)) },
        confirmButton = {
            TextButton(onClick = onDeleteAll) {
                Text(stringResource(Res.string.grocery_delete_all))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.grocery_cancel)) }
                TextButton(onClick = onDeletePurchased) {
                    Text(stringResource(Res.string.grocery_delete_purchased))
                }
            }
        },
    )
}

@Composable
private fun GroceryListInput(
    name: StateFlow<String>,
    suggestions: List<String>,
    queryMatchesSavedAutocomplete: Boolean,
    onNameChange: (String) -> Unit,
    onAddClick: () -> Unit,
    onSaveAutocompleteItem: (String) -> Unit,
    modifier: Modifier = Modifier,
    forceShowSuggestions: Boolean = false,
) {
    val state = name.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val trimmedQuery = state.value.trim()
    // Offer to save the typed text only when it isn't already one of the suggestions (a saved
    // custom item or a built-in already suggests it, so re-saving would be redundant) and it isn't
    // already in the saved autocomplete library.
    val saveQuery = trimmedQuery.takeIf { q ->
        q.isNotEmpty() &&
            !queryMatchesSavedAutocomplete &&
            suggestions.none { it.equals(q, ignoreCase = true) }
    }
    // Hide the whole dropdown once the entered text matches a saved library item — there is nothing
    // left to suggest or save. This is also what collapses the menu right after the user saves.
    val expanded =
        (isFocused || forceShowSuggestions) &&
            !queryMatchesSavedAutocomplete &&
            (suggestions.isNotEmpty() || saveQuery != null)
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    Column(modifier = modifier) {
        // Suggestions render directly above the field rather than as a downward dropdown: the input
        // is anchored at the bottom of the screen, so a dropdown would open behind the keyboard
        // (especially on iOS, where it doesn't flip above the anchor). They sit above the
        // field/Done row so the Done button only centers against the field, not the suggestion
        // list.
        if (expanded) {
            AutocompleteSuggestionRows(
                suggestions = suggestions,
                onNameChange = onNameChange,
                saveQuery = saveQuery,
                onSaveClick = { saveQuery?.let(onSaveAutocompleteItem) },
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroceryItemNameTextField(
                value = state.value,
                onValueChange = onNameChange,
                onFocusChanged = { isFocused = it },
                onAddClick = onAddClick,
                onDismissKeyboard = dismissKeyboard,
                modifier = Modifier.weight(1f),
            )
            AnimatedVisibility(
                visible = isFocused,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally(),
            ) {
                // Done finishes what the user was typing: anything left in the field is added
                // before the keyboard goes away, so half-typed text isn't silently abandoned.
                TextButton(
                    onClick = {
                        if (trimmedQuery.isNotEmpty()) onAddClick()
                        dismissKeyboard()
                    },
                    modifier = Modifier.testTag(GroceryListTestTags.DONE_BUTTON),
                ) {
                    Text(stringResource(Res.string.grocery_done))
                }
            }
        }
    }
}

@Composable
private fun AutocompleteSuggestionRows(
    suggestions: List<String>,
    onNameChange: (String) -> Unit,
    saveQuery: String?,
    onSaveClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall,
        shadowElevation = ChefMateTheme.dimens.paddingExtraSmall,
        tonalElevation = ChefMateTheme.dimens.paddingExtraSmall,
    ) {
        Column {
            suggestions.forEach { suggestion ->
                ListItem(
                    headlineContent = { Text(suggestion) },
                    // canFocus = false keeps the row clickable without stealing focus from the text
                    // field. Otherwise tapping a suggestion unfocuses the field, which collapses
                    // the
                    // list (it's gated on focus) and removes the row before the tap can register.
                    modifier =
                        Modifier.focusProperties { canFocus = false }
                            .clickable { onNameChange(suggestion) }
                            .testTag(GroceryListTestTags.ITEM_SUGGESTION),
                )
            }
            if (saveQuery != null) {
                val saveLabel =
                    PhraseModel(
                            Res.string.grocery_save_autocomplete,
                            "item" to FixedString(saveQuery),
                        )
                        .localized()
                ListItem(
                    headlineContent = { Text(saveLabel) },
                    leadingContent = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    },
                    // Same canFocus = false rationale as the suggestion rows above.
                    modifier =
                        Modifier.focusProperties { canFocus = false }
                            .clickable { onSaveClick() }
                            .testTag(GroceryListTestTags.SAVE_AUTOCOMPLETE),
                )
            }
        }
    }
}

@Composable
private fun GroceryItemNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onAddClick: () -> Unit,
    onDismissKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    // The name is owned externally (the BLoC). When it changes from outside the field — a
    // suggestion tap or the clear after adding — sync it in and move the cursor to the end. Normal
    // typing keeps text and value equal, so this leaves the user's cursor alone.
    LaunchedEffect(value) {
        if (value != fieldValue.text) {
            fieldValue = TextFieldValue(value, TextRange(value.length))
        }
    }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = {
            fieldValue = it
            onValueChange(it.text)
        },
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .testTag(GroceryListTestTags.ITEM_INPUT),
        singleLine = true,
        placeholder = { Text(stringResource(Res.string.grocery_add_item_hint)) },
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
        // The IME action (or Enter on desktop) is the only way to submit — there is no in-field add
        // button. Submitting keeps the keyboard open so the user can keep entering items. On an
        // empty field the action instead dismisses the keyboard, matching the Done button and
        // scroll-to-dismiss gestures.
        keyboardActions =
            KeyboardActions(
                onSend = {
                    if (fieldValue.text.isNotBlank()) onAddClick() else onDismissKeyboard()
                }
            ),
    )
}

@Composable
private fun GroceryItemTrailingContent(
    item: GroceryItem,
    onDeleteClick: () -> Unit,
    showDeleteButton: Boolean,
) {
    val syncingDescription = stringResource(Res.string.grocery_sync_syncing)
    // Without the delete button the sync icon is last in the row, and it carries none of the
    // padding an IconButton builds in — so it needs its own inset off the trailing edge.
    Box(
        modifier =
            if (showDeleteButton) {
                Modifier
            } else {
                Modifier.padding(end = ChefMateTheme.dimens.paddingNormal)
            }
    ) {
        when (item.syncStatus) {
            SyncStatus.NOT_SYNCED ->
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = stringResource(Res.string.grocery_sync_not_synced),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            SyncStatus.SYNCING ->
                PlusLoadingIndicator(
                    modifier = Modifier.size(16.dp),
                    contentDescription = syncingDescription,
                    strokeWidth = 2.dp,
                )
            SyncStatus.SYNCED ->
                Icon(
                    imageVector = Icons.Outlined.CloudDone,
                    contentDescription = stringResource(Res.string.grocery_sync_synced),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
        }
    }

    if (showDeleteButton) {
        IconButton(onClick = onDeleteClick) {
            Icon(Icons.Default.Delete, stringResource(Res.string.grocery_delete_item))
        }
    }
}

@Composable
private fun EmptyGroceryListState(
    onBrowseRecipesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ChefMateTheme.dimens
    // Allow the centered content to scroll when the viewport is too short (e.g. landscape) so
    // nothing gets cut off; heightIn(min = maxHeight) keeps it vertically centered when it fits.
    BoxWithConstraints(modifier = modifier.testTag(GroceryListTestTags.EMPTY_STATE)) {
        Column(
            modifier =
                Modifier.verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(dimens.paddingNormal))
            Text(
                text = stringResource(Res.string.grocery_list_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(dimens.paddingSmall))
            Text(
                text = stringResource(Res.string.grocery_list_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimens.paddingLarge))
            Button(
                onClick = onBrowseRecipesClicked,
                modifier =
                    Modifier.testTag(GroceryListTestTags.BROWSE_RECIPES_BUTTON).fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.grocery_list_empty_browse_recipes))
            }
        }
    }
}

/**
 * Shown when the list has items but the active purchase/recipe filters leave nothing visible. Lets
 * the user recover by clearing filters (primary) or jumping to recipes (secondary) instead of
 * staring at a blank screen.
 */
@Composable
private fun FilteredEmptyGroceryListState(
    onClearFiltersClicked: () -> Unit,
    onBrowseRecipesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ChefMateTheme.dimens
    // Allow the centered content to scroll when the viewport is too short (e.g. landscape) so
    // nothing gets cut off; heightIn(min = maxHeight) keeps it vertically centered when it fits.
    BoxWithConstraints(modifier = modifier.testTag(GroceryListTestTags.FILTERED_EMPTY_STATE)) {
        Column(
            modifier =
                Modifier.verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .fillMaxWidth()
                    .padding(horizontal = dimens.paddingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(dimens.paddingNormal))
            Text(
                text = stringResource(Res.string.grocery_list_filtered_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(dimens.paddingSmall))
            Text(
                text = stringResource(Res.string.grocery_list_filtered_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(dimens.paddingLarge))
            Button(
                onClick = onClearFiltersClicked,
                modifier =
                    Modifier.testTag(GroceryListTestTags.CLEAR_FILTERS_BUTTON).fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.grocery_list_filtered_empty_clear_filters))
            }
            Spacer(Modifier.height(dimens.paddingSmall))
            OutlinedButton(
                onClick = onBrowseRecipesClicked,
                modifier =
                    Modifier.testTag(GroceryListTestTags.BROWSE_RECIPES_BUTTON).fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.grocery_list_empty_browse_recipes))
            }
        }
    }
}

@Composable
private fun GroceryListSelectorItem(
    list: GroceryListModel,
    isSelected: Boolean,
    onListSelected: (GroceryListModel) -> Unit,
    onEditListClicked: (GroceryListModel) -> Unit,
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = list.name,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                if (list.isShared) {
                    Text(
                        text = stringResource(Res.string.grocery_shared_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        },
        modifier = Modifier.clickable { onListSelected(list) },
        trailingContent = {
            IconButton(onClick = { onEditListClicked(list) }) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.grocery_edit_list),
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

package com.plusmobileapps.chefmate.recipe.list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.RiceBowl
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.Tapas
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import chefmate.client.recipe.list.public.generated.resources.Res
import chefmate.client.recipe.list.public.generated.resources.recipe_list_clear_filters
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_all_recipes
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_cancel
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_categories
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_clear
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_placeholder
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_quick_filters
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_results
import chefmate.client.recipe.list.public.generated.resources.recipe_list_search_scope_book
import com.arkivanov.essenty.backhandler.BackCallback
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.ui.components.LocalBottomNavBarInset
import com.plusmobileapps.chefmate.ui.components.PlusLoadingIndicator
import com.plusmobileapps.chefmate.ui.components.PlusTextField
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

/**
 * The recipe list's search screen, shown over the list when the header's search button is tapped.
 * Before anything is typed or picked it leads with quick filters and categories; once there is a
 * query or an active filter those collapse into a chip row pinned at the top and the matching
 * recipes appear beneath it, updating as the user types.
 *
 * Query and filters live in [RecipeListBloc], so the results are the same list the screen shows;
 * [RecipeListScreen] restores the pre-search filters when this is cancelled.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RecipeSearchScreen(
    state: RecipeListBloc.Model,
    bloc: RecipeListBloc,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRefining = state.searchQuery.isNotEmpty() || state.totalActiveFilterCount > 0
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.fillMaxSize().testTag(RecipeListTestTags.SEARCH_SCREEN),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchHeader(state = state, bloc = bloc, onCancel = onCancel)
            AnimatedContent(
                targetState = isRefining,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "recipe_search_content",
            ) { refining ->
                val listState = rememberLazyListState()
                // Scrolling the results or filters is the cue to put the keyboard away.
                LaunchedEffect(listState.isScrollInProgress) {
                    if (listState.isScrollInProgress) focusManager.clearFocus()
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top = ChefMateTheme.dimens.paddingSmall,
                            bottom =
                                ChefMateTheme.dimens.paddingNormal + LocalBottomNavBarInset.current,
                        ),
                ) {
                    if (refining) {
                        stickyHeader(key = "chips") { FilterChipRow(state = state, bloc = bloc) }
                        results(state = state, bloc = bloc)
                    } else {
                        item(key = "quick") { QuickFilters(state = state, bloc = bloc) }
                        item(key = "categories") { Categories(state = state, bloc = bloc) }
                    }
                }
            }
        }
    }
}

/**
 * Lets the system back gesture close the search screen instead of leaving the tab. Registered on
 * the BLoC's Essenty back handler, so it outranks the tab stack while [isEnabled] and works on
 * every platform Decompose drives back for.
 */
@Composable
internal fun SearchBackHandler(bloc: RecipeListBloc, isEnabled: Boolean, onBack: () -> Unit) {
    val currentOnBack by rememberUpdatedState(onBack)
    val callback = remember { BackCallback(isEnabled = isEnabled) { currentOnBack() } }
    callback.isEnabled = isEnabled
    DisposableEffect(bloc) {
        bloc.backHandler.register(callback)
        onDispose { bloc.backHandler.unregister(callback) }
    }
}

/** The list's filters and recipe book at the moment search opened. */
internal class SearchFilterSnapshot(state: RecipeListBloc.Model) {
    private val sort = state.currentSort
    private val filters = state.activeFilters
    private val categories = state.activeCategories
    private val userCategoryIds = state.activeUserCategoryIds
    /** The open book, or null for the cross-book "All recipes" view. */
    private val bookId = if (state.isAllRecipesSelected) null else state.activeBook?.id

    /** Re-applies this snapshot, touching only what changed since it was taken. */
    fun restore(bloc: RecipeListBloc, current: RecipeListBloc.Model) {
        val now = SearchFilterSnapshot(current)
        if (now.bookId != bookId && bookId != null) bloc.onBookSelected(bookId)
        if (
            now.filters != filters ||
                now.categories != categories ||
                now.userCategoryIds != userCategoryIds
        ) {
            bloc.onApplySortAndFilters(sort, filters, categories, userCategoryIds)
        }
    }
}

// region Header

@Composable
private fun SearchHeader(state: RecipeListBloc.Model, bloc: RecipeListBloc, onCancel: () -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    start = ChefMateTheme.dimens.paddingNormal,
                    end = ChefMateTheme.dimens.paddingSmall,
                    top = ChefMateTheme.dimens.paddingSmall,
                ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlusTextField(
                value = state.searchQuery,
                onValueChange = bloc::onSearchQueryChanged,
                modifier = Modifier.weight(1f).testTag(RecipeListTestTags.SEARCH_FIELD),
                focusRequester = focusRequester,
                placeholder = { Text(stringResource(Res.string.recipe_list_search_placeholder)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
            )
            // The clear control sits beside the field, not in its trailing slot: on iOS the native
            // text input covers the field's bounds and swallows taps on anything drawn inside it.
            if (state.searchQuery.isNotEmpty()) {
                IconButton(onClick = { bloc.onSearchQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.recipe_list_search_clear),
                    )
                }
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(RecipeListTestTags.SEARCH_CANCEL),
            ) {
                Text(stringResource(Res.string.recipe_list_search_cancel))
            }
        }
        val activeBook = state.activeBook
        if (activeBook != null && !state.isAllRecipesSelected) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        stringResource(Res.string.recipe_list_search_scope_book)
                            .replace("{book}", activeBook.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = bloc::onAllRecipesSelected) {
                    Text(
                        stringResource(Res.string.recipe_list_search_all_recipes),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

// endregion

// region Browse layout

/** The three one-tap filters as large tiles, so they read as the fastest way in. */
@Composable
private fun QuickFilters(state: RecipeListBloc.Model, bloc: RecipeListBloc) {
    Column(modifier = Modifier.padding(bottom = ChefMateTheme.dimens.paddingLarge)) {
        SectionTitle(stringResource(Res.string.recipe_list_search_quick_filters))
        Row(
            modifier = Modifier.padding(horizontal = ChefMateTheme.dimens.paddingNormal),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RecipeFilterOption.entries.forEach { option ->
                QuickFilterTile(
                    title = stringResource(option.labelRes()),
                    icon = option.searchIcon(),
                    container = option.tileContainerColor(),
                    content = option.tileContentColor(),
                    onClick = {
                        bloc.applyFilters(state, filters = state.activeFilters.toggle(option))
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Categories(state: RecipeListBloc.Model, bloc: RecipeListBloc) {
    Column {
        SectionTitle(stringResource(Res.string.recipe_list_search_categories))
        FlowRow(
            modifier = Modifier.padding(horizontal = ChefMateTheme.dimens.paddingNormal),
            horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
            verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
        ) {
            BuiltinCategory.entries.forEach { category ->
                SearchFilterChip(
                    label = stringResource(category.labelRes()),
                    icon = category.searchIcon(),
                    selected = false,
                    onClick = {
                        bloc.applyFilters(
                            state,
                            categories = state.activeCategories.toggle(category),
                        )
                    },
                )
            }
            state.availableUserCategories.forEach { category ->
                SearchFilterChip(
                    label = category.name,
                    icon = Icons.Outlined.Sell,
                    selected = false,
                    onClick = {
                        bloc.applyFilters(
                            state,
                            userCategoryIds = state.activeUserCategoryIds.toggle(category.id),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun QuickFilterTile(
    title: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(container, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = content)
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// endregion

// region Refining layout

private data class SearchChip(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/** Every filter as one scrollable chip row, the active ones first so they stay in view. */
@Composable
private fun FilterChipRow(state: RecipeListBloc.Model, bloc: RecipeListBloc) {
    val quick =
        RecipeFilterOption.entries.map { option ->
            SearchChip(
                key = "f-${option.name}",
                label = stringResource(option.labelRes()),
                icon = option.searchIcon(),
                selected = option in state.activeFilters,
                onClick = {
                    bloc.applyFilters(state, filters = state.activeFilters.toggle(option))
                },
            )
        }
    val builtin =
        BuiltinCategory.entries.map { category ->
            SearchChip(
                key = "c-${category.name}",
                label = stringResource(category.labelRes()),
                icon = category.searchIcon(),
                selected = category in state.activeCategories,
                onClick = {
                    bloc.applyFilters(state, categories = state.activeCategories.toggle(category))
                },
            )
        }
    val user =
        state.availableUserCategories.map { category ->
            SearchChip(
                key = "u-${category.id}",
                label = category.name,
                icon = Icons.Outlined.Sell,
                selected = category.id in state.activeUserCategoryIds,
                onClick = {
                    bloc.applyFilters(
                        state,
                        userCategoryIds = state.activeUserCategoryIds.toggle(category.id),
                    )
                },
            )
        }
    val all = quick + builtin + user
    val ordered = all.filter { it.selected } + all.filterNot { it.selected }

    val hasActiveFilters = state.totalActiveFilterCount > 0
    val rowState = rememberLazyListState()
    // A picked chip jumps to the front, so bring the front back into view to show it landed.
    val selectedKeys = ordered.filter { it.selected }.map { it.key }
    LaunchedEffect(selectedKeys) { rowState.animateScrollToItem(0) }

    Row(
        modifier =
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = ChefMateTheme.dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Pinned outside the scrolling row so it is always reachable while any filter is on.
        if (hasActiveFilters) {
            IconButton(
                onClick = bloc::onClearFilters,
                modifier =
                    Modifier.padding(start = ChefMateTheme.dimens.paddingNormal)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.recipe_list_clear_filters),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        LazyRow(
            state = rowState,
            modifier = Modifier.weight(1f),
            contentPadding =
                PaddingValues(
                    start =
                        if (hasActiveFilters) ChefMateTheme.dimens.paddingSmall
                        else ChefMateTheme.dimens.paddingNormal,
                    end = ChefMateTheme.dimens.paddingNormal,
                ),
            horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items(ordered, key = { it.key }) { chip ->
                SearchFilterChip(
                    label = chip.label,
                    icon = chip.icon,
                    selected = chip.selected,
                    onClick = chip.onClick,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.results(
    state: RecipeListBloc.Model,
    bloc: RecipeListBloc,
) {
    when {
        state.recipes.isEmpty() && state.isLoading -> {
            item(key = "loading") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PlusLoadingIndicator()
                }
            }
        }
        state.recipes.isEmpty() -> {
            item(key = "empty") {
                SearchEmptyState(
                    canSearchAllRecipes = !state.isAllRecipesSelected,
                    onSearchAllRecipes = bloc::onAllRecipesSelected,
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                )
            }
        }
        else -> {
            item(key = "results-header") {
                Row(
                    modifier = Modifier.padding(top = ChefMateTheme.dimens.paddingSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionTitle(stringResource(Res.string.recipe_list_search_results))
                    Text(
                        text = state.recipes.size.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
            }
            items(state.recipes.size, key = { state.recipes[it].id }) { index ->
                val recipe = state.recipes[index]
                Column(modifier = Modifier.animateItem()) {
                    RecipeListItemContent(
                        recipe = recipe,
                        onClick = { bloc.onRecipeClicked(recipe) },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = ChefMateTheme.dimens.paddingNormal),
                    )
                }
            }
        }
    }
}

// endregion

// region Pieces

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier =
            Modifier.padding(
                start = ChefMateTheme.dimens.paddingNormal,
                end = ChefMateTheme.dimens.paddingSmall,
                bottom = 12.dp,
            ),
    )
}

@Composable
private fun SearchFilterChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = if (selected) Icons.Default.Check else icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize),
            )
        },
        modifier = modifier.height(36.dp),
    )
}

/** Applies a change to one filter group, keeping the sort and the other groups as they are. */
private fun RecipeListBloc.applyFilters(
    state: RecipeListBloc.Model,
    filters: Set<RecipeFilterOption> = state.activeFilters,
    categories: Set<BuiltinCategory> = state.activeCategories,
    userCategoryIds: Set<Long> = state.activeUserCategoryIds,
) {
    onApplySortAndFilters(state.currentSort, filters, categories, userCategoryIds)
}

private fun <T> Set<T>.toggle(value: T): Set<T> = if (value in this) this - value else this + value

private fun RecipeFilterOption.searchIcon(): ImageVector =
    when (this) {
        RecipeFilterOption.FAVORITES -> Icons.Default.Favorite
        RecipeFilterOption.RATED -> Icons.Default.Star
        RecipeFilterOption.QUICK_RECIPES -> Icons.Outlined.Timer
    }

@Composable
private fun RecipeFilterOption.tileContainerColor(): Color =
    when (this) {
        RecipeFilterOption.FAVORITES -> MaterialTheme.colorScheme.errorContainer
        RecipeFilterOption.RATED -> MaterialTheme.colorScheme.tertiaryContainer
        RecipeFilterOption.QUICK_RECIPES -> MaterialTheme.colorScheme.secondaryContainer
    }

@Composable
private fun RecipeFilterOption.tileContentColor(): Color =
    when (this) {
        RecipeFilterOption.FAVORITES -> MaterialTheme.colorScheme.onErrorContainer
        RecipeFilterOption.RATED -> MaterialTheme.colorScheme.onTertiaryContainer
        RecipeFilterOption.QUICK_RECIPES -> MaterialTheme.colorScheme.onSecondaryContainer
    }

private fun BuiltinCategory.searchIcon(): ImageVector =
    when (this) {
        BuiltinCategory.BREAKFAST -> Icons.Outlined.FreeBreakfast
        BuiltinCategory.LUNCH -> Icons.Outlined.LunchDining
        BuiltinCategory.DINNER -> Icons.Outlined.DinnerDining
        BuiltinCategory.APPETIZER -> Icons.Outlined.Tapas
        BuiltinCategory.SIDE -> Icons.Outlined.RiceBowl
        BuiltinCategory.DESSERT -> Icons.Outlined.Cake
        BuiltinCategory.SNACK -> Icons.Outlined.Cookie
        BuiltinCategory.DRINK -> Icons.Outlined.LocalCafe
        BuiltinCategory.OTHER -> Icons.Outlined.Restaurant
        BuiltinCategory.AI -> Icons.Outlined.AutoAwesome
    }

// endregion

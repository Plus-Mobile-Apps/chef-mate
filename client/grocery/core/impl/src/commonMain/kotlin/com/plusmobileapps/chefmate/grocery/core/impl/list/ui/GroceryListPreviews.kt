package com.plusmobileapps.chefmate.grocery.core.impl.list.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryFilter
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryGroup
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GrocerySort
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListScreen
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryListInvite
import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.grocery.data.SyncStatus
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

private val sampleList = GroceryListModel(id = 1L, name = "My List", syncStatus = SyncStatus.SYNCED)

private val sampleGroups =
    persistentListOf(
        GroceryGroup(
            category = GroceryCategory.PRODUCE,
            items =
                persistentListOf(
                    GroceryItem(
                        id = 1L,
                        name = "**Honeycrisp** apples",
                        quantity = "6",
                        category = GroceryCategory.PRODUCE,
                        syncStatus = SyncStatus.SYNCED,
                    ),
                    GroceryItem(
                        id = 2L,
                        name = "_baby_ spinach",
                        quantity = "1 bag",
                        category = GroceryCategory.PRODUCE,
                        isChecked = true,
                        syncStatus = SyncStatus.SYNCED,
                    ),
                ),
        ),
        GroceryGroup(
            category = GroceryCategory.DAIRY,
            items =
                persistentListOf(
                    GroceryItem(
                        id = 3L,
                        name = "Whole milk",
                        quantity = "1 gal",
                        category = GroceryCategory.DAIRY,
                        syncStatus = SyncStatus.SYNCED,
                        recipeName = "Pancakes",
                    )
                ),
        ),
    )

private fun groceryListBloc(
    model: GroceryListBloc.Model,
    pendingInput: String = "",
): GroceryListBloc =
    object : GroceryListBloc {
        override val state = MutableStateFlow(model)
        override val newGroceryItemName = MutableStateFlow(pendingInput)
        override val childSlot: Value<ChildSlot<*, GroceryListBloc.Sheet>> =
            MutableValue(ChildSlot<Any, GroceryListBloc.Sheet>(null))

        override fun onGroceryItemCheckedChange(item: GroceryItem, isChecked: Boolean) = Unit

        override fun onGroceryItemDelete(item: GroceryItem) = Unit

        override fun onNewGroceryItemNameChange(name: String) = Unit

        override fun saveGroceryItem() = Unit

        override fun onSaveAutocompleteItem(name: String) = Unit

        override fun onGroceryItemClicked(item: GroceryItem) = Unit

        override fun onDismissSheet() = Unit

        override fun onSyncClicked() = Unit

        override fun onSyncTooltipDismissed() = Unit

        override fun onListSelected(list: GroceryListModel) = Unit

        override fun onCreateListClicked() = Unit

        override fun onCreateListDismissed() = Unit

        override fun onCreateListConfirmed(name: String) = Unit

        override fun onDeleteListClicked(list: GroceryListModel) = Unit

        override fun onApplySortAndFilter(
            sort: GrocerySort,
            filter: GroceryFilter,
            recipeFilter: String?,
        ) = Unit

        override fun onClearFiltersClicked() = Unit

        override fun onDeleteClicked() = Unit

        override fun onDeleteDismissed() = Unit

        override fun onDeletePurchasedConfirmed() = Unit

        override fun onDeleteAllConfirmed() = Unit

        override fun onListSelectorClicked() = Unit

        override fun onListSelectorDismissed() = Unit

        override fun onBrowseRecipesClicked() = Unit

        override fun onEditListClicked(list: GroceryListModel) = Unit

        override fun onAcceptInvitation(invite: GroceryListInvite) = Unit

        override fun onRejectInvitation(invite: GroceryListInvite) = Unit
    }

/** Grocery list with items in two categories — exercises grouped rendering + sync badges. */
val previewGroceryListBloc: GroceryListBloc =
    groceryListBloc(
        GroceryListBloc.Model(
            groupedItems = sampleGroups,
            lists = persistentListOf(sampleList),
            selectedList = sampleList,
        )
    )

private val sampleFamilyList =
    GroceryListModel(
        id = 2L,
        name = "Weekly shop",
        syncStatus = SyncStatus.SYNCED,
        familyId = "family-1",
        familyName = "The Smiths",
    )

/**
 * A family list is selected and the list picker holds personal, family, and shared lists — the
 * family lists group under the family's name.
 */
val previewGroceryListBlocFamily: GroceryListBloc =
    groceryListBloc(
        GroceryListBloc.Model(
            groupedItems = sampleGroups,
            lists =
                persistentListOf(
                    sampleList,
                    sampleFamilyList,
                    sampleFamilyList.copy(id = 3L, name = "Costco run"),
                    GroceryListModel(
                        id = 4L,
                        name = "Roommates",
                        syncStatus = SyncStatus.SYNCED,
                        isShared = true,
                    ),
                ),
            selectedList = sampleFamilyList,
        )
    )

/**
 * Empty grocery list — exercises the empty state CTA introduced alongside the "Browse my recipes"
 * jump-to-recipes flow. Should NOT appear when filters are active (see `previewGroceryListBloc`).
 */
val previewGroceryListBlocEmpty: GroceryListBloc =
    groceryListBloc(
        GroceryListBloc.Model(lists = persistentListOf(sampleList), selectedList = sampleList)
    )

/**
 * Empty list because the active filter matched nothing — exercises the filtered-empty state with
 * its "Clear filters" + "Browse my recipes" recovery CTAs. Distinct from
 * [previewGroceryListBlocEmpty], which only renders when no filter is applied.
 */
val previewGroceryListBlocFilteredEmpty: GroceryListBloc =
    groceryListBloc(
        GroceryListBloc.Model(
            groupedItems = persistentListOf(),
            filter = GroceryFilter.PURCHASED,
            availableRecipes = persistentListOf("Pancakes"),
            hasNoRecipeItems = true,
            lists = persistentListOf(sampleList),
            selectedList = sampleList,
        )
    )

/** Grocery list with autocomplete suggestions visible for screenshot coverage. */
val previewGroceryListBlocAutocomplete: GroceryListBloc =
    groceryListBloc(
        model =
            GroceryListBloc.Model(
                groupedItems = sampleGroups,
                autocompleteSuggestions =
                    persistentListOf("Strawberries", "Strawberry jam", "Strawberry yogurt"),
                lists = persistentListOf(sampleList),
                selectedList = sampleList,
            ),
        pendingInput = "stra",
    )

/**
 * The typed text exactly matches an item already saved in the autocomplete library
 * (`queryMatchesSavedAutocomplete = true`), so the suggestion dropdown must stay hidden even though
 * suggestions are present and the field is "focused". Regression coverage for #371.
 */
val previewGroceryListBlocAutocompleteSaved: GroceryListBloc =
    groceryListBloc(
        model =
            GroceryListBloc.Model(
                groupedItems = sampleGroups,
                autocompleteSuggestions = persistentListOf("Strawberry jam", "Strawberry yogurt"),
                queryMatchesSavedAutocomplete = true,
                lists = persistentListOf(sampleList),
                selectedList = sampleList,
            ),
        pendingInput = "Strawberries",
    )

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun GroceryListPreview() {
    ChefMateTheme { GroceryListScreen(bloc = previewGroceryListBloc) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun GroceryListEmptyPreview() {
    ChefMateTheme { GroceryListScreen(bloc = previewGroceryListBlocEmpty) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun GroceryListFilteredEmptyPreview() {
    ChefMateTheme { GroceryListScreen(bloc = previewGroceryListBlocFilteredEmpty) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun GroceryListAutocompletePreview() {
    ChefMateTheme {
        GroceryListScreen(
            bloc = previewGroceryListBlocAutocomplete,
            forceShowAutocompleteSuggestions = true,
        )
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun GroceryListAutocompleteSavedPreview() {
    ChefMateTheme {
        GroceryListScreen(
            bloc = previewGroceryListBlocAutocompleteSaved,
            forceShowAutocompleteSuggestions = true,
        )
    }
}

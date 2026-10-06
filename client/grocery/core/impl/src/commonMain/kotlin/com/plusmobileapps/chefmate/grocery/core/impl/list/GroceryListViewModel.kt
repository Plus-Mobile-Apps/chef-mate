@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.grocery.core.impl.list

import chefmate.client.grocery.core.public.generated.resources.Res
import chefmate.client.grocery.core.public.generated.resources.grocery_item_deleted
import chefmate.client.grocery.core.public.generated.resources.grocery_undo
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.combineStates
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryFilter
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryGroup
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GrocerySort
import com.plusmobileapps.chefmate.grocery.data.GroceryAutocompleteRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryListInvite
import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.grocery.data.IngredientParser
import com.plusmobileapps.chefmate.grocery.data.ListRole
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.toast.ToastService
import com.russhwolf.settings.Settings
import com.russhwolf.settings.string
import dev.zacsweers.metro.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
class GroceryListViewModel(
    @Main mainContext: CoroutineContext,
    private val repository: GroceryRepository,
    private val autocompleteRepository: GroceryAutocompleteRepository,
    settings: Settings,
    private val coachMarkController: CoachMarkController,
    private val toastService: ToastService,
) : ViewModel(mainContext) {
    private var sortPref by settings.string(KEY_SORT, GrocerySort.AISLE.name)
    private var filterPref by settings.string(KEY_FILTER, GroceryFilter.ALL.name)

    private val initialSort = GrocerySort.entries.find { it.name == sortPref } ?: GrocerySort.AISLE
    private val initialFilter =
        GroceryFilter.entries.find { it.name == filterPref } ?: GroceryFilter.ALL

    private val _state = MutableStateFlow(State(sort = initialSort, filter = initialFilter))
    private val _newGroceryItemName = MutableStateFlow("")
    private val selectedListId = MutableStateFlow<Long?>(null)
    private val _sort = MutableStateFlow(initialSort)
    private val _filter = MutableStateFlow(initialFilter)
    private val _recipeFilter = MutableStateFlow<String?>(null)

    // The user's saved autocomplete entries, folded into the suggestion pool below. Kept as its own
    // flow so edits from the settings screen re-trigger the suggestion combine reactively.
    private val customAutocompleteNames = MutableStateFlow<List<String>>(emptyList())

    // The sync coach mark only shows while it's the active mark in the shared controller, so at
    // most one coach mark is ever visible across the app at a time.
    val state: StateFlow<State> =
        combineStates(_state, coachMarkController.activeCoachMark) { state, activeCoachMark ->
            state.copy(showSyncTooltip = activeCoachMark == CoachMarkId.GROCERY_LIST_SYNC)
        }

    val newGroceryItemName: StateFlow<String> = _newGroceryItemName.asStateFlow()

    init {
        coachMarkController.request(CoachMarkId.GROCERY_LIST_SYNC)

        scope.launch {
            val defaultListId = repository.ensureDefaultList()
            if (selectedListId.value == null) {
                selectedListId.value = defaultListId
            }
        }

        scope.launch {
            repository.getGroceryLists().collect { lists ->
                _state.update { currentState ->
                    val currentSelectedId = selectedListId.value
                    val updatedSelected =
                        lists.firstOrNull { it.id == currentSelectedId } ?: lists.firstOrNull()
                    if (updatedSelected != null && selectedListId.value != updatedSelected.id) {
                        selectedListId.value = updatedSelected.id
                    }
                    currentState.copy(lists = lists, selectedList = updatedSelected)
                }
            }
        }

        scope.launch {
            autocompleteRepository.observeItems().collect { items ->
                customAutocompleteNames.value = items.map { it.name }
            }
        }

        scope.launch {
            // Pre-combine the query with the user's saved items so the outer combine stays within
            // the 5-arg typed overload.
            val queryWithCustom =
                combine(_newGroceryItemName, customAutocompleteNames) { query, custom ->
                    query to custom
                }
            combine(
                    selectedListId.flatMapLatest { listId ->
                        if (listId != null) {
                            repository.getGroceries(listId)
                        } else {
                            flowOf(emptyList())
                        }
                    },
                    _filter,
                    _sort,
                    _recipeFilter,
                    queryWithCustom,
                ) { items, filter, sort, recipeFilter, (newGroceryItemName, customNames) ->
                    val availableRecipes = items.mapNotNull { it.recipeName }.distinct().sorted()
                    val hasNoRecipeItems = items.any { it.recipeName == null }
                    val autocompleteSuggestions =
                        buildAutocompleteSuggestions(
                            query = newGroceryItemName,
                            items = items,
                            customItems = customNames,
                        )
                    val queryMatchesSavedAutocomplete =
                        queryMatchesSavedAutocomplete(
                            query = newGroceryItemName,
                            customItems = customNames,
                        )
                    val filtered =
                        when (filter) {
                            GroceryFilter.ALL -> items
                            GroceryFilter.UNPURCHASED -> items.filter { !it.isChecked }
                            GroceryFilter.PURCHASED -> items.filter { it.isChecked }
                        }
                    val recipeFiltered =
                        when (recipeFilter) {
                            null -> filtered
                            GroceryListBloc.NO_RECIPE_FILTER ->
                                filtered.filter { it.recipeName == null }
                            else -> filtered.filter { it.recipeName == recipeFilter }
                        }
                    val grouped =
                        when (sort) {
                            GrocerySort.AISLE ->
                                recipeFiltered
                                    .groupBy { it.category }
                                    .entries
                                    .sortedBy { it.key.ordinal }
                                    .map { (category, categoryItems) ->
                                        GroceryGroup(
                                            category = category,
                                            items = categoryItems.toImmutableList(),
                                        )
                                    }
                            GrocerySort.ALPHABETICAL ->
                                listOf(
                                        GroceryGroup(
                                            category =
                                                recipeFiltered.firstOrNull()?.category
                                                    ?: com.plusmobileapps.chefmate.grocery.data
                                                        .GroceryCategory
                                                        .OTHER,
                                            items =
                                                recipeFiltered
                                                    .sortedBy { it.displayName.lowercase() }
                                                    .toImmutableList(),
                                        )
                                    )
                                    .filter { it.items.isNotEmpty() }
                        }
                    GroceryListContent(
                        groupedItems = grouped,
                        availableRecipes = availableRecipes,
                        autocompleteSuggestions = autocompleteSuggestions,
                        queryMatchesSavedAutocomplete = queryMatchesSavedAutocomplete,
                        hasNoRecipeItems = hasNoRecipeItems,
                    )
                }
                .collect { content ->
                    _state.update {
                        it.copy(
                            groupedItems = content.groupedItems,
                            availableRecipes = content.availableRecipes,
                            autocompleteSuggestions = content.autocompleteSuggestions,
                            queryMatchesSavedAutocomplete = content.queryMatchesSavedAutocomplete,
                            hasNoRecipeItems = content.hasNoRecipeItems,
                        )
                    }
                }
        }

        scope.launch {
            repository.getPendingInvitations().collect { invitations ->
                _state.update { it.copy(pendingInvitations = invitations) }
            }
        }
    }

    fun onGroceryItemCheckedChange(item: GroceryItem, isChecked: Boolean) {
        scope.launch { repository.updateChecked(item, isChecked) }
    }

    /**
     * Hides [item] right away and offers an "Undo" toast; the row is only deleted once the toast
     * goes away untouched. The toast callbacks capture the app-scoped repository rather than this
     * ViewModel, so undo/commit still work if the user leaves the screen while the toast is up.
     */
    fun onGroceryItemDelete(item: GroceryItem) {
        val repository = repository
        repository.stageDelete(item)
        toastService.show(
            message =
                PhraseModel(
                    Res.string.grocery_item_deleted,
                    "item" to FixedString(item.displayName.ifBlank { item.name }),
                ),
            actionLabel = ResourceString(Res.string.grocery_undo),
            onAction = { repository.undoDelete(item.id) },
            onDismiss = { repository.commitDelete(item.id) },
        )
    }

    fun onNewGroceryItemNameChange(name: String) {
        _newGroceryItemName.value = name
    }

    fun saveGroceryItem() {
        val name = newGroceryItemName.value
        if (name.isBlank()) return
        val listId = selectedListId.value ?: return
        scope.launch { repository.addGrocery(listId, name) }
        _newGroceryItemName.value = ""
    }

    /**
     * Saves the current query text as a reusable autocomplete entry. Confirmation (and the deep
     * link to the autocomplete settings) is surfaced by the BLoC, which owns the navigation output.
     */
    fun saveAutocompleteItem(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        scope.launch { autocompleteRepository.addItem(trimmed) }
    }

    fun onSyncClicked() {
        coachMarkController.dismiss(CoachMarkId.GROCERY_LIST_SYNC)
        scope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                repository.syncAllUnsynced()
            } finally {
                _state.update { it.copy(isSyncing = false) }
            }
        }
    }

    /** Hide the sync coach mark and remember the dismissal so it never shows again. */
    fun dismissSyncTooltip() {
        coachMarkController.dismiss(CoachMarkId.GROCERY_LIST_SYNC)
    }

    override fun onCleared() {
        super.onCleared()
        // Leaving the screen without dismissing frees the queue so other coach marks can show.
        coachMarkController.release(CoachMarkId.GROCERY_LIST_SYNC)
    }

    fun onListSelected(list: GroceryListModel) {
        selectedListId.value = list.id
        _state.update {
            it.copy(selectedList = list, showListSelector = false, currentUserRole = list.role)
        }
    }

    fun onCreateListClicked() {
        _state.update { it.copy(showCreateListDialog = true) }
    }

    fun onCreateListDismissed() {
        _state.update { it.copy(showCreateListDialog = false) }
    }

    fun onCreateListConfirmed(name: String) {
        if (name.isBlank()) return
        _state.update { it.copy(showCreateListDialog = false) }
        scope.launch {
            val newId = repository.createGroceryList(name)
            selectedListId.value = newId
        }
    }

    fun onDeleteListClicked(list: GroceryListModel) {
        scope.launch {
            repository.deleteGroceryList(list.id)
            if (selectedListId.value == list.id) {
                val remaining = _state.value.lists.filter { it.id != list.id }
                val next = remaining.firstOrNull()
                selectedListId.value = next?.id
                _state.update { it.copy(selectedList = next) }
            }
        }
    }

    fun onApplySortAndFilter(
        sort: GrocerySort,
        filter: GroceryFilter,
        recipeFilter: String? = null,
    ) {
        _sort.value = sort
        _filter.value = filter
        _recipeFilter.value = recipeFilter
        sortPref = sort.name
        filterPref = filter.name
        _state.update { it.copy(sort = sort, filter = filter, recipeFilter = recipeFilter) }
    }

    /**
     * Resets the purchase and recipe filters to their defaults while preserving the chosen sort
     * order (sort never removes items, so it can't be the cause of an empty filtered list).
     */
    fun onClearFiltersClicked() {
        _filter.value = GroceryFilter.ALL
        _recipeFilter.value = null
        filterPref = GroceryFilter.ALL.name
        _state.update { it.copy(filter = GroceryFilter.ALL, recipeFilter = null) }
    }

    fun onDeleteClicked() {
        _state.update { it.copy(showDeleteDialog = true) }
    }

    fun onDeleteDismissed() {
        _state.update { it.copy(showDeleteDialog = false) }
    }

    fun onDeletePurchasedConfirmed() {
        val listId = selectedListId.value ?: return
        _state.update { it.copy(showDeleteDialog = false) }
        scope.launch { repository.deletePurchasedGroceries(listId) }
    }

    fun onListSelectorClicked() {
        _state.update { it.copy(showListSelector = true) }
    }

    fun onListSelectorDismissed() {
        _state.update { it.copy(showListSelector = false) }
    }

    fun onDeleteAllConfirmed() {
        val listId = selectedListId.value ?: return
        _state.update { it.copy(showDeleteDialog = false) }
        scope.launch { repository.deleteAllGroceries(listId) }
    }

    fun onAcceptInvitation(invite: GroceryListInvite) {
        scope.launch {
            repository.acceptInvitation(invite.memberId)
            _state.update {
                it.copy(
                    pendingInvitations =
                        it.pendingInvitations.filterNot { pending ->
                            pending.memberId == invite.memberId
                        }
                )
            }
            repository.syncAllUnsynced()
        }
    }

    fun onRejectInvitation(invite: GroceryListInvite) {
        scope.launch {
            repository.rejectInvitation(invite.memberId)
            _state.update {
                it.copy(
                    pendingInvitations =
                        it.pendingInvitations.filterNot { pending ->
                            pending.memberId == invite.memberId
                        }
                )
            }
        }
    }

    data class State(
        val groupedItems: List<GroceryGroup> = emptyList(),
        val sort: GrocerySort = GrocerySort.AISLE,
        val filter: GroceryFilter = GroceryFilter.ALL,
        val recipeFilter: String? = null,
        val availableRecipes: List<String> = emptyList(),
        val autocompleteSuggestions: List<String> = emptyList(),
        val queryMatchesSavedAutocomplete: Boolean = false,
        val hasNoRecipeItems: Boolean = false,
        val isSyncing: Boolean = false,
        val lists: List<GroceryListModel> = emptyList(),
        val selectedList: GroceryListModel? = null,
        val showCreateListDialog: Boolean = false,
        val showDeleteDialog: Boolean = false,
        val showListSelector: Boolean = false,
        val pendingInvitations: List<GroceryListInvite> = emptyList(),
        val currentUserRole: ListRole = ListRole.OWNER,
        val showSyncTooltip: Boolean = false,
    )

    private data class GroceryListContent(
        val groupedItems: List<GroceryGroup>,
        val availableRecipes: List<String>,
        val autocompleteSuggestions: List<String>,
        val queryMatchesSavedAutocomplete: Boolean,
        val hasNoRecipeItems: Boolean,
    )

    companion object {
        private const val KEY_SORT = "grocery_list_sort"
        private const val KEY_FILTER = "grocery_list_filter"
        private const val MAX_AUTOCOMPLETE_SUGGESTIONS = 6

        private fun buildAutocompleteSuggestions(
            query: String,
            items: List<GroceryItem>,
            customItems: List<String>,
        ): List<String> {
            val normalizedQuery = IngredientParser.parse(query).name.trim().lowercase()
            if (normalizedQuery.isBlank()) return emptyList()

            val existingItemSuggestions =
                items
                    .asSequence()
                    .map { it.displayName.trim() }
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() }
                    .filter { it.lowercase().startsWith(normalizedQuery) }

            // The user's saved autocomplete entries rank right after items already on the list and
            // ahead of the built-in vocabulary.
            val customSuggestions =
                customItems
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .filter { it.lowercase().startsWith(normalizedQuery) }

            return (existingItemSuggestions +
                    customSuggestions +
                    IngredientParser.suggestedNamesFor(query).asSequence())
                .distinctBy { it.lowercase() }
                .filter { it.lowercase() != normalizedQuery }
                .take(MAX_AUTOCOMPLETE_SUGGESTIONS)
                .toList()
        }

        /**
         * True when [query] resolves to an item already saved in the user's autocomplete library.
         * Used to hide the suggestion dropdown entirely — there is nothing to add and the exact
         * match would otherwise dangle as a redundant "Save …" row.
         */
        private fun queryMatchesSavedAutocomplete(
            query: String,
            customItems: List<String>,
        ): Boolean {
            val normalizedQuery = IngredientParser.parse(query).name.trim().lowercase()
            if (normalizedQuery.isBlank()) return false
            return customItems.any { it.trim().lowercase() == normalizedQuery }
        }
    }
}

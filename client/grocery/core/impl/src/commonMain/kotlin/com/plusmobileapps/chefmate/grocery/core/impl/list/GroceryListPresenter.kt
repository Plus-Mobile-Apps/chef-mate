package com.plusmobileapps.chefmate.grocery.core.impl.list

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import chefmate.client.grocery.core.public.generated.resources.Res
import chefmate.client.grocery.core.public.generated.resources.grocery_item_deleted
import chefmate.client.grocery.core.public.generated.resources.grocery_undo
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryFilter
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryGroup
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GrocerySort
import com.plusmobileapps.chefmate.grocery.data.GroceryAutocompleteRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Molecule presenter for the grocery list: a `@Composable` function that turns [Event]s plus the
 * repository flows into a single [State]. It emits no UI — Compose is used purely as a reactive
 * state engine. [GroceryListViewModel] runs it with `launchMolecule` inside its retained scope.
 *
 * Reading guide:
 * - Inputs (repository flows) are `collectAsState()`d. The flow instance is `remember`ed so it is
 *   not re-subscribed on every recomposition.
 * - Screen-local mutable state is `remember { mutableStateOf(...) }`; event handlers write it.
 * - Everything else is plain derivation in the function body, memoized with keyed `remember`.
 */
@Inject
class GroceryListPresenter(
    private val repository: GroceryRepository,
    private val autocompleteRepository: GroceryAutocompleteRepository,
    private val settings: Settings,
    private val coachMarkController: CoachMarkController,
    private val toastService: ToastService,
) {
    private var sortPref by settings.string(KEY_SORT, GrocerySort.AISLE.name)
    private var filterPref by settings.string(KEY_FILTER, GroceryFilter.ALL.name)

    /**
     * @param events one-shot user actions; collected for the lifetime of the composition.
     * @param query the add-item text field. It is owned by the ViewModel rather than this presenter
     *   so the text field can update synchronously (a round trip through recomposition would make
     *   the cursor jump).
     */
    @Composable
    fun present(events: Flow<Event>, query: StateFlow<String>): State {
        // --- Screen-local state -----------------------------------------------------------------
        var selectedListId by remember { mutableStateOf<Long?>(null) }
        var sort by remember { mutableStateOf(sortFromName(sortPref)) }
        var filter by remember { mutableStateOf(filterFromName(filterPref)) }
        var recipeFilter by remember { mutableStateOf<String?>(null) }
        var isSyncing by remember { mutableStateOf(false) }
        var currentUserRole by remember { mutableStateOf(ListRole.OWNER) }
        var showCreateListDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showListSelector by remember { mutableStateOf(false) }
        // Invites accepted/rejected this session, hidden until the repository catches up.
        var handledInviteIds by remember { mutableStateOf(emptySet<String>()) }

        // --- Inputs -----------------------------------------------------------------------------
        val currentQuery by query.collectAsState()
        val lists by remember { repository.getGroceryLists() }.collectAsState(emptyList())
        val invitations by remember {
            repository.getPendingInvitations()
        }
            .collectAsState(emptyList())
        val customNames by remember {
            autocompleteRepository.observeItems().map { items -> items.map { it.name } }
        }
            .collectAsState(emptyList())
        val activeCoachMark by coachMarkController.activeCoachMark.collectAsState()

        // The selection falls back to the first list whenever the chosen one disappears, so there
        // is no "keep selectedListId in sync with lists" bookkeeping.
        val selectedList = lists.firstOrNull { it.id == selectedListId } ?: lists.firstOrNull()
        val listId = selectedList?.id ?: selectedListId
        // The event collector below is launched once, so it must read the *latest* listId through
        // a State rather than capturing this composition's plain local.
        val currentListId by rememberUpdatedState(listId)

        // Re-keyed on listId: switching lists swaps the subscription, like flatMapLatest.
        val items by
            remember(listId) {
                    if (listId != null) repository.getGroceries(listId) else flowOf(emptyList())
                }
                .collectAsState(emptyList())

        // --- Derivations ------------------------------------------------------------------------
        val groupedItems =
            remember(items, filter, sort, recipeFilter) {
                groupItems(items = items, filter = filter, sort = sort, recipeFilter = recipeFilter)
            }
        val autocompleteSuggestions =
            remember(currentQuery, items, customNames) {
                buildAutocompleteSuggestions(currentQuery, items, customNames)
            }

        // --- Effects ----------------------------------------------------------------------------
        LaunchedEffect(Unit) {
            val defaultListId = repository.ensureDefaultList()
            if (selectedListId == null) selectedListId = defaultListId
        }

        // Request the sync coach mark while this screen is alive. Cancelling the ViewModel's scope
        // disposes the composition, which runs onDispose and frees the coach mark queue.
        DisposableEffect(Unit) {
            coachMarkController.request(CoachMarkId.GROCERY_LIST_SYNC)
            onDispose { coachMarkController.release(CoachMarkId.GROCERY_LIST_SYNC) }
        }

        LaunchedEffect(events) {
            events.collect { event ->
                when (event) {
                    is Event.CheckedChange ->
                        launch { repository.updateChecked(event.item, event.isChecked) }
                    is Event.DeleteItem -> stageDeleteWithUndo(event.item)
                    is Event.AddItem -> {
                        val id = currentListId
                        if (id != null) launch { repository.addGrocery(id, event.name) }
                    }
                    is Event.SaveAutocompleteItem -> {
                        val trimmed = event.name.trim()
                        if (trimmed.isNotBlank()) {
                            launch { autocompleteRepository.addItem(trimmed) }
                        }
                    }
                    Event.Sync -> {
                        coachMarkController.dismiss(CoachMarkId.GROCERY_LIST_SYNC)
                        launch {
                            isSyncing = true
                            try {
                                repository.syncAllUnsynced()
                            } finally {
                                isSyncing = false
                            }
                        }
                    }
                    Event.DismissSyncTooltip ->
                        coachMarkController.dismiss(CoachMarkId.GROCERY_LIST_SYNC)
                    is Event.SelectList -> {
                        selectedListId = event.list.id
                        currentUserRole = event.list.role
                        showListSelector = false
                    }
                    Event.ShowListSelector -> showListSelector = true
                    Event.DismissListSelector -> showListSelector = false
                    Event.ShowCreateList -> showCreateListDialog = true
                    Event.DismissCreateList -> showCreateListDialog = false
                    is Event.CreateList -> {
                        if (event.name.isNotBlank()) {
                            showCreateListDialog = false
                            launch { selectedListId = repository.createGroceryList(event.name) }
                        }
                    }
                    is Event.DeleteList ->
                        launch {
                            repository.deleteGroceryList(event.list.id)
                            if (selectedListId == event.list.id) {
                                selectedListId = lists.firstOrNull { it.id != event.list.id }?.id
                            }
                        }
                    is Event.ApplySortAndFilter -> {
                        sort = event.sort
                        filter = event.filter
                        recipeFilter = event.recipeFilter
                        sortPref = event.sort.name
                        filterPref = event.filter.name
                    }
                    Event.ClearFilters -> {
                        filter = GroceryFilter.ALL
                        recipeFilter = null
                        filterPref = GroceryFilter.ALL.name
                    }
                    Event.ShowDeleteDialog -> showDeleteDialog = true
                    Event.DismissDeleteDialog -> showDeleteDialog = false
                    Event.DeletePurchased -> {
                        val id = currentListId
                        if (id != null) {
                            showDeleteDialog = false
                            launch { repository.deletePurchasedGroceries(id) }
                        }
                    }
                    Event.DeleteAll -> {
                        val id = currentListId
                        if (id != null) {
                            showDeleteDialog = false
                            launch { repository.deleteAllGroceries(id) }
                        }
                    }
                    is Event.AcceptInvitation ->
                        launch {
                            repository.acceptInvitation(event.invite.memberId)
                            handledInviteIds = handledInviteIds + event.invite.memberId
                            repository.syncAllUnsynced()
                        }
                    is Event.RejectInvitation ->
                        launch {
                            repository.rejectInvitation(event.invite.memberId)
                            handledInviteIds = handledInviteIds + event.invite.memberId
                        }
                }
            }
        }

        return State(
            groupedItems = groupedItems,
            sort = sort,
            filter = filter,
            recipeFilter = recipeFilter,
            availableRecipes = items.mapNotNull { it.recipeName }.distinct().sorted(),
            autocompleteSuggestions = autocompleteSuggestions,
            queryMatchesSavedAutocomplete =
                queryMatchesSavedAutocomplete(currentQuery, customNames),
            hasNoRecipeItems = items.any { it.recipeName == null },
            isSyncing = isSyncing,
            lists = lists,
            selectedList = selectedList,
            showCreateListDialog = showCreateListDialog,
            showDeleteDialog = showDeleteDialog,
            showListSelector = showListSelector,
            pendingInvitations = invitations.filterNot { it.memberId in handledInviteIds },
            currentUserRole = currentUserRole,
            showSyncTooltip = activeCoachMark == CoachMarkId.GROCERY_LIST_SYNC,
        )
    }

    /**
     * Hides [item] right away and offers an "Undo" toast; the row is only deleted once the toast
     * goes away untouched. The toast callbacks capture the app-scoped repository rather than this
     * presenter, so undo/commit still work if the user leaves the screen while the toast is up.
     */
    private fun stageDeleteWithUndo(item: GroceryItem) {
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

    sealed interface Event {
        data class CheckedChange(val item: GroceryItem, val isChecked: Boolean) : Event

        data class DeleteItem(val item: GroceryItem) : Event

        data class AddItem(val name: String) : Event

        data class SaveAutocompleteItem(val name: String) : Event

        data object Sync : Event

        data object DismissSyncTooltip : Event

        data class SelectList(val list: GroceryListModel) : Event

        data object ShowListSelector : Event

        data object DismissListSelector : Event

        data object ShowCreateList : Event

        data object DismissCreateList : Event

        data class CreateList(val name: String) : Event

        data class DeleteList(val list: GroceryListModel) : Event

        data class ApplySortAndFilter(
            val sort: GrocerySort,
            val filter: GroceryFilter,
            val recipeFilter: String? = null,
        ) : Event

        data object ClearFilters : Event

        data object ShowDeleteDialog : Event

        data object DismissDeleteDialog : Event

        data object DeletePurchased : Event

        data object DeleteAll : Event

        data class AcceptInvitation(val invite: GroceryListInvite) : Event

        data class RejectInvitation(val invite: GroceryListInvite) : Event
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

    companion object {
        private const val KEY_SORT = "grocery_list_sort"
        private const val KEY_FILTER = "grocery_list_filter"
        private const val MAX_AUTOCOMPLETE_SUGGESTIONS = 6

        private fun sortFromName(name: String): GrocerySort =
            GrocerySort.entries.find { it.name == name } ?: GrocerySort.AISLE

        private fun filterFromName(name: String): GroceryFilter =
            GroceryFilter.entries.find { it.name == name } ?: GroceryFilter.ALL

        private fun groupItems(
            items: List<GroceryItem>,
            filter: GroceryFilter,
            sort: GrocerySort,
            recipeFilter: String?,
        ): List<GroceryGroup> {
            val filtered =
                when (filter) {
                    GroceryFilter.ALL -> items
                    GroceryFilter.UNPURCHASED -> items.filter { !it.isChecked }
                    GroceryFilter.PURCHASED -> items.filter { it.isChecked }
                }
            val recipeFiltered =
                when (recipeFilter) {
                    null -> filtered
                    GroceryListBloc.NO_RECIPE_FILTER -> filtered.filter { it.recipeName == null }
                    else -> filtered.filter { it.recipeName == recipeFilter }
                }
            return when (sort) {
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
                                    recipeFiltered.firstOrNull()?.category ?: GroceryCategory.OTHER,
                                items =
                                    recipeFiltered
                                        .sortedBy { it.displayName.lowercase() }
                                        .toImmutableList(),
                            )
                        )
                        .filter { it.items.isNotEmpty() }
            }
        }

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

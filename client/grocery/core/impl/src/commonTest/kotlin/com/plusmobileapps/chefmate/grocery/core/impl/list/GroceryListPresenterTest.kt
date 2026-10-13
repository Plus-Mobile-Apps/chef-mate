@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.grocery.core.impl.list

import app.cash.molecule.RecompositionMode
import app.cash.molecule.moleculeFlow
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import chefmate.client.grocery.core.public.generated.resources.Res
import chefmate.client.grocery.core.public.generated.resources.grocery_item_deleted
import chefmate.client.grocery.core.public.generated.resources.grocery_undo
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.grocery.core.impl.list.GroceryListPresenter.Event
import com.plusmobileapps.chefmate.grocery.core.impl.list.GroceryListPresenter.State
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryFilter
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GrocerySort
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryListInvite
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.grocery.data.ListRole
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryAutocompleteRepository
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryRepository
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/**
 * Exercises [GroceryListPresenter] directly, with no Decompose or ViewModel involved: events go in,
 * [State]s come out. `moleculeFlow` runs on `runTest`'s StandardTestDispatcher, and the fakes give
 * real repository behavior (adding an item makes it appear, deleting a list drops it, ...).
 */
class GroceryListPresenterTest {
    private val fakeRepository = FakeGroceryRepository()
    private var repository: GroceryRepository = fakeRepository
    private val autocompleteRepository = FakeGroceryAutocompleteRepository()
    private val settings = MapSettings()
    private val toastService = FakeToastService()

    // Seed the sync coach mark as already seen so it doesn't affect the other assertions. The
    // unseen path has its own dedicated test.
    private var coachMarkController =
        CoachMarkController(MapSettings()).apply { dismiss(CoachMarkId.GROCERY_LIST_SYNC) }

    private val events = Channel<Event>(Channel.UNLIMITED)
    private val eventFlow = events.receiveAsFlow()
    private val query = MutableStateFlow("")

    private fun send(event: Event) {
        events.trySend(event)
    }

    private fun presenterTest(validate: suspend ReceiveTurbine<State>.(TestScope) -> Unit) =
        runTest {
            val presenter =
                GroceryListPresenter(
                    repository = repository,
                    autocompleteRepository = autocompleteRepository,
                    settings = settings,
                    coachMarkController = coachMarkController,
                    toastService = toastService,
                )
            moleculeFlow(RecompositionMode.Immediate) { presenter.present(eventFlow, query) }
                // Immediate mode emits once per recomposition; collapse identical frames so tests
                // only see real changes.
                .distinctUntilChanged()
                .test {
                    validate(this@runTest)
                    // Tests assert on the frames they care about; trailing frames are irrelevant.
                    cancelAndIgnoreRemainingEvents()
                }
        }

    /** Skips intermediate frames until one matches [predicate]. */
    private suspend fun ReceiveTurbine<State>.awaitState(predicate: (State) -> Boolean): State {
        while (true) {
            val state = awaitItem()
            if (predicate(state)) return state
        }
    }

    /** The default list has been ensured and selected. */
    private suspend fun ReceiveTurbine<State>.awaitLoaded(): State = awaitState {
        it.selectedList != null
    }

    private fun State.itemNames(): List<String> = groupedItems.flatMap { it.items }.map { it.name }

    @Test
    fun When_first_composed_Then_default_state_then_default_list_selected() = presenterTest {
        awaitItem() shouldBe State()

        val loaded = awaitLoaded()
        loaded.selectedList?.id shouldBe 1L
        loaded.lists.map { it.name } shouldBe listOf("My Grocery List")
    }

    @Test
    fun When_items_on_list_Then_grouped_by_aisle_in_category_order() = presenterTest {
        fakeRepository.addGrocery(1L, "Milk")
        fakeRepository.addGrocery(1L, "Apples")

        val state = awaitState { it.itemNames().size == 2 }

        state.groupedItems.map { it.category } shouldBe
            listOf(GroceryCategory.DAIRY, GroceryCategory.PRODUCE).sortedBy { it.ordinal }
        state.hasNoRecipeItems shouldBe true
    }

    @Test
    fun When_alphabetical_sort_applied_Then_flattened_sorted_and_persisted() = presenterTest {
        fakeRepository.addGrocery(1L, "Chicken")
        fakeRepository.addGrocery(1L, "Apples")
        fakeRepository.addGrocery(1L, "Bread")
        awaitState { it.itemNames().size == 3 }

        send(Event.ApplySortAndFilter(GrocerySort.ALPHABETICAL, GroceryFilter.ALL))

        val state = awaitState { it.sort == GrocerySort.ALPHABETICAL }
        state.groupedItems.size shouldBe 1
        state.itemNames() shouldBe listOf("Apples", "Bread", "Chicken")
        settings.getString("grocery_list_sort", "") shouldBe "ALPHABETICAL"
    }

    @Test
    fun When_sort_and_filter_saved_Then_restored_on_launch() {
        settings.putString("grocery_list_sort", "ALPHABETICAL")
        settings.putString("grocery_list_filter", "PURCHASED")

        presenterTest {
            val initial = awaitItem()
            initial.sort shouldBe GrocerySort.ALPHABETICAL
            initial.filter shouldBe GroceryFilter.PURCHASED
        }
    }

    @Test
    fun When_unpurchased_filter_applied_Then_checked_items_hidden() = presenterTest {
        fakeRepository.addGrocery(1L, "Apples")
        fakeRepository.addGrocery(1L, "Bananas")
        val apples = awaitState { it.itemNames().size == 2 }.groupedItems[0].items[0]
        fakeRepository.updateChecked(apples, isChecked = true)

        send(Event.ApplySortAndFilter(GrocerySort.AISLE, GroceryFilter.UNPURCHASED))

        awaitState { it.filter == GroceryFilter.UNPURCHASED }.itemNames() shouldBe listOf("Bananas")
    }

    @Test
    fun When_recipe_filters_applied_Then_items_narrowed_to_recipe() = presenterTest {
        fakeRepository.addGroceries(1L, listOf("Flour", "Sugar"), recipeName = "Chocolate Cake")
        fakeRepository.addGroceries(1L, listOf("Chicken"), recipeName = "Chicken Soup")
        fakeRepository.addGrocery(1L, "Salt")

        val loaded = awaitState { it.itemNames().size == 4 }
        loaded.availableRecipes shouldBe listOf("Chicken Soup", "Chocolate Cake")
        loaded.hasNoRecipeItems shouldBe true

        send(
            Event.ApplySortAndFilter(GrocerySort.ALPHABETICAL, GroceryFilter.ALL, "Chocolate Cake")
        )
        awaitState { it.recipeFilter == "Chocolate Cake" }.itemNames() shouldBe
            listOf("Flour", "Sugar")

        send(
            Event.ApplySortAndFilter(
                GrocerySort.ALPHABETICAL,
                GroceryFilter.ALL,
                GroceryListBloc.NO_RECIPE_FILTER,
            )
        )
        awaitState { it.recipeFilter == GroceryListBloc.NO_RECIPE_FILTER }.itemNames() shouldBe
            listOf("Salt")
    }

    @Test
    fun When_clear_filters_Then_filters_reset_but_sort_preserved() = presenterTest {
        fakeRepository.addGroceries(1L, listOf("Flour"), recipeName = "Chocolate Cake")
        fakeRepository.addGrocery(1L, "Salt")
        awaitState { it.itemNames().size == 2 }
        send(
            Event.ApplySortAndFilter(
                GrocerySort.ALPHABETICAL,
                GroceryFilter.PURCHASED,
                "Chocolate Cake",
            )
        )
        awaitState { it.itemNames().isEmpty() }

        send(Event.ClearFilters)

        val state = awaitState { it.itemNames().size == 2 }
        state.filter shouldBe GroceryFilter.ALL
        state.recipeFilter shouldBe null
        state.sort shouldBe GrocerySort.ALPHABETICAL
        settings.getString("grocery_list_filter", "") shouldBe "ALL"
    }

    @Test
    fun When_query_typed_Then_suggestions_rank_list_items_then_saved_then_vocabulary() =
        presenterTest {
            fakeRepository.addGrocery(1L, "Milk chocolate")
            autocompleteRepository.addItem("Mizuna greens")
            awaitState { it.itemNames().size == 1 }

            query.value = "mi"

            awaitState { it.autocompleteSuggestions.isNotEmpty() }
                .autocompleteSuggestions
                .take(2) shouldBe listOf("Milk chocolate", "Mizuna greens")
        }

    @Test
    fun When_query_matches_suggestion_exactly_Then_exact_match_hidden() = presenterTest {
        awaitLoaded()

        query.value = "sal"
        awaitState { it.autocompleteSuggestions.isNotEmpty() }.autocompleteSuggestions shouldBe
            listOf("Salami", "Salmon", "Salad", "Salsa", "Salt")

        query.value = "salt"
        awaitState { it.autocompleteSuggestions.isEmpty() }
    }

    @Test
    fun When_query_matches_saved_autocomplete_item_Then_flag_set() = presenterTest {
        autocompleteRepository.addItem("Zeppole")
        awaitLoaded()

        query.value = "zep"
        awaitState { it.autocompleteSuggestions == listOf("Zeppole") }
            .queryMatchesSavedAutocomplete shouldBe false

        query.value = "Zeppole"
        awaitState { it.queryMatchesSavedAutocomplete }
    }

    @Test
    fun When_add_item_Then_added_to_selected_list() = presenterTest {
        awaitLoaded()

        send(Event.AddItem("Eggs"))

        awaitState { it.itemNames() == listOf("Eggs") }
    }

    @Test
    fun When_list_selected_Then_items_role_and_selector_follow() = presenterTest {
        fakeRepository.addGrocery(1L, "Milk")
        val secondId = fakeRepository.createGroceryList("Costco")
        fakeRepository.addGrocery(secondId, "Paper towels")
        send(Event.ShowListSelector)
        val loaded = awaitState { it.showListSelector && it.itemNames() == listOf("Milk") }

        val costco = loaded.lists.first { it.id == secondId }.copy(role = ListRole.EDITOR)
        send(Event.SelectList(costco))

        // The selection flips first; items follow once the new list's subscription emits.
        val state = awaitState { it.itemNames() == listOf("Paper towels") }
        state.selectedList?.id shouldBe secondId
        state.currentUserRole shouldBe ListRole.EDITOR
        state.showListSelector shouldBe false
    }

    @Test
    fun When_list_created_Then_dialog_closes_and_new_list_selected() = presenterTest {
        awaitLoaded()
        send(Event.ShowCreateList)
        awaitState { it.showCreateListDialog }

        send(Event.CreateList("   "))
        send(Event.CreateList("Weekend"))

        val state = awaitState { it.selectedList?.name == "Weekend" }
        state.showCreateListDialog shouldBe false
        state.lists.map { it.name } shouldBe listOf("My Grocery List", "Weekend")
    }

    @Test
    fun When_selected_list_deleted_Then_falls_back_to_remaining_list() = presenterTest {
        val secondId = fakeRepository.createGroceryList("Costco")
        val loaded = awaitState { it.lists.size == 2 }
        send(Event.SelectList(loaded.lists.first { it.id == secondId }))
        val selected = awaitState { it.selectedList?.id == secondId }

        send(Event.DeleteList(selected.selectedList!!))

        val state = awaitState { it.lists.size == 1 }
        state.selectedList?.id shouldBe 1L
    }

    @Test
    fun When_delete_purchased_confirmed_Then_checked_items_removed_and_dialog_closed() =
        presenterTest {
            fakeRepository.addGrocery(1L, "Apples")
            fakeRepository.addGrocery(1L, "Bananas")
            val apples = awaitState { it.itemNames().size == 2 }.groupedItems[0].items[0]
            fakeRepository.updateChecked(apples, isChecked = true)
            send(Event.ShowDeleteDialog)
            awaitState { it.showDeleteDialog }

            send(Event.DeletePurchased)

            val state = awaitState { !it.showDeleteDialog && it.itemNames().size == 1 }
            state.itemNames() shouldBe listOf("Bananas")
        }

    @Test
    fun When_item_deleted_Then_hidden_with_undo_toast_and_undo_restores() = presenterTest {
        fakeRepository.addGrocery(1L, "Apples")
        val apples = awaitState { it.itemNames().size == 1 }.groupedItems[0].items[0]

        send(Event.DeleteItem(apples))

        awaitState { it.itemNames().isEmpty() }
        toastService.shown shouldContain
            PhraseModel(Res.string.grocery_item_deleted, "item" to FixedString("Apples"))
        toastService.shownActionLabels shouldContain ResourceString(Res.string.grocery_undo)

        toastService.lastOnAction?.invoke()

        awaitState { it.itemNames() == listOf("Apples") }
    }

    @Test
    fun When_delete_toast_dismissed_Then_delete_committed() = presenterTest {
        fakeRepository.addGrocery(1L, "Apples")
        val apples = awaitState { it.itemNames().size == 1 }.groupedItems[0].items[0]
        send(Event.DeleteItem(apples))
        awaitState { it.itemNames().isEmpty() }

        toastService.lastOnDismiss?.invoke()
        // A committed delete can no longer be undone.
        fakeRepository.undoDelete(apples.id)

        fakeRepository.getGrocery(apples.id) shouldBe null
    }

    @Test
    fun When_sync_clicked_Then_syncing_while_in_flight() {
        val syncGate = CompletableDeferred<Unit>()
        repository =
            object : GroceryRepository by fakeRepository {
                override suspend fun syncAllUnsynced() = syncGate.await()
            }

        presenterTest {
            awaitLoaded()

            send(Event.Sync)
            awaitState { it.isSyncing }

            syncGate.complete(Unit)
            awaitState { !it.isSyncing }
        }
    }

    @Test
    fun When_invitations_handled_Then_removed_from_pending() = presenterTest {
        val keep = GroceryListInvite(memberId = "a", listName = "Family", role = ListRole.EDITOR)
        val accept = GroceryListInvite(memberId = "b", listName = "Roomies", role = ListRole.EDITOR)
        val reject = GroceryListInvite(memberId = "c", listName = "Work", role = ListRole.VIEWER)
        fakeRepository.pendingInvitations.value = listOf(keep, accept, reject)
        awaitState { it.pendingInvitations.size == 3 }

        send(Event.AcceptInvitation(accept))
        send(Event.RejectInvitation(reject))

        awaitState { it.pendingInvitations == listOf(keep) }
        fakeRepository.acceptedInvitations shouldBe listOf("b")
        fakeRepository.rejectedInvitations shouldBe listOf("c")
    }

    @Test
    fun When_sync_coach_mark_unseen_Then_shown_until_dismissed() {
        coachMarkController = CoachMarkController(MapSettings())

        presenterTest {
            awaitState { it.showSyncTooltip }

            send(Event.DismissSyncTooltip)

            awaitState { !it.showSyncTooltip }
            coachMarkController.hasSeen(CoachMarkId.GROCERY_LIST_SYNC) shouldBe true
        }
    }

    @Test
    fun When_presenter_leaves_composition_Then_coach_mark_released_but_not_seen() {
        val controller = CoachMarkController(MapSettings())
        coachMarkController = controller

        presenterTest { scope ->
            awaitState { it.showSyncTooltip }
            controller.activeCoachMark.value shouldBe CoachMarkId.GROCERY_LIST_SYNC

            cancel()
            scope.testScheduler.advanceUntilIdle()

            controller.activeCoachMark.value shouldBe null
            controller.hasSeen(CoachMarkId.GROCERY_LIST_SYNC) shouldBe false
        }
    }
}

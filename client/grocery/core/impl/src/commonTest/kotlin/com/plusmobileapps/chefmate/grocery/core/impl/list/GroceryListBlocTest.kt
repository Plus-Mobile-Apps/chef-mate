@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.grocery.core.impl.list

import app.cash.turbine.test
import chefmate.client.grocery.core.public.generated.resources.Res
import chefmate.client.grocery.core.public.generated.resources.grocery_autocomplete_manage
import chefmate.client.grocery.core.public.generated.resources.grocery_autocomplete_saved
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.grocery.core.detail.GroceryDetailBloc
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryGroup
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryAutocompleteRepository
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * Covers what the BLoC itself owns: event routing into the ViewModel, navigation outputs, the
 * detail sheet slot, and the autocomplete toast. State derivation is covered by
 * [GroceryListPresenterTest].
 *
 * Molecule needs a dispatching test dispatcher. With an unconfined main context, a state write made
 * while the first composition is being applied recomposes re-entrantly and stalls the molecule.
 * Hence the shared [StandardTestDispatcher] and the explicit `runCurrent()` calls.
 */
class GroceryListBlocTest {
    private val dispatcher = StandardTestDispatcher()
    private val lifecycle = LifecycleRegistry().apply { resume() }
    private val context = TestBlocContext.create(lifecycle = lifecycle, mainContext = dispatcher)
    private val output = TestConsumer<GroceryListBloc.Output>()
    private val groceries = MutableStateFlow<List<GroceryItem>>(emptyList())
    private val repository: GroceryRepository = mock {
        every { getGroceries(1L) } returns groceries
        every { getGroceryLists() } returns flowOf(emptyList<GroceryListModel>())
        every { getPendingInvitations() } returns flowOf(emptyList())
        everySuspend { ensureDefaultList() } returns 1L
    }
    private val autocompleteRepository = FakeGroceryAutocompleteRepository()
    private val toastService = FakeToastService()

    private var detailItemId: Long? = null
    private var detailOutput: Consumer<GroceryDetailBloc.Output> = Consumer {}
    private val groceryDetailFactory = GroceryDetailBloc.Factory { _, id, output ->
        detailItemId = id
        detailOutput = output
        mock(MockMode.autoUnit)
    }

    private val coachMarkController = CoachMarkController(MapSettings())

    private val bloc =
        GroceryListBlocImpl(
            context = context,
            output = output,
            viewModelFactory = {
                GroceryListViewModel(
                    mainContext = context.mainContext,
                    presenter =
                        GroceryListPresenter(
                            repository = repository,
                            autocompleteRepository = autocompleteRepository,
                            settings = MapSettings(),
                            coachMarkController = coachMarkController,
                            toastService = toastService,
                        ),
                )
            },
            groceryDetailFactory = groceryDetailFactory,
            toastService = toastService,
        )

    /** Runs on the BLoC's dispatcher, after letting the presenter's startup effects run. */
    private fun blocTest(block: suspend TestScope.() -> Unit) =
        runTest(dispatcher) {
            runCurrent()
            block()
        }

    // Destroying the lifecycle clears the ViewModel, cancelling the molecule.
    @AfterTest
    fun tearDown() {
        if (lifecycle.state != Lifecycle.State.DESTROYED) lifecycle.destroy()
    }

    @Test
    fun When_items_loaded_Then_presenter_state_maps_to_model() = blocTest {
        val items = listOf(GroceryItem(id = 1, name = "Apples", category = GroceryCategory.PRODUCE))

        bloc.state.test {
            awaitItem().groupedItems shouldBe persistentListOf()
            groceries.value = items
            awaitItem().groupedItems shouldBe
                persistentListOf(GroceryGroup(GroceryCategory.PRODUCE, items.toImmutableList()))
        }
    }

    @Test
    fun When_grocery_item_checked_change_Then_routed_to_repository() = blocTest {
        val item = GroceryItem(id = 1, name = "Apples", isChecked = false)
        everySuspend { repository.updateChecked(item, true) } returns Unit

        bloc.onGroceryItemCheckedChange(item, true)
        runCurrent()

        verifySuspend { repository.updateChecked(item, true) }
    }

    @Test
    fun When_name_entered_with_newline_Then_item_saved_and_field_cleared() = blocTest {
        everySuspend { repository.addGrocery(1L, "Milk") } returns Unit

        bloc.onNewGroceryItemNameChange("Milk")
        bloc.newGroceryItemName.value shouldBe "Milk"
        bloc.onNewGroceryItemNameChange("Milk\n")
        runCurrent()

        verifySuspend { repository.addGrocery(1L, "Milk") }
        bloc.newGroceryItemName.value shouldBe ""
    }

    @Test
    fun When_onSaveAutocompleteItem_Then_name_is_persisted() = blocTest {
        bloc.onSaveAutocompleteItem("Cold brew concentrate")
        runCurrent()

        autocompleteRepository.observeItems().first().map { it.name } shouldContain
            "Cold brew concentrate"
    }

    @Test
    fun When_onSaveAutocompleteItem_Then_toast_shown_with_manage_action() = blocTest {
        bloc.onSaveAutocompleteItem("Cold brew concentrate")

        toastService.shown shouldContain ResourceString(Res.string.grocery_autocomplete_saved)
        toastService.shownActionLabels shouldContain
            ResourceString(Res.string.grocery_autocomplete_manage)

        // Tapping the toast action deep links to the autocomplete settings screen.
        toastService.lastOnAction?.invoke()
        output.lastValue shouldBe GroceryListBloc.Output.OpenAutocompleteSettings
    }

    @Test
    fun When_blank_autocomplete_item_saved_Then_no_toast_and_nothing_persisted() = blocTest {
        bloc.onSaveAutocompleteItem("   ")
        runCurrent()

        toastService.shown shouldBe emptyList()
        autocompleteRepository.observeItems().first() shouldBe emptyList()
    }

    @Test
    fun When_browse_recipes_clicked_Then_open_recipes_output_emitted() = blocTest {
        bloc.onBrowseRecipesClicked()
        output.lastValue shouldBe GroceryListBloc.Output.OpenRecipes
    }

    @Test
    fun When_edit_list_clicked_Then_open_edit_list_output_emitted() = blocTest {
        bloc.onEditListClicked(GroceryListModel(id = 3L, name = "Costco"))
        output.lastValue shouldBe GroceryListBloc.Output.OpenEditList(3L)
    }

    @Test
    fun When_item_clicked_Then_detail_sheet_slot_activated() = blocTest {
        val item = GroceryItem(id = 42, name = "Apples", isChecked = false)
        bloc.childSlot.value.child shouldBe null
        bloc.onGroceryItemClicked(item)
        bloc.childSlot.value.child?.instance.let {
            (it as GroceryListBloc.Sheet.GroceryDetail).bloc shouldBe it.bloc
        }
        detailItemId shouldBe 42L
    }

    @Test
    fun When_detail_finishes_Then_sheet_slot_dismissed() = blocTest {
        val item = GroceryItem(id = 7, name = "Milk", isChecked = false)
        bloc.onGroceryItemClicked(item)
        bloc.childSlot.value.child shouldNotBe null
        detailOutput.onNext(GroceryDetailBloc.Output.Finished)
        bloc.childSlot.value.child shouldBe null
    }

    @Test
    fun When_dismiss_sheet_called_Then_slot_dismissed() = blocTest {
        val item = GroceryItem(id = 9, name = "Cheese", isChecked = false)
        bloc.onGroceryItemClicked(item)
        bloc.childSlot.value.child shouldNotBe null
        bloc.onDismissSheet()
        bloc.childSlot.value.child shouldBe null
    }

    @Test
    fun When_sync_coach_mark_unseen_Then_shown_and_dismiss_persists() = blocTest {
        bloc.state.value.showSyncTooltip shouldBe true

        bloc.onSyncTooltipDismissed()
        runCurrent()

        bloc.state.value.showSyncTooltip shouldBe false
        coachMarkController.hasSeen(CoachMarkId.GROCERY_LIST_SYNC) shouldBe true
    }

    @Test
    fun When_screen_destroyed_Then_coach_mark_released() = blocTest {
        coachMarkController.activeCoachMark.value shouldBe CoachMarkId.GROCERY_LIST_SYNC

        lifecycle.destroy()
        runCurrent()

        coachMarkController.activeCoachMark.value shouldBe null
        coachMarkController.hasSeen(CoachMarkId.GROCERY_LIST_SYNC) shouldBe false
    }
}

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.grocery.core.impl.list

import app.cash.turbine.test
import chefmate.client.grocery.core.public.generated.resources.Res
import chefmate.client.grocery.core.public.generated.resources.grocery_autocomplete_manage
import chefmate.client.grocery.core.public.generated.resources.grocery_autocomplete_saved
import chefmate.client.grocery.core.public.generated.resources.grocery_item_deleted
import chefmate.client.grocery.core.public.generated.resources.grocery_undo
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
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class GroceryListBlocTest {
    val context = TestBlocContext.Companion.create()
    val output = TestConsumer<GroceryListBloc.Output>()
    val groceries = MutableSharedFlow<List<GroceryItem>>()
    val groceryLists = MutableSharedFlow<List<GroceryListModel>>()
    val settings = MapSettings()
    val repository: GroceryRepository = mock {
        every { getGroceries() } returns groceries.asSharedFlow()
        every { getGroceries(1L) } returns groceries.asSharedFlow()
        every { getGroceryLists() } returns groceryLists.asSharedFlow()
        every { getListCollaborators(1L) } returns kotlinx.coroutines.flow.flowOf(emptyList())
        every { getPendingInvitations() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        everySuspend { ensureDefaultList() } returns 1L
    }
    val autocompleteRepository = FakeGroceryAutocompleteRepository()
    val toastService = FakeToastService()

    var detailItemId: Long? = null
    var detailOutput: Consumer<GroceryDetailBloc.Output> = Consumer {}
    val groceryDetailFactory = GroceryDetailBloc.Factory { _, id, output ->
        detailItemId = id
        detailOutput = output
        mock(MockMode.autoUnit)
    }

    // Seed the sync coach mark as already seen so the default-state assertions below aren't
    // affected by the tooltip. The unseen path has its own dedicated test.
    val coachMarkController =
        CoachMarkController(MapSettings()).apply { dismiss(CoachMarkId.GROCERY_LIST_SYNC) }

    val bloc =
        GroceryListBlocImpl(
            context = context,
            output = output,
            viewModelFactory = {
                GroceryListViewModel(
                    mainContext = context.mainContext,
                    repository = repository,
                    autocompleteRepository = autocompleteRepository,
                    settings = settings,
                    coachMarkController = coachMarkController,
                    toastService = toastService,
                )
            },
            groceryDetailFactory = groceryDetailFactory,
            toastService = toastService,
        )

    @Test
    fun When_items_loaded_Then_state_is_updated() = runTest {
        bloc.state.test {
            awaitItem() shouldBe GroceryListBloc.Model()
            val items =
                listOf(
                    GroceryItem(
                        id = 1,
                        name = "Apples",
                        category = GroceryCategory.PRODUCE,
                        isChecked = false,
                    ),
                    GroceryItem(
                        id = 2,
                        name = "Bananas",
                        category = GroceryCategory.PRODUCE,
                        isChecked = true,
                    ),
                )
            groceries.emit(items)
            awaitItem() shouldBe
                GroceryListBloc.Model(
                    groupedItems =
                        persistentListOf(
                            GroceryGroup(
                                category = GroceryCategory.PRODUCE,
                                items = items.toImmutableList(),
                            )
                        ),
                    hasNoRecipeItems = true,
                )
        }
    }

    @Test
    fun When_grocery_item_checked_change_Then_repository_is_updated() = runTest {
        val item = GroceryItem(id = 1, name = "Apples", isChecked = false)
        everySuspend { repository.updateChecked(item, true) } returns Unit
        bloc.onGroceryItemCheckedChange(item, true)
        verifySuspend { repository.updateChecked(item, true) }
    }

    @Test
    fun When_grocery_item_deleted_Then_staged_and_undo_toast_shown() = runTest {
        val item = GroceryItem(id = 1, name = "Apples", isChecked = false)
        every { repository.stageDelete(item) } returns Unit
        bloc.onGroceryItemDelete(item)
        verify { repository.stageDelete(item) }
        toastService.shown shouldContain
            PhraseModel(Res.string.grocery_item_deleted, "item" to FixedString("Apples"))
        toastService.shownActionLabels shouldContain ResourceString(Res.string.grocery_undo)
        verifySuspend(VerifyMode.not) { repository.deleteGrocery(item) }
    }

    @Test
    fun When_delete_undo_tapped_Then_item_restored_and_not_deleted() = runTest {
        val item = GroceryItem(id = 1, name = "Apples", isChecked = false)
        every { repository.stageDelete(item) } returns Unit
        every { repository.undoDelete(item.id) } returns Unit
        bloc.onGroceryItemDelete(item)

        toastService.lastOnAction?.invoke()

        verify { repository.undoDelete(item.id) }
        verify(VerifyMode.not) { repository.commitDelete(item.id) }
    }

    @Test
    fun When_delete_toast_dismissed_Then_delete_committed() = runTest {
        val item = GroceryItem(id = 1, name = "Apples", isChecked = false)
        every { repository.stageDelete(item) } returns Unit
        every { repository.commitDelete(item.id) } returns Unit
        bloc.onGroceryItemDelete(item)

        toastService.lastOnDismiss?.invoke()

        verify { repository.commitDelete(item.id) }
        verify(VerifyMode.not) { repository.undoDelete(item.id) }
    }

    @Test
    fun When_alphabetical_sort_applied_Then_items_sorted_alphabetically() = runTest {
        val items =
            listOf(
                GroceryItem(
                    id = 1,
                    name = "Chicken",
                    displayName = "Chicken",
                    category = GroceryCategory.MEAT,
                    isChecked = false,
                ),
                GroceryItem(
                    id = 2,
                    name = "Apples",
                    displayName = "Apples",
                    category = GroceryCategory.PRODUCE,
                    isChecked = false,
                ),
                GroceryItem(
                    id = 3,
                    name = "Bread",
                    displayName = "Bread",
                    category = GroceryCategory.BAKERY,
                    isChecked = false,
                ),
            )
        groceries.emit(items)
        bloc.onApplySortAndFilter(
            GroceryListBloc.GrocerySort.ALPHABETICAL,
            GroceryListBloc.GroceryFilter.ALL,
        )
        bloc.state.test {
            val result = awaitItem()
            result.sort shouldBe GroceryListBloc.GrocerySort.ALPHABETICAL
            result.groupedItems.size shouldBe 1
            result.groupedItems[0].items.map { it.displayName } shouldBe
                listOf("Apples", "Bread", "Chicken")
        }
    }

    @Test
    fun When_sort_and_filter_applied_Then_state_reflects_both() = runTest {
        val items =
            listOf(
                GroceryItem(
                    id = 1,
                    name = "Apples",
                    displayName = "Apples",
                    category = GroceryCategory.PRODUCE,
                    isChecked = true,
                ),
                GroceryItem(
                    id = 2,
                    name = "Bananas",
                    displayName = "Bananas",
                    category = GroceryCategory.PRODUCE,
                    isChecked = false,
                ),
            )
        groceries.emit(items)
        bloc.onApplySortAndFilter(
            GroceryListBloc.GrocerySort.ALPHABETICAL,
            GroceryListBloc.GroceryFilter.UNPURCHASED,
        )
        bloc.state.test {
            val result = awaitItem()
            result.sort shouldBe GroceryListBloc.GrocerySort.ALPHABETICAL
            result.filter shouldBe GroceryListBloc.GroceryFilter.UNPURCHASED
            result.groupedItems.flatMap { it.items }.size shouldBe 1
            result.groupedItems.flatMap { it.items }.first().displayName shouldBe "Bananas"
        }
    }

    @Test
    fun When_recipe_filter_applied_Then_only_items_from_that_recipe_shown() = runTest {
        val items =
            listOf(
                GroceryItem(
                    id = 1,
                    name = "Flour",
                    displayName = "Flour",
                    category = GroceryCategory.BAKING,
                    isChecked = false,
                    recipeName = "Chocolate Cake",
                ),
                GroceryItem(
                    id = 2,
                    name = "Sugar",
                    displayName = "Sugar",
                    category = GroceryCategory.BAKING,
                    isChecked = false,
                    recipeName = "Chocolate Cake",
                ),
                GroceryItem(
                    id = 3,
                    name = "Chicken",
                    displayName = "Chicken",
                    category = GroceryCategory.MEAT,
                    isChecked = false,
                    recipeName = "Chicken Soup",
                ),
                GroceryItem(
                    id = 4,
                    name = "Salt",
                    displayName = "Salt",
                    category = GroceryCategory.SPICES,
                    isChecked = false,
                ),
            )
        groceries.emit(items)
        bloc.onApplySortAndFilter(
            GroceryListBloc.GrocerySort.AISLE,
            GroceryListBloc.GroceryFilter.ALL,
            recipeFilter = "Chocolate Cake",
        )
        bloc.state.test {
            val result = awaitItem()
            result.recipeFilter shouldBe "Chocolate Cake"
            val allItems = result.groupedItems.flatMap { it.items }
            allItems.size shouldBe 2
            allItems.map { it.displayName } shouldBe listOf("Flour", "Sugar")
        }
    }

    @Test
    fun When_no_recipe_filter_applied_Then_only_items_without_recipe_shown() = runTest {
        val items =
            listOf(
                GroceryItem(
                    id = 1,
                    name = "Flour",
                    displayName = "Flour",
                    category = GroceryCategory.BAKING,
                    isChecked = false,
                    recipeName = "Chocolate Cake",
                ),
                GroceryItem(
                    id = 2,
                    name = "Salt",
                    displayName = "Salt",
                    category = GroceryCategory.SPICES,
                    isChecked = false,
                ),
            )
        groceries.emit(items)
        bloc.onApplySortAndFilter(
            GroceryListBloc.GrocerySort.AISLE,
            GroceryListBloc.GroceryFilter.ALL,
            recipeFilter = GroceryListBloc.NO_RECIPE_FILTER,
        )
        bloc.state.test {
            val result = awaitItem()
            result.recipeFilter shouldBe GroceryListBloc.NO_RECIPE_FILTER
            val allItems = result.groupedItems.flatMap { it.items }
            allItems.size shouldBe 1
            allItems.first().displayName shouldBe "Salt"
        }
    }

    @Test
    fun When_available_recipes_exist_Then_model_contains_sorted_recipe_names() = runTest {
        bloc.state.test {
            awaitItem() shouldBe GroceryListBloc.Model()
            val items =
                listOf(
                    GroceryItem(
                        id = 1,
                        name = "Flour",
                        category = GroceryCategory.BAKING,
                        isChecked = false,
                        recipeName = "Chocolate Cake",
                    ),
                    GroceryItem(
                        id = 2,
                        name = "Chicken",
                        category = GroceryCategory.MEAT,
                        isChecked = false,
                        recipeName = "Chicken Soup",
                    ),
                    GroceryItem(
                        id = 3,
                        name = "Salt",
                        category = GroceryCategory.SPICES,
                        isChecked = false,
                    ),
                )
            groceries.emit(items)
            val result = awaitItem()
            result.availableRecipes shouldBe listOf("Chicken Soup", "Chocolate Cake")
            result.hasNoRecipeItems shouldBe true
        }
    }

    @Test
    fun When_item_name_entered_Then_autocomplete_suggestions_include_matching_items() = runTest {
        bloc.state.test {
            awaitItem() shouldBe GroceryListBloc.Model()
            groceries.emit(
                listOf(
                    GroceryItem(
                        id = 1,
                        name = "Milk chocolate",
                        displayName = "Milk chocolate",
                        category = GroceryCategory.BAKING,
                    ),
                    GroceryItem(
                        id = 2,
                        name = "Spinach",
                        displayName = "Spinach",
                        category = GroceryCategory.PRODUCE,
                    ),
                )
            )
            awaitItem().autocompleteSuggestions shouldBe emptyList()

            bloc.onNewGroceryItemNameChange("mi")

            val result = awaitItem()
            result.autocompleteSuggestions.take(2) shouldBe listOf("Milk chocolate", "Microgreen")
        }
    }

    @Test
    fun When_item_name_matches_suggestion_exactly_Then_exact_suggestion_is_hidden() = runTest {
        bloc.state.test {
            awaitItem() shouldBe GroceryListBloc.Model()
            groceries.emit(
                listOf(
                    GroceryItem(
                        id = 1,
                        name = "Eggs",
                        displayName = "Eggs",
                        category = GroceryCategory.DAIRY,
                    )
                )
            )
            awaitItem()

            bloc.onNewGroceryItemNameChange("sal")
            awaitItem().autocompleteSuggestions shouldBe
                listOf("Salami", "Salmon", "Salad", "Salsa", "Salt")

            bloc.onNewGroceryItemNameChange("salt")

            awaitItem().autocompleteSuggestions shouldBe emptyList()
        }
    }

    @Test
    fun When_custom_autocomplete_item_saved_Then_it_surfaces_in_suggestions() = runTest {
        bloc.state.test {
            awaitItem() shouldBe GroceryListBloc.Model()
            groceries.emit(
                listOf(
                    GroceryItem(
                        id = 1,
                        name = "Eggs",
                        displayName = "Eggs",
                        category = GroceryCategory.DAIRY,
                    )
                )
            )
            awaitItem()

            // "Zeppole" isn't in the built-in vocabulary — it only suggests once saved.
            autocompleteRepository.addItem("Zeppole")
            bloc.onNewGroceryItemNameChange("zep")

            awaitItem().autocompleteSuggestions shouldBe listOf("Zeppole")
        }
    }

    @Test
    fun When_onSaveAutocompleteItem_Then_name_is_persisted() = runTest {
        bloc.onSaveAutocompleteItem("Cold brew concentrate")

        autocompleteRepository.observeItems().first().map { it.name } shouldContain
            "Cold brew concentrate"
    }

    @Test
    fun When_onSaveAutocompleteItem_Then_toast_shown_with_manage_action() = runTest {
        bloc.onSaveAutocompleteItem("Cold brew concentrate")

        toastService.shown shouldContain ResourceString(Res.string.grocery_autocomplete_saved)
        toastService.shownActionLabels shouldContain
            ResourceString(Res.string.grocery_autocomplete_manage)

        // Tapping the toast action deep links to the autocomplete settings screen.
        toastService.lastOnAction?.invoke()
        output.lastValue shouldBe GroceryListBloc.Output.OpenAutocompleteSettings
    }

    @Test
    fun When_blank_autocomplete_item_saved_Then_no_toast_and_nothing_persisted() = runTest {
        bloc.onSaveAutocompleteItem("   ")

        toastService.shown shouldBe emptyList()
        autocompleteRepository.observeItems().first() shouldBe emptyList()
    }

    @Test
    fun When_query_matches_saved_autocomplete_item_Then_flag_is_set() = runTest {
        bloc.state.test {
            awaitItem() shouldBe GroceryListBloc.Model()
            groceries.emit(
                listOf(
                    GroceryItem(
                        id = 1,
                        name = "Eggs",
                        displayName = "Eggs",
                        category = GroceryCategory.DAIRY,
                    )
                )
            )
            awaitItem()

            autocompleteRepository.addItem("Zeppole")
            // A partial query keeps the dropdown open (flag stays false)…
            bloc.onNewGroceryItemNameChange("zep")
            awaitItem().queryMatchesSavedAutocomplete shouldBe false

            // …but once the text exactly matches the saved library item, the menu is suppressed.
            bloc.onNewGroceryItemNameChange("Zeppole")
            awaitItem().queryMatchesSavedAutocomplete shouldBe true
        }
    }

    @Test
    fun When_clear_filters_applied_Then_recipe_filter_is_also_cleared() = runTest {
        val items =
            listOf(
                GroceryItem(
                    id = 1,
                    name = "Flour",
                    displayName = "Flour",
                    category = GroceryCategory.BAKING,
                    isChecked = false,
                    recipeName = "Chocolate Cake",
                ),
                GroceryItem(
                    id = 2,
                    name = "Salt",
                    displayName = "Salt",
                    category = GroceryCategory.SPICES,
                    isChecked = false,
                ),
            )
        groceries.emit(items)
        bloc.onApplySortAndFilter(
            GroceryListBloc.GrocerySort.ALPHABETICAL,
            GroceryListBloc.GroceryFilter.ALL,
            recipeFilter = "Chocolate Cake",
        )
        bloc.onApplySortAndFilter(
            GroceryListBloc.GrocerySort.AISLE,
            GroceryListBloc.GroceryFilter.ALL,
            recipeFilter = null,
        )
        bloc.state.test {
            val result = awaitItem()
            result.recipeFilter shouldBe null
            result.sort shouldBe GroceryListBloc.GrocerySort.AISLE
            result.filter shouldBe GroceryListBloc.GroceryFilter.ALL
            result.groupedItems.flatMap { it.items }.size shouldBe 2
        }
    }

    @Test
    fun When_clear_filters_clicked_Then_filters_reset_but_sort_preserved() = runTest {
        val items =
            listOf(
                GroceryItem(
                    id = 1,
                    name = "Flour",
                    displayName = "Flour",
                    category = GroceryCategory.BAKING,
                    isChecked = false,
                    recipeName = "Chocolate Cake",
                ),
                GroceryItem(
                    id = 2,
                    name = "Salt",
                    displayName = "Salt",
                    category = GroceryCategory.SPICES,
                    isChecked = true,
                ),
            )
        groceries.emit(items)
        // Apply a sort + a purchase filter + a recipe filter that together hide everything.
        bloc.onApplySortAndFilter(
            GroceryListBloc.GrocerySort.ALPHABETICAL,
            GroceryListBloc.GroceryFilter.UNPURCHASED,
            recipeFilter = "Chocolate Cake",
        )
        bloc.onClearFiltersClicked()
        bloc.state.test {
            val result = awaitItem()
            result.filter shouldBe GroceryListBloc.GroceryFilter.ALL
            result.recipeFilter shouldBe null
            // Sort is intentionally preserved — it never causes an empty filtered list.
            result.sort shouldBe GroceryListBloc.GrocerySort.ALPHABETICAL
            result.groupedItems.flatMap { it.items }.size shouldBe 2
        }
    }

    @Test
    fun When_browse_recipes_clicked_Then_open_recipes_output_emitted() = runTest {
        bloc.onBrowseRecipesClicked()
        output.lastValue shouldBe GroceryListBloc.Output.OpenRecipes
    }

    @Test
    fun When_item_clicked_Then_detail_sheet_slot_activated() = runTest {
        val item = GroceryItem(id = 42, name = "Apples", isChecked = false)
        bloc.childSlot.value.child shouldBe null
        bloc.onGroceryItemClicked(item)
        bloc.childSlot.value.child?.instance.let {
            (it as GroceryListBloc.Sheet.GroceryDetail).bloc shouldBe it.bloc
        }
        detailItemId shouldBe 42L
    }

    @Test
    fun When_detail_finishes_Then_sheet_slot_dismissed() = runTest {
        val item = GroceryItem(id = 7, name = "Milk", isChecked = false)
        bloc.onGroceryItemClicked(item)
        bloc.childSlot.value.child shouldNotBe null
        detailOutput.onNext(GroceryDetailBloc.Output.Finished)
        bloc.childSlot.value.child shouldBe null
    }

    @Test
    fun When_dismiss_sheet_called_Then_slot_dismissed() = runTest {
        val item = GroceryItem(id = 9, name = "Cheese", isChecked = false)
        bloc.onGroceryItemClicked(item)
        bloc.childSlot.value.child shouldNotBe null
        bloc.onDismissSheet()
        bloc.childSlot.value.child shouldBe null
    }

    @Test
    fun When_sync_coach_mark_unseen_Then_shown_and_dismiss_persists() = runTest {
        val freshContext = TestBlocContext.Companion.create()
        val controller = CoachMarkController(MapSettings())
        val tooltipBloc =
            GroceryListBlocImpl(
                context = freshContext,
                output = output,
                viewModelFactory = {
                    GroceryListViewModel(
                        mainContext = freshContext.mainContext,
                        repository = repository,
                        autocompleteRepository = autocompleteRepository,
                        settings = MapSettings(),
                        coachMarkController = controller,
                        toastService = toastService,
                    )
                },
                groceryDetailFactory = groceryDetailFactory,
                toastService = toastService,
            )

        tooltipBloc.state.value.showSyncTooltip shouldBe true
        tooltipBloc.onSyncTooltipDismissed()
        tooltipBloc.state.value.showSyncTooltip shouldBe false
        controller.hasSeen(CoachMarkId.GROCERY_LIST_SYNC) shouldBe true
    }

    @Test
    fun When_items_have_recipe_name_Then_model_contains_recipe_name() = runTest {
        bloc.state.test {
            awaitItem() shouldBe GroceryListBloc.Model()
            val items =
                listOf(
                    GroceryItem(
                        id = 1,
                        name = "Flour",
                        displayName = "Flour",
                        category = GroceryCategory.BAKING,
                        isChecked = false,
                        recipeName = "Chocolate Cake",
                    ),
                    GroceryItem(
                        id = 2,
                        name = "Sugar",
                        displayName = "Sugar",
                        category = GroceryCategory.BAKING,
                        isChecked = false,
                    ),
                )
            groceries.emit(items)
            val result = awaitItem()
            val bakingItems = result.groupedItems.first { it.category == GroceryCategory.BAKING }
            bakingItems.items[0].recipeName shouldBe "Chocolate Cake"
            bakingItems.items[1].recipeName shouldBe null
        }
    }
}

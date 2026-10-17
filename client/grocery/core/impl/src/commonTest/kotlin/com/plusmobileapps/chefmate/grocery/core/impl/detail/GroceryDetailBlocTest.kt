@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.grocery.core.impl.detail

import app.cash.turbine.test
import com.plusmobileapps.chefmate.grocery.core.detail.GroceryDetailBloc
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryCategoryOverrideRepository
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.matcher.matches
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class GroceryDetailBlocTest {
    val context = TestBlocContext.create()
    val groceryItem = GroceryItem(id = 1L, name = "Milk", isChecked = false)
    val repository: GroceryRepository = mock { everySuspend { getGrocery(1L) } returns groceryItem }
    val overrideRepository = FakeGroceryCategoryOverrideRepository()
    val testConsumer = TestConsumer<GroceryDetailBloc.Output>()

    val bloc =
        GroceryDetailBlocImpl(
            context = context,
            id = 1L,
            output = testConsumer,
            viewModelFactory = { id ->
                GroceryDetailViewModel(
                    id = id,
                    mainContext = context.mainContext,
                    repository = repository,
                    categoryOverrideRepository = overrideRepository,
                )
            },
        )

    @Test
    fun WHEN_grocery_loaded_THEN_update_state_with_grocery_details() {
        runTest {
            bloc.models.test {
                awaitItem() shouldBe GroceryDetailBloc.Model.Loaded(item = groceryItem)
            }
        }
    }

    @Test
    fun WHEN_grocery_name_changed_THEN_update_state_with_new_display_name() {
        runTest {
            bloc.onGroceryNameChanged("Bread")
            bloc.models.test {
                awaitItem() shouldBe
                    GroceryDetailBloc.Model.Loaded(item = groceryItem.copy(displayName = "Bread"))
            }
        }
    }

    @Test
    fun WHEN_grocery_quantity_changed_THEN_update_state_with_new_quantity() {
        runTest {
            bloc.onGroceryQuantityChanged("2 cups")
            bloc.models.test {
                awaitItem() shouldBe
                    GroceryDetailBloc.Model.Loaded(item = groceryItem.copy(quantity = "2 cups"))
            }
        }
    }

    @Test
    fun WHEN_quantity_cleared_THEN_quantity_becomes_null() {
        runTest {
            bloc.onGroceryQuantityChanged("2 cups")
            bloc.onGroceryQuantityChanged("")
            bloc.models.test {
                awaitItem() shouldBe
                    GroceryDetailBloc.Model.Loaded(item = groceryItem.copy(quantity = null))
            }
        }
    }

    @Test
    fun WHEN_grocery_checked_changed_THEN_update_state_with_new_isChecked() {
        runTest {
            bloc.onGroceryCheckedChanged(true)
            bloc.models.test {
                awaitItem() shouldBe
                    GroceryDetailBloc.Model.Loaded(item = groceryItem.copy(isChecked = true))
            }
        }
    }

    @Test
    fun WHEN_aisle_changed_THEN_update_state_with_new_category() {
        runTest {
            bloc.onAisleChanged(GroceryCategory.DAIRY)
            bloc.models.test {
                awaitItem() shouldBe
                    GroceryDetailBloc.Model.Loaded(
                        item = groceryItem.copy(category = GroceryCategory.DAIRY)
                    )
            }
        }
    }

    @Test
    fun WHEN_save_clicked_THEN_persist_combined_name_and_emit_finished_output() {
        runTest {
            everySuspend { repository.updateGrocery(any()) } returns Unit

            bloc.onGroceryQuantityChanged("2 cups")
            bloc.onGroceryNameChanged("Flour")
            bloc.onGroceryCheckedChanged(true)
            bloc.onSaveClicked()

            verifySuspend {
                repository.updateGrocery(
                    matches {
                        it.name == "2 cups Flour" &&
                            it.displayName == "Flour" &&
                            it.quantity == "2 cups" &&
                            it.isChecked
                    }
                )
            }
            testConsumer.lastValue shouldBe GroceryDetailBloc.Output.Finished
        }
    }

    @Test
    fun WHEN_save_clicked_with_no_quantity_THEN_persist_display_name_only() {
        runTest {
            everySuspend { repository.updateGrocery(any()) } returns Unit

            bloc.onGroceryNameChanged("Bread")
            bloc.onSaveClicked()

            verifySuspend {
                repository.updateGrocery(matches { it.name == "Bread" && it.quantity == null })
            }
            testConsumer.lastValue shouldBe GroceryDetailBloc.Output.Finished
        }
    }

    @Test
    fun GIVEN_blank_grocery_name_WHEN_save_clicked_THEN_do_not_update_repository_or_emit_finished_output() {
        runTest {
            bloc.onGroceryNameChanged("")
            bloc.onSaveClicked()

            verifySuspend { repository.getGrocery(1L) }
            testConsumer.values.size shouldBe 0
        }
    }

    @Test
    fun WHEN_quantity_incremented_THEN_bump_the_amount_and_keep_the_unit() {
        runTest {
            bloc.onGroceryQuantityChanged("1 gal")
            bloc.onQuantityIncrementClicked()

            bloc.models.test {
                awaitItem() shouldBe
                    GroceryDetailBloc.Model.Loaded(item = groceryItem.copy(quantity = "2 gal"))
            }
        }
    }

    @Test
    fun WHEN_quantity_incremented_from_empty_THEN_start_at_one() {
        runTest {
            bloc.onQuantityIncrementClicked()

            bloc.models.test {
                awaitItem() shouldBe
                    GroceryDetailBloc.Model.Loaded(item = groceryItem.copy(quantity = "1"))
            }
        }
    }

    @Test
    fun WHEN_quantity_decremented_THEN_lower_the_amount() {
        runTest {
            bloc.onGroceryQuantityChanged("3 cups")
            bloc.onQuantityDecrementClicked()

            bloc.models.test {
                awaitItem() shouldBe
                    GroceryDetailBloc.Model.Loaded(item = groceryItem.copy(quantity = "2 cups"))
            }
        }
    }

    @Test
    fun WHEN_quantity_decremented_at_one_THEN_leave_it_alone() {
        runTest {
            bloc.onGroceryQuantityChanged("1 cup")
            bloc.onQuantityDecrementClicked()

            bloc.models.test {
                awaitItem() shouldBe
                    GroceryDetailBloc.Model.Loaded(item = groceryItem.copy(quantity = "1 cup"))
            }
        }
    }

    @Test
    fun WHEN_back_clicked_THEN_emit_finished_output() {
        runTest {
            bloc.onBackClicked()
            testConsumer.lastValue shouldBe GroceryDetailBloc.Output.Finished
        }
    }

    @Test
    fun WHEN_always_file_here_toggled_on_THEN_persist_rule_and_check_the_box() {
        runTest {
            bloc.onAisleChanged(GroceryCategory.DAIRY)
            bloc.onAlwaysFileHereToggled(true)

            overrideRepository.observeOverrideMap().first() shouldBe
                mapOf("milk" to GroceryCategory.DAIRY)
            bloc.models.test {
                (awaitItem() as GroceryDetailBloc.Model.Loaded).alwaysFileHere shouldBe true
            }
        }
    }

    @Test
    fun WHEN_always_file_here_toggled_off_THEN_remove_the_rule() {
        runTest {
            bloc.onAlwaysFileHereToggled(true)
            bloc.onAlwaysFileHereToggled(false)

            overrideRepository.observeOverrideMap().first() shouldBe emptyMap()
            bloc.models.test {
                (awaitItem() as GroceryDetailBloc.Model.Loaded).alwaysFileHere shouldBe false
            }
        }
    }

    @Test
    fun GIVEN_rule_active_WHEN_aisle_changed_THEN_rule_follows_the_new_aisle() {
        runTest {
            bloc.onAlwaysFileHereToggled(true)
            bloc.onAisleChanged(GroceryCategory.BEVERAGES)

            overrideRepository.observeOverrideMap().first() shouldBe
                mapOf("milk" to GroceryCategory.BEVERAGES)
        }
    }

    @Test
    fun GIVEN_existing_rule_for_name_WHEN_loaded_THEN_box_is_checked() {
        runTest {
            val itemInOverriddenAisle = groceryItem.copy(category = GroceryCategory.BEVERAGES)
            val repo: GroceryRepository = mock {
                everySuspend { getGrocery(1L) } returns itemInOverriddenAisle
            }
            val overrides = FakeGroceryCategoryOverrideRepository()
            overrides.setOverride("Milk", GroceryCategory.BEVERAGES)
            val blocWithRule =
                GroceryDetailBlocImpl(
                    context = TestBlocContext.create(),
                    id = 1L,
                    output = TestConsumer(),
                    viewModelFactory = { id ->
                        GroceryDetailViewModel(
                            id = id,
                            mainContext = context.mainContext,
                            repository = repo,
                            categoryOverrideRepository = overrides,
                        )
                    },
                )

            blocWithRule.models.test {
                (awaitItem() as GroceryDetailBloc.Model.Loaded).alwaysFileHere shouldBe true
            }
        }
    }
}

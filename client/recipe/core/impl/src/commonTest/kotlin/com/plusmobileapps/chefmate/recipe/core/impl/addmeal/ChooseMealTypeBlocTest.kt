@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.recipe.core.impl.addmeal

import app.cash.turbine.test
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.meal.data.testing.FakeMealPlanScopeRepository
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseMealTypeBloc
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class ChooseMealTypeBlocTest {
    private val context = TestBlocContext.create()
    private val output = TestConsumer<ChooseMealTypeBloc.Output>()
    private val recipeId = 42L
    private val date = "2026-04-21"

    private val recipeRepository: RecipeRepository = mock {
        everySuspend { getRecipe(recipeId) } returns
            flowOf(
                com.plusmobileapps.chefmate.recipe.data.Recipe(
                    id = recipeId,
                    title = "Test Recipe",
                    description = null,
                    imageUrl = null,
                    sourceUrl = null,
                    servings = null,
                    prepTime = null,
                    cookTime = null,
                    totalTime = null,
                    calories = null,
                    starRating = null,
                    ingredients = "",
                    directions = "",
                    isFavorite = false,
                    createdAt = Instant.DISTANT_PAST,
                    updatedAt = Instant.DISTANT_PAST,
                )
            )
    }

    private val mealPlanRepository: MealPlanRepository = mock {
        everySuspend { addMeal(recipeId, date, MealType.DINNER) } returns Unit
        everySuspend { addMeal(recipeId, date, MealType.BREAKFAST) } returns Unit
        everySuspend {
            addMeal(recipeId, date, MealType.DINNER, MealPlanScope.Family("fam-1"))
        } returns Unit
    }

    private val scopeRepository = FakeMealPlanScopeRepository()

    private fun createBloc(): ChooseMealTypeBlocImpl =
        ChooseMealTypeBlocImpl(
            context = context,
            recipeId = recipeId,
            date = date,
            output = output,
            viewModelFactory =
                ChooseMealTypeViewModel.Factory { rid, d ->
                    ChooseMealTypeViewModel(
                        recipeId = rid,
                        date = d,
                        mainContext = context.mainContext,
                        recipeRepository = recipeRepository,
                        mealPlanRepository = mealPlanRepository,
                        scopeRepository = scopeRepository,
                    )
                },
        )

    @Test
    fun WHEN_created_THEN_loads_recipe_title() = runTest {
        val bloc = createBloc()
        bloc.state.test {
            val state = awaitItem()
            state.recipeTitle shouldBe "Test Recipe"
            state.selectedDate shouldBe date
            state.selectedMealType shouldBe MealType.DINNER
        }
    }

    @Test
    fun WHEN_meal_type_selected_THEN_state_updated() = runTest {
        val bloc = createBloc()
        bloc.onMealTypeSelected(MealType.BREAKFAST)
        bloc.state.test { awaitItem().selectedMealType shouldBe MealType.BREAKFAST }
    }

    @Test
    fun WHEN_save_clicked_THEN_meal_saved_and_output_finished() = runTest {
        val bloc = createBloc()
        bloc.onSaveClicked()

        verifySuspend { mealPlanRepository.addMeal(recipeId, date, MealType.DINNER) }
        output.lastValue shouldBe ChooseMealTypeBloc.Output.Finished
    }

    @Test
    fun WHEN_a_family_plan_is_selected_THEN_meal_is_added_to_it() = runTest {
        scopeRepository.selectedScope.value = MealPlanScope.Family("fam-1")
        val bloc = createBloc()
        bloc.onSaveClicked()

        verifySuspend {
            mealPlanRepository.addMeal(
                recipeId,
                date,
                MealType.DINNER,
                MealPlanScope.Family("fam-1"),
            )
        }
    }

    @Test
    fun WHEN_back_clicked_THEN_output_back() {
        val bloc = createBloc()
        bloc.onBackClicked()
        output.lastValue shouldBe ChooseMealTypeBloc.Output.Back
    }
}

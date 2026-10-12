@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.recipe.core.impl.addmeal

import app.cash.turbine.test
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseDateBloc
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.testing.FakeRecipeRepository
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import com.plusmobileapps.chefmate.util.testing.FakeDateTimeUtil
import com.russhwolf.settings.MapSettings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify.VerifyMode
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class ChooseDateBlocTest {
    private val context = TestBlocContext.create()
    private val output = TestConsumer<ChooseDateBloc.Output>()
    private val mealsFlow = MutableSharedFlow<List<MealPlanItem>>()
    private val fakeDate = LocalDate(2026, 4, 21)
    private val dateTimeUtil = FakeDateTimeUtil(fakeToday = fakeDate)
    private val settings = MapSettings()
    private val recipeId = 42L
    private val recipeRepository =
        FakeRecipeRepository(
            MutableStateFlow(listOf(Recipe.Sample.copy(id = recipeId, title = "Test Recipe")))
        )

    private val repository: MealPlanRepository = mock {
        every { getMealsByDateRange("2026-04-01", "2026-04-30") } returns mealsFlow
        everySuspend { addMeal(any(), any(), any()) } returns Unit
    }

    private fun createBloc(): ChooseDateBlocImpl =
        ChooseDateBlocImpl(
            context = context,
            recipeId = recipeId,
            output = output,
            viewModelFactory = { rid ->
                ChooseDateViewModel(
                    recipeId = rid,
                    mainContext = context.mainContext,
                    mealPlanRepository = repository,
                    recipeRepository = recipeRepository,
                    dateTimeUtil = dateTimeUtil,
                    settings = settings,
                )
            },
        )

    @Test
    fun WHEN_created_THEN_defaults_to_today() = runTest {
        val bloc = createBloc()
        bloc.state.test {
            val state = awaitItem()
            state.selectedDate shouldBe fakeDate
            state.monthLabel shouldBe "April 2026"
            state.firstDayOfMonth shouldBe LocalDate(2026, 4, 1)
            state.formattedSelectedDate shouldNotBe ""
        }
    }

    @Test
    fun WHEN_day_selected_THEN_state_updated() = runTest {
        val bloc = createBloc()
        val selectedDate = LocalDate(2026, 4, 15)
        bloc.onDaySelected(selectedDate)
        bloc.state.test {
            val state = awaitItem()
            state.selectedDate shouldBe selectedDate
        }
    }

    @Test
    fun WHEN_next_clicked_with_selected_date_THEN_output_date_selected() = runTest {
        val bloc = createBloc()
        val selectedDate = LocalDate(2026, 4, 15)
        bloc.onDaySelected(selectedDate)
        bloc.onNextClicked()
        output.lastValue shouldBe ChooseDateBloc.Output.DateSelected(selectedDate)
    }

    @Test
    fun WHEN_back_clicked_THEN_output_back() {
        val bloc = createBloc()
        bloc.onBackClicked()
        output.lastValue shouldBe ChooseDateBloc.Output.Back
    }

    @Test
    fun WHEN_next_month_THEN_month_advances() = runTest {
        every { repository.getMealsByDateRange("2026-05-01", "2026-05-31") } returns mealsFlow
        val bloc = createBloc()
        bloc.onNextMonth()
        bloc.state.test {
            val state = awaitItem()
            state.monthLabel shouldBe "May 2026"
            state.firstDayOfMonth shouldBe LocalDate(2026, 5, 1)
            state.selectedDate shouldBe null
        }
    }

    @Test
    fun WHEN_previous_month_THEN_month_goes_back() = runTest {
        every { repository.getMealsByDateRange("2026-03-01", "2026-03-31") } returns mealsFlow
        val bloc = createBloc()
        bloc.onPreviousMonth()
        bloc.state.test {
            val state = awaitItem()
            state.monthLabel shouldBe "March 2026"
            state.firstDayOfMonth shouldBe LocalDate(2026, 3, 1)
            state.selectedDate shouldBe null
        }
    }

    @Test
    fun WHEN_today_clicked_THEN_resets_to_today() = runTest {
        every { repository.getMealsByDateRange("2026-05-01", "2026-05-31") } returns mealsFlow
        val bloc = createBloc()
        bloc.onNextMonth()
        bloc.onTodayClicked()
        bloc.state.test {
            val state = awaitItem()
            state.selectedDate shouldBe fakeDate
            state.monthLabel shouldBe "April 2026"
        }
    }

    @Test
    fun WHEN_meals_emitted_THEN_days_with_meals_updated() = runTest {
        val bloc = createBloc()
        val meals =
            listOf(
                MealPlanItem(
                    id = 1,
                    recipeId = 10,
                    recipeTitle = "Pancakes",
                    recipeImageUrl = null,
                    date = "2026-04-15",
                    mealType = MealType.BREAKFAST,
                )
            )
        mealsFlow.emit(meals)
        bloc.state.test {
            val state = awaitItem()
            state.daysWithMeals shouldBe setOf("2026-04-15")
        }
    }

    @Test
    fun WHEN_saved_date_in_future_THEN_restores_saved_date() = runTest {
        settings.putString("choose_date_last_selected", "2026-04-25")
        val bloc = createBloc()
        bloc.state.test {
            val state = awaitItem()
            state.selectedDate shouldBe LocalDate(2026, 4, 25)
        }
    }

    @Test
    fun WHEN_saved_date_in_past_THEN_defaults_to_today() = runTest {
        settings.putString("choose_date_last_selected", "2026-04-10")
        val bloc = createBloc()
        bloc.state.test {
            val state = awaitItem()
            state.selectedDate shouldBe fakeDate
        }
    }

    @Test
    fun WHEN_created_THEN_loads_recipe_title_and_defaults_to_dinner() = runTest {
        val bloc = createBloc()
        bloc.state.test {
            val state = awaitItem()
            state.recipeTitle shouldBe "Test Recipe"
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
    fun WHEN_save_clicked_THEN_adds_meal_for_selected_date_and_type_and_finishes() = runTest {
        val bloc = createBloc()
        bloc.onDaySelected(LocalDate(2026, 4, 25))
        bloc.onMealTypeSelected(MealType.LUNCH)
        bloc.onSaveClicked()
        verifySuspend { repository.addMeal(recipeId, "2026-04-25", MealType.LUNCH) }
        output.lastValue shouldBe ChooseDateBloc.Output.Finished
    }

    @Test
    fun WHEN_save_clicked_without_selected_date_THEN_does_nothing() = runTest {
        every { repository.getMealsByDateRange("2026-05-01", "2026-05-31") } returns mealsFlow
        val bloc = createBloc()
        bloc.onNextMonth()
        bloc.onSaveClicked()
        verifySuspend(VerifyMode.not) { repository.addMeal(any(), any(), any()) }
        output.values shouldBe emptyList()
    }
}

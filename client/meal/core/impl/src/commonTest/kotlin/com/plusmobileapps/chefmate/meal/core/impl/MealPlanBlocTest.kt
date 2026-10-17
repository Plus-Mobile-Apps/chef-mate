@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.meal.core.impl

import app.cash.turbine.test
import chefmate.client.meal.core.public.generated.resources.Res
import chefmate.client.meal.core.public.generated.resources.meal_plan_added_to_cook_mode_multiple
import chefmate.client.meal.core.public.generated.resources.meal_plan_added_to_cook_mode_single
import chefmate.client.meal.core.public.generated.resources.meal_plan_replaced_cook_mode_multiple
import chefmate.client.meal.core.public.generated.resources.meal_plan_replaced_cook_mode_single
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.resume
import com.plusmobileapps.chefmate.cook.data.CookingSessionRepository
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.meal.core.MealPlanBloc
import com.plusmobileapps.chefmate.meal.data.FamilyPlan
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.meal.data.testing.FakeMealPlanScopeRepository
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import com.plusmobileapps.chefmate.util.testing.FakeDateTimeUtil
import com.russhwolf.settings.MapSettings
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class MealPlanBlocTest {
    val context = TestBlocContext.Companion.create()
    val output = TestConsumer<MealPlanBloc.Output>()
    val mealsFlow = MutableSharedFlow<List<MealPlanItem>>()
    val fakeDate = LocalDate(2026, 4, 17)
    val dateTimeUtil = FakeDateTimeUtil(fakeToday = fakeDate)
    val coachMarkController = CoachMarkController(MapSettings())

    val repository: MealPlanRepository = mock {
        every { getMealsByDate("2026-04-17") } returns mealsFlow
        every { getMealsByDateRange("2026-04-13", "2026-04-19") } returns mealsFlow
    }

    val cookingSessionRepository: CookingSessionRepository = mock {
        every { observeRecipeIds() } returns flowOf(emptyList())
        everySuspend { start(any()) } returns Unit
        everySuspend { replaceAll(any()) } returns Unit
        everySuspend { markSelected(any()) } returns Unit
        everySuspend { stop(any()) } returns Unit
        everySuspend { stopAll() } returns Unit
    }

    val toastService = FakeToastService()

    val scopeRepository = FakeMealPlanScopeRepository()

    val settings = MapSettings()

    val bloc = createBloc()

    private fun createBloc() =
        MealPlanBlocImpl(
            context = TestBlocContext.create(),
            output = output,
            viewModelFactory = {
                MealPlanViewModel(
                    mainContext = context.mainContext,
                    repository = repository,
                    scopeRepository = scopeRepository,
                    cookingSessionRepository = cookingSessionRepository,
                    dateTimeUtil = dateTimeUtil,
                    coachMarkController = coachMarkController,
                    toastService = toastService,
                    settings = settings,
                )
            },
        )

    @Test
    fun WHEN_user_has_no_family_plans_THEN_no_plan_selector() = runTest {
        bloc.state.value.planSelector shouldBe null
    }

    @Test
    fun WHEN_user_has_family_plans_THEN_selector_shows_personal_plan_selected() = runTest {
        scopeRepository.familyPlans.value = listOf(FamilyPlan("fam-1", "Smiths"))

        val selector = bloc.state.value.planSelector!!
        selector.families shouldBe listOf(FamilyPlan("fam-1", "Smiths"))
        selector.selectedFamilyId shouldBe null
    }

    @Test
    fun WHEN_plan_selector_clicked_THEN_picker_opens_and_dismisses() = runTest {
        scopeRepository.familyPlans.value = listOf(FamilyPlan("fam-1", "Smiths"))

        bloc.onPlanSelectorClicked()
        bloc.state.value.planSelector!!.isPickerOpen shouldBe true

        bloc.onPlanPickerDismissed()
        bloc.state.value.planSelector!!.isPickerOpen shouldBe false
    }

    @Test
    fun WHEN_family_plan_selected_THEN_its_meals_are_shown() = runTest {
        val familyMeals = MutableStateFlow(listOf(sampleMeal(id = 9, title = "Family Tacos")))
        every { repository.getMealsByDate("2026-04-17", MealPlanScope.Family("fam-1")) } returns
            familyMeals
        scopeRepository.familyPlans.value = listOf(FamilyPlan("fam-1", "Smiths"))
        bloc.onPlanSelectorClicked()

        bloc.onPlanSelected("fam-1")

        scopeRepository.selectedScope.value shouldBe MealPlanScope.Family("fam-1")
        val state = bloc.state.value
        state.planSelector!!.selectedFamilyId shouldBe "fam-1"
        state.planSelector!!.isPickerOpen shouldBe false
        state.dayMeals!!.dinner.map { it.recipeTitle } shouldBe listOf("Family Tacos")
    }

    @Test
    fun WHEN_personal_plan_selected_THEN_scope_is_personal() = runTest {
        every { repository.getMealsByDate("2026-04-17", MealPlanScope.Family("fam-1")) } returns
            mealsFlow
        scopeRepository.familyPlans.value = listOf(FamilyPlan("fam-1", "Smiths"))
        bloc.onPlanSelected("fam-1")

        bloc.onPlanSelected(null)

        scopeRepository.selectedScope.value shouldBe MealPlanScope.Personal
    }

    @Test
    fun WHEN_resumed_on_a_family_plan_THEN_families_reload_and_meals_sync() = runTest {
        every { repository.getMealsByDate("2026-04-17", MealPlanScope.Family("fam-1")) } returns
            mealsFlow
        everySuspend { repository.syncAllUnsynced() } returns Unit
        scopeRepository.selectedScope.value = MealPlanScope.Family("fam-1")
        val lifecycle = LifecycleRegistry()

        MealPlanBlocImpl(
            context = TestBlocContext.create(lifecycle = lifecycle),
            output = output,
            viewModelFactory = {
                MealPlanViewModel(
                    mainContext = context.mainContext,
                    repository = repository,
                    scopeRepository = scopeRepository,
                    cookingSessionRepository = cookingSessionRepository,
                    dateTimeUtil = dateTimeUtil,
                    coachMarkController = coachMarkController,
                    toastService = toastService,
                    settings = settings,
                )
            },
        )
        val refreshesBefore = scopeRepository.refreshCount
        lifecycle.resume()

        scopeRepository.refreshCount shouldBe refreshesBefore + 1
        verifySuspend { repository.syncAllUnsynced() }
    }

    private fun sampleMeal(id: Long, title: String) =
        MealPlanItem(
            id = id,
            recipeId = id * 10,
            recipeTitle = title,
            recipeImageUrl = null,
            date = "2026-04-17",
            mealType = MealType.DINNER,
            familyId = "fam-1",
        )

    @Test
    fun WHEN_screen_opens_THEN_first_coach_mark_active() = runTest {
        bloc.state.value.activeCoachMark shouldBe CoachMarkId.MEAL_PLAN_ADD_MEAL
    }

    @Test
    fun WHEN_active_coach_mark_dismissed_THEN_persisted_and_next_shown() = runTest {
        bloc.onCoachMarkDismissed(CoachMarkId.MEAL_PLAN_ADD_MEAL)

        coachMarkController.hasSeen(CoachMarkId.MEAL_PLAN_ADD_MEAL) shouldBe true
        bloc.state.value.activeCoachMark shouldBe CoachMarkId.MEAL_PLAN_VIEW_MODE
    }

    @Test
    fun WHEN_inactive_coach_mark_dismissed_THEN_ignored() = runTest {
        bloc.onCoachMarkDismissed(CoachMarkId.MEAL_PLAN_VIEW_MODE)

        coachMarkController.hasSeen(CoachMarkId.MEAL_PLAN_VIEW_MODE) shouldBe false
        bloc.state.value.activeCoachMark shouldBe CoachMarkId.MEAL_PLAN_ADD_MEAL
    }

    @Test
    fun WHEN_meals_loaded_THEN_state_is_updated_with_day_meals() = runTest {
        bloc.state.test {
            awaitItem() // initial loading state

            val items =
                listOf(
                    MealPlanItem(
                        id = 1,
                        recipeId = 10,
                        recipeTitle = "Pancakes",
                        recipeImageUrl = null,
                        date = "2026-04-17",
                        mealType = MealType.BREAKFAST,
                    ),
                    MealPlanItem(
                        id = 2,
                        recipeId = 20,
                        recipeTitle = "Salad",
                        recipeImageUrl = null,
                        date = "2026-04-17",
                        mealType = MealType.LUNCH,
                    ),
                )
            mealsFlow.emit(items)

            val state = awaitItem()
            state.isLoading shouldBe false
            state.viewMode shouldBe MealPlanBloc.ViewMode.DAY
            state.dayMeals?.breakfast?.size shouldBe 1
            state.dayMeals?.lunch?.size shouldBe 1
            state.dayMeals?.dinner?.size shouldBe 0
        }
    }

    @Test
    fun WHEN_meal_clicked_THEN_output_open_recipe() = runTest {
        val item =
            MealPlanItem(
                id = 1,
                recipeId = 10,
                recipeTitle = "Pancakes",
                recipeImageUrl = null,
                date = "2026-04-17",
                mealType = MealType.BREAKFAST,
            )
        bloc.onMealClicked(item)
        output.lastValue shouldBe MealPlanBloc.Output.OpenRecipe(10)
    }

    @Test
    fun WHEN_delete_meal_clicked_THEN_state_shows_meal_to_delete() = runTest {
        val item =
            MealPlanItem(
                id = 1,
                recipeId = 10,
                recipeTitle = "Pancakes",
                recipeImageUrl = null,
                date = "2026-04-17",
                mealType = MealType.BREAKFAST,
            )
        bloc.onDeleteMealClicked(item)

        bloc.state.test { awaitItem().mealToDelete shouldBe item }
    }

    @Test
    fun WHEN_delete_meal_confirmed_THEN_meal_removed_and_dialog_dismissed() = runTest {
        val item =
            MealPlanItem(
                id = 1,
                recipeId = 10,
                recipeTitle = "Pancakes",
                recipeImageUrl = null,
                date = "2026-04-17",
                mealType = MealType.BREAKFAST,
            )
        everySuspend { repository.removeMeal(1) } returns Unit

        bloc.onDeleteMealClicked(item)
        bloc.onDeleteMealConfirmed()

        bloc.state.test { awaitItem().mealToDelete shouldBe null }
        verifySuspend { repository.removeMeal(1) }
    }

    @Test
    fun WHEN_delete_meal_dismissed_THEN_dialog_dismissed() = runTest {
        val item =
            MealPlanItem(
                id = 1,
                recipeId = 10,
                recipeTitle = "Pancakes",
                recipeImageUrl = null,
                date = "2026-04-17",
                mealType = MealType.BREAKFAST,
            )
        bloc.onDeleteMealClicked(item)
        bloc.onDeleteMealDismissed()

        bloc.state.test { awaitItem().mealToDelete shouldBe null }
    }

    @Test
    fun WHEN_week_mode_selected_THEN_mode_switches_to_week() = runTest {
        every { repository.getMealsByDateRange("2026-04-12", "2026-04-18") } returns mealsFlow

        bloc.onViewModeSelected(MealPlanBloc.ViewMode.WEEK)

        bloc.state.test { awaitItem().viewMode shouldBe MealPlanBloc.ViewMode.WEEK }
    }

    @Test
    fun WHEN_month_mode_selected_THEN_mode_switches_to_month() = runTest {
        every { repository.getMealsByDateRange("2026-04-01", "2026-04-30") } returns mealsFlow

        bloc.onViewModeSelected(MealPlanBloc.ViewMode.MONTH)

        bloc.state.test { awaitItem().viewMode shouldBe MealPlanBloc.ViewMode.MONTH }
    }

    @Test
    fun WHEN_view_mode_selected_THEN_it_is_restored_on_next_launch() = runTest {
        every { repository.getMealsByDateRange("2026-04-01", "2026-04-30") } returns mealsFlow

        bloc.onViewModeSelected(MealPlanBloc.ViewMode.MONTH)

        // A new bloc backed by the same settings stands in for a relaunch of the app.
        createBloc().state.test { awaitItem().viewMode shouldBe MealPlanBloc.ViewMode.MONTH }
    }

    @Test
    fun WHEN_no_view_mode_persisted_THEN_day_mode_is_used() = runTest {
        createBloc().state.test { awaitItem().viewMode shouldBe MealPlanBloc.ViewMode.DAY }
    }

    @Test
    fun WHEN_persisted_view_mode_is_unknown_THEN_day_mode_is_used() = runTest {
        settings.putString("meal_plan_view_mode", "YEAR")

        createBloc().state.test { awaitItem().viewMode shouldBe MealPlanBloc.ViewMode.DAY }
    }

    @Test
    fun WHEN_next_clicked_in_day_mode_THEN_date_advances_one_day() = runTest {
        every { repository.getMealsByDate("2026-04-18") } returns mealsFlow

        bloc.onNextClicked()

        bloc.state.test {
            val state = awaitItem()
            state.dateLabel shouldBe
                FixedString(dateTimeUtil.formatLocalDate(LocalDate(2026, 4, 18)))
        }
    }

    @Test
    fun WHEN_previous_clicked_in_day_mode_THEN_date_goes_back_one_day() = runTest {
        every { repository.getMealsByDate("2026-04-16") } returns mealsFlow

        bloc.onPreviousClicked()

        bloc.state.test {
            val state = awaitItem()
            state.dateLabel shouldBe
                FixedString(dateTimeUtil.formatLocalDate(LocalDate(2026, 4, 16)))
        }
    }

    @Test
    fun WHEN_replace_cook_mode_clicked_THEN_confirmation_pending_and_session_untouched() = runTest {
        val meals =
            listOf(
                MealPlanItem(
                    id = 1,
                    recipeId = 10,
                    recipeTitle = "Pancakes",
                    recipeImageUrl = null,
                    date = "2026-04-17",
                    mealType = MealType.BREAKFAST,
                )
            )

        bloc.onReplaceCookModeClicked(meals)

        bloc.state.test { awaitItem().pendingReplaceCookMode shouldBe meals }
    }

    @Test
    fun WHEN_replace_cook_mode_confirmed_THEN_repository_replace_all_called_with_distinct_ids_and_snackbar_uses_replaced_copy() =
        runTest {
            val meals =
                listOf(
                    MealPlanItem(
                        id = 1,
                        recipeId = 10,
                        recipeTitle = "Pancakes",
                        recipeImageUrl = null,
                        date = "2026-04-17",
                        mealType = MealType.BREAKFAST,
                    ),
                    MealPlanItem(
                        id = 2,
                        recipeId = 20,
                        recipeTitle = "Eggs",
                        recipeImageUrl = null,
                        date = "2026-04-17",
                        mealType = MealType.BREAKFAST,
                    ),
                )

            bloc.onReplaceCookModeClicked(meals)
            bloc.onReplaceCookModeConfirmed()

            bloc.state.test { awaitItem().pendingReplaceCookMode shouldBe null }
            toastService.shown.last() shouldBe
                PhraseModel(
                    Res.string.meal_plan_replaced_cook_mode_multiple,
                    "count" to FixedString("2"),
                )
            verifySuspend { cookingSessionRepository.replaceAll(listOf(10L, 20L)) }
        }

    @Test
    fun WHEN_replace_cook_mode_confirmed_with_single_recipe_THEN_snackbar_uses_recipe_title() =
        runTest {
            val meals =
                listOf(
                    MealPlanItem(
                        id = 1,
                        recipeId = 10,
                        recipeTitle = "Pancakes",
                        recipeImageUrl = null,
                        date = "2026-04-17",
                        mealType = MealType.BREAKFAST,
                    )
                )

            bloc.onReplaceCookModeClicked(meals)
            bloc.onReplaceCookModeConfirmed()

            toastService.shown.last() shouldBe
                PhraseModel(
                    Res.string.meal_plan_replaced_cook_mode_single,
                    "name" to FixedString("Pancakes"),
                )
        }

    @Test
    fun WHEN_replace_cook_mode_dismissed_THEN_dialog_dismissed_without_clearing() = runTest {
        val meals =
            listOf(
                MealPlanItem(
                    id = 1,
                    recipeId = 10,
                    recipeTitle = "Pancakes",
                    recipeImageUrl = null,
                    date = "2026-04-17",
                    mealType = MealType.BREAKFAST,
                )
            )

        bloc.onReplaceCookModeClicked(meals)
        bloc.onReplaceCookModeDismissed()

        bloc.state.test { awaitItem().pendingReplaceCookMode shouldBe null }
    }

    @Test
    fun WHEN_add_to_cook_mode_single_recipe_THEN_snackbar_uses_recipe_title() = runTest {
        val meals =
            listOf(
                MealPlanItem(
                    id = 1,
                    recipeId = 10,
                    recipeTitle = "Pancakes",
                    recipeImageUrl = null,
                    date = "2026-04-17",
                    mealType = MealType.BREAKFAST,
                )
            )

        bloc.onAddToCookModeClicked(meals)

        toastService.shown.last() shouldBe
            PhraseModel(
                Res.string.meal_plan_added_to_cook_mode_single,
                "name" to FixedString("Pancakes"),
            )
        verifySuspend { cookingSessionRepository.start(10) }
        verifySuspend { cookingSessionRepository.markSelected(10) }
    }

    @Test
    fun WHEN_add_to_cook_mode_multiple_recipes_THEN_snackbar_uses_count() = runTest {
        val meals =
            listOf(
                MealPlanItem(
                    id = 1,
                    recipeId = 10,
                    recipeTitle = "Pancakes",
                    recipeImageUrl = null,
                    date = "2026-04-17",
                    mealType = MealType.BREAKFAST,
                ),
                MealPlanItem(
                    id = 2,
                    recipeId = 20,
                    recipeTitle = "Eggs",
                    recipeImageUrl = null,
                    date = "2026-04-17",
                    mealType = MealType.BREAKFAST,
                ),
            )

        bloc.onAddToCookModeClicked(meals)

        toastService.shown.last() shouldBe
            PhraseModel(
                Res.string.meal_plan_added_to_cook_mode_multiple,
                "count" to FixedString("2"),
            )
    }

    @Test
    fun WHEN_continue_cooking_clicked_with_active_session_THEN_output_open_cook_mode_with_first_recipe() =
        runTest {
            val sessionIdsFlow = MutableStateFlow(listOf(42L, 7L))
            val sessionRepo: CookingSessionRepository = mock {
                every { observeRecipeIds() } returns sessionIdsFlow
            }
            val freshBloc =
                MealPlanBlocImpl(
                    context = TestBlocContext.create(),
                    output = output,
                    viewModelFactory = {
                        MealPlanViewModel(
                            mainContext = context.mainContext,
                            repository = repository,
                            scopeRepository = scopeRepository,
                            cookingSessionRepository = sessionRepo,
                            dateTimeUtil = dateTimeUtil,
                            coachMarkController = CoachMarkController(MapSettings()),
                            toastService = toastService,
                            settings = MapSettings(),
                        )
                    },
                )

            freshBloc.state.test {
                val state = awaitItem()
                state.cookingRecipeCount shouldBe 2
                cancelAndIgnoreRemainingEvents()
            }

            freshBloc.onContinueCookingClicked()

            output.lastValue shouldBe MealPlanBloc.Output.OpenCookMode(42L)
        }

    @Test
    fun WHEN_done_cooking_confirmed_THEN_session_cleared_and_dialog_dismissed() = runTest {
        bloc.onDoneCookingClicked()
        bloc.state.test { awaitItem().showDoneCookingDialog shouldBe true }

        bloc.onDoneCookingConfirmed()

        bloc.state.test { awaitItem().showDoneCookingDialog shouldBe false }
        verifySuspend { cookingSessionRepository.stopAll() }
    }

    @Test
    fun WHEN_done_cooking_dismissed_THEN_dialog_dismissed_without_clearing() = runTest {
        bloc.onDoneCookingClicked()
        bloc.onDoneCookingDismissed()

        bloc.state.test { awaitItem().showDoneCookingDialog shouldBe false }
    }
}

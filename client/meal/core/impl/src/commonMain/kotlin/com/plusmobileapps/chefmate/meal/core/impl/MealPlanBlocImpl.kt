package com.plusmobileapps.chefmate.meal.core.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.chefmate.meal.core.MealPlanBloc
import com.plusmobileapps.chefmate.meal.core.MealPlanBloc.Output
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.recipe.core.addmeal.MealPlannerRootBloc
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

@AssistedInject
@ContributesAssistedFactory(scope = AppScope::class, assistedFactory = MealPlanBloc.Factory::class)
class MealPlanBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    private val viewModelFactory: () -> MealPlanViewModel,
) : MealPlanBloc, BlocContext by context {

    private val viewModel: MealPlanViewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<MealPlanBloc.Model> =
        viewModel.state.mapState {
            MealPlanBloc.Model(
                isLoading = it.isLoading,
                isSyncing = it.isSyncing,
                viewMode = it.viewMode,
                dateLabel = FixedString(viewModel.getDateLabel()),
                dayMeals =
                    if (it.viewMode == MealPlanBloc.ViewMode.DAY) {
                        viewModel.buildDayMeals(it.meals)
                    } else {
                        null
                    },
                weekMeals =
                    if (it.viewMode == MealPlanBloc.ViewMode.WEEK) {
                        viewModel.buildWeekMeals(it.meals).toImmutableList()
                    } else {
                        null
                    },
                monthModel =
                    if (it.viewMode == MealPlanBloc.ViewMode.MONTH) {
                        viewModel.buildMonthModel(it.meals, it.selectedMonthDay)
                    } else {
                        null
                    },
                mealToDelete = it.mealToDelete,
                cookingRecipeCount = it.cookingRecipeIds.size,
                showDoneCookingDialog = it.showDoneCookingDialog,
                pendingReplaceCookMode = it.pendingReplaceCookMode?.toImmutableList(),
                activeCoachMark = it.activeCoachMark,
            )
        }

    override fun onViewModeSelected(mode: MealPlanBloc.ViewMode) {
        viewModel.dismissCoachMark(CoachMarkId.MEAL_PLAN_VIEW_MODE)
        viewModel.selectViewMode(mode)
    }

    override fun onMonthDaySelected(date: LocalDate) {
        viewModel.onMonthDaySelected(date)
    }

    override fun onPreviousClicked() {
        viewModel.goPrevious()
    }

    override fun onNextClicked() {
        viewModel.goNext()
    }

    override fun onMealClicked(item: MealPlanItem) {
        output.onNext(Output.OpenRecipe(item.recipeId))
    }

    override fun onDeleteMealClicked(item: MealPlanItem) {
        viewModel.requestRemoveMeal(item)
    }

    override fun onDeleteMealConfirmed() {
        viewModel.confirmRemoveMeal()
    }

    override fun onDeleteMealDismissed() {
        viewModel.dismissRemoveMeal()
    }

    override fun onAddMealClicked() {
        viewModel.dismissCoachMark(CoachMarkId.MEAL_PLAN_ADD_MEAL)
        output.onNext(Output.OpenMealPlanner(MealPlannerRootBloc.Props.FromMealPlanner))
    }

    override fun onSyncClicked() {
        viewModel.onSyncClicked()
    }

    override fun onReplaceCookModeClicked(meals: List<MealPlanItem>) {
        viewModel.requestReplaceCookMode(meals)
    }

    override fun onReplaceCookModeConfirmed() {
        viewModel.confirmReplaceCookMode()
    }

    override fun onReplaceCookModeDismissed() {
        viewModel.dismissReplaceCookMode()
    }

    override fun onAddToCookModeClicked(meals: List<MealPlanItem>) {
        viewModel.addToCookMode(meals)
    }

    override fun onContinueCookingClicked() {
        viewModel.state.value.cookingRecipeIds.firstOrNull()?.let { recipeId ->
            output.onNext(Output.OpenCookMode(recipeId))
        }
    }

    override fun onDoneCookingClicked() {
        viewModel.showDoneCookingDialog()
    }

    override fun onDoneCookingConfirmed() {
        viewModel.confirmDoneCooking()
    }

    override fun onDoneCookingDismissed() {
        viewModel.dismissDoneCookingDialog()
    }

    override fun onCoachMarkDismissed(id: String) {
        viewModel.dismissCoachMark(id)
    }
}

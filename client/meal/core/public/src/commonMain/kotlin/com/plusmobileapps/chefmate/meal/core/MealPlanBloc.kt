package com.plusmobileapps.chefmate.meal.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.meal.data.FamilyPlan
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.recipe.core.addmeal.MealPlannerRootBloc
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface MealPlanBloc : ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        MealPlanScreen(bloc = this, modifier = modifier)
    }

    fun onViewModeSelected(mode: ViewMode)

    fun onPreviousClicked()

    fun onNextClicked()

    fun onMealClicked(item: MealPlanItem)

    fun onDeleteMealClicked(item: MealPlanItem)

    fun onDeleteMealConfirmed()

    fun onDeleteMealDismissed()

    fun onMonthDaySelected(date: LocalDate)

    fun onAddMealClicked()

    fun onSyncClicked()

    fun onReplaceCookModeClicked(meals: List<MealPlanItem>)

    fun onReplaceCookModeConfirmed()

    fun onReplaceCookModeDismissed()

    fun onAddToCookModeClicked(meals: List<MealPlanItem>)

    fun onContinueCookingClicked()

    fun onDoneCookingClicked()

    fun onDoneCookingConfirmed()

    fun onDoneCookingDismissed()

    /** Marks the first-run coach mark with [id] as seen so it never shows again. */
    fun onCoachMarkDismissed(id: String)

    fun onPlanSelectorClicked()

    fun onPlanPickerDismissed()

    /** Switches to the family plan with [familyId], or to the personal plan when null. */
    fun onPlanSelected(familyId: String?)

    data class Model(
        val isLoading: Boolean = true,
        val isSyncing: Boolean = false,
        val viewMode: ViewMode = ViewMode.DAY,
        val dateLabel: TextData = FixedString(""),
        val dayMeals: DayMeals? = null,
        val weekMeals: ImmutableList<DayGroup>? = null,
        val monthModel: MonthModel? = null,
        val mealToDelete: MealPlanItem? = null,
        val cookingRecipeCount: Int = 0,
        val showDoneCookingDialog: Boolean = false,
        val pendingReplaceCookMode: ImmutableList<MealPlanItem>? = null,
        /** Id of the first-run coach mark currently allowed to show, or null when none. */
        val activeCoachMark: String? = null,
        /** Personal/family plan switcher; null when the user has no family plans to switch to. */
        val planSelector: PlanSelector? = null,
    )

    data class PlanSelector(
        val families: ImmutableList<FamilyPlan>,
        /** The selected family plan's id, or null when the personal plan is selected. */
        val selectedFamilyId: String? = null,
        val isPickerOpen: Boolean = false,
    )

    data class DayMeals(
        val breakfast: ImmutableList<MealPlanItem> = persistentListOf(),
        val lunch: ImmutableList<MealPlanItem> = persistentListOf(),
        val dinner: ImmutableList<MealPlanItem> = persistentListOf(),
        val snacks: ImmutableList<MealPlanItem> = persistentListOf(),
    )

    data class DayGroup(val dateLabel: TextData, val meals: ImmutableList<MealPlanItem>)

    data class MonthModel(
        val firstDayOfMonth: LocalDate,
        val selectedDay: LocalDate,
        val daysWithMeals: Set<String>,
        val selectedDayMeals: DayMeals,
    )

    enum class ViewMode {
        DAY,
        WEEK,
        MONTH,
    }

    sealed class Output {
        data class OpenRecipe(val recipeId: Long) : Output()

        data class OpenMealPlanner(val props: MealPlannerRootBloc.Props) : Output()

        data class OpenCookMode(val recipeId: Long) : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): MealPlanBloc
    }
}

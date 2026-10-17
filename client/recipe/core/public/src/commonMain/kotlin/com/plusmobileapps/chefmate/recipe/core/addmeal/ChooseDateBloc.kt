package com.plusmobileapps.chefmate.recipe.core.addmeal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.meal.data.FamilyPlan
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

interface ChooseDateBloc : BackClickBloc, ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        ChooseDateScreen(bloc = this, modifier = modifier)
    }

    fun onDaySelected(date: LocalDate)

    fun onNextClicked()

    fun onPreviousMonth()

    fun onNextMonth()

    fun onTodayClicked()

    /**
     * Picks the meal type from the inline pane shown on wide layouts, where date and meal type are
     * chosen together on this screen instead of on a separate [ChooseMealTypeBloc] step.
     */
    fun onMealTypeSelected(mealType: MealType)

    /** Saves the meal straight from the wide combined layout, emitting [Output.Finished]. */
    fun onSaveClicked()

    fun onPlanSelectorClicked()

    fun onPlanPickerDismissed()

    /** Adds the meal to the family plan with [familyId], or to the personal plan when null. */
    fun onPlanSelected(familyId: String?)

    data class Model(
        val firstDayOfMonth: LocalDate = LocalDate(2000, 1, 1),
        val daysWithMeals: Set<String> = emptySet(),
        val monthLabel: String = "",
        val selectedDate: LocalDate? = null,
        val formattedSelectedDate: String = "",
        val selectedDayMeals: ImmutableList<MealPlanItem> = persistentListOf(),
        val selectedMealType: MealType = MealType.DINNER,
        val isSaving: Boolean = false,
        val recipeTitle: String = "",
        /** Family plans the meal can go to instead; the plan selector is hidden when empty. */
        val familyPlans: ImmutableList<FamilyPlan> = persistentListOf(),
        /** The family plan the meal will be added to, or null for the personal plan. */
        val selectedFamilyId: String? = null,
        val isPlanPickerOpen: Boolean = false,
    )

    sealed class Output {
        data class DateSelected(val date: LocalDate) : Output()

        /** The meal was saved from the wide combined layout; the add-meal flow is done. */
        data object Finished : Output()

        data object Back : Output()
    }

    fun interface Factory {
        fun create(
            context: BlocContext,
            recipeId: Long,
            output: Consumer<Output>,
        ): ChooseDateBloc
    }
}

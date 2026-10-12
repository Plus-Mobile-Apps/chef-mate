package com.plusmobileapps.chefmate.meal.core.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plusmobileapps.chefmate.meal.core.MealPlanBloc
import com.plusmobileapps.chefmate.meal.core.MealPlanScreen
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

private val sampleDayMeals =
    MealPlanBloc.DayMeals(
        breakfast =
            persistentListOf(
                MealPlanItem(
                    id = 1L,
                    recipeId = 10L,
                    recipeTitle = "Sourdough Pancakes",
                    recipeImageUrl = null,
                    date = "2026-04-17",
                    mealType = MealType.BREAKFAST,
                )
            ),
        lunch =
            persistentListOf(
                MealPlanItem(
                    id = 2L,
                    recipeId = 20L,
                    recipeTitle = "Caesar Salad",
                    recipeImageUrl = null,
                    date = "2026-04-17",
                    mealType = MealType.LUNCH,
                )
            ),
        dinner =
            persistentListOf(
                MealPlanItem(
                    id = 3L,
                    recipeId = 30L,
                    recipeTitle = "Pasta Carbonara",
                    recipeImageUrl = null,
                    date = "2026-04-17",
                    mealType = MealType.DINNER,
                )
            ),
    )

private fun mealPlanBloc(model: MealPlanBloc.Model): MealPlanBloc =
    object : MealPlanBloc {
        override val state = MutableStateFlow(model)

        override fun onViewModeSelected(mode: MealPlanBloc.ViewMode) = Unit

        override fun onCoachMarkDismissed(id: String) = Unit

        override fun onPreviousClicked() = Unit

        override fun onNextClicked() = Unit

        override fun onMealClicked(item: MealPlanItem) = Unit

        override fun onDeleteMealClicked(item: MealPlanItem) = Unit

        override fun onDeleteMealConfirmed() = Unit

        override fun onDeleteMealDismissed() = Unit

        override fun onMonthDaySelected(date: LocalDate) = Unit

        override fun onAddMealClicked() = Unit

        override fun onSyncClicked() = Unit

        override fun onReplaceCookModeClicked(meals: List<MealPlanItem>) = Unit

        override fun onReplaceCookModeConfirmed() = Unit

        override fun onReplaceCookModeDismissed() = Unit

        override fun onAddToCookModeClicked(meals: List<MealPlanItem>) = Unit

        override fun onContinueCookingClicked() = Unit

        override fun onDoneCookingClicked() = Unit

        override fun onDoneCookingConfirmed() = Unit

        override fun onDoneCookingDismissed() = Unit
    }

val previewMealPlanBloc: MealPlanBloc =
    mealPlanBloc(
        MealPlanBloc.Model(
            isLoading = false,
            viewMode = MealPlanBloc.ViewMode.DAY,
            dateLabel = FixedString("Apr 17, 2026"),
            dayMeals = sampleDayMeals,
        )
    )

/**
 * Day view with an active cooking session — exercises the Continue/Done Cooking FAB stack alongside
 * the existing Add Meal FAB.
 */
val previewMealPlanBlocCooking: MealPlanBloc =
    mealPlanBloc(
        MealPlanBloc.Model(
            isLoading = false,
            viewMode = MealPlanBloc.ViewMode.DAY,
            dateLabel = FixedString("Apr 17, 2026"),
            dayMeals = sampleDayMeals,
            cookingRecipeCount = 2,
        )
    )

val previewMealPlanBlocEmpty: MealPlanBloc =
    mealPlanBloc(
        MealPlanBloc.Model(
            isLoading = false,
            viewMode = MealPlanBloc.ViewMode.DAY,
            dateLabel = FixedString("Apr 17, 2026"),
            dayMeals = MealPlanBloc.DayMeals(),
        )
    )

/** Day view mid-sync — the header sync action shows a spinner and is disabled. */
val previewMealPlanBlocSyncing: MealPlanBloc =
    mealPlanBloc(
        MealPlanBloc.Model(
            isLoading = false,
            viewMode = MealPlanBloc.ViewMode.DAY,
            dateLabel = FixedString("Apr 17, 2026"),
            dayMeals = sampleDayMeals,
            isSyncing = true,
        )
    )

/**
 * Week view — section headers should NOT show the cook-mode buttons, since a week-day group mixes
 * meal types and isn't a meaningful "queue this for cooking" unit.
 */
val previewMealPlanBlocWeek: MealPlanBloc =
    mealPlanBloc(
        MealPlanBloc.Model(
            isLoading = false,
            viewMode = MealPlanBloc.ViewMode.WEEK,
            dateLabel = FixedString("Apr 12 - Apr 18, 2026"),
            weekMeals =
                persistentListOf(
                    MealPlanBloc.DayGroup(
                        dateLabel = FixedString("Fri, Apr 17"),
                        meals = (sampleDayMeals.breakfast + sampleDayMeals.lunch).toImmutableList(),
                    )
                ),
        )
    )

/** Month view — the calendar grid stays capped and centered on wide windows. */
val previewMealPlanBlocMonth: MealPlanBloc =
    mealPlanBloc(
        MealPlanBloc.Model(
            isLoading = false,
            viewMode = MealPlanBloc.ViewMode.MONTH,
            dateLabel = FixedString("April 2026"),
            monthModel =
                MealPlanBloc.MonthModel(
                    firstDayOfMonth = LocalDate(2026, 4, 1),
                    selectedDay = LocalDate(2026, 4, 17),
                    daysWithMeals = setOf("2026-04-08", "2026-04-17", "2026-04-23"),
                    selectedDayMeals = sampleDayMeals,
                ),
        )
    )

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun MealPlanPreview() {
    ChefMateTheme { MealPlanScreen(bloc = previewMealPlanBloc) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun MealPlanCookingPreview() {
    ChefMateTheme { MealPlanScreen(bloc = previewMealPlanBlocCooking) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun MealPlanEmptyPreview() {
    ChefMateTheme { MealPlanScreen(bloc = previewMealPlanBlocEmpty) }
}

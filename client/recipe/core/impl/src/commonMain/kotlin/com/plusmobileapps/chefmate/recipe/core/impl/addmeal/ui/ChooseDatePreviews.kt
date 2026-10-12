package com.plusmobileapps.chefmate.recipe.core.impl.addmeal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseDateBloc
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

private fun chooseDateBloc(model: ChooseDateBloc.Model): ChooseDateBloc =
    object : ChooseDateBloc {
        override val state = MutableStateFlow(model)

        override fun onDaySelected(date: LocalDate) = Unit

        override fun onNextClicked() = Unit

        override fun onPreviousMonth() = Unit

        override fun onNextMonth() = Unit

        override fun onTodayClicked() = Unit

        override fun onMealTypeSelected(mealType: MealType) = Unit

        override fun onSaveClicked() = Unit

        override fun onBackClicked() = Unit
    }

/** April 2026 with the 17th selected — it already has a breakfast planned. */
val previewChooseDateBloc: ChooseDateBloc =
    chooseDateBloc(
        ChooseDateBloc.Model(
            firstDayOfMonth = LocalDate(2026, 4, 1),
            daysWithMeals = setOf("2026-04-08", "2026-04-17", "2026-04-23"),
            monthLabel = "April 2026",
            selectedDate = LocalDate(2026, 4, 17),
            formattedSelectedDate = "Apr 17, 2026",
            selectedDayMeals =
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
            selectedMealType = MealType.DINNER,
            recipeTitle = "Pasta Carbonara",
        )
    )

@Preview
@Composable
internal fun ChooseDateScreenPreview() {
    ChefMateTheme { previewChooseDateBloc.Content(Modifier) }
}

@Preview(widthDp = 1280, heightDp = 800)
@Composable
internal fun ChooseDateScreenExpandedPreview() {
    ChefMateTheme { previewChooseDateBloc.Content(Modifier) }
}

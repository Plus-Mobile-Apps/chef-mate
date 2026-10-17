package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.meal.core.MealPlanBloc
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBloc
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBlocCooking
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBlocEmpty
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBlocFamilyPlan
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBlocMonth
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBlocPersonalPlanWithFamilies
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBlocSyncing
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBlocWeek
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanPlanSelector
import com.plusmobileapps.chefmate.recipe.core.addmeal.MealPlanPickerContent
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// MealPlanScreen uses PlusNavContainer which paints no top-level Surface — in production its
// background comes from the app shell. Wrap each test in one so dark snapshots actually render
// dark instead of inheriting the default white screenshot canvas.
@Composable
private fun MealPlanScreenshot(bloc: MealPlanBloc, darkTheme: Boolean = false) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            bloc.Content()
        }
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MealPlanLightScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBloc)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MealPlanDarkScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBloc, darkTheme = true)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MealPlanCookingSessionScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBlocCooking)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MealPlanEmptyScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBlocEmpty)
}

// Syncing — header sync action renders a spinner in place of the icon.
@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MealPlanSyncingScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBlocSyncing)
}

// Week view — locks in that section headers do NOT carry the replace/add cook-mode buttons.
@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MealPlanWeekViewScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBlocWeek)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MealPlanMonthViewScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBlocMonth)
}

// Expanded width — the month calendar stays capped and centered instead of stretching.
@PreviewTest
@Preview(showBackground = true, widthDp = 1280, heightDp = 900)
@Composable
fun MealPlanMonthViewExpandedScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBlocMonth)
}

// Family plan — the plan selector pill replaces the "Meals" title.
@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MealPlanFamilyPlanScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBlocFamilyPlan)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MealPlanFamilyPlanDarkScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBlocFamilyPlan, darkTheme = true)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1100)
@Composable
fun MealPlanPersonalPlanWithFamiliesScreenshot() {
    MealPlanScreenshot(bloc = previewMealPlanBlocPersonalPlanWithFamilies)
}

// Picker body, rendered directly: ModalBottomSheet doesn't snapshot under the screenshot plugin.
@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MealPlanPlanPickerScreenshot() {
    ChefMateTheme {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MealPlanPickerContent(
                families = previewMealPlanPlanSelector.families,
                selectedFamilyId = previewMealPlanPlanSelector.selectedFamilyId,
                onPlanSelected = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MealPlanPlanPickerDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            MealPlanPickerContent(
                families = previewMealPlanPlanSelector.families,
                selectedFamilyId = previewMealPlanPlanSelector.selectedFamilyId,
                onPlanSelected = {},
            )
        }
    }
}

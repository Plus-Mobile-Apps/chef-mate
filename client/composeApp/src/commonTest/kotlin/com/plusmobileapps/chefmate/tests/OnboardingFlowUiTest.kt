@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.plusmobileapps.chefmate.fixtures.TestRecipes
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.onboarding.robots.onboarding
import com.plusmobileapps.chefmate.recipe.list.robots.recipeList
import kotlin.test.Test

class OnboardingFlowUiTest {

    @Test
    fun walking_through_the_full_tour_loads_the_main_app() =
        runRootBlocTest(showOnboarding = true) {
            onboarding()
                .assertWelcomeDisplayed()
                .clickGetStarted()
                .assertShareRecipesDisplayed()
                .clickShareRecipesNext()
                .assertSaveRecipesDisplayed()
                .clickSaveRecipesNext()
                .assertAddToGroceryDisplayed()
                .clickAddToGroceryNext()
                .assertMealPlanningDisplayed()
                .clickMealPlanningNext()
                .assertCookModeDisplayed()
                .clickCookModeNext()
                .assertRecipeBooksDisplayed()
                .clickRecipeBooksNext()
                .assertGroceryListsDisplayed()
                .clickGroceryListsNext()
                .assertFamilySharingDisplayed()
                .clickFamilySharingNext()
                .assertStartCookingDisplayed()
                .clickStartCooking()

            // Finishing replaces the onboarding stack with the bottom nav (recipes tab).
            waitUntilExactlyOneExists(hasText(TestRecipes.fullyPopulated.title))
            recipeList().assertRecipeIsDisplayed(TestRecipes.fullyPopulated.title)
        }

    @Test
    fun skipping_from_the_welcome_screen_loads_the_main_app() =
        runRootBlocTest(showOnboarding = true) {
            onboarding().assertWelcomeDisplayed().clickSkip()

            waitUntilExactlyOneExists(hasText(TestRecipes.fullyPopulated.title))
            recipeList().assertRecipeIsDisplayed(TestRecipes.fullyPopulated.title)
        }
}

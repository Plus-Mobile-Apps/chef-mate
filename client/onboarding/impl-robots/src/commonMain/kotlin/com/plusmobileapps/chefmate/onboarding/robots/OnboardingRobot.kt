@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.onboarding.robots

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.plusmobileapps.chefmate.onboarding.OnboardingTestTags

/**
 * Robot for the onboarding flow. Each step is matched by its screen root test tag, and each action
 * clicks the step's button by tag, so the test reads as the user walking through the tour.
 */
class OnboardingRobot(private val test: ComposeUiTest) {

    fun assertWelcomeDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.WELCOME_SCREEN)

    fun clickGetStarted(): OnboardingRobot = click(OnboardingTestTags.WELCOME_GET_STARTED_BUTTON)

    fun clickSignIn(): OnboardingRobot = click(OnboardingTestTags.WELCOME_SIGN_IN_BUTTON)

    /** Skip lives in the shared top nav bar, so it's available from every step. */
    fun clickSkip(): OnboardingRobot = click(OnboardingTestTags.NAV_SKIP_BUTTON)

    /** Back lives in the shared top nav bar; hidden on the first (Welcome) step. */
    fun clickBack(): OnboardingRobot = click(OnboardingTestTags.NAV_BACK_BUTTON)

    fun assertShareRecipesDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.SHARE_RECIPES_SCREEN)

    fun clickShareRecipesNext(): OnboardingRobot =
        click(OnboardingTestTags.SHARE_RECIPES_NEXT_BUTTON)

    fun assertSaveRecipesDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.SAVE_RECIPES_SCREEN)

    fun clickSaveRecipesNext(): OnboardingRobot = click(OnboardingTestTags.SAVE_RECIPES_NEXT_BUTTON)

    fun assertAddToGroceryDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.ADD_TO_GROCERY_SCREEN)

    fun clickAddToGroceryNext(): OnboardingRobot =
        click(OnboardingTestTags.ADD_TO_GROCERY_NEXT_BUTTON)

    fun assertMealPlanningDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.MEAL_PLANNING_SCREEN)

    fun clickMealPlanningNext(): OnboardingRobot =
        click(OnboardingTestTags.MEAL_PLANNING_NEXT_BUTTON)

    fun assertCookModeDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.COOK_MODE_SCREEN)

    fun clickCookModeNext(): OnboardingRobot = click(OnboardingTestTags.COOK_MODE_NEXT_BUTTON)

    fun assertRecipeBooksDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.RECIPE_BOOKS_SCREEN)

    fun clickRecipeBooksNext(): OnboardingRobot = click(OnboardingTestTags.RECIPE_BOOKS_NEXT_BUTTON)

    fun assertGroceryListsDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.GROCERY_LISTS_SCREEN)

    fun clickGroceryListsNext(): OnboardingRobot =
        click(OnboardingTestTags.GROCERY_LISTS_NEXT_BUTTON)

    fun assertFamilySharingDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.FAMILY_SHARING_SCREEN)

    fun clickFamilySharingNext(): OnboardingRobot =
        click(OnboardingTestTags.FAMILY_SHARING_NEXT_BUTTON)

    fun assertStartCookingDisplayed(): OnboardingRobot =
        assertDisplayed(OnboardingTestTags.START_COOKING_SCREEN)

    fun clickStartCooking(): OnboardingRobot = click(OnboardingTestTags.START_COOKING_BUTTON)

    fun clickStartCookingSignUp(): OnboardingRobot =
        click(OnboardingTestTags.START_COOKING_SIGN_UP_BUTTON)

    private fun assertDisplayed(tag: String): OnboardingRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(tag))
    }

    private fun click(tag: String): OnboardingRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(tag))
        test.onNodeWithTag(tag).performClick()
    }
}

fun ComposeUiTest.onboarding(): OnboardingRobot = OnboardingRobot(this)

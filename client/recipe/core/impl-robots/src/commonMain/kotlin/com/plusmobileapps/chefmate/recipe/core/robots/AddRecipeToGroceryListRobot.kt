package com.plusmobileapps.chefmate.recipe.core.robots

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListTestTags

/**
 * Robot for the sheet that picks which of a recipe's ingredients to add to a grocery list.
 *
 * Construct via [addRecipeToGroceryList] from inside a `runComposeUiTest { … }` block.
 */
@OptIn(ExperimentalTestApi::class)
class AddRecipeToGroceryListRobot(private val test: ComposeUiTest) {

    private val onScreen = hasTestTag(AddRecipeToGroceryListTestTags.SCREEN)

    fun awaitDisplayed(): AddRecipeToGroceryListRobot = apply {
        test.waitUntilExactlyOneExists(onScreen)
    }

    /** Asserts a row showing exactly [text] is on the sheet (an ingredient name or its amount). */
    fun assertIngredientDisplayed(text: String): AddRecipeToGroceryListRobot = apply {
        test.onNode(hasText(text) and hasAnyAncestor(onScreen)).assertIsDisplayed()
    }

    /** Opens the scale dropdown and picks the option with [label] (e.g. `2×`). */
    fun selectIngredientScale(label: String): AddRecipeToGroceryListRobot = apply {
        test
            .onNode(
                hasTestTag(AddRecipeToGroceryListTestTags.INGREDIENT_SCALE_BUTTON) and
                    hasAnyAncestor(onScreen)
            )
            .performClick()
        test.onNode(hasText(label)).performClick()
    }

    /** Asserts the scale button currently reads [label] (e.g. `2×`). */
    fun assertIngredientScale(label: String): AddRecipeToGroceryListRobot = apply {
        test
            .onNode(
                hasTestTag(AddRecipeToGroceryListTestTags.INGREDIENT_SCALE_BUTTON) and
                    hasAnyAncestor(onScreen)
            )
            .assert(hasText(label))
    }

    /** Taps the Add button, adding the selected ingredients to the chosen grocery list. */
    fun tapAdd(): AddRecipeToGroceryListRobot = apply {
        test.onNode(hasTestTag(AddRecipeToGroceryListTestTags.ADD_BUTTON)).performClick()
    }

    fun awaitDismissed(): AddRecipeToGroceryListRobot = apply {
        test.waitUntilDoesNotExist(onScreen)
    }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.addRecipeToGroceryList(): AddRecipeToGroceryListRobot =
    AddRecipeToGroceryListRobot(this)

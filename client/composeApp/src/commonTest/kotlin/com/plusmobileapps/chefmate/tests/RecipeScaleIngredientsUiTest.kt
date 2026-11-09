package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import com.plusmobileapps.chefmate.fixtures.TestRecipes
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.recipe.core.robots.addRecipeToGroceryList
import com.plusmobileapps.chefmate.recipe.core.robots.recipeDetail
import com.plusmobileapps.chefmate.recipe.list.robots.recipeList
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RecipeScaleIngredientsUiTest {

    @Test
    fun scaling_ingredients_doubles_the_displayed_amounts() = runRootBlocTest {
        // TestRecipes.fullyPopulated starts with "200g spaghetti" and "2 large eggs".
        recipeList().clickRecipe(TestRecipes.fullyPopulated.title)
        recipeDetail()
            .awaitDisplayed()
            .assertIngredientDisplayed("200g spaghetti")
            .selectIngredientScale("2×")
            .assertIngredientScale("2×")
            .assertIngredientDisplayed("400g spaghetti")
    }

    @Test
    fun scale_chosen_on_recipe_detail_carries_into_the_add_to_grocery_sheet() = runRootBlocTest {
        // The scale is a per-recipe preference, so the sheet opens on the factor already chosen
        // and offers the scaled amounts — no need to pick 2× a second time.
        recipeList().clickRecipe(TestRecipes.fullyPopulated.title)
        recipeDetail().awaitDisplayed().selectIngredientScale("2×").tapAddToGroceryList()
        addRecipeToGroceryList()
            .awaitDisplayed()
            .assertIngredientScale("2×")
            .assertIngredientDisplayed("400g")
    }

    @Test
    fun adding_ingredients_from_recipe_detail_closes_the_sheet() = runRootBlocTest {
        recipeList().clickRecipe(TestRecipes.fullyPopulated.title)
        recipeDetail().awaitDisplayed().tapAddToGroceryList()
        addRecipeToGroceryList().awaitDisplayed().tapAdd().awaitDismissed()
        recipeDetail().awaitDisplayed()
    }
}

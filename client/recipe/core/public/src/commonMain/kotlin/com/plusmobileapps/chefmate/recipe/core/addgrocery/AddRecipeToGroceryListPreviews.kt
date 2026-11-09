package com.plusmobileapps.chefmate.recipe.core.addgrocery

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.GroceryListItem
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.IngredientGroup
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.ListItem
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

private fun addToGroceryBloc(model: AddRecipeToGroceryListBloc.Model): AddRecipeToGroceryListBloc =
    object : AddRecipeToGroceryListBloc {
        override val state = MutableStateFlow(model)

        override fun onIngredientToggled(ingredient: Int) = Unit

        override fun onGroceryListSelected(listId: Long) = Unit

        override fun onScaleChanged(scale: Double) = Unit

        override fun onSaveClicked() = Unit

        override fun onBackClicked() = Unit
    }

private fun ingredientGroups(
    spaghetti: String,
    eggs: String,
    spaghettiQuantity: String,
    eggsQuantity: String,
) =
    persistentListOf(
        IngredientGroup(
            category = GroceryCategory.DAIRY,
            items =
                persistentListOf(
                    ListItem(
                        id = 0,
                        name = eggs,
                        displayName = "eggs",
                        quantity = eggsQuantity,
                        isSelected = true,
                    ),
                ),
        ),
        IngredientGroup(
            category = GroceryCategory.GRAINS,
            items =
                persistentListOf(
                    ListItem(
                        id = 1,
                        name = spaghetti,
                        displayName = "spaghetti",
                        quantity = spaghettiQuantity,
                        isSelected = true,
                    ),
                ),
        ),
    )

/** The sheet at the recipe author's own amounts (1×). */
val previewAddToGroceryBloc: AddRecipeToGroceryListBloc =
    addToGroceryBloc(
        AddRecipeToGroceryListBloc.Model(
            isLoading = false,
            isAdding = false,
            groupedIngredients =
                ingredientGroups(
                    spaghetti = "200g spaghetti",
                    eggs = "2 large eggs",
                    spaghettiQuantity = "200g",
                    eggsQuantity = "2 large",
                ),
            groceryLists = persistentListOf(GroceryListItem(id = 1L, name = "My Grocery List")),
            selectedGroceryList = GroceryListItem(id = 1L, name = "My Grocery List"),
        ),
    )

/** The sheet at a persisted 2× scale — the amounts and the control both reflect the factor. */
val previewAddToGroceryBlocScaled: AddRecipeToGroceryListBloc =
    addToGroceryBloc(
        AddRecipeToGroceryListBloc.Model(
            isLoading = false,
            isAdding = false,
            groupedIngredients =
                ingredientGroups(
                    spaghetti = "400g spaghetti",
                    eggs = "4 large eggs",
                    spaghettiQuantity = "400g",
                    eggsQuantity = "4 large",
                ),
            groceryLists = persistentListOf(GroceryListItem(id = 1L, name = "My Grocery List")),
            selectedGroceryList = GroceryListItem(id = 1L, name = "My Grocery List"),
            ingredientScale = 2.0,
        ),
    )

/** No ingredients to pick from — the empty message replaces the list. */
val previewAddToGroceryBlocEmpty: AddRecipeToGroceryListBloc =
    addToGroceryBloc(
        AddRecipeToGroceryListBloc.Model(
            isLoading = false,
            isAdding = false,
            groupedIngredients = persistentListOf(),
        ),
    )

/** Personal lists plus one family's lists — the picker groups them under headers. */
val previewGroceryListSections: List<AddRecipeToGroceryListBloc.GroceryListSection> =
    AddRecipeToGroceryListBloc.Model(
            isLoading = false,
            isAdding = false,
            groupedIngredients = persistentListOf(),
            groceryLists =
                persistentListOf(
                    GroceryListItem(id = 1L, name = "My Grocery List"),
                    GroceryListItem(id = 2L, name = "Costco"),
                    GroceryListItem(
                        id = 3L,
                        name = "Safeway",
                        familyId = "family-1",
                        familyName = "The Smiths",
                    ),
                    GroceryListItem(
                        id = 4L,
                        name = "PCC",
                        familyId = "family-1",
                        familyName = "The Smiths",
                    ),
                ),
        )
        .groceryListSections

@Preview
@Composable
internal fun AddRecipeToGroceryListSelectorMenuPreview() {
    ChefMateTheme {
        Surface {
            Column {
                GroceryListSelectorMenuContent(
                    sections = previewGroceryListSections,
                    onListSelected = {},
                )
            }
        }
    }
}

@Preview
@Composable
internal fun AddRecipeToGroceryListPreview() {
    ChefMateTheme { AddRecipeToGroceryListScreen(bloc = previewAddToGroceryBloc) }
}

@Preview
@Composable
internal fun AddRecipeToGroceryListScaledPreview() {
    ChefMateTheme { AddRecipeToGroceryListScreen(bloc = previewAddToGroceryBlocScaled) }
}

@Preview
@Composable
internal fun AddRecipeToGroceryListEmptyPreview() {
    ChefMateTheme { AddRecipeToGroceryListScreen(bloc = previewAddToGroceryBlocEmpty) }
}

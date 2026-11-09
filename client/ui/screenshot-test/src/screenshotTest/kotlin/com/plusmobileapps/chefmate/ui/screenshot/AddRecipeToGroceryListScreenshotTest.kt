package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListScreen
import com.plusmobileapps.chefmate.recipe.core.addgrocery.GroceryListSelectorMenuContent
import com.plusmobileapps.chefmate.recipe.core.addgrocery.previewAddToGroceryBloc
import com.plusmobileapps.chefmate.recipe.core.addgrocery.previewAddToGroceryBlocEmpty
import com.plusmobileapps.chefmate.recipe.core.addgrocery.previewAddToGroceryBlocScaled
import com.plusmobileapps.chefmate.recipe.core.addgrocery.previewGroceryListSections
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

@PreviewTest
@Preview(showBackground = true, heightDp = 700)
@Composable
fun AddRecipeToGroceryListScreenshot() {
    ChefMateTheme { AddRecipeToGroceryListScreen(bloc = previewAddToGroceryBloc) }
}

// A recipe the user has already scaled: the control reads 2× and the listed amounts are doubled.
@PreviewTest
@Preview(showBackground = true, heightDp = 700)
@Composable
fun AddRecipeToGroceryListScaledScreenshot() {
    ChefMateTheme { AddRecipeToGroceryListScreen(bloc = previewAddToGroceryBlocScaled) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 700, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRecipeToGroceryListScaledDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        AddRecipeToGroceryListScreen(bloc = previewAddToGroceryBlocScaled)
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 700)
@Composable
fun AddRecipeToGroceryListEmptyScreenshot() {
    ChefMateTheme { AddRecipeToGroceryListScreen(bloc = previewAddToGroceryBlocEmpty) }
}

// The list picker's rows, grouped under Personal and family headers. Rendered directly because the
// dropdown's popup doesn't snapshot under the plugin.
@PreviewTest
@Preview(showBackground = true)
@Composable
fun AddRecipeToGroceryListSelectorMenuScreenshot() {
    ChefMateTheme { SelectorMenu() }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddRecipeToGroceryListSelectorMenuDarkScreenshot() {
    ChefMateTheme(darkTheme = true) { SelectorMenu() }
}

@Composable
private fun SelectorMenu() {
    Surface {
        Column {
            GroceryListSelectorMenuContent(
                sections = previewGroceryListSections,
                onListSelected = {},
            )
        }
    }
}

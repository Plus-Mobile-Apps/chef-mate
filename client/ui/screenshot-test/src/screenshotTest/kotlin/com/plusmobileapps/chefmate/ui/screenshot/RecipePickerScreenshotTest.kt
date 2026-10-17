package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.recipe.core.addmeal.RecipePickerBloc
import com.plusmobileapps.chefmate.recipe.core.impl.addmeal.ui.previewRecipePickerBloc
import com.plusmobileapps.chefmate.recipe.core.impl.addmeal.ui.previewRecipePickerBlocLoading
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// RecipePickerScreen uses PlusHeaderContainer, which paints no top-level Surface — wrap each test
// in
// one so dark snapshots render dark instead of inheriting the white screenshot canvas.
@Composable
private fun RecipePickerScreenshot(bloc: RecipePickerBloc, darkTheme: Boolean = false) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            bloc.Content()
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun RecipePickerLightScreenshot() {
    RecipePickerScreenshot(bloc = previewRecipePickerBloc)
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RecipePickerDarkScreenshot() {
    RecipePickerScreenshot(bloc = previewRecipePickerBloc, darkTheme = true)
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun RecipePickerLoadingScreenshot() {
    RecipePickerScreenshot(bloc = previewRecipePickerBlocLoading)
}

// Expanded width (unfolded foldable / tablet / desktop) — the list stays capped and centered.
@PreviewTest
@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun RecipePickerExpandedScreenshot() {
    RecipePickerScreenshot(bloc = previewRecipePickerBloc)
}

package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesScreen
import com.plusmobileapps.chefmate.grocery.categoryrules.previewGroceryCategoryRulesBloc
import com.plusmobileapps.chefmate.grocery.categoryrules.previewGroceryCategoryRulesBlocCreating
import com.plusmobileapps.chefmate.grocery.categoryrules.previewGroceryCategoryRulesBlocEmpty
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun GroceryCategoryRulesScreenshot() {
    ChefMateTheme { GroceryCategoryRulesScreen(bloc = previewGroceryCategoryRulesBloc) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun GroceryCategoryRulesDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        GroceryCategoryRulesScreen(bloc = previewGroceryCategoryRulesBloc)
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun GroceryCategoryRulesEmptyScreenshot() {
    ChefMateTheme { GroceryCategoryRulesScreen(bloc = previewGroceryCategoryRulesBlocEmpty) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun GroceryCategoryRulesCreatingScreenshot() {
    ChefMateTheme { GroceryCategoryRulesScreen(bloc = previewGroceryCategoryRulesBlocCreating) }
}

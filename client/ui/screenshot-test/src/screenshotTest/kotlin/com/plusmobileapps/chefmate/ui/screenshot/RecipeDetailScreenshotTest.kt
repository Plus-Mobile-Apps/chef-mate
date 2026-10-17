package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailScreen
import com.plusmobileapps.chefmate.recipe.core.detail.previewRecipeDetailBloc
import com.plusmobileapps.chefmate.recipe.core.detail.previewRecipeDetailBlocWithAiChat
import com.plusmobileapps.chefmate.recipe.core.detail.previewRecipeDetailBlocWithLockedAiChat
import com.plusmobileapps.chefmate.toast.LocalToastService
import com.plusmobileapps.chefmate.toast.testing.FakeToastService
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// RecipeDetailScreen reads LocalToastService.current directly, so it must be provided for the
// screen to compose at all.
@Composable
private fun RecipeDetailPreview(
    darkTheme: Boolean = false,
    bloc: com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailBloc = previewRecipeDetailBloc,
) {
    CompositionLocalProvider(LocalToastService provides FakeToastService()) {
        ChefMateTheme(darkTheme = darkTheme) { RecipeDetailScreen(bloc = bloc) }
    }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1000)
@Composable
fun RecipeDetailAiChatButtonScreenshot() {
    RecipeDetailPreview(bloc = previewRecipeDetailBlocWithAiChat)
}

// Non-subscriber: the AI Chat button is still present but dimmed, since tapping it opens the
// ChefMate Premium upsell rather than the chat.
@PreviewTest
@Preview(showBackground = true, heightDp = 1000)
@Composable
fun RecipeDetailAiChatButtonLockedScreenshot() {
    RecipeDetailPreview(bloc = previewRecipeDetailBlocWithLockedAiChat)
}

@PreviewTest
@Preview(showBackground = true, heightDp = 1000)
@Composable
fun RecipeDetailCompactScreenshot() {
    RecipeDetailPreview()
}

// Wide window: the top-bar actions fan out into dedicated Edit / Share / Delete buttons instead of
// the compact three-dot overflow.
@PreviewTest
@Preview(showBackground = true, widthDp = 900, heightDp = 800)
@Composable
fun RecipeDetailTabletScreenshot() {
    RecipeDetailPreview()
}

@PreviewTest
@Preview(
    showBackground = true,
    widthDp = 900,
    heightDp = 800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
fun RecipeDetailTabletDarkScreenshot() {
    RecipeDetailPreview(darkTheme = true)
}

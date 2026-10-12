package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseDateBloc
import com.plusmobileapps.chefmate.recipe.core.impl.addmeal.ui.previewChooseDateBloc
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

@Composable
private fun ChooseDateScreenshot(bloc: ChooseDateBloc, darkTheme: Boolean = false) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            bloc.Content()
        }
    }
}

// Compact — date only; Next continues to the separate meal-type step.
@PreviewTest
@Preview(showBackground = true)
@Composable
fun ChooseDateLightScreenshot() {
    ChooseDateScreenshot(bloc = previewChooseDateBloc)
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ChooseDateDarkScreenshot() {
    ChooseDateScreenshot(bloc = previewChooseDateBloc, darkTheme = true)
}

// Medium width (unfolded foldable) — meal type folds into a side pane and Save replaces Next.
@PreviewTest
@Preview(showBackground = true, widthDp = 800, heightDp = 840)
@Composable
fun ChooseDateMediumScreenshot() {
    ChooseDateScreenshot(bloc = previewChooseDateBloc)
}

// Expanded width — the combined panes stay capped and centered.
@PreviewTest
@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
fun ChooseDateExpandedScreenshot() {
    ChooseDateScreenshot(bloc = previewChooseDateBloc)
}

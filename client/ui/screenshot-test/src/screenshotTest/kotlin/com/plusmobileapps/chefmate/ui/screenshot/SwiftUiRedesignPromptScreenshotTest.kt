package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.recipe.bottomnav.SwiftUiRedesignPromptContent
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// The sheet body only — ModalBottomSheet itself doesn't snapshot under the screenshot plugin.

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SwiftUiRedesignPromptLightScreenshot() {
    ChefMateTheme { Surface { SwiftUiRedesignPromptContent(onAccept = {}, onDismiss = {}) } }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SwiftUiRedesignPromptDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface { SwiftUiRedesignPromptContent(onAccept = {}, onDismiss = {}) }
    }
}

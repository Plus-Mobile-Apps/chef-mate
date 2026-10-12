package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.ui.components.PlusDropdownSelector
import com.plusmobileapps.chefmate.ui.components.WindowSizeClass
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Covers the collapsed pill only: the open picker is a DropdownMenu or ModalBottomSheet, neither of
// which renders under the screenshot test plugin. Each caller snapshots its picker content
// directly.

@Composable
private fun SelectorPreview(
    text: String,
    supportingText: String? = null,
    darkTheme: Boolean = false,
) {
    ChefMateTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            PlusDropdownSelector(
                text = text,
                supportingText = supportingText,
                expanded = false,
                windowSizeClass = WindowSizeClass.COMPACT,
                onClick = {},
                onDismiss = {},
                modifier = Modifier.padding(16.dp),
            ) {}
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PlusDropdownSelectorLightScreenshot() {
    SelectorPreview(text = "Weekly groceries")
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PlusDropdownSelectorDarkScreenshot() {
    SelectorPreview(text = "Weekly groceries", darkTheme = true)
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PlusDropdownSelectorSupportingTextScreenshot() {
    SelectorPreview(text = "A very long recipe book name that truncates", supportingText = "(12)")
}

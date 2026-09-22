package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.settings.ui.SettingsScreen
import com.plusmobileapps.chefmate.settings.ui.previewSettingsBlocAiChatLocked
import com.plusmobileapps.chefmate.settings.ui.previewSettingsBlocAiChatUnlocked
import com.plusmobileapps.chefmate.settings.ui.previewSettingsBlocManageFamily
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Covers the two states of the More tab's AI Chat row. The row is present either way — only the
// trailing "Premium" badge distinguishes a non-subscriber, whose tap opens the upsell.

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun SettingsAiChatRowUnlockedScreenshot() {
    ChefMateTheme { SettingsScreen(bloc = previewSettingsBlocAiChatUnlocked) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun SettingsAiChatRowLockedScreenshot() {
    ChefMateTheme { SettingsScreen(bloc = previewSettingsBlocAiChatLocked) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsAiChatRowLockedDarkScreenshot() {
    ChefMateTheme(darkTheme = true) { SettingsScreen(bloc = previewSettingsBlocAiChatLocked) }
}

// The flagged Manage Family row in the More tab. Off by default, so this is the only reference
// that shows it.

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun SettingsManageFamilyRowScreenshot() {
    ChefMateTheme { SettingsScreen(bloc = previewSettingsBlocManageFamily) }
}

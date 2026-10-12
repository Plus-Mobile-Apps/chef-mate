package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.settings.ui.SettingsProfileHeader
import com.plusmobileapps.chefmate.settings.ui.SettingsScreen
import com.plusmobileapps.chefmate.settings.ui.previewSettingsBlocGuest
import com.plusmobileapps.chefmate.settings.ui.previewSettingsBlocSignedOut
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

// Covers the More tab's signed-out and guest states — side-by-side Sign In / Sign Up buttons above
// the icon rows. The signed-in state (with the destructive Sign Out button) is covered by
// SettingsAiChatRowScreenshotTest.

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun SettingsSignedOutScreenshot() {
    ChefMateTheme { SettingsScreen(bloc = previewSettingsBlocSignedOut) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsSignedOutDarkScreenshot() {
    ChefMateTheme(darkTheme = true) { SettingsScreen(bloc = previewSettingsBlocSignedOut) }
}

@PreviewTest
@Preview(showBackground = true, heightDp = 900)
@Composable
fun SettingsGuestScreenshot() {
    ChefMateTheme { SettingsScreen(bloc = previewSettingsBlocGuest) }
}

// The signed-in profile header mid-collapse and fully collapsed (the expanded state is the top of
// SettingsAiChatRowScreenshotTest).

@Composable
private fun ProfileHeader(collapsedFraction: Float) {
    SettingsProfileHeader(
        imageUrl = null,
        name = "John Doe",
        greeting = FixedString("Hello John Doe!"),
        collapsedFraction = { collapsedFraction },
        onAvatarClick = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsProfileHeaderHalfCollapsedScreenshot() {
    ChefMateTheme { ProfileHeader(collapsedFraction = 0.5f) }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun SettingsProfileHeaderCollapsedScreenshot() {
    ChefMateTheme { ProfileHeader(collapsedFraction = 1f) }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SettingsProfileHeaderCollapsedDarkScreenshot() {
    ChefMateTheme(darkTheme = true) { ProfileHeader(collapsedFraction = 1f) }
}

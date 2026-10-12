package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavBarPreview
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavExpandedRailPreview
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavRailPreview
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BottomNavBarNoBadgeScreenshot() {
    ChefMateTheme { Surface { BottomNavBarPreview(notificationCount = 0) } }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BottomNavBarWithBadgeScreenshot() {
    ChefMateTheme { Surface { BottomNavBarPreview(notificationCount = 3) } }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BottomNavBarWithBadgeDarkScreenshot() {
    ChefMateTheme(darkTheme = true) { Surface { BottomNavBarPreview(notificationCount = 3) } }
}

// The compact side rail a phone shows in landscape: the same lens, sliding vertically.
@PreviewTest
@Preview(showBackground = true)
@Composable
fun BottomNavRailWithBadgeScreenshot() {
    ChefMateTheme { Surface { BottomNavRailPreview(notificationCount = 3) } }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BottomNavRailWithBadgeDarkScreenshot() {
    ChefMateTheme(darkTheme = true) { Surface { BottomNavRailPreview(notificationCount = 3) } }
}

// The expanded rail a desktop or tablet window shows: the same lens behind wide label rows.
@PreviewTest
@Preview(showBackground = true)
@Composable
fun BottomNavExpandedRailWithBadgeScreenshot() {
    ChefMateTheme { Surface { BottomNavExpandedRailPreview(notificationCount = 3) } }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BottomNavExpandedRailWithBadgeDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface { BottomNavExpandedRailPreview(notificationCount = 3) }
    }
}

// The expanded rail folded down by its toggle: the compact column, with the toggle to reopen it.
@PreviewTest
@Preview(showBackground = true)
@Composable
fun BottomNavExpandedRailCollapsedScreenshot() {
    ChefMateTheme {
        Surface { BottomNavExpandedRailPreview(notificationCount = 3, collapsed = true) }
    }
}

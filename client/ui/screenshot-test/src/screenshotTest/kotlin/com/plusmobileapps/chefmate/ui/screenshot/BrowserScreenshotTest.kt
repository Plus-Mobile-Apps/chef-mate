package com.plusmobileapps.chefmate.ui.screenshot

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.plusmobileapps.chefmate.browser.PreviewBrowserAddressBar
import com.plusmobileapps.chefmate.browser.previewBrowserBookmarksBloc
import com.plusmobileapps.chefmate.browser.previewBrowserBookmarksEmptyBloc
import com.plusmobileapps.chefmate.browser.previewBrowserBookmarksSelectingBloc
import com.plusmobileapps.chefmate.browser.previewBrowserLandingBloc
import com.plusmobileapps.chefmate.browser.previewBrowserLandingWithBookmarksBloc
import com.plusmobileapps.chefmate.browser.previewBrowserSelectEngineBloc
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BrowserSelectEngineLightScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) { previewBrowserSelectEngineBloc.Content() }
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BrowserSelectEngineDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) { previewBrowserSelectEngineBloc.Content() }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BrowserLandingLightScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) { previewBrowserLandingBloc.Content() }
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BrowserLandingDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) { previewBrowserLandingBloc.Content() }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BrowserLandingBookmarksLightScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            previewBrowserLandingWithBookmarksBloc.Content()
        }
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BrowserLandingBookmarksDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            previewBrowserLandingWithBookmarksBloc.Content()
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 840, heightDp = 900)
@Composable
fun BrowserLandingBookmarksWideScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            previewBrowserLandingWithBookmarksBloc.Content()
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BrowserAddressBarLightScreenshot() {
    ChefMateTheme { Surface { PreviewBrowserAddressBar() } }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BrowserAddressBarDarkScreenshot() {
    ChefMateTheme(darkTheme = true) { Surface { PreviewBrowserAddressBar() } }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BrowserBookmarksLightScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) { previewBrowserBookmarksBloc.Content() }
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BrowserBookmarksDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) { previewBrowserBookmarksBloc.Content() }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BrowserBookmarksSelectingLightScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            previewBrowserBookmarksSelectingBloc.Content()
        }
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BrowserBookmarksSelectingDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) {
            previewBrowserBookmarksSelectingBloc.Content()
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BrowserBookmarksEmptyLightScreenshot() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) { previewBrowserBookmarksEmptyBloc.Content() }
    }
}

@PreviewTest
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BrowserBookmarksEmptyDarkScreenshot() {
    ChefMateTheme(darkTheme = true) {
        Surface(modifier = Modifier.fillMaxSize()) { previewBrowserBookmarksEmptyBloc.Content() }
    }
}

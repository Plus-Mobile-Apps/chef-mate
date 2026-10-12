package com.plusmobileapps.chefmate.browser

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.coroutines.flow.MutableStateFlow

private fun landingBloc(model: BrowserLandingBloc.Model): BrowserLandingBloc =
    object : BrowserLandingBloc {
        override val state = MutableStateFlow(model)

        override fun onSearchFieldFocused() = Unit

        override fun onOpenEngineHomeClicked() = Unit

        override fun onCoachMarkDismissed(id: String) = Unit

        override fun onSearchEngineSelected(engine: SearchEngine) = Unit

        override fun onBookmarkClicked(bookmark: BrowserBookmark) = Unit

        override fun onDeleteBookmarkClicked(bookmark: BrowserBookmark) = Unit
    }

val previewBrowserLandingBloc: BrowserLandingBloc =
    landingBloc(BrowserLandingBloc.Model(selectedEngine = SearchEngine.GOOGLE))

private val previewBookmarks =
    listOf(
        BrowserBookmark(
            id = 1,
            url = "https://www.seriouseats.com/classic-banana-bread-recipe",
            title = "Classic Banana Bread Recipe",
        ),
        BrowserBookmark(
            id = 2,
            url = "https://cooking.nytimes.com/",
            title = "NYT Cooking - Recipes and Cooking Guides",
        ),
        BrowserBookmark(id = 3, url = "https://www.budgetbytes.com/", title = "Budget Bytes"),
        BrowserBookmark(id = 4, url = "https://smittenkitchen.com/", title = "smitten kitchen"),
        BrowserBookmark(id = 5, url = "https://www.kingarthurbaking.com/", title = "King Arthur"),
    )

val previewBrowserLandingWithBookmarksBloc: BrowserLandingBloc =
    landingBloc(
        BrowserLandingBloc.Model(selectedEngine = SearchEngine.GOOGLE, bookmarks = previewBookmarks)
    )

val previewBrowserSelectEngineBloc: BrowserSelectEngineBloc =
    object : BrowserSelectEngineBloc {
        override fun onEngineSelected(engine: SearchEngine) = Unit
    }

/**
 * The browser's address bar in isolation. [BrowserScreen] itself can't be previewed because
 * [PlatformWebView] needs a live platform WebView, so the bar is snapshotted on its own.
 */
@Composable
fun PreviewBrowserAddressBar(modifier: Modifier = Modifier) {
    BrowserAddressBar(
        url = "https://www.seriouseats.com/classic-banana-bread-recipe",
        canGoBack = true,
        canGoForward = false,
        canOpenExternally = true,
        isBookmarked = true,
        onUrlChanged = {},
        onNavigate = {},
        onGoBack = {},
        onGoForward = {},
        onAddressBarFocused = {},
        onToggleBookmark = {},
        onHomeClick = {},
        onOpenInDefaultBrowser = {},
        modifier = modifier,
    )
}

@Preview
@Composable
internal fun BrowserAddressBarPreview() {
    ChefMateTheme { Surface { PreviewBrowserAddressBar() } }
}

@Preview
@Composable
internal fun BrowserLandingScreenPreview() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BrowserLandingScreen(bloc = previewBrowserLandingBloc)
        }
    }
}

@Preview
@Composable
internal fun BrowserLandingWithBookmarksScreenPreview() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BrowserLandingScreen(bloc = previewBrowserLandingWithBookmarksBloc)
        }
    }
}

@Preview
@Composable
internal fun BrowserSelectEngineScreenPreview() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BrowserSelectEngineScreen(bloc = previewBrowserSelectEngineBloc)
        }
    }
}

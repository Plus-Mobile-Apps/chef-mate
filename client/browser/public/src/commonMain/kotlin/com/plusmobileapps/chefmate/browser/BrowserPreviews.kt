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
    }

val previewBrowserLandingBloc: BrowserLandingBloc =
    landingBloc(BrowserLandingBloc.Model(selectedEngine = SearchEngine.GOOGLE))

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
        onUrlChanged = {},
        onNavigate = {},
        onGoBack = {},
        onGoForward = {},
        onAddressBarFocused = {},
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
internal fun BrowserSelectEngineScreenPreview() {
    ChefMateTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BrowserSelectEngineScreen(bloc = previewBrowserSelectEngineBloc)
        }
    }
}

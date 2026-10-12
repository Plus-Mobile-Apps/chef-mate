@file:Suppress("UNUSED_PARAMETER")

package com.plusmobileapps.chefmate.browser

import android.content.Context
import android.webkit.WebSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator

@Composable
actual fun PlatformWebView(
    url: String,
    onUrlLoaded: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onCanNavigateChanged: (canGoBack: Boolean, canGoForward: Boolean) -> Unit,
    goBackTrigger: Int,
    goForwardTrigger: Int,
    captureHtmlTrigger: Int,
    onHtmlCaptured: (String?) -> Unit,
    instanceKeeper: InstanceKeeper,
    modifier: Modifier,
) {
    val webViewState = rememberSaveableWebViewState(url = url)
    // The WebView library defaults its background to transparent, so a page that doesn't paint one
    // lets the app's surface show through — which reads as a black page in dark mode. Browsers
    // render on white; do the same so a site without dark styling stays legible. Sites that do
    // support dark mode paint over this. Must be set before the AndroidView factory runs.
    webViewState.webSettings.backgroundColor = Color.White
    val context = LocalContext.current
    webViewState.webSettings.customUserAgentString = remember { browserUserAgent(context) }
    val webViewNavigator = rememberWebViewNavigator()
    var lastCommandedUrl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(url) {
        if (url.isNotBlank() && url != lastCommandedUrl) {
            lastCommandedUrl = url
            webViewNavigator.loadUrl(url, BLANK_REQUESTED_WITH)
        }
    }

    LaunchedEffect(webViewState.lastLoadedUrl) {
        webViewState.lastLoadedUrl?.let {
            lastCommandedUrl = it
            onUrlLoaded(it)
        }
    }

    LaunchedEffect(webViewState.isLoading) { onLoadingChanged(webViewState.isLoading) }

    LaunchedEffect(webViewNavigator.canGoBack, webViewNavigator.canGoForward) {
        onCanNavigateChanged(webViewNavigator.canGoBack, webViewNavigator.canGoForward)
    }

    LaunchedEffect(goBackTrigger) { if (goBackTrigger > 0) webViewNavigator.navigateBack() }

    LaunchedEffect(goForwardTrigger) {
        if (goForwardTrigger > 0) webViewNavigator.navigateForward()
    }

    LaunchedEffect(captureHtmlTrigger) {
        if (captureHtmlTrigger > 0) {
            webViewNavigator.evaluateJavaScript(CAPTURE_HTML_SCRIPT) { onHtmlCaptured(it) }
        }
    }

    WebView(
        state = webViewState,
        modifier = modifier,
        navigator = webViewNavigator,
        onCreated = { nativeWebView: android.webkit.WebView ->
            nativeWebView.settings.presentAsChrome()
        },
    )
}

/**
 * The system WebView's own user agent with the two tokens that mark it as an embedded WebView
 * removed — `; wv` and the frozen `Version/4.0` — so it reads as the Chrome build it actually is.
 * Google in particular answers the WebView form with a stream of CAPTCHAs. Deriving it from the
 * device's default keeps the Android and Chrome versions honest instead of hard-coding ones that go
 * stale. `null` (the WebView default) if the WebView provider can't be queried.
 */
private fun browserUserAgent(context: Context): String? = runCatching {
    WebSettings.getDefaultUserAgent(context)
}
    .getOrNull()
    ?.replace("; wv)", ")")
    ?.replace(Regex("""Version/[\d.]+ """), "")

/**
 * The user agent string isn't the only thing that gives the WebView away. Chromium also sends
 * User-Agent Client Hints (`Sec-CH-UA`, readable as `navigator.userAgentData`) whose brand list
 * names "Android WebView", and an `X-Requested-With: <package>` header on every request. Google
 * reads these, so a clean UA string alone still lands on its CAPTCHA page. Rename the brand to
 * Chrome — keeping the WebView's own versions — and send the header nowhere. Each is skipped where
 * the WebView doesn't support it; current WebView builds dropped the header allow-list, which is
 * why [BLANK_REQUESTED_WITH] also exists.
 */
private fun WebSettings.presentAsChrome() {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) {
        val metadata = WebSettingsCompat.getUserAgentMetadata(this)
        val brands =
            metadata.brandVersionList.map { brand ->
                if (brand.brand == WEBVIEW_BRAND) {
                    UserAgentMetadata.BrandVersion.Builder(brand).setBrand(CHROME_BRAND).build()
                } else {
                    brand
                }
            }
        WebSettingsCompat.setUserAgentMetadata(
            this,
            UserAgentMetadata.Builder(metadata).setBrandVersionList(brands).build(),
        )
    }
    if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(this, emptySet())
    }
}

/**
 * Passed with each URL the app loads. Chromium only adds its own `X-Requested-With` when the
 * request doesn't already carry one, so this blanks the package name on the navigations we start —
 * notably the search-results page — on WebView builds where [WebSettingsCompat]'s allow-list is
 * gone. Requests the page itself makes still carry it; the WebView exposes no hook to strip those.
 */
private val BLANK_REQUESTED_WITH = mapOf("X-Requested-With" to "")

private const val WEBVIEW_BRAND = "Android WebView"
private const val CHROME_BRAND = "Google Chrome"

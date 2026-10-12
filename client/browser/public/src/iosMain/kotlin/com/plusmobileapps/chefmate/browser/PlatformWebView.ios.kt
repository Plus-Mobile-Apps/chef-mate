@file:Suppress("UNUSED_PARAMETER")

package com.plusmobileapps.chefmate.browser

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberSaveableWebViewState
import com.multiplatform.webview.web.rememberWebViewNavigator
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad

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
    onTitleChanged: (String?) -> Unit,
    instanceKeeper: InstanceKeeper,
    modifier: Modifier,
) {
    val webViewState = rememberSaveableWebViewState(url = url)
    // The WebView library defaults the WKWebView to non-opaque, so a page that doesn't paint its
    // own background lets the app's surface show through — which reads as a black page in dark
    // mode. An opaque WKWebView falls back to its own white canvas (and derives the overscroll
    // colour from the page), matching what a real browser shows. Sites that do support dark mode
    // paint over it. Must be set before the UIKitView factory runs.
    webViewState.webSettings.iOSWebSettings.opaque = true
    webViewState.webSettings.customUserAgentString = remember { browserUserAgent() }
    val webViewNavigator = rememberWebViewNavigator()
    var lastCommandedUrl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(url) {
        if (url.isNotBlank() && url != lastCommandedUrl) {
            lastCommandedUrl = url
            webViewNavigator.loadUrl(url)
        }
    }

    LaunchedEffect(webViewState.lastLoadedUrl) {
        webViewState.lastLoadedUrl?.let {
            lastCommandedUrl = it
            onUrlLoaded(it)
        }
    }

    LaunchedEffect(webViewState.pageTitle) { onTitleChanged(webViewState.pageTitle) }

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

    WebView(state = webViewState, modifier = modifier, navigator = webViewNavigator)
}

/**
 * Mobile Safari's user agent for this device. WKWebView's default omits Safari's `Version/…` and
 * `Safari/…` tokens, which is how sites tell an embedded web view from the browser — and Google
 * answers that with a stream of CAPTCHAs. iPads get the desktop-class form, as iPad Safari sends.
 */
private fun browserUserAgent(): String {
    val device = UIDevice.currentDevice
    val osVersion = device.systemVersion
    val safariVersion = osVersion.split('.').take(2).joinToString(".")
    return if (device.userInterfaceIdiom == UIUserInterfaceIdiomPad) {
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 " +
            "(KHTML, like Gecko) Version/$safariVersion Safari/605.1.15"
    } else {
        "Mozilla/5.0 (iPhone; CPU iPhone OS ${osVersion.replace('.', '_')} like Mac OS X) " +
            "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/$safariVersion " +
            "Mobile/15E148 Safari/604.1"
    }
}

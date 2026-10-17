package com.plusmobileapps.chefmate.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.instancekeeper.InstanceKeeper

/**
 * Platform-specific WebView composable. On mobile platforms this renders a native in-app browser.
 * On desktop it provides a fallback experience.
 *
 * @param url The URL to load in the browser.
 * @param onUrlLoaded Callback invoked when the WebView finishes navigating to a URL.
 * @param onLoadingChanged Callback invoked when the WebView loading state changes.
 * @param onCanNavigateChanged Callback invoked when back/forward availability changes.
 * @param goBackTrigger Incremented each time the user requests to navigate back.
 * @param goForwardTrigger Incremented each time the user requests to navigate forward.
 * @param captureHtmlTrigger Incremented each time the loaded page's markup is needed.
 * @param onHtmlCaptured Callback invoked with the result of a capture, in whatever form the
 *   platform's JavaScript bridge returns it. Always invoked once per [captureHtmlTrigger] — with a
 *   blank or `null` value when the page couldn't be read — so a caller can stop waiting.
 * @param onTitleChanged Callback invoked with the loaded page's `<title>` whenever it changes.
 * @param instanceKeeper Used on JVM to cache the WebView across recompositions so browser history
 *   survives tab switches.
 * @param modifier Modifier for layout.
 */
@Composable
expect fun PlatformWebView(
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
    modifier: Modifier = Modifier,
)

/**
 * Serialises the loaded page as the user is actually seeing it — after its scripts have run, and
 * after whatever bot check the site put in front of it — for the recipe parser to read.
 *
 * Works on a clone so the live page is left untouched, and strips the tags the parser never looks
 * at. That last part isn't just tidiness: most of a modern recipe page's weight is ad and analytics
 * script, and the result has to cross the platform's JavaScript bridge in one piece. Returns an
 * empty string rather than throwing, so the caller always gets an answer.
 */
internal const val CAPTURE_HTML_SCRIPT: String =
    """
    (function () {
      try {
        var doc = document.documentElement.cloneNode(true);
        var junk = doc.querySelectorAll(
          'script:not([type="application/ld+json"]),style,noscript,svg,iframe,template,link'
        );
        for (var i = 0; i < junk.length; i++) {
          if (junk[i].parentNode) junk[i].parentNode.removeChild(junk[i]);
        }
        return doc.outerHTML;
      } catch (e) {
        return '';
      }
    })()
    """

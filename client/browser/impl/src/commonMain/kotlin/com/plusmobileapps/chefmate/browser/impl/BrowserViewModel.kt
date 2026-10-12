package com.plusmobileapps.chefmate.browser.impl

import co.touchlab.kermit.Logger
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.browser.BrowserBloc
import com.plusmobileapps.chefmate.browser.BrowserBookmarkRepository
import com.plusmobileapps.chefmate.browser.BrowserHistoryRepository
import com.plusmobileapps.chefmate.di.Main
import dev.zacsweers.metro.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json

@Inject
class BrowserViewModel(
    @Main mainContext: CoroutineContext,
    private val recipeExtractorService: RecipeExtractorService,
    private val historyRepository: BrowserHistoryRepository,
    private val bookmarkRepository: BrowserBookmarkRepository,
) : ViewModel(mainContext) {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var output: Consumer<BrowserBloc.Output>? = null

    private var pendingHtmlCapture: CompletableDeferred<String?>? = null

    init {
        scope.launch {
            bookmarkRepository.observeBookmarks().collect { bookmarks ->
                _state.value =
                    _state.value.copy(bookmarkedUrls = bookmarks.mapTo(HashSet()) { it.url })
            }
        }
    }

    fun setOutput(output: Consumer<BrowserBloc.Output>) {
        this.output = output
    }

    fun onUrlChanged(url: String) {
        _state.value = _state.value.copy(addressBarText = url)
    }

    fun onNavigate() {
        val url = _state.value.addressBarText.ensureHttps()
        // Reset webViewReportedUrl so the BLoC's navigateUrl formula falls through to currentUrl,
        // ensuring a new navigation is always triggered even if the formula value didn't change.
        _state.value =
            _state.value.copy(
                currentUrl = url,
                addressBarText = url,
                webViewReportedUrl = "",
                pageTitle = null,
            )
    }

    fun onUrlLoadedInWebView(url: String) {
        _state.value = _state.value.copy(webViewReportedUrl = url, addressBarText = url)
        scope.launch { historyRepository.recordVisit(url) }
    }

    fun onWebViewLoadingChanged(isLoading: Boolean) {
        _state.value = _state.value.copy(isWebViewLoading = isLoading)
    }

    fun extractRecipe() {
        val url = _state.value.webViewReportedUrl.ifBlank { _state.value.currentUrl }
        if (url.isBlank() || _state.value.isExtracting) return

        val capture = CompletableDeferred<String?>()
        pendingHtmlCapture = capture
        _state.value =
            _state.value.copy(
                isExtracting = true,
                extractionFailed = false,
                captureHtmlTrigger = _state.value.captureHtmlTrigger + 1,
            )

        scope.launch {
            // The WebView holds the only copy of the page as the user actually sees it — past any
            // bot-check interstitial, and with client-rendered markup in place — so hand that to
            // the extractor. A WebView that never answers must not strand the user on a spinner,
            // so give up waiting and let the extractor fall back to fetching the URL itself.
            val renderedHtml = withTimeoutOrNull(HTML_CAPTURE_TIMEOUT_MS) { capture.await() }
            pendingHtmlCapture = null
            try {
                val extracted = recipeExtractorService.extractRecipe(url, renderedHtml)
                _state.value = _state.value.copy(isExtracting = false)
                output?.onNext(BrowserBloc.Output.RecipeExtracted(extracted))
            } catch (e: Exception) {
                Logger.d(e) { "Failed to extract recipe from $url" }
                _state.value = _state.value.copy(isExtracting = false, extractionFailed = true)
            }
        }
    }

    /** Called from the WebView for each `captureHtmlTrigger`, on an arbitrary thread. */
    fun onHtmlCaptured(html: String?) {
        pendingHtmlCapture?.complete(decodeCapturedHtml(html))
    }

    fun dismissMessage() {
        _state.value = _state.value.copy(extractionFailed = false)
    }

    fun onCanNavigateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _state.value = _state.value.copy(canGoBack = canGoBack, canGoForward = canGoForward)
    }

    fun onGoBack() {
        _state.value = _state.value.copy(goBackTrigger = _state.value.goBackTrigger + 1)
    }

    fun onGoForward() {
        _state.value = _state.value.copy(goForwardTrigger = _state.value.goForwardTrigger + 1)
    }

    fun onAddressBarFocused() {
        val currentText = _state.value.addressBarText
        output?.onNext(BrowserBloc.Output.NavigateToLanding(currentText))
    }

    fun onPageTitleChanged(title: String?) {
        _state.value = _state.value.copy(pageTitle = title?.takeIf { it.isNotBlank() })
    }

    fun toggleBookmark() {
        val state = _state.value
        val url = BookmarkUrl.normalize(state.webViewReportedUrl.ifBlank { state.currentUrl })
        if (url == null) return
        scope.launch {
            if (url in state.bookmarkedUrls) {
                bookmarkRepository.removeBookmarkByUrl(url)
            } else {
                bookmarkRepository.addBookmark(url, state.pageTitle)
            }
        }
    }

    fun onHomeClicked() {
        output?.onNext(BrowserBloc.Output.NavigateHome)
    }

    private fun String.ensureHttps(): String =
        if (!startsWith("http://") && !startsWith("https://")) {
            "https://$this"
        } else {
            this
        }

    data class State(
        val currentUrl: String = DEFAULT_URL,
        val addressBarText: String = DEFAULT_URL,
        val webViewReportedUrl: String = "",
        val isExtracting: Boolean = false,
        val isWebViewLoading: Boolean = false,
        val extractionFailed: Boolean = false,
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,
        val goBackTrigger: Int = 0,
        val goForwardTrigger: Int = 0,
        val captureHtmlTrigger: Int = 0,
        /** The loaded page's `<title>`, or null before the WebView reports one. */
        val pageTitle: String? = null,
        /** Normalized URLs of every bookmark, to light up the star for the loaded page. */
        val bookmarkedUrls: Set<String> = emptySet(),
    )

    companion object {
        private const val DEFAULT_URL = "https://www.google.com"

        /** Generous enough for a large page to serialise, short enough not to read as a hang. */
        private const val HTML_CAPTURE_TIMEOUT_MS = 5_000L

        /**
         * Android hands a script's result back JSON-encoded (`"<html>…"`); iOS and the JavaFX
         * engine hand back the string itself. Normalises both, and treats JavaScript's `null` —
         * what Android sends when the script failed or its result was too large to marshal — as
         * nothing captured.
         */
        internal fun decodeCapturedHtml(raw: String?): String? {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty() || trimmed == "null") return null
            val decoded =
                if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                    runCatching { Json.decodeFromString<String>(trimmed) }.getOrNull()
                } else {
                    trimmed
                }
            return decoded?.takeIf { it.isNotBlank() }
        }
    }
}

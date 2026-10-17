@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.browser.impl

import com.plusmobileapps.chefmate.browser.BrowserBloc
import com.plusmobileapps.chefmate.browser.BrowserBookmark
import com.plusmobileapps.chefmate.browser.BrowserHistoryRepository
import com.plusmobileapps.chefmate.browser.testing.FakeBrowserBookmarkRepository
import com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData
import dev.mokkery.answering.returns
import dev.mokkery.answering.throws
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class BrowserViewModelTest {

    private val extractorService: RecipeExtractorService = mock()
    private val historyRepository: BrowserHistoryRepository =
        mock(block = { everySuspend { recordVisit(any()) } returns Unit })
    private val bookmarkRepository = FakeBrowserBookmarkRepository()
    private val outputs = mutableListOf<BrowserBloc.Output>()
    // Shared with `runTest(scheduler)` below so the view model's HTML-capture timeout runs on the
    // test's virtual clock rather than stalling for real seconds.
    private val scheduler = TestCoroutineScheduler()
    private val viewModel =
        BrowserViewModel(
                mainContext = UnconfinedTestDispatcher(scheduler),
                recipeExtractorService = extractorService,
                historyRepository = historyRepository,
                bookmarkRepository = bookmarkRepository,
            )
            .also { it.setOutput { output -> outputs.add(output) } }

    // region Bookmarks

    @Test
    fun When_toggling_an_unbookmarked_page_Then_bookmarked_with_page_title() {
        viewModel.onUrlLoadedInWebView("https://example.com/recipe#step-2")
        viewModel.onPageTitleChanged("Banana Bread")

        viewModel.toggleBookmark()

        bookmarkRepository.bookmarks.value.single().let {
            it.url shouldBe "https://example.com/recipe"
            it.title shouldBe "Banana Bread"
        }
        viewModel.state.value.bookmarkedUrls shouldBe setOf("https://example.com/recipe")
    }

    @Test
    fun When_toggling_a_bookmarked_page_Then_bookmark_removed() {
        bookmarkRepository.bookmarks.value =
            listOf(BrowserBookmark(id = 1, url = "https://example.com/recipe", title = "Recipe"))
        viewModel.onUrlLoadedInWebView("https://example.com/recipe")

        viewModel.toggleBookmark()

        bookmarkRepository.bookmarks.value shouldBe emptyList()
        viewModel.state.value.bookmarkedUrls shouldBe emptySet()
    }

    @Test
    fun When_navigating_Then_stale_page_title_cleared() {
        viewModel.onPageTitleChanged("Old page")
        viewModel.onUrlChanged("example.com")
        viewModel.onNavigate()

        viewModel.state.value.pageTitle shouldBe null
    }

    @Test
    fun When_home_clicked_Then_navigate_home_emitted() {
        viewModel.onHomeClicked()
        outputs.last() shouldBe BrowserBloc.Output.NavigateHome
    }

    // endregion

    // region URL handling

    @Test
    fun initial_state_has_default_url() {
        val state = viewModel.state.value
        state.currentUrl shouldBe "https://www.google.com"
        state.addressBarText shouldBe "https://www.google.com"
    }

    @Test
    fun When_url_changed_Then_address_bar_text_updated() {
        viewModel.onUrlChanged("example.com")
        viewModel.state.value.addressBarText shouldBe "example.com"
    }

    @Test
    fun When_navigate_Then_https_prepended_and_current_url_updated() {
        viewModel.onUrlChanged("example.com")
        viewModel.onNavigate()
        val state = viewModel.state.value
        state.currentUrl shouldBe "https://example.com"
        state.addressBarText shouldBe "https://example.com"
    }

    @Test
    fun When_navigate_with_http_prefix_Then_not_double_prefixed() {
        viewModel.onUrlChanged("http://example.com")
        viewModel.onNavigate()
        viewModel.state.value.currentUrl shouldBe "http://example.com"
    }

    @Test
    fun When_navigate_with_https_prefix_Then_not_double_prefixed() {
        viewModel.onUrlChanged("https://example.com")
        viewModel.onNavigate()
        viewModel.state.value.currentUrl shouldBe "https://example.com"
    }

    @Test
    fun When_url_loaded_in_webview_Then_webViewReportedUrl_and_addressBarText_updated() {
        viewModel.onUrlLoadedInWebView("https://www.example.com/page")
        val state = viewModel.state.value
        state.webViewReportedUrl shouldBe "https://www.example.com/page"
        state.addressBarText shouldBe "https://www.example.com/page"
    }

    @Test
    fun When_url_loaded_in_webview_Then_currentUrl_not_changed() {
        viewModel.onUrlLoadedInWebView("https://www.example.com/page")
        viewModel.state.value.currentUrl shouldBe "https://www.google.com"
    }

    @Test
    fun When_url_loaded_in_webview_Then_history_repository_records_visit() = runTest {
        viewModel.onUrlLoadedInWebView("https://www.example.com/page")
        verifySuspend { historyRepository.recordVisit("https://www.example.com/page") }
    }

    // endregion

    // region WebView loading state

    @Test
    fun When_webview_loading_changed_to_true_Then_state_reflects_loading() {
        viewModel.onWebViewLoadingChanged(true)
        viewModel.state.value.isWebViewLoading shouldBe true
    }

    @Test
    fun When_webview_loading_changed_to_false_Then_state_reflects_not_loading() {
        viewModel.onWebViewLoadingChanged(true)
        viewModel.onWebViewLoadingChanged(false)
        viewModel.state.value.isWebViewLoading shouldBe false
    }

    // endregion

    // region Recipe extraction

    @Test
    fun When_extract_recipe_succeeds_Then_emits_output_with_extracted_data() =
        runTest(scheduler) {
            val extracted = testExtractedRecipe()
            everySuspend { extractorService.extractRecipe(any(), any()) } returns extracted

            viewModel.onUrlChanged("https://example.com/recipe")
            viewModel.onNavigate()
            viewModel.extractRecipeWithCapture(PAGE_HTML)

            val state = viewModel.state.value
            state.isExtracting shouldBe false
            state.extractionFailed shouldBe false
            outputs shouldBe listOf(BrowserBloc.Output.RecipeExtracted(extracted))
        }

    @Test
    fun When_extract_recipe_fails_Then_marks_failure_and_emits_no_output() =
        runTest(scheduler) {
            everySuspend { extractorService.extractRecipe(any(), any()) } throws
                IllegalStateException("No recipe")

            viewModel.onUrlChanged("https://example.com")
            viewModel.onNavigate()
            viewModel.extractRecipeWithCapture(PAGE_HTML)

            val state = viewModel.state.value
            state.isExtracting shouldBe false
            state.extractionFailed shouldBe true
            outputs shouldBe emptyList()
        }

    @Test
    fun When_already_extracting_Then_second_call_ignored() =
        runTest(scheduler) {
            everySuspend { extractorService.extractRecipe(any(), any()) } returns
                testExtractedRecipe()

            viewModel.onUrlChanged("https://example.com/recipe")
            viewModel.onNavigate()
            // Withholding the WebView's answer leaves the first extraction in flight.
            viewModel.extractRecipe()
            val trigger = viewModel.state.value.captureHtmlTrigger
            viewModel.extractRecipe()

            // A second tap must not kick off another capture, which would orphan the first.
            viewModel.state.value.captureHtmlTrigger shouldBe trigger
        }

    @Test
    fun When_extract_uses_webViewReportedUrl_over_currentUrl() =
        runTest(scheduler) {
            val extracted = testExtractedRecipe()
            everySuspend { extractorService.extractRecipe(any(), any()) } returns extracted

            viewModel.onUrlChanged("https://google.com")
            viewModel.onNavigate()
            viewModel.onUrlLoadedInWebView("https://example.com/actual-recipe")
            viewModel.extractRecipeWithCapture(PAGE_HTML)

            verifySuspend {
                extractorService.extractRecipe("https://example.com/actual-recipe", PAGE_HTML)
            }
        }

    // endregion

    // region Rendered HTML capture

    @Test
    fun When_extract_requested_Then_webview_is_asked_to_capture_the_page() =
        runTest(scheduler) {
            everySuspend { extractorService.extractRecipe(any(), any()) } returns
                testExtractedRecipe()

            viewModel.onUrlChanged("https://example.com/recipe")
            viewModel.onNavigate()
            val before = viewModel.state.value.captureHtmlTrigger
            viewModel.extractRecipeWithCapture(PAGE_HTML)

            viewModel.state.value.captureHtmlTrigger shouldBe before + 1
        }

    @Test
    fun When_captured_html_is_json_encoded_Then_it_is_decoded_before_extraction() =
        runTest(scheduler) {
            everySuspend { extractorService.extractRecipe(any(), any()) } returns
                testExtractedRecipe()

            viewModel.onUrlChanged("https://example.com/recipe")
            viewModel.onNavigate()
            // What Android's WebView hands back: the result JSON-encoded as a quoted string.
            viewModel.extractRecipeWithCapture("\"<html>a \\\"quoted\\\" title</html>\"")

            verifySuspend {
                extractorService.extractRecipe(
                    "https://example.com/recipe",
                    "<html>a \"quoted\" title</html>",
                )
            }
        }

    @Test
    fun When_webview_reports_no_html_Then_extraction_proceeds_without_it() =
        runTest(scheduler) {
            everySuspend { extractorService.extractRecipe(any(), any()) } returns
                testExtractedRecipe()

            viewModel.onUrlChanged("https://example.com/recipe")
            viewModel.onNavigate()
            // Android sends the string "null" when a script fails or its result can't be
            // marshalled.
            viewModel.extractRecipeWithCapture("null")

            verifySuspend { extractorService.extractRecipe("https://example.com/recipe", null) }
        }

    @Test
    fun When_webview_never_answers_Then_extraction_proceeds_after_the_timeout() =
        runTest(scheduler) {
            everySuspend { extractorService.extractRecipe(any(), any()) } returns
                testExtractedRecipe()

            viewModel.onUrlChanged("https://example.com/recipe")
            viewModel.onNavigate()
            viewModel.extractRecipe()
            // No onHtmlCaptured call — a WebView that can't run the script must not strand the
            // user on a spinner.
            viewModel.state.value.isExtracting shouldBe true
            advanceUntilIdle()

            viewModel.state.value.isExtracting shouldBe false
            verifySuspend { extractorService.extractRecipe("https://example.com/recipe", null) }
        }

    // endregion

    // region Dismiss message

    @Test
    fun When_dismiss_message_Then_clears_failure_state() =
        runTest(scheduler) {
            everySuspend { extractorService.extractRecipe(any(), any()) } throws
                IllegalStateException("No recipe")

            viewModel.onUrlChanged("https://example.com/recipe")
            viewModel.onNavigate()
            viewModel.extractRecipeWithCapture(PAGE_HTML)

            viewModel.dismissMessage()

            viewModel.state.value.extractionFailed shouldBe false
        }

    // endregion

    /**
     * Plays the WebView's half of the extraction handshake: the view model asks for the page, the
     * WebView answers with [html]. Callers that want the no-answer path call `extractRecipe` alone.
     */
    private fun BrowserViewModel.extractRecipeWithCapture(html: String?) {
        extractRecipe()
        onHtmlCaptured(html)
    }

    private fun testExtractedRecipe() =
        ExtractedRecipeData(
            title = "Test Recipe",
            description = "A test recipe",
            ingredients = listOf("1 cup flour"),
            directions = listOf("Mix"),
            imageUrl = null,
            sourceUrl = "https://example.com/recipe",
            servings = 4,
            prepTime = 10,
            cookTime = 20,
            totalTime = 30,
            calories = 200,
        )

    companion object {
        private const val PAGE_HTML = "<html><body>recipe</body></html>"
    }
}

@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.browser.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.browser.BrowserBloc
import com.plusmobileapps.chefmate.browser.BrowserEditQueryBloc
import com.plusmobileapps.chefmate.browser.BrowserLandingBloc
import com.plusmobileapps.chefmate.browser.BrowserRootBloc
import com.plusmobileapps.chefmate.browser.BrowserSelectEngineBloc
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import com.russhwolf.settings.MapSettings
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.mock
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow

class BrowserRootBlocImplTest {

    private val context = TestBlocContext.create()
    private val output = TestConsumer<BrowserRootBloc.Output>()

    private var browserOutput: Consumer<BrowserBloc.Output> = Consumer {}
    private var landingOutput: Consumer<BrowserLandingBloc.Output> = Consumer {}
    private var editQueryOutput: Consumer<BrowserEditQueryBloc.Output> = Consumer {}
    private var selectEngineOutput: Consumer<BrowserSelectEngineBloc.Output> = Consumer {}
    private var lastEditQueryInitialText: String? = null

    /** Defaults to a chosen engine so most tests bypass the first-run picker. */
    private fun createBloc(
        initialUrl: String? = null,
        showControls: Boolean = true,
        selectedEngine: SearchEngine? = SearchEngine.GOOGLE,
    ): BrowserRootBlocImpl =
        BrowserRootBlocImpl(
            context = context,
            output = output,
            initialUrl = initialUrl,
            showControls = showControls,
            presentation = BrowserRootBloc.Presentation.Embedded,
            browserBlocFactory =
                object : BrowserBloc.Factory {
                    override fun create(
                        context: BlocContext,
                        output: Consumer<BrowserBloc.Output>,
                        showControls: Boolean,
                    ): BrowserBloc {
                        browserOutput = output
                        val bloc: BrowserBloc = mock(MockMode.autoUnit)
                        // observeCanGoBack collects bloc.state when Browser is active; provide
                        // a real StateFlow so the collect doesn't fail on platforms where
                        // mokkery's autoUnit doesn't synthesize one (notably iOS).
                        every { bloc.state } returns MutableStateFlow(BrowserBloc.Model())
                        return bloc
                    }
                },
            landingBlocFactory =
                BrowserLandingBloc.Factory { _, output ->
                    landingOutput = output
                    mock(MockMode.autoUnit)
                },
            editQueryBlocFactory =
                BrowserEditQueryBloc.Factory { _, output, initialText ->
                    editQueryOutput = output
                    lastEditQueryInitialText = initialText
                    mock(MockMode.autoUnit)
                },
            selectEngineBlocFactory =
                BrowserSelectEngineBloc.Factory { _, output ->
                    selectEngineOutput = output
                    mock(MockMode.autoUnit)
                },
            browserPreferences =
                BrowserPreferencesImpl(MapSettings()).apply {
                    selectedEngine?.let { setDefaultSearchEngine(it) }
                },
        )

    private fun BrowserRootBloc.activeChild(): BrowserRootBloc.Child =
        routerState.value.active.instance

    private fun BrowserRootBloc.stackSize(): Int = routerState.value.items.size

    @Test
    fun When_no_initial_url_and_engine_selected_Then_landing_is_shown() {
        val bloc = createBloc(initialUrl = null, selectedEngine = SearchEngine.GOOGLE)
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.Landing>()
        bloc.stackSize() shouldBe 1
    }

    @Test
    fun When_no_engine_selected_Then_select_engine_is_shown() {
        val bloc = createBloc(initialUrl = null, selectedEngine = null)
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.SelectEngine>()
        bloc.stackSize() shouldBe 1
    }

    @Test
    fun When_initial_url_present_Then_select_engine_is_skipped() {
        val bloc = createBloc(initialUrl = "https://example.com", selectedEngine = null)
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.Browser>()
    }

    @Test
    fun When_engine_selected_output_Then_landing_replaces_select_engine() {
        val bloc = createBloc(initialUrl = null, selectedEngine = null)
        selectEngineOutput.onNext(BrowserSelectEngineBloc.Output.EngineSelected)
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.Landing>()
        bloc.stackSize() shouldBe 1
    }

    @Test
    fun When_initial_url_Then_browser_is_shown() {
        val bloc = createBloc(initialUrl = "https://example.com/recipe")
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.Browser>()
        bloc.stackSize() shouldBe 1
    }

    @Test
    fun When_landing_emits_open_edit_query_Then_edit_query_pushed_on_top_of_landing() {
        val bloc = createBloc()
        landingOutput.onNext(BrowserLandingBloc.Output.OpenEditQuery)
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.EditQuery>()
        bloc.stackSize() shouldBe 2
        // Landing always seeds an empty initialText since the field is read-only.
        lastEditQueryInitialText shouldBe ""
    }

    @Test
    fun When_edit_query_emits_navigate_Then_browser_replaces_edit_query() {
        val bloc = createBloc()
        landingOutput.onNext(BrowserLandingBloc.Output.OpenEditQuery)
        editQueryOutput.onNext(BrowserEditQueryBloc.Output.Navigate("https://example.com"))
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.Browser>()
        // Landing remains below the new Browser; back from Browser returns to Landing.
        bloc.stackSize() shouldBe 2
    }

    @Test
    fun When_landing_emits_open_url_Then_browser_pushed_on_top_of_landing() {
        val bloc = createBloc()
        landingOutput.onNext(BrowserLandingBloc.Output.OpenUrl(SearchEngine.GOOGLE.homeUrl))
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.Browser>()
        // Back from the engine's page returns to Landing.
        bloc.stackSize() shouldBe 2
    }

    @Test
    fun When_landing_emits_open_url_twice_Then_single_browser_on_stack() {
        val bloc = createBloc()
        landingOutput.onNext(BrowserLandingBloc.Output.OpenUrl(SearchEngine.GOOGLE.homeUrl))
        landingOutput.onNext(BrowserLandingBloc.Output.OpenUrl(SearchEngine.GOOGLE.homeUrl))
        bloc.stackSize() shouldBe 2
    }

    @Test
    fun When_edit_query_emits_cancel_Then_stack_pops_to_previous_child() {
        val bloc = createBloc()
        landingOutput.onNext(BrowserLandingBloc.Output.OpenEditQuery)
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.EditQuery>()

        editQueryOutput.onNext(BrowserEditQueryBloc.Output.Cancel)
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.Landing>()
        bloc.stackSize() shouldBe 1
    }

    @Test
    fun When_browser_emits_navigate_to_landing_Then_edit_query_pushed_with_initial_text() {
        val bloc = createBloc(initialUrl = "https://example.com")
        browserOutput.onNext(BrowserBloc.Output.NavigateToLanding("https://example.com"))
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.EditQuery>()
        bloc.stackSize() shouldBe 2
        lastEditQueryInitialText shouldBe "https://example.com"
    }

    @Test
    fun When_browser_emits_recipe_extracted_Then_root_outputs_recipe_extracted() {
        createBloc(initialUrl = "https://example.com")
        val extracted = sampleExtractedRecipe()
        browserOutput.onNext(BrowserBloc.Output.RecipeExtracted(extracted))
        output.lastValue shouldBe BrowserRootBloc.Output.RecipeExtracted(extracted)
    }

    @Test
    fun When_navigate_to_url_called_Then_browser_replaces_stack() {
        val bloc = createBloc()
        bloc.navigateToUrl("https://example.com/shared")
        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.Browser>()
        bloc.stackSize() shouldBe 1
    }

    /**
     * Regression for the "Configurations must be unique" crash. Reproducing the original flow:
     * Landing → push EditQuery("") → submit URL (push Browser) → tap address bar (push EditQuery
     * with empty text again because the address bar was just cleared). Without the dedupe helper
     * the stack would contain two EditQuery("") configurations and Decompose would crash.
     */
    @Test
    fun When_edit_query_pushed_while_one_already_in_back_stack_Then_no_duplicate_crash() {
        val bloc = createBloc()
        landingOutput.onNext(BrowserLandingBloc.Output.OpenEditQuery)
        editQueryOutput.onNext(BrowserEditQueryBloc.Output.Navigate("https://example.com"))
        // Tapping the cleared address bar in the new Browser would otherwise duplicate
        // EditQuery("") in the stack.
        browserOutput.onNext(BrowserBloc.Output.NavigateToLanding(""))

        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.EditQuery>()
        // Stack should still only contain a single EditQuery: [Landing, Browser, EditQuery("")].
        bloc.routerState.value.items.count {
            it.instance is BrowserRootBloc.Child.EditQuery
        } shouldBe 1
    }

    @Test
    fun When_edit_query_navigate_with_same_url_as_existing_browser_Then_no_duplicate_crash() {
        val bloc = createBloc()
        // Get to a Browser with a known URL the only way Landing currently allows: through
        // EditQuery. [Landing] -> [Landing, EditQuery] -> [Landing, Browser].
        landingOutput.onNext(BrowserLandingBloc.Output.OpenEditQuery)
        editQueryOutput.onNext(BrowserEditQueryBloc.Output.Navigate("https://example.com"))
        // Tap address bar → push EditQuery
        browserOutput.onNext(BrowserBloc.Output.NavigateToLanding("https://example.com"))
        // Submit the same URL we were just on; the existing Browser config matches.
        editQueryOutput.onNext(BrowserEditQueryBloc.Output.Navigate("https://example.com"))

        bloc.activeChild().shouldBeInstanceOf<BrowserRootBloc.Child.Browser>()
        bloc.routerState.value.items.count { it.instance is BrowserRootBloc.Child.Browser } shouldBe
            1
    }

    private fun sampleExtractedRecipe() =
        ExtractedRecipeData(
            title = "Test",
            description = null,
            ingredients = listOf("flour"),
            directions = listOf("mix"),
            imageUrl = null,
            sourceUrl = "https://example.com/recipe",
            servings = 4,
            prepTime = 10,
            cookTime = 20,
            totalTime = 30,
            calories = 200,
        )
}

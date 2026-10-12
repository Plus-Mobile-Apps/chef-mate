package com.plusmobileapps.chefmate.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.coroutines.flow.StateFlow

interface BrowserLandingBloc : ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        BrowserLandingScreen(bloc = this, modifier = modifier)
    }

    fun onSearchFieldFocused()

    /** Marks the first-run coach mark with [id] as seen so it never shows again. */
    fun onCoachMarkDismissed(id: String)

    /**
     * Opens the selected engine's own front page. Searching from the engine's form, rather than
     * loading a results URL the app built, is what a real browser session looks like — Google
     * answers a WebView that jumps straight to `/search?q=` with a CAPTCHA.
     */
    fun onOpenEngineHomeClicked()

    /** Changes the default search engine from the landing dropdown. */
    fun onSearchEngineSelected(engine: SearchEngine)

    data class Model(
        /** Id of the first-run coach mark currently allowed to show, or null when none. */
        val activeCoachMark: String? = null,
        /** The current default search engine, reflected in the dropdown and search hint. */
        val selectedEngine: SearchEngine = SearchEngine.GOOGLE,
    )

    sealed class Output {
        data object OpenEditQuery : Output()

        data class OpenUrl(val url: String) : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): BrowserLandingBloc
    }
}

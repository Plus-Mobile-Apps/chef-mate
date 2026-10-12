@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.browser.impl

import com.plusmobileapps.chefmate.browser.BrowserLandingBloc
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BrowserLandingBlocImplTest {

    private val context = TestBlocContext.create()
    private val output = TestConsumer<BrowserLandingBloc.Output>()
    private val coachMarkController = CoachMarkController(MapSettings())
    private val browserPreferences = BrowserPreferencesImpl(MapSettings())
    private val bloc =
        BrowserLandingBlocImpl(
            context = context,
            output = output,
            coachMarkController = coachMarkController,
            browserPreferences = browserPreferences,
        )

    @Test
    fun When_search_field_focused_Then_open_edit_query_emitted() {
        bloc.onSearchFieldFocused()
        output.lastValue shouldBe BrowserLandingBloc.Output.OpenEditQuery
    }

    @Test
    fun When_open_engine_home_clicked_Then_selected_engine_home_url_emitted() {
        browserPreferences.setDefaultSearchEngine(SearchEngine.DUCK_DUCK_GO)
        bloc.onOpenEngineHomeClicked()
        output.lastValue shouldBe
            BrowserLandingBloc.Output.OpenUrl(SearchEngine.DUCK_DUCK_GO.homeUrl)
    }

    @Test
    fun When_open_engine_home_clicked_Then_coach_mark_dismissed() {
        bloc.onOpenEngineHomeClicked()
        coachMarkController.hasSeen(CoachMarkId.BROWSER_SEARCH) shouldBe true
    }

    @Test
    fun When_screen_opens_Then_search_coach_mark_active() {
        bloc.state.value.activeCoachMark shouldBe CoachMarkId.BROWSER_SEARCH
    }

    @Test
    fun When_search_field_focused_Then_coach_mark_dismissed_and_persisted() {
        bloc.onSearchFieldFocused()

        coachMarkController.hasSeen(CoachMarkId.BROWSER_SEARCH) shouldBe true
        bloc.state.value.activeCoachMark shouldBe null
    }

    @Test
    fun When_engine_set_in_preferences_Then_state_reflects_it() {
        browserPreferences.setDefaultSearchEngine(SearchEngine.BING)
        bloc.state.value.selectedEngine shouldBe SearchEngine.BING
    }

    @Test
    fun When_no_engine_set_Then_state_defaults_to_google() {
        bloc.state.value.selectedEngine shouldBe SearchEngine.GOOGLE
    }

    @Test
    fun When_search_engine_selected_Then_persisted_to_preferences() {
        bloc.onSearchEngineSelected(SearchEngine.DUCK_DUCK_GO)
        browserPreferences.defaultSearchEngine.value shouldBe SearchEngine.DUCK_DUCK_GO
    }
}

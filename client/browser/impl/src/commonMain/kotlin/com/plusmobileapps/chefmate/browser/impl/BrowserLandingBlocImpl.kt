package com.plusmobileapps.chefmate.browser.impl

import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.browser.BrowserBookmark
import com.plusmobileapps.chefmate.browser.BrowserBookmarkRepository
import com.plusmobileapps.chefmate.browser.BrowserLandingBloc
import com.plusmobileapps.chefmate.browser.BrowserPreferences
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.combineStates
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = BrowserLandingBloc.Factory::class,
)
class BrowserLandingBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<BrowserLandingBloc.Output>,
    private val coachMarkController: CoachMarkController,
    private val browserPreferences: BrowserPreferences,
    private val bookmarkRepository: BrowserBookmarkRepository,
) : BrowserLandingBloc, BlocContext by context {

    private val scope = createScope()

    private val bookmarks: StateFlow<List<BrowserBookmark>> =
        bookmarkRepository.observeBookmarks().stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val state: StateFlow<BrowserLandingBloc.Model> =
        combineStates(
            coachMarkController.activeCoachMark,
            browserPreferences.defaultSearchEngine,
            bookmarks,
        ) { activeCoachMark, engine, bookmarks ->
            BrowserLandingBloc.Model(
                activeCoachMark = activeCoachMark,
                selectedEngine = engine ?: SearchEngine.GOOGLE,
                bookmarks = bookmarks,
            )
        }

    init {
        coachMarkController.request(CoachMarkId.BROWSER_SEARCH)
        // Leaving the screen without dismissing frees the queue so other coach marks can show.
        lifecycle.doOnDestroy { coachMarkController.release(CoachMarkId.BROWSER_SEARCH) }
    }

    override fun onSearchFieldFocused() {
        dismissCoachMark(CoachMarkId.BROWSER_SEARCH)
        output.onNext(BrowserLandingBloc.Output.OpenEditQuery)
    }

    override fun onOpenEngineHomeClicked() {
        dismissCoachMark(CoachMarkId.BROWSER_SEARCH)
        output.onNext(BrowserLandingBloc.Output.OpenUrl(state.value.selectedEngine.homeUrl))
    }

    override fun onCoachMarkDismissed(id: String) {
        dismissCoachMark(id)
    }

    override fun onSearchEngineSelected(engine: SearchEngine) {
        browserPreferences.setDefaultSearchEngine(engine)
    }

    override fun onBookmarkClicked(bookmark: BrowserBookmark) {
        dismissCoachMark(CoachMarkId.BROWSER_SEARCH)
        output.onNext(BrowserLandingBloc.Output.OpenUrl(bookmark.url))
    }

    override fun onDeleteBookmarkClicked(bookmark: BrowserBookmark) {
        scope.launch { bookmarkRepository.removeBookmark(bookmark.id) }
    }

    /** Mark a coach mark seen, but only if it's the one currently showing. */
    private fun dismissCoachMark(id: String) {
        if (coachMarkController.activeCoachMark.value == id) {
            coachMarkController.dismiss(id)
        }
    }
}

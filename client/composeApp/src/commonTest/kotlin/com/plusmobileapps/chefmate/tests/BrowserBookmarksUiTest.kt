package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import com.plusmobileapps.chefmate.browser.SearchEngine
import com.plusmobileapps.chefmate.browser.robots.browser
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.recipe.bottomnav.robots.bottomNav
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class BrowserBookmarksUiTest {

    @Test
    fun bookmarks_show_on_landing_and_can_be_deleted() = runRootBlocTest { component ->
        component.browserBookmarkRepository.addBookmark(
            url = "https://www.seriouseats.com/classic-banana-bread-recipe",
            title = "Classic Banana Bread",
        )

        bottomNav().clickBrowserTab()

        browser()
            .awaitSelectEngineDisplayed()
            .selectEngine(SearchEngine.GOOGLE)
            .awaitLandingDisplayed()
            .awaitBookmarkDisplayed("Classic Banana Bread")
            .deleteBookmark("Classic Banana Bread")
            .awaitBookmarkGone("Classic Banana Bread")
    }
}

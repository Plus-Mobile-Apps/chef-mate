@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.browser.robots

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.waitUntilDoesNotExist
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.plusmobileapps.chefmate.browser.BrowserTestTags
import com.plusmobileapps.chefmate.browser.SearchEngine

/**
 * Robot for the in-app browser's first-run search-engine picker and landing screen. Construct via
 * [browser] from inside a `runComposeUiTest { … }` block.
 */
class BrowserRobot(private val test: ComposeUiTest) {

    fun awaitSelectEngineDisplayed(): BrowserRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(BrowserTestTags.SELECT_ENGINE_SCREEN))
    }

    fun selectEngine(engine: SearchEngine): BrowserRobot = apply {
        test
            .onNode(
                hasTestTag(BrowserTestTags.engineOption(engine)) and
                    hasAnyAncestor(hasTestTag(BrowserTestTags.SELECT_ENGINE_SCREEN))
            )
            .performClick()
    }

    fun awaitLandingDisplayed(): BrowserRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(BrowserTestTags.LANDING_SCREEN))
    }

    /** Asserts the landing engine dropdown shows [engine]'s display name. */
    fun assertLandingEngine(engine: SearchEngine): BrowserRobot = apply {
        test
            .onNodeWithTag(BrowserTestTags.LANDING_ENGINE_DROPDOWN)
            .assertTextContains(engine.displayName)
    }

    fun openLandingEngineDropdown(): BrowserRobot = apply {
        test.onNodeWithTag(BrowserTestTags.LANDING_ENGINE_DROPDOWN).performClick()
    }

    /** Waits for a bookmark tile titled [title] in the landing screen's bookmarks grid. */
    fun awaitBookmarkDisplayed(title: String): BrowserRobot = apply {
        test.waitUntilExactlyOneExists(bookmarkTitle(title))
        test.onNode(bookmarkTitle(title)).performScrollTo()
    }

    /** Deletes the bookmark titled [title] through its tile's options menu. */
    fun deleteBookmark(title: String): BrowserRobot = apply {
        test
            .onNode(
                hasContentDescription(title, substring = true) and
                    hasAnyAncestor(hasTestTag(BrowserTestTags.LANDING_BOOKMARKS))
            )
            .performClick()
        test.onNodeWithTag(BrowserTestTags.BOOKMARK_DELETE).performClick()
    }

    fun awaitBookmarkGone(title: String): BrowserRobot = apply {
        test.waitUntilDoesNotExist(bookmarkTitle(title))
    }

    private fun bookmarkTitle(title: String) =
        hasText(title) and hasAnyAncestor(hasTestTag(BrowserTestTags.LANDING_BOOKMARKS))
}

fun ComposeUiTest.browser(): BrowserRobot = BrowserRobot(this)

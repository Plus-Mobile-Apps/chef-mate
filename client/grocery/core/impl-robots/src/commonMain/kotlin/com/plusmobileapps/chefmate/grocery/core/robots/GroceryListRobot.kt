@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.grocery.core.robots

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilAtLeastOneExists
import androidx.compose.ui.test.waitUntilExactlyOneExists
import androidx.compose.ui.text.AnnotatedString
import com.plusmobileapps.chefmate.grocery.core.detail.GroceryDetailTestTags
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListTestTags

/**
 * Robot for interacting with and asserting on the grocery list screen, including the detail bottom
 * sheet that overlays it. Lives in `client/grocery/core/impl-robots` so cross-feature UI tests can
 * compose against domain-level vocabulary.
 *
 * Every list-screen lookup is scoped under [GroceryListTestTags.SCREEN]. Sheet content is matched
 * under [GroceryDetailTestTags.SHEET] because once the sheet is open it floats above the list and
 * lives in a separate semantic subtree.
 *
 * Construct via [groceryList] from inside a `runComposeUiTest { … }` block.
 */
class GroceryListRobot(private val test: ComposeUiTest) {

    private val onScreen = hasAnyAncestor(hasTestTag(GroceryListTestTags.SCREEN))
    private val inDetailSheet = hasAnyAncestor(hasTestTag(GroceryDetailTestTags.SHEET))

    fun clickItem(displayName: String): GroceryListRobot = apply {
        test.onNode(hasText(displayName) and onScreen).assertIsDisplayed().performClick()
    }

    fun awaitDetailSheetDisplayed(): GroceryListRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(GroceryDetailTestTags.SHEET))
    }

    fun assertAisleSelected(label: String): GroceryListRobot = apply {
        test.onNode(hasText(label) and inDetailSheet).assertIsDisplayed()
    }

    fun enterItemNamePrefix(prefix: String): GroceryListRobot = apply {
        test
            .onNode(hasTestTag(GroceryListTestTags.ITEM_INPUT) and onScreen)
            .assertIsDisplayed()
            .performClick()
            .performTextInput(prefix)
    }

    fun clickItemSuggestion(suggestion: String): GroceryListRobot = apply {
        val matcher = hasTestTag(GroceryListTestTags.ITEM_SUGGESTION) and hasText(suggestion)
        test.waitUntilExactlyOneExists(matcher)
        test.onNode(matcher).assertIsDisplayed().performClick()
    }

    fun assertItemInputText(text: String): GroceryListRobot = apply {
        test.onNode(hasTestTag(GroceryListTestTags.ITEM_INPUT) and onScreen).assertTextEquals(text)
    }

    /**
     * Asserts the input has been cleared. Checked against `EditableText` rather than
     * [assertItemInputText] because an empty field renders its placeholder, and the placeholder
     * counts towards the node's `Text`.
     */
    fun assertItemInputEmpty(): GroceryListRobot = apply {
        test
            .onNode(hasTestTag(GroceryListTestTags.ITEM_INPUT) and onScreen)
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
            )
    }

    /** Taps the Done button beside the input, which only exists while the field has focus. */
    fun clickDone(): GroceryListRobot = apply {
        val matcher = hasTestTag(GroceryListTestTags.DONE_BUTTON) and onScreen
        test.waitUntilExactlyOneExists(matcher)
        test.onNode(matcher).performClick()
    }

    /**
     * Waits for a list row named [displayName]. The add-item field is excluded: right after typing
     * a name and submitting it, the field still holds that same text until the add lands, and
     * matching it would let this return before the item exists.
     */
    fun awaitItemDisplayed(displayName: String): GroceryListRobot = apply {
        test.waitUntilAtLeastOneExists(
            hasText(displayName) and onScreen and !hasTestTag(GroceryListTestTags.ITEM_INPUT)
        )
    }

    /** Opens the list selector (bottom sheet on phones, dropdown on tablets). */
    fun openListSelector(): GroceryListRobot = apply {
        test
            .onNode(hasTestTag(GroceryListTestTags.LIST_SELECTOR) and onScreen)
            .assertIsDisplayed()
            .performClick()
    }

    /**
     * Clicks the edit (pencil) icon on the first list row in the open selector. The selector floats
     * in its own subtree (sheet or dropdown popup), so this is matched by content description
     * rather than scoped to the list screen.
     */
    fun clickEditFirstList(editContentDescription: String): GroceryListRobot = apply {
        test.waitUntilAtLeastOneExists(hasContentDescription(editContentDescription))
        test.onAllNodes(hasContentDescription(editContentDescription)).onFirst().performClick()
    }
}

fun ComposeUiTest.groceryList(): GroceryListRobot = GroceryListRobot(this)

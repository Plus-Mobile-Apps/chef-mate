@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.family.manage.robots

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.plusmobileapps.chefmate.family.manage.FamilyListTestTags

/**
 * Robot for the Manage Family list screen. Every lookup is scoped under [FamilyListTestTags.SCREEN]
 * so a family name never matches a like-named node elsewhere.
 */
class FamilyListRobot(private val test: ComposeUiTest) {

    private val onScreen = hasAnyAncestor(hasTestTag(FamilyListTestTags.SCREEN))

    fun awaitDisplayed(): FamilyListRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(FamilyListTestTags.SCREEN))
    }

    fun assertDisplayed(): FamilyListRobot = apply {
        test.onNodeWithTag(FamilyListTestTags.SCREEN).assertIsDisplayed()
    }

    fun assertNotDisplayed(): FamilyListRobot = apply {
        test.onNodeWithTag(FamilyListTestTags.SCREEN).assertDoesNotExist()
    }

    /** Waits for the empty state's "Create your first family" button and taps it. */
    fun createFirstFamily(): FamilyListRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(FamilyListTestTags.CREATE_FIRST_BUTTON))
        test.onNodeWithTag(FamilyListTestTags.CREATE_FIRST_BUTTON).performClick()
    }

    fun typeFamilyName(name: String): FamilyListRobot = apply {
        // The test tag sits on the PlusTextField wrapper; the editable node is the inner field.
        test
            .onNode(
                hasSetTextAction() and
                    hasAnyAncestor(hasTestTag(FamilyListTestTags.CREATE_NAME_FIELD))
            )
            .performTextReplacement(name)
    }

    /** Taps the confirm button of the "New family" dialog. */
    fun confirmCreate(label: String = "Create"): FamilyListRobot = apply {
        test.onNode(hasText(label)).performClick()
    }

    fun awaitFamily(name: String): FamilyListRobot = apply {
        test.waitUntilExactlyOneExists(hasText(name, substring = true) and onScreen)
    }

    fun openFamily(name: String): FamilyListRobot = apply {
        awaitFamily(name)
        test.onNode(hasText(name, substring = true) and onScreen).performClick()
    }
}

fun ComposeUiTest.familyList(): FamilyListRobot = FamilyListRobot(this)

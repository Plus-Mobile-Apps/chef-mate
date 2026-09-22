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
import com.plusmobileapps.chefmate.family.manage.FamilyDetailTestTags

/** Robot for one family's member list, invite form and danger zone. */
class FamilyDetailRobot(private val test: ComposeUiTest) {

    private val onScreen = hasAnyAncestor(hasTestTag(FamilyDetailTestTags.SCREEN))

    fun awaitDisplayed(): FamilyDetailRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(FamilyDetailTestTags.SCREEN))
    }

    fun assertDisplayed(): FamilyDetailRobot = apply {
        test.onNodeWithTag(FamilyDetailTestTags.SCREEN).assertIsDisplayed()
    }

    fun awaitMember(text: String): FamilyDetailRobot = apply {
        test.waitUntilExactlyOneExists(hasText(text, substring = true) and onScreen)
    }

    fun typeInviteEmail(email: String): FamilyDetailRobot = apply {
        test
            .onNode(
                hasSetTextAction() and
                    hasAnyAncestor(hasTestTag(FamilyDetailTestTags.INVITE_EMAIL_FIELD))
            )
            .performTextReplacement(email)
    }

    fun sendInvite(): FamilyDetailRobot = apply {
        test.onNodeWithTag(FamilyDetailTestTags.INVITE_BUTTON).performClick()
    }

    fun leaveFamily(): FamilyDetailRobot = apply {
        test.onNodeWithTag(FamilyDetailTestTags.LEAVE_BUTTON).performClick()
    }

    fun deleteFamily(): FamilyDetailRobot = apply {
        test.onNodeWithTag(FamilyDetailTestTags.DELETE_BUTTON).performClick()
    }

    fun assertInviteNotShown(): FamilyDetailRobot = apply {
        test.onNodeWithTag(FamilyDetailTestTags.INVITE_BUTTON).assertDoesNotExist()
    }

    fun assertDeleteNotShown(): FamilyDetailRobot = apply {
        test.onNodeWithTag(FamilyDetailTestTags.DELETE_BUTTON).assertDoesNotExist()
    }

    /** Taps the confirm button of whichever dialog is showing. */
    fun confirmDialog(label: String): FamilyDetailRobot = apply {
        test.waitUntilExactlyOneExists(hasText(label))
        test.onNode(hasText(label)).performClick()
    }
}

fun ComposeUiTest.familyDetail(): FamilyDetailRobot = FamilyDetailRobot(this)

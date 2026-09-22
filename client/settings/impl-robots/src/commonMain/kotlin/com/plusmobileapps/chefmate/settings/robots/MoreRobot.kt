@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.settings.robots

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.plusmobileapps.chefmate.settings.SettingsTestTags

/**
 * Robot for the "More" tab content (the SettingsScreen list of rows). Every node lookup is scoped
 * under [SettingsTestTags.SCREEN] so a row label here never matches a like-named node on another
 * screen.
 */
class MoreRobot(private val test: ComposeUiTest) {

    private val onScreen = hasAnyAncestor(hasTestTag(SettingsTestTags.SCREEN))

    fun awaitDisplayed(): MoreRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(SettingsTestTags.SCREEN))
    }

    fun clickAiChatRow(): MoreRobot = clickRow("AI Chat")

    fun clickAppSettingsRow(): MoreRobot = clickRow("Settings")

    fun clickManageProfileRow(): MoreRobot = clickRow("Manage Profile")

    fun clickNotificationsRow(): MoreRobot = clickRow("Notifications")

    fun clickManageFamilyRow(): MoreRobot = clickRow("Manage Family")

    fun assertManageFamilyRowNotShown(): MoreRobot = apply {
        test.onNodeWithTag(SettingsTestTags.MANAGE_FAMILY_ROW).assertDoesNotExist()
    }

    private fun clickRow(label: String): MoreRobot = apply {
        test.waitUntilExactlyOneExists(hasText(label) and onScreen)
        test.onNode(hasText(label) and onScreen).performClick()
    }
}

fun ComposeUiTest.more(): MoreRobot = MoreRobot(this)

@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.grocery.categoryrules.robots

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.waitUntilExactlyOneExists
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesTestTags

/**
 * Robot for the Settings → Grocery → Category rules management screen. Every node lookup is scoped
 * under [GroceryCategoryRulesTestTags.SCREEN] so a rule label here never matches a like-named node
 * on another screen (e.g. the same item name rendered in the grocery list).
 */
class GroceryCategoryRulesRobot(private val test: ComposeUiTest) {

    private val onScreen = hasAnyAncestor(hasTestTag(GroceryCategoryRulesTestTags.SCREEN))

    fun awaitDisplayed(): GroceryCategoryRulesRobot = apply {
        test.waitUntilExactlyOneExists(hasTestTag(GroceryCategoryRulesTestTags.SCREEN))
    }

    fun assertTextDisplayed(text: String): GroceryCategoryRulesRobot = apply {
        test.onNode(hasText(text, substring = true) and onScreen).assertIsDisplayed()
    }

    fun openAddField(): GroceryCategoryRulesRobot = apply {
        test.onNode(hasTestTag(GroceryCategoryRulesTestTags.ADD_BUTTON) and onScreen).performClick()
    }

    fun typeRuleName(name: String): GroceryCategoryRulesRobot = apply {
        test
            .onNode(hasTestTag(GroceryCategoryRulesTestTags.CREATE_FIELD) and onScreen)
            .performTextInput(name)
    }
}

fun ComposeUiTest.groceryCategoryRules(): GroceryCategoryRulesRobot =
    GroceryCategoryRulesRobot(this)

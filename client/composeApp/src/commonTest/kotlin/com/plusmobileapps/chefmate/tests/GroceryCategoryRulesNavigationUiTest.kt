package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import com.plusmobileapps.chefmate.grocery.categoryrules.robots.groceryCategoryRules
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.recipe.bottomnav.robots.bottomNav
import com.plusmobileapps.chefmate.settings.robots.more
import com.plusmobileapps.chefmate.settings.root.robots.settingsRoot
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class GroceryCategoryRulesNavigationUiTest {

    @Test
    fun opening_settings_then_category_rules_lands_on_the_management_screen() = runRootBlocTest {
        bottomNav().clickMoreTab()
        more().awaitDisplayed().clickAppSettingsRow()

        settingsRoot().awaitDisplayed().clickRow("Category rules")

        // The "Your rules" section header renders near the top of the list, so it's a stable
        // signal the management screen loaded.
        groceryCategoryRules().awaitDisplayed().assertTextDisplayed("Your rules")
    }
}

@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import com.plusmobileapps.chefmate.grocery.core.robots.groceryList
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.recipe.bottomnav.robots.bottomNav
import kotlin.test.Test

class GroceryListDoneButtonUiTest {

    @Test
    fun done_adds_whatever_is_left_in_the_input() = runRootBlocTest {
        bottomNav().clickGroceriesTab()

        groceryList()
            .enterItemNamePrefix("Strawberries")
            .clickDone()
            .awaitItemDisplayed("Strawberries")
            .assertItemInputEmpty()
    }
}

@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.tests

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.waitUntilDoesNotExist
import com.plusmobileapps.chefmate.grocery.core.list.GroceryDisplayGroup
import com.plusmobileapps.chefmate.grocery.core.list.GroceryDisplayItem
import com.plusmobileapps.chefmate.grocery.core.list.GroceryGroupedList
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the swipe-to-delete gesture on the shared grocery list rows. `swipeToDeleteEnabled` is
 * passed explicitly rather than left to its platform default so the gesture is exercised on every
 * target the test suite runs on, not just touch platforms.
 */
class GroceryListSwipeToDeleteUiTest {

    @Test
    fun swiping_a_row_away_deletes_that_item() = runComposeUiTest {
        val deleted = mutableStateListOf<GroceryDisplayItem>()
        setGroceryListContent(deleted, swipeToDeleteEnabled = true)

        onNodeWithText("Apples").performTouchInput { swipeLeft() }

        waitUntilDoesNotExist(hasText("Apples"))
        assertEquals(listOf("Apples"), deleted.map { it.displayName })
    }

    @Test
    fun a_swiped_row_that_comes_back_is_not_deleted_again() = runComposeUiTest {
        val deleted = mutableStateListOf<GroceryDisplayItem>()
        setGroceryListContent(deleted, swipeToDeleteEnabled = true)
        onNodeWithText("Apples").performTouchInput { swipeLeft() }
        waitUntilDoesNotExist(hasText("Apples"))

        // Undo: the row returns under the same key.
        val restored = deleted.removeAt(0)
        waitForIdle()

        onNodeWithText("Apples").assertIsDisplayed()
        assertTrue(restored !in deleted, "restored row was deleted again")
    }

    @Test
    fun swiping_does_nothing_when_swipe_to_delete_is_disabled() = runComposeUiTest {
        val deleted = mutableStateListOf<GroceryDisplayItem>()
        setGroceryListContent(deleted, swipeToDeleteEnabled = false)

        onNodeWithText("Apples").performTouchInput { swipeLeft() }
        waitForIdle()

        onNodeWithText("Apples").assertIsDisplayed()
        assertTrue(deleted.isEmpty())
    }
}

private fun ComposeUiTest.setGroceryListContent(
    deleted: MutableList<GroceryDisplayItem>,
    swipeToDeleteEnabled: Boolean,
) {
    setContent {
        val items =
            listOf(
                    GroceryDisplayItem(key = 1L, displayName = "Apples"),
                    GroceryDisplayItem(key = 2L, displayName = "Bananas"),
                )
                .filterNot { it in deleted }
        ChefMateTheme {
            GroceryGroupedList(
                groups = listOf(GroceryDisplayGroup(GroceryCategory.PRODUCE, items)),
                onItemClick = {},
                onCheckedChange = {},
                onSwipeToDelete = { deleted += it },
                swipeToDeleteEnabled = swipeToDeleteEnabled,
            )
        }
    }
}

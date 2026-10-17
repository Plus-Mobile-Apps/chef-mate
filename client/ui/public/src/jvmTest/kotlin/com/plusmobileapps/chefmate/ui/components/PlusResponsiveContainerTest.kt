@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlin.test.Test

class PlusResponsiveContainerTest {

    /**
     * Regression test for a Compose Desktop crash ("RootNodeOwner is already disposed") when a
     * window resize crossed a breakpoint while a Popup — e.g. a coach mark — lived in the branch
     * being swapped out.
     */
    @Test
    fun crossingBreakpointSwapsPopupBranchWithoutCrashing() = runComposeUiTest {
        var width by mutableStateOf(900.dp)
        setContent {
            Box(Modifier.width(width)) {
                PlusResponsiveContainer { sizeClass ->
                    if (sizeClass == WindowSizeClass.COMPACT) Popup { Text("compact") }
                    else Popup { Text("wide") }
                }
            }
        }
        onNodeWithText("wide").assertExists()

        repeat(2) {
            width = 400.dp
            waitForIdle()
            onNodeWithText("compact").assertExists()
            onNodeWithText("wide").assertDoesNotExist()

            width = 900.dp
            waitForIdle()
            onNodeWithText("wide").assertExists()
            onNodeWithText("compact").assertDoesNotExist()
        }
    }
}

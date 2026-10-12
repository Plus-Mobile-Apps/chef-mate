package com.plusmobileapps.chefmate.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    val paddingExtraSmall: Dp = 4.dp,
    val paddingSmall: Dp = 8.dp,
    val paddingNormal: Dp = 16.dp,
    val paddingLarge: Dp = 24.dp,
    val paddingExtraLarge: Dp = 32.dp,
    val rowHeight: Dp = 56.dp,
    val fabClearance: Dp = 88.dp,
    // Widest a month calendar grid grows to. Past this the day cells spread so far apart the grid
    // stops reading as a calendar, so wide windows center it instead of stretching it.
    val maxCalendarWidth: Dp = 480.dp,
)

internal val LocalDimensions = staticCompositionLocalOf { AppDimensions() }

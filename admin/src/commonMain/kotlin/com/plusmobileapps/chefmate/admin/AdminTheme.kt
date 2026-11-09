@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.plusmobileapps.chefmate.admin

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Mirrors the shipping app's theme (client/ui/public/.../ui/theme). The admin module can't depend
// on `:client:ui:public` because no shared client module has a wasmJs target, so the tokens are
// copied here. Keep them in sync when the app theme changes.

private val purple200 = Color(0xFFBB86FC)
private val purple500 = Color(0xFF6200EE)
private val teal200 = Color(0xFF03DAC5)

private val DarkColorPalette = darkColorScheme(primary = purple200, secondary = teal200)

private val LightColorPalette = lightColorScheme(primary = purple500, secondary = teal200)

/** Same corner scale as the app's `chefMateShapes`. */
private val AdminShapes =
    Shapes(
        small = RoundedCornerShape(4.dp),
        medium = RoundedCornerShape(8.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

/** Spacing tokens matching the app's `AppDimensions`. */
object AdminDimens {
    val paddingExtraSmall = 4.dp
    val paddingSmall = 8.dp
    val paddingNormal = 16.dp
    val paddingLarge = 24.dp
    val paddingExtraLarge = 32.dp
    val fabClearance = 88.dp
}

/** Pill corners for prominent single-line inputs, as the app's `PlusTextFieldDefaults`. */
val PillShape = RoundedCornerShape(28.dp)

@Composable
fun AdminTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkColorPalette else LightColorPalette,
        shapes = AdminShapes,
        content = content,
    )
}

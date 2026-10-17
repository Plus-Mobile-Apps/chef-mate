package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

private val StepperButtonSize = 48.dp

// The material "remove" glyph lives in the extended icon artifact, which nothing else in the app
// pulls in — a single horizontal bar isn't worth that dependency, so draw it here. The fill color
// is a placeholder; Icon tints the whole vector at draw time.
private val MinusIcon: ImageVector =
    ImageVector.Builder(
            name = "Minus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        .apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 13f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(19f)
                close()
            }
        }
        .build()

/**
 * A text field flanked by decrement and increment buttons — minus on the left, plus on the right.
 *
 * The field stays fully editable, so a value the buttons can't express (a unit, a fraction, a note
 * like "a pinch") can still be typed. The buttons are a shortcut for the common ±1 case, not the
 * only way in, which is why stepping is the caller's business: this component just reports the taps
 * and renders whatever value comes back.
 */
@Composable
fun PlusStepperTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementContentDescription: String,
    incrementContentDescription: String,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlusIconButton(
            icon = MinusIcon,
            contentDescription = decrementContentDescription,
            onClick = onDecrement,
            size = StepperButtonSize,
            enabled = enabled && decrementEnabled,
        )
        PlusTextField(
            modifier = Modifier.weight(1f),
            value = value,
            onValueChange = onValueChange,
            label = label,
            placeholder = placeholder,
            enabled = enabled,
            singleLine = true,
            keyboardOptions = keyboardOptions,
        )
        PlusIconButton(
            icon = Icons.Default.Add,
            contentDescription = incrementContentDescription,
            onClick = onIncrement,
            size = StepperButtonSize,
            enabled = enabled && incrementEnabled,
        )
    }
}

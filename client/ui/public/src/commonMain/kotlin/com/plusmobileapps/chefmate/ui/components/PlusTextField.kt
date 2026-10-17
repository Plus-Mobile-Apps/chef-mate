package com.plusmobileapps.chefmate.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

object PlusTextFieldDefaults {
    /**
     * Pill-shaped corners for prominent single-line inputs (browser search bar, recipe search,
     * grocery quick-add). Half the 56dp outlined field height, so the ends are fully rounded.
     */
    val RoundedShape: Shape = RoundedCornerShape(28.dp)
}

@Composable
fun PlusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    error: TextData? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    focusRequester: FocusRequester? = null,
    outputTransformation: OutputTransformation? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentType: ContentType? = null,
    shape: Shape = PlusTextFieldDefaults.RoundedShape,
) {
    val isError = error != null

    // Back the field with a TextFieldState so the field itself owns its text *and* its
    // cursor/selection. This is the pairing Compose Multiplatform's native iOS text input expects:
    // the native UIView and Compose share a single buffer, so the caret can't desync from the text.
    //
    // The previous value/onValueChange + local-TextFieldValue bridge kept the *logical* selection
    // correct (text still inserted in order) but the native iOS input rendered its caret at the
    // start of the field, because that experimental input path doesn't honor Compose-driven
    // selection pushed through the TextFieldValue overload. TextFieldState removes that round-trip
    // entirely.
    val state = rememberTextFieldState(initialText = value)

    // Long-lived collectors below must always see the latest hoisted value/callback without
    // restarting, so read them through rememberUpdatedState.
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    // Push local edits out to the caller. The guard means the value catching up to what the user
    // just typed (the Bloc round-trip echo) never re-notifies or loops.
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }
            .collect { text -> if (text != currentValue) currentOnValueChange(text) }
    }

    // Adopt genuine upstream changes (clearing the query, a form reset, autofill) without moving
    // the cursor on ordinary keystroke round-trips.
    LaunchedEffect(value) {
        if (value != state.text.toString()) {
            state.setTextAndPlaceCursorAtEnd(value)
        }
    }

    // Capture into a local so the null-check smart-casts inside the semantics lambda. The autofill
    // hint must land on the text field node itself (not the outer Column) so the platform autofill
    // service can identify the field and offer to save/fill credentials for it.
    val autofillContentType = contentType
    val fieldModifier =
        Modifier.fillMaxWidth()
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .let {
                if (autofillContentType != null) {
                    it.semantics { this.contentType = autofillContentType }
                } else {
                    it
                }
            }

    val lineLimits =
        if (singleLine) {
            TextFieldLineLimits.SingleLine
        } else {
            TextFieldLineLimits.MultiLine(minHeightInLines = minLines, maxHeightInLines = maxLines)
        }

    Column(modifier = modifier) {
        OutlinedTextField(
            state = state,
            modifier = fieldModifier,
            enabled = enabled,
            readOnly = readOnly,
            // The state-based overload's label carries a TextFieldLabelScope receiver; adapt the
            // plain slot callers pass so PlusTextField's public API stays receiver-free.
            label = label?.let { slot -> { slot() } },
            placeholder = placeholder,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            isError = isError,
            lineLimits = lineLimits,
            outputTransformation = outputTransformation,
            keyboardOptions = keyboardOptions.withNativeTextInput(),
            onKeyboardAction = keyboardActions.toKeyboardActionHandler(keyboardOptions.imeAction),
            shape = shape,
        )
        AnimatedVisibility(visible = isError) {
            error?.let {
                Text(
                    text = it.localized(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier =
                        Modifier.padding(
                            start = ChefMateTheme.dimens.paddingNormal,
                            top = ChefMateTheme.dimens.paddingSmall,
                        ),
                )
            }
        }
    }
}

/**
 * Bridges the legacy [KeyboardActions] callbacks onto the [androidx.compose.foundation.text.input]
 * API's single [KeyboardActionHandler], dispatching to the lambda that matches the field's
 * [imeAction]. Returns null when no matching action is set, so the platform default runs.
 */
private fun KeyboardActions.toKeyboardActionHandler(imeAction: ImeAction): KeyboardActionHandler? {
    val action: (KeyboardActionScope.() -> Unit) =
        when (imeAction) {
            ImeAction.Done -> onDone
            ImeAction.Go -> onGo
            ImeAction.Next -> onNext
            ImeAction.Previous -> onPrevious
            ImeAction.Search -> onSearch
            ImeAction.Send -> onSend
            else -> null
        } ?: return null
    return KeyboardActionHandler { performDefault ->
        val scope =
            object : KeyboardActionScope {
                override fun defaultKeyboardAction(imeAction: ImeAction) = performDefault()
            }
        scope.action()
    }
}

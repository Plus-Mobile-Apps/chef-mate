package com.plusmobileapps.chefmate.family.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import chefmate.client.family.manage.public.generated.resources.Res
import chefmate.client.family.manage.public.generated.resources.family_cancel
import chefmate.client.family.manage.public.generated.resources.family_name_label
import chefmate.client.family.manage.public.generated.resources.family_role_admin
import chefmate.client.family.manage.public.generated.resources.family_role_member
import chefmate.client.family.manage.public.generated.resources.family_role_owner
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusButtonVariant
import com.plusmobileapps.chefmate.ui.components.PlusDialogScaffold
import com.plusmobileapps.chefmate.ui.components.PlusTextField
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

/** The localized label for a family role. */
fun FamilyRole.label(): TextData =
    ResourceString(
        when (this) {
            FamilyRole.OWNER -> Res.string.family_role_owner
            FamilyRole.ADMIN -> Res.string.family_role_admin
            FamilyRole.MEMBER -> Res.string.family_role_member
        }
    )

/**
 * A dialog with a single family-name field, shared by "New family" and "Rename family". The name is
 * hoisted into the BLoC so it survives configuration changes.
 */
@Composable
internal fun FamilyNameDialog(
    title: TextData,
    confirmText: TextData,
    name: String,
    canConfirm: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean = false,
    error: TextData? = null,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    PlusDialogScaffold(
        onDismissRequest = onDismiss,
        header = { Text(title.localized()) },
        content = {
            PlusTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(Res.string.family_name_label)) },
                singleLine = true,
                error = error,
                keyboardOptions =
                    KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions = KeyboardActions(onDone = { if (canConfirm) onConfirm() }),
                focusRequester = focusRequester,
                modifier = Modifier.fillMaxWidth().testTag(FamilyListTestTags.CREATE_NAME_FIELD),
            )
        },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingNormal)) {
                PlusButton(
                    text = ResourceString(Res.string.family_cancel),
                    variant = PlusButtonVariant.SECONDARY,
                    onClick = onDismiss,
                )
                PlusButton(
                    text = confirmText,
                    enabled = canConfirm,
                    isLoading = isSaving,
                    onClick = onConfirm,
                )
            }
        },
    )
}

package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chefmate.client.ui.public.generated.resources.Res
import chefmate.client.ui.public.generated.resources.okay
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

@Composable
fun PlusDialogScaffold(
    header: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
    footer: @Composable () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        title = header,
        text = content,
        confirmButton = footer,
    )
}

@Composable
fun PlusDialog(
    title: TextData,
    message: TextData? = null,
    confirmButtonText: TextData = ResourceString(Res.string.okay),
    dismissButtonText: TextData? = null,
    onConfirmClick: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PlusDialogScaffold(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        header = { Text(title.localized()) },
        content = message?.let { { Text(it.localized()) } },
        footer = {
            Row(horizontalArrangement = spacedBy(ChefMateTheme.dimens.paddingNormal)) {
                if (dismissButtonText != null) {
                    PlusButton(
                        text = dismissButtonText,
                        variant = PlusButtonVariant.SECONDARY,
                        onClick = onDismissRequest,
                    )
                }
                PlusButton(text = confirmButtonText, onClick = onConfirmClick)
            }
        },
    )
}

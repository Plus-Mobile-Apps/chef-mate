@file:OptIn(ExperimentalMaterial3Api::class)

package com.plusmobileapps.chefmate.recipe.bottomnav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chefmate.client.bottomnav.public.generated.resources.Res
import chefmate.client.bottomnav.public.generated.resources.swiftui_redesign_prompt_accept
import chefmate.client.bottomnav.public.generated.resources.swiftui_redesign_prompt_body
import chefmate.client.bottomnav.public.generated.resources.swiftui_redesign_prompt_dismiss
import chefmate.client.bottomnav.public.generated.resources.swiftui_redesign_prompt_title
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusButtonVariant
import com.plusmobileapps.chefmate.ui.components.PlusResponsiveModal
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

object SwiftUiRedesignPromptTestTags {
    const val SHEET = "swiftui_redesign_prompt_sheet"
    const val ACCEPT = "swiftui_redesign_prompt_accept"
    const val DISMISS = "swiftui_redesign_prompt_dismiss"
}

/**
 * One-time sheet on the home screen offering the native SwiftUI redesign (iOS only). Any way out
 * other than [onAccept] — "Not now", swiping down, tapping the scrim — goes to [onDismiss].
 */
@Composable
fun SwiftUiRedesignPromptSheet(onAccept: () -> Unit, onDismiss: () -> Unit) {
    PlusResponsiveModal(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag(SwiftUiRedesignPromptTestTags.SHEET),
    ) {
        SwiftUiRedesignPromptContent(onAccept = onAccept, onDismiss = onDismiss)
    }
}

/**
 * The sheet's body without the modal chrome — public so screenshot tests can render it, since
 * `ModalBottomSheet` doesn't snapshot reliably.
 */
@Composable
fun SwiftUiRedesignPromptContent(
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = ChefMateTheme.dimens
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = dimens.paddingLarge)
                .padding(bottom = dimens.paddingNormal),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.paddingNormal),
    ) {
        Box(
            modifier =
                Modifier.size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = stringResource(Res.string.swiftui_redesign_prompt_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.swiftui_redesign_prompt_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = dimens.paddingSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall),
        ) {
            PlusButton(
                text = Res.string.swiftui_redesign_prompt_accept.asTextData(),
                modifier = Modifier.fillMaxWidth().testTag(SwiftUiRedesignPromptTestTags.ACCEPT),
                onClick = onAccept,
            )
            PlusButton(
                text = Res.string.swiftui_redesign_prompt_dismiss.asTextData(),
                variant = PlusButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth().testTag(SwiftUiRedesignPromptTestTags.DISMISS),
                onClick = onDismiss,
            )
        }
    }
}

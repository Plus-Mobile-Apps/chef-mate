package com.plusmobileapps.chefmate.subscription.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import chefmate.client.subscription.public.generated.resources.Res
import chefmate.client.subscription.public.generated.resources.premium_locked_badge
import chefmate.client.subscription.public.generated.resources.premium_required_confirm
import chefmate.client.subscription.public.generated.resources.premium_required_message
import chefmate.client.subscription.public.generated.resources.premium_required_title
import com.plusmobileapps.chefmate.subscription.SubscriptionTestTags
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Size of the inline lock glyph — deliberately smaller than a full 24dp icon so it reads as a badge
 * next to a row label rather than an action of its own.
 */
private val LOCK_BADGE_SIZE = 16.dp

/** Matches the Material default icon-button glyph size the toolbars use. */
private val TOOLBAR_ICON_SIZE = 24.dp
private val TOOLBAR_LOCK_BADGE_SIZE = 13.dp
private val TOOLBAR_LOCK_BADGE_INSET = 1.dp

/**
 * Shown when a non-subscriber taps a premium entry point. There is no paywall to route to yet, so
 * confirming just dismisses.
 *
 * TODO: once the paywall screen exists, give this a "Subscribe" confirm button that routes to it
 *   and keep [onDismiss] as the cancel path.
 */
@Composable
fun PremiumRequiredDialog(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    // PlusDialog renders into a dialog window, so the tag has to ride on content inside it for the
    // semantics tree to pick it up from the screen's root.
    PlusDialog(
        title = Res.string.premium_required_title.asTextData(),
        message = Res.string.premium_required_message.asTextData(),
        confirmButtonText = Res.string.premium_required_confirm.asTextData(),
        onConfirmClick = onDismiss,
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(SubscriptionTestTags.PREMIUM_REQUIRED_DIALOG),
    )
}

/**
 * A toolbar icon marked subscriber-only: [imageVector] with a small lock badge over its
 * bottom-trailing corner.
 *
 * A badge rather than a dimmed tint because in the toolbars this is used in, `LocalContentColor`
 * already resolves to the muted on-surface color — tinting produced a pixel-identical icon, so the
 * lock state was invisible. The badge sits on a background-colored disc so it stays legible against
 * whatever the glyph underneath is doing.
 */
@Composable
fun PremiumLockedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(TOOLBAR_ICON_SIZE)) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
        )
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = ChefMateTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier.align(Alignment.BottomEnd)
                    .size(TOOLBAR_LOCK_BADGE_SIZE)
                    .background(
                        color = ChefMateTheme.colorScheme.background,
                        shape = CircleShape,
                    )
                    .padding(TOOLBAR_LOCK_BADGE_INSET),
        )
    }
}

/**
 * Trailing "Premium" affordance for a settings-style row whose action is subscriber-only. Signals
 * the row is still tappable — tapping opens [PremiumRequiredDialog] rather than doing nothing.
 */
@Composable
fun PremiumLockBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.testTag(SubscriptionTestTags.PREMIUM_LOCK_BADGE),
        horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = ChefMateTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(LOCK_BADGE_SIZE),
        )
        Text(
            text = stringResource(Res.string.premium_locked_badge),
            style = ChefMateTheme.typography.labelMedium,
            color = ChefMateTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.plusmobileapps.chefmate.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Admin counterparts of the app's shared `Plus*` components (client/ui/public/.../components), so
// the dashboard reads as the same product. See AdminTheme.kt for why they're copies.

private const val DISABLED_ALPHA = 0.38f

/**
 * Tonal rounded-square icon button, as the app's `PlusIconButton(ImageVector)`. The label doubles
 * as a hover tooltip, which matters on desktop where icons alone are ambiguous.
 */
@Composable
fun AdminIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    iconTint: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    enabled: Boolean = true,
) {
    TooltipBox(
        positionProvider =
            TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState(),
    ) {
        Box(
            modifier =
                modifier
                    .size(size)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        if (enabled) containerColor
                        else containerColor.copy(alpha = DISABLED_ALPHA),
                    )
                    .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) iconTint else iconTint.copy(alpha = DISABLED_ALPHA),
                modifier = Modifier.padding(AdminDimens.paddingSmall),
            )
        }
    }
}

/** Transparent variant for low-emphasis row actions (archive, delete). */
@Composable
fun AdminPlainIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    AdminIconButton(
        icon = icon,
        label = label,
        onClick = onClick,
        size = 40.dp,
        containerColor = Color.Transparent,
        iconTint = tint,
    )
}

enum class AdminButtonVariant {
    PRIMARY,
    SECONDARY,
    DESTRUCTIVE,
}

/** As the app's `PlusButton`: filled primary, tonal secondary, or red destructive. */
@Composable
fun AdminButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: AdminButtonVariant = AdminButtonVariant.PRIMARY,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading,
        colors =
            when (variant) {
                AdminButtonVariant.PRIMARY ->
                    ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary,
                    )
                AdminButtonVariant.SECONDARY ->
                    ButtonDefaults.buttonColors(
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer,
                    )
                AdminButtonVariant.DESTRUCTIVE ->
                    ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError,
                    )
            },
    ) {
        if (isLoading) {
            AdminLoadingIndicator(Modifier.size(18.dp), color = LocalContentColor.current)
        } else {
            Text(text)
        }
    }
}

/** Extended FAB with the app's extra-large corners, as `PlusFloatingActionButton`. */
@Composable
fun AdminFloatingActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    ExtendedFloatingActionButton(
        text = { Text(text) },
        icon = { icon?.let { Icon(it, contentDescription = null) } },
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier,
    )
}

/** The app's wavy expressive spinner (`PlusLoadingIndicator`). */
@Composable
fun AdminLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    CircularWavyProgressIndicator(modifier = modifier, color = color)
}

/**
 * A rounded-outline group of related fields with a section title, as the app's
 * `PlusOutlinedContainer` (the recipe editor's "More details" box).
 */
@Composable
fun AdminOutlinedSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.medium,
                )
                .padding(AdminDimens.paddingNormal),
        verticalArrangement = Arrangement.spacedBy(AdminDimens.paddingNormal),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** Section label used above a group of controls (the recipe editor's "Rating", "Categories"). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

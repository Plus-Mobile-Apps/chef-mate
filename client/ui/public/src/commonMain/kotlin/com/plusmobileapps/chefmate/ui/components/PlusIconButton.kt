package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

/** Container/icon opacity for a button that is present but not currently actionable. */
private const val DISABLED_ALPHA = 0.38f

@Composable
fun PlusIconButton(
    icon: Painter,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
) {
    val bgColor = backgroundColor ?: ChefMateTheme.colorScheme.primaryContainer
    val tintColor = iconTint ?: ChefMateTheme.colorScheme.onPrimaryContainer

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(if (enabled) bgColor else bgColor.copy(alpha = DISABLED_ALPHA))
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.padding(ChefMateTheme.dimens.paddingSmall),
            painter = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tintColor else tintColor.copy(alpha = DISABLED_ALPHA),
        )
    }
}

@Composable
fun PlusIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    backgroundColor: Color? = null,
    iconTint: Color? = null,
    enabled: Boolean = true,
) {
    val bgColor = backgroundColor ?: ChefMateTheme.colorScheme.secondaryContainer
    val tintColor = iconTint ?: ChefMateTheme.colorScheme.onSecondaryContainer

    Box(
        modifier =
            modifier
                .size(size)
                .clip(ChefMateTheme.shapes.large)
                .background(if (enabled) bgColor else bgColor.copy(alpha = DISABLED_ALPHA))
                .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.padding(ChefMateTheme.dimens.paddingSmall),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tintColor else tintColor.copy(alpha = DISABLED_ALPHA),
        )
    }
}

@Preview
@Composable
fun PlusIconButtonPreview() {
    PlusIconButton(
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = "Close",
        onClick = {},
    )
}

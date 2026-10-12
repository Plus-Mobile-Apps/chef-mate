package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private val DefaultAvatarSize = 40.dp

/**
 * A circular user avatar. Renders [imageUrl] when present; otherwise falls back to the first letter
 * of [fallbackText] on a tonal background, or a generic person glyph when [fallbackText] is blank.
 *
 * Avatars for other users are only available once their profile photo is reachable — for now only
 * the current user's own [imageUrl] is populated, so collaborators show the lettered fallback.
 */
@Composable
fun PlusAvatar(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackText: String = "",
    size: Dp = DefaultAvatarSize,
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
) {
    val avatarModifier = modifier.size(size).clip(shape)
    if (imageUrl != null) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = avatarModifier,
            contentScale = ContentScale.Crop,
        )
    } else {
        val initial = fallbackText.trim().firstOrNull()?.uppercaseChar()
        Box(
            modifier = avatarModifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (initial != null) {
                Text(
                    text = initial.toString(),
                    // Scale the initial with the avatar so it stays proportional at any size; at
                    // the default 40dp this is exactly titleMedium.
                    style =
                        MaterialTheme.typography.titleMedium.let {
                            it.copy(fontSize = it.fontSize * (size / DefaultAvatarSize))
                        },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(size / 2),
                )
            }
        }
    }
}

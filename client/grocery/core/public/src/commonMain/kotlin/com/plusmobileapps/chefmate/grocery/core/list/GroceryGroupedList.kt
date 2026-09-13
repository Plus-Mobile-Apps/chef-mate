package com.plusmobileapps.chefmate.grocery.core.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import chefmate.client.grocery.core.public.generated.resources.Res
import chefmate.client.grocery.core.public.generated.resources.grocery_checked
import chefmate.client.grocery.core.public.generated.resources.grocery_delete_item
import chefmate.client.grocery.core.public.generated.resources.grocery_not_checked
import chefmate.client.grocery.core.public.generated.resources.grocery_recipe_source
import com.plusmobileapps.chefmate.Platform
import com.plusmobileapps.chefmate.currentPlatform
import com.plusmobileapps.chefmate.grocery.core.displayName
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.ui.text.toInlineMarkdownAnnotatedString
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

data class GroceryDisplayItem(
    val key: Any,
    val displayName: String,
    val quantity: String? = null,
    val isChecked: Boolean = false,
    val recipeName: String? = null,
)

data class GroceryDisplayGroup(val category: GroceryCategory, val items: List<GroceryDisplayItem>)

/** Peak opacity of the brief highlight shown on a freshly added grocery item. */
private const val HIGHLIGHT_PEAK_ALPHA = 0.5f

/**
 * Default for `swipeToDeleteEnabled`. Swipe-to-delete is a touch idiom, so it's offered on
 * phones/tablets only. On desktop the row's trailing delete button is the way to remove an item,
 * and a mouse drag across a row would be an easy way to lose one by accident.
 */
internal val supportsSwipeToDelete: Boolean
    get() = currentPlatform == Platform.ANDROID || currentPlatform == Platform.IOS

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroceryGroupedList(
    groups: List<GroceryDisplayGroup>,
    onItemClick: (Any) -> Unit,
    onCheckedChange: (Any) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    showHeaders: Boolean = true,
    highlightedKey: Any? = null,
    trailingContent: (@Composable (GroceryDisplayItem) -> Unit)? = null,
    onSwipeToDelete: ((GroceryDisplayItem) -> Unit)? = null,
    swipeToDeleteEnabled: Boolean = supportsSwipeToDelete,
    footer: (LazyListScope.() -> Unit)? = null,
) {
    LazyColumn(state = state, modifier = modifier.fillMaxSize()) {
        groups.forEach { group ->
            if (showHeaders) {
                stickyHeader(key = "header_${group.category.name}") {
                    CategoryHeader(category = group.category)
                }
            }
            items(group.items.size, key = { group.items[it].key }) { index ->
                val item = group.items[index]
                GroceryDisplayListItem(
                    item = item,
                    onCheckedChange = { onCheckedChange(item.key) },
                    onClick = { onItemClick(item.key) },
                    trailingContent = trailingContent,
                    highlighted = item.key == highlightedKey,
                    onSwipeToDelete =
                        onSwipeToDelete
                            ?.takeIf { swipeToDeleteEnabled }
                            ?.let { delete -> { delete(item) } },
                    modifier = Modifier.animateItem(),
                )
                HorizontalDivider()
            }
        }
        footer?.invoke(this)
    }
}

@Composable
internal fun CategoryHeader(category: GroceryCategory, modifier: Modifier = Modifier) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier.fillMaxWidth()) {
        Text(
            text = category.displayName().localized(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun GroceryDisplayListItem(
    item: GroceryDisplayItem,
    onCheckedChange: () -> Unit,
    onClick: () -> Unit,
    trailingContent: (@Composable (GroceryDisplayItem) -> Unit)?,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onSwipeToDelete: (() -> Unit)? = null,
) {
    if (onSwipeToDelete != null) {
        val dismissState = rememberSwipeToDismissBoxState()
        SwipeToDismissBox(
            state = dismissState,
            modifier = modifier,
            // Start-to-end is left alone so the swipe doesn't fight the Android system back
            // gesture, which starts at the leading edge. Delete is a swipe towards the trailing
            // edge only.
            enableDismissFromStartToEnd = false,
            onDismiss = { onSwipeToDelete() },
            backgroundContent = { DeleteSwipeBackground() },
        ) {
            GroceryItemRow(
                item = item,
                onCheckedChange = onCheckedChange,
                onClick = onClick,
                trailingContent = trailingContent,
                highlighted = highlighted,
                // The row is normally transparent over the screen background; it needs to be
                // opaque here so the red delete background stays hidden until it's swiped aside.
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            )
        }
    } else {
        GroceryItemRow(
            item = item,
            onCheckedChange = onCheckedChange,
            onClick = onClick,
            trailingContent = trailingContent,
            highlighted = highlighted,
            modifier = modifier,
        )
    }
}

/** Revealed behind a grocery row as it's swiped away, signalling what the gesture will do. */
@Composable
private fun DeleteSwipeBackground() {
    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = ChefMateTheme.dimens.paddingNormal),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(Res.string.grocery_delete_item),
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun GroceryItemRow(
    item: GroceryDisplayItem,
    onCheckedChange: () -> Unit,
    onClick: () -> Unit,
    trailingContent: (@Composable (GroceryDisplayItem) -> Unit)?,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    // Briefly tint the row when it's the freshly added item, then fade back to transparent so the
    // user can see where it landed in the list.
    val highlightColor = MaterialTheme.colorScheme.primaryContainer
    val highlightAlpha = remember { Animatable(0f) }
    LaunchedEffect(highlighted) {
        if (highlighted) {
            highlightAlpha.snapTo(HIGHLIGHT_PEAK_ALPHA)
            highlightAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 1200, delayMillis = 400),
            )
        }
    }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(highlightColor.copy(alpha = highlightAlpha.value))
                .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCheckedChange) {
            if (item.isChecked) {
                Icon(
                    Icons.Default.CheckBox,
                    contentDescription = stringResource(Res.string.grocery_checked),
                )
            } else {
                Icon(
                    Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = stringResource(Res.string.grocery_not_checked),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            Text(
                text = item.displayName.toInlineMarkdownAnnotatedString(),
                style = MaterialTheme.typography.bodyLarge,
            )
            val quantity = item.quantity
            if (quantity != null) {
                Text(
                    text = quantity,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val recipeName = item.recipeName
            if (recipeName != null) {
                Text(
                    text =
                        PhraseModel(
                                Res.string.grocery_recipe_source,
                                "recipe" to FixedString(recipeName),
                            )
                            .localized(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
        if (trailingContent != null) {
            trailingContent(item)
        }
    }
}

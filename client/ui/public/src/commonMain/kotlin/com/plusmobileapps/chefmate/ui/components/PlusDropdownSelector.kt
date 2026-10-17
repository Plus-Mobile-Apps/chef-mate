package com.plusmobileapps.chefmate.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme

/**
 * Outlined pill that names the current selection (e.g. the active recipe book or grocery list) and
 * opens a picker when tapped. The picker adapts to [windowSizeClass]: a [DropdownMenu] anchored to
 * the pill on tablet/desktop widths, and a [ModalBottomSheet] on phone widths. [menuContent] is
 * shared between both presentations.
 *
 * [windowSizeClass] is passed in rather than measured here because the pill usually sits in a top
 * bar slot, whose constraints say nothing about the window width.
 *
 * ModalBottomSheet doesn't render reliably under the Compose screenshot test plugin, so snapshot
 * the picker by rendering [menuContent] directly.
 *
 * @param text the current selection.
 * @param supportingText optional muted text after [text], e.g. an item count.
 * @param expanded whether the picker is showing; the caller owns this state.
 * @param iconContentDescription a11y label for the dropdown arrow.
 * @param modifier applied to the outlined pill itself (test tags and semantics go here).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlusDropdownSelector(
    text: String,
    expanded: Boolean,
    windowSizeClass: WindowSizeClass,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    iconContentDescription: String? = null,
    menuContent: @Composable ColumnScope.() -> Unit,
) {
    Box {
        Row(
            modifier =
                modifier
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onClick)
                    .padding(
                        start = ChefMateTheme.dimens.paddingNormal,
                        end = ChefMateTheme.dimens.paddingSmall,
                        top = ChefMateTheme.dimens.paddingExtraSmall,
                        bottom = ChefMateTheme.dimens.paddingExtraSmall,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(start = ChefMateTheme.dimens.paddingSmall),
                )
            }
            Icon(
                imageVector =
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = iconContentDescription,
            )
        }
        if (windowSizeClass == WindowSizeClass.COMPACT) {
            if (expanded) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
                    Column(modifier = Modifier.navigationBarsPadding(), content = menuContent)
                }
            }
        } else {
            DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, content = menuContent)
        }
    }
}

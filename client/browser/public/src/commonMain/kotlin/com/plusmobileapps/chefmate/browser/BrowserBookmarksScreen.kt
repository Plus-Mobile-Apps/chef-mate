package com.plusmobileapps.chefmate.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Deselect
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import chefmate.client.browser.public.generated.resources.Res
import chefmate.client.browser.public.generated.resources.browser_bookmarks_cancel
import chefmate.client.browser.public.generated.resources.browser_bookmarks_delete
import chefmate.client.browser.public.generated.resources.browser_bookmarks_delete_message_one
import chefmate.client.browser.public.generated.resources.browser_bookmarks_delete_message_other
import chefmate.client.browser.public.generated.resources.browser_bookmarks_delete_selected_a11y
import chefmate.client.browser.public.generated.resources.browser_bookmarks_delete_title
import chefmate.client.browser.public.generated.resources.browser_bookmarks_deselect_all_a11y
import chefmate.client.browser.public.generated.resources.browser_bookmarks_empty
import chefmate.client.browser.public.generated.resources.browser_bookmarks_select_all_a11y
import chefmate.client.browser.public.generated.resources.browser_bookmarks_select_mode_a11y
import chefmate.client.browser.public.generated.resources.browser_bookmarks_selection_count
import chefmate.client.browser.public.generated.resources.browser_bookmarks_selection_empty_title
import chefmate.client.browser.public.generated.resources.browser_bookmarks_title
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainerDefaults
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun BrowserBookmarksScreen(bloc: BrowserBookmarksBloc, modifier: Modifier = Modifier) {
    val model by bloc.state.collectAsState()

    val headerData =
        if (model.selectionMode) {
            PlusHeaderData.Modal(
                title =
                    if (model.selectedCount == 0)
                        Res.string.browser_bookmarks_selection_empty_title.asTextData()
                    else
                        PhraseModel(
                            Res.string.browser_bookmarks_selection_count,
                            "count" to FixedString(model.selectedCount.toString()),
                        ),
                onCloseClick = bloc::onCancelSelection,
                trailingAccessory =
                    PlusHeaderData.TrailingAccessory.Custom {
                        SelectionActions(
                            allSelected = model.allSelected,
                            hasSelection = model.selectedCount > 0,
                            onSelectAll = bloc::onSelectAllClicked,
                            onDelete = bloc::onDeleteSelectedClicked,
                        )
                    },
            )
        } else {
            PlusHeaderData.Child(
                title = Res.string.browser_bookmarks_title.asTextData(),
                onBackClick = bloc::onBackClicked,
                // Nothing to select in an empty list, so don't offer the mode.
                trailingAccessory =
                    if (model.bookmarks.isEmpty()) null
                    else
                        PlusHeaderData.TrailingAccessory.Icon(
                            icon = Icons.Default.Checklist,
                            contentDesc =
                                Res.string.browser_bookmarks_select_mode_a11y.asTextData(),
                            onClick = bloc::onSelectModeClicked,
                        ),
            )
        }

    PlusHeaderContainer(
        modifier = modifier.testTag(BrowserTestTags.BOOKMARKS_SCREEN),
        data = headerData,
        // The LazyColumn scrolls itself; an outer scroll would hand it infinite height.
        scrollEnabled = false,
        maxContentWidth = Dp.Unspecified,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            if (!model.isLoading && model.bookmarks.isEmpty()) {
                EmptyBookmarksHint()
            } else {
                BookmarkList(
                    bookmarks = model.bookmarks,
                    selectedIds = model.selectedIds,
                    selectionMode = model.selectionMode,
                    onClick = bloc::onBookmarkClicked,
                    onLongClick = bloc::onBookmarkLongClicked,
                )
            }
        },
    )

    val pendingDeleteCount = model.pendingDeleteCount
    if (pendingDeleteCount != null) {
        PlusDialog(
            title = ResourceString(Res.string.browser_bookmarks_delete_title),
            message =
                if (pendingDeleteCount == 1)
                    ResourceString(Res.string.browser_bookmarks_delete_message_one)
                else
                    PhraseModel(
                        Res.string.browser_bookmarks_delete_message_other,
                        "count" to FixedString(pendingDeleteCount.toString()),
                    ),
            confirmButtonText = ResourceString(Res.string.browser_bookmarks_delete),
            dismissButtonText = ResourceString(Res.string.browser_bookmarks_cancel),
            onConfirmClick = bloc::onDeleteSelectedConfirmed,
            onDismissRequest = bloc::onDeleteSelectedDismissed,
        )
    }
}

@Composable
private fun SelectionActions(
    allSelected: Boolean,
    hasSelection: Boolean,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
) {
    IconButton(
        onClick = onSelectAll,
        modifier = Modifier.testTag(BrowserTestTags.BOOKMARKS_SELECT_ALL),
    ) {
        Icon(
            imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
            contentDescription =
                stringResource(
                    if (allSelected) Res.string.browser_bookmarks_deselect_all_a11y
                    else Res.string.browser_bookmarks_select_all_a11y
                ),
            tint = ChefMateTheme.colorScheme.onBackground,
        )
    }
    // Hide the trash until something is selected — a dead-looking icon is more confusing than none.
    if (hasSelection) {
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag(BrowserTestTags.BOOKMARKS_DELETE_SELECTED),
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription =
                    stringResource(Res.string.browser_bookmarks_delete_selected_a11y),
                tint = ChefMateTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun BookmarkList(
    bookmarks: List<BrowserBookmark>,
    selectedIds: Set<Long>,
    selectionMode: Boolean,
    onClick: (BrowserBookmark) -> Unit,
    onLongClick: (BrowserBookmark) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items(bookmarks, key = { it.id }) { bookmark ->
            BookmarkRow(
                bookmark = bookmark,
                selected = bookmark.id in selectedIds,
                selectionMode = selectionMode,
                onClick = { onClick(bookmark) },
                onLongClick = { onLongClick(bookmark) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkRow(
    bookmark: BrowserBookmark,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val host = remember(bookmark.url) { bookmark.url.displayHost() }
    Row(
        modifier =
            // widthIn must precede fillMaxWidth, which would otherwise pin min = max = parent.
            Modifier.widthIn(max = PlusHeaderContainerDefaults.MaxContentWidth)
                .fillMaxWidth()
                .heightIn(min = ChefMateTheme.dimens.rowHeight)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .testTag(BrowserTestTags.bookmarkRow(bookmark.id))
                .padding(
                    horizontal = ChefMateTheme.dimens.paddingNormal,
                    vertical = ChefMateTheme.dimens.paddingSmall,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Checkbox(checked = selected, onCheckedChange = { onClick() })
        } else {
            BookmarkMonogram(host = host)
        }
        Spacer(Modifier.size(ChefMateTheme.dimens.paddingNormal))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = bookmark.url,
                style = MaterialTheme.typography.bodySmall,
                color = ChefMateTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmptyBookmarksHint() {
    Box(
        modifier =
            Modifier.widthIn(max = PlusHeaderContainerDefaults.MaxContentWidth)
                .fillMaxWidth()
                .padding(ChefMateTheme.dimens.paddingNormal)
    ) {
        Text(
            text = stringResource(Res.string.browser_bookmarks_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = ChefMateTheme.colorScheme.onSurfaceVariant,
        )
    }
}

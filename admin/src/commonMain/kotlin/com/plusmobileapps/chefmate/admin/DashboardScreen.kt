package com.plusmobileapps.chefmate.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(repo: FeatureFlagAdminRepository, onSignOut: suspend () -> Unit) {
    val widthClass = LocalWidthClass.current
    val scope = rememberCoroutineScope()
    var flags by remember { mutableStateOf<List<AdminFeatureFlag>>(emptyList()) }
    var showArchived by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<FlagDraft?>(null) }
    var pendingDelete by remember { mutableStateOf<AdminFeatureFlag?>(null) }
    val searchFocus = remember { FocusRequester() }

    suspend fun reload() {
        loading = true
        error = null
        runCatching { repo.list(includeArchived = showArchived) }
            .onSuccess { flags = it }
            .onFailure { error = it.message ?: "Failed to load flags" }
        loading = false
    }

    LaunchedEffect(showArchived) { reload() }

    // On desktop, start with the search field focused so typing filters immediately and the
    // Ctrl/⌘ F shortcut has a focused element to receive it.
    LaunchedEffect(Unit) { if (widthClass == WidthClass.Expanded) searchFocus.requestFocus() }

    fun save(draft: FlagDraft, flag: AdminFeatureFlag) {
        scope.launch {
            val result =
                if (draft.isNew) runCatching { repo.create(flag) }
                else runCatching { repo.update(flag) }
            result
                .onSuccess {
                    // Keep the saved flag open on desktop so follow-up tweaks don't need a
                    // re-click.
                    editing = if (widthClass == WidthClass.Expanded) FlagDraft.from(flag) else null
                    reload()
                }
                .onFailure { error = it.message ?: "Save failed" }
        }
    }

    val visibleFlags = remember(flags, query) { flags.filter { it.matches(query) } }
    val listDetail = widthClass == WidthClass.Expanded

    // Single-pane layouts swap the whole screen for the editor.
    if (!listDetail) {
        editing?.let { draft ->
            FlagEditorScreen(
                draft = draft,
                onCancel = { editing = null },
                onSave = { save(draft, it) },
            )
            return
        }
    }

    Scaffold(
        modifier =
            Modifier.onPreviewKeyEvent {
                if (it.type == KeyEventType.KeyDown && it.isShortcut(Key.F)) {
                    searchFocus.requestFocus()
                    true
                } else {
                    false
                }
            },
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Feature flags")
                        if (!widthClass.isCompact && !loading) {
                            Text(
                                "  ${flags.size}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AdminDimens.paddingSmall),
                        modifier = Modifier.padding(end = AdminDimens.paddingSmall),
                    ) {
                        FlagHelpAction()
                        AdminIconButton(
                            Icons.Filled.Refresh,
                            "Refresh",
                            onClick = { scope.launch { reload() } },
                        )
                        AdminIconButton(
                            Icons.AutoMirrored.Filled.Logout,
                            "Sign out",
                            onClick = { scope.launch { onSignOut() } },
                        )
                        if (!widthClass.isCompact) {
                            AdminIconButton(
                                Icons.Filled.Add,
                                "New flag",
                                onClick = { editing = FlagDraft() },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (widthClass.isCompact) {
                AdminFloatingActionButton(
                    text = "New flag",
                    icon = Icons.Filled.Add,
                    onClick = { editing = FlagDraft() },
                )
            }
        },
    ) { padding ->
        val list: @Composable (Modifier) -> Unit = { modifier ->
            FlagList(
                flags = visibleFlags,
                totalCount = flags.size,
                query = query,
                onQueryChange = { query = it },
                searchFocus = searchFocus,
                showArchived = showArchived,
                onToggleArchived = { showArchived = !showArchived },
                loading = loading,
                error = error,
                selectedKey = editing?.takeIf { listDetail && !it.isNew }?.key,
                onEdit = { editing = FlagDraft.from(it) },
                onToggleArchive = { flag ->
                    scope.launch {
                        runCatching { repo.setArchived(flag.key, !flag.archived) }
                            .onSuccess {
                                if (editing?.key == flag.key) editing = null
                                reload()
                            }
                            .onFailure { error = it.message ?: "Update failed" }
                    }
                },
                onDelete = { pendingDelete = it },
                bottomPadding =
                    if (widthClass.isCompact) AdminDimens.fabClearance
                    else AdminDimens.paddingLarge,
                modifier = modifier,
            )
        }

        if (listDetail) {
            Row(Modifier.fillMaxSize().padding(padding)) {
                list(Modifier.weight(0.42f).fillMaxHeight())
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Box(Modifier.weight(0.58f).fillMaxHeight()) {
                    val draft = editing
                    if (draft == null) {
                        EmptyDetail(onNew = { editing = FlagDraft() })
                    } else {
                        FlagEditorPane(
                            draft = draft,
                            onCancel = { editing = null },
                            onSave = { save(draft, it) },
                        )
                    }
                }
            }
        } else {
            CenteredContent(maxWidth = 840.dp, modifier = Modifier.padding(padding)) {
                list(Modifier.fillMaxSize())
            }
        }
    }

    pendingDelete?.let { flag ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete “${flag.key}”?") },
            text = { Text("This permanently removes the flag row. This can't be undone.") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(AdminDimens.paddingNormal)) {
                    AdminButton(
                        text = "Cancel",
                        variant = AdminButtonVariant.SECONDARY,
                        onClick = { pendingDelete = null },
                    )
                    AdminButton(
                        text = "Delete",
                        variant = AdminButtonVariant.DESTRUCTIVE,
                        onClick = {
                            val key = flag.key
                            pendingDelete = null
                            if (editing?.key == key) editing = null
                            scope.launch {
                                runCatching { repo.delete(key) }
                                    .onSuccess { reload() }
                                    .onFailure { error = it.message ?: "Delete failed" }
                            }
                        },
                    )
                }
            },
        )
    }
}

private fun AdminFeatureFlag.matches(query: String): Boolean {
    val q = query.trim()
    if (q.isEmpty()) return true
    return key.contains(q, ignoreCase = true) ||
        description.orEmpty().contains(q, ignoreCase = true) ||
        value.contains(q, ignoreCase = true)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagList(
    flags: List<AdminFeatureFlag>,
    totalCount: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    searchFocus: FocusRequester,
    showArchived: Boolean,
    onToggleArchived: () -> Unit,
    loading: Boolean,
    error: String?,
    selectedKey: String?,
    onEdit: (AdminFeatureFlag) -> Unit,
    onToggleArchive: (AdminFeatureFlag) -> Unit,
    onDelete: (AdminFeatureFlag) -> Unit,
    bottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        FlowRow(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(
                        horizontal = AdminDimens.paddingNormal,
                        vertical = AdminDimens.paddingSmall,
                    ),
            horizontalArrangement = Arrangement.spacedBy(AdminDimens.paddingSmall),
            verticalArrangement = Arrangement.spacedBy(AdminDimens.paddingSmall),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search flags") },
                singleLine = true,
                shape = PillShape,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon =
                    if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    } else null,
                modifier =
                    Modifier.weight(1f).focusRequester(searchFocus).onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Escape) {
                            onQueryChange("")
                            true
                        } else false
                    },
            )
            FilterChip(
                selected = showArchived,
                onClick = onToggleArchived,
                label = { Text("Show archived") },
                leadingIcon =
                    if (showArchived) {
                        {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(FilterChipDefaults.IconSize),
                            )
                        }
                    } else null,
            )
        }
        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier.padding(
                        horizontal = AdminDimens.paddingNormal,
                        vertical = AdminDimens.paddingExtraSmall,
                    ),
            )
        }

        when {
            loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AdminLoadingIndicator()
                }
            totalCount == 0 -> EmptyMessage("No flags yet. Use “New flag” to create one.")
            flags.isEmpty() -> EmptyMessage("No flags match “$query”.")
            else ->
                LazyColumn(
                    contentPadding =
                        PaddingValues(
                            start = AdminDimens.paddingSmall,
                            end = AdminDimens.paddingSmall,
                            top = AdminDimens.paddingExtraSmall,
                            bottom = bottomPadding,
                        ),
                    verticalArrangement = Arrangement.spacedBy(AdminDimens.paddingExtraSmall),
                ) {
                    items(flags, key = { it.key }) { flag ->
                        FlagRow(
                            flag = flag,
                            selected = flag.key == selectedKey,
                            onEdit = { onEdit(flag) },
                            onToggleArchive = { onToggleArchive(flag) },
                            onDelete = { onDelete(flag) },
                        )
                    }
                }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(
        text,
        Modifier.padding(AdminDimens.paddingNormal),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * A flat list row in the style of the app's recipe list: no card chrome, generous spacing, and a
 * tonal rounded highlight for the selected row (as the recipe filter sidebar's selected item).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagRow(
    flag: AdminFeatureFlag,
    selected: Boolean,
    onEdit: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(if (selected) colors.secondaryContainer else Color.Transparent)
                .clickable(onClick = onEdit)
                .padding(
                    start = AdminDimens.paddingNormal,
                    end = AdminDimens.paddingExtraSmall,
                    top = AdminDimens.paddingNormal,
                    bottom = AdminDimens.paddingNormal,
                ),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AdminDimens.paddingSmall),
        ) {
            SelectionContainer {
                Text(
                    flag.key,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (flag.archived) colors.onSurfaceVariant else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            flag.description
                ?.takeIf { it.isNotBlank() }
                ?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (flag.enabled) {
                    Pill("Enabled", colors.primary, colors.onPrimary)
                } else {
                    Pill("Disabled", colors.surfaceContainerHighest, colors.onSurfaceVariant)
                }
                if (flag.archived) Pill("Archived", colors.errorContainer, colors.onErrorContainer)
                Pill("${flag.valueType} = ${flag.value}", monospace = true)
                Pill("${flag.rolloutPercent}%")
                flag.platforms?.takeIf { it.isNotEmpty() }?.let { Pill(it.joinToString(", ")) }
                flag.userIds
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { Pill(if (it.size == 1) "1 user" else "${it.size} users") }
                versionRange(flag)?.let { Pill(it) }
            }
        }
        Row {
            AdminPlainIconButton(
                icon = if (flag.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                label = if (flag.archived) "Unarchive" else "Archive",
                onClick = onToggleArchive,
            )
            AdminPlainIconButton(
                icon = Icons.Outlined.Delete,
                label = "Delete",
                onClick = onDelete,
                tint = colors.error,
            )
        }
    }
}

private fun versionRange(flag: AdminFeatureFlag): String? {
    val min = flag.minVersion?.takeIf { it.isNotBlank() }
    val max = flag.maxVersion?.takeIf { it.isNotBlank() }
    return when {
        min != null && max != null -> "v$min–$max"
        min != null -> "≥ v$min"
        max != null -> "≤ v$max"
        else -> null
    }
}

@Composable
private fun Pill(
    text: String,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    monospace: Boolean = false,
) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.medium) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = AdminDimens.paddingSmall, vertical = 3.dp),
        )
    }
}

@Composable
private fun EmptyDetail(onNew: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(AdminDimens.paddingExtraLarge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.size(AdminDimens.paddingNormal))
        Text("Select a flag to edit it", style = MaterialTheme.typography.titleMedium)
        Text(
            "or create a new one. Ctrl/⌘ F jumps to search.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier.padding(
                    top = AdminDimens.paddingExtraSmall,
                    bottom = AdminDimens.paddingNormal,
                ),
        )
        AdminButton(text = "New flag", onClick = onNew)
    }
}

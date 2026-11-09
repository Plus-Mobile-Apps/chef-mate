package com.plusmobileapps.chefmate.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Full-screen editor used on phone and tablet widths, where the dashboard is single-pane. Laid out
 * like the app's Edit Recipe screen: tonal back button, form, and a Save FAB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagEditorScreen(draft: FlagDraft, onCancel: () -> Unit, onSave: (AdminFeatureFlag) -> Unit) {
    var state by remember(draft) { mutableStateOf(draft) }
    val validation = FlagDraftValidator.validate(state)
    val save = { if (validation.isValid) onSave(state.toFlag()) }

    Scaffold(
        modifier = Modifier.editorShortcuts(dirty = state != draft, onSave = save, onCancel),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                title = { Text(if (draft.isNew) "New flag" else "Edit flag") },
                actions = {
                    Box(Modifier.padding(end = AdminDimens.paddingSmall)) { FlagHelpAction() }
                },
                navigationIcon = {
                    AdminIconButton(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "Back",
                        onClick = onCancel,
                        modifier = Modifier.padding(start = AdminDimens.paddingSmall),
                    )
                },
            )
        },
        floatingActionButton = {
            // Always shown; an invalid form doesn't save and the field errors explain why.
            AdminFloatingActionButton(
                text = if (draft.isNew) "Create" else "Save",
                icon = Icons.Filled.Check,
                onClick = save,
            )
        },
    ) { padding ->
        CenteredContent(
            maxWidth = 760.dp,
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()),
        ) {
            Column(
                Modifier.padding(
                    start = AdminDimens.paddingNormal,
                    end = AdminDimens.paddingNormal,
                    top = AdminDimens.paddingNormal,
                    bottom = AdminDimens.fabClearance,
                ),
            ) {
                FlagEditorForm(state, validation, onChange = { state = it })
            }
        }
    }
}

/** Embedded editor for the desktop detail pane: header with actions, then a scrolling form. */
@Composable
fun FlagEditorPane(draft: FlagDraft, onCancel: () -> Unit, onSave: (AdminFeatureFlag) -> Unit) {
    var state by remember(draft) { mutableStateOf(draft) }
    val validation = FlagDraftValidator.validate(state)
    val dirty = state != draft
    val save = { if (validation.isValid) onSave(state.toFlag()) }

    Column(Modifier.fillMaxSize().editorShortcuts(dirty = dirty, onSave = save, onCancel)) {
        Row(
            Modifier.fillMaxWidth()
                .padding(
                    start = AdminDimens.paddingNormal,
                    end = AdminDimens.paddingLarge,
                    top = AdminDimens.paddingSmall,
                    bottom = AdminDimens.paddingSmall,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AdminIconButton(Icons.Filled.Close, if (dirty) "Discard" else "Close", onCancel)
            Spacer(Modifier.width(AdminDimens.paddingNormal))
            Column(Modifier.weight(1f)) {
                Text(
                    if (draft.isNew) "New flag" else draft.key,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = if (draft.isNew) null else FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (dirty) "Unsaved changes · Ctrl/⌘ S to save" else "No changes",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        if (dirty) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AdminButton(
                text = if (draft.isNew) "Create flag" else "Save",
                onClick = save,
                enabled = validation.isValid && dirty,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        CenteredContent(
            maxWidth = 760.dp,
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(AdminDimens.paddingLarge)) {
                FlagEditorForm(state, validation, onChange = { state = it })
            }
        }
    }
}

/**
 * Ctrl/⌘ S saves; Escape closes, but only when nothing has been edited so a stray key press can't
 * throw away work.
 */
private fun Modifier.editorShortcuts(
    dirty: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
): Modifier = onPreviewKeyEvent {
    if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
    when {
        it.isShortcut(Key.S) -> {
            onSave()
            true
        }
        it.key == Key.Escape && !dirty -> {
            onCancel()
            true
        }
        else -> false
    }
}

/**
 * The flag form, grouped into outlined sections like the app's recipe editor. The "Value" and
 * "Rollout" groups sit side by side once the form has room (>= 640dp of its own width), so it
 * adapts to both a full-width window and the narrower desktop detail pane.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagEditorForm(
    state: FlagDraft,
    validation: FlagValidation,
    onChange: (FlagDraft) -> Unit,
) {
    val unknownPlatforms = state.unknownPlatforms
    BoxWithConstraints {
        val twoColumn = maxWidth >= 640.dp
        Column(verticalArrangement = Arrangement.spacedBy(AdminDimens.paddingLarge)) {
            OutlinedTextField(
                value = state.key,
                onValueChange = { onChange(state.copy(key = it)) },
                label = { Text("Key") },
                singleLine = true,
                shape = PillShape,
                // Key is the primary key — don't allow changing it once the row exists.
                enabled = state.isNew,
                isError = validation.keyError != null,
                supportingText = { validation.keyError?.let { Text(it) } },
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )

            Columns(twoColumn) {
                AdminOutlinedSection("Value", Modifier.columnWeight(this)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AdminDimens.paddingSmall),
                    ) {
                        FlagType.entries.forEach { type ->
                            FilterChip(
                                selected = state.type == type,
                                onClick = { onChange(state.copy(type = type)) },
                                label = {
                                    Text(if (type == FlagType.BOOL) "Boolean" else "String")
                                },
                            )
                        }
                    }
                    when (state.type) {
                        FlagType.BOOL ->
                            LabeledSwitch(
                                checked = state.boolValue,
                                label = if (state.boolValue) "true" else "false",
                                onCheckedChange = { onChange(state.copy(boolValue = it)) },
                            )
                        FlagType.STRING ->
                            OutlinedTextField(
                                value = state.stringValue,
                                onValueChange = { onChange(state.copy(stringValue = it)) },
                                label = { Text("String value") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                    }
                }
                AdminOutlinedSection("Rollout", Modifier.columnWeight(this)) {
                    LabeledSwitch(
                        checked = state.enabled,
                        label = if (state.enabled) "Enabled" else "Disabled",
                        onCheckedChange = { onChange(state.copy(enabled = it)) },
                    )
                    Column {
                        Text(
                            "${state.rolloutPercent}% of users",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Slider(
                            value = state.rolloutPercent.toFloat(),
                            onValueChange = { onChange(state.copy(rolloutPercent = it.toInt())) },
                            valueRange = 0f..100f,
                            steps = 99,
                        )
                    }
                }
            }

            AdminOutlinedSection("Targeting") {
                Column(verticalArrangement = Arrangement.spacedBy(AdminDimens.paddingSmall)) {
                    SectionLabel("Platforms (none = all)")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(AdminDimens.paddingSmall),
                    ) {
                        FlagDraft.ALL_PLATFORMS.forEach { platform ->
                            val selected = platform in state.platforms
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onChange(
                                        state.copy(
                                            platforms =
                                                if (selected) state.platforms - platform
                                                else state.platforms + platform,
                                        ),
                                    )
                                },
                                label = { Text(FlagDraft.platformLabel(platform)) },
                            )
                        }
                        // Values left over from older rows (`desktop`, `web`) that no app reports.
                        // Shown so they can be removed deliberately, never dropped silently.
                        unknownPlatforms.forEach { platform ->
                            FilterChip(
                                selected = true,
                                onClick = {
                                    onChange(state.copy(platforms = state.platforms - platform))
                                },
                                label = { Text(platform) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove $platform",
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                },
                                colors =
                                    FilterChipDefaults.filterChipColors(
                                        selectedContainerColor =
                                            MaterialTheme.colorScheme.errorContainer,
                                        selectedLabelColor =
                                            MaterialTheme.colorScheme.onErrorContainer,
                                        selectedTrailingIconColor =
                                            MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                            )
                        }
                    }
                    if (unknownPlatforms.isNotEmpty()) {
                        Text(
                            unknownPlatformsWarning(unknownPlatforms, state.platforms),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(AdminDimens.paddingNormal)) {
                    OutlinedTextField(
                        value = state.minVersion,
                        onValueChange = { onChange(state.copy(minVersion = it)) },
                        label = { Text("Min version") },
                        singleLine = true,
                        shape = PillShape,
                        isError = validation.versionError != null,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.maxVersion,
                        onValueChange = { onChange(state.copy(maxVersion = it)) },
                        label = { Text("Max version") },
                        singleLine = true,
                        shape = PillShape,
                        isError = validation.versionError != null,
                        modifier = Modifier.weight(1f),
                    )
                }
                validation.versionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                OutlinedTextField(
                    value = state.userIdsText,
                    onValueChange = { onChange(state.copy(userIdsText = it)) },
                    label = { Text("User allowlist (Supabase user ids)") },
                    supportingText = {
                        Text(
                            "One id per line (or comma-separated). These users get the flag on top of the rollout %.",
                        )
                    },
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = { onChange(state.copy(description = it)) },
                label = { Text("Description (admin notes)") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(AdminDimens.paddingSmall))
        }
    }
}

/** Side by side when [twoColumn], otherwise stacked. Children use [columnWeight]. */
@Composable
private fun Columns(twoColumn: Boolean, content: @Composable ColumnsScope.() -> Unit) {
    if (twoColumn) {
        Row(horizontalArrangement = Arrangement.spacedBy(AdminDimens.paddingNormal)) {
            ColumnsScope(rowScope = this).content()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(AdminDimens.paddingLarge)) {
            ColumnsScope(rowScope = null).content()
        }
    }
}

private class ColumnsScope(val rowScope: RowScope?)

private fun Modifier.columnWeight(scope: ColumnsScope): Modifier =
    scope.rowScope?.run { this@columnWeight.weight(1f) } ?: this.fillMaxWidth()

@Composable
private fun LabeledSwitch(checked: Boolean, label: String, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = AdminDimens.paddingSmall + 4.dp),
        )
    }
}

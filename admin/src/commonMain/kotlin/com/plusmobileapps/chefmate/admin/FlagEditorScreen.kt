package com.plusmobileapps.chefmate.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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

/** Full-screen editor used on phone and tablet widths, where the dashboard is single-pane. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlagEditorScreen(draft: FlagDraft, onCancel: () -> Unit, onSave: (AdminFeatureFlag) -> Unit) {
    var state by remember(draft) { mutableStateOf(draft) }
    val validation = FlagDraftValidator.validate(state)
    val save = { if (validation.isValid) onSave(state.toFlag()) }

    Scaffold(
        modifier = Modifier.editorShortcuts(dirty = state != draft, onSave = save, onCancel),
        topBar = {
            TopAppBar(
                title = { Text(if (draft.isNew) "New flag" else "Edit flag") },
                navigationIcon = { TextButton(onClick = onCancel) { Text("Cancel") } },
                actions = {
                    TextButton(onClick = save, enabled = validation.isValid) { Text("Save") }
                },
            )
        },
    ) { padding ->
        CenteredContent(
            maxWidth = 760.dp,
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlagEditorForm(state, validation, onChange = { state = it })
                if (!draft.isNew) {
                    Button(
                        onClick = save,
                        enabled = validation.isValid,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save changes")
                    }
                }
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
                .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
            OutlinedButton(onClick = onCancel) { Text(if (dirty) "Discard" else "Close") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = save, enabled = validation.isValid && dirty) {
                Text(if (draft.isNew) "Create flag" else "Save")
            }
        }
        HorizontalDivider()
        CenteredContent(
            maxWidth = 760.dp,
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(24.dp)) {
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
 * The flag form. Lays out in two columns once it has room (>= 640dp of its own width), so it adapts
 * to both a full-width window and the narrower desktop detail pane.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlagEditorForm(
    state: FlagDraft,
    validation: FlagValidation,
    onChange: (FlagDraft) -> Unit,
) {
    BoxWithConstraints {
        val twoColumn = maxWidth >= 640.dp
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            OutlinedTextField(
                value = state.key,
                onValueChange = { onChange(state.copy(key = it)) },
                label = { Text("Key") },
                singleLine = true,
                // Key is the primary key — don't allow changing it once the row exists.
                enabled = state.isNew,
                isError = validation.keyError != null,
                supportingText = { validation.keyError?.let { Text(it) } },
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )

            Columns(twoColumn) {
                LabeledSection("Type", Modifier.columnWeight(this)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                }
                LabeledSection("Value", Modifier.columnWeight(this)) {
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
            }

            Columns(twoColumn) {
                LabeledSection("Status", Modifier.columnWeight(this)) {
                    LabeledSwitch(
                        checked = state.enabled,
                        label = "Enabled",
                        onCheckedChange = { onChange(state.copy(enabled = it)) },
                    )
                }
                LabeledSection("Rollout: ${state.rolloutPercent}%", Modifier.columnWeight(this)) {
                    Slider(
                        value = state.rolloutPercent.toFloat(),
                        onValueChange = { onChange(state.copy(rolloutPercent = it.toInt())) },
                        valueRange = 0f..100f,
                        steps = 99,
                    )
                }
            }

            HorizontalDivider()

            LabeledSection("Platforms (none = all)") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            label = { Text(platform) },
                        )
                    }
                }
            }

            LabeledSection("App versions (optional)") {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = state.minVersion,
                        onValueChange = { onChange(state.copy(minVersion = it)) },
                        label = { Text("Min version") },
                        singleLine = true,
                        isError = validation.versionError != null,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.maxVersion,
                        onValueChange = { onChange(state.copy(maxVersion = it)) },
                        label = { Text("Max version") },
                        singleLine = true,
                        isError = validation.versionError != null,
                        modifier = Modifier.weight(1f),
                    )
                }
                validation.versionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }

            LabeledSection("User allowlist (optional)") {
                OutlinedTextField(
                    value = state.userIdsText,
                    onValueChange = { onChange(state.copy(userIdsText = it)) },
                    label = { Text("Supabase user ids") },
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
        }
    }
}

/** Side by side when [twoColumn], otherwise stacked. Children use [columnWeight]. */
@Composable
private fun Columns(twoColumn: Boolean, content: @Composable ColumnsScope.() -> Unit) {
    if (twoColumn) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            ColumnsScope(rowScope = this).content()
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ColumnsScope(rowScope = null).content()
        }
    }
}

private class ColumnsScope(val rowScope: androidx.compose.foundation.layout.RowScope?)

private fun Modifier.columnWeight(scope: ColumnsScope): Modifier =
    scope.rowScope?.run { this@columnWeight.weight(1f) } ?: this.fillMaxWidth()

@Composable
private fun LabeledSwitch(checked: Boolean, label: String, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun LabeledSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

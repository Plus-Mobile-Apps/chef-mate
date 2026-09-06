@file:OptIn(ExperimentalMaterial3Api::class)

package com.plusmobileapps.chefmate.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import chefmate.client.ui.public.generated.resources.Res
import chefmate.client.ui.public.generated.resources.markdown_editor_bold_a11y
import chefmate.client.ui.public.generated.resources.markdown_editor_bulleted_list_a11y
import chefmate.client.ui.public.generated.resources.markdown_editor_italic_a11y
import chefmate.client.ui.public.generated.resources.markdown_editor_link_recipe_a11y
import chefmate.client.ui.public.generated.resources.markdown_editor_mode_markdown
import chefmate.client.ui.public.generated.resources.markdown_editor_mode_rich_text
import chefmate.client.ui.public.generated.resources.markdown_editor_numbered_list_a11y
import chefmate.client.ui.public.generated.resources.markdown_editor_preview_empty
import chefmate.client.ui.public.generated.resources.markdown_editor_resize_handle_a11y
import chefmate.client.ui.public.generated.resources.markdown_editor_tab_preview
import chefmate.client.ui.public.generated.resources.markdown_editor_tab_write
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.OutlinedRichTextEditor
import com.plusmobileapps.chefmate.ui.text.BOLD_MARKER
import com.plusmobileapps.chefmate.ui.text.ITALIC_MARKER
import com.plusmobileapps.chefmate.ui.text.insertMarkdownLink
import com.plusmobileapps.chefmate.ui.text.parseListLine
import com.plusmobileapps.chefmate.ui.text.toDisplayAnnotatedString
import com.plusmobileapps.chefmate.ui.text.toggleBulletList
import com.plusmobileapps.chefmate.ui.text.toggleInlineMarker
import com.plusmobileapps.chefmate.ui.text.toggleNumberedList
import com.plusmobileapps.chefmate.ui.text.withoutLineBreakTags
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Imperative handle for inserting a `[label](url)` link into a [PlusMarkdownEditor] from outside
 * the composable — e.g. after a recipe picker returns a selection. Create one with `remember`, pass
 * it to the editor, and call [insertLink]; the editor splices the link into the active mode (raw
 * markdown at the caret, or a rendered link in rich-text mode).
 */
class PlusMarkdownEditorController {
    internal var insertLinkImpl: ((label: String, url: String) -> Unit)? = null

    fun insertLink(label: String, url: String) {
        insertLinkImpl?.invoke(label, url)
    }
}

/**
 * A shared, resizable editor for plain-text fields that carry inline markdown (`**bold**`,
 * `_italic_`). The caller stores plain markdown [String]; the editor owns only transient state
 * (cursor/selection, the Markdown write/preview sub-toggle, and the resize height).
 *
 * Two modes, switched by [richTextMode] (a persisted, app-wide preference hoisted to the caller):
 * - **Rich text** — a WYSIWYG editor (compose-rich-editor): Bold/Italic format the selection and
 *   the field renders the formatting inline, markers hidden. The more approachable mode on mobile.
 * - **Markdown** — edits the raw source with a Bold/Italic toolbar plus a GitHub-style
 *   Write/Preview toggle (the switch animates). Toolbar buttons are non-focusable so tapping one
 *   never clears the field's selection.
 *
 * [showListButtons] adds bulleted/numbered list buttons to the toolbar — meaningful for the
 * one-item-per-line ingredient and direction fields, omitted for free-form fields like description.
 *
 * A drag handle below the field, aligned to the end, resizes the editor between [minHeight] and
 * [maxHeight].
 */
@Composable
fun PlusMarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    richTextMode: Boolean,
    onRichTextModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    showListButtons: Boolean = false,
    minHeight: Dp = 160.dp,
    maxHeight: Dp = 480.dp,
    initialHeight: Dp = 200.dp,
    controller: PlusMarkdownEditorController? = null,
    onInsertLinkClick: (() -> Unit)? = null,
) {
    var heightDp by rememberSaveable { mutableStateOf(initialHeight.value) }
    var showPreview by rememberSaveable { mutableStateOf(false) }

    // ── Markdown-mode editing state ───────────────────────────────────────────────────────────
    var fieldValue by remember { mutableStateOf(TextFieldValue(value)) }
    LaunchedEffect(value) {
        // Re-sync only when external text diverges, so typing keeps the caret.
        if (value != fieldValue.text) fieldValue = TextFieldValue(value, TextRange(value.length))
    }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    fun updateMarkdown(newValue: TextFieldValue) {
        fieldValue = newValue
        onValueChange(newValue.text)
    }

    fun applyMarkdownMarker(marker: String) {
        updateMarkdown(fieldValue.toggleInlineMarker(marker))
        scope.launch { runCatching { focusRequester.requestFocus() } }
    }

    fun applyMarkdownTransform(transform: (TextFieldValue) -> TextFieldValue) {
        updateMarkdown(transform(fieldValue))
        scope.launch { runCatching { focusRequester.requestFocus() } }
    }

    // ── Rich-text-mode editing state ──────────────────────────────────────────────────────────
    val richTextState = rememberRichTextState()
    val latestValue by rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    // Seed the initial content synchronously so it's present on the first frame (no empty flash).
    remember(richTextState) { richTextState.setMarkdown(value) }
    // The rich editor's markdown as the caller last saw it. Tracking it lets both halves of the
    // sync below compare without re-serializing the whole document on every keystroke.
    var richTextMarkdown by remember { mutableStateOf(value) }

    // Both effects run only while rich text is the active editor. In Markdown mode the raw field
    // owns the value, and letting the hidden rich state write back would round-trip every keystroke
    // through the library's parser — which turns a pair of trailing blank lines into a literal
    // `<br>` that the user then cannot delete, because deleting it re-creates it.
    LaunchedEffect(richTextMode, value) {
        if (!richTextMode) return@LaunchedEffect
        // Load external markdown into the rich editor; the equality guard avoids resetting the
        // caret while the user is typing (we just emitted this exact value).
        if (value != richTextMarkdown) {
            richTextMarkdown = value
            richTextState.setMarkdown(value)
        }
    }
    LaunchedEffect(richTextMode, richTextState) {
        if (!richTextMode) return@LaunchedEffect
        snapshotFlow { richTextState.annotatedString }
            .collect {
                // Blank paragraphs serialize to `<br>`, which is meaningless in the plain
                // one-item-per-line text the caller stores — normalize it back to a blank line.
                val markdown = richTextState.toMarkdown().withoutLineBreakTags()
                richTextMarkdown = markdown
                // Compare against the normalized caller value so a recipe saved with a stray
                // `<br>` by an older build isn't reported as an edit the moment it's opened.
                if (markdown != latestValue.withoutLineBreakTags()) latestOnValueChange(markdown)
            }
    }

    fun onBold() {
        if (richTextMode) richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
        else applyMarkdownMarker(BOLD_MARKER)
    }

    fun onItalic() {
        if (richTextMode) richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
        else applyMarkdownMarker(ITALIC_MARKER)
    }

    fun onBulletList() {
        if (richTextMode) richTextState.toggleUnorderedList()
        else applyMarkdownTransform { it.toggleBulletList() }
    }

    fun onNumberedList() {
        if (richTextMode) richTextState.toggleOrderedList()
        else applyMarkdownTransform { it.toggleNumberedList() }
    }

    // Bind the imperative insert handle to the active mode. Reassigned every recomposition so it
    // always closes over the current mode and field state.
    controller?.insertLinkImpl = { label, url ->
        if (richTextMode) {
            richTextState.addLink(text = label, url = url)
        } else {
            updateMarkdown(fieldValue.insertMarkdownLink(label, url))
        }
    }

    // A light rounded outline groups each editor as one unit, so it's clear which toolbar/toggle
    // belongs to which field when several editors stack on the form.
    PlusOutlinedContainer(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            EditorModeToggle(
                richTextMode = richTextMode,
                onRichTextModeChange = onRichTextModeChange,
            )
        }

        // Toolbar row: Bold/Italic on the left (hidden only in Markdown preview), Write/Preview
        // toggle on the right (Markdown mode only).
        val showFormatButtons = richTextMode || !showPreview
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (showFormatButtons) {
                MarkdownFormatToolbar(
                    onBold = ::onBold,
                    onItalic = ::onItalic,
                    showListButtons = showListButtons,
                    onBulletList = ::onBulletList,
                    onNumberedList = ::onNumberedList,
                    onInsertLinkClick = onInsertLinkClick,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (!richTextMode) {
                WritePreviewToggle(
                    showPreview = showPreview,
                    onShowPreviewChange = { showPreview = it },
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(heightDp.dp)) {
            if (richTextMode) {
                OutlinedRichTextEditor(
                    state = richTextState,
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AnimatedContent(
                    targetState = showPreview,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                    label = "markdown-write-preview",
                    modifier = Modifier.fillMaxSize(),
                ) { preview ->
                    if (preview) {
                        MarkdownPreview(text = fieldValue.text, modifier = Modifier.fillMaxSize())
                    } else {
                        OutlinedTextField(
                            value = fieldValue,
                            onValueChange = ::updateMarkdown,
                            placeholder = { Text(placeholder) },
                            modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                        )
                    }
                }
            }
        }

        // The handle sits below the field rather than overlaying its bottom-end corner: on touch
        // platforms an overlay swallows the drags that move the caret and selection handles, which
        // land in exactly that corner on the last line.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ResizeHandle(
                label = label,
                onResizeBy = { deltaDp ->
                    heightDp = (heightDp + deltaDp).coerceIn(minHeight.value, maxHeight.value)
                },
            )
        }
    }
}

@Composable
private fun EditorModeToggle(
    richTextMode: Boolean,
    onRichTextModeChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = richTextMode,
            onClick = { onRichTextModeChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
            Text(stringResource(Res.string.markdown_editor_mode_rich_text))
        }
        SegmentedButton(
            selected = !richTextMode,
            onClick = { onRichTextModeChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
            Text(stringResource(Res.string.markdown_editor_mode_markdown))
        }
    }
}

@Composable
private fun WritePreviewToggle(
    showPreview: Boolean,
    onShowPreviewChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        SegmentedButton(
            selected = !showPreview,
            onClick = { onShowPreviewChange(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
        ) {
            Text(stringResource(Res.string.markdown_editor_tab_write))
        }
        SegmentedButton(
            selected = showPreview,
            onClick = { onShowPreviewChange(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
        ) {
            Text(stringResource(Res.string.markdown_editor_tab_preview))
        }
    }
}

@Composable
private fun MarkdownFormatToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    showListButtons: Boolean,
    onBulletList: () -> Unit,
    onNumberedList: () -> Unit,
    onInsertLinkClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingExtraSmall),
    ) {
        FormatButton(
            icon = Icons.Default.FormatBold,
            contentDescription = stringResource(Res.string.markdown_editor_bold_a11y),
            onClick = onBold,
        )
        FormatButton(
            icon = Icons.Default.FormatItalic,
            contentDescription = stringResource(Res.string.markdown_editor_italic_a11y),
            onClick = onItalic,
        )
        if (showListButtons) {
            FormatButton(
                icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                contentDescription = stringResource(Res.string.markdown_editor_bulleted_list_a11y),
                onClick = onBulletList,
            )
            FormatButton(
                icon = Icons.Default.FormatListNumbered,
                contentDescription = stringResource(Res.string.markdown_editor_numbered_list_a11y),
                onClick = onNumberedList,
            )
        }
        if (onInsertLinkClick != null) {
            FormatButton(
                icon = Icons.Default.AddLink,
                contentDescription = stringResource(Res.string.markdown_editor_link_recipe_a11y),
                onClick = onInsertLinkClick,
            )
        }
    }
}

@Composable
private fun FormatButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit) {
    // Non-focusable so pressing it doesn't pull focus off the text field and collapse its
    // selection.
    IconButton(onClick = onClick, modifier = Modifier.focusProperties { canFocus = false }) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}

@Composable
private fun MarkdownPreview(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.extraSmall,
                )
                .padding(ChefMateTheme.dimens.paddingNormal)
    ) {
        if (text.isBlank()) {
            Text(
                text = stringResource(Res.string.markdown_editor_preview_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingExtraSmall),
            ) {
                text.withoutLineBreakTags().split("\n").forEach { line ->
                    Text(
                        text = parseListLine(line).toDisplayAnnotatedString(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResizeHandle(
    label: String,
    onResizeBy: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = stringResource(Res.string.markdown_editor_resize_handle_a11y, label),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier.padding(ChefMateTheme.dimens.paddingSmall).size(20.dp).pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onResizeBy(dragAmount.y.toDp().value)
                }
            },
    )
}

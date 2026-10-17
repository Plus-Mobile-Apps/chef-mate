package com.plusmobileapps.chefmate.ui.text

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

// A leading bullet marker on a line: "- item" or "* item".
private val bulletLine = Regex("""^\s*[-*]\s+""")

// A leading ordered enumerator on a line: "1. item", "2) item".
private val orderedLine = Regex("""^\s*\d+[.)]\s+""")

/**
 * Toggles an inline [marker] (e.g. [BOLD_MARKER] or [ITALIC_MARKER]) around the current selection:
 * - If the selection is already wrapped in [marker], it is unwrapped and the inner text
 *   re-selected.
 * - Otherwise the selection is wrapped and the inner text re-selected.
 * - With an empty selection, an empty marker pair is inserted and the cursor placed between the
 *   markers, ready for the user to type formatted text.
 */
fun TextFieldValue.toggleInlineMarker(marker: String): TextFieldValue {
    val start = minOf(selection.start, selection.end)
    val end = maxOf(selection.start, selection.end)
    val before = text.substring(0, start)
    val selected = text.substring(start, end)
    val after = text.substring(end)

    val alreadyWrapped =
        selected.length >= marker.length * 2 &&
            selected.startsWith(marker) &&
            selected.endsWith(marker)

    return if (alreadyWrapped) {
        val inner = selected.substring(marker.length, selected.length - marker.length)
        TextFieldValue(
            text = before + inner + after,
            selection = TextRange(start, start + inner.length),
        )
    } else {
        val innerStart = start + marker.length
        TextFieldValue(
            text = before + marker + selected + marker + after,
            selection = TextRange(innerStart, innerStart + selected.length),
        )
    }
}

/**
 * Replaces the current selection with a `[label](url)` markdown link and places the cursor just
 * after it. With an empty selection the link is inserted at the caret.
 */
fun TextFieldValue.insertMarkdownLink(label: String, url: String): TextFieldValue {
    val start = minOf(selection.start, selection.end)
    val end = maxOf(selection.start, selection.end)
    val markdown = "[$label]($url)"
    val newText = text.substring(0, start) + markdown + text.substring(end)
    val cursor = start + markdown.length
    return TextFieldValue(text = newText, selection = TextRange(cursor))
}

/**
 * Toggles a bulleted list across the line(s) the selection touches. If every non-blank line in the
 * range is already bulleted the markers are removed; otherwise a `- ` marker is added to each
 * (replacing any existing ordered enumerator). The whole affected range is re-selected.
 */
fun TextFieldValue.toggleBulletList(): TextFieldValue =
    toggleLinePrefix(
        isMarked = { bulletLine.containsMatchIn(it) },
        strip = { it.replaceFirst(bulletLine, "").replaceFirst(orderedLine, "") },
        addPrefix = { _, line -> "$BULLET_MARKER$line" },
    )

/**
 * Toggles a numbered list across the line(s) the selection touches. If every non-blank line in the
 * range is already numbered the enumerators are removed; otherwise each line is renumbered `1. `,
 * `2. `, … (replacing any existing bullet). The whole affected range is re-selected.
 */
fun TextFieldValue.toggleNumberedList(): TextFieldValue =
    toggleLinePrefix(
        isMarked = { orderedLine.containsMatchIn(it) },
        strip = { it.replaceFirst(orderedLine, "").replaceFirst(bulletLine, "") },
        addPrefix = { index, line -> "${index + 1}. $line" },
    )

/**
 * Shared line-prefix toggle. Expands the selection to whole lines, and — unless every non-blank
 * line already satisfies [isMarked], in which case it [strip]s them — applies [addPrefix] (0-based
 * index among non-blank lines) to each non-blank line. Blank lines are left untouched. The
 * rewritten range is re-selected so a follow-up tap toggles it back off.
 */
private fun TextFieldValue.toggleLinePrefix(
    isMarked: (String) -> Boolean,
    strip: (String) -> String,
    addPrefix: (index: Int, line: String) -> String,
): TextFieldValue {
    val selStart = minOf(selection.start, selection.end)
    val selEnd = maxOf(selection.start, selection.end)

    // Grow the range to cover the full lines the selection intersects.
    val lineStart = text.lastIndexOf('\n', selStart - 1) + 1
    val lineEndIndex = text.indexOf('\n', selEnd)
    val lineEnd = if (lineEndIndex == -1) text.length else lineEndIndex

    val before = text.substring(0, lineStart)
    val block = text.substring(lineStart, lineEnd)
    val after = text.substring(lineEnd)

    val lines = block.split("\n")
    val nonBlank = lines.filter { it.isNotBlank() }
    val allMarked = nonBlank.isNotEmpty() && nonBlank.all(isMarked)

    var counter = 0
    val rewritten =
        lines.joinToString("\n") { line ->
            if (line.isBlank()) {
                line
            } else if (allMarked) {
                strip(line)
            } else {
                addPrefix(counter++, strip(line))
            }
        }

    return TextFieldValue(
        text = before + rewritten + after,
        selection = TextRange(lineStart, lineStart + rewritten.length),
    )
}

/** The HTML tag the rich-text editor serializes a run of consecutive blank paragraphs to. */
private const val LINE_BREAK_TAG = "<br>"

/**
 * Rewrites the standalone `<br>` lines the rich-text editor emits for consecutive blank paragraphs
 * back into the plain blank lines they stand for.
 *
 * Recipe text is stored as plain, one-item-per-line markdown and rendered with the inline parser,
 * which knows nothing about HTML — so a `<br>` that reaches storage shows up as a literal line of
 * text on the detail and cook screens, and as a stray item when ingredients are added to a grocery
 * list. The blank line it stands for survives the round trip through the rich editor unchanged, so
 * dropping the tag is lossless. Lines without the tag are returned untouched.
 */
fun String.withoutLineBreakTags(): String =
    if (!contains(LINE_BREAK_TAG)) {
        this
    } else {
        split("\n").joinToString("\n") { line ->
            if (!line.contains(LINE_BREAK_TAG)) line
            else line.replace(LINE_BREAK_TAG, "").let { if (it.isBlank()) "" else it }
        }
    }

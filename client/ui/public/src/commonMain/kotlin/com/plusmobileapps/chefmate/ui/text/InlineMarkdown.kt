package com.plusmobileapps.chefmate.ui.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

/**
 * Marker for bold inline spans. Wrapping a selection in [BOLD_MARKER] renders it bold both in the
 * editor toolbar and when the recipe is displayed.
 */
const val BOLD_MARKER: String = "**"

/** Marker for italic inline spans. */
const val ITALIC_MARKER: String = "_"

/**
 * Parses a single line of lightweight inline markdown into an [AnnotatedString], supporting
 * `**bold**`, `_italic_` (nestable, e.g. `**bold _and italic_**`), `[label](url)` links, and bare
 * `http(s)://` URLs. Markers without a matching partner are left as literal text, so plain recipe
 * lines — and stray `*` or `_` characters — render unchanged. This is intentionally inline-only:
 * ingredients and directions stay one item per line, so block markdown (headings, lists, etc.) is
 * not interpreted.
 *
 * A `[label](url)` link always renders as just its [label] (the raw markdown is never shown). When
 * [onLinkClick] is non-null the label is wrapped in a clickable [LinkAnnotation] — styled with
 * [linkStyle] — whose listener is invoked with the link's `url`; when it is null (read-only
 * surfaces like cook mode) the label renders as plain, inert text. A bare URL is linked the same
 * way (its text is the label) and is otherwise kept verbatim, so `_` or `**` inside it is never
 * read as a style marker.
 */
fun String.toInlineMarkdownAnnotatedString(
    linkStyle: SpanStyle? = null,
    onLinkClick: ((String) -> Unit)? = null,
): AnnotatedString = buildAnnotatedString {
    appendInlineMarkdown(this@toInlineMarkdownAnnotatedString, linkStyle, onLinkClick)
}

private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    linkStyle: SpanStyle?,
    onLinkClick: ((String) -> Unit)?,
) {
    var literalStart = 0
    var i = 0
    while (i < text.length) {
        val link = linkAt(text, i)
        if (link != null) {
            if (literalStart < i) append(text.substring(literalStart, i))
            appendLink(link, linkStyle, onLinkClick)
            i = link.end
            literalStart = i
            continue
        }
        val bareUrlEnd = bareUrlEndAt(text, i)
        if (bareUrlEnd != null) {
            if (literalStart < i) append(text.substring(literalStart, i))
            appendBareUrl(text.substring(i, bareUrlEnd), linkStyle, onLinkClick)
            i = bareUrlEnd
            literalStart = i
            continue
        }
        val marker = markerAt(text, i)
        if (marker == null) {
            i++
            continue
        }
        val contentStart = i + marker.length
        val close = text.indexOf(marker, contentStart)
        if (close == -1) {
            // No closing marker — treat this marker as literal text and keep scanning past it.
            i += marker.length
            continue
        }
        if (literalStart < i) append(text.substring(literalStart, i))
        withStyle(styleFor(marker)) {
            appendInlineMarkdown(text.substring(contentStart, close), linkStyle, onLinkClick)
        }
        i = close + marker.length
        literalStart = i
    }
    if (literalStart < text.length) append(text.substring(literalStart))
}

private fun AnnotatedString.Builder.appendLink(
    link: InlineLink,
    linkStyle: SpanStyle?,
    onLinkClick: ((String) -> Unit)?,
) {
    if (onLinkClick == null) {
        // No handler — render the label as plain (still inline-styled) text; the link is inert.
        appendInlineMarkdown(link.label, linkStyle, null)
        return
    }
    val annotation =
        LinkAnnotation.Clickable(
            tag = link.url,
            styles = linkStyle?.let { TextLinkStyles(style = it) },
            linkInteractionListener = { onLinkClick(link.url) },
        )
    withLink(annotation) { appendInlineMarkdown(link.label, linkStyle, onLinkClick) }
}

private fun AnnotatedString.Builder.appendBareUrl(
    url: String,
    linkStyle: SpanStyle?,
    onLinkClick: ((String) -> Unit)?,
) {
    if (onLinkClick == null) {
        append(url)
        return
    }
    val annotation =
        LinkAnnotation.Clickable(
            tag = url,
            styles = linkStyle?.let { TextLinkStyles(style = it) },
            linkInteractionListener = { onLinkClick(url) },
        )
    withLink(annotation) { append(url) }
}

/** A parsed `[label](url)` link and the index just past its closing `)`. */
private class InlineLink(val label: String, val url: String, val end: Int)

/**
 * Attempts to parse a `[label](url)` link starting at [index]. Uses the first `]` and first `)` so
 * a non-link `[` (e.g. a stray bracket) falls through to literal text. Empty label or url is
 * rejected.
 */
private fun linkAt(text: String, index: Int): InlineLink? {
    if (text[index] != '[') return null
    val labelEnd = text.indexOf(']', index + 1)
    if (labelEnd == -1 || labelEnd + 1 >= text.length || text[labelEnd + 1] != '(') return null
    val urlEnd = text.indexOf(')', labelEnd + 2)
    if (urlEnd == -1) return null
    val label = text.substring(index + 1, labelEnd)
    val url = text.substring(labelEnd + 2, urlEnd)
    if (label.isEmpty() || url.isEmpty()) return null
    return InlineLink(label = label, url = url, end = urlEnd + 1)
}

private val BARE_URL_SCHEMES = listOf("https://", "http://")

/** Trailing characters that usually end the surrounding sentence rather than the URL itself. */
private const val BARE_URL_TRAILING_PUNCTUATION = ".,;:!?'\")]}"

/**
 * Returns the end index of a bare `http(s)://` URL starting at [index], or null when there isn't
 * one. The URL must start a word (so `xhttps://` isn't matched) and runs to the next whitespace,
 * minus any trailing sentence punctuation (`see https://example.com.` links `https://example.com`).
 */
private fun bareUrlEndAt(text: String, index: Int): Int? {
    if (index > 0 && text[index - 1].isLetterOrDigit()) return null
    val scheme =
        BARE_URL_SCHEMES.firstOrNull { text.startsWith(it, index, ignoreCase = true) }
            ?: return null
    var end = index + scheme.length
    while (end < text.length && !text[end].isWhitespace()) end++
    while (end > index + scheme.length && text[end - 1] in BARE_URL_TRAILING_PUNCTUATION) end--
    return if (end > index + scheme.length) end else null
}

private fun markerAt(text: String, index: Int): String? =
    when {
        text.startsWith(BOLD_MARKER, index) -> BOLD_MARKER
        text.startsWith(ITALIC_MARKER, index) -> ITALIC_MARKER
        else -> null
    }

private fun styleFor(marker: String): SpanStyle =
    when (marker) {
        BOLD_MARKER -> SpanStyle(fontWeight = FontWeight.Bold)
        else -> SpanStyle(fontStyle = FontStyle.Italic)
    }

import SwiftUI
import ComposeApp

/// The native counterpart of Compose's `PlusMarkdownEditor` in its markdown mode: a `TextEditor`
/// over the raw markdown the BLoC stores, with a formatting row that acts on the selection (bold,
/// italic, bullet and numbered lines, link a recipe) and a preview that renders the text the way
/// recipe detail will. The WYSIWYG rich-text mode is not ported; iOS always edits markdown.
struct MarkdownEditorField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    @Binding var selection: TextSelection?
    var showListButtons = false
    var minHeight: CGFloat = 200
    /// Shows a "link a recipe" button when set.
    var onLinkRecipe: (() -> Void)?

    @State private var isPreviewing = false

    var body: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
            HStack(spacing: ChefTheme.Spacing.extraSmall) {
                if !isPreviewing {
                    FormatButton("Bold", systemImage: "bold") { apply { MarkdownEdit.wrap($0, $1, marker: "**") } }
                    FormatButton("Italic", systemImage: "italic") { apply { MarkdownEdit.wrap($0, $1, marker: "_") } }
                    if showListButtons {
                        FormatButton("Bulleted list", systemImage: "list.bullet") {
                            apply { MarkdownEdit.prefixLines($0, $1, numbered: false) }
                        }
                        FormatButton("Numbered list", systemImage: "list.number") {
                            apply { MarkdownEdit.prefixLines($0, $1, numbered: true) }
                        }
                    }
                    if let onLinkRecipe {
                        FormatButton("Link a recipe", systemImage: "link", action: onLinkRecipe)
                    }
                }
                Spacer(minLength: 0)
                Toggle(isOn: $isPreviewing.animation(.snappy)) {
                    Label("Preview", systemImage: "eye")
                        .labelStyle(.iconOnly)
                }
                .toggleStyle(.button)
                .accessibilityLabel("Preview")
            }
            .tint(ChefTheme.primary)

            if isPreviewing {
                MarkdownPreview(text: text, placeholder: placeholder)
                    .frame(maxWidth: .infinity, minHeight: minHeight, alignment: .topLeading)
            } else {
                TextEditor(text: $text, selection: $selection)
                    .frame(minHeight: minHeight)
                    .overlay(alignment: .topLeading) {
                        if text.isEmpty {
                            Text(placeholder)
                                .foregroundStyle(.tertiary)
                                .padding(.top, 8)
                                .padding(.leading, 5)
                                .allowsHitTesting(false)
                        }
                    }
                    .accessibilityLabel(label)
            }
        }
    }

    /// Runs a markdown edit on the current text and selection, then writes both back.
    private func apply(_ edit: (String, Range<Int>) -> (String, Range<Int>)) {
        let (newText, newRange) = edit(text, selectionOffsets(in: text))
        text = newText
        selection = TextSelection(range: newText.index(at: newRange.lowerBound)..<newText.index(at: newRange.upperBound))
    }

    private func selectionOffsets(in text: String) -> Range<Int> {
        switch selection?.indices {
        case .selection(let range):
            let lower = text.distance(from: text.startIndex, to: min(range.lowerBound, text.endIndex))
            let upper = text.distance(from: text.startIndex, to: min(range.upperBound, text.endIndex))
            return lower..<max(lower, upper)
        default:
            return text.count..<text.count
        }
    }
}

extension MarkdownEditorField {
    /// Inserts `[label](url)` at `selection` in `text` (replacing any selected text), as the
    /// Compose editor's controller does when a recipe link is picked.
    static func insertingLink(label: String, url: String, into text: String, at selection: TextSelection?) -> (String, TextSelection) {
        let range: Range<Int>
        if case .selection(let r) = selection?.indices {
            let lower = text.distance(from: text.startIndex, to: min(r.lowerBound, text.endIndex))
            let upper = text.distance(from: text.startIndex, to: min(r.upperBound, text.endIndex))
            range = lower..<max(lower, upper)
        } else {
            range = text.count..<text.count
        }
        let (newText, newRange) = MarkdownEdit.replace(text, range, with: "[\(label)](\(url))")
        return (newText, TextSelection(insertionPoint: newText.index(at: newRange.upperBound)))
    }
}

private struct FormatButton: View {
    let title: String
    let systemImage: String
    let action: () -> Void

    init(_ title: String, systemImage: String, action: @escaping () -> Void) {
        self.title = title
        self.systemImage = systemImage
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .frame(width: 36, height: 32)
                .contentShape(Rectangle())
        }
        .buttonStyle(.borderless)
        .accessibilityLabel(title)
    }
}

/// The text rendered with the same rules as recipe detail (`RecipeTextFormatter`).
private struct MarkdownPreview: View {
    let text: String
    let placeholder: String

    var body: some View {
        let lines = RecipeTextFormatter.shared.lines(text: text)
        if lines.isEmpty {
            Text(placeholder).foregroundStyle(.tertiary)
        } else {
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
                ForEach(Array(lines.enumerated()), id: \.offset) { _, line in
                    Text(line.attributed)
                        .fontWeight(line.isHeader ? .bold : nil)
                }
            }
            // Links in the preview are for show; don't navigate away mid-edit.
            .environment(\.openURL, OpenURLAction { _ in .handled })
        }
    }
}

/// Pure markdown edits on character offsets, so they can be unit-reasoned without UI.
enum MarkdownEdit {
    /// Wraps the selection in `marker` (e.g. `**`), or inserts a pair and puts the caret between.
    static func wrap(_ text: String, _ range: Range<Int>, marker: String) -> (String, Range<Int>) {
        var chars = Array(text)
        let selected = String(chars[range])
        chars.replaceSubrange(range, with: Array(marker + selected + marker))
        let start = range.lowerBound + marker.count
        return (String(chars), start..<(start + selected.count))
    }

    /// Replaces the selection with `insert`; the returned range covers the inserted text.
    static func replace(_ text: String, _ range: Range<Int>, with insert: String) -> (String, Range<Int>) {
        var chars = Array(text)
        chars.replaceSubrange(range, with: Array(insert))
        return (String(chars), range.lowerBound..<(range.lowerBound + insert.count))
    }

    /// Toggles a `- ` bullet or `1. ` numbering on every line the selection touches. If every
    /// non-blank line already has that marker it's removed; otherwise it's added (numbers count
    /// up from 1).
    static func prefixLines(_ text: String, _ range: Range<Int>, numbered: Bool) -> (String, Range<Int>) {
        let chars = Array(text)
        var start = range.lowerBound
        while start > 0 && chars[start - 1] != "\n" { start -= 1 }
        var end = range.upperBound
        while end < chars.count && chars[end] != "\n" { end += 1 }

        let lines = String(chars[start..<end]).components(separatedBy: "\n")
        let pattern = numbered ? #"^\s*\d+[.)]\s+"# : #"^\s*[-*]\s+"#
        let hasMarker: (String) -> Bool = { $0.range(of: pattern, options: .regularExpression) != nil }
        let nonBlank = lines.filter { !$0.trimmingCharacters(in: .whitespaces).isEmpty }
        let removing = !nonBlank.isEmpty && nonBlank.allSatisfy(hasMarker)

        var number = 0
        let updated = lines.map { line -> String in
            if line.trimmingCharacters(in: .whitespaces).isEmpty { return line }
            if removing {
                return line.replacingOccurrences(of: pattern, with: "", options: .regularExpression)
            }
            // Swap the other list style for this one rather than stacking markers.
            let other = numbered ? #"^\s*[-*]\s+"# : #"^\s*\d+[.)]\s+"#
            let bare = hasMarker(line) ? line : line.replacingOccurrences(of: other, with: "", options: .regularExpression)
            if hasMarker(bare) { return bare }
            number += 1
            return (numbered ? "\(number). " : "- ") + bare
        }.joined(separator: "\n")

        var result = chars
        result.replaceSubrange(start..<end, with: Array(updated))
        return (String(result), start..<(start + updated.count))
    }
}

extension String {
    /// The index `offset` characters in, clamped to the end.
    func index(at offset: Int) -> String.Index {
        index(startIndex, offsetBy: max(0, offset), limitedBy: endIndex) ?? endIndex
    }
}

import SwiftUI
import UIKit
import ComposeApp

extension Array where Element == TextRun {
    /// The runs as one `AttributedString`: bold/italic spans, and links tinted and underlined.
    /// Taps on links reach the view's `openURL` action (see `RecipeDetailView.handleLink`).
    var attributed: AttributedString {
        var result = AttributedString()
        for run in self {
            var piece = AttributedString(run.text)
            // Presentation intents compose with the surrounding Text's font, unlike a fixed Font.
            var intent: InlinePresentationIntent = []
            if run.isBold { intent.insert(.stronglyEmphasized) }
            if run.isItalic { intent.insert(.emphasized) }
            if !intent.isEmpty { piece.inlinePresentationIntent = intent }
            if let url = run.url.flatMap(URL.init(string:)) {
                piece.link = url
                piece.foregroundColor = ChefTheme.primary
                piece.underlineStyle = .single
            }
            result.append(piece)
        }
        return result
    }
}

extension RecipeTextLine {
    var attributed: AttributedString { runs.attributed }
}

/// Lays children out left to right, wrapping onto new rows — the SwiftUI counterpart of Compose's
/// `FlowRow`, used for category chips.
struct FlowLayout: Layout {
    var spacing: CGFloat = ChefTheme.Spacing.small

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = rows(for: subviews, width: proposal.width ?? .infinity)
        let height = rows.map(\.height).reduce(0, +) + spacing * CGFloat(max(0, rows.count - 1))
        let width = rows.map(\.width).max() ?? 0
        return CGSize(width: proposal.width ?? width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var y = bounds.minY
        for row in rows(for: subviews, width: bounds.width) {
            var x = bounds.minX
            for index in row.indices {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
                x += size.width + spacing
            }
            y += row.height + spacing
        }
    }

    private struct Row {
        var indices: [Int] = []
        var width: CGFloat = 0
        var height: CGFloat = 0
    }

    private func rows(for subviews: Subviews, width: CGFloat) -> [Row] {
        var rows: [Row] = [Row()]
        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(.unspecified)
            let needed = rows[rows.count - 1].indices.isEmpty ? size.width : rows[rows.count - 1].width + spacing + size.width
            if needed > width && !rows[rows.count - 1].indices.isEmpty {
                rows.append(Row())
            }
            var row = rows[rows.count - 1]
            row.width = row.indices.isEmpty ? size.width : row.width + spacing + size.width
            row.height = max(row.height, size.height)
            row.indices.append(index)
            rows[rows.count - 1] = row
        }
        return rows
    }
}

/// Presents the system share sheet for content that arrives asynchronously (e.g. a share link the
/// BLoC emits once the recipe is public), where SwiftUI's `ShareLink` can't be triggered.
enum SharePresenter {
    static func share(_ text: String) {
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first(where: { $0.activationState == .foregroundActive }),
              var top = scene.keyWindow?.rootViewController else { return }
        while let presented = top.presentedViewController { top = presented }
        let controller = UIActivityViewController(activityItems: [text], applicationActivities: nil)
        controller.popoverPresentationController?.sourceView = top.view
        controller.popoverPresentationController?.sourceRect = CGRect(
            x: top.view.bounds.midX, y: top.view.bounds.minY + 60, width: 0, height: 0
        )
        top.present(controller, animated: true)
    }
}

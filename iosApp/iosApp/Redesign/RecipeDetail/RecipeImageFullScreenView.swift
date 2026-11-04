import SwiftUI

/// The redesigned `RecipeImageFullScreenScreen`: the recipe photo on black, pinch or double-tap to
/// zoom, pan while zoomed, and drag down (or tap close) to dismiss. Presented as a full-screen cover
/// that zooms out of the hero image.
struct RecipeImageFullScreenView: View {
    let imageUrl: String
    let title: String
    let onDismiss: () -> Void

    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero
    @State private var dismissDrag: CGFloat = 0

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.black
                .opacity(1 - min(0.6, abs(dismissDrag) / 400))
                .ignoresSafeArea()

            AsyncImage(url: URL(string: imageUrl)) { image in
                image.resizable().scaledToFit()
            } placeholder: {
                ProgressView().tint(.white)
            }
            .scaleEffect(scale)
            .offset(x: offset.width, y: offset.height + dismissDrag)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .gesture(magnify.simultaneously(with: drag))
            .onTapGesture(count: 2) {
                withAnimation(.snappy) {
                    if scale > 1 { reset() } else { scale = 2.5; lastScale = 2.5 }
                }
            }
            .accessibilityLabel(title)
            .accessibilityAddTraits(.isImage)

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.headline)
                    .padding(12)
            }
            .buttonStyle(.plain)
            .foregroundStyle(.white)
            .chefGlass(in: Circle())
            .padding(ChefTheme.Spacing.normal)
            .accessibilityLabel("Close")
        }
        .statusBarHidden()
    }

    private var magnify: some Gesture {
        MagnifyGesture()
            .onChanged { value in scale = max(1, min(5, lastScale * value.magnification)) }
            .onEnded { _ in
                lastScale = scale
                if scale <= 1 { withAnimation(.snappy) { reset() } }
            }
    }

    private var drag: some Gesture {
        DragGesture()
            .onChanged { value in
                if scale > 1 {
                    offset = CGSize(width: lastOffset.width + value.translation.width,
                                    height: lastOffset.height + value.translation.height)
                } else {
                    dismissDrag = value.translation.height
                }
            }
            .onEnded { value in
                if scale > 1 {
                    lastOffset = offset
                } else if abs(value.translation.height) > 120 {
                    onDismiss()
                } else {
                    withAnimation(.snappy) { dismissDrag = 0 }
                }
            }
    }

    private func reset() {
        scale = 1
        lastScale = 1
        offset = .zero
        lastOffset = .zero
    }
}

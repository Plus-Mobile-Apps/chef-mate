import SwiftUI
import UIKit

/// The native `CropPhotoOverlay`: pan and pinch a picked photo inside a square frame, then crop it
/// to that square. The result is JPEG, capped at 2048px, the same output Compose hands the BLoC.
struct PhotoCropView: View {
    let image: UIImage
    let onCancel: () -> Void
    let onCrop: (Data) -> Void

    @State private var scale: CGFloat = 1
    @State private var lastScale: CGFloat = 1
    @State private var offset: CGSize = .zero
    @State private var lastOffset: CGSize = .zero

    var body: some View {
        GeometryReader { geometry in
            let side = min(geometry.size.width, geometry.size.height) - ChefTheme.Spacing.large * 2
            let base = baseSize(for: side)
            ZStack {
                Color.black.ignoresSafeArea()
                Image(uiImage: image)
                    .resizable()
                    .frame(width: base.width * scale, height: base.height * scale)
                    .offset(offset)
                    .frame(width: side, height: side)
                    .clipped()
                    .overlay(Rectangle().stroke(.white, lineWidth: 2))
                    .contentShape(Rectangle())
                    .gesture(drag(side: side, base: base).simultaneously(with: magnify(side: side, base: base)))
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .safeAreaInset(edge: .bottom) {
                HStack {
                    Button(ComposeStrings["edit_recipe_crop_cancel"], action: onCancel)
                    Spacer()
                    Button(ComposeStrings["edit_recipe_crop_confirm"]) { crop(side: side, base: base) }
                        .fontWeight(.semibold)
                }
                .foregroundStyle(.white)
                .padding(ChefTheme.Spacing.large)
            }
            .safeAreaInset(edge: .top) {
                Text(ComposeStrings["edit_recipe_crop_title"])
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(ChefTheme.Spacing.normal)
            }
        }
    }

    /// The image's size at scale 1: just covering the square.
    private func baseSize(for side: CGFloat) -> CGSize {
        let ratio = max(side / image.size.width, side / image.size.height)
        return CGSize(width: image.size.width * ratio, height: image.size.height * ratio)
    }

    private func drag(side: CGFloat, base: CGSize) -> some Gesture {
        DragGesture()
            .onChanged { value in
                offset = clamped(CGSize(width: lastOffset.width + value.translation.width,
                                        height: lastOffset.height + value.translation.height), side: side, base: base)
            }
            .onEnded { _ in lastOffset = offset }
    }

    private func magnify(side: CGFloat, base: CGSize) -> some Gesture {
        MagnifyGesture()
            .onChanged { value in
                scale = max(1, min(5, lastScale * value.magnification))
                offset = clamped(offset, side: side, base: base)
            }
            .onEnded { _ in
                lastScale = scale
                lastOffset = offset
            }
    }

    /// Keeps the square fully covered by the image.
    private func clamped(_ proposed: CGSize, side: CGFloat, base: CGSize) -> CGSize {
        let maxX = max(0, (base.width * scale - side) / 2)
        let maxY = max(0, (base.height * scale - side) / 2)
        return CGSize(width: min(maxX, max(-maxX, proposed.width)), height: min(maxY, max(-maxY, proposed.height)))
    }

    private func crop(side: CGFloat, base: CGSize) {
        let upright = image.normalizedOrientation()
        // Points on screen → image pixels.
        let pixelsPerPoint = upright.size.width * upright.scale / (base.width * scale)
        let displayed = CGSize(width: base.width * scale, height: base.height * scale)
        let originX = (displayed.width - side) / 2 - offset.width
        let originY = (displayed.height - side) / 2 - offset.height
        let rect = CGRect(x: originX * pixelsPerPoint, y: originY * pixelsPerPoint,
                          width: side * pixelsPerPoint, height: side * pixelsPerPoint).integral
        guard let cgImage = upright.cgImage?.cropping(to: rect) else { return onCancel() }
        let target = min(rect.width, 2048)
        let renderer = UIGraphicsImageRenderer(size: CGSize(width: target, height: target), format: {
            let format = UIGraphicsImageRendererFormat()
            format.scale = 1
            return format
        }())
        let output = renderer.jpegData(withCompressionQuality: 0.85) { _ in
            UIImage(cgImage: cgImage).draw(in: CGRect(x: 0, y: 0, width: target, height: target))
        }
        onCrop(output)
    }
}

private extension UIImage {
    /// The image redrawn upright, so pixel crops line up with what was shown.
    func normalizedOrientation() -> UIImage {
        guard imageOrientation != .up else { return self }
        let format = UIGraphicsImageRendererFormat()
        format.scale = scale
        return UIGraphicsImageRenderer(size: size, format: format).image { _ in draw(in: CGRect(origin: .zero, size: size)) }
    }
}

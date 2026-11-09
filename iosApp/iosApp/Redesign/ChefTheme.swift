import SwiftUI

/// SwiftUI mirror of `ChefMateTheme` (client/ui/public/.../theme). Keep the two in sync.
enum ChefTheme {
    /// `primary`: purple500 in light, purple200 in dark.
    static let primary = Color(UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0xBB / 255, green: 0x86 / 255, blue: 0xFC / 255, alpha: 1)
            : UIColor(red: 0x62 / 255, green: 0x00 / 255, blue: 0xEE / 255, alpha: 1)
    })
    /// `onPrimary`: white in light, Material 3's dark default (#381E72) in dark, where `primary` is
    /// too light for white text.
    static let onPrimary = Color(UIColor { traits in
        traits.userInterfaceStyle == .dark
            ? UIColor(red: 0x38 / 255, green: 0x1E / 255, blue: 0x72 / 255, alpha: 1)
            : .white
    })
    /// `secondary`: teal200.
    static let secondary = Color(red: 0x03 / 255, green: 0xDA / 255, blue: 0xC5 / 255)

    /// `AppDimensions` spacing tokens.
    enum Spacing {
        static let extraSmall: CGFloat = 4
        static let small: CGFloat = 8
        static let normal: CGFloat = 16
        static let large: CGFloat = 24
        static let extraLarge: CGFloat = 32
        static let rowHeight: CGFloat = 56
    }
}

extension View {
    /// Liquid Glass on iOS 26, a material on earlier versions. For custom floating controls only:
    /// standard bars, tab bars, and sheets get glass from the system.
    @ViewBuilder
    func chefGlass<S: Shape>(in shape: S = Capsule()) -> some View {
        if #available(iOS 26.0, *) {
            glassEffect(.regular.interactive(), in: shape)
        } else {
            background(.regularMaterial, in: shape)
        }
    }
}

import SwiftUI
import UIKit
import ComposeApp

extension View {
    /// The ChefMate Premium upsell a non-subscriber sees on a gated entry point: an alert offering
    /// "Not now" / "Subscribe", where Subscribe opens RevenueCat's paywall full-screen. Mirrors
    /// Compose's `PremiumRequiredDialog`.
    ///
    /// `isPresented` is the screen BLoC's "premium prompt visible" flag; its setter must tell the
    /// BLoC the prompt was dismissed. A purchase needs no handling here — it reaches every gate
    /// through `SubscriptionRepository.isSubscribed`.
    func premiumUpsell(isPresented: Binding<Bool>) -> some View {
        modifier(PremiumUpsell(isPresented: isPresented))
    }
}

private struct PremiumUpsell: ViewModifier {
    @Binding var isPresented: Bool
    @State private var showPaywall = false

    func body(content: Content) -> some View {
        content
            .alert(ComposeStrings["premium_required_title"], isPresented: $isPresented) {
                // Either button closes the alert, which dismisses the BLoC prompt via the binding.
                Button(ComposeStrings["premium_required_dismiss"], role: .cancel) {}
                Button(ComposeStrings["premium_required_confirm"]) { showPaywall = true }
            } message: {
                Text(ComposeStrings["premium_required_message"])
            }
            .fullScreenCover(isPresented: $showPaywall) {
                PremiumPaywallHost { showPaywall = false }
                    .ignoresSafeArea()
            }
    }
}

/// Hosts the Compose `PremiumPaywallScreen` (RevenueCat's paywall, or its "unavailable" fallback in
/// a build without a RevenueCat key).
private struct PremiumPaywallHost: UIViewControllerRepresentable {
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        PremiumPaywallViewController.shared.create(onDismiss: onDismiss)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

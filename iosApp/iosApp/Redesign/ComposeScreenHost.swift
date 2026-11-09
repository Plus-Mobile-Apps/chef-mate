import SwiftUI
import UIKit
import ComposeApp

/// Renders a not-yet-redesigned Compose screen inside the SwiftUI shell. The Compose screen draws
/// its own app bar, so the native navigation bar is hidden.
struct ComposeScreenHost: View {
    let screen: PublicComposeScreen
    /// Safe-area edges Compose draws under and insets itself. Tab content keeps the bottom edge so
    /// it isn't covered by the tab bar.
    var edges: Edge.Set = .all

    var body: some View {
        Representable(screen: screen)
            .ignoresSafeArea(.container, edges: edges)
            .ignoresSafeArea(.keyboard)
            .toolbar(.hidden, for: .navigationBar)
    }

    private struct Representable: UIViewControllerRepresentable {
        let screen: PublicComposeScreen

        func makeUIViewController(context: Context) -> UIViewController {
            ComposeScreenViewController.shared.create(screen: screen)
        }

        func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
    }
}

/// Keeps the edge-swipe back gesture working on screens that hide the navigation bar (every hosted
/// Compose screen). UIKit disables it with the bar hidden unless the recognizer has a delegate.
extension UINavigationController: @retroactive UIGestureRecognizerDelegate {
    override open func viewDidLoad() {
        super.viewDidLoad()
        interactivePopGestureRecognizer?.delegate = self
    }

    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        viewControllers.count > 1 && !SwipeBackGate.isBlocked
    }
}

/// Lets a screen veto the edge-swipe back gesture while it's showing, so back always goes through
/// its BLoC (e.g. the recipe editor's discard-changes prompt). Apply with `.blocksSwipeBack()`.
enum SwipeBackGate {
    fileprivate(set) static var isBlocked = false
}

extension View {
    func blocksSwipeBack() -> some View {
        onAppear { SwipeBackGate.isBlocked = true }
            .onDisappear { SwipeBackGate.isBlocked = false }
    }
}

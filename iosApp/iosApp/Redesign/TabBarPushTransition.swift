import SwiftUI
import UIKit

/// Makes the system tab bar slide out with a push and back in with a pop, tracking the
/// interactive swipe-back gesture, like UIKit's `hidesBottomBarWhenPushed` (which SwiftUI's tab
/// controller ignores; `.toolbar(.hidden, for: .tabBar)` alone just snaps the bar away on a push
/// and back once a pop has finished).
///
/// Pushed screens still hide the real bar with `.toolbar(.hidden, for: .tabBar)`, so SwiftUI keeps
/// owning the bar and the safe areas. This only adds the motion: as a screen is pushed over a tab
/// root, a snapshot of the bar is pinned to the root's view, so the push carries it off screen with
/// the root and a pop (or swipe back) brings it back in with the root. Once the root is fully on
/// screen again the real bar is back, and the snapshot is dropped.
///
/// Attach to a tab's root content with `.background(TabBarPushTransition())`.
struct TabBarPushTransition: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> Controller { Controller() }
    func updateUIViewController(_ controller: Controller, context: Context) {}

    final class Controller: UIViewController {
        /// The bar snapshot riding on the root's view while a pushed screen covers it.
        private var ghost: UIView?

        override func loadView() {
            view = UIView()
            view.isUserInteractionEnabled = false
        }

        /// The controller the navigation stack pushes over: this controller's ancestor that is a
        /// direct child of the `UINavigationController`.
        private var stackRoot: UIViewController? {
            var controller: UIViewController? = self
            while let current = controller, !(current.parent is UINavigationController) {
                controller = current.parent
            }
            return controller
        }

        override func viewWillDisappear(_ animated: Bool) {
            super.viewWillDisappear(animated)
            // Only a push covers the root; switching tabs or presenting a sheet doesn't.
            guard let root = stackRoot, let nav = root.navigationController, nav.topViewController !== root
            else { return }
            installGhost(on: root)
        }

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            // A completed pop: the real bar is showing again. (A cancelled swipe back never gets
            // here, so the snapshot stays for the next attempt.)
            ghost?.removeFromSuperview()
            ghost = nil
        }

        private func installGhost(on root: UIViewController) {
            // While the bar is already hidden (the root reappearing under a cancelled swipe back),
            // keep the snapshot taken when the screen was first pushed.
            guard let window = view.window, let tabBar = tabBarController?.tabBar, !tabBar.isHidden,
                  tabBar.window != nil
            else { return }
            let rect = tabBar.convert(tabBar.bounds, to: window)
            // A window snapshot rather than the bar's own: the glass is drawn from what's behind
            // it, which a view-only snapshot can't capture.
            guard let snapshot = window.resizableSnapshotView(
                from: rect, afterScreenUpdates: false, withCapInsets: .zero
            ) else { return }
            ghost?.removeFromSuperview()
            snapshot.frame = window.convert(rect, to: root.view)
            snapshot.autoresizingMask = [.flexibleTopMargin, .flexibleWidth]
            snapshot.isUserInteractionEnabled = false
            snapshot.accessibilityElementsHidden = true
            root.view.addSubview(snapshot)
            ghost = snapshot
        }
    }
}


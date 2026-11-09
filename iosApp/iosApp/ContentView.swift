import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let rootBloc: RootBloc
    let backDispatcher: BackDispatcher
    
    func makeUIViewController(context: Context) -> UIViewController {
        let controller = MainViewController.shared.create(rootBloc: rootBloc, backDispatcher: backDispatcher)
        controller.overrideUserInterfaceStyle = .light
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    let appDelegate: AppDelegate

    /// Picks the UI shell once per launch. `root` is built in `didFinishLaunchingWithOptions`, so
    /// the flag snapshot is already taken by the time SwiftUI asks for the body.
    @ViewBuilder
    private var shell: some View {
        if RootBlocProvider.shared.useSwiftUiRedesign {
            RedesignRootView(appDelegate: appDelegate)
        } else {
            ComposeView(
                rootBloc: appDelegate.root,
                backDispatcher: appDelegate.backDispatcher
            )
            .ignoresSafeArea(.all)
            .ignoresSafeArea(.keyboard)
        }
    }

    var body: some View {
        shell
        // SwiftUI delivers BOTH custom-scheme links (chefmate://…) and https Universal Links
        // (https://chefmate.plusmobileapps.com/notifications from the invite emails) here, for
        // cold and warm launches alike. The invite deep link previously fell through because this
        // only forwarded scheme == "chefmate" and dropped the https URL.
        .onOpenURL { url in
            // The share extension re-enters via chefmate://import?url=… — hand that to the browser.
            if url.scheme == "chefmate", url.host == "import",
               let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
               let sharedUrl = components.queryItems?.first(where: { $0.name == "url" })?.value {
                appDelegate.root.handleSharedUrl(url: sharedUrl)
                return
            }
            // Any other deep link — custom scheme or https Universal Link. DeepLink.parse validates
            // the host/scheme and yields DeepLink.None (a no-op) for anything unrecognized.
            appDelegate.root.handleDeepLink(url: url.absoluteString)
        }
        // Secondary net: some iOS versions surface Universal Links as a browsing NSUserActivity
        // through this modifier instead of .onOpenURL. Whichever fires, handleDeepLink is
        // idempotent (it just brings the destination to the front), so double routing is harmless.
        .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
            if let url = activity.webpageURL {
                appDelegate.root.handleDeepLink(url: url.absoluteString)
            }
        }
    }
}




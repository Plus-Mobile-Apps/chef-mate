import AppIntents
import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    // Must live on the `App` struct: `@UIApplicationDelegateAdaptor` is only honored here, not on a
    // child `View`. Placed on a `View` it silently no-ops ("used outside of an App or Scene"), so
    // `didFinishLaunchingWithOptions` never fires and the watch data bridge never starts.
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate: AppDelegate

    var body: some Scene {
        WindowGroup {
            ContentView(appDelegate: appDelegate)
        }
    }
}

class AppDelegate: NSObject, UIApplicationDelegate {
    var backDispatcher: BackDispatcher = BackDispatcherKt.BackDispatcher()
    private var launchDeepLinkUrl: String?

    /// Serves grocery data to the watch companion. Lazily created after `root` builds the DI graph
    /// (which sets `RootBlocProvider.watchDataBridge`).
    private lazy var watchDataSender = WatchDataSender(
        bridge: RootBlocProvider.shared.watchDataBridge
    )

    override init() {
        super.init()
        BugsnagStartup_iosKt.initializeBugsnag()
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        if let url = launchOptions?[.url] as? URL,
           url.scheme == "chefmate",
           url.host != "import" {
            launchDeepLinkUrl = url.absoluteString
        }
        // Build the graph (which sets watchDataBridge), then start the watch data bridge.
        _ = root
        _ = watchDataSender
        // Re-index the grocery names Siri listens for in "Add <item> to Chef Mate".
        ChefMateAppShortcuts.updateAppShortcutParameters()
        return true
    }

    lazy var root: RootBloc = RootBlocProvider.shared.buildRootBloc(
        componentContext: DefaultComponentContext(
            lifecycle: ApplicationLifecycle(),
            stateKeeper: nil,
            instanceKeeper: nil,
            backHandler: backDispatcher
        ),
        application: UIApplication.shared,
        deepLinkUrl: launchDeepLinkUrl
    )
}

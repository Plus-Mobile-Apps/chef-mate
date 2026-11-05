import SwiftUI
import ComposeApp

/// The redesigned Browser tab, mirroring the Android `BrowserRootScreen`: the browser's own stack
/// (engine picker → landing → edit query → page) rendered in place with a crossfade, and the
/// address bar morphing between the screens that show it.
///
/// Like Compose, the browser's stack is not flattened into the root `NavigationStack`: its screens
/// replace one another rather than push, and the tab bar stays visible throughout.
struct BrowserRootView: View {
    let bloc: BrowserRootBloc
    /// Shown as a pushed root child (a web link opened from elsewhere in the app) rather than as
    /// the Browser tab: keeps the navigation bar for its back button and adds "Open in browser".
    var isModal = false
    @StateObject private var stack: ObservedValue<ChildStack<AnyObject, BrowserRootBlocChild>>
    @Namespace private var addressBar

    init(bloc: BrowserRootBloc, isModal: Bool = false) {
        self.bloc = bloc
        self.isModal = isModal
        _stack = StateObject(wrappedValue: ObservedValue(bloc.routerState))
    }

    var body: some View {
        let active = stack.value.active.instance
        ZStack {
            child(active)
                .id(ObjectIdentifier(active))
                .transition(.opacity)
        }
        .animation(.easeInOut(duration: 0.3), value: ObjectIdentifier(active))
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .tint(ChefTheme.primary)
    }

    @ViewBuilder
    private func child(_ child: BrowserRootBlocChild) -> some View {
        switch child {
        case let selectEngine as BrowserRootBlocChild.SelectEngine:
            BrowserSelectEngineView(bloc: selectEngine.bloc)
                .toolbar(.hidden, for: .navigationBar)
        case let landing as BrowserRootBlocChild.Landing:
            BrowserLandingView(bloc: landing.bloc, addressBar: addressBar)
                .toolbar(.hidden, for: .navigationBar)
        case let editQuery as BrowserRootBlocChild.EditQuery:
            BrowserEditQueryView(bloc: editQuery.bloc, addressBar: addressBar)
                .toolbar(.hidden, for: .navigationBar)
        case let browser as BrowserRootBlocChild.Browser:
            BrowserView(bloc: browser.bloc, addressBar: addressBar, isModal: isModal)
        default:
            ComposeScreenHost(screen: child.bloc)
        }
    }
}

/// The address bar's id in `BrowserRootView`'s namespace, shared by every screen that draws it.
let browserAddressBarId = "browser-address-bar"

extension SearchEngine {
    /// The engine's logo in the asset catalog (copied from the Compose `drawable/` resources).
    var logoImageName: String {
        switch self {
        case .duckDuckGo: "SearchEngineDuckDuckGo"
        case .bing: "SearchEngineBing"
        case .brave: "SearchEngineBrave"
        default: "SearchEngineGoogle"
        }
    }

    /// `browser_landing_hint`: "Search {engine} or type a URL".
    var searchHint: String {
        ComposeStrings["browser_landing_hint"].replacingOccurrences(of: "{engine}", with: displayName)
    }
}

/// The capsule every browser screen draws its address / search field in.
struct BrowserFieldBackground: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(.horizontal, ChefTheme.Spacing.normal - 2)
            .frame(minHeight: 48)
            .background(Color(.secondarySystemBackground), in: Capsule())
            .overlay(Capsule().strokeBorder(Color(.separator), lineWidth: 0.5))
    }
}

extension View {
    func browserField() -> some View { modifier(BrowserFieldBackground()) }
}

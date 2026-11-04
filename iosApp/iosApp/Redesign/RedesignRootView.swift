import SwiftUI
import ComposeApp

/// Entry point of the native SwiftUI redesign, shown instead of the Compose UI when the
/// `ios_swiftui_redesign` flag is on (see `RootBlocProvider.useSwiftUiRedesign` and
/// docs/ios-swiftui-redesign.md).
///
/// Renders the Decompose root stack as a `NavigationStack`: the first child is the stack's root
/// (normally the tab bar), every later child is a pushed entry. Decompose stays the source of
/// truth — a native swipe-back or back button only asks `RootBloc` to pop.
struct RedesignRootView: View {
    let appDelegate: AppDelegate
    @StateObject private var stack: ObservedValue<ChildStack<AnyObject, RootBlocChild>>

    init(appDelegate: AppDelegate) {
        self.appDelegate = appDelegate
        _stack = StateObject(wrappedValue: ObservedValue(appDelegate.root.state))
    }

    var body: some View {
        let children = stack.value.items.map { $0.instance }
        if let base = children.first {
            let pushed = children.dropFirst().map(StackEntry.init)
            let path = Binding<[StackEntry]>(
                get: { pushed },
                set: { newPath in
                    // Only pops originate from SwiftUI; pushes always come from the BLoCs.
                    for _ in 0..<max(0, pushed.count - newPath.count) {
                        appDelegate.root.onBackClicked()
                    }
                }
            )
            if let bottomNav = base as? RootBlocChild.BottomNavigation {
                BottomNavView(bloc: bottomNav.bloc, path: path)
            } else {
                NavigationStack(path: path) {
                    RootChildView(child: base)
                        .navigationDestination(for: StackEntry.self) { RootChildView(child: $0.child) }
                }
            }
        }
    }
}

/// One pushed root-stack child, identified by its (stable) Decompose instance.
struct StackEntry: Hashable {
    let child: RootBlocChild

    static func == (lhs: StackEntry, rhs: StackEntry) -> Bool { lhs.child === rhs.child }
    func hash(into hasher: inout Hasher) { hasher.combine(ObjectIdentifier(child)) }
}

/// Native view for a root-stack child, or its Compose screen until that screen is redesigned.
struct RootChildView: View {
    let child: RootBlocChild

    var body: some View {
        ComposeScreenHost(screen: child.bloc)
    }
}

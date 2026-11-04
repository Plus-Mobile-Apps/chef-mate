import SwiftUI
import ComposeApp

/// Entry point of the native SwiftUI redesign, shown instead of the Compose UI when the
/// `ios_swiftui_redesign` flag is on (see `RootBlocProvider.useSwiftUiRedesign` and
/// docs/ios-swiftui-redesign.md).
///
/// Renders the Decompose root stack, with redesigned nested stacks flattened into it (see
/// `RootStackModel`), as one `NavigationStack`: the first entry is the stack's root (normally the
/// tab bar), every later entry is pushed. Decompose stays the source of truth — a native swipe-back
/// or back button only asks the entry's owning BLoC to pop.
struct RedesignRootView: View {
    let appDelegate: AppDelegate
    @StateObject private var stack: RootStackModel

    init(appDelegate: AppDelegate) {
        self.appDelegate = appDelegate
        _stack = StateObject(wrappedValue: RootStackModel(root: appDelegate.root))
    }

    var body: some View {
        let entries = stack.entries
        if let base = entries.first {
            let pushed = Array(entries.dropFirst())
            let path = Binding<[StackEntry]>(
                get: { pushed },
                set: { newPath in
                    // Only pops originate from SwiftUI; pushes always come from the BLoCs.
                    stack.pop(count: pushed.count - newPath.count)
                }
            )
            let bottomNav = base.rootChild as? RootBlocChild.BottomNavigation
            Group {
                if let bottomNav {
                    BottomNavView(bloc: bottomNav.bloc, path: path)
                } else {
                    NavigationStack(path: path) {
                        StackEntryView(entry: base)
                            .navigationDestination(for: StackEntry.self) { StackEntryView(entry: $0) }
                    }
                }
            }
            .globalHosts(
                toastService: RootBlocProvider.shared.toastService,
                reviewService: RootBlocProvider.shared.inAppReviewService,
                // Pushed entries hide the tab bar; only the bare tab content shows it.
                bottomInset: bottomNav != nil && pushed.isEmpty ? ChefTabBar.height : 0
            )
        }
    }
}

/// The Decompose root stack flattened into navigation entries. Most root children are one entry.
/// A child whose own nested stack is redesigned (so far `RecipeRoot`) contributes one entry per
/// nested child instead, so its screens push and swipe back natively rather than inside a hosted
/// Compose stack. Each entry remembers which BLoC pops it.
final class RootStackModel: ObservableObject {
    @Published private(set) var entries: [StackEntry] = []
    private let root: RootBloc
    private var rootCancellation: Cancellation?
    private var nestedCancellations: [ObjectIdentifier: Cancellation] = [:]
    private var isRebuilding = false

    init(root: RootBloc) {
        self.root = root
        rootCancellation = root.state.subscribe(observer_: { [weak self] _ in self?.rebuild() })
    }

    deinit {
        rootCancellation?.cancel()
        nestedCancellations.values.forEach { $0.cancel() }
    }

    /// Pops the top `count` entries, newest first, each through the BLoC that owns it.
    func pop(count: Int) {
        guard count > 0 else { return }
        for entry in entries.suffix(count).reversed() {
            switch entry.popper {
            case .root: root.onBackClicked()
            case .recipeRoot(let bloc): bloc.onBackClicked()
            }
        }
    }

    private func rebuild() {
        // Subscribing to a nested stack delivers its current value synchronously; the outer pass
        // already reads that value, so skip the re-entrant call.
        guard !isRebuilding else { return }
        isRebuilding = true
        defer { isRebuilding = false }

        var result: [StackEntry] = []
        var liveNested = Set<ObjectIdentifier>()
        for item in root.state.value.items {
            let child = item.instance
            if let recipeRoot = child as? RootBlocChild.RecipeRoot {
                let bloc = recipeRoot.bloc
                let key = ObjectIdentifier(bloc)
                liveNested.insert(key)
                if nestedCancellations[key] == nil {
                    nestedCancellations[key] = bloc.routerState.subscribe(observer_: { [weak self] _ in self?.rebuild() })
                }
                for (index, nested) in bloc.routerState.value.items.enumerated() {
                    // The nested stack's own root can't pop itself; the root stack removes the
                    // whole RecipeRoot instead.
                    result.append(StackEntry(content: .recipe(nested.instance), popper: index == 0 ? .root : .recipeRoot(bloc)))
                }
            } else {
                result.append(StackEntry(content: .root(child), popper: .root))
            }
        }
        for (key, cancellation) in nestedCancellations where !liveNested.contains(key) {
            cancellation.cancel()
            nestedCancellations[key] = nil
        }
        entries = result
    }
}

/// One navigation entry, identified by its (stable) Decompose child instance.
struct StackEntry: Hashable {
    enum Content {
        case root(RootBlocChild)
        case recipe(RecipeRootBlocChild)
    }

    enum Popper {
        case root
        case recipeRoot(RecipeRootBloc)
    }

    let content: Content
    let popper: Popper

    var rootChild: RootBlocChild? {
        if case .root(let child) = content { child } else { nil }
    }

    private var instance: AnyObject {
        switch content {
        case .root(let child): child
        case .recipe(let child): child
        }
    }

    static func == (lhs: StackEntry, rhs: StackEntry) -> Bool { lhs.instance === rhs.instance }
    func hash(into hasher: inout Hasher) { hasher.combine(ObjectIdentifier(instance)) }
}

/// Native view for a navigation entry, or its Compose screen until that screen is redesigned.
struct StackEntryView: View {
    let entry: StackEntry

    var body: some View {
        switch entry.content {
        case .root(let child):
            ComposeScreenHost(screen: child.bloc)
        case .recipe(let child):
            if let detail = child as? RecipeRootBlocChild.Detail {
                RecipeDetailView(bloc: detail.bloc)
            } else {
                ComposeScreenHost(screen: child.bloc)
            }
        }
    }
}

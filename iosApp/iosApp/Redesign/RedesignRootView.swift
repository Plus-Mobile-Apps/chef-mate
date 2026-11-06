import SwiftUI
import ComposeApp

/// Entry point of the native SwiftUI redesign, shown instead of the Compose UI when the
/// `ios_swiftui_redesign` flag is on (see `RootBlocProvider.useSwiftUiRedesign` and
/// docs/ios-swiftui-redesign.md).
///
/// Renders the Decompose root stack, with redesigned nested stacks flattened into it (see
/// `RootStackModel`), as one `NavigationStack`: the first entry is the stack's root (normally the
/// tab bar), every later entry is pushed. A modal root child (so far `AiChat`, `CookMode`) and everything above
/// it go into a full-screen cover with its own `NavigationStack` instead. Decompose stays the source
/// of truth — a native swipe-back or back button only asks the entry's owning BLoC to pop.
struct RedesignRootView: View {
    let appDelegate: AppDelegate
    @StateObject private var stack: RootStackModel

    init(appDelegate: AppDelegate) {
        self.appDelegate = appDelegate
        _stack = StateObject(wrappedValue: RootStackModel(root: appDelegate.root))
    }

    var body: some View {
        let entries = stack.mainEntries
        if let base = entries.first {
            // A trailing sheet entry (e.g. the recipe book form) is presented over the stack
            // instead of pushed onto it, like the Android modal slide-up.
            let sheetEntry = entries.count > 1 && entries.last?.presentsAsSheet == true ? entries.last : nil
            let pushed = Array(entries.dropFirst().dropLast(sheetEntry == nil ? 0 : 1))
            let isModalShown = stack.modal != nil
            let path = Binding<[StackEntry]>(
                get: { pushed },
                set: { newPath in
                    // Only pops originate from SwiftUI; pushes always come from the BLoCs. The
                    // stack under a modal or sheet can't be popped from the UI while it is covered.
                    guard !isModalShown, sheetEntry == nil else { return }
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
            // `item:` keeps the flow on screen while the sheet animates away after the BLoC closes it.
            .sheet(item: Binding(
                get: { stack.mealPlanner.map(ModalBloc.init) },
                set: { item in
                    // A swipe-down closes the whole add-meal flow, whichever step it was on.
                    if item == nil && stack.mealPlanner != nil { appDelegate.root.onBackClicked() }
                }
            )) { modal in
                MealPlannerSheet(bloc: modal.bloc)
            }
            .sheet(item: Binding(
                get: { sheetEntry },
                set: { newValue in
                    // A swipe-down dismisses natively; pop it through its BLoC. Skip if the BLoC
                    // already removed it (e.g. after Save), so nothing else gets popped.
                    if newValue == nil, let sheetEntry, stack.entries.last == sheetEntry { stack.pop(count: 1) }
                }
            )) { entry in
                NavigationStack {
                    StackEntryView(entry: entry)
                }
            }
            .globalHosts(
                toastService: RootBlocProvider.shared.toastService,
                reviewService: RootBlocProvider.shared.inAppReviewService,
                // Pushed entries hide the tab bar; only the bare tab content shows it.
                bottomInset: bottomNav != nil && pushed.isEmpty ? BottomNavView.tabBarHeight : 0,
                // A full-screen cover hides this overlay; the cover shows toasts itself.
                showsToasts: !isModalShown
            )
            .fullScreenCover(item: Binding(get: { stack.modal }, set: { _ in })) { _ in
                ModalStackView(stack: stack)
                    .toastOverlay(toastService: RootBlocProvider.shared.toastService)
            }
        }
    }

}

/// The navigation stack inside the full-screen cover: the modal root child's entries plus anything
/// pushed above it (e.g. the recipe editor opened by the AI chat's "Add recipe").
private struct ModalStackView: View {
    @ObservedObject var stack: RootStackModel

    var body: some View {
        // While the cover animates away the modal is already gone from the stack; keep showing
        // its last entries rather than an empty screen.
        let entries = stack.modalEntries.isEmpty ? stack.lastModalEntries : stack.modalEntries
        if let base = entries.first {
            let pushed = Array(entries.dropFirst())
            NavigationStack(path: Binding(
                get: { pushed },
                set: { newPath in stack.pop(count: pushed.count - newPath.count) }
            )) {
                StackEntryView(entry: base)
                    // A nested stack can replace its root (e.g. "New conversation"); give the new
                    // BLoC a fresh view so its observers aren't the old one's.
                    .id(base)
                    .navigationDestination(for: StackEntry.self) { StackEntryView(entry: $0) }
            }
        }
    }
}

/// The modal root child currently presented in a full-screen cover.
struct ModalPresentation: Identifiable {
    let owner: RootBlocChild
    var id: ObjectIdentifier { ObjectIdentifier(owner) }
}

/// The Decompose root stack flattened into navigation entries. Most root children are one entry.
/// A child whose own nested stack is redesigned (`RecipeRoot`, `AiChat`, `ManageFamily`,
/// `SettingsRoot`) contributes one entry per nested child instead, so its screens push and swipe back natively rather than inside a hosted
/// Compose stack. Each entry remembers which BLoC pops it.
///
/// The entries split at the first modal root child (the Android `isModal` set; so far
/// `AiChat` and `CookMode` are native): those below it are the main navigation stack, the rest are presented in a
/// full-screen cover. `MealPlanner` is the exception: it presents as a sheet (`MealPlannerSheet`),
/// and it and anything above it are left out of `entries`.
final class RootStackModel: ObservableObject {
    @Published private(set) var entries: [StackEntry] = []
    /// Index of the first entry belonging to a modal root child, if one is showing.
    private var modalStart: Int?
    /// The last non-empty `modalEntries`, shown while the cover animates away.
    private(set) var lastModalEntries: [StackEntry] = []
    /// The add-meal flow while it is open, presented as a sheet.
    @Published private(set) var mealPlanner: MealPlannerRootBloc?

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

    var mainEntries: [StackEntry] { modalStart.map { Array(entries[..<$0]) } ?? entries }
    var modalEntries: [StackEntry] { modalStart.map { Array(entries[$0...]) } ?? [] }
    var modal: ModalPresentation? { modalStart.map { ModalPresentation(owner: entries[$0].owner) } }

    /// Pops the top `count` entries, newest first, each through the BLoC that owns it.
    func pop(count: Int) {
        guard count > 0 else { return }
        for entry in entries.suffix(count).reversed() {
            switch entry.popper {
            case .root: root.onBackClicked()
            case .recipeRoot(let bloc): bloc.onBackClicked()
            case .aiChatRoot(let bloc): bloc.onBackClicked()
            case .editor(let bloc): bloc.onBackClicked()
            case .manageFamily(let bloc): bloc.onBackClicked()
            case .child(let bloc): bloc.onBackClicked()
            case .settingsRoot(let bloc): bloc.onBackClicked()
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
        var modal: MealPlannerRootBloc?
        var liveNested = Set<ObjectIdentifier>()

        /// Adds one entry per child of a nested stack, each popped through `popper`. A nested
        /// stack's own root can't pop itself, so `popper` normally returns `.root` for index 0 and
        /// the root stack removes the whole root child instead.
        func flatten<T: AnyObject>(
            _ owner: RootBlocChild,
            bloc: AnyObject,
            stack: Value<ChildStack<AnyObject, T>>,
            content: (T) -> StackEntry.Content,
            popper: (_ index: Int, _ child: T) -> StackEntry.Popper
        ) {
            let key = ObjectIdentifier(bloc)
            liveNested.insert(key)
            if nestedCancellations[key] == nil {
                nestedCancellations[key] = stack.subscribe(observer_: { [weak self] _ in self?.rebuild() })
            }
            for (index, nested) in stack.value.items.enumerated() {
                result.append(StackEntry(
                    content: content(nested.instance),
                    popper: popper(index, nested.instance),
                    owner: owner
                ))
            }
        }

        for item in root.state.value.items {
            let child = item.instance
            if let mealPlanner = child as? RootBlocChild.MealPlanner {
                modal = mealPlanner.bloc
                break
            } else if let recipeRoot = child as? RootBlocChild.RecipeRoot {
                let bloc = recipeRoot.bloc
                flatten(child, bloc: bloc, stack: bloc.routerState, content: { .recipe($0) }) { index, nested in
                    if let edit = nested as? RecipeRootBlocChild.Edit {
                        // The editor decides: discard prompt when there are changes, else it
                        // closes itself (popping, or finishing a create flow).
                        return .editor(edit.bloc)
                    }
                    return index == 0 ? .root : .recipeRoot(bloc)
                }
            } else if let aiChat = child as? RootBlocChild.AiChat {
                let bloc = aiChat.bloc
                flatten(child, bloc: bloc, stack: bloc.routerState, content: { .aiChat($0) }) { index, _ in
                    return index == 0 ? .root : .aiChatRoot(bloc)
                }
            } else if let manageFamily = child as? RootBlocChild.ManageFamily {
                let bloc = manageFamily.bloc
                flatten(child, bloc: bloc, stack: bloc.routerState, content: { .family($0) }) { index, _ in
                    return index == 0 ? .root : .manageFamily(bloc)
                }
            } else if child is RootBlocChild.Authentication || child is RootBlocChild.OtpVerification,
                      let bloc = child.bloc as? BackClickBloc {
                // Auth screens pop through their own BLoC: its back output also abandons whatever
                // the sign-in was gating (RootBlocImpl.destinationAfterAuth).
                result.append(StackEntry(content: .root(child), popper: .child(bloc), owner: child))
            } else if let settingsRoot = child as? RootBlocChild.SettingsRoot {
                let bloc = settingsRoot.bloc
                flatten(child, bloc: bloc, stack: bloc.routerState, content: { .settings($0) }) { index, _ in
                    return index == 0 ? .root : .settingsRoot(bloc)
                }
            } else {
                result.append(StackEntry(content: .root(child), popper: .root, owner: child))
            }
        }
        for (key, cancellation) in nestedCancellations where !liveNested.contains(key) {
            cancellation.cancel()
            nestedCancellations[key] = nil
        }

        // The stack's own root is never presented modally.
        modalStart = result.indices.dropFirst().first { result[$0].owner.isModal }
        if let modalStart { lastModalEntries = Array(result[modalStart...]) }
        entries = result
        if mealPlanner !== modal { mealPlanner = modal }
    }
}

private extension RootBlocChild {
    /// Root children presented as a full-screen cover once redesigned — the Android `isModal` set
    /// (Browser, MealPlanner, CookMode, AiChat, EditRecipeBook, EditGroceryList). Only the native
    /// ones are listed; hosted ones still push.
    var isModal: Bool { self is RootBlocChild.AiChat || self is RootBlocChild.CookMode }
}

/// A modal root child's BLoC, identified by instance, for `.sheet(item:)`.
struct ModalBloc<Bloc: AnyObject>: Identifiable {
    let bloc: Bloc
    var id: ObjectIdentifier { ObjectIdentifier(bloc) }
}

/// One navigation entry, identified by its (stable) Decompose child instance.
struct StackEntry: Hashable {
    enum Content {
        case root(RootBlocChild)
        case recipe(RecipeRootBlocChild)
        case aiChat(AiChatRootBlocChild)
        case family(ManageFamilyRootBlocChild)
        case settings(SettingsRootBlocChild)
    }

    enum Popper {
        case root
        case recipeRoot(RecipeRootBloc)
        case aiChatRoot(AiChatRootBloc)
        case editor(EditRecipeBloc)
        case manageFamily(ManageFamilyRootBloc)
        /// A root child whose own back handler pops it (and may do more than a plain pop).
        case child(BackClickBloc)
        case settingsRoot(SettingsRootBloc)
    }

    let content: Content
    let popper: Popper
    /// The root-stack child this entry belongs to (itself, or the owner of its nested stack).
    let owner: RootBlocChild

    var rootChild: RootBlocChild? {
        if case .root(let child) = content { child } else { nil }
    }

    private var instance: AnyObject {
        switch content {
        case .root(let child): child
        case .recipe(let child): child
        case .aiChat(let child): child
        case .family(let child): child
        case .settings(let child): child
        }
    }

    /// Presented as a sheet over the stack rather than pushed (see `RedesignRootView`). Only
    /// native modal screens; hosted ones still push until their own PR.
    var presentsAsSheet: Bool {
        rootChild is RootBlocChild.EditRecipeBook || rootChild is RootBlocChild.EditGroceryList
    }

    static func == (lhs: StackEntry, rhs: StackEntry) -> Bool { lhs.instance === rhs.instance }
    func hash(into hasher: inout Hasher) { hasher.combine(ObjectIdentifier(instance)) }
}

extension StackEntry: Identifiable {
    var id: ObjectIdentifier { ObjectIdentifier(instance) }
}

/// Native view for a navigation entry, or its Compose screen until that screen is redesigned.
struct StackEntryView: View {
    let entry: StackEntry

    var body: some View {
        switch entry.content {
        case .root(let child):
            if let cookMode = child as? RootBlocChild.CookMode {
                CookModeView(bloc: cookMode.bloc)
            } else if let browser = child as? RootBlocChild.Browser {
                BrowserRootView(bloc: browser.bloc, isModal: true)
            } else if let auth = child as? RootBlocChild.Authentication {
                AuthenticationView(bloc: auth.bloc)
            } else if let otp = child as? RootBlocChild.OtpVerification {
                OtpView(bloc: otp.bloc)
            } else if let onboarding = child as? RootBlocChild.Onboarding {
                OnboardingView(bloc: onboarding.bloc)
            } else if let publicRecipe = child as? RootBlocChild.PublicRecipe {
                PublicRecipeView(bloc: publicRecipe.bloc)
            } else if let export = child as? RootBlocChild.ExportRecipes {
                ExportRecipesView(bloc: export.bloc)
            } else if let editBook = child as? RootBlocChild.EditRecipeBook {
                EditRecipeBookView(bloc: editBook.bloc)
            } else if let editList = child as? RootBlocChild.EditGroceryList {
                EditGroceryListView(bloc: editList.bloc)
            } else if let profile = child as? RootBlocChild.ManageProfile {
                ManageProfileView(bloc: profile.bloc)
            } else if let notifications = child as? RootBlocChild.Notifications {
                NotificationsView(bloc: notifications.bloc)
            } else if let devSettings = child as? RootBlocChild.DeveloperSettings {
                DeveloperSettingsView(bloc: devSettings.bloc)
            } else if let flags = child as? RootBlocChild.FeatureFlags {
                FeatureFlagsView(bloc: flags.bloc)
            } else {
                ComposeScreenHost(screen: child.bloc)
            }
        case .recipe(let child):
            if let detail = child as? RecipeRootBlocChild.Detail {
                RecipeDetailView(bloc: detail.bloc)
            } else if let edit = child as? RecipeRootBlocChild.Edit {
                EditRecipeView(bloc: edit.bloc)
            } else {
                ComposeScreenHost(screen: child.bloc)
            }
        case .aiChat(let child):
            if let chat = child as? AiChatRootBlocChild.Chat {
                AiChatView(bloc: chat.bloc)
            } else if let history = child as? AiChatRootBlocChild.History {
                AiChatHistoryView(bloc: history.bloc)
            } else {
                ComposeScreenHost(screen: child.bloc)
            }
        case .family(let child):
            if let list = child as? ManageFamilyRootBlocChild.FamilyList {
                FamilyListView(bloc: list.bloc)
            } else if let detail = child as? ManageFamilyRootBlocChild.FamilyDetail {
                FamilyDetailView(bloc: detail.bloc)
            }
        case .settings(let child):
            switch child {
            case let categories as SettingsRootBlocChild.RecipeCategories:
                RecipeCategoriesView(bloc: categories.bloc)
            case let export as SettingsRootBlocChild.ExportRecipes:
                ExportRecipesView(bloc: export.bloc)
            case let importRecipes as SettingsRootBlocChild.ImportRecipes:
                ImportRecipesView(bloc: importRecipes.bloc)
            case let appSettings as SettingsRootBlocChild.AppSettings:
                AppSettingsView(bloc: appSettings.bloc)
            case let navOrder as SettingsRootBlocChild.BottomNavOrder:
                BottomNavOrderView(bloc: navOrder.bloc)
            case let autocomplete as SettingsRootBlocChild.GroceryAutocomplete:
                GroceryAutocompleteView(bloc: autocomplete.bloc)
            case let rules as SettingsRootBlocChild.GroceryCategoryRules:
                GroceryCategoryRulesView(bloc: rules.bloc)
            case let bookmarks as SettingsRootBlocChild.BrowserBookmarks:
                BrowserBookmarksView(bloc: bookmarks.bloc)
            default:
                ComposeScreenHost(screen: child.bloc)
            }
        }
    }
}

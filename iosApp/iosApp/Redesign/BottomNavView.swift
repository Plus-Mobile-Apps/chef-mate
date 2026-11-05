import SwiftUI
import ComposeApp

/// The redesigned bottom navigation. Tab order and selection come from `BottomNavBloc`; each tab
/// owns the `NavigationStack` that root-stack children push onto.
///
/// Like the Android bottom nav, the tab bar only shows on the tab roots. A `TabView` keeps the
/// tabs, but its system bar is hidden and `ChefTabBar` is drawn inside each tab's root screen
/// instead, so it slides out with a push and back in with a pop, tracking the swipe-back gesture
/// (UIKit's `hidesBottomBarWhenPushed` look). Hiding the system bar per pushed screen with
/// `.toolbar(.hidden, for: .tabBar)` made it snap back only after the pop finished, and SwiftUI's
/// tab controller ignores `hidesBottomBarWhenPushed`.
struct BottomNavView: View {
    let bloc: BottomNavBloc
    @Binding var path: [StackEntry]
    @StateObject private var model: ObservedFlow<BottomNavBlocModel>
    @StateObject private var content: ObservedValue<ChildStack<AnyObject, BottomNavBlocChild>>
    /// True while the software keyboard is up. The bar is a `safeAreaInset`, which SwiftUI lays
    /// out above the keyboard, so it would otherwise ride up on top of it; like the system tab bar
    /// (and the Android nav pill) it should stay behind the keyboard instead.
    @State private var keyboardVisible = false

    init(bloc: BottomNavBloc, path: Binding<[StackEntry]>) {
        self.bloc = bloc
        _path = path
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
        _content = StateObject(wrappedValue: ObservedValue(bloc.content))
    }

    var body: some View {
        let selectedTab = model.value.selectedTab
        TabView(selection: Binding(get: { model.current.selectedTab }, set: { bloc.onTabSelected(tab: $0) })) {
            ForEach(model.value.tabs, id: \.self) { tab in
                Tab(tab.title, systemImage: tab.systemImage, value: tab) {
                    // Only the selected tab binds the root path; Decompose only keeps the active
                    // tab's child alive, so the others render nothing until selected.
                    NavigationStack(path: tab == selectedTab ? $path : .constant([])) {
                        Group {
                            if tab == selectedTab {
                                TabChildView(child: content.value.active.instance)
                            } else {
                                Color.clear
                            }
                        }
                        .safeAreaInset(edge: .bottom, spacing: 0) {
                            if !keyboardVisible {
                                ChefTabBar(
                                    tabs: model.value.tabs,
                                    selected: selectedTab,
                                    notificationCount: Int(model.value.notificationCount),
                                    onSelect: { bloc.onTabSelected(tab: $0) }
                                )
                            }
                        }
                        .toolbar(.hidden, for: .tabBar)
                        .navigationDestination(for: StackEntry.self) { entry in
                            StackEntryView(entry: entry)
                        }
                    }
                }
            }
        }
        .tint(ChefTheme.primary)
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillShowNotification)) { _ in
            keyboardVisible = true
        }
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillHideNotification)) { _ in
            keyboardVisible = false
        }
    }
}

/// The tab bar drawn inside each tab's root screen: a glass capsule of tab buttons, styled after the
/// iOS 26 tab bar.
struct ChefTabBar: View {
    /// The space the bar takes above the bottom safe area, for content that must clear it.
    static let height: CGFloat = 72

    let tabs: [BottomNavBlocTab]
    let selected: BottomNavBlocTab
    let notificationCount: Int
    let onSelect: (BottomNavBlocTab) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(tabs, id: \.self) { tab in
                let isSelected = tab == selected
                Button { onSelect(tab) } label: {
                    VStack(spacing: 2) {
                        Image(systemName: tab.systemImage)
                            .font(.system(size: 20, weight: .medium))
                            .frame(height: 26)
                            .overlay(alignment: .topTrailing) {
                                if tab == .settings && notificationCount > 0 {
                                    Text("\(notificationCount)")
                                        .font(.caption2.weight(.bold))
                                        .foregroundStyle(.white)
                                        .padding(.horizontal, 5)
                                        .background(.red, in: Capsule())
                                        .offset(x: 10, y: -4)
                                }
                            }
                        Text(tab.title)
                            .font(.caption2.weight(.medium))
                            .lineLimit(1)
                    }
                    .foregroundStyle(isSelected ? ChefTheme.primary : Color.primary)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 6)
                    .background {
                        if isSelected {
                            Capsule().fill(Color.primary.opacity(0.08))
                        }
                    }
                    .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
                .accessibilityValue(tab == .settings && notificationCount > 0 ? "\(notificationCount)" : "")
            }
        }
        .padding(4)
        .chefGlass()
        .padding(.horizontal, ChefTheme.Spacing.normal + 4)
        .padding(.bottom, ChefTheme.Spacing.extraSmall)
    }
}

/// Native view for a tab's content, or its Compose screen until that tab is redesigned.
private struct TabChildView: View {
    let child: BottomNavBlocChild

    var body: some View {
        switch child {
        case let recipes as BottomNavBlocChild.RecipeList:
            RecipeListView(bloc: recipes.bloc)
        case let grocery as BottomNavBlocChild.GroceryList:
            GroceryListView(bloc: grocery.bloc)
        case let settings as BottomNavBlocChild.Settings:
            MoreView(bloc: settings.bloc)
        case let browser as BottomNavBlocChild.Browser:
            BrowserRootView(bloc: browser.bloc)
        case let meals as BottomNavBlocChild.Meals:
            MealPlanView(bloc: meals.bloc)
        default:
            ComposeScreenHost(screen: child.bloc, edges: .top)
        }
    }
}

private extension BottomNavBlocTab {
    var title: String {
        switch self {
        case .recipes: ComposeStrings["tab_recipes"]
        case .groceries: ComposeStrings["tab_grocery"]
        case .meals: ComposeStrings["tab_meals"]
        case .browser: ComposeStrings["tab_browser"]
        default: ComposeStrings["tab_more"]
        }
    }

    var systemImage: String {
        switch self {
        case .recipes: "list.bullet"
        case .groceries: "cart"
        case .meals: "calendar"
        case .browser: "globe"
        default: "line.3.horizontal"
        }
    }
}

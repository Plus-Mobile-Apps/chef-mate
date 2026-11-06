import SwiftUI
import ComposeApp

/// The redesigned bottom navigation. Tab order and selection come from `BottomNavBloc`; each tab
/// owns the `NavigationStack` that root-stack children push onto.
///
/// Uses the system `TabView` tab bar, so on iOS 26+ it is the native Liquid Glass bar (and the
/// standard bar on earlier versions). Like the Android bottom nav, the bar only shows on the tab
/// roots: pushed screens hide it with `.toolbar(.hidden, for: .tabBar)`.
struct BottomNavView: View {
    /// The space the system tab bar takes above the bottom safe area, for overlays (toasts) that
    /// are drawn above the `TabView` and must clear it.
    static let tabBarHeight: CGFloat = 56

    let bloc: BottomNavBloc
    @Binding var path: [StackEntry]
    @StateObject private var model: ObservedFlow<BottomNavBlocModel>
    @StateObject private var content: ObservedValue<ChildStack<AnyObject, BottomNavBlocChild>>

    init(bloc: BottomNavBloc, path: Binding<[StackEntry]>) {
        self.bloc = bloc
        _path = path
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
        _content = StateObject(wrappedValue: ObservedValue(bloc.content))
    }

    var body: some View {
        let selectedTab = model.value.selectedTab
        let notificationCount = Int(model.value.notificationCount)
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
                        .navigationDestination(for: StackEntry.self) { entry in
                            StackEntryView(entry: entry)
                                .toolbar(.hidden, for: .tabBar)
                        }
                    }
                }
                .badge(tab == .settings ? notificationCount : 0)
            }
        }
        .tint(ChefTheme.primary)
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

extension BottomNavBlocTab {
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

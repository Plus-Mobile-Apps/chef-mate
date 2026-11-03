import SwiftUI
import ComposeApp

/// The redesigned bottom navigation: a native `TabView`, which gets the Liquid Glass tab bar on
/// iOS 26. Tab order and selection come from `BottomNavBloc`. Each tab owns the `NavigationStack`
/// that root-stack children push onto, so they slide over the tab bar like on Android.
struct BottomNavView: View {
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
                            RootChildView(child: entry.child)
                                .toolbar(.hidden, for: .tabBar)
                        }
                    }
                }
                .badge(tab == .settings ? Int(model.value.notificationCount) : 0)
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
        case let settings as BottomNavBlocChild.Settings:
            MoreView(bloc: settings.bloc)
        default:
            ComposeScreenHost(screen: child.bloc, edges: .top)
        }
    }
}

private extension BottomNavBlocTab {
    var title: String {
        switch self {
        case .recipes: "Recipes"
        case .groceries: "Grocery"
        case .meals: "Meals"
        case .browser: "Browser"
        default: "More"
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

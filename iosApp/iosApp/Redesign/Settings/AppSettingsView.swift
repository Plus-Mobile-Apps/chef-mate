import SwiftUI
import ComposeApp

/// The redesigned app settings (More → Settings), mirroring the Android `AppSettingsScreen`:
/// Navigation (tab order), Browser (default engine, bookmarks, history toggle, clear history),
/// Recipes (categories), Grocery (autocomplete, aisle rules) and Backup (import / export). The
/// engine picker is a native menu on the row rather than the Android dialog.
struct AppSettingsView: View {
    let bloc: AppSettingsBloc
    @StateObject private var model: ObservedFlow<AppSettingsBlocModel>

    init(bloc: AppSettingsBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        Form {
            Section(ComposeStrings["app_settings_navigation_section"]) {
                MoreRow(ComposeStrings["app_settings_bottom_nav_order"], systemImage: "arrow.up.arrow.down", action: bloc.onBottomNavOrderClicked)
            }

            Section(ComposeStrings["app_settings_browser_section"]) {
                SearchEngineMenu(selected: state.selectedSearchEngine) { bloc.onSearchEngineSelected(engine: $0) }
                MoreRow(ComposeStrings["app_settings_browser_bookmarks"], systemImage: "bookmark", action: bloc.onBookmarksClicked)
                Toggle(isOn: Binding(
                    get: { model.current.isHistoryEnabled },
                    set: { bloc.onHistoryEnabledChanged(enabled: $0) }
                )) {
                    Label {
                        Text(ComposeStrings["app_settings_browser_history_enabled"])
                    } icon: {
                        Image(systemName: "clock.arrow.circlepath").foregroundStyle(ChefTheme.primary)
                    }
                }
                Button(role: .destructive, action: bloc.onClearHistoryClicked) {
                    Label(ComposeStrings["app_settings_clear_history"], systemImage: "trash")
                }
            }

            Section(ComposeStrings["app_settings_recipes_section"]) {
                MoreRow(ComposeStrings["app_settings_recipe_categories"], systemImage: "tag", action: bloc.onRecipeCategoriesClicked)
            }

            Section(ComposeStrings["app_settings_grocery_section"]) {
                MoreRow(ComposeStrings["app_settings_grocery_autocomplete"], systemImage: "text.badge.plus", action: bloc.onGroceryAutocompleteClicked)
                MoreRow(ComposeStrings["app_settings_grocery_category_rules"], systemImage: "cart", action: bloc.onGroceryCategoryRulesClicked)
            }

            Section(ComposeStrings["app_settings_backup_section"]) {
                MoreRow(ComposeStrings["import_recipes"], systemImage: "square.and.arrow.down", action: bloc.onImportRecipesClicked)
                MoreRow(ComposeStrings["export_recipes"], systemImage: "square.and.arrow.up", action: bloc.onExportRecipesClicked)
            }
        }
        .navigationTitle(ComposeStrings["app_settings_title"])
        .tint(ChefTheme.primary)
        .alert(
            ComposeStrings["app_settings_clear_history_dialog_title"],
            isPresented: Binding(
                get: { model.value.showClearHistoryDialog },
                set: { shown in
                    if !shown && model.current.showClearHistoryDialog { bloc.onClearHistoryDismissed() }
                }
            )
        ) {
            Button(ComposeStrings["app_settings_clear_history_dialog_confirm"], role: .destructive, action: bloc.onClearHistoryConfirmed)
            Button(ComposeStrings["app_settings_clear_history_dialog_cancel"], role: .cancel, action: bloc.onClearHistoryDismissed)
        } message: {
            Text(ComposeStrings["app_settings_clear_history_dialog_message"])
        }
    }
}

/// The "Default search engine" row: the current engine's logo and name (or "None"), opening a
/// menu of every engine with the current one checked.
private struct SearchEngineMenu: View {
    let selected: SearchEngine?
    let onSelect: (SearchEngine) -> Void

    var body: some View {
        Menu {
            ForEach(SearchEngine.entries, id: \.self) { engine in
                Button { onSelect(engine) } label: {
                    if engine == selected {
                        Label(engine.displayName, systemImage: "checkmark")
                    } else {
                        Label { Text(engine.displayName) } icon: { Image(engine.logoImageName) }
                    }
                }
            }
        } label: {
            HStack {
                Label {
                    Text(ComposeStrings["app_settings_default_search_engine"]).foregroundStyle(Color.primary)
                } icon: {
                    Image(systemName: "magnifyingglass").foregroundStyle(ChefTheme.primary)
                }
                Spacer()
                if let selected {
                    Image(selected.logoImageName)
                        .resizable()
                        .scaledToFit()
                        .frame(width: 20, height: 20)
                        .accessibilityHidden(true)
                }
                Text(selected?.displayName ?? ComposeStrings["app_settings_default_search_engine_none"])
                    .foregroundStyle(.secondary)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
        }
        .accessibilityLabel(ComposeStrings["app_settings_default_search_engine_dialog_title"])
        .accessibilityValue(selected?.displayName ?? ComposeStrings["app_settings_default_search_engine_none"])
    }
}

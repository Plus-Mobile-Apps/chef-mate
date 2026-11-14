import SwiftUI
import ComposeApp

/// The Recipes tab's search screen, shown in place of the list when the toolbar's search button is
/// tapped. Before anything is typed or picked it leads with quick filters and categories; as soon as
/// there is a query or an active filter, those collapse into a chip row and matching recipes appear
/// beneath it, updating as the user types.
///
/// Query and filters live in `RecipeListBloc`, so the results are the same list the tab shows;
/// `RecipeListView` restores the filters it had before search when the screen is cancelled.
struct RecipeSearchView: View {
    let state: RecipeListBlocModel
    let bloc: RecipeListBloc
    var isFocused: FocusState<Bool>.Binding
    let onCancel: () -> Void

    private var query: Binding<String> {
        Binding(get: { state.searchQuery }, set: { bloc.onSearchQueryChanged(query: $0) })
    }

    /// Typing or picking any filter switches from the browse layout to live results.
    private var isRefining: Bool {
        !state.searchQuery.isEmpty || state.totalActiveFilterCount > 0
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.large) {
                if isRefining {
                    chipRow
                    results
                } else {
                    quickFilters
                    categories
                }
            }
            .padding(.vertical, ChefTheme.Spacing.normal)
            .animation(.snappy, value: isRefining)
        }
        .scrollDismissesKeyboard(.immediately)
        .safeAreaInset(edge: .top, spacing: 0) { header }
        .background(Color(.systemBackground))
    }

    // MARK: - Header

    private var header: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
            HStack(spacing: ChefTheme.Spacing.normal - 4) {
                HStack(spacing: ChefTheme.Spacing.small) {
                    Image(systemName: "magnifyingglass")
                        .foregroundStyle(.secondary)
                    TextField(ComposeStrings["recipe_list_search_placeholder"], text: query)
                        .focused(isFocused)
                        .submitLabel(.search)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                    if !state.searchQuery.isEmpty {
                        Button { bloc.onSearchQueryChanged(query: "") } label: {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundStyle(.secondary)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(ComposeStrings["recipe_list_search_clear"])
                    }
                }
                .padding(.horizontal, ChefTheme.Spacing.normal - 4)
                .frame(height: 44)
                .background(Color(.secondarySystemBackground), in: Capsule())

                Button(ComposeStrings["recipe_list_search_cancel"], action: onCancel)
                    .fontWeight(.medium)
            }
            if let book = state.activeBook, !state.isAllRecipesSelected {
                HStack(spacing: ChefTheme.Spacing.small) {
                    Label(ComposeStrings.format("recipe_list_search_scope_book", ["book": book.name]),
                          systemImage: "books.vertical")
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                    Button(ComposeStrings["recipe_list_search_all_recipes"], action: bloc.onAllRecipesSelected)
                }
                .font(.footnote)
                .padding(.horizontal, ChefTheme.Spacing.extraSmall)
            }
        }
        .padding(.horizontal, ChefTheme.Spacing.normal)
        .padding(.top, ChefTheme.Spacing.small)
        .padding(.bottom, ChefTheme.Spacing.small)
        .background(Color(.systemBackground))
    }

    // MARK: - Browse layout

    /// The three one-tap filters as large tiles, so they read as the fastest way in.
    private var quickFilters: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.normal - 4) {
            SectionTitle(ComposeStrings["recipe_list_search_quick_filters"])
            HStack(spacing: ChefTheme.Spacing.normal - 4) {
                ForEach(RecipeFilterOption.entries, id: \.self) { option in
                    Button { toggle(option) } label: {
                        QuickFilterTile(title: ComposeStrings[option.labelKey], systemImage: option.systemImage,
                                        tint: option.tint)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, ChefTheme.Spacing.normal)
        }
    }

    private var categories: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.normal - 4) {
            SectionTitle(ComposeStrings["recipe_list_search_categories"])
            FlowLayout(spacing: ChefTheme.Spacing.small) {
                ForEach(BuiltinCategory.entries, id: \.self) { category in
                    FilterChip(title: ComposeStrings[category.labelKey], systemImage: category.systemImage,
                               isSelected: false) { toggle(category) }
                }
                ForEach(state.availableUserCategories, id: \.id) { category in
                    FilterChip(title: category.name, systemImage: "tag", isSelected: false) {
                        toggle(userCategory: category.id)
                    }
                }
            }
            .padding(.horizontal, ChefTheme.Spacing.normal)
        }
    }

    // MARK: - Refining layout

    /// Every filter as a scrollable chip row, the active ones first so they stay in view.
    private var chipRow: some View {
        let userIds = Set(state.activeUserCategoryIds.map { $0.int64Value })
        let quick = RecipeFilterOption.entries.map { option in
            Chip(id: "f-\(option.name)", title: ComposeStrings[option.labelKey], systemImage: option.systemImage,
                 isSelected: state.activeFilters.contains(option)) { toggle(option) }
        }
        let builtin = BuiltinCategory.entries.map { category in
            Chip(id: "c-\(category.name)", title: ComposeStrings[category.labelKey],
                 systemImage: category.systemImage,
                 isSelected: state.activeCategories.contains(category)) { toggle(category) }
        }
        let user = state.availableUserCategories.map { category in
            Chip(id: "u-\(category.id)", title: category.name, systemImage: "tag",
                 isSelected: userIds.contains(category.id)) { toggle(userCategory: category.id) }
        }
        let all = quick + builtin + user
        let ordered = all.filter(\.isSelected) + all.filter { !$0.isSelected }
        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: ChefTheme.Spacing.small) {
                if state.totalActiveFilterCount > 0 {
                    Button(action: bloc.onClearFilters) {
                        Image(systemName: "xmark")
                            .font(.subheadline.weight(.semibold))
                            .frame(width: 36, height: 36)
                            .background(Color(.secondarySystemBackground), in: Circle())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(ComposeStrings["recipe_list_clear_filters"])
                }
                ForEach(ordered) { chip in
                    FilterChip(title: chip.title, systemImage: chip.systemImage, isSelected: chip.isSelected,
                               action: chip.action)
                }
            }
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .animation(.snappy, value: ordered.map(\.id))
        }
    }

    @ViewBuilder
    private var results: some View {
        if state.recipes.isEmpty && state.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.top, ChefTheme.Spacing.extraLarge)
        } else if state.recipes.isEmpty {
            ContentUnavailableView {
                Label(ComposeStrings[state.searchQuery.isEmpty
                                     ? "recipe_list_filter_empty_title" : "recipe_list_search_empty"],
                      systemImage: "magnifyingglass")
            } description: {
                if state.activeBook != nil && !state.isAllRecipesSelected {
                    Text(ComposeStrings["recipe_list_search_empty_book_hint"])
                }
            }
            .padding(.top, ChefTheme.Spacing.large)
        } else {
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
                HStack(spacing: ChefTheme.Spacing.small) {
                    SectionTitle(ComposeStrings["recipe_list_search_results"])
                    Text("\(state.recipes.count)")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .contentTransition(.numericText())
                }
                LazyVStack(spacing: 0) {
                    ForEach(state.recipes, id: \.id) { recipe in
                        Button { bloc.onRecipeClicked(recipe: recipe) } label: {
                            RecipeRow(recipe: recipe)
                                .padding(.horizontal, ChefTheme.Spacing.normal)
                                .padding(.vertical, ChefTheme.Spacing.small)
                        }
                        .buttonStyle(.plain)
                        .contextMenu {
                            Button { bloc.onToggleFavorite(recipe: recipe) } label: {
                                Label(recipe.isFavorite ? "Unfavorite" : "Favorite",
                                      systemImage: recipe.isFavorite ? "heart.slash" : "heart")
                            }
                        }
                        Divider().padding(.leading, ChefTheme.Spacing.normal + 80 + ChefTheme.Spacing.normal)
                    }
                }
            }
            .transition(.opacity.combined(with: .move(edge: .bottom)))
        }
    }

    // MARK: - Filter changes

    private func toggle(_ option: RecipeFilterOption) {
        var filters = state.activeFilters
        if filters.contains(option) { filters.remove(option) } else { filters.insert(option) }
        apply(filters: filters)
    }

    private func toggle(_ category: BuiltinCategory) {
        var categories = state.activeCategories
        if categories.contains(category) { categories.remove(category) } else { categories.insert(category) }
        apply(categories: categories)
    }

    private func toggle(userCategory id: Int64) {
        var ids = Set(state.activeUserCategoryIds.map { $0.int64Value })
        if ids.contains(id) { ids.remove(id) } else { ids.insert(id) }
        apply(userCategoryIds: ids)
    }

    private func apply(
        filters: Set<RecipeFilterOption>? = nil,
        categories: Set<BuiltinCategory>? = nil,
        userCategoryIds: Set<Int64>? = nil
    ) {
        let ids = userCategoryIds ?? Set(state.activeUserCategoryIds.map { $0.int64Value })
        withAnimation(.snappy) {
            bloc.onApplySortAndFilters(
                sort: state.currentSort,
                filters: filters ?? state.activeFilters,
                categories: categories ?? state.activeCategories,
                userCategoryIds: Set(ids.map { KotlinLong(value: $0) })
            )
        }
    }
}

// MARK: - Pieces

private struct Chip: Identifiable {
    let id: String
    let title: String
    let systemImage: String
    let isSelected: Bool
    let action: () -> Void
}

private struct SectionTitle: View {
    let text: String
    init(_ text: String) { self.text = text }

    var body: some View {
        Text(text)
            .font(.title3.weight(.semibold))
            .padding(.horizontal, ChefTheme.Spacing.normal)
    }
}

private struct QuickFilterTile: View {
    let title: String
    let systemImage: String
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
            Image(systemName: systemImage)
                .font(.title2)
                .foregroundStyle(tint)
                .frame(width: 44, height: 44)
                .background(tint.opacity(0.15), in: Circle())
            Text(title)
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(Color.primary)
                .lineLimit(2, reservesSpace: true)
                .multilineTextAlignment(.leading)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(ChefTheme.Spacing.normal - 4)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        .contentShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct FilterChip: View {
    let title: String
    let systemImage: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: isSelected ? "checkmark" : systemImage)
                .font(.subheadline.weight(.medium))
                .padding(.horizontal, ChefTheme.Spacing.normal - 4)
                .frame(height: 36)
                .foregroundStyle(isSelected ? ChefTheme.onPrimary : Color.primary)
                .background(isSelected ? ChefTheme.primary : Color(.secondarySystemBackground), in: Capsule())
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// MARK: - Icons

private extension RecipeFilterOption {
    var systemImage: String {
        switch self {
        case .favorites: "heart.fill"
        case .rated: "star.fill"
        default: "timer"
        }
    }

    var tint: Color {
        switch self {
        case .favorites: .pink
        case .rated: .orange
        default: .teal
        }
    }
}

private extension BuiltinCategory {
    var systemImage: String {
        switch self {
        case .breakfast: "sun.horizon"
        case .lunch: "takeoutbag.and.cup.and.straw"
        case .dinner: "fork.knife"
        case .appetizer: "carrot"
        case .side: "leaf"
        case .dessert: "birthday.cake"
        case .snack: "popcorn"
        case .drink: "cup.and.saucer"
        case .ai: "sparkles"
        default: "square.grid.2x2"
        }
    }
}

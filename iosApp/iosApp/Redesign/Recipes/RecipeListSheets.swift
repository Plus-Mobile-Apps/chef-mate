import SwiftUI
import ComposeApp

// MARK: - Labels

/// Catalog keys for the list's enums — the Swift side of `labelRes()` in `RecipeListScreen.kt`.
extension RecipeSortOption {
    var labelKey: String {
        switch self {
        case .recentlyAdded: "recipe_list_sort_recently_added"
        case .oldestFirst: "recipe_list_sort_oldest_first"
        case .alphabeticalAsc: "recipe_list_sort_a_to_z"
        case .alphabeticalDesc: "recipe_list_sort_z_to_a"
        default: "recipe_list_sort_top_rated"
        }
    }
}

extension RecipeFilterOption {
    var labelKey: String {
        switch self {
        case .favorites: "recipe_list_filter_favorites"
        case .rated: "recipe_list_filter_rated"
        default: "recipe_list_filter_quick_recipes"
        }
    }
}

extension BuiltinCategory {
    var labelKey: String { "recipe_list_category_\(name.lowercased())" }
}

// MARK: - Recipe books

/// Books grouped the way Compose's `RecipeBookSections` groups them: the user's own, books shared
/// with them, then one section per family.
struct RecipeBookGroups {
    let mine: [PublicRecipeBook]
    let shared: [PublicRecipeBook]
    let families: [(name: String, books: [PublicRecipeBook])]

    init(_ books: [PublicRecipeBook]) {
        mine = books.filter { $0.isOwnedByCurrentUser && !$0.isFamily }
        shared = books.filter { !$0.isOwnedByCurrentUser && !$0.isFamily }
        var order: [String] = []
        var byFamily: [String: [PublicRecipeBook]] = [:]
        for book in books where book.isFamily {
            let key = book.familyId ?? ""
            if byFamily[key] == nil { order.append(key) }
            byFamily[key, default: []].append(book)
        }
        families = order.map { key in
            let list = byFamily[key] ?? []
            return (list.first?.familyName ?? ComposeStrings["recipe_list_book_family_fallback"], list)
        }
    }
}

/// The book selector's picker (Compose `RecipeBookPickerContent`): "All recipes", the grouped books
/// with an edit action on the ones the user manages, and "Create new book".
struct RecipeBookPickerSheet: View {
    let state: RecipeListBlocModel
    let bloc: RecipeListBloc

    var body: some View {
        let groups = RecipeBookGroups(state.recipeBooks)
        NavigationStack {
            List {
                Section {
                    row(ComposeStrings["recipe_list_book_all_recipes"], selected: state.isAllRecipesSelected) {
                        bloc.onAllRecipesSelected()
                    }
                }
                Section(ComposeStrings["recipe_list_book_my_books"]) {
                    ForEach(groups.mine, id: \.id) { bookRow($0, editable: true) }
                    Button(action: bloc.onCreateBookClicked) {
                        Label(ComposeStrings["recipe_list_book_create"], systemImage: "plus")
                    }
                }
                if !groups.shared.isEmpty {
                    Section(ComposeStrings["recipe_list_book_shared_with_you"]) {
                        ForEach(groups.shared, id: \.id) { bookRow($0, editable: true, badge: true) }
                    }
                }
                // Family books are managed from Manage Family, so they offer no edit action here.
                ForEach(groups.families, id: \.name) { family in
                    Section(family.name) {
                        ForEach(family.books, id: \.id) { bookRow($0, editable: false) }
                    }
                }
            }
            .navigationTitle(ComposeStrings["recipe_list_book_selector"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .cancel, action: bloc.onBookPickerDismissed) {
                        Image(systemName: "xmark")
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .tint(ChefTheme.primary)
    }

    private func bookRow(_ book: PublicRecipeBook, editable: Bool, badge: Bool = false) -> some View {
        let selected = !state.isAllRecipesSelected && state.activeBook?.id == book.id
        return row(book.name, selected: selected, badge: badge) {
            bloc.onBookSelected(bookId: book.id)
        }
        .swipeActions(edge: .trailing) {
            if editable {
                Button { bloc.onEditBookClicked(bookId: book.id) } label: {
                    Label(ComposeStrings["recipe_list_book_edit"], systemImage: "pencil")
                }
                .tint(ChefTheme.primary)
            }
        }
        .contextMenu {
            if editable {
                Button { bloc.onEditBookClicked(bookId: book.id) } label: {
                    Label(ComposeStrings["recipe_list_book_edit"], systemImage: "pencil")
                }
            }
        }
    }

    private func row(_ title: String, selected: Bool, badge: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                // `Color.primary`, not `.primary`: inside a List button the hierarchical style
                // resolves to the tint.
                Text(title).foregroundStyle(Color.primary)
                if badge {
                    Text(ComposeStrings["recipe_list_book_shared_badge"])
                        .font(.caption2.weight(.semibold))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(ChefTheme.primary.opacity(0.15), in: Capsule())
                        .foregroundStyle(ChefTheme.primary)
                }
                Spacer()
                if selected {
                    Image(systemName: "checkmark").foregroundStyle(ChefTheme.primary)
                }
            }
        }
    }
}

/// Picks the book to file the selected recipes under (Compose `BulkAddToBookSheet`).
struct BulkAddToBookSheet: View {
    let books: [PublicRecipeBook]
    let onSelect: (Int64) -> Void
    @SwiftUI.Environment(\.dismiss) private var dismiss

    var body: some View {
        let groups = RecipeBookGroups(books)
        NavigationStack {
            List {
                if !groups.mine.isEmpty {
                    Section(ComposeStrings["recipe_list_book_my_books"]) {
                        ForEach(groups.mine, id: \.id) { row($0) }
                    }
                }
                if !groups.shared.isEmpty {
                    Section(ComposeStrings["recipe_list_book_shared_with_you"]) {
                        ForEach(groups.shared, id: \.id) { row($0) }
                    }
                }
                ForEach(groups.families, id: \.name) { family in
                    Section(family.name) {
                        ForEach(family.books, id: \.id) { row($0) }
                    }
                }
            }
            .navigationTitle(ComposeStrings["recipe_list_bulk_book_title"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .cancel) { dismiss() } label: { Image(systemName: "xmark") }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .tint(ChefTheme.primary)
    }

    private func row(_ book: PublicRecipeBook) -> some View {
        Button(book.name) { onSelect(book.id) }
            .foregroundStyle(Color.primary)
    }
}

/// Picks the category to tag the selected recipes with (Compose `BulkAddToCategorySheet`).
struct BulkAddToCategorySheet: View {
    let userCategories: [ComposeApp.Category]
    let onBuiltin: (BuiltinCategory) -> Void
    let onUserCategory: (Int64) -> Void
    @SwiftUI.Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(BuiltinCategory.entries, id: \.self) { category in
                        Button(ComposeStrings[category.labelKey]) { onBuiltin(category) }
                            .foregroundStyle(Color.primary)
                    }
                }
                if !userCategories.isEmpty {
                    Section {
                        ForEach(userCategories, id: \.id) { category in
                            Button(category.name) { onUserCategory(category.id) }
                                .foregroundStyle(Color.primary)
                        }
                    }
                }
            }
            .navigationTitle(ComposeStrings["recipe_list_bulk_category_title"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .cancel) { dismiss() } label: { Image(systemName: "xmark") }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .tint(ChefTheme.primary)
    }
}

// MARK: - Sort & filter

/// Compose `SortFilterSheetContent`: edits a draft of sort, filters and categories, applied together.
struct SortFilterSheet: View {
    let userCategories: [ComposeApp.Category]
    let onApply: (RecipeSortOption, Set<RecipeFilterOption>, Set<BuiltinCategory>, Set<Int64>) -> Void
    @State private var sort: RecipeSortOption
    @State private var filters: Set<RecipeFilterOption>
    @State private var categories: Set<BuiltinCategory>
    @State private var userCategoryIds: Set<Int64>
    @SwiftUI.Environment(\.dismiss) private var dismiss

    init(
        state: RecipeListBlocModel,
        onApply: @escaping (RecipeSortOption, Set<RecipeFilterOption>, Set<BuiltinCategory>, Set<Int64>) -> Void
    ) {
        userCategories = state.availableUserCategories
        self.onApply = onApply
        _sort = State(initialValue: state.currentSort)
        _filters = State(initialValue: state.activeFilters)
        _categories = State(initialValue: state.activeCategories)
        _userCategoryIds = State(initialValue: Set(state.activeUserCategoryIds.map { $0.int64Value }))
    }

    private var hasFilters: Bool {
        !filters.isEmpty || !categories.isEmpty || !userCategoryIds.isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section(ComposeStrings["recipe_list_sort_by"]) {
                    Picker(ComposeStrings["recipe_list_sort_by"], selection: $sort) {
                        ForEach(RecipeSortOption.entries, id: \.self) { option in
                            Text(ComposeStrings[option.labelKey]).tag(option)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }
                Section(ComposeStrings["recipe_list_filter_by"]) {
                    ForEach(RecipeFilterOption.entries, id: \.self) { option in
                        Toggle(ComposeStrings[option.labelKey], isOn: membership(option, in: $filters))
                    }
                }
                Section(ComposeStrings["recipe_list_filter_by_category"]) {
                    ForEach(BuiltinCategory.entries, id: \.self) { category in
                        Toggle(ComposeStrings[category.labelKey], isOn: membership(category, in: $categories))
                    }
                    ForEach(userCategories, id: \.id) { category in
                        Toggle(category.name, isOn: membership(category.id, in: $userCategoryIds))
                    }
                }
                if hasFilters {
                    Section {
                        Button(ComposeStrings["recipe_list_clear_filters"], role: .destructive) {
                            filters = []
                            categories = []
                            userCategoryIds = []
                        }
                    }
                }
            }
            .tint(ChefTheme.primary)
            .navigationTitle(ComposeStrings["recipe_list_sort_and_filter"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(role: .cancel) { dismiss() } label: { Image(systemName: "xmark") }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(ComposeStrings["recipe_list_apply"]) {
                        onApply(sort, filters, categories, userCategoryIds)
                    }
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private func membership<T: Hashable>(_ value: T, in set: Binding<Set<T>>) -> Binding<Bool> {
        Binding(
            get: { set.wrappedValue.contains(value) },
            set: { isOn in
                if isOn { set.wrappedValue.insert(value) } else { set.wrappedValue.remove(value) }
            }
        )
    }
}

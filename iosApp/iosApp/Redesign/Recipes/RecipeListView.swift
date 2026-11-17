import SwiftUI
import PhotosUI
import ComposeApp

/// The redesigned Recipes tab, mirroring the Android `RecipeListScreen`: searchable list or grid of
/// recipe cards, recipe-book picker, sort & filter sheet, selection mode with bulk actions, scan from
/// photo, pending book invites, and the continue / done cooking buttons.
///
/// Not yet ported (docs/ios-swiftui-redesign.md, PR 3): coach marks and the tablet filter rail.
struct RecipeListView: View {
    let bloc: RecipeListBloc
    @StateObject private var model: ObservedFlow<RecipeListBlocModel>
    @State private var showSortFilter = false
    @State private var showPhotoPicker = false
    @State private var pickedPhoto: PhotosPickerItem?
    @State private var pendingDelete: RecipeListItem?
    /// Whether the search screen (`RecipeSearchView`) is showing in place of the list. Opened from
    /// the toolbar's search button; while open the navigation and tab bars hide.
    @State private var isSearchPresented = false
    /// The list's filters and book when search opened, restored when it is cancelled so filters
    /// picked while searching don't linger on the list.
    @State private var preSearchFilters: FilterSnapshot?
    @FocusState private var isSearchFocused: Bool

    init(bloc: RecipeListBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        let screen = ZStack {
            browse(state)
            if isSearchPresented {
                RecipeSearchView(state: state, bloc: bloc, isFocused: $isSearchFocused, onCancel: closeSearch)
                    .transition(.asymmetric(
                        insertion: .opacity.combined(with: .offset(y: 32)),
                        removal: .opacity
                    ))
                    .zIndex(1)
            }
        }
        .navigationTitle(title(state))
        .toolbar {
            if state.isSelectionMode {
                selectionToolbar(state)
            } else {
                browseToolbar(state)
            }
        }
        .toolbar(isSearchPresented ? .hidden : .automatic, for: .navigationBar)
        .toolbar(isSearchPresented ? .hidden : .automatic, for: .tabBar)
        .animation(.snappy, value: isSearchPresented)
        .onAppear {
            // A query restored by the BLoC reopens search, so the filter is never invisible.
            if !model.current.searchQuery.isEmpty && !isSearchPresented { openSearch() }
        }
        .onChange(of: state.isSelectionMode) { _, selecting in
            if selecting { closeSearch() }
        }
        .overlay {
            if state.isScanning { ScanningOverlay() }
        }
        dialogs(screen, state)
    }

    /// The list itself, with its pending-invite banners and cooking buttons.
    private func browse(_ state: RecipeListBlocModel) -> some View {
        content(state)
            .refreshable { bloc.onSyncClicked() }
            .safeAreaInset(edge: .top, spacing: 0) {
                if !state.pendingInvites.isEmpty {
                    VStack(spacing: ChefTheme.Spacing.small) {
                        ForEach(state.pendingInvites, id: \.memberId) { invite in
                            InviteBanner(
                                invite: invite,
                                onAccept: { bloc.onAcceptInvite(memberId: invite.memberId) },
                                onDecline: { bloc.onDeclineInvite(memberId: invite.memberId) }
                            )
                        }
                    }
                    .padding(.horizontal, ChefTheme.Spacing.normal)
                    .padding(.vertical, ChefTheme.Spacing.small)
                }
            }
            .overlay(alignment: .bottom) {
                if state.cookingRecipeCount > 0 && !state.isSelectionMode && !isSearchPresented {
                    CookingSessionBar(
                        continueTitle: ComposeStrings["recipe_list_continue_cooking"],
                        doneTitle: ComposeStrings["recipe_list_done_cooking"],
                        onContinue: bloc.onContinueCookingClicked,
                        onDone: bloc.onDoneCookingClicked
                    )
                }
            }
    }

    /// Adds the screen's alerts, sheets and photo picker; kept apart so `body` type-checks quickly.
    private func dialogs<Content: View>(_ view: Content, _ state: RecipeListBlocModel) -> some View {
        view
            .alert(
                ComposeStrings["recipe_list_done_cooking_title"],
                isPresented: blocFlag(state.showDoneCookingDialog, onDismiss: bloc.onDoneCookingDismissed)
            ) {
                Button(ComposeStrings["recipe_list_done_cooking_confirm"], role: .destructive) {
                    bloc.onDoneCookingConfirmed()
                }
                Button(ComposeStrings["recipe_list_done_cooking_cancel"], role: .cancel) {
                    bloc.onDoneCookingDismissed()
                }
            } message: {
                Text(ComposeStrings["recipe_list_done_cooking_message"])
            }
            .alert(
                ComposeStrings["recipe_list_scan_failed_title"],
                isPresented: blocFlag(state.scanError != nil, onDismiss: bloc.onScanErrorDismissed)
            ) {
                Button("OK", role: .cancel) { bloc.onScanErrorDismissed() }
            } message: {
                if let error = state.scanError { LocalizedText(text: error) }
            }
            .confirmationDialog(
                pendingDelete.map { "Delete “\($0.title)”?" } ?? "",
                isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
                titleVisibility: .visible
            ) {
                Button("Delete", role: .destructive) {
                    if let recipe = pendingDelete { bloc.onDeleteRecipe(recipe: recipe) }
                    pendingDelete = nil
                }
            }
            .sheet(isPresented: $showSortFilter) {
                SortFilterSheet(state: state) { sort, filters, categories, userCategoryIds in
                    bloc.onApplySortAndFilters(
                        sort: sort,
                        filters: filters,
                        categories: categories,
                        userCategoryIds: Set(userCategoryIds.map { KotlinLong(value: $0) })
                    )
                    showSortFilter = false
                }
            }
            .sheet(isPresented: blocFlag(state.isBookPickerOpen, onDismiss: bloc.onBookPickerDismissed)) {
                RecipeBookPickerSheet(state: state, bloc: bloc)
            }
            .sheet(isPresented: blocFlag(state.isBulkBookPickerOpen, onDismiss: bloc.onBulkBookPickerDismissed)) {
                BulkAddToBookSheet(books: state.recipeBooks, onSelect: { bloc.onAddSelectedToBook(bookId: $0) })
            }
            .sheet(isPresented: blocFlag(state.isBulkCategoryPickerOpen, onDismiss: bloc.onBulkCategoryPickerDismissed)) {
                BulkAddToCategorySheet(
                    userCategories: state.availableUserCategories,
                    onBuiltin: { bloc.onAddSelectedToBuiltinCategory(category: $0) },
                    onUserCategory: { bloc.onAddSelectedToUserCategory(categoryId: $0) }
                )
            }
            .photosPicker(isPresented: $showPhotoPicker, selection: $pickedPhoto, matching: .images)
            .onChange(of: pickedPhoto) { _, item in
                guard let item else { return }
                pickedPhoto = nil
                Task { await scan(item) }
            }
            .tint(ChefTheme.primary)
    }

    /// The book name when any book exists ("All recipes" for the cross-book view), else "Recipes" —
    /// or the selection count while selecting.
    private func title(_ state: RecipeListBlocModel) -> String {
        if state.isSelectionMode {
            return ComposeStrings.format("recipe_list_selection_count", ["count": "\(state.selectedRecipeIds.count)"])
        }
        if state.recipeBooks.isEmpty { return ComposeStrings["recipe_list_title"] }
        return state.activeBook?.name ?? ComposeStrings["recipe_list_book_all_recipes"]
    }

    // MARK: - Toolbars

    @ToolbarContentBuilder
    private func browseToolbar(_ state: RecipeListBlocModel) -> some ToolbarContent {
        if !state.recipeBooks.isEmpty {
            ToolbarItem(placement: .topBarLeading) {
                Button(action: bloc.onBookSelectorClicked) {
                    Image(systemName: "books.vertical")
                }
                .accessibilityLabel(ComposeStrings["recipe_list_book_selector"])
            }
        }
        ToolbarItemGroup(placement: .topBarTrailing) {
            Button { showSortFilter = true } label: {
                let filtered = state.totalActiveFilterCount > 0
                Image(systemName: filtered
                      ? "line.3.horizontal.decrease.circle.fill"
                      : "line.3.horizontal.decrease.circle")
            }
            .accessibilityLabel(ComposeStrings["recipe_list_filter"])
            .accessibilityValue(state.totalActiveFilterCount > 0 ? "\(state.totalActiveFilterCount)" : "")

            Button(action: openSearch) {
                Image(systemName: "magnifyingglass")
            }
            .accessibilityLabel(ComposeStrings["recipe_list_search_placeholder"])

            Menu {
                Button(action: bloc.onAddRecipeClicked) {
                    Label(ComposeStrings["recipe_list_create_recipe"], systemImage: "square.and.pencil")
                }
                if state.isScanFromPhotoEnabled {
                    Button { showPhotoPicker = true } label: {
                        Label(ComposeStrings["recipe_list_scan_from_photo"], systemImage: "camera.viewfinder")
                    }
                }
                Divider()
                Button(action: bloc.onToggleViewMode) {
                    if state.isGridView {
                        Label(ComposeStrings["recipe_list_view_list"], systemImage: "list.bullet")
                    } else {
                        Label(ComposeStrings["recipe_list_view_grid"], systemImage: "square.grid.2x2")
                    }
                }
                Divider()
                Button(action: bloc.onSyncClicked) {
                    Label(ComposeStrings["recipe_list_menu_sync"], systemImage: "arrow.triangle.2.circlepath")
                }
                Button(action: bloc.onEnterSelectionMode) {
                    Label(ComposeStrings["recipe_list_menu_select"], systemImage: "checkmark.circle")
                }
                Button(action: bloc.onExportClicked) {
                    Label(ComposeStrings["recipe_list_menu_export_all"], systemImage: "square.and.arrow.up")
                }
                Button(action: bloc.onCollaborateClicked) {
                    Label(ComposeStrings["recipe_list_menu_collaborate"], systemImage: "person.2")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
            .accessibilityLabel(ComposeStrings["recipe_list_more_actions"])
        }
    }

    @ToolbarContentBuilder
    private func selectionToolbar(_ state: RecipeListBlocModel) -> some ToolbarContent {
        let hasSelection = !state.selectedRecipeIds.isEmpty
        let selected = selectedIds(state)
        let allSelected = !state.recipes.isEmpty && state.recipes.allSatisfy { selected.contains($0.id) }
        ToolbarItem(placement: .topBarLeading) {
            Button(action: bloc.onToggleSelectAllVisible) {
                Text(ComposeStrings[allSelected ? "recipe_list_selection_deselect_all" : "recipe_list_selection_select_all"])
            }
        }
        ToolbarItemGroup(placement: .topBarTrailing) {
            Button(action: bloc.onExportClicked) {
                Image(systemName: "square.and.arrow.up")
            }
            .disabled(!hasSelection)
            .accessibilityLabel(ComposeStrings["recipe_list_selection_export"])

            Menu {
                Button(action: bloc.onAddToBookClicked) {
                    Label(ComposeStrings["recipe_list_selection_add_to_book"], systemImage: "books.vertical")
                }
                Button(action: bloc.onAddToCategoryClicked) {
                    Label(ComposeStrings["recipe_list_selection_add_to_category"], systemImage: "tag")
                }
            } label: {
                Image(systemName: "ellipsis.circle")
            }
            .disabled(!hasSelection)
            .accessibilityLabel(ComposeStrings["recipe_list_selection_more"])

            Button(action: bloc.onExitSelectionMode) {
                Image(systemName: "xmark")
            }
            .accessibilityLabel(ComposeStrings["recipe_list_selection_exit"])
        }
    }

    // MARK: - Content

    @ViewBuilder
    private func content(_ state: RecipeListBlocModel) -> some View {
        if state.recipes.isEmpty && state.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if !state.isLoading && state.totalRecipeCount == 0 {
            EmptyRecipesView(onBrowse: bloc.onBrowseRecipesClicked, onCreate: bloc.onAddRecipeClicked)
        } else if state.recipes.isEmpty && state.isSearchActive {
            SearchEmptyView(
                canSearchAllRecipes: !state.isAllRecipesSelected,
                onSearchAllRecipes: bloc.onAllRecipesSelected
            )
        } else if state.recipes.isEmpty && state.totalActiveFilterCount > 0 {
            FilterEmptyView(state: state, onClear: bloc.onClearFilters)
        } else if state.isGridView {
            grid(state)
        } else {
            list(state)
        }
    }

    private func grid(_ state: RecipeListBlocModel) -> some View {
        let selected = selectedIds(state)
        return ScrollView {
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 160), spacing: ChefTheme.Spacing.normal)],
                      spacing: ChefTheme.Spacing.normal) {
                ForEach(state.recipes, id: \.id) { recipe in
                    Button { bloc.onRecipeClicked(recipe: recipe) } label: {
                        RecipeGridCard(recipe: recipe)
                            .overlay(alignment: .topLeading) {
                                if state.isSelectionMode {
                                    SelectionBadge(isSelected: selected.contains(recipe.id))
                                        .padding(ChefTheme.Spacing.small)
                                }
                            }
                            .overlay {
                                if state.isSelectionMode && selected.contains(recipe.id) {
                                    RoundedRectangle(cornerRadius: 16).strokeBorder(ChefTheme.primary, lineWidth: 2)
                                }
                            }
                    }
                    .buttonStyle(.plain)
                    .contextMenu { recipeActions(recipe, state: state) }
                }
            }
            .padding(ChefTheme.Spacing.normal)
        }
        .contentMargins(.bottom, cookingReserve(state), for: .scrollContent)
    }

    private func list(_ state: RecipeListBlocModel) -> some View {
        let selected = selectedIds(state)
        return List(state.recipes, id: \.id) { recipe in
            Button { bloc.onRecipeClicked(recipe: recipe) } label: {
                HStack(spacing: ChefTheme.Spacing.normal) {
                    if state.isSelectionMode {
                        SelectionBadge(isSelected: selected.contains(recipe.id))
                    }
                    RecipeRow(recipe: recipe)
                }
            }
            .buttonStyle(.plain)
            .listRowBackground(
                state.isSelectionMode && selected.contains(recipe.id) ? ChefTheme.primary.opacity(0.12) : nil
            )
            .contextMenu { recipeActions(recipe, state: state) }
            .swipeActions(edge: .trailing) {
                if !state.isSelectionMode {
                    Button(role: .destructive) { pendingDelete = recipe } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
            }
            .swipeActions(edge: .leading) {
                if !state.isSelectionMode {
                    Button { bloc.onToggleFavorite(recipe: recipe) } label: {
                        Label(recipe.isFavorite ? "Unfavorite" : "Favorite",
                              systemImage: recipe.isFavorite ? "heart.slash" : "heart")
                    }
                    .tint(.pink)
                }
            }
        }
        .listStyle(.plain)
        .contentMargins(.bottom, cookingReserve(state), for: .scrollContent)
    }

    /// Long-press menu. "Select" is the iOS counterpart of Compose's long-press-to-select.
    @ViewBuilder
    private func recipeActions(_ recipe: RecipeListItem, state: RecipeListBlocModel) -> some View {
        if !state.isSelectionMode {
            Button { bloc.onRecipeLongClicked(recipe: recipe) } label: {
                Label(ComposeStrings["recipe_list_menu_select"], systemImage: "checkmark.circle")
            }
            Button { bloc.onToggleFavorite(recipe: recipe) } label: {
                Label(recipe.isFavorite ? "Unfavorite" : "Favorite",
                      systemImage: recipe.isFavorite ? "heart.slash" : "heart")
            }
            Button(role: .destructive) { pendingDelete = recipe } label: {
                Label("Delete", systemImage: "trash")
            }
        }
    }

    private func openSearch() {
        preSearchFilters = FilterSnapshot(model.current)
        isSearchPresented = true
        // Focus once the field is in the hierarchy so the keyboard comes up with the screen.
        DispatchQueue.main.async { isSearchFocused = true }
    }

    /// Leaving search clears the query and puts back the list's filters and book from before it
    /// opened: anything picked while searching was for that search only.
    private func closeSearch() {
        guard isSearchPresented else { return }
        isSearchFocused = false
        isSearchPresented = false
        let state = model.current
        if !state.searchQuery.isEmpty { bloc.onSearchQueryChanged(query: "") }
        if let snapshot = preSearchFilters {
            snapshot.restore(on: bloc, from: state)
            preSearchFilters = nil
        }
    }

    /// Room under the last row so the cooking buttons never cover it (Compose's `FabStackReserve`).
    private func cookingReserve(_ state: RecipeListBlocModel) -> CGFloat {
        state.cookingRecipeCount > 0 && !state.isSelectionMode ? 80 : 0
    }

    private func selectedIds(_ state: RecipeListBlocModel) -> Set<Int64> {
        Set(state.selectedRecipeIds.map { $0.int64Value })
    }

    /// A sheet/alert presentation flag owned by the BLoC: SwiftUI only reports dismissal.
    private func blocFlag(_ value: Bool, onDismiss: @escaping () -> Void) -> Binding<Bool> {
        Binding(get: { value }, set: { if !$0 && value { onDismiss() } })
    }

    /// Loads the picked photo as JPEG (HEIC and friends included) and hands it to the scanner.
    private func scan(_ item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        let jpeg = UIImage(data: data)?.jpegData(compressionQuality: 0.9) ?? data
        bloc.onScanRecipePhotoPicked(bytes: NativeBytes.shared.fromData(data: jpeg), fileExtension: "jpg")
    }
}

// MARK: - Search

/// The list's filters and recipe book at the moment search opened.
private struct FilterSnapshot {
    let sort: RecipeSortOption
    let filters: Set<RecipeFilterOption>
    let categories: Set<BuiltinCategory>
    let userCategoryIds: Set<Int64>
    /// The open book, or nil for the cross-book "All recipes" view.
    let bookId: Int64?

    init(_ state: RecipeListBlocModel) {
        sort = state.currentSort
        filters = state.activeFilters
        categories = state.activeCategories
        userCategoryIds = Set(state.activeUserCategoryIds.map { $0.int64Value })
        bookId = state.isAllRecipesSelected ? nil : state.activeBook?.id
    }

    /// Re-applies this snapshot, touching only what changed since it was taken.
    func restore(on bloc: RecipeListBloc, from state: RecipeListBlocModel) {
        let now = FilterSnapshot(state)
        if now.bookId != bookId, let bookId {
            bloc.onBookSelected(bookId: bookId)
        }
        if now.filters != filters || now.categories != categories || now.userCategoryIds != userCategoryIds {
            bloc.onApplySortAndFilters(
                sort: sort,
                filters: filters,
                categories: categories,
                userCategoryIds: Set(userCategoryIds.map { KotlinLong(value: $0) })
            )
        }
    }
}

// MARK: - Scan

private struct ScanningOverlay: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.25).ignoresSafeArea()
            VStack(spacing: ChefTheme.Spacing.normal) {
                ProgressView().controlSize(.large)
                Text(ComposeStrings["recipe_list_scanning_title"]).font(.headline)
                Text(ComposeStrings["recipe_list_scanning_message"])
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)
            }
            .padding(ChefTheme.Spacing.large)
            .frame(maxWidth: 280)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
        }
    }
}

// MARK: - Invites

private struct InviteBanner: View {
    let invite: RecipeBookInvite
    let onAccept: () -> Void
    let onDecline: () -> Void

    var body: some View {
        HStack(spacing: ChefTheme.Spacing.small) {
            Image(systemName: "envelope.badge")
                .foregroundStyle(ChefTheme.primary)
            Text(ComposeStrings.format("recipe_list_invite_message", ["book": invite.bookName]))
                .font(.subheadline)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(ComposeStrings["recipe_list_invite_decline"], action: onDecline)
                .buttonStyle(.borderless)
            Button(ComposeStrings["recipe_list_invite_accept"], action: onAccept)
                .buttonStyle(.borderedProminent)
        }
        .font(.subheadline)
        .padding(ChefTheme.Spacing.normal - 4)
        .background(ChefTheme.primary.opacity(0.1), in: RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - Rows

private struct SelectionBadge: View {
    let isSelected: Bool

    var body: some View {
        Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
            .font(.title3)
            .foregroundStyle(isSelected ? ChefTheme.primary : .secondary)
            .background(Circle().fill(.background).padding(2))
            .accessibilityLabel(isSelected ? "Selected" : "Not selected")
    }
}

/// A recipe's list row; also used for `RecipeSearchView`'s results.
struct RecipeRow: View {
    let recipe: RecipeListItem

    var body: some View {
        HStack(spacing: ChefTheme.Spacing.normal) {
            Color.clear
                .frame(width: 80, height: 80)
                .overlay { RecipeImage(url: recipe.imageUrl) }
                .clipShape(RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
                HStack(spacing: ChefTheme.Spacing.small) {
                    Text(recipe.title)
                        .font(.headline)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                    if recipe.isFavorite {
                        Image(systemName: "heart.fill")
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                    SyncStatusIcon(status: recipe.syncStatus.name)
                }
                if let description = recipe.description_, !description.isEmpty {
                    Text(RecipeTextFormatter.shared.inlineRuns(text: description).attributed)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                StarRating(rating: recipe.starRating?.intValue)
                RecipeInfo(recipe: recipe)
            }
        }
        .padding(.vertical, ChefTheme.Spacing.extraSmall)
        .contentShape(Rectangle())
    }
}

/// Compose's `SyncStatusIcon`: a muted cloud for synced, a slashed cloud for not synced.
private struct SyncStatusIcon: View {
    /// The Kotlin `SyncStatus` entry's `name`. Several exported modules declare their own
    /// `SyncStatus`, so its Swift type name (`SyncStatus`, `SyncStatus_`, …) depends on export order.
    let status: String

    var body: some View {
        switch status {
        case "SYNCED":
            Image(systemName: "checkmark.icloud")
                .font(.footnote)
                .foregroundStyle(.tertiary)
                .accessibilityLabel(ComposeStrings["recipe_sync_synced"])
        case "NOT_SYNCED":
            Image(systemName: "icloud.slash")
                .font(.footnote)
                .foregroundStyle(.secondary)
                .accessibilityLabel(ComposeStrings["recipe_sync_not_synced"])
        default:
            ProgressView()
                .controlSize(.mini)
                .accessibilityLabel(ComposeStrings["recipe_sync_syncing"])
        }
    }
}

private struct RecipeGridCard: View {
    let recipe: RecipeListItem

    var body: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
            // A fixed-size box with the photo center-cropped into it, so the image's own aspect
            // ratio can never widen or heighten the card.
            Color.clear
                .frame(maxWidth: .infinity)
                .frame(height: 120)
                .overlay { RecipeImage(url: recipe.imageUrl) }
                .clipped()
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
                HStack(alignment: .top) {
                    Text(recipe.title)
                        .font(.headline)
                        .lineLimit(2, reservesSpace: true)
                    Spacer(minLength: 0)
                    if recipe.isFavorite {
                        Image(systemName: "heart.fill")
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
                StarRating(rating: recipe.starRating?.intValue)
                RecipeInfo(recipe: recipe)
            }
            .padding([.horizontal, .bottom], ChefTheme.Spacing.small)
        }
        // Fill the grid row so cards beside each other match heights even when one has a rating
        // or more info than the other.
        .frame(maxHeight: .infinity, alignment: .top)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct StarRating: View {
    let rating: Int?

    var body: some View {
        if let rating, rating > 0 {
            HStack(spacing: 2) {
                ForEach(1...5, id: \.self) { index in
                    Image(systemName: index <= rating ? "star.fill" : "star")
                }
            }
            .font(.caption)
            .foregroundStyle(.orange)
        }
    }
}

private struct RecipeInfo: View {
    let recipe: RecipeListItem

    var body: some View {
        // Wraps instead of overflowing, so a narrow grid card never grows wider than its column.
        FlowLayout(spacing: ChefTheme.Spacing.small + 4) {
            if let time = recipe.formattedTotalTime {
                Label { LocalizedText(text: time) } icon: { Image(systemName: "clock") }
            }
            if let servings = recipe.servings {
                Label(ComposeStrings.format("recipe_list_item_servings", ["servings": "\(servings.intValue)"]),
                      systemImage: "fork.knife")
            }
            if let calories = recipe.calories {
                Label(ComposeStrings.format("recipe_list_item_calories", ["calories": "\(calories.intValue)"]),
                      systemImage: "flame")
            }
        }
        .font(.caption)
        .foregroundStyle(.secondary)
        .labelStyle(CompactLabelStyle())
    }
}

private struct CompactLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 4) {
            configuration.icon
            configuration.title
        }
    }
}

// MARK: - Empty states

private struct EmptyRecipesView: View {
    let onBrowse: () -> Void
    let onCreate: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(ComposeStrings["recipe_list_empty_title"], systemImage: "fork.knife")
        } description: {
            Text(ComposeStrings["recipe_list_empty_description"])
        } actions: {
            Button(action: onBrowse) {
                Label(ComposeStrings["recipe_list_empty_browse"], systemImage: "magnifyingglass")
            }
            .buttonStyle(.borderedProminent)
            Button(action: onCreate) {
                Label(ComposeStrings["recipe_list_empty_create"], systemImage: "plus")
            }
            .buttonStyle(.bordered)
        }
        .tint(ChefTheme.primary)
    }
}

/// Compose's `SearchEmptyState`: inside a single book it offers to widen the search to every book.
private struct SearchEmptyView: View {
    let canSearchAllRecipes: Bool
    let onSearchAllRecipes: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(ComposeStrings["recipe_list_search_empty"], systemImage: "magnifyingglass")
        } description: {
            if canSearchAllRecipes {
                Text(ComposeStrings["recipe_list_search_empty_book_hint"])
            }
        } actions: {
            if canSearchAllRecipes {
                Button(action: onSearchAllRecipes) {
                    Label(ComposeStrings["recipe_list_search_all_recipes"], systemImage: "books.vertical")
                }
                .buttonStyle(.borderedProminent)
            }
        }
        .tint(ChefTheme.primary)
    }
}

/// Compose's `FilterEmptyState`: names the active filters and offers to clear them.
private struct FilterEmptyView: View {
    let state: RecipeListBlocModel
    let onClear: () -> Void

    var body: some View {
        let names = state.activeFilters.map { ComposeStrings[$0.labelKey] }
            + state.activeCategories.map { ComposeStrings[$0.labelKey] }
        ContentUnavailableView {
            Label(ComposeStrings["recipe_list_filter_empty_title"], systemImage: "line.3.horizontal.decrease.circle")
        } description: {
            if !names.isEmpty {
                Text(ComposeStrings.format("recipe_list_filter_empty_description",
                                           ["filters": names.sorted().joined(separator: ", ")]))
            }
        } actions: {
            Button(ComposeStrings["recipe_list_clear_filters"], action: onClear)
                .buttonStyle(.bordered)
        }
        .tint(ChefTheme.primary)
    }
}

import SwiftUI
import ComposeApp

/// The redesigned Groceries tab, mirroring the Android `GroceryListScreen`: items grouped under
/// pinned aisle headers (or one alphabetical run), a check-off control per row, swipe to delete
/// (the BLoC's toast offers Undo), and an add row floating at the bottom with autocomplete
/// suggestions opening above it. The list picker is the navigation title's menu; sort & filter is
/// a sheet; delete-items and create-list are native dialogs. Tapping an item opens
/// `GroceryDetailView` in a sheet.
struct GroceryListView: View {
    let bloc: GroceryListBloc
    @StateObject private var model: ObservedFlow<GroceryListBlocModel>
    @StateObject private var newItemName: ObservedFlow<NSString>
    @StateObject private var sheet: ObservedValue<ChildSlot<AnyObject, GroceryListBlocSheet>>
    @State private var showSortFilter = false
    @State private var newListName = ""
    @FocusState private var inputFocused: Bool

    // Scroll to and briefly highlight the item the add row just created. Its id isn't known up
    // front, so a pending add treats the next id that appears as the new item.
    @State private var awaitingAddedItem = false
    @State private var knownItemIds: Set<Int64> = []
    @State private var highlightedItemId: Int64?

    init(bloc: GroceryListBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
        _newItemName = StateObject(wrappedValue: ObservedFlow(bloc.newGroceryItemName))
        _sheet = StateObject(wrappedValue: ObservedValue(bloc.childSlot))
    }

    var body: some View {
        let state = model.value
        ScrollViewReader { proxy in
            itemList(state)
                .onChange(of: itemIds(state)) { _, ids in
                    onItemIdsChanged(ids, proxy: proxy)
                }
        }
        .overlay { emptyState(state) }
        .navigationTitle(state.selectedList?.name ?? ComposeStrings["grocery_list"])
        .navigationBarTitleDisplayMode(.inline)
        .toolbarTitleMenu {
            GroceryListPickerMenu(state: state, bloc: bloc)
        }
        .toolbar { toolbar(state) }
        .refreshable { await sync() }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if state.currentUserRole != .viewer {
                GroceryAddRow(
                    name: Binding(
                        get: { newItemName.current as String },
                        set: { bloc.onNewGroceryItemNameChange(name: $0) }
                    ),
                    suggestions: state.autocompleteSuggestions,
                    queryMatchesSavedAutocomplete: state.queryMatchesSavedAutocomplete,
                    focused: $inputFocused,
                    onAdd: {
                        awaitingAddedItem = true
                        bloc.saveGroceryItem()
                    },
                    onSuggestion: { bloc.onNewGroceryItemNameChange(name: $0) },
                    onSaveAutocomplete: { bloc.onSaveAutocompleteItem(name: $0) }
                )
            }
        }
        // Toasts lift by the larger of the tab bar and this, so clear both: the add row sits on the bar.
        .toastClearance(state.currentUserRole != .viewer ? BottomNavView.tabBarHeight + 64 : 0)
        .tint(ChefTheme.primary)
        .sheet(isPresented: $showSortFilter) {
            GrocerySortFilterSheet(state: state) { sort, filter, recipe in
                bloc.onApplySortAndFilter(sort: sort, filter: filter, recipeFilter: recipe)
                showSortFilter = false
            }
        }
        .sheet(isPresented: detailSheetBinding) {
            if let detail = sheet.value.child?.instance as? GroceryListBlocSheet.GroceryDetail {
                NavigationStack {
                    GroceryDetailView(bloc: detail.bloc)
                }
                .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
            }
        }
        .confirmationDialog(
            ComposeStrings["grocery_delete_items_title"],
            isPresented: alertBinding(\.showDeleteDialog, dismiss: bloc.onDeleteDismissed),
            titleVisibility: .visible
        ) {
            Button(ComposeStrings["grocery_delete_purchased"], role: .destructive, action: bloc.onDeletePurchasedConfirmed)
            Button(ComposeStrings["grocery_delete_all"], role: .destructive, action: bloc.onDeleteAllConfirmed)
            Button(ComposeStrings["grocery_cancel"], role: .cancel, action: bloc.onDeleteDismissed)
        } message: {
            Text(ComposeStrings["grocery_delete_items_message"])
        }
        .alert(
            ComposeStrings["grocery_create_list_title"],
            isPresented: alertBinding(\.showCreateListDialog, dismiss: bloc.onCreateListDismissed)
        ) {
            TextField(ComposeStrings["grocery_create_list_hint"], text: $newListName)
            Button(ComposeStrings["grocery_create_list_confirm"]) {
                bloc.onCreateListConfirmed(name: newListName)
                newListName = ""
            }
            .disabled(newListName.trimmingCharacters(in: .whitespaces).isEmpty)
            Button(ComposeStrings["grocery_create_list_cancel"], role: .cancel) {
                newListName = ""
                bloc.onCreateListDismissed()
            }
        }
    }

    // MARK: List

    @ViewBuilder
    private func itemList(_ state: GroceryListBlocModel) -> some View {
        let showHeaders = state.sort == .aisle
        List {
            ForEach(state.pendingInvitations, id: \.memberId) { invite in
                GroceryInviteBanner(
                    invite: invite,
                    onAccept: { bloc.onAcceptInvitation(invite: invite) },
                    onReject: { bloc.onRejectInvitation(invite: invite) }
                )
                .listRowSeparator(.hidden)
            }
            ForEach(state.groupedItems, id: \.category) { group in
                Section {
                    ForEach(group.items, id: \.id) { item in
                        row(item)
                    }
                } header: {
                    if showHeaders {
                        GroceryCategoryHeader(text: group.category.displayName())
                    }
                }
            }
        }
        .listStyle(.plain)
        .scrollDismissesKeyboard(.immediately)
        .animation(.default, value: itemIds(state))
    }

    private func row(_ item: GroceryItem) -> some View {
        GroceryItemRow(
            item: item,
            onToggle: { bloc.onGroceryItemCheckedChange(item: item, isChecked: !item.isChecked) },
            onOpen: {
                inputFocused = false
                bloc.onGroceryItemClicked(item: item)
            }
        )
        .id(item.id)
        .listRowBackground(
            highlightedItemId == item.id ? ChefTheme.primary.opacity(0.2) : Color.clear
        )
        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
            Button(role: .destructive) { bloc.onGroceryItemDelete(item: item) } label: {
                Label(ComposeStrings["grocery_delete_item"], systemImage: "trash")
            }
            // The screen's brand tint would otherwise override the destructive red.
            .tint(.red)
        }
        .swipeActions(edge: .leading, allowsFullSwipe: true) {
            Button { bloc.onGroceryItemCheckedChange(item: item, isChecked: !item.isChecked) } label: {
                Label(
                    ComposeStrings[item.isChecked ? "grocery_not_checked" : "grocery_checked"],
                    systemImage: item.isChecked ? "arrow.uturn.backward.circle" : "checkmark.circle"
                )
            }
            .tint(ChefTheme.primary)
        }
    }

    @ViewBuilder
    private func emptyState(_ state: GroceryListBlocModel) -> some View {
        if state.groupedItems.isEmpty && !state.isSyncing {
            let filtersApplied = state.filter != .all || state.recipeFilter != nil
            if filtersApplied {
                ContentUnavailableView {
                    Label(ComposeStrings["grocery_list_filtered_empty_title"], systemImage: "line.3.horizontal.decrease.circle")
                } description: {
                    Text(ComposeStrings["grocery_list_filtered_empty_description"])
                } actions: {
                    Button(ComposeStrings["grocery_list_filtered_empty_clear_filters"], action: bloc.onClearFiltersClicked)
                        .buttonStyle(.borderedProminent)
                    Button(ComposeStrings["grocery_list_empty_browse_recipes"], action: bloc.onBrowseRecipesClicked)
                        .buttonStyle(.bordered)
                }
            } else {
                ContentUnavailableView {
                    Label(ComposeStrings["grocery_list_empty_title"], systemImage: "cart")
                } description: {
                    Text(ComposeStrings["grocery_list_empty_description"])
                } actions: {
                    Button(ComposeStrings["grocery_list_empty_browse_recipes"], action: bloc.onBrowseRecipesClicked)
                        .buttonStyle(.borderedProminent)
                }
            }
        }
    }

    // MARK: Toolbar

    @ToolbarContentBuilder
    private func toolbar(_ state: GroceryListBlocModel) -> some ToolbarContent {
        ToolbarItemGroup(placement: .topBarTrailing) {
            let activeCount = (state.filter != .all ? 1 : 0)
                + (state.sort != .aisle ? 1 : 0)
                + (state.recipeFilter != nil ? 1 : 0)
            Button { showSortFilter = true } label: {
                Image(systemName: activeCount > 0
                      ? "line.3.horizontal.decrease.circle.fill"
                      : "line.3.horizontal.decrease.circle")
            }
            .accessibilityLabel(ComposeStrings["grocery_filter"])
            .accessibilityValue(activeCount > 0 ? "\(activeCount)" : "")

            Button(action: bloc.onDeleteClicked) {
                Image(systemName: "trash")
            }
            .accessibilityLabel(ComposeStrings["grocery_delete_items"])

            if state.isSyncing {
                ProgressView()
            } else {
                Button(action: bloc.onSyncClicked) {
                    Image(systemName: "arrow.triangle.2.circlepath")
                }
                .accessibilityLabel(ComposeStrings["grocery_sync_all"])
                .popover(isPresented: alertBinding(\.showSyncTooltip, dismiss: bloc.onSyncTooltipDismissed)) {
                    Text(ComposeStrings["grocery_sync_onboarding"])
                        .font(.callout)
                        .padding()
                        .frame(maxWidth: 260)
                        .presentationCompactAdaptation(.popover)
                }
            }
        }
    }

    // MARK: Behavior

    /// Pull to refresh holds its spinner until the sync the BLoC starts has finished.
    private func sync() async {
        bloc.onSyncClicked()
        for _ in 0..<300 {
            try? await Task.sleep(for: .milliseconds(100))
            if !model.current.isSyncing { return }
        }
    }

    private func itemIds(_ state: GroceryListBlocModel) -> [Int64] {
        state.groupedItems.flatMap { $0.items.map(\.id) }
    }

    private func onItemIdsChanged(_ ids: [Int64], proxy: ScrollViewProxy) {
        let current = Set(ids)
        if awaitingAddedItem, let added = ids.first(where: { !knownItemIds.contains($0) }) {
            awaitingAddedItem = false
            withAnimation { proxy.scrollTo(added, anchor: .center) }
            highlightedItemId = added
            Task {
                try? await Task.sleep(for: .milliseconds(400))
                withAnimation(.easeOut(duration: 1.2)) {
                    if highlightedItemId == added { highlightedItemId = nil }
                }
            }
        }
        knownItemIds = current
    }

    private func alertBinding(_ flag: KeyPath<GroceryListBlocModel, Bool>, dismiss: @escaping () -> Void) -> Binding<Bool> {
        Binding(
            get: { model.value[keyPath: flag] },
            set: { shown in
                if !shown && model.current[keyPath: flag] { dismiss() }
            }
        )
    }

    private var detailSheetBinding: Binding<Bool> {
        Binding(
            get: { sheet.value.child != nil },
            set: { shown in
                // A swipe-down dismisses natively; tell the BLoC so the slot closes too.
                if !shown && sheet.value.child != nil { bloc.onDismissSheet() }
            }
        )
    }
}

// MARK: - Rows

private struct GroceryItemRow: View {
    let item: GroceryItem
    let onToggle: () -> Void
    let onOpen: () -> Void

    var body: some View {
        HStack(spacing: ChefTheme.Spacing.normal - 4) {
            Button(action: onToggle) {
                Image(systemName: item.isChecked ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundStyle(item.isChecked ? ChefTheme.primary : Color.secondary)
                    .contentTransition(.symbolEffect(.replace))
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(ComposeStrings[item.isChecked ? "grocery_checked" : "grocery_not_checked"])

            Button(action: onOpen) {
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(RecipeTextFormatter.shared.inlineRuns(text: item.displayName).attributed)
                            .foregroundStyle(item.isChecked ? Color.secondary : Color.primary)
                            .strikethrough(item.isChecked, color: .secondary)
                        if let quantity = item.quantity {
                            Text(quantity)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                        if let recipe = item.recipeName {
                            Text(ComposeStrings["grocery_recipe_source"].replacingOccurrences(of: "{recipe}", with: recipe))
                                .font(.footnote)
                                .foregroundStyle(.tertiary)
                        }
                    }
                    Spacer(minLength: 0)
                    SyncStatusIcon(status: item.syncStatus.name)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
        }
        .padding(.vertical, 2)
    }
}

private struct SyncStatusIcon: View {
    /// The Kotlin `SyncStatus` entry's `name`. Several exported modules declare their own
    /// `SyncStatus`, so its Swift type name (`SyncStatus`, `SyncStatus_`, …) depends on export order.
    let status: String

    var body: some View {
        Group {
            switch status {
            case "SYNCING":
                ProgressView()
                    .controlSize(.small)
                    .accessibilityLabel(ComposeStrings["grocery_sync_syncing"])
            case "SYNCED":
                Image(systemName: "checkmark.icloud")
                    .accessibilityLabel(ComposeStrings["grocery_sync_synced"])
            default:
                Image(systemName: "icloud.slash")
                    .accessibilityLabel(ComposeStrings["grocery_sync_not_synced"])
            }
        }
        .font(.footnote)
        .foregroundStyle(.secondary)
    }
}

private struct GroceryInviteBanner: View {
    let invite: GroceryListInvite
    let onAccept: () -> Void
    let onReject: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
            Label(
                ComposeStrings["grocery_pending_invite_message"].replacingOccurrences(of: "{list}", with: invite.listName),
                systemImage: "person.2"
            )
            .font(.subheadline)
            HStack {
                Spacer()
                Button(ComposeStrings["grocery_reject"], action: onReject)
                    .buttonStyle(.bordered)
                Button(ComposeStrings["grocery_accept"], action: onAccept)
                    .buttonStyle(.borderedProminent)
            }
            .controlSize(.small)
        }
        .padding(ChefTheme.Spacing.normal - 4)
        .background(ChefTheme.primary.opacity(0.1), in: RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - List picker

/// The navigation title's menu: My Lists, one section per family, Shared with you, plus create and
/// edit. Family lists are managed from Manage Family, so they offer no edit action.
private struct GroceryListPickerMenu: View {
    let state: GroceryListBlocModel
    let bloc: GroceryListBloc

    var body: some View {
        let myLists = state.lists.filter { !$0.isShared && !$0.isFamily }
        let sharedLists = state.lists.filter(\.isShared)
        let familyLists = state.lists.filter(\.isFamily)
        let familyIds = familyLists.compactMap(\.familyId).reduce(into: [String]()) { ids, id in
            if !ids.contains(id) { ids.append(id) }
        }

        Section(ComposeStrings["grocery_my_lists"]) {
            ForEach(myLists, id: \.id) { listButton($0) }
            Button(action: bloc.onCreateListClicked) {
                Label(ComposeStrings["grocery_create_new_list"], systemImage: "plus")
            }
        }
        ForEach(familyIds, id: \.self) { familyId in
            let lists = familyLists.filter { $0.familyId == familyId }
            Section(lists.first?.familyName ?? ComposeStrings["grocery_family_lists_fallback"]) {
                ForEach(lists, id: \.id) { listButton($0) }
            }
        }
        if !sharedLists.isEmpty {
            Section(ComposeStrings["grocery_shared_with_you"]) {
                ForEach(sharedLists, id: \.id) { listButton($0) }
            }
        }
        if let selected = state.selectedList, !selected.isFamily {
            Section {
                Button { bloc.onEditListClicked(list: selected) } label: {
                    Label(ComposeStrings["grocery_edit_list"], systemImage: "pencil")
                }
            }
        }
    }

    private func listButton(_ list: GroceryListModel) -> some View {
        Button { bloc.onListSelected(list: list) } label: {
            if list.id == state.selectedList?.id {
                Image(systemName: "checkmark")
            }
            Text(list.name)
            if list.isShared {
                Text(ComposeStrings["grocery_shared_badge"])
            }
        }
    }
}

// MARK: - Add row

/// The add-item field floating over the bottom of the list, with autocomplete suggestions opening
/// above it (a dropdown below would open behind the keyboard). Return adds and keeps typing; Done
/// adds whatever is left and dismisses the keyboard.
private struct GroceryAddRow: View {
    @Binding var name: String
    let suggestions: [String]
    let queryMatchesSavedAutocomplete: Bool
    var focused: FocusState<Bool>.Binding
    let onAdd: () -> Void
    let onSuggestion: (String) -> Void
    let onSaveAutocomplete: (String) -> Void

    var body: some View {
        let query = name.trimmingCharacters(in: .whitespaces)
        // Offer to save the typed text only when no suggestion already matches it and it isn't
        // already in the saved autocomplete library.
        let saveQuery: String? = !query.isEmpty && !queryMatchesSavedAutocomplete
            && !suggestions.contains { $0.caseInsensitiveCompare(query) == .orderedSame } ? query : nil
        let showSuggestions = focused.wrappedValue && !queryMatchesSavedAutocomplete
            && (!suggestions.isEmpty || saveQuery != nil)

        VStack(spacing: ChefTheme.Spacing.small) {
            if showSuggestions {
                VStack(spacing: 0) {
                    ForEach(suggestions, id: \.self) { suggestion in
                        suggestionRow { onSuggestion(suggestion) } label: { Text(suggestion) }
                        Divider()
                    }
                    if let saveQuery {
                        suggestionRow { onSaveAutocomplete(saveQuery) } label: {
                            Label(
                                ComposeStrings["grocery_save_autocomplete"].replacingOccurrences(of: "{item}", with: saveQuery),
                                systemImage: "plus"
                            )
                        }
                    }
                }
                .chefGlass(in: RoundedRectangle(cornerRadius: 20))
                .transition(.opacity.combined(with: .move(edge: .bottom)))
            }
            HStack(spacing: ChefTheme.Spacing.small) {
                TextField(ComposeStrings["grocery_add_item_hint"], text: $name)
                    .focused(focused)
                    .submitLabel(.send)
                    .textInputAutocapitalization(.sentences)
                    .onSubmit {
                        if query.isEmpty {
                            focused.wrappedValue = false
                        } else {
                            onAdd()
                            // Keep the keyboard up so the next item can be typed straight away.
                            focused.wrappedValue = true
                        }
                    }
                    .padding(.horizontal, ChefTheme.Spacing.normal)
                    .frame(height: 48)
                    .chefGlass()
                if focused.wrappedValue {
                    Button {
                        if !name.trimmingCharacters(in: .whitespaces).isEmpty { onAdd() }
                        focused.wrappedValue = false
                    } label: {
                        Text(ComposeStrings["grocery_done"])
                            .font(.headline)
                            .padding(.horizontal, ChefTheme.Spacing.normal)
                            .frame(height: 48)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(ChefTheme.primary)
                    .chefGlass()
                    .transition(.opacity.combined(with: .move(edge: .trailing)))
                }
            }
        }
        .padding(.horizontal, ChefTheme.Spacing.normal)
        .padding(.bottom, ChefTheme.Spacing.small)
        .animation(.snappy, value: focused.wrappedValue)
        .animation(.snappy, value: showSuggestions)
    }

    private func suggestionRow<Label: View>(action: @escaping () -> Void, @ViewBuilder label: () -> Label) -> some View {
        Button(action: action) {
            label()
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, ChefTheme.Spacing.normal)
                .padding(.vertical, ChefTheme.Spacing.small + 4)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Sort & filter

private struct GrocerySortFilterSheet: View {
    let state: GroceryListBlocModel
    let onApply: (GroceryListBlocGrocerySort, GroceryListBlocGroceryFilter, String?) -> Void
    @State private var sort: GroceryListBlocGrocerySort
    @State private var filter: GroceryListBlocGroceryFilter
    @State private var recipeFilter: String?

    init(state: GroceryListBlocModel, onApply: @escaping (GroceryListBlocGrocerySort, GroceryListBlocGroceryFilter, String?) -> Void) {
        self.state = state
        self.onApply = onApply
        _sort = State(initialValue: state.sort)
        _filter = State(initialValue: state.filter)
        _recipeFilter = State(initialValue: state.recipeFilter)
    }

    var body: some View {
        let noRecipe = GroceryListBlocCompanion.shared.NO_RECIPE_FILTER
        NavigationStack {
            Form {
                Section(ComposeStrings["grocery_sort_by"]) {
                    Picker(ComposeStrings["grocery_sort_by"], selection: $sort) {
                        Text(ComposeStrings["grocery_sort_aisle"]).tag(GroceryListBlocGrocerySort.aisle)
                        Text(ComposeStrings["grocery_sort_alphabetical"]).tag(GroceryListBlocGrocerySort.alphabetical)
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                }
                Section(ComposeStrings["grocery_filter_by"]) {
                    Picker(ComposeStrings["grocery_filter_by"], selection: $filter) {
                        Text(ComposeStrings["grocery_filter_all"]).tag(GroceryListBlocGroceryFilter.all)
                        Text(ComposeStrings["grocery_filter_unpurchased"]).tag(GroceryListBlocGroceryFilter.unpurchased)
                        Text(ComposeStrings["grocery_filter_purchased"]).tag(GroceryListBlocGroceryFilter.purchased)
                    }
                    .pickerStyle(.segmented)
                    .labelsHidden()
                }
                if !state.availableRecipes.isEmpty || state.hasNoRecipeItems {
                    Section(ComposeStrings["grocery_filter_by_recipe"]) {
                        ForEach(state.availableRecipes, id: \.self) { recipe in
                            recipeRow(recipe, value: recipe)
                        }
                        if state.hasNoRecipeItems {
                            recipeRow(ComposeStrings["grocery_filter_no_recipe"], value: noRecipe)
                        }
                    }
                }
            }
            .navigationTitle(ComposeStrings["grocery_sort_and_filter"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    if sort != .aisle || filter != .all || recipeFilter != nil {
                        Button(ComposeStrings["grocery_clear_filters"]) {
                            sort = .aisle
                            filter = .all
                            recipeFilter = nil
                        }
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(ComposeStrings["grocery_apply"]) { onApply(sort, filter, recipeFilter) }
                }
            }
        }
        .presentationDetents([.medium, .large])
        .tint(ChefTheme.primary)
    }

    private func recipeRow(_ title: String, value: String) -> some View {
        Button {
            recipeFilter = recipeFilter == value ? nil : value
        } label: {
            HStack {
                Text(title).foregroundStyle(Color.primary)
                Spacer()
                if recipeFilter == value {
                    Image(systemName: "checkmark").foregroundStyle(ChefTheme.primary)
                }
            }
        }
    }
}

/// A pinned aisle header on a solid gray band, like the Android `CategoryHeader` (a
/// `surfaceVariant` surface), so sections stand apart more than the default translucent header.
private struct GroceryCategoryHeader: View {
    let text: TextData

    var body: some View {
        LocalizedText(text: text)
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(.secondary)
            .textCase(nil)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .padding(.vertical, ChefTheme.Spacing.small)
            .background(Color(.secondarySystemBackground))
            .listRowInsets(EdgeInsets())
    }
}

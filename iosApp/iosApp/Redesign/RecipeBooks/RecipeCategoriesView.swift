import SwiftUI
import ComposeApp

/// The redesigned category manager (Settings → Categories), mirroring the Android
/// `RecipeCategoriesScreen`: "Your categories" with an inline create row, then the read-only
/// built-ins. Each custom row has a rename / delete menu (also on long press), and selection mode
/// bulk-deletes from the navigation bar.
struct RecipeCategoriesView: View {
    let bloc: RecipeCategoriesBloc
    @StateObject private var model: ObservedFlow<RecipeCategoriesBlocModel>
    @FocusState private var createFocused: Bool
    @State private var renameText = ""

    init(bloc: RecipeCategoriesBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        let userItems = state.categories.filter { !$0.isBuiltin }
        let builtinItems = state.categories.filter { $0.isBuiltin }
        let isCreating = state.createState is RecipeCategoriesBlocCreateState.Editing

        List {
            Section {
                if isCreating {
                    HStack {
                        TextField(
                            ComposeStrings["recipe_categories_create_placeholder"],
                            text: Binding(
                                get: { (model.current.createState as? RecipeCategoriesBlocCreateState.Editing)?.text ?? "" },
                                set: { bloc.onCreateTextChanged(text: $0) }
                            )
                        )
                        .autocorrectionDisabled()
                        .submitLabel(.done)
                        .focused($createFocused)
                        .onSubmit(submitCreate)
                        .onAppear { createFocused = true }
                        Button(action: submitCreate) {
                            Image(systemName: "checkmark.circle.fill").font(.title3)
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(ComposeStrings["recipe_categories_create_a11y"])
                        Button(action: bloc.onCreateCancelled) {
                            Image(systemName: "xmark.circle.fill").font(.title3).foregroundStyle(.secondary)
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(ComposeStrings["recipe_categories_create_cancel_a11y"])
                    }
                }
                if userItems.isEmpty && !isCreating {
                    Text(ComposeStrings["recipe_categories_empty_user"])
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(userItems, id: \.rowKey) { row($0, state: state) }
                }
            } header: {
                HStack {
                    Text(ComposeStrings["recipe_categories_section_user"])
                    Spacer()
                    // Hidden in selection mode (the bar's trash owns that flow) and while the
                    // create row is already open.
                    if !state.selectionMode && !isCreating {
                        Button(action: bloc.onCreateClicked) {
                            Image(systemName: "plus")
                        }
                        .accessibilityLabel(ComposeStrings["recipe_categories_create_a11y"])
                    }
                }
            }

            Section(ComposeStrings["recipe_categories_section_builtin"]) {
                ForEach(builtinItems, id: \.rowKey) { row($0, state: state) }
            }
        }
        .listStyle(.insetGrouped)
        .overlay {
            if state.isLoading && state.categories.isEmpty { ProgressView() }
        }
        .animation(.default, value: isCreating)
        .navigationTitle(title(state))
        .navigationBarTitleDisplayMode(state.selectionMode ? .inline : .large)
        .navigationBarBackButtonHidden(state.selectionMode)
        .toolbar { toolbar(state) }
        .tint(ChefTheme.primary)
        .alert(
            ComposeStrings["category_rename_title"],
            isPresented: dialogBinding(RecipeCategoriesBlocDialogState.Rename.self, dismiss: bloc.onRenameDismissed)
        ) {
            TextField(ComposeStrings["category_rename_placeholder"], text: $renameText)
                .autocorrectionDisabled()
            Button(ComposeStrings["category_dialog_cancel"], role: .cancel, action: bloc.onRenameDismissed)
            Button(ComposeStrings["category_rename_confirm"]) {
                if let rename = model.current.dialog as? RecipeCategoriesBlocDialogState.Rename,
                   !renameText.trimmingCharacters(in: .whitespaces).isEmpty {
                    bloc.onRenameSubmitted(id: rename.target.id, newName: renameText)
                } else {
                    bloc.onRenameDismissed()
                }
            }
        }
        .alert(
            ComposeStrings["category_delete_title"],
            isPresented: dialogBinding(RecipeCategoriesBlocDialogState.Delete.self, dismiss: bloc.onDeleteDismissed)
        ) {
            Button(ComposeStrings["category_delete_confirm"], role: .destructive, action: bloc.onDeleteConfirmed)
            Button(ComposeStrings["category_dialog_cancel"], role: .cancel, action: bloc.onDeleteDismissed)
        } message: {
            if let delete = state.dialog as? RecipeCategoriesBlocDialogState.Delete {
                Text(ComposeStrings["category_delete_message", ["category": delete.target.name]])
            }
        }
        .alert(
            ComposeStrings["recipe_categories_bulk_delete_title"],
            isPresented: dialogBinding(RecipeCategoriesBlocDialogState.BulkDelete.self, dismiss: bloc.onBulkDeleteDismissed)
        ) {
            Button(ComposeStrings["category_delete"], role: .destructive, action: bloc.onBulkDeleteConfirmed)
            Button(ComposeStrings["recipe_categories_back"], role: .cancel, action: bloc.onBulkDeleteDismissed)
        } message: {
            if let bulk = state.dialog as? RecipeCategoriesBlocDialogState.BulkDelete {
                Text(bulk.count == 1
                    ? ComposeStrings["recipe_categories_bulk_delete_message_one"]
                    : ComposeStrings["recipe_categories_bulk_delete_message_other", ["count": "\(bulk.count)"]])
            }
        }
        .onChange(of: (state.dialog as? RecipeCategoriesBlocDialogState.Rename)?.target.id) { _, _ in
            // Seed the rename field with the category's current name each time the dialog opens.
            if let rename = model.current.dialog as? RecipeCategoriesBlocDialogState.Rename {
                renameText = rename.target.name
            }
        }
    }

    @ViewBuilder
    private func row(_ item: RecipeCategoriesBlocCategoryItem, state: RecipeCategoriesBlocModel) -> some View {
        let selected = state.selectedIds.contains(KotlinLong(value: item.id))
        let disabled = state.selectionMode && !item.isEditable
        Button { bloc.onCategoryClicked(item: item) } label: {
            HStack(spacing: ChefTheme.Spacing.normal - 4) {
                if state.selectionMode {
                    Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                        .font(.title3)
                        .foregroundStyle(selected ? ChefTheme.primary : Color.secondary)
                        .opacity(item.isEditable ? 1 : 0)
                }
                // Explicit colors: inside a List, a Button would otherwise tint the whole label.
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.displayName).foregroundStyle(Color.primary)
                    Text(item.subtitle)
                        .font(.footnote)
                        .foregroundStyle(Color.secondary)
                }
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
        }
        .disabled(disabled)
        .opacity(disabled ? 0.4 : 1)
        .contextMenu {
            if item.isEditable && !state.selectionMode {
                Button { bloc.onRenameRequested(item: item) } label: {
                    Label(ComposeStrings["category_rename"], systemImage: "pencil")
                }
                // The Android long press: enter selection mode with this row selected.
                Button { bloc.onCategoryLongClicked(item: item) } label: {
                    Label(ComposeStrings["recipe_categories_select_mode_a11y"], systemImage: "checkmark.circle")
                }
                Divider()
                Button(role: .destructive) { bloc.onDeleteRequested(item_: item) } label: {
                    Label(ComposeStrings["category_delete"], systemImage: "trash")
                }
            }
        }
        .swipeActions(edge: .trailing) {
            if item.isEditable && !state.selectionMode {
                Button(role: .destructive) { bloc.onDeleteRequested(item_: item) } label: {
                    Label(ComposeStrings["category_delete"], systemImage: "trash")
                }
                Button { bloc.onRenameRequested(item: item) } label: {
                    Label(ComposeStrings["category_rename"], systemImage: "pencil")
                }
                .tint(.gray)
            }
        }
        .accessibilityAddTraits(state.selectionMode && selected ? .isSelected : [])
    }

    @ToolbarContentBuilder
    private func toolbar(_ state: RecipeCategoriesBlocModel) -> some ToolbarContent {
        if state.selectionMode {
            ToolbarItem(placement: .cancellationAction) {
                Button(action: bloc.onCancelSelection) {
                    Label(ComposeStrings["recipe_categories_selection_close_a11y"], systemImage: "xmark")
                }
            }
            if state.selectedCount > 0 {
                ToolbarItem(placement: .primaryAction) {
                    Button(role: .destructive, action: bloc.onBulkDeleteRequested) {
                        Label(ComposeStrings["recipe_categories_selection_delete_a11y"], systemImage: "trash")
                    }
                }
            }
        } else {
            ToolbarItem(placement: .primaryAction) {
                Button(action: bloc.onSelectModeClicked) {
                    Label(ComposeStrings["recipe_categories_select_mode_a11y"], systemImage: "checklist")
                }
            }
        }
    }

    private func title(_ state: RecipeCategoriesBlocModel) -> String {
        guard state.selectionMode else { return ComposeStrings["recipe_categories_title"] }
        return state.selectedCount == 0
            ? ComposeStrings["recipe_categories_selection_empty_title"]
            : ComposeStrings["recipe_categories_selection_count", ["count": "\(state.selectedCount)"]]
    }

    private func submitCreate() {
        let text = (model.current.createState as? RecipeCategoriesBlocCreateState.Editing)?.text ?? ""
        if !text.trimmingCharacters(in: .whitespaces).isEmpty { bloc.onCreateSubmitted() }
    }

    /// An alert's visibility, driven by the model's dialog being of type `dialog`. Dismissing it
    /// only notifies the BLoC if that dialog is still showing, so a confirm isn't followed by a
    /// dismiss.
    private func dialogBinding<D>(_ dialog: D.Type, dismiss: @escaping () -> Void) -> Binding<Bool> {
        Binding(
            get: { model.value.dialog is D },
            set: { shown in
                if !shown && model.current.dialog is D { dismiss() }
            }
        )
    }
}

private extension RecipeCategoriesBlocCategoryItem {
    var rowKey: String { id != 0 ? "id-\(id)" : "builtin-\(builtinId ?? "")" }

    var displayName: String {
        if let builtinId {
            let key = "recipe_category_\(builtinId)"
            let label = ComposeStrings[key]
            return label == key ? name : label
        }
        return name
    }

    var subtitle: String {
        let count: String = switch recipeCount {
        case 0: ComposeStrings["recipe_categories_recipe_count_zero"]
        case 1: ComposeStrings["recipe_categories_recipe_count_one"]
        default: ComposeStrings["recipe_categories_recipe_count_other", ["count": "\(recipeCount)"]]
        }
        return isBuiltin ? "\(ComposeStrings["recipe_categories_built_in_label"]) • \(count)" : count
    }
}

import SwiftUI
import ComposeApp

/// The native `CategoryPickerSheet`: built-in categories, then the user's own, each toggled with a
/// checkmark. "+" creates a category (attached right away); swipe or long-press a user category to
/// rename or delete it. Changes apply immediately through the BLoC, so "Done" just closes.
struct CategoryPickerView: View {
    let bloc: EditRecipeBloc
    @ObservedObject var selected: ObservedFlow<NSSet>
    @ObservedObject var userCategories: ObservedFlow<NSArray>
    @SwiftUI.Environment(\.dismiss) private var dismiss

    @State private var isCreating = false
    @State private var newName = ""
    @State private var renameTarget: ComposeApp.Category?
    @State private var renameText = ""
    @State private var deleteTarget: ComposeApp.Category?

    var body: some View {
        let selectedCategories = selected.value.allObjects.compactMap { $0 as? ComposeApp.Category }
        let attachedBuiltinIds = Set(selectedCategories.compactMap(\.builtinId))
        let attachedIds = Set(selectedCategories.map(\.id))
        let formatter = RecipeTextFormatter.shared
        // observeUserCategories also returns materialized presets (non-nil builtinId); those are
        // already listed with the built-ins.
        let custom = userCategories.value.compactMap { $0 as? ComposeApp.Category }.filter { $0.builtinId == nil }

        NavigationStack {
            List {
                Section {
                    ForEach(formatter.builtinCategories, id: \.id) { builtin in
                        let attached = attachedBuiltinIds.contains(builtin.id)
                        CheckRow(title: formatter.builtinLabel(builtin: builtin, lookup: ComposeStrings.shared), checked: attached) {
                            if attached {
                                if let row = selectedCategories.first(where: { $0.builtinId == builtin.id }) {
                                    bloc.onDetachCategory(category: row)
                                }
                            } else {
                                bloc.onAttachBuiltin(builtin: builtin)
                            }
                        }
                    }
                }
                if !custom.isEmpty {
                    Section {
                        ForEach(custom, id: \.id) { category in
                            let attached = attachedIds.contains(category.id)
                            CheckRow(title: category.name, checked: attached) {
                                if attached { bloc.onDetachCategory(category: category) } else { bloc.onAttachCategory(category: category) }
                            }
                            .swipeActions {
                                Button(ComposeStrings["category_delete"], role: .destructive) { deleteTarget = category }
                                Button(ComposeStrings["category_rename"]) { startRename(category) }
                            }
                            .contextMenu {
                                Button { startRename(category) } label: {
                                    Label(ComposeStrings["category_rename"], systemImage: "pencil")
                                }
                                Button(role: .destructive) { deleteTarget = category } label: {
                                    Label(ComposeStrings["category_delete"], systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle(ComposeStrings["edit_recipe_field_category_picker_title"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        newName = ""
                        isCreating = true
                    } label: {
                        Label(ComposeStrings["edit_recipe_field_category_create_a11y"], systemImage: "plus")
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(ComposeStrings["edit_recipe_field_category_save"]) { dismiss() }
                }
            }
            .alert(ComposeStrings["edit_recipe_field_category_create_a11y"], isPresented: $isCreating) {
                TextField(ComposeStrings["edit_recipe_field_category_create_placeholder"], text: $newName)
                Button(ComposeStrings["edit_recipe_field_category_create_confirm"]) {
                    let name = newName.trimmingCharacters(in: .whitespaces)
                    if !name.isEmpty { bloc.onCreateUserCategory(name: name) }
                }
                Button(ComposeStrings["edit_recipe_discard_cancel"], role: .cancel) {}
            }
            .alert(ComposeStrings["category_rename_title"], isPresented: Binding(get: { renameTarget != nil }, set: { if !$0 { renameTarget = nil } })) {
                TextField(ComposeStrings["category_rename_placeholder"], text: $renameText)
                Button(ComposeStrings["category_rename_confirm"]) {
                    let name = renameText.trimmingCharacters(in: .whitespaces)
                    if let target = renameTarget, !name.isEmpty { bloc.onRenameCategory(id: target.id, newName: name) }
                }
                Button(ComposeStrings["edit_recipe_discard_cancel"], role: .cancel) {}
            }
            .alert(ComposeStrings["category_delete_title"], isPresented: Binding(get: { deleteTarget != nil }, set: { if !$0 { deleteTarget = nil } })) {
                Button(ComposeStrings["category_delete_confirm"], role: .destructive) {
                    if let target = deleteTarget { bloc.onDeleteCategory(id: target.id) }
                }
                Button(ComposeStrings["edit_recipe_discard_cancel"], role: .cancel) {}
            } message: {
                Text(ComposeStrings["category_delete_message"].replacingOccurrences(of: "{category}", with: deleteTarget?.name ?? ""))
            }
        }
        .tint(ChefTheme.primary)
    }

    private func startRename(_ category: ComposeApp.Category) {
        renameText = category.name
        renameTarget = category
    }
}

private struct CheckRow: View {
    let title: String
    let checked: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(title).foregroundStyle(.primary)
                Spacer()
                if checked {
                    Image(systemName: "checkmark")
                        .fontWeight(.semibold)
                        .foregroundStyle(ChefTheme.primary)
                }
            }
            .contentShape(Rectangle())
        }
        .accessibilityAddTraits(checked ? .isSelected : [])
    }
}

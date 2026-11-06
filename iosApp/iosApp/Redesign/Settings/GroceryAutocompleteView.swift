import SwiftUI
import ComposeApp

/// The redesigned grocery autocomplete manager (Settings → Grocery autocomplete), mirroring the
/// Android `GroceryAutocompleteSettingsScreen`: "Your items" with an inline create row and a
/// delete per row (swipe, context menu, or the row menu), then the read-only built-in suggestions.
struct GroceryAutocompleteView: View {
    let bloc: GroceryAutocompleteBloc
    @StateObject private var model: ObservedFlow<GroceryAutocompleteBlocModel>
    @FocusState private var createFocused: Bool

    init(bloc: GroceryAutocompleteBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        let isCreating = state.createState is GroceryAutocompleteBlocCreateState.Editing

        List {
            Section {
                if isCreating {
                    HStack {
                        TextField(
                            ComposeStrings["grocery_autocomplete_create_placeholder"],
                            text: Binding(
                                get: { (model.current.createState as? GroceryAutocompleteBlocCreateState.Editing)?.text ?? "" },
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
                        .accessibilityLabel(ComposeStrings["grocery_autocomplete_create_confirm_a11y"])
                        Button(action: bloc.onCreateCancelled) {
                            Image(systemName: "xmark.circle.fill").font(.title3).foregroundStyle(.secondary)
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(ComposeStrings["grocery_autocomplete_create_cancel_a11y"])
                    }
                }
                if state.userItems.isEmpty {
                    if !isCreating {
                        Text(ComposeStrings["grocery_autocomplete_empty_user"])
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                } else {
                    ForEach(state.userItems, id: \.id) { item in
                        userRow(item)
                    }
                }
            } header: {
                HStack {
                    Text(ComposeStrings["grocery_autocomplete_section_user"])
                    Spacer()
                    if !isCreating {
                        Button(action: bloc.onCreateClicked) {
                            Image(systemName: "plus")
                        }
                        .accessibilityLabel(ComposeStrings["grocery_autocomplete_create_a11y"])
                    }
                }
            }

            if !state.defaults.isEmpty {
                Section(ComposeStrings["grocery_autocomplete_section_defaults"]) {
                    ForEach(state.defaults, id: \.self) { name in
                        Text(name)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .overlay {
            if state.isLoading && state.userItems.isEmpty && state.defaults.isEmpty { ProgressView() }
        }
        .animation(.default, value: isCreating)
        .navigationTitle(ComposeStrings["grocery_autocomplete_title"])
        .tint(ChefTheme.primary)
        .alert(
            ComposeStrings["grocery_autocomplete_delete_title"],
            isPresented: Binding(
                get: { model.value.dialog is GroceryAutocompleteBlocDialogState.Delete },
                set: { shown in
                    if !shown && model.current.dialog is GroceryAutocompleteBlocDialogState.Delete {
                        bloc.onDeleteDismissed()
                    }
                }
            )
        ) {
            Button(ComposeStrings["grocery_autocomplete_delete_confirm"], role: .destructive, action: bloc.onDeleteConfirmed)
            Button(ComposeStrings["grocery_autocomplete_delete_cancel"], role: .cancel, action: bloc.onDeleteDismissed)
        } message: {
            if let delete = state.dialog as? GroceryAutocompleteBlocDialogState.Delete {
                Text(ComposeStrings["grocery_autocomplete_delete_message", ["item": delete.target.name]])
            }
        }
    }

    private func userRow(_ item: GroceryAutocompleteBlocItem) -> some View {
        HStack {
            Text(item.name)
            Spacer()
            Menu {
                deleteButton(item)
            } label: {
                Image(systemName: "ellipsis")
                    .foregroundStyle(.secondary)
                    .frame(width: 32, height: 32)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(ComposeStrings["grocery_autocomplete_more_a11y", ["item": item.name]])
        }
        .contextMenu { deleteButton(item) }
        .swipeActions(edge: .trailing) { deleteButton(item) }
    }

    private func deleteButton(_ item: GroceryAutocompleteBlocItem) -> some View {
        Button(role: .destructive) { bloc.onDeleteRequested(item: item) } label: {
            Label(ComposeStrings["grocery_autocomplete_delete"], systemImage: "trash")
        }
    }

    private func submitCreate() {
        let text = (model.current.createState as? GroceryAutocompleteBlocCreateState.Editing)?.text ?? ""
        if !text.trimmingCharacters(in: .whitespaces).isEmpty { bloc.onCreateSubmitted() }
    }
}

#Preview("Items") {
    NavigationStack { GroceryAutocompleteView(bloc: GroceryAutocompletePreviewsKt.previewGroceryAutocompleteBloc) }
}

#Preview("Creating") {
    NavigationStack { GroceryAutocompleteView(bloc: GroceryAutocompletePreviewsKt.previewGroceryAutocompleteBlocCreating) }
}

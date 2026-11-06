import SwiftUI
import ComposeApp

/// The redesigned aisle-rule manager (Settings → Grocery category rules), mirroring the Android
/// `GroceryCategoryRulesScreen`: "Your rules" as "name → aisle" rows, an inline create row (name
/// field plus an aisle picker), and a delete per row (swipe, context menu, or the row menu).
struct GroceryCategoryRulesView: View {
    let bloc: GroceryCategoryRulesBloc
    @StateObject private var model: ObservedFlow<GroceryCategoryRulesBlocModel>
    @FocusState private var createFocused: Bool

    init(bloc: GroceryCategoryRulesBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        let editing = state.createState as? GroceryCategoryRulesBlocCreateState.Editing

        List {
            Section {
                if let editing {
                    HStack {
                        TextField(
                            ComposeStrings["grocery_category_rules_name_placeholder"],
                            text: Binding(
                                get: { currentEditing?.name ?? "" },
                                set: { bloc.onCreateNameChanged(name: $0) }
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
                        .accessibilityLabel(ComposeStrings["grocery_category_rules_create_confirm_a11y"])
                        Button(action: bloc.onCreateCancelled) {
                            Image(systemName: "xmark.circle.fill").font(.title3).foregroundStyle(.secondary)
                        }
                        .buttonStyle(.borderless)
                        .accessibilityLabel(ComposeStrings["grocery_category_rules_create_cancel_a11y"])
                    }
                    Picker(
                        ComposeStrings["grocery_category_rules_category_label"],
                        selection: Binding(
                            get: { currentEditing?.category ?? editing.category },
                            set: { bloc.onCreateCategorySelected(category: $0) }
                        )
                    ) {
                        ForEach(GroceryCategory.entries, id: \.self) { category in
                            LocalizedText(text: category.displayName()).tag(category)
                        }
                    }
                }
                if state.rules.isEmpty {
                    if editing == nil {
                        Text(ComposeStrings["grocery_category_rules_empty"])
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                } else {
                    ForEach(state.rules, id: \.id) { rule in
                        ruleRow(rule)
                    }
                }
            } header: {
                HStack {
                    Text(ComposeStrings["grocery_category_rules_section"])
                    Spacer()
                    if editing == nil {
                        Button(action: bloc.onCreateClicked) {
                            Image(systemName: "plus")
                        }
                        .accessibilityLabel(ComposeStrings["grocery_category_rules_create_a11y"])
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .overlay {
            if state.isLoading && state.rules.isEmpty && editing == nil { ProgressView() }
        }
        .animation(.default, value: editing != nil)
        .navigationTitle(ComposeStrings["grocery_category_rules_title"])
        .tint(ChefTheme.primary)
        .alert(
            ComposeStrings["grocery_category_rules_delete_title"],
            isPresented: Binding(
                get: { model.value.dialog is GroceryCategoryRulesBlocDialogState.Delete },
                set: { shown in
                    if !shown && model.current.dialog is GroceryCategoryRulesBlocDialogState.Delete {
                        bloc.onDeleteDismissed()
                    }
                }
            )
        ) {
            Button(ComposeStrings["grocery_category_rules_delete_confirm"], role: .destructive, action: bloc.onDeleteConfirmed)
            Button(ComposeStrings["grocery_category_rules_delete_cancel"], role: .cancel, action: bloc.onDeleteDismissed)
        } message: {
            if let delete = state.dialog as? GroceryCategoryRulesBlocDialogState.Delete {
                Text(ComposeStrings["grocery_category_rules_delete_message", ["name": delete.target.name]])
            }
        }
    }

    private var currentEditing: GroceryCategoryRulesBlocCreateState.Editing? {
        model.current.createState as? GroceryCategoryRulesBlocCreateState.Editing
    }

    private func ruleRow(_ rule: GroceryCategoryRulesBlocRule) -> some View {
        HStack {
            Text(ComposeStrings["grocery_category_rules_row_summary", [
                "name": rule.name,
                "aisle": rule.category.displayName().localized,
            ]])
            Spacer()
            Menu {
                deleteButton(rule)
            } label: {
                Image(systemName: "ellipsis")
                    .foregroundStyle(.secondary)
                    .frame(width: 32, height: 32)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(ComposeStrings["grocery_category_rules_more_a11y", ["name": rule.name]])
        }
        .contextMenu { deleteButton(rule) }
        .swipeActions(edge: .trailing) { deleteButton(rule) }
    }

    private func deleteButton(_ rule: GroceryCategoryRulesBlocRule) -> some View {
        Button(role: .destructive) { bloc.onDeleteRequested(rule: rule) } label: {
            Label(ComposeStrings["grocery_category_rules_delete"], systemImage: "trash")
        }
    }

    private func submitCreate() {
        let name = currentEditing?.name ?? ""
        if !name.trimmingCharacters(in: .whitespaces).isEmpty { bloc.onCreateSubmitted() }
    }
}

#Preview("Rules") {
    NavigationStack { GroceryCategoryRulesView(bloc: GroceryCategoryRulesPreviewsKt.previewGroceryCategoryRulesBloc) }
}

#Preview("Creating") {
    NavigationStack { GroceryCategoryRulesView(bloc: GroceryCategoryRulesPreviewsKt.previewGroceryCategoryRulesBlocCreating) }
}

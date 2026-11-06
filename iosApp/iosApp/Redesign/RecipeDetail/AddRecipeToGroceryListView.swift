import SwiftUI
import ComposeApp

/// The redesigned "Add to grocery list" sheet opened from recipe detail, mirroring the Android
/// `AddRecipeToGroceryListScreen`: a list picker (only when there's more than one list, grouped
/// Personal / per family / Shared with you), the ingredient scale, then the recipe's ingredients
/// grouped by aisle with a check per row. Add sits in the navigation bar.
struct AddRecipeToGroceryListView: View {
    let bloc: AddRecipeToGroceryListBloc
    @StateObject private var model: ObservedFlow<AddRecipeToGroceryListBlocModel>

    init(bloc: AddRecipeToGroceryListBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        content(state)
            .navigationTitle(ComposeStrings["recipe_add_to_grocery_list"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: bloc.onBackClicked) {
                        Label(ComposeStrings["recipe_add_to_grocery_list_back"], systemImage: "xmark")
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    if state.isAdding {
                        ProgressView()
                    } else {
                        Button(ComposeStrings["recipe_add_to_grocery_list_add"], action: bloc.onSaveClicked)
                            .disabled(state.isLoading || !state.hasSelectedIngredients)
                    }
                }
            }
            .tint(ChefTheme.primary)
    }

    @ViewBuilder
    private func content(_ state: AddRecipeToGroceryListBlocModel) -> some View {
        if state.isLoading {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if state.isEmpty {
            StatusBlock(message: ComposeStrings["recipe_add_to_grocery_list_no_ingredients"])
        } else {
            List {
                Section {
                    if state.groceryLists.count > 1 {
                        GroceryListPicker(state: state) { bloc.onGroceryListSelected(listId: $0) }
                    }
                    HStack {
                        Text(ComposeStrings["recipe_add_to_grocery_list_scale"])
                        Spacer()
                        ScaleMenu(scale: state.ingredientScale) { bloc.onScaleChanged(scale: $0) }
                    }
                }
                ForEach(state.groupedIngredients, id: \.category) { group in
                    Section {
                        ForEach(group.items, id: \.id) { item in
                            IngredientRow(item: item) { bloc.onIngredientToggled(ingredient: item.id) }
                        }
                    } header: {
                        LocalizedText(text: group.category.displayName())
                    }
                }
            }
            .listStyle(.insetGrouped)
        }
    }
}

/// The destination list as a menu row, sectioned like the grocery tab's own picker. Headers only
/// show once there's more than one section.
private struct GroceryListPicker: View {
    let state: AddRecipeToGroceryListBlocModel
    let onSelect: (Int64) -> Void

    var body: some View {
        let sections = state.groceryListSections
        Menu {
            ForEach(Array(sections.enumerated()), id: \.offset) { _, section in
                if sections.count > 1 {
                    Section(section.header.title) { buttons(section.lists) }
                } else {
                    buttons(section.lists)
                }
            }
        } label: {
            HStack {
                Text(ComposeStrings["recipe_add_to_grocery_list_select_list"])
                    .foregroundStyle(Color.primary)
                Spacer()
                Text(state.selectedGroceryList?.name ?? "")
                    .foregroundStyle(.secondary)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }

    @ViewBuilder
    private func buttons(_ lists: [AddRecipeToGroceryListBlocGroceryListItem]) -> some View {
        ForEach(lists, id: \.id) { list in
            Button { onSelect(list.id) } label: {
                if list.id == state.selectedGroceryList?.id {
                    Label(list.name, systemImage: "checkmark")
                } else {
                    Text(list.name)
                }
            }
        }
    }
}

private struct IngredientRow: View {
    let item: AddRecipeToGroceryListBlocListItem
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: ChefTheme.Spacing.normal - 4) {
                Image(systemName: item.isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title2)
                    .foregroundStyle(item.isSelected ? ChefTheme.primary : Color.secondary)
                    .contentTransition(.symbolEffect(.replace))
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.displayName).foregroundStyle(Color.primary)
                    if let quantity = item.quantity {
                        Text(quantity)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(item.isSelected ? .isSelected : [])
    }
}

private extension AddRecipeToGroceryListBlocGroceryListSection.Header {
    var title: String {
        switch self {
        case let family as AddRecipeToGroceryListBlocGroceryListSection.HeaderFamily:
            family.name ?? ComposeStrings["recipe_add_to_grocery_list_family_lists_fallback"]
        case is AddRecipeToGroceryListBlocGroceryListSection.HeaderSharedWithYou:
            ComposeStrings["recipe_add_to_grocery_list_shared_lists"]
        default:
            ComposeStrings["recipe_add_to_grocery_list_personal_lists"]
        }
    }
}

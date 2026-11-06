import SwiftUI
import ComposeApp

/// The redesigned grocery item sheet, mirroring the Android `GroceryDetailSheetContent`: name,
/// quantity with a stepper, aisle picker with "always file here", the recipe it came from, and
/// purchased. Save sits in the navigation bar.
struct GroceryDetailView: View {
    let bloc: GroceryDetailBloc
    @StateObject private var model: ObservedFlow<GroceryDetailBlocModel>

    init(bloc: GroceryDetailBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.models))
    }

    var body: some View {
        let loaded = model.value as? GroceryDetailBlocModel.Loaded
        Group {
            if let loaded {
                form(loaded)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle(ComposeStrings["grocery_detail"])
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(action: bloc.onBackClicked) {
                    Label(ComposeStrings["grocery_cancel"], systemImage: "xmark")
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button(ComposeStrings["save"], action: bloc.onSaveClicked)
                    .disabled(loaded == nil)
            }
        }
        .tint(ChefTheme.primary)
    }

    private func form(_ loaded: GroceryDetailBlocModel.Loaded) -> some View {
        let item = loaded.item
        let stepper = GroceryQuantityStepper.shared
        return Form {
            Section {
                TextField(
                    ComposeStrings["grocery_detail_name_label"],
                    text: Binding(
                        get: { (model.current as? GroceryDetailBlocModel.Loaded)?.item.displayName ?? "" },
                        set: { bloc.onGroceryNameChanged(name: $0) }
                    )
                )
                HStack {
                    TextField(
                        ComposeStrings["grocery_detail_quantity_label"],
                        text: Binding(
                            get: { (model.current as? GroceryDetailBlocModel.Loaded)?.item.quantity ?? "" },
                            set: { bloc.onGroceryQuantityChanged(quantity: $0) }
                        )
                    )
                    // Free-form text ("a pinch", "2 bunches"); the buttons only step the leading
                    // amount and grey out when there's nothing sensible to step.
                    Button(action: bloc.onQuantityDecrementClicked) {
                        Image(systemName: "minus.circle.fill").font(.title2)
                    }
                    .buttonStyle(.borderless)
                    .disabled(!stepper.canDecrement(quantity: item.quantity))
                    .accessibilityLabel(ComposeStrings["grocery_detail_quantity_decrease"])
                    Button(action: bloc.onQuantityIncrementClicked) {
                        Image(systemName: "plus.circle.fill").font(.title2)
                    }
                    .buttonStyle(.borderless)
                    .disabled(!stepper.canIncrement(quantity: item.quantity))
                    .accessibilityLabel(ComposeStrings["grocery_detail_quantity_increase"])
                }
            } footer: {
                if let recipe = item.recipeName {
                    Text(ComposeStrings["grocery_recipe_source", ["recipe": recipe]])
                }
            }

            Section {
                Picker(ComposeStrings["grocery_detail_aisle_label"], selection: Binding(
                    get: { (model.current as? GroceryDetailBlocModel.Loaded)?.item.category ?? item.category },
                    set: { bloc.onAisleChanged(category: $0) }
                )) {
                    ForEach(GroceryCategory.entries, id: \.self) { category in
                        LocalizedText(text: category.displayName()).tag(category)
                    }
                }
                if !item.displayName.trimmingCharacters(in: .whitespaces).isEmpty {
                    Toggle(isOn: Binding(
                        get: { (model.current as? GroceryDetailBlocModel.Loaded)?.alwaysFileHere ?? false },
                        set: { bloc.onAlwaysFileHereToggled(enabled: $0) }
                    )) {
                        Text(ComposeStrings["grocery_detail_always_file_here", [
                            "name": item.displayName,
                            "aisle": item.category.displayName().localized,
                        ]])
                        .font(.subheadline)
                    }
                }
            }

            Section {
                Toggle(ComposeStrings["purchased"], isOn: Binding(
                    get: { (model.current as? GroceryDetailBlocModel.Loaded)?.item.isChecked ?? false },
                    set: { bloc.onGroceryCheckedChanged(isChecked: $0) }
                ))
            }
        }
    }
}

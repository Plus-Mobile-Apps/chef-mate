import SwiftUI
import ComposeApp

/// The redesigned What's Cooking list (Android `WhatsCookingScreen`), shown as a sheet over Cook
/// Mode: every recipe being cooked, tap one to make it active. Select mode picks recipes to remove
/// from cook mode.
struct WhatsCookingSheet: View {
    let bloc: WhatsCookingBloc
    let onDismiss: () -> Void
    @StateObject private var model: ObservedFlow<WhatsCookingBlocModel>

    init(bloc: WhatsCookingBloc, onDismiss: @escaping () -> Void) {
        self.bloc = bloc
        self.onDismiss = onDismiss
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        NavigationStack {
            Group {
                if state.recipes.isEmpty {
                    Text(ComposeStrings["whats_cooking_empty"])
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(ChefTheme.Spacing.extraLarge)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(state.recipes, id: \.recipeId) { item in
                        let isSelected = state.selectedRecipeIds.contains(KotlinLong(value: item.recipeId))
                        Button {
                            if model.current.isSelectMode {
                                bloc.onSelectionToggled(recipeId: item.recipeId)
                            } else {
                                bloc.onRecipeClicked(recipeId: item.recipeId)
                                onDismiss()
                            }
                        } label: {
                            WhatsCookingRow(item: item, isSelectMode: state.isSelectMode, isSelected: isSelected)
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(isSelected ? .isSelected : [])
                    }
                    .listStyle(.plain)
                    .animation(.snappy, value: state.isSelectMode)
                }
            }
            .navigationTitle(ComposeStrings["whats_cooking_title"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        bloc.onCloseClicked()
                        onDismiss()
                    } label: {
                        Label(ComposeStrings["cook_mode_close"], systemImage: "xmark")
                    }
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    if state.isSelectMode && state.hasSelection {
                        Button(role: .destructive) { bloc.onDeleteSelectedClicked() } label: {
                            Label(ComposeStrings["whats_cooking_delete_selected"], systemImage: "trash")
                        }
                        .tint(.red)
                    }
                    Button { bloc.onSelectModeToggled() } label: {
                        Label(
                            ComposeStrings[state.isSelectMode ? "whats_cooking_cancel_select" : "whats_cooking_select"],
                            systemImage: state.isSelectMode ? "checklist.checked" : "checklist"
                        )
                    }
                }
            }
        }
        .tint(ChefTheme.primary)
    }
}

private struct WhatsCookingRow: View {
    let item: WhatsCookingBlocModel.Item
    let isSelectMode: Bool
    let isSelected: Bool

    var body: some View {
        HStack(spacing: 12) {
            if isSelectMode {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(isSelected ? ChefTheme.primary : Color.secondary)
                    .transition(.move(edge: .leading).combined(with: .opacity))
            }
            AsyncImage(url: item.imageUrl.flatMap(URL.init(string:))) { image in
                image.resizable().scaledToFill()
            } placeholder: {
                ZStack {
                    Color(.tertiarySystemFill)
                    Image(systemName: "fork.knife")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            .frame(width: 40, height: 40)
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
            .accessibilityHidden(true)

            Text(item.title)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, ChefTheme.Spacing.extraSmall)
        .contentShape(Rectangle())
    }
}

import SwiftUI
import ComposeApp

/// The redesigned family list, mirroring the Android `FamilyListScreen`: the user's families, an
/// empty state that creates the first one, and the "New family" sheet.
struct FamilyListView: View {
    let bloc: FamilyListBloc
    @StateObject private var model: ObservedFlow<FamilyListBlocModel>

    init(bloc: FamilyListBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        content(state)
            .navigationTitle(ComposeStrings["family_list_title"])
            .toolbar {
                if !state.families.isEmpty {
                    ToolbarItem(placement: .topBarTrailing) {
                        Button(action: bloc.onCreateFamilyClicked) {
                            Image(systemName: "plus")
                        }
                        .accessibilityLabel(ComposeStrings["family_list_create_a11y"])
                    }
                }
            }
            .tint(ChefTheme.primary)
            .sheet(isPresented: Binding(
                get: { model.current.createDialog != nil },
                // Confirming closes the dialog in the BLoC first; only a user dismissal is left.
                set: { if !$0 && model.current.createDialog != nil { bloc.onCreateDismissed() } }
            )) {
                if let dialog = model.value.createDialog {
                    FamilyNameSheet(
                        title: ComposeStrings["family_create_title"],
                        confirmText: ComposeStrings["family_create_confirm"],
                        label: ComposeStrings["family_name_label"],
                        name: dialog.name,
                        canConfirm: dialog.canConfirm,
                        isSaving: dialog.isSaving,
                        error: dialog.error,
                        onNameChange: { bloc.onNewFamilyNameChanged(name: $0) },
                        onConfirm: bloc.onCreateConfirmed,
                        onDismiss: bloc.onCreateDismissed
                    )
                }
            }
    }

    @ViewBuilder
    private func content(_ state: FamilyListBlocModel) -> some View {
        if state.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = state.loadError {
            FamilyLoadError(message: error, onRetry: bloc.onRetryClicked)
        } else if state.isEmpty {
            ContentUnavailableView {
                Label(ComposeStrings["family_list_empty_title"], systemImage: "person.2")
            } description: {
                Text(ComposeStrings["family_list_empty_message"])
            } actions: {
                Button(ComposeStrings["family_list_create_first"], action: bloc.onCreateFamilyClicked)
                    .buttonStyle(.borderedProminent)
                    .tint(ChefTheme.primary)
            }
        } else {
            List {
                Section(ComposeStrings["family_list_section"]) {
                    ForEach(state.families, id: \.id) { family in
                        FamilyRow(family: family) { bloc.onFamilyClicked(familyId: family.id) }
                    }
                }
            }
            .listStyle(.insetGrouped)
        }
    }
}

private struct FamilyRow: View {
    let family: Family
    let onTap: () -> Void

    var body: some View {
        let subtitle = familyPhrase(
            "family_list_member_count",
            ["count": String(family.memberCount), "role": family.myRole.labelText]
        )
        Button(action: onTap) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(family.name)
                        .font(.headline)
                        .foregroundStyle(Color.primary)
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
            .frame(minHeight: ChefTheme.Spacing.rowHeight - 16)
            .contentShape(Rectangle())
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(family.name), \(subtitle)")
    }
}

#Preview("Families") {
    NavigationStack { FamilyListView(bloc: FamilyPreviewsKt.previewFamilyListBloc) }
}

#Preview("Empty") {
    NavigationStack { FamilyListView(bloc: FamilyPreviewsKt.previewFamilyListEmptyBloc) }
}

#Preview("Error") {
    NavigationStack { FamilyListView(bloc: FamilyPreviewsKt.previewFamilyListErrorBloc) }
}

#Preview("Create") {
    NavigationStack { FamilyListView(bloc: FamilyPreviewsKt.previewFamilyListCreateDialogBloc) }
}

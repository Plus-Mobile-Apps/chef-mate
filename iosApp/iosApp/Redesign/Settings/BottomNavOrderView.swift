import SwiftUI
import ComposeApp

/// The redesigned tab-order editor (Settings → Bottom navigation order), mirroring the Android
/// `BottomNavOrderScreen`: every tab with its icon in the edited order, reordered with the system
/// drag handles (the list stays in edit mode), and a Save button that appears once the order
/// differs from the saved one.
struct BottomNavOrderView: View {
    let bloc: BottomNavOrderBloc
    @StateObject private var model: ObservedFlow<BottomNavOrderBlocModel>

    init(bloc: BottomNavOrderBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        List {
            ForEach(state.editedOrder, id: \.self) { tab in
                Label {
                    Text(tab.title)
                } icon: {
                    Image(systemName: tab.systemImage).foregroundStyle(ChefTheme.primary)
                }
                .frame(minHeight: ChefTheme.Spacing.rowHeight - 12)
                .accessibilityHint(ComposeStrings["bottom_nav_order_drag_handle_content_description"])
            }
            .onMove(perform: move)
        }
        .listStyle(.insetGrouped)
        // Always editing, so the drag handles show like the Android screen's.
        .environment(\.editMode, .constant(.active))
        .navigationTitle(ComposeStrings["bottom_nav_order_title"])
        .toolbar {
            if state.hasUnsavedChanges {
                ToolbarItem(placement: .confirmationAction) {
                    Button(ComposeStrings["bottom_nav_order_save"], action: bloc.onSave)
                }
            }
        }
        .animation(.default, value: state.hasUnsavedChanges)
        .tint(ChefTheme.primary)
    }

    /// SwiftUI's destination is the insertion index before removal; the BLoC's `onMove` takes the
    /// final index (remove at `from`, insert at `to`).
    private func move(from source: IndexSet, to destination: Int) {
        guard let from = source.first else { return }
        let to = destination > from ? destination - 1 : destination
        guard to != from else { return }
        bloc.onMove(from: Int32(from), to: Int32(to))
    }
}

import SwiftUI
import ComposeApp

/// Where the user types a search or URL, mirroring the Android `BrowserEditQueryScreen`: a focused
/// field (its text preselected so typing replaces it) over the browsing history.
struct BrowserEditQueryView: View {
    let bloc: BrowserEditQueryBloc
    let addressBar: Namespace.ID
    @StateObject private var model: ObservedFlow<BrowserEditQueryBlocModel>
    /// The field's text lives here and is pushed to the BLoC, like the Compose screen's local
    /// `TextFieldValue`, so typing never waits on a round trip.
    @State private var text: String
    @State private var selection: TextSelection?
    @FocusState private var isFocused: Bool

    init(bloc: BrowserEditQueryBloc, addressBar: Namespace.ID) {
        self.bloc = bloc
        self.addressBar = addressBar
        let flow = ObservedFlow<BrowserEditQueryBlocModel>(bloc.state)
        _model = StateObject(wrappedValue: flow)
        let initial = flow.current.searchText
        _text = State(initialValue: initial)
        _selection = State(initialValue: initial.isEmpty ? nil : TextSelection(range: initial.startIndex..<initial.endIndex))
    }

    var body: some View {
        let state = model.value
        VStack(spacing: ChefTheme.Spacing.small) {
            HStack(spacing: ChefTheme.Spacing.small) {
                Button {
                    isFocused = false
                    bloc.onCancel()
                } label: {
                    Image(systemName: "chevron.backward")
                        .frame(width: 28, height: 44)
                }
                .accessibilityLabel(ComposeStrings["browser_back"])

                TextField(state.searchEngine.searchHint, text: $text, selection: $selection)
                    .focused($isFocused)
                    .keyboardType(.webSearch)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.search)
                    .onSubmit {
                        isFocused = false
                        bloc.onNavigate()
                    }
                    .onChange(of: text) { _, newValue in bloc.onSearchTextChanged(text: newValue) }

                if !text.isEmpty {
                    Button {
                        selection = nil
                        text = ""
                        isFocused = true
                    } label: {
                        Image(systemName: "xmark.circle.fill").foregroundStyle(.secondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(ComposeStrings["browser_clear"])
                }
            }
            .browserField()
            .matchedGeometryEffect(id: browserAddressBarId, in: addressBar)
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .padding(.top, ChefTheme.Spacing.normal)

            List {
                ForEach(state.history, id: \.id) { entry in
                    HistoryRow(
                        entry: entry,
                        onOpen: {
                            isFocused = false
                            bloc.onHistoryItemClicked(entry: entry)
                        },
                        onDelete: { bloc.onHistoryItemDeleteClicked(entry: entry) }
                    )
                }
            }
            .listStyle(.plain)
            .scrollDismissesKeyboard(.interactively)
        }
        .onAppear { isFocused = true }
    }
}

private struct HistoryRow: View {
    let entry: BrowserHistoryEntry
    let onOpen: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: ChefTheme.Spacing.normal - 4) {
            Button(action: onOpen) {
                HStack(spacing: ChefTheme.Spacing.normal - 4) {
                    Image(systemName: "clock.arrow.circlepath")
                        .foregroundStyle(.secondary)
                    Text(entry.url)
                        .font(.subheadline)
                        .lineLimit(1)
                        .truncationMode(.middle)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            Button(action: onDelete) {
                Image(systemName: "xmark")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.secondary)
                    .frame(width: 32, height: 32)
            }
            .buttonStyle(.borderless)
            .accessibilityLabel(ComposeStrings["browser_history_delete"])
        }
        .swipeActions(edge: .trailing) {
            Button(role: .destructive, action: onDelete) {
                Label(ComposeStrings["browser_history_delete"], systemImage: "trash")
            }
        }
    }
}

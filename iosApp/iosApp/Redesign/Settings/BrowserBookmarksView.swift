import SwiftUI
import ComposeApp

/// The redesigned bookmark manager (Settings → Browser bookmarks), mirroring the Android
/// `BrowserBookmarksScreen`: each bookmark with its monogram, title and URL. Selection mode (from
/// the toolbar, a tap, or the context menu) selects all / bulk-deletes from the navigation bar,
/// with a confirmation first.
struct BrowserBookmarksView: View {
    let bloc: BrowserBookmarksBloc
    @StateObject private var model: ObservedFlow<BrowserBookmarksBlocModel>

    init(bloc: BrowserBookmarksBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        List {
            ForEach(state.bookmarks, id: \.id) { bookmark in
                row(bookmark, state: state)
            }
        }
        .listStyle(.insetGrouped)
        .overlay {
            if state.isLoading && state.bookmarks.isEmpty {
                ProgressView()
            } else if state.bookmarks.isEmpty {
                StatusBlock(systemImage: "bookmark", message: ComposeStrings["browser_bookmarks_empty"])
            }
        }
        .navigationTitle(title(state))
        .navigationBarTitleDisplayMode(state.selectionMode ? .inline : .large)
        .navigationBarBackButtonHidden(state.selectionMode)
        .toolbar { toolbar(state) }
        .tint(ChefTheme.primary)
        .alert(
            ComposeStrings["browser_bookmarks_delete_title"],
            isPresented: Binding(
                get: { model.value.pendingDeleteCount != nil },
                set: { shown in
                    if !shown && model.current.pendingDeleteCount != nil { bloc.onDeleteSelectedDismissed() }
                }
            )
        ) {
            Button(ComposeStrings["browser_bookmarks_delete"], role: .destructive, action: bloc.onDeleteSelectedConfirmed)
            Button(ComposeStrings["browser_bookmarks_cancel"], role: .cancel, action: bloc.onDeleteSelectedDismissed)
        } message: {
            if let count = state.pendingDeleteCount?.intValue {
                Text(count == 1
                    ? ComposeStrings["browser_bookmarks_delete_message_one"]
                    : ComposeStrings["browser_bookmarks_delete_message_other", ["count": "\(count)"]])
            }
        }
    }

    @ViewBuilder
    private func row(_ bookmark: BrowserBookmark, state: BrowserBookmarksBlocModel) -> some View {
        let selected = state.selectedIds.contains(KotlinLong(value: bookmark.id))
        Button { bloc.onBookmarkClicked(bookmark: bookmark) } label: {
            HStack(spacing: ChefTheme.Spacing.normal) {
                if state.selectionMode {
                    Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                        .font(.title2)
                        .foregroundStyle(selected ? ChefTheme.primary : Color.secondary)
                        .frame(width: 40, height: 40)
                } else {
                    BookmarkMonogram(host: bookmark.url.displayHost)
                }
                // Explicit colors: inside a List, a Button would otherwise tint the whole label.
                VStack(alignment: .leading, spacing: 2) {
                    Text(bookmark.title)
                        .foregroundStyle(Color.primary)
                        .lineLimit(1)
                    Text(bookmark.url)
                        .font(.footnote)
                        .foregroundStyle(Color.secondary)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
            }
            .contentShape(Rectangle())
        }
        .contextMenu {
            if !state.selectionMode {
                // The Android long press: enter selection mode with this bookmark selected.
                Button { bloc.onBookmarkLongClicked(bookmark: bookmark) } label: {
                    Label(ComposeStrings["browser_bookmarks_select_mode_a11y"], systemImage: "checkmark.circle")
                }
            }
        }
        .accessibilityAddTraits(state.selectionMode && selected ? .isSelected : [])
    }

    @ToolbarContentBuilder
    private func toolbar(_ state: BrowserBookmarksBlocModel) -> some ToolbarContent {
        if state.selectionMode {
            ToolbarItem(placement: .cancellationAction) {
                Button(action: bloc.onCancelSelection) {
                    Label(ComposeStrings["browser_bookmarks_cancel"], systemImage: "xmark")
                }
            }
            ToolbarItem(placement: .primaryAction) {
                Button(action: bloc.onSelectAllClicked) {
                    if state.allSelected {
                        Label(ComposeStrings["browser_bookmarks_deselect_all_a11y"], systemImage: "circle.dashed")
                    } else {
                        Label(ComposeStrings["browser_bookmarks_select_all_a11y"], systemImage: "checkmark.circle")
                    }
                }
            }
            // Hidden until something is selected, like the Android trash.
            if state.selectedCount > 0 {
                ToolbarItem(placement: .primaryAction) {
                    Button(role: .destructive, action: bloc.onDeleteSelectedClicked) {
                        Label(ComposeStrings["browser_bookmarks_delete_selected_a11y"], systemImage: "trash")
                    }
                }
            }
        } else if !state.bookmarks.isEmpty {
            // Nothing to select in an empty list, so don't offer the mode.
            ToolbarItem(placement: .primaryAction) {
                Button(action: bloc.onSelectModeClicked) {
                    Label(ComposeStrings["browser_bookmarks_select_mode_a11y"], systemImage: "checklist")
                }
            }
        }
    }

    private func title(_ state: BrowserBookmarksBlocModel) -> String {
        guard state.selectionMode else { return ComposeStrings["browser_bookmarks_title"] }
        return state.selectedCount == 0
            ? ComposeStrings["browser_bookmarks_selection_empty_title"]
            : ComposeStrings["browser_bookmarks_selection_count", ["count": "\(state.selectedCount)"]]
    }
}

#Preview("Bookmarks") {
    NavigationStack { BrowserBookmarksView(bloc: BrowserPreviewsKt.previewBrowserBookmarksBloc) }
}

#Preview("Selecting") {
    NavigationStack { BrowserBookmarksView(bloc: BrowserPreviewsKt.previewBrowserBookmarksSelectingBloc) }
}

#Preview("Empty") {
    NavigationStack { BrowserBookmarksView(bloc: BrowserPreviewsKt.previewBrowserBookmarksEmptyBloc) }
}

import SwiftUI
import ComposeApp

/// The browser's home, mirroring the Android `BrowserLandingScreen`: the app icon, a search-engine
/// picker, a search field that opens the edit-query screen, a button to the engine's own front
/// page, and the user's bookmarks as a grid.
struct BrowserLandingView: View {
    let bloc: BrowserLandingBloc
    let addressBar: Namespace.ID
    @StateObject private var model: ObservedFlow<BrowserLandingBlocModel>

    init(bloc: BrowserLandingBloc, addressBar: Namespace.ID) {
        self.bloc = bloc
        self.addressBar = addressBar
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        let engine = state.selectedEngine
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 0) {
                    Image("BrowserAppIcon")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 144, height: 144)
                        .clipShape(RoundedRectangle(cornerRadius: 32, style: .continuous))
                        .accessibilityHidden(true)
                        .padding(.top, ChefTheme.Spacing.normal)
                    Text("Chef Mate")
                        .font(.largeTitle.bold())
                        .padding(.top, ChefTheme.Spacing.small)

                    EngineMenu(selected: engine, onSelect: { bloc.onSearchEngineSelected(engine: $0) })
                        .padding(.top, ChefTheme.Spacing.large)

                    searchField(engine: engine, showsCoachMark: state.activeCoachMark == CoachMarkId.shared.BROWSER_SEARCH)
                        .padding(.top, ChefTheme.Spacing.normal)

                    Button(action: bloc.onOpenEngineHomeClicked) {
                        Text(ComposeStrings["browser_landing_open_engine"].replacingOccurrences(of: "{engine}", with: engine.displayName))
                            .font(.headline)
                            .padding(.horizontal, ChefTheme.Spacing.small)
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .controlSize(.large)
                    .padding(.top, ChefTheme.Spacing.normal)

                    Text(ComposeStrings["browser_landing_tagline"])
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.top, ChefTheme.Spacing.normal)

                    if !state.bookmarks.isEmpty {
                        BookmarksGrid(
                            bookmarks: state.bookmarks,
                            onOpen: { bloc.onBookmarkClicked(bookmark: $0) },
                            onDelete: { bloc.onDeleteBookmarkClicked(bookmark: $0) }
                        )
                        .padding(.top, ChefTheme.Spacing.extraLarge)
                        .padding(.bottom, ChefTheme.Spacing.large)
                    }
                }
                .padding(.horizontal, ChefTheme.Spacing.extraLarge)
                .frame(maxWidth: 600)
                // Centred while shorter than the screen; scrolls once the bookmarks outgrow it.
                .frame(maxWidth: .infinity, minHeight: geometry.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
    }

    private func searchField(engine: SearchEngine, showsCoachMark: Bool) -> some View {
        Button(action: bloc.onSearchFieldFocused) {
            HStack(spacing: ChefTheme.Spacing.small) {
                Image(systemName: "magnifyingglass")
                Text(engine.searchHint)
                    .lineLimit(1)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .foregroundStyle(.secondary)
            .browserField()
            .matchedGeometryEffect(id: browserAddressBarId, in: addressBar)
        }
        .buttonStyle(.plain)
        .popover(
            isPresented: Binding(
                get: { showsCoachMark },
                set: { if !$0 { bloc.onCoachMarkDismissed(id: CoachMarkId.shared.BROWSER_SEARCH) } }
            ),
            attachmentAnchor: .rect(.bounds),
            arrowEdge: .top
        ) {
            Text(ComposeStrings["browser_search_onboarding"])
                .font(.subheadline)
                .padding()
                .frame(maxWidth: 280)
                .presentationCompactAdaptation(.popover)
        }
    }
}

/// The engine chip with a dropdown of every engine, like the Android `SearchEngineDropdown`.
private struct EngineMenu: View {
    let selected: SearchEngine
    let onSelect: (SearchEngine) -> Void

    var body: some View {
        Menu {
            ForEach(SearchEngine.entries, id: \.self) { engine in
                Button { onSelect(engine) } label: {
                    Label { Text(engine.displayName) } icon: { Image(engine.logoImageName) }
                }
            }
        } label: {
            HStack(spacing: ChefTheme.Spacing.small) {
                Image(selected.logoImageName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 18, height: 18)
                Text(selected.displayName)
                    .font(.subheadline.weight(.medium))
                Image(systemName: "chevron.down")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
            }
            .foregroundStyle(.primary)
            .padding(.horizontal, ChefTheme.Spacing.normal - 4)
            .padding(.vertical, ChefTheme.Spacing.small)
            .overlay(Capsule().strokeBorder(Color(.separator)))
            .contentShape(Capsule())
        }
        .accessibilityLabel(ComposeStrings["browser_landing_engine_dropdown"])
        .accessibilityValue(selected.displayName)
    }
}

/// The user's bookmarks as tiles, a column dropping once a tile would get narrower than 136pt.
private struct BookmarksGrid: View {
    let bookmarks: [BrowserBookmark]
    let onOpen: (BrowserBookmark) -> Void
    let onDelete: (BrowserBookmark) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
            Text(ComposeStrings["browser_bookmarks_title"])
                .font(.headline)
                .padding(.bottom, ChefTheme.Spacing.extraSmall)
            // A plain grid: it sits inside the landing screen's scroll view.
            LazyVGrid(
                columns: [GridItem(.adaptive(minimum: 136), spacing: ChefTheme.Spacing.small)],
                spacing: ChefTheme.Spacing.small
            ) {
                ForEach(bookmarks, id: \.id) { bookmark in
                    BookmarkTile(bookmark: bookmark, onOpen: { onOpen(bookmark) }, onDelete: { onDelete(bookmark) })
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct BookmarkTile: View {
    let bookmark: BrowserBookmark
    let onOpen: () -> Void
    let onDelete: () -> Void

    var body: some View {
        let host = bookmark.url.displayHost
        Button(action: onOpen) {
            VStack(spacing: 0) {
                BookmarkMonogram(host: host)
                Text(bookmark.title)
                    .font(.subheadline.weight(.medium))
                    .multilineTextAlignment(.center)
                    .lineLimit(2, reservesSpace: true)
                    .padding(.top, ChefTheme.Spacing.small)
                Text(host)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            .padding(.horizontal, ChefTheme.Spacing.small)
            .padding(.vertical, ChefTheme.Spacing.normal)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .contentShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
        .overlay(alignment: .topTrailing) {
            Menu {
                deleteButton
            } label: {
                Image(systemName: "ellipsis")
                    .foregroundStyle(.secondary)
                    .frame(width: 36, height: 36)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(ComposeStrings["browser_bookmark_options"].replacingOccurrences(of: "{title}", with: bookmark.title))
        }
        .contextMenu { deleteButton }
    }

    private var deleteButton: some View {
        Button(role: .destructive, action: onDelete) {
            Label(ComposeStrings["browser_bookmark_delete"], systemImage: "trash")
        }
    }
}

/// A letter avatar standing in for the site's favicon, which would mean a fetch per tile.
struct BookmarkMonogram: View {
    let host: String

    var body: some View {
        Text(host.first.map { String($0).uppercased() } ?? "")
            .font(.headline)
            .foregroundStyle(ChefTheme.primary)
            .frame(width: 40, height: 40)
            .background(ChefTheme.primary.opacity(0.15), in: Circle())
            .accessibilityHidden(true)
    }
}

#Preview("Landing") {
    @Previewable @Namespace var namespace
    BrowserLandingView(bloc: BrowserPreviewsKt.previewBrowserLandingBloc, addressBar: namespace)
}

#Preview("Landing with bookmarks") {
    @Previewable @Namespace var namespace
    BrowserLandingView(bloc: BrowserPreviewsKt.previewBrowserLandingWithBookmarksBloc, addressBar: namespace)
}

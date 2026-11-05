import SwiftUI
import ComposeApp

/// The page itself, mirroring the Android `BrowserScreen`: an address bar (back, home, the URL,
/// and an overflow menu with forward / bookmark / open in browser), the native web view, and the
/// Download button that extracts the recipe on the page.
struct BrowserView: View {
    let bloc: BrowserBloc
    let addressBar: Namespace.ID
    var isModal = false
    @StateObject private var model: ObservedFlow<BrowserBlocModel>
    @SwiftUI.Environment(\.openURL) private var openURL

    init(bloc: BrowserBloc, addressBar: Namespace.ID, isModal: Bool = false) {
        self.bloc = bloc
        self.addressBar = addressBar
        self.isModal = isModal
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        VStack(spacing: 0) {
            if state.showControls {
                BrowserAddressBar(state: state, bloc: bloc, addressBar: addressBar, openCurrentPage: openCurrentPage)
                Divider()
            }
            // In a pushed modal the edge swipe belongs to the navigation stack's back gesture.
            BrowserWebView(bloc: bloc, model: state, allowsHistoryGestures: !isModal)
            if state.showControls && !state.currentUrl.isEmpty {
                Divider()
                HStack {
                    Spacer()
                    downloadButton(isExtracting: state.isExtracting)
                }
                .padding(.horizontal, ChefTheme.Spacing.normal)
                .padding(.vertical, ChefTheme.Spacing.small)
            }
        }
        .alert(
            state.extractionFailureMessage?.localized ?? "",
            isPresented: Binding(
                get: { model.current.extractionFailureMessage != nil },
                set: { if !$0 { bloc.onDismissMessage() } }
            )
        ) {
            Button("OK") { bloc.onDismissMessage() }
        } message: {
            Text(ComposeStrings["browser_extraction_failed_body"])
        }
        .modifier(ModalChrome(isModal: isModal, currentUrl: state.currentUrl, openCurrentPage: openCurrentPage))
    }

    /// Deliberately gated only on `isExtracting`, not on the page still loading: a load that never
    /// reports finishing would otherwise leave the button disabled with no way to retry (see the
    /// Compose `BrowserBottomBar`).
    private func downloadButton(isExtracting: Bool) -> some View {
        Button(action: bloc.onExtractRecipe) {
            ZStack {
                Text(ComposeStrings["browser_download"]).opacity(isExtracting ? 0 : 1)
                if isExtracting { ProgressView().tint(.white) }
            }
            .font(.headline)
            .padding(.horizontal, ChefTheme.Spacing.small)
        }
        .buttonStyle(.borderedProminent)
        .buttonBorderShape(.capsule)
        .controlSize(.large)
        .disabled(isExtracting)
    }

    /// Hands the page that's actually loaded (not the possibly half-typed address text) to the
    /// system, which opens it in the user's default browser.
    private func openCurrentPage() {
        if let url = URL(string: model.current.currentUrl) { openURL(url) }
    }
}

/// Back and home flank the address field and the rarer page actions sit behind one overflow menu,
/// like Chrome (and the Compose `BrowserAddressBar`). Home is the only way back to the landing
/// screen, since the tab has no back button.
private struct BrowserAddressBar: View {
    let state: BrowserBlocModel
    let bloc: BrowserBloc
    let addressBar: Namespace.ID
    let openCurrentPage: () -> Void

    var body: some View {
        let hasPage = !state.currentUrl.isEmpty
        HStack(spacing: ChefTheme.Spacing.extraSmall) {
            Button(action: bloc.onGoBack) {
                Image(systemName: "chevron.backward").frame(width: 36, height: 44)
            }
            .disabled(!state.canGoBack)
            .accessibilityLabel(ComposeStrings["browser_back"])

            Button(action: bloc.onHomeClicked) {
                Image(systemName: "house").frame(width: 36, height: 44)
            }
            .accessibilityLabel(ComposeStrings["browser_home"])

            // Editing happens on the EditQuery screen, so the field is a button that opens it.
            Button(action: bloc.onAddressBarFocused) {
                HStack(spacing: ChefTheme.Spacing.small) {
                    Text(state.addressBarText.isEmpty ? ComposeStrings["browser_address_hint"] : state.addressBarText)
                        .foregroundStyle(state.addressBarText.isEmpty ? .secondary : .primary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if !state.addressBarText.isEmpty {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.secondary)
                            .onTapGesture {
                                bloc.onUrlChanged(url: "")
                                bloc.onAddressBarFocused()
                            }
                            .accessibilityLabel(ComposeStrings["browser_clear"])
                            .accessibilityAddTraits(.isButton)
                    }
                }
                .font(.subheadline)
                .browserField()
                .matchedGeometryEffect(id: browserAddressBarId, in: addressBar)
            }
            .buttonStyle(.plain)

            Menu {
                Button(action: bloc.onGoForward) {
                    Label(ComposeStrings["browser_forward"], systemImage: "chevron.forward")
                }
                .disabled(!state.canGoForward)
                Button(action: bloc.onToggleBookmark) {
                    Label(
                        ComposeStrings[state.isBookmarked ? "browser_remove_bookmark" : "browser_add_bookmark"],
                        systemImage: state.isBookmarked ? "star.fill" : "star"
                    )
                }
                .disabled(!hasPage)
                Button(action: openCurrentPage) {
                    Label(ComposeStrings["browser_open_in_default_browser"], systemImage: "safari")
                }
                .disabled(!hasPage)
            } label: {
                Image(systemName: "ellipsis").frame(width: 36, height: 44)
            }
            .accessibilityLabel(ComposeStrings["browser_more_options"])
        }
        .padding(.horizontal, ChefTheme.Spacing.small)
        .padding(.vertical, ChefTheme.Spacing.extraSmall)
    }
}

/// A pushed browser (a link opened from elsewhere, without the address bar) keeps the navigation
/// bar for its back button, titled with the site, and offers "Open in browser" like the Compose
/// modal header. The Browser tab hides the bar.
private struct ModalChrome: ViewModifier {
    let isModal: Bool
    let currentUrl: String
    let openCurrentPage: () -> Void

    func body(content: Content) -> some View {
        if isModal {
            content
                .navigationTitle(currentUrl.displayHost)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    if !currentUrl.isEmpty {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(action: openCurrentPage) {
                                Image(systemName: "safari")
                            }
                            .accessibilityLabel(ComposeStrings["browser_open_in_default_browser"])
                        }
                    }
                }
        } else {
            content.toolbar(.hidden, for: .navigationBar)
        }
    }
}

extension String {
    /// `https://www.seriouseats.com/banana-bread` → `seriouseats.com` (mirrors the Compose
    /// `displayHost`).
    var displayHost: String {
        var rest = self
        if let scheme = rest.range(of: "://") { rest = String(rest[scheme.upperBound...]) }
        rest = String(rest.prefix { $0 != "/" && $0 != "?" })
        if rest.hasPrefix("www.") { rest.removeFirst(4) }
        return rest.isEmpty ? self : rest
    }
}

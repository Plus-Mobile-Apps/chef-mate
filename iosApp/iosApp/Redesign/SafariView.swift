import SwiftUI
import SafariServices

/// A web page to show in a Safari sheet. Native screens open web links this way instead of pushing
/// the in-app Browser screen.
struct WebPage: Identifiable {
    let url: URL
    var id: URL { url }
}

extension View {
    /// Presents `item`'s page in an `SFSafariViewController` sheet while it is non-nil.
    func safariSheet(item: Binding<WebPage?>) -> some View {
        sheet(item: item) { page in
            SafariView(url: page.url)
                .ignoresSafeArea()
        }
    }
}

/// `SFSafariViewController` for SwiftUI: Safari's reader, sign-ins and share sheet, presented as a
/// sheet over the screen.
struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.preferredControlTintColor = UIColor(ChefTheme.primary)
        return controller
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}

import SwiftUI
import UIKit
import WebKit
import ComposeApp

/// The native `WKWebView` behind `BrowserView`, the SwiftUI counterpart of the Compose
/// `PlatformWebView`. Commands flow in from `BrowserBloc.Model` (the URL to load and the
/// back / forward / capture-HTML triggers); everything the page does flows back out through the
/// BLoC's callbacks.
///
/// The web view itself lives in the BLoC's `InstanceKeeper` (see `BrowserWebViewHolder`), not in
/// the SwiftUI view, so switching tabs and coming back keeps the page and its history, and it is
/// torn down exactly when the BLoC is.
struct BrowserWebView: UIViewRepresentable {
    let bloc: BrowserBloc
    let model: BrowserBlocModel
    /// Lets the user swipe from the edge to go back and forward through the page history. Off
    /// where that would fight the navigation stack's own swipe-back.
    var allowsHistoryGestures = true

    func makeUIView(context: Context) -> WKWebView {
        let holder = BrowserWebViewHolder.of(bloc)
        holder.webView.allowsBackForwardNavigationGestures = allowsHistoryGestures
        return holder.webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {
        BrowserWebViewHolder.of(bloc).apply(model)
    }
}

/// Owns one BLoC's `WKWebView` and the bookkeeping that turns the model's commands into one-shot
/// actions. Stored in the BLoC's `InstanceKeeper`, so it is destroyed with the BLoC.
final class BrowserWebViewHolder: NSObject, Instance_keeperInstanceKeeperInstance, WKNavigationDelegate {
    private static let key = "BrowserWebViewHolder"

    let webView: WKWebView
    private weak var bloc: BrowserBloc?
    private var observations: [NSKeyValueObservation] = []

    /// The last `navigateUrl` acted on, so each new value loads once.
    private var lastNavigateUrl = ""
    /// The URL the web view was last told to load or last reported, so a model echoing the page
    /// that's already showing doesn't reload it.
    private var lastCommandedUrl = ""
    /// Trigger counts already handled. Seeded from the model on creation so a rebuilt view never
    /// replays an old back / forward / capture.
    private var goBackTrigger: Int32
    private var goForwardTrigger: Int32
    private var captureHtmlTrigger: Int32

    /// The holder for `bloc`, created (with its web view) on first use.
    static func of(_ bloc: BrowserBloc) -> BrowserWebViewHolder {
        if let existing = bloc.instanceKeeper.get(key: key) as? BrowserWebViewHolder {
            return existing
        }
        let holder = BrowserWebViewHolder(bloc: bloc)
        bloc.instanceKeeper.put(key: key, instance: holder)
        return holder
    }

    private init(bloc: BrowserBloc) {
        self.bloc = bloc
        let model = bloc.state.value as! BrowserBlocModel
        goBackTrigger = model.goBackTrigger
        goForwardTrigger = model.goForwardTrigger
        captureHtmlTrigger = model.captureHtmlTrigger

        webView = WKWebView(frame: .zero, configuration: WKWebViewConfiguration())
        // Opaque so a page that doesn't paint its own background shows the web view's white
        // canvas, like Safari, instead of the app's (possibly dark) background.
        webView.isOpaque = true
        webView.customUserAgent = Self.safariUserAgent()
        super.init()
        webView.navigationDelegate = self
        observe()
    }

    func onDestroy() {
        observations.forEach { $0.invalidate() }
        observations = []
        webView.stopLoading()
        webView.navigationDelegate = nil
    }

    /// Acts on whatever in `model` changed since the last call.
    func apply(_ model: BrowserBlocModel) {
        let navigateUrl = model.navigateUrl
        if !navigateUrl.isEmpty, navigateUrl != lastNavigateUrl {
            lastNavigateUrl = navigateUrl
            if navigateUrl != lastCommandedUrl, let url = URL(string: navigateUrl) {
                lastCommandedUrl = navigateUrl
                webView.load(URLRequest(url: url))
            }
        }
        if model.goBackTrigger > goBackTrigger {
            goBackTrigger = model.goBackTrigger
            webView.goBack()
        }
        if model.goForwardTrigger > goForwardTrigger {
            goForwardTrigger = model.goForwardTrigger
            webView.goForward()
        }
        if model.captureHtmlTrigger > captureHtmlTrigger {
            captureHtmlTrigger = model.captureHtmlTrigger
            // The BLoC waits on exactly one answer per trigger, blank when the page can't be read.
            webView.evaluateJavaScript(PlatformWebViewKt.CAPTURE_HTML_SCRIPT) { [weak self] result, _ in
                self?.bloc?.onHtmlCaptured(html: result as? String)
            }
        }
    }

    private func observe() {
        observations = [
            webView.observe(\.url, options: [.new]) { [weak self] webView, _ in
                guard let self, let url = webView.url?.absoluteString, !url.isEmpty else { return }
                self.lastCommandedUrl = url
                self.bloc?.onUrlLoadedInWebView(url: url)
            },
            webView.observe(\.isLoading, options: [.initial, .new]) { [weak self] webView, _ in
                self?.bloc?.onWebViewLoadingChanged(isLoading: webView.isLoading)
            },
            webView.observe(\.title, options: [.new]) { [weak self] webView, _ in
                self?.bloc?.onPageTitleChanged(title: webView.title)
            },
            webView.observe(\.canGoBack, options: [.initial, .new]) { [weak self] _, _ in
                self?.reportCanNavigate()
            },
            webView.observe(\.canGoForward, options: [.new]) { [weak self] _, _ in
                self?.reportCanNavigate()
            },
        ]
    }

    private func reportCanNavigate() {
        bloc?.onCanNavigateChanged(canGoBack: webView.canGoBack, canGoForward: webView.canGoForward)
    }

    // MARK: WKNavigationDelegate

    /// Links the web view can't show itself (`tel:`, `mailto:`, App Store, …) go to the system.
    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        guard let url = navigationAction.request.url, let scheme = url.scheme?.lowercased() else {
            decisionHandler(.allow)
            return
        }
        if ["http", "https", "about", "data", "blob", "file"].contains(scheme) {
            decisionHandler(.allow)
        } else {
            UIApplication.shared.open(url)
            decisionHandler(.cancel)
        }
    }

    /// Mobile Safari's user agent for this device (mirrors `browserUserAgent()` in
    /// `PlatformWebView.ios.kt`). WKWebView's default omits Safari's `Version/…` and `Safari/…`
    /// tokens, which is how sites spot an embedded web view — and Google answers that with
    /// CAPTCHAs. iPads get the desktop-class form, as iPad Safari sends.
    private static func safariUserAgent() -> String {
        let device = UIDevice.current
        let osVersion = device.systemVersion
        let safariVersion = osVersion.split(separator: ".").prefix(2).joined(separator: ".")
        if device.userInterfaceIdiom == .pad {
            return "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 "
                + "(KHTML, like Gecko) Version/\(safariVersion) Safari/605.1.15"
        }
        return "Mozilla/5.0 (iPhone; CPU iPhone OS \(osVersion.replacingOccurrences(of: ".", with: "_")) like Mac OS X) "
            + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/\(safariVersion) "
            + "Mobile/15E148 Safari/604.1"
    }
}

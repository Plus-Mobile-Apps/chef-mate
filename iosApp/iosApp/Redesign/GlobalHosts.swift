import SwiftUI
import StoreKit
import ComposeApp

extension View {
    /// The app-wide hosts that the Compose `App` composable provides on the other UI path: the
    /// `ToastService` overlay and the in-app review prompt. Apply once, at the root of the shell.
    /// `bottomInset` lifts toasts over the tab bar while it is showing; screens with their own
    /// bottom toolbar add theirs with `toastClearance(_:)`. `showsToasts` is false while a
    /// full-screen cover hides this overlay (the cover shows them with `toastOverlay`).
    func globalHosts(
        toastService: ToastService,
        reviewService: InAppReviewService,
        bottomInset: CGFloat,
        showsToasts: Bool = true
    ) -> some View {
        toastOverlay(toastService: toastService, bottomInset: bottomInset, isEnabled: showsToasts)
            .background {
                ReviewHost(service: reviewService)
            }
    }

    /// The toast overlay alone, for content presented over the shell's root (a full-screen cover),
    /// where the root overlay can't be seen. Only one overlay should be enabled at a time, so two
    /// hosts never drain the same queue.
    func toastOverlay(toastService: ToastService, bottomInset: CGFloat = 0, isEnabled: Bool = true) -> some View {
        modifier(ToastOverlay(toastService: toastService, bottomInset: bottomInset, isEnabled: isEnabled))
    }
}

extension View {
    /// Keeps toasts above this screen's bottom-anchored chrome (e.g. a `.bottomBar` toolbar) while
    /// the screen is on screen — the native counterpart of Compose's `snackbarAnchor()`.
    func toastClearance(_ height: CGFloat) -> some View {
        modifier(ToastClearance(height: height))
    }
}

/// The bottom clearances registered by the screens currently showing. Toasts float above the
/// tallest one. Keyed per registering view, so overlapping appear/disappear during a push or pop
/// can't drop or double-count a clearance.
final class ToastClearances: ObservableObject {
    @Published private var heights: [UUID: CGFloat] = [:]

    var inset: CGFloat { heights.values.max() ?? 0 }

    func set(_ height: CGFloat?, for key: UUID) {
        heights[key] = height
    }
}

private struct ToastClearancesKey: EnvironmentKey {
    static let defaultValue: ToastClearances? = nil
}

extension EnvironmentValues {
    var toastClearances: ToastClearances? {
        get { self[ToastClearancesKey.self] }
        set { self[ToastClearancesKey.self] = newValue }
    }
}

private struct ToastClearance: ViewModifier {
    let height: CGFloat
    @SwiftUI.Environment(\.toastClearances) private var clearances
    @State private var key = UUID()

    func body(content: Content) -> some View {
        content
            .onAppear { clearances?.set(height, for: key) }
            .onDisappear { clearances?.set(nil, for: key) }
    }
}

private struct ToastOverlay: ViewModifier {
    let toastService: ToastService
    let bottomInset: CGFloat
    let isEnabled: Bool
    @StateObject private var clearances = ToastClearances()

    func body(content: Content) -> some View {
        content
            .environment(\.toastClearances, clearances)
            .overlay(alignment: .bottom) {
                if isEnabled {
                    ToastHost(service: toastService)
                        // The larger of the two, not the sum: a tab root with its own bottom chrome
                        // over the tab bar (the grocery add row) registers a clearance that already
                        // includes `BottomNavView.tabBarHeight`.
                        .padding(.bottom, max(bottomInset, clearances.inset))
                        .animation(.snappy, value: clearances.inset)
                }
            }
    }
}

/// Native replacement for the Compose `ToastScaffold`: drains `ToastService`'s queue one message at
/// a time. Each toast ends exactly once — by its timer, a swipe down, or its action button — and
/// `ToastBridge.finish` then runs the matching Kotlin callback and dequeues it.
private struct ToastHost: View {
    let service: ToastService
    @StateObject private var queue: ObservedFlow<AnyObject>
    /// Ids already finished, so a timer firing after a swipe (or vice versa) can't end one twice.
    @State private var finished = Set<Int64>()

    init(service: ToastService) {
        self.service = service
        _queue = StateObject(wrappedValue: ObservedFlow(service.queue))
    }

    var body: some View {
        let toast = ToastBridge.shared.head(queue: queue.value)
        ZStack {
            if let toast, !finished.contains(toast.id) {
                ToastCard(toast: toast) { finish(toast, actionTapped: $0) }
                    .id(toast.id)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .task(id: toast.id) {
                        guard let seconds = toast.durationSeconds?.doubleValue else { return }
                        try? await Task.sleep(for: .seconds(seconds))
                        if !Task.isCancelled { finish(toast, actionTapped: false) }
                    }
            }
        }
        .animation(.snappy, value: toast.map { $0.id })
        .padding(.horizontal, ChefTheme.Spacing.normal)
        .padding(.bottom, ChefTheme.Spacing.small)
    }

    private func finish(_ toast: NativeToast, actionTapped: Bool) {
        guard finished.insert(toast.id).inserted else { return }
        ToastBridge.shared.finish(service: service, toast: toast, actionTapped: actionTapped)
    }
}

private struct ToastCard: View {
    let toast: NativeToast
    let onFinish: (_ actionTapped: Bool) -> Void
    @State private var dragOffset: CGFloat = 0

    var body: some View {
        HStack(spacing: ChefTheme.Spacing.normal) {
            LocalizedText(text: toast.text)
                .font(.subheadline)
                .frame(maxWidth: .infinity, alignment: .leading)
            if let label = toast.actionLabel {
                Button(label.localized) { onFinish(true) }
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(ChefTheme.primary)
            }
        }
        .padding(.horizontal, ChefTheme.Spacing.normal + 4)
        .padding(.vertical, ChefTheme.Spacing.normal - 2)
        .chefGlass(in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .offset(y: max(0, dragOffset))
        .gesture(
            DragGesture()
                .onChanged { dragOffset = $0.translation.height }
                .onEnded { value in
                    if value.translation.height > 40 {
                        onFinish(false)
                    } else {
                        withAnimation(.snappy) { dragOffset = 0 }
                    }
                }
        )
        .accessibilityAddTraits(.isStaticText)
        .accessibilityAction(named: "Dismiss") { onFinish(false) }
    }
}

/// Native replacement for the Compose `InAppReviewHost`: asks StoreKit for the review sheet whenever
/// `InAppReviewService` requests one. The OS decides whether a sheet is actually shown.
private struct ReviewHost: View {
    let service: InAppReviewService
    @StateObject private var requested: ObservedFlow<KotlinBoolean>
    @SwiftUI.Environment(\.requestReview) private var requestReview

    init(service: InAppReviewService) {
        self.service = service
        _requested = StateObject(wrappedValue: ObservedFlow(service.isPromptRequested))
    }

    var body: some View {
        Color.clear
            .onAppear(perform: promptIfRequested)
            .onChange(of: requested.value.boolValue) { promptIfRequested() }
    }

    private func promptIfRequested() {
        guard requested.current.boolValue else { return }
        requestReview()
        service.onPromptFinished()
    }
}

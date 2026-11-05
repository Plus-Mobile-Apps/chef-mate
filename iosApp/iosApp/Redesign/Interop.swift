import SwiftUI
import ComposeApp

/// Republishes a BLoC's `StateFlow` to SwiftUI. Hold it in a `@StateObject` so the subscription
/// lives as long as the view.
final class ObservedFlow<T: AnyObject>: ObservableObject {
    @Published private(set) var value: T
    private let watcher: FlowWatcher<T>
    private var cancellable: Cancellable?

    /// The StateFlow's value right now, ahead of the next `value` publish. Read this in bindings
    /// whose setter calls the BLoC, so SwiftUI doesn't snap back to the stale value in between.
    var current: T { watcher.value }

    init(_ flow: Kotlinx_coroutines_coreStateFlow) {
        watcher = FlowWatcher<T>(flow: flow)
        value = watcher.value
        cancellable = watcher.watch { [weak self] newValue in self?.value = newValue }
    }

    deinit { cancellable?.cancel() }
}

/// `ObservedFlow` for a nullable `StateFlow` (Kotlin `NullableFlowWatcher`).
final class ObservedOptionalFlow<T: AnyObject>: ObservableObject {
    @Published private(set) var value: T?
    private let watcher: NullableFlowWatcher<T>
    private var cancellable: Cancellable?

    /// The StateFlow's value right now; see `ObservedFlow.current`.
    var current: T? { watcher.value }

    init(_ flow: Kotlinx_coroutines_coreStateFlow) {
        watcher = NullableFlowWatcher<T>(flow: flow)
        value = watcher.value
        cancellable = watcher.watch { [weak self] newValue in self?.value = newValue }
    }

    deinit { cancellable?.cancel() }
}

/// Republishes a Decompose `Value` (e.g. a `ChildStack`) to SwiftUI.
final class ObservedValue<T: AnyObject>: ObservableObject {
    @Published private(set) var value: T
    private var cancellation: Cancellation?

    init(_ source: Value<T>) {
        value = source.value
        cancellation = source.subscribe(observer_: { [weak self] newValue in self?.value = newValue })
    }

    deinit { cancellation?.cancel() }
}

/// Renders a Kotlin `TextData`, resolved synchronously from the `ComposeStrings` catalog.
struct LocalizedText: View {
    let text: TextData

    var body: some View {
        Text(text.localized)
    }
}

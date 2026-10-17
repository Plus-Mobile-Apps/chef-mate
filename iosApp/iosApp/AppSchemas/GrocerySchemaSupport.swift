import Foundation
import ComposeApp

/// Shared plumbing for the iOS 27 `reminders` App Schema adoption, which lets Apple Intelligence
/// Siri understand free-form requests like "add dragon fruit and oat milk to my Chef Mate grocery
/// list" without a predefined phrase. Chef Mate grocery lists map onto the schema's lists, and
/// grocery items onto its reminders.
///
/// Reads go through `WatchDataBridge.currentSnapshot`, the same JSON the watch companion consumes,
/// so this adds no new Kotlin surface for listing data.
enum GrocerySchemaSupport {
    struct Snapshot: Decodable {
        struct List: Decodable {
            let id: Int64
            let name: String
            let isShared: Bool
        }

        struct Item: Decodable {
            let id: Int64
            let listId: Int64
            let name: String
            let quantity: String?
            let isChecked: Bool
        }

        let lists: [List]
        let items: [Item]
    }

    enum Failure: Error, CustomLocalizedStringResourceConvertible {
        case notReady

        var localizedStringResource: LocalizedStringResource {
            "Open Chef Mate once, then try that again."
        }
    }

    /// Waits briefly for the DI graph to finish building on a cold background launch.
    static func bridge() async throws -> WatchDataBridge {
        for _ in 0 ..< 50 {  // up to ~5s
            if let bridge = RootBlocProvider.shared.watchDataBridgeOrNull {
                return bridge
            }
            try? await Task.sleep(nanoseconds: 100_000_000)  // 0.1s
        }
        guard let bridge = RootBlocProvider.shared.watchDataBridgeOrNull else {
            throw Failure.notReady
        }
        return bridge
    }

    static func snapshot() async throws -> Snapshot {
        let bridge = try await bridge()
        let json: String = await withCheckedContinuation { continuation in
            bridge.currentSnapshot { continuation.resume(returning: $0) }
        }
        return try JSONDecoder().decode(Snapshot.self, from: Data(json.utf8))
    }

    /// Adds [name] to [listId] (or the default list when nil), returning once it is persisted so a
    /// background-launched process isn't torn down mid-write.
    @discardableResult
    static func addItem(name: String, listId: Int64?) async throws -> Int64 {
        let bridge = try await bridge()
        let target: Int64
        if let listId {
            target = listId
        } else {
            target = await withCheckedContinuation { continuation in
                bridge.ensureDefaultList { continuation.resume(returning: $0.int64Value) }
            }
        }
        await withCheckedContinuation { continuation in
            bridge.addItem(listId: target, name: name) { continuation.resume() }
        }
        return target
    }
}

import AppIntents
import ComposeApp

/// "Hey Siri, add milk to Chef Mate." Runs on the phone (no watch required): it adds the item to the
/// default list through the shared `WatchDataBridge`, which persists it and syncs to Supabase — the
/// same path the watch's add action flows through.
///
/// The item is a `GroceryItemEntity` rather than a plain `String` because App Shortcut phrases may
/// only embed `AppEntity`/`AppEnum` parameters. With a `String`, Siri could never match a one-shot
/// "Add milk to Chef Mate" to this intent and fell back to Reminders' built-in grocery list.
///
/// Runs without opening the app (`openAppWhenRun = false`). When Siri triggers this while the app is
/// not running, the system background-launches it, which builds the DI graph in `AppDelegate` before
/// `perform()` runs. `awaitBridge()` still guards the cold-launch race so we never touch the
/// `lateinit` bridge before it exists.
struct AddGroceryItemIntent: AppIntent {
    static var title: LocalizedStringResource = "Add to Chef Mate Grocery List"
    static var description = IntentDescription(
        "Adds an item to your Chef Mate grocery list (not Reminders)."
    )
    static var openAppWhenRun: Bool = false

    @Parameter(title: "Item", requestValueDialog: "What should I add to Chef Mate?")
    var item: GroceryItemEntity

    static var parameterSummary: some ParameterSummary {
        Summary("Add \(\.$item) to my Chef Mate grocery list")
    }

    func perform() async throws -> some IntentResult & ProvidesDialog {
        let name = item.id.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else {
            return .result(dialog: "I didn't catch what to add.")
        }

        guard let bridge = await GroceryBridge.ready() else {
            return .result(dialog: "Open Chef Mate once, then try adding that again.")
        }

        await withCheckedContinuation { continuation in
            bridge.ensureDefaultList { listId in
                // Resume only once the add has actually been persisted. `addItem` applies the write
                // on a coroutine, so resuming as soon as it returned let `perform()` finish — and
                // the system tear down the background-launched process — before the item was saved.
                bridge.addItem(listId: listId.int64Value, name: name) { continuation.resume() }
            }
        }

        // A brand-new item becomes a recognizable phrase value for next time.
        ChefMateAppShortcuts.updateAppShortcutParameters()
        return .result(dialog: "Added \(name) to your Chef Mate grocery list.")
    }
}

/// A grocery item name, spoken or typed. The id *is* the name, so any free-form item resolves —
/// the suggested values only teach Siri which names to listen for inside a phrase.
struct GroceryItemEntity: AppEntity {
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Grocery Item"
    static var defaultQuery = GroceryItemQuery()

    let id: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(title: "\(id)")
    }
}

struct GroceryItemQuery: EntityStringQuery {
    /// Each value multiplies into every parameterized phrase, and Siri caps an app's total phrases
    /// (~1,000), so keep this well under that divided by the phrase count below.
    private static let suggestionLimit: Int32 = 150

    func entities(for identifiers: [String]) async throws -> [GroceryItemEntity] {
        identifiers.map(GroceryItemEntity.init(id:))
    }

    /// Echo back exactly what was said. Returning extra fuzzy matches would make Siri ask "Which
    /// one?" after every free-form answer to "What should I add?".
    func entities(matching string: String) async throws -> [GroceryItemEntity] {
        let name = string.trimmingCharacters(in: .whitespacesAndNewlines)
        return name.isEmpty ? [] : [GroceryItemEntity(id: name)]
    }

    func suggestedEntities() async throws -> [GroceryItemEntity] {
        guard let bridge = await GroceryBridge.ready() else { return [] }
        let names: [String] = await withCheckedContinuation { continuation in
            bridge.siriSuggestionNames(limit: Self.suggestionLimit) { continuation.resume(returning: $0) }
        }
        return names.map(GroceryItemEntity.init(id:))
    }
}

private enum GroceryBridge {
    /// Waits briefly for the DI graph to finish building on a cold background launch. Returns nil if
    /// the bridge never appears (app never fully launched), so the caller can prompt the user.
    static func ready() async -> WatchDataBridge? {
        for _ in 0 ..< 50 {  // up to ~5s
            if let bridge = RootBlocProvider.shared.watchDataBridgeOrNull {
                return bridge
            }
            try? await Task.sleep(nanoseconds: 100_000_000)  // 0.1s
        }
        return RootBlocProvider.shared.watchDataBridgeOrNull
    }
}

/// The iPhone app's single App Shortcuts entry. Every phrase names the app via
/// `\(.applicationName)` and leans on "Chef Mate" rather than a bare "grocery list": Siri routes
/// generic "add to my grocery list" wording to Reminders, which owns a built-in grocery list.
///
/// The `\(\.$item)` phrases match only the suggested item names (see `GroceryItemQuery`) and are
/// refreshed via `updateAppShortcutParameters()`. Anything else falls through to the item-less
/// phrases, after which Siri asks "What should I add to Chef Mate?" and accepts any answer.
struct ChefMateAppShortcuts: AppShortcutsProvider {
    static var appShortcuts: [AppShortcut] {
        AppShortcut(
            intent: AddGroceryItemIntent(),
            phrases: [
                "Add \(\.$item) to \(.applicationName)",
                "Add \(\.$item) to my \(.applicationName) list",
                "Add \(\.$item) to my \(.applicationName) grocery list",
                "Put \(\.$item) on my \(.applicationName) list",
                "\(.applicationName) add \(\.$item)",
                "Add to \(.applicationName)",
                "Add to my \(.applicationName) list",
                "Add to my \(.applicationName) grocery list",
                "Add an item to my \(.applicationName) grocery list",
                "Add to my grocery list in \(.applicationName)",
            ],
            shortTitle: "Add Grocery Item",
            systemImageName: "cart.badge.plus"
        )
    }
}

import AppIntents
import Foundation

/// A Chef Mate grocery list in the shape Apple Intelligence understands (`reminders.list`).
@available(iOS 27.0, *)
@AppEntity(schema: .reminders.list)
struct GroceryListSchemaEntity {
    static let defaultQuery = GroceryListSchemaEntityQuery()

    var id: String
    var name: String
    var type: GroceryListTypeSchemaEnum

    /// The local list id the bridge addresses, kept off the schema shape.
    var listId: Int64

    var displayRepresentation: DisplayRepresentation {
        .init(title: "\(name)", image: .init(systemName: "cart"))
    }

    init(list: GrocerySchemaSupport.Snapshot.List) {
        // Plain properties first: the schema's own are wrapped, and a wrapper setter goes through
        // `self`, which Swift requires to be fully initialized.
        self.listId = list.id
        self.id = String(list.id)
        self.name = list.name
        self.type = .standard
    }

    /// An empty list, for the transient section entity that must carry one to satisfy the schema.
    init() {
        self.listId = 0
        self.id = ""
        self.name = ""
        self.type = .standard
    }
}

@available(iOS 27.0, *)
struct GroceryListSchemaEntityQuery: EntityStringQuery {
    func entities(for identifiers: [String]) async throws -> [GroceryListSchemaEntity] {
        let wanted = Set(identifiers)
        return try await lists().filter { wanted.contains($0.id) }
    }

    func entities(matching string: String) async throws -> [GroceryListSchemaEntity] {
        try await lists().filter { $0.name.localizedCaseInsensitiveContains(string) }
    }

    func suggestedEntities() async throws -> [GroceryListSchemaEntity] {
        try await lists()
    }

    private func lists() async throws -> [GroceryListSchemaEntity] {
        try await GrocerySchemaSupport.snapshot().lists.map(GroceryListSchemaEntity.init(list:))
    }
}

/// The schema requires the `standard` case by name. Chef Mate has one kind of list, and the SDK
/// doesn't document a grocery case, so only `standard` is declared rather than guessing one Siri
/// might not recognize.
@available(iOS 27.0, *)
@AppEnum(schema: .reminders.listType)
enum GroceryListTypeSchemaEnum: String {
    case standard

    static let caseDisplayRepresentations: [GroceryListTypeSchemaEnum: DisplayRepresentation] = [
        .standard: "Grocery List",
    ]
}

import AppIntents
import CoreLocation
import Foundation
import GeoToolbox

/// A Chef Mate grocery item in the shape Apple Intelligence understands (`reminders.reminder`).
///
/// Grocery items carry a name, a quantity and a checked state. Due dates, flags, tags, URLs,
/// location triggers and recurrence have no equivalent, so they stay empty or nil.
@available(iOS 27.0, *)
@AppEntity(schema: .reminders.reminder)
struct GroceryItemSchemaEntity {
    static let defaultQuery = GroceryItemSchemaEntityQuery()

    var id: String
    var title: String
    var list: GroceryListSchemaEntity
    var dueDate: DateComponents?
    var completionDate: Date?
    var creationDate: Date?
    var isCompleted: Bool
    var isFlagged: Bool?
    var recurrence: Calendar.RecurrenceRule?
    var locationTrigger: GroceryLocationTriggerSchemaEntity?
    var note: String?
    var tags: Set<String>
    var urls: [URL]

    var displayRepresentation: DisplayRepresentation {
        .init(title: "\(title)", image: .init(systemName: "cart"))
    }

    init(id: String, title: String, list: GroceryListSchemaEntity, isCompleted: Bool, note: String?) {
        self.id = id
        self.title = title
        self.list = list
        self.isCompleted = isCompleted
        self.note = note
        // Chef Mate grocery items have none of these.
        self.dueDate = nil
        self.completionDate = nil
        self.creationDate = nil
        self.isFlagged = nil
        self.recurrence = nil
        self.locationTrigger = nil
        self.tags = []
        self.urls = []
    }

    init(item: GrocerySchemaSupport.Snapshot.Item, list: GroceryListSchemaEntity) {
        self.init(
            id: String(item.id),
            title: item.name,
            list: list,
            isCompleted: item.isChecked,
            note: item.quantity
        )
    }
}

@available(iOS 27.0, *)
struct GroceryItemSchemaEntityQuery: EntityStringQuery {
    func entities(for identifiers: [String]) async throws -> [GroceryItemSchemaEntity] {
        let wanted = Set(identifiers)
        return try await items().filter { wanted.contains($0.id) }
    }

    func entities(matching string: String) async throws -> [GroceryItemSchemaEntity] {
        try await items().filter { $0.title.localizedCaseInsensitiveContains(string) }
    }

    func suggestedEntities() async throws -> [GroceryItemSchemaEntity] {
        try await items().filter { !$0.isCompleted }
    }

    private func items() async throws -> [GroceryItemSchemaEntity] {
        let snapshot = try await GrocerySchemaSupport.snapshot()
        let lists = Dictionary(
            uniqueKeysWithValues: snapshot.lists.map { ($0.id, GroceryListSchemaEntity(list: $0)) }
        )
        return snapshot.items.compactMap { item in
            lists[item.listId].map { GroceryItemSchemaEntity(item: item, list: $0) }
        }
    }
}

/// Chef Mate lists have no sections, so this only ever appears as nil. It exists because the
/// schema requires the parameter to have this shape.
@available(iOS 27.0, *)
@AppEntity(schema: .reminders.section)
struct GrocerySectionSchemaEntity: TransientAppEntity {
    var name: String
    var list: GroceryListSchemaEntity

    var displayRepresentation: DisplayRepresentation {
        .init(title: "\(name)")
    }

    init() {
        self.name = ""
        self.list = GroceryListSchemaEntity()
    }
}

/// Chef Mate items have no location trigger, so this only ever appears as nil. It exists because
/// the schema requires the property to have this shape.
@available(iOS 27.0, *)
@AppEntity(schema: .reminders.locationTrigger)
struct GroceryLocationTriggerSchemaEntity: TransientAppEntity {
    var place: PlaceDescriptor
    var event: GroceryLocationTriggerEventSchemaEnum

    var displayRepresentation: DisplayRepresentation {
        .init(title: "Location")
    }

    init() {
        // A `PlaceDescriptor` must describe something and traps otherwise; there is no place to
        // name, so this is the null coordinate rather than an invented address.
        self.place = PlaceDescriptor(
            representations: [.coordinate(CLLocationCoordinate2D())],
            commonName: nil
        )
        self.event = .arrive
    }
}

@available(iOS 27.0, *)
@AppEnum(schema: .reminders.locationTriggerEvent)
enum GroceryLocationTriggerEventSchemaEnum: String {
    case arrive
    case depart

    static let caseDisplayRepresentations: [GroceryLocationTriggerEventSchemaEnum: DisplayRepresentation] = [
        .arrive: "Arriving",
        .depart: "Leaving",
    ]
}

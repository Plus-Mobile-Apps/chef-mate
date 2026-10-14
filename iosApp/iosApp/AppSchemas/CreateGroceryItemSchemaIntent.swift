import AppIntents
import Foundation

/// Apple Intelligence Siri's "add to list" action, adopted for Chef Mate grocery lists: "Add dragon
/// fruit to my Chef Mate grocery list" with any item, no predefined phrase or vocabulary. Older
/// iOS versions and devices without Apple Intelligence keep using `AddGroceryItemIntent`'s App
/// Shortcut phrases.
///
/// Due dates, flags, tags, URLs, images, location triggers, recurrence and sections have no
/// equivalent on a grocery item, so they are accepted to satisfy the schema and then ignored rather
/// than folded into the item, which would put text on the list the user did not dictate.
@available(iOS 27.0, *)
@AppIntent(schema: .reminders.createReminder)
struct CreateGroceryItemSchemaIntent {
    var title: String
    var list: GroceryListSchemaEntity?
    var note: AttributedString?
    var isFlagged: Bool?
    var images: [IntentFile]
    var tags: Set<String>
    var urls: [URL]
    var dueDate: DateComponents?
    var recurrence: Calendar.RecurrenceRule?
    var locationTrigger: GroceryLocationTriggerSchemaEntity?
    var section: GrocerySectionSchemaEntity?

    func perform() async throws -> some ReturnsValue<GroceryItemSchemaEntity> {
        let name = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !name.isEmpty else {
            throw $title.needsValueError("What should I add to Chef Mate?")
        }
        let listId = try await GrocerySchemaSupport.addItem(name: name, listId: list?.listId)

        // A brand-new item becomes a recognizable App Shortcut phrase value too.
        ChefMateAppShortcuts.updateAppShortcutParameters()

        // `addItem` doesn't hand back the new row's id (and may merge into an existing one), so
        // the entity describes what was asked for, resolved against the list it landed on.
        let snapshot = try await GrocerySchemaSupport.snapshot()
        let target = snapshot.lists.first { $0.id == listId }.map(GroceryListSchemaEntity.init(list:))
            ?? list
            ?? GroceryListSchemaEntity()
        return .result(value: GroceryItemSchemaEntity(
            id: "\(listId)-\(name)",
            title: name,
            list: target,
            isCompleted: false,
            note: nil
        ))
    }
}

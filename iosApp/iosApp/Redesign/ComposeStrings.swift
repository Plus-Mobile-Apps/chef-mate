import Foundation
import ComposeApp

/// Native lookups into `Generated/ComposeStrings.xcstrings`, which Gradle generates from every
/// module's Compose `strings.xml` (`GenerateIosStringCatalogTask`). Keys are the Compose resource
/// names, so native screens and Kotlin `TextData` share one set of strings.
final class ComposeStrings: NSObject, StringLookup {
    static let shared = ComposeStrings()
    private static let table = "ComposeStrings"

    /// The string named `key` in the Compose resources, e.g. `ComposeStrings["sign_in"]`.
    static subscript(_ key: String) -> String { shared.string(key: key) }

    func string(key: String) -> String {
        NSLocalizedString(key, tableName: Self.table, comment: "")
    }

    func plural(key: String, quantity: Int32) -> String {
        String.localizedStringWithFormat(string(key: key), Int(quantity))
    }
}

extension TextData {
    /// This text resolved from the `ComposeStrings` catalog.
    var localized: String {
        TextDataLocalizer.shared.localize(text: self, lookup: ComposeStrings.shared)
    }
}

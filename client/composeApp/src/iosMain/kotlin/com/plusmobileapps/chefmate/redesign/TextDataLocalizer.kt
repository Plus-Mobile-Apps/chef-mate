package com.plusmobileapps.chefmate.redesign

import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.JoinedTextData
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.PluralResourceString
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.TextData

/**
 * Native string lookups, implemented in Swift against the generated `ComposeStrings` String Catalog
 * (see `GenerateIosStringCatalogTask`).
 */
interface StringLookup {
    fun string(key: String): String

    fun plural(key: String, quantity: Int): String
}

/**
 * Resolves [TextData] synchronously for the SwiftUI redesign. Mirrors [TextData.localized], with
 * Compose resource reads swapped for [StringLookup]. The traversal stays in Kotlin because the
 * `TextData` subclasses aren't visible to Swift.
 */
object TextDataLocalizer {
    fun localize(text: TextData, lookup: StringLookup): String =
        when (text) {
            is FixedString -> text.value
            is ResourceString -> lookup.string(text.resource.key)
            is PhraseModel -> lookup.string(text.resource.key).fill(text.args, lookup)
            is PluralResourceString ->
                lookup.plural(text.resource.key, text.quantity).fill(text.args, lookup)
            is JoinedTextData -> {
                val resolved = text.parts.map { localize(it, lookup) }
                when (resolved.size) {
                    0 -> ""
                    1 -> resolved.first()
                    2 -> resolved[0] + text.lastSeparator + resolved[1]
                    else ->
                        resolved.dropLast(1).joinToString(text.separator) +
                            text.lastSeparator +
                            resolved.last()
                }
            }
        }

    private fun String.fill(args: Map<String, TextData>, lookup: StringLookup): String {
        var result = this
        args.forEach { (key, value) -> result = result.replace("{$key}", localize(value, lookup)) }
        return result
    }
}

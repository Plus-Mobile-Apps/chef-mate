package com.plusmobileapps.chefmate.redesign

import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.JoinedTextData
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.PluralResourceString
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.TextData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/**
 * Resolves [TextData] outside a composition, for the SwiftUI redesign. Mirrors
 * [TextData.localized], using the suspending Compose resource readers.
 */
object TextDataResolver {
    fun resolve(text: TextData, onResult: (String) -> Unit) {
        MainScope().launch { onResult(text.resolve()) }
    }

    private suspend fun TextData.resolve(): String =
        when (this) {
            is FixedString -> value
            is ResourceString -> getString(resource)
            is PhraseModel -> getString(resource).fill(args)
            is PluralResourceString -> getPluralString(resource, quantity).fill(args)
            is JoinedTextData -> {
                val resolved = parts.map { it.resolve() }
                when (resolved.size) {
                    0 -> ""
                    1 -> resolved.first()
                    2 -> resolved[0] + lastSeparator + resolved[1]
                    else ->
                        resolved.dropLast(1).joinToString(separator) +
                            lastSeparator +
                            resolved.last()
                }
            }
        }

    private suspend fun String.fill(args: Map<String, TextData>): String {
        var result = this
        args.forEach { (key, value) -> result = result.replace("{$key}", value.resolve()) }
        return result
    }
}

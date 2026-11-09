package com.plusmobileapps.chefmate.ios

import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.w3c.dom.Element

/**
 * Generates an Xcode String Catalog (`.xcstrings`) from every module's Compose Multiplatform
 * `strings.xml`, so the SwiftUI redesign can resolve `TextData` synchronously from native iOS
 * localization while Compose resources stay the single source of truth.
 *
 * All modules share one table keyed by the resource name: names are unique across the app, and a
 * name defined twice with different text fails the build. Escapes are unwrapped the way Compose
 * resources does, and Android positional specifiers become their Foundation equivalents (`%1$s` →
 * `%1$@`, `%d` → `%lld`). `<plurals>` become plural variations. Xcode requires every variant to
 * reference the number, so the repo's `{quantity}` placeholder becomes `%lld` (filled by
 * `String.localizedStringWithFormat`) and a variant without it fails the build. Other
 * `{placeholder}`s are filled afterwards by the Kotlin `TextDataLocalizer`, like any other string.
 */
@CacheableTask
abstract class GenerateIosStringCatalogTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val strings: ConfigurableFileCollection

    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val entries = sortedMapOf<String, Entry>()
        val origins = mutableMapOf<String, String>()
        strings.files
            .sortedBy { it.path }
            .forEach { file ->
                val root =
                    DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(file)
                        .documentElement
                root.childElements().forEach { element ->
                    val name = element.getAttribute("name")
                    val entry =
                        when (element.tagName) {
                            "string" -> Entry.Single(element.textContent.toIosString())
                            "plurals" ->
                                Entry.Plural(
                                    element
                                        .childElements()
                                        .filter { it.tagName == "item" }
                                        .associate {
                                            val quantity = it.getAttribute("quantity")
                                            quantity to
                                                it.textContent.toPluralVariant(name, quantity)
                                        }
                                        .toSortedMap(),
                                )
                            else -> return@forEach
                        }
                    val existing = entries[name]
                    if (existing != null && existing != entry) {
                        throw GradleException(
                            "String '$name' is defined differently in ${origins[name]} and $file. " +
                                "The iOS String Catalog needs app-wide unique string names.",
                        )
                    }
                    entries[name] = entry
                    origins[name] = file.path
                }
            }
        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(render(entries))
        }
    }

    private sealed interface Entry {
        data class Single(val value: String) : Entry

        data class Plural(val variants: Map<String, String>) : Entry
    }

    private fun render(entries: Map<String, Entry>): String = buildString {
        appendLine("{")
        appendLine("  \"sourceLanguage\" : \"en\",")
        appendLine("  \"strings\" : {")
        entries.entries.forEachIndexed { index, (name, entry) ->
            appendLine("    ${name.json()} : {")
            appendLine("      \"extractionState\" : \"manual\",")
            appendLine("      \"localizations\" : {")
            appendLine("        \"en\" : {")
            when (entry) {
                is Entry.Single -> appendLine("          ${stringUnit(entry.value)}")
                is Entry.Plural -> {
                    appendLine("          \"variations\" : {")
                    appendLine("            \"plural\" : {")
                    entry.variants.entries.forEachIndexed { i, (quantity, value) ->
                        val comma = if (i < entry.variants.size - 1) "," else ""
                        appendLine(
                            "              ${quantity.json()} : { ${stringUnit(value)} }$comma",
                        )
                    }
                    appendLine("            }")
                    appendLine("          }")
                }
            }
            appendLine("        }")
            appendLine("      }")
            appendLine("    }" + if (index < entries.size - 1) "," else "")
        }
        appendLine("  },")
        appendLine("  \"version\" : \"1.0\"")
        appendLine("}")
    }

    private fun stringUnit(value: String) =
        "\"stringUnit\" : { \"state\" : \"translated\", \"value\" : ${value.json()} }"

    private fun Element.childElements(): List<Element> =
        (0 until childNodes.length).map { childNodes.item(it) }.filterIsInstance<Element>()

    /** Compose-resources unescaping, then Android → Foundation format specifiers. */
    private fun String.toIosString(): String =
        unescape()
            .replace(Regex("""%(\d+\$)?s"""), "%$1@")
            .replace(Regex("""%(\d+\$)?d"""), "%$1lld")

    /** A plural variant is a format string: escape literal `%`, then `{quantity}` → `%lld`. */
    private fun String.toPluralVariant(name: String, quantity: String): String {
        val variant = unescape().replace("%", "%%")
        if (QUANTITY_PLACEHOLDER !in variant) {
            throw GradleException(
                "Plural '$name' ($quantity) must contain $QUANTITY_PLACEHOLDER: iOS String " +
                    "Catalogs require every plural variant to reference the number.",
            )
        }
        return variant.replace(QUANTITY_PLACEHOLDER, "%lld")
    }

    private fun String.unescape(): String {
        return buildString {
            var i = 0
            val source = this@unescape
            while (i < source.length) {
                val c = source[i]
                if (c == '\\' && i + 1 < source.length) {
                    when (val next = source[i + 1]) {
                        'n' -> append('\n')
                        't' -> append('\t')
                        'u' -> {
                            append(source.substring(i + 2, i + 6).toInt(16).toChar())
                            i += 4
                        }
                        else -> append(next)
                    }
                    i += 2
                } else {
                    append(c)
                    i++
                }
            }
        }
    }

    private fun String.json(): String = buildString {
        append('"')
        this@json.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
        append('"')
    }

    private companion object {
        const val QUANTITY_PLACEHOLDER = "{quantity}"
    }
}

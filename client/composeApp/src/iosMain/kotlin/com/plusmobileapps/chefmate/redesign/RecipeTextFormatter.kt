package com.plusmobileapps.chefmate.redesign

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.plusmobileapps.chefmate.recipe.categories.pickerLabelRes
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.IngredientScaler
import com.plusmobileapps.chefmate.recipe.data.IngredientSection
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.ui.text.ListLine
import com.plusmobileapps.chefmate.ui.text.parseListLine
import com.plusmobileapps.chefmate.ui.text.toDisplayAnnotatedString
import com.plusmobileapps.chefmate.ui.text.toInlineMarkdownAnnotatedString
import com.plusmobileapps.chefmate.ui.text.withoutLineBreakTags

/** A run of text with one style. [url] is set when the run is a link's label. */
class TextRun(val text: String, val isBold: Boolean, val isItalic: Boolean, val url: String?)

/**
 * One display line of a recipe's ingredients or directions. [isHeader] marks a sub-section header
 * ("For the sauce:"), rendered bold and not tappable.
 */
class RecipeTextLine(val isHeader: Boolean, val runs: List<TextRun>)

/**
 * Turns recipe text into styled runs for the SwiftUI redesign, reusing the exact parsing the
 * Compose `RecipeDetailScreen` uses (inline markdown, list markers, section headers, ingredient
 * scaling) so both UIs read a recipe the same way. Swift only maps runs to an `AttributedString`.
 */
object RecipeTextFormatter {
    /** The ingredient lines, amounts scaled by [scale]. */
    fun ingredientLines(recipe: Recipe, scale: Double): List<RecipeTextLine> =
        splitLines(recipe.ingredients).map { line ->
            if (IngredientSection.isHeader(line)) {
                RecipeTextLine(isHeader = true, runs = listOf(plainRun(line)))
            } else {
                // Scale the marker-stripped content so a leading bullet doesn't hide the amount.
                val parsed = parseListLine(line)
                val scaled = ListLine(parsed.marker, IngredientScaler.scale(parsed.content, scale))
                RecipeTextLine(
                    isHeader = false,
                    runs = scaled.toDisplayAnnotatedString(onLinkClick = {}).toRuns(),
                )
            }
        }

    /** The direction lines; the author's own bullets and numbers are kept, never generated. */
    fun directionLines(recipe: Recipe): List<RecipeTextLine> =
        splitLines(recipe.directions).map { line ->
            if (IngredientSection.isHeader(line)) {
                RecipeTextLine(isHeader = true, runs = listOf(plainRun(line)))
            } else {
                RecipeTextLine(
                    isHeader = false,
                    runs = parseListLine(line).toDisplayAnnotatedString(onLinkClick = {}).toRuns(),
                )
            }
        }

    /** A single paragraph of inline markdown, such as the recipe description. */
    fun inlineRuns(text: String): List<TextRun> =
        text.toInlineMarkdownAnnotatedString(onLinkClick = {}).toRuns()

    /** Display names of [recipe]'s categories, built-in ones localized through [lookup]. */
    fun categoryLabels(recipe: Recipe, lookup: StringLookup): List<String> =
        recipe.categories.map { category ->
            BuiltinCategory.fromId(category.builtinId)?.let {
                lookup.string(it.pickerLabelRes().key)
            } ?: category.name
        }

    /** The factors the ingredient scale menu offers (½× through 4×). */
    val scaleOptions: List<Double>
        get() = IngredientScaler.DEFAULT_FACTORS

    /** The menu label for [factor], e.g. `2×`. */
    fun scaleLabel(factor: Double): String = IngredientScaler.label(factor)

    /** The whole recipe as plain text, for the "share recipe text" action. */
    fun shareText(recipe: Recipe): String = buildString {
        appendLine(recipe.title)
        recipe.description?.let {
            appendLine()
            appendLine(it)
        }
        appendLine()
        appendLine("Ingredients:")
        appendLine(recipe.ingredients)
        appendLine()
        appendLine("Directions:")
        appendLine(recipe.directions)
        recipe.sourceUrl?.let {
            appendLine()
            append("Source: $it")
        }
    }

    private fun splitLines(text: String): List<String> =
        text.withoutLineBreakTags().split("\n").filter { it.isNotBlank() }

    private fun plainRun(text: String) = TextRun(text, isBold = false, isItalic = false, url = null)

    /** Flattens the bold/italic span styles and link annotations into consecutive runs. */
    private fun AnnotatedString.toRuns(): List<TextRun> {
        val links = getLinkAnnotations(0, length)
        val boundaries = buildSet {
            add(0)
            add(length)
            spanStyles.forEach {
                add(it.start)
                add(it.end)
            }
            links.forEach {
                add(it.start)
                add(it.end)
            }
        }
            .filter { it in 0..length }
            .sorted()
        return boundaries.zipWithNext().mapNotNull { (start, end) ->
            if (start == end) return@mapNotNull null
            val styles = spanStyles.filter { it.start <= start && it.end >= end }
            val url =
                links
                    .firstOrNull { it.start <= start && it.end >= end }
                    ?.let { (it.item as? LinkAnnotation.Clickable)?.tag }
            TextRun(
                text = text.substring(start, end),
                isBold = styles.any { it.item.fontWeight == FontWeight.Bold },
                isItalic = styles.any { it.item.fontStyle == FontStyle.Italic },
                url = url,
            )
        }
    }
}

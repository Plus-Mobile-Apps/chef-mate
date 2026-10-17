package com.plusmobileapps.chefmate.grocery.data

/**
 * Parsing and formatting for the free-form quantity prefix on a grocery line — "2 cups", "1 1/2
 * lbs", "1½", "3". Shared by [IngredientQuantityMerger], which sums two lines that resolve to the
 * same item, and [GroceryQuantityStepper], which nudges a single amount up or down.
 *
 * Units are canonicalized on the way in ("gallons" → "gal") and rendered back out in the form that
 * matches the resulting count ("1 cup" vs "2 cups"), so both callers agree on how a quantity reads.
 */
internal object GroceryQuantityFormat {

    data class AmountAndUnit(val amount: Double, val unit: String?)

    fun parse(quantity: String): AmountAndUnit? {
        val trimmed = quantity.trim()
        val amountMatch = LEADING_AMOUNT_REGEX.find(trimmed) ?: return null
        val amount = parseAmount(amountMatch.value) ?: return null
        val unitWord = trimmed.substring(amountMatch.value.length).trim().lowercase()
        val unit = unitWord.ifBlank { null }?.let { UNIT_SYNONYMS[it] ?: it }
        return AmountAndUnit(amount, unit)
    }

    private fun parseAmount(token: String): Double? {
        MIXED_NUMBER_REGEX.matchEntire(token)?.let { match ->
            val (whole, num, den) = match.destructured
            val denominator = den.toDoubleOrNull() ?: return null
            if (denominator == 0.0) return null
            return whole.toDouble() + num.toDouble() / denominator
        }
        FRACTION_REGEX.matchEntire(token)?.let { match ->
            val (num, den) = match.destructured
            val denominator = den.toDoubleOrNull() ?: return null
            if (denominator == 0.0) return null
            return num.toDouble() / denominator
        }
        VULGAR_FRACTIONS[token.lastOrNull()]?.let { fraction ->
            val wholePart = token.dropLast(1)
            val whole = if (wholePart.isEmpty()) 0.0 else wholePart.toDoubleOrNull() ?: return null
            return whole + fraction
        }
        return token.toDoubleOrNull()
    }

    fun format(amount: Double, unit: String?): String {
        val rounded = kotlin.math.round(amount * 100) / 100
        val amountText =
            if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
            else rounded.toString()
        if (unit == null) return amountText
        val display = UNIT_DISPLAY[unit]
        val unitText =
            when {
                display == null -> unit
                rounded == 1.0 -> display.first
                else -> display.second
            }
        return "$amountText $unitText"
    }

    // Matches a leading amount token: a mixed number ("1 1/2"), a mixed unicode fraction ("1½"),
    // a bare unicode fraction ("½"), a simple fraction ("1/2"), a decimal, or a plain integer.
    // Order matters — longer/more specific alternatives must come first.
    private val LEADING_AMOUNT_REGEX =
        Regex("""^\d+\s+\d+/\d+|^\d+[½⅓⅔¼¾⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞]|^[½⅓⅔¼¾⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞]|^\d+/\d+|^\d+\.\d+|^\d+""")
    private val MIXED_NUMBER_REGEX = Regex("""^(\d+)\s+(\d+)/(\d+)$""")
    private val FRACTION_REGEX = Regex("""^(\d+)/(\d+)$""")

    private val VULGAR_FRACTIONS: Map<Char, Double> =
        mapOf(
            '½' to 1.0 / 2,
            '⅓' to 1.0 / 3,
            '⅔' to 2.0 / 3,
            '¼' to 1.0 / 4,
            '¾' to 3.0 / 4,
            '⅕' to 1.0 / 5,
            '⅖' to 2.0 / 5,
            '⅗' to 3.0 / 5,
            '⅘' to 4.0 / 5,
            '⅙' to 1.0 / 6,
            '⅚' to 5.0 / 6,
            '⅛' to 1.0 / 8,
            '⅜' to 3.0 / 8,
            '⅝' to 5.0 / 8,
            '⅞' to 7.0 / 8,
        )

    // Canonical volume/weight units. Unlike countable units and size descriptors (cans, cloves,
    // "large", "medium"…), these measure a continuous quantity, so a bare re-add — an implicit
    // count of 1 with no unit — can't be summed into them.
    val MEASUREMENT_UNITS: Set<String> =
        setOf("cup", "tbsp", "tsp", "oz", "lb", "g", "kg", "ml", "l", "qt", "gal", "pt")

    private val UNIT_SYNONYMS: Map<String, String> = buildMap {
        fun canonical(unit: String, vararg aliases: String) {
            put(unit, unit)
            aliases.forEach { put(it, unit) }
        }
        canonical("cup", "cups")
        canonical("tbsp", "tablespoon", "tablespoons")
        canonical("tsp", "teaspoon", "teaspoons")
        canonical("oz", "ounce", "ounces")
        canonical("lb", "lbs", "pound", "pounds")
        canonical("g", "gram", "grams")
        canonical("kg", "kilogram", "kilograms")
        canonical("ml", "milliliter", "milliliters")
        canonical("l", "liter", "liters")
        canonical("qt", "quart", "quarts")
        canonical("gal", "gallon", "gallons")
        canonical("pt", "pint", "pints")
        canonical("can", "cans")
        canonical("bottle", "bottles")
        canonical("package", "packages", "pkg", "pkgs")
        canonical("bag", "bags")
        canonical("bunch", "bunches")
        canonical("head", "heads")
        canonical("clove", "cloves")
        canonical("stalk", "stalks")
        canonical("slice", "slices")
        canonical("piece", "pieces")
        canonical("stick", "sticks")
        canonical("sprig", "sprigs")
        canonical("ear", "ears")
    }

    // Only units whose plural form isn't identical to the singular need an entry; anything
    // missing (abbreviations like "tbsp", "oz", metric units) is used as-is regardless of count.
    private val UNIT_DISPLAY: Map<String, Pair<String, String>> =
        mapOf(
            "cup" to ("cup" to "cups"),
            "lb" to ("lb" to "lbs"),
            "can" to ("can" to "cans"),
            "bottle" to ("bottle" to "bottles"),
            "package" to ("package" to "packages"),
            "bag" to ("bag" to "bags"),
            "bunch" to ("bunch" to "bunches"),
            "head" to ("head" to "heads"),
            "clove" to ("clove" to "cloves"),
            "stalk" to ("stalk" to "stalks"),
            "slice" to ("slice" to "slices"),
            "piece" to ("piece" to "pieces"),
            "stick" to ("stick" to "sticks"),
            "sprig" to ("sprig" to "sprigs"),
            "ear" to ("ear" to "ears"),
        )
}

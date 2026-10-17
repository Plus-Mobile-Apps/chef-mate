package com.plusmobileapps.chefmate.grocery.data

/**
 * Combines two raw ingredient lines (e.g. "2 cups flour" + "1 cup flour") that [IngredientParser]
 * resolves to the same item name, so adding a recipe's ingredients to a grocery list merges into
 * one row instead of creating a duplicate. Amounts are summed when both lines carry a parseable
 * quantity in the same unit; otherwise the two quantities are concatenated rather than guessed at.
 */
object IngredientQuantityMerger {

    fun merge(existingLine: String, newLine: String): String {
        val existing = IngredientParser.parse(existingLine)
        val incoming = IngredientParser.parse(newLine)
        val name = existing.name.ifBlank { incoming.name }
        val mergedQuantity = mergeQuantities(existing.quantity, incoming.quantity)
        return if (mergedQuantity == null) name else "$mergedQuantity $name"
    }

    private fun mergeQuantities(a: String?, b: String?): String? {
        val parsedA = a?.let { GroceryQuantityFormat.parse(it) }
        val parsedB = b?.let { GroceryQuantityFormat.parse(it) }

        // A bare item (no quantity at all) is an implicit count of 1 with no unit.
        val amountA = if (a == null) 1.0 else parsedA?.amount
        val amountB = if (b == null) 1.0 else parsedB?.amount
        val unitA = if (a == null) null else parsedA?.unit
        val unitB = if (b == null) null else parsedB?.unit

        if (amountA == null || amountB == null) {
            // A non-null quantity we couldn't pull an amount from; keep something visible.
            return keepSpecified(a, b)
        }

        // Decide whether the two amounts can be summed, and under which unit.
        //  - Same unit → sum (e.g. "2 cups" + "1 cup", or "2 large" + "1 large").
        //  - A bare re-add (implicit count of 1) combines with a *countable* unit or descriptor
        //    like "large" or "clove", adopting it: "2 large red bell pepper" + a bare re-add
        //    means three of them. It cannot combine with a volume/weight measurement, since a
        //    dimensionless "+1" onto "2 cups" is meaningless — that keeps its measured quantity.
        val combinedUnit: String? =
            when {
                unitA == unitB -> unitA
                a == null && unitB !in GroceryQuantityFormat.MEASUREMENT_UNITS -> unitB
                b == null && unitA !in GroceryQuantityFormat.MEASUREMENT_UNITS -> unitA
                else -> return keepSpecified(a, b)
            }
        return GroceryQuantityFormat.format(amountA + amountB, combinedUnit)
    }

    // Units couldn't be combined: keep the side that actually specifies a quantity; if both do,
    // list them so neither is lost.
    private fun keepSpecified(a: String?, b: String?): String? =
        when {
            a == null -> b
            b == null -> a
            else -> "$a + $b"
        }
}

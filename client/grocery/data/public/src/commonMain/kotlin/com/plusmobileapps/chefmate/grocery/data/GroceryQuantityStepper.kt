package com.plusmobileapps.chefmate.grocery.data

import kotlin.math.max

/**
 * Nudges the leading amount of a grocery quantity up or down by one while leaving its unit alone —
 * "1 gal" → "2 gal", "2 cups" → "1 cup". Backs the +/- buttons on the grocery detail sheet so the
 * common case (one more of the same thing) doesn't need the keyboard.
 *
 * A quantity we can't read an amount out of ("a pinch") is left untouched: guessing at what to
 * increment would be worse than doing nothing, so the buttons report themselves as unavailable via
 * [canIncrement] / [canDecrement]. Stepping never produces zero or a negative amount — an item on a
 * grocery list is there because at least one is wanted, and removing it is what delete is for.
 */
object GroceryQuantityStepper {

    /** The smallest amount stepping will produce. */
    private const val MIN_AMOUNT = 1.0

    /**
     * The stepped quantity, or [quantity] unchanged when it can't be stepped. An empty quantity
     * starts at "1", since an item with no amount is an implicit one.
     */
    fun increment(quantity: String?): String? {
        if (quantity.isNullOrBlank()) return MIN_AMOUNT.toAmountText()
        val parsed = GroceryQuantityFormat.parse(quantity) ?: return quantity
        return GroceryQuantityFormat.format(parsed.amount + 1, parsed.unit)
    }

    /**
     * The stepped quantity, or [quantity] unchanged when it is already at [MIN_AMOUNT] or lower.
     */
    fun decrement(quantity: String?): String? {
        if (quantity.isNullOrBlank()) return quantity
        val parsed = GroceryQuantityFormat.parse(quantity) ?: return quantity
        if (parsed.amount <= MIN_AMOUNT) return quantity
        return GroceryQuantityFormat.format(max(MIN_AMOUNT, parsed.amount - 1), parsed.unit)
    }

    fun canIncrement(quantity: String?): Boolean =
        quantity.isNullOrBlank() || GroceryQuantityFormat.parse(quantity) != null

    fun canDecrement(quantity: String?): Boolean {
        if (quantity.isNullOrBlank()) return false
        val parsed = GroceryQuantityFormat.parse(quantity) ?: return false
        return parsed.amount > MIN_AMOUNT
    }

    private fun Double.toAmountText(): String = GroceryQuantityFormat.format(this, unit = null)
}

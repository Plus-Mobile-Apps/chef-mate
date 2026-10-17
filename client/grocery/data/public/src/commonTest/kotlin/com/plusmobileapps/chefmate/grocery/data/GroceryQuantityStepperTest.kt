@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.grocery.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GroceryQuantityStepperTest {

    @Test
    fun increment_keeps_the_unit() {
        assertEquals("2 gal", GroceryQuantityStepper.increment("1 gal"))
    }

    @Test
    fun increment_pluralizes_the_unit() {
        assertEquals("2 cups", GroceryQuantityStepper.increment("1 cup"))
    }

    @Test
    fun decrement_singularizes_the_unit() {
        assertEquals("1 cup", GroceryQuantityStepper.decrement("2 cups"))
    }

    @Test
    fun increment_of_a_bare_count_has_no_unit() {
        assertEquals("4", GroceryQuantityStepper.increment("3"))
    }

    @Test
    fun increment_of_an_empty_quantity_starts_at_one() {
        assertEquals("1", GroceryQuantityStepper.increment(null))
        assertEquals("1", GroceryQuantityStepper.increment(""))
    }

    @Test
    fun increment_adds_a_whole_unit_to_a_fraction() {
        assertEquals("1.5 cups", GroceryQuantityStepper.increment("1/2 cup"))
    }

    @Test
    fun decrement_clamps_at_one() {
        assertEquals("1 cup", GroceryQuantityStepper.decrement("1.5 cups"))
    }

    @Test
    fun decrement_leaves_an_amount_already_at_one_alone() {
        assertEquals("1 gal", GroceryQuantityStepper.decrement("1 gal"))
    }

    @Test
    fun decrement_leaves_an_empty_quantity_empty() {
        assertEquals(null, GroceryQuantityStepper.decrement(null))
    }

    @Test
    fun stepping_leaves_an_unreadable_quantity_alone() {
        assertEquals("a pinch", GroceryQuantityStepper.increment("a pinch"))
        assertEquals("a pinch", GroceryQuantityStepper.decrement("a pinch"))
    }

    @Test
    fun buttons_are_available_only_when_stepping_would_do_something() {
        assertTrue(GroceryQuantityStepper.canIncrement(null))
        assertTrue(GroceryQuantityStepper.canIncrement("2 cups"))
        assertFalse(GroceryQuantityStepper.canIncrement("a pinch"))

        assertTrue(GroceryQuantityStepper.canDecrement("2 cups"))
        assertFalse(GroceryQuantityStepper.canDecrement("1 cup"))
        assertFalse(GroceryQuantityStepper.canDecrement(null))
        assertFalse(GroceryQuantityStepper.canDecrement("a pinch"))
    }
}

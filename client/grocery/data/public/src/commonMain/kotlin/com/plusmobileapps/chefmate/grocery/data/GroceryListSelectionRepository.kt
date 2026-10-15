package com.plusmobileapps.chefmate.grocery.data

/**
 * Remembers the grocery list the user last picked so the grocery screen reopens on it next launch.
 */
interface GroceryListSelectionRepository {
    /** Local id of the list the user last picked, or null when they haven't picked one. */
    val lastSelectedListId: Long?

    fun select(listId: Long)

    fun clear()
}

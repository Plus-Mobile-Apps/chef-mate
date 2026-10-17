package com.plusmobileapps.chefmate.grocery.data.testing

import com.plusmobileapps.chefmate.grocery.data.GroceryListSelectionRepository

class FakeGroceryListSelectionRepository(override var lastSelectedListId: Long? = null) :
    GroceryListSelectionRepository {

    override fun select(listId: Long) {
        lastSelectedListId = listId
    }

    override fun clear() {
        lastSelectedListId = null
    }
}

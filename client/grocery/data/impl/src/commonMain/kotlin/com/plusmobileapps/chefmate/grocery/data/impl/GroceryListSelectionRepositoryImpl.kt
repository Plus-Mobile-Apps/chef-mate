package com.plusmobileapps.chefmate.grocery.data.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.grocery.data.GroceryListSelectionRepository
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class GroceryListSelectionRepositoryImpl(private val settings: Settings) :
    GroceryListSelectionRepository {

    override val lastSelectedListId: Long?
        get() = settings.getLongOrNull(KEY_SELECTED_LIST_ID)

    override fun select(listId: Long) {
        settings.putLong(KEY_SELECTED_LIST_ID, listId)
    }

    override fun clear() {
        settings.remove(KEY_SELECTED_LIST_ID)
    }

    private companion object {
        const val KEY_SELECTED_LIST_ID = "grocery_selected_list_id"
    }
}

@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.grocery.data.impl

import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class GroceryListSelectionRepositoryImplTest {

    private val settings = MapSettings()

    @Test
    fun nothing_is_selected_before_the_user_picks_a_list() {
        GroceryListSelectionRepositoryImpl(settings).lastSelectedListId shouldBe null
    }

    @Test
    fun remembers_the_selected_list_across_launches() {
        GroceryListSelectionRepositoryImpl(settings).select(42L)

        GroceryListSelectionRepositoryImpl(settings).lastSelectedListId shouldBe 42L
    }

    @Test
    fun clearing_forgets_the_selected_list() {
        val repo = GroceryListSelectionRepositoryImpl(settings)
        repo.select(42L)

        repo.clear()

        GroceryListSelectionRepositoryImpl(settings).lastSelectedListId shouldBe null
    }
}

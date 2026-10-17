@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.recipebook.data.impl

import com.plusmobileapps.chefmate.recipebook.data.RecipeBookSelection
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class RecipeBookSelectionRepositoryImplTest {

    private val settings = MapSettings()

    @Test
    fun nothing_is_selected_before_the_user_picks_a_book() {
        RecipeBookSelectionRepositoryImpl(settings).lastSelection shouldBe null
    }

    @Test
    fun remembers_the_selected_book_across_launches() {
        RecipeBookSelectionRepositoryImpl(settings).select(RecipeBookSelection.Book(7L))

        RecipeBookSelectionRepositoryImpl(settings).lastSelection shouldBe
            RecipeBookSelection.Book(7L)
    }

    @Test
    fun remembers_the_all_recipes_view_across_launches() {
        RecipeBookSelectionRepositoryImpl(settings).select(RecipeBookSelection.AllRecipes)

        RecipeBookSelectionRepositoryImpl(settings).lastSelection shouldBe
            RecipeBookSelection.AllRecipes
    }

    @Test
    fun clearing_forgets_the_selection() {
        val repo = RecipeBookSelectionRepositoryImpl(settings)
        repo.select(RecipeBookSelection.Book(7L))

        repo.clear()

        RecipeBookSelectionRepositoryImpl(settings).lastSelection shouldBe null
    }

    @Test
    fun reads_the_no_selection_sentinel_older_builds_stored() {
        settings.putLong("recipe_active_book", -1L)

        RecipeBookSelectionRepositoryImpl(settings).lastSelection shouldBe null
    }
}

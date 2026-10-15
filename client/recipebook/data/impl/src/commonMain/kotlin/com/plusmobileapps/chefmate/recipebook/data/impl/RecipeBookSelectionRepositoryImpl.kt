package com.plusmobileapps.chefmate.recipebook.data.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookSelection
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookSelectionRepository
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class RecipeBookSelectionRepositoryImpl(private val settings: Settings) :
    RecipeBookSelectionRepository {

    override val lastSelection: RecipeBookSelection?
        get() =
            when (val stored = settings.getLongOrNull(KEY_ACTIVE_BOOK)) {
                null,
                NO_ACTIVE_BOOK -> null
                ALL_RECIPES_SELECTED -> RecipeBookSelection.AllRecipes
                else -> RecipeBookSelection.Book(stored)
            }

    override fun select(selection: RecipeBookSelection) {
        val stored =
            when (selection) {
                RecipeBookSelection.AllRecipes -> ALL_RECIPES_SELECTED
                is RecipeBookSelection.Book -> selection.id
            }
        settings.putLong(KEY_ACTIVE_BOOK, stored)
    }

    override fun clear() {
        settings.remove(KEY_ACTIVE_BOOK)
    }

    private companion object {
        // Kept from when the book repository stored this itself, so existing choices survive.
        const val KEY_ACTIVE_BOOK = "recipe_active_book"
        // Older builds wrote this sentinel instead of removing the key.
        const val NO_ACTIVE_BOOK = -1L
        const val ALL_RECIPES_SELECTED = -2L
    }
}

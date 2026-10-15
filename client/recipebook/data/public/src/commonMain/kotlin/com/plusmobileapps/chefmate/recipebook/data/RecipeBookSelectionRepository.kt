package com.plusmobileapps.chefmate.recipebook.data

/**
 * Remembers the recipe book the user last picked so the recipe list reopens on it next launch. Only
 * stores the choice — [RecipeBookRepository] checks the book still exists before restoring it.
 */
interface RecipeBookSelectionRepository {
    /** What the user last picked, or null when they haven't picked anything. */
    val lastSelection: RecipeBookSelection?

    fun select(selection: RecipeBookSelection)

    fun clear()
}

sealed interface RecipeBookSelection {
    /** The cross-book "All recipes" view. */
    data object AllRecipes : RecipeBookSelection

    data class Book(val id: Long) : RecipeBookSelection
}

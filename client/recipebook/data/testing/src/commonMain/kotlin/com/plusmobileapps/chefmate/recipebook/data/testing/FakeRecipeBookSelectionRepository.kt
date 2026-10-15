package com.plusmobileapps.chefmate.recipebook.data.testing

import com.plusmobileapps.chefmate.recipebook.data.RecipeBookSelection
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookSelectionRepository

class FakeRecipeBookSelectionRepository(override var lastSelection: RecipeBookSelection? = null) :
    RecipeBookSelectionRepository {

    override fun select(selection: RecipeBookSelection) {
        lastSelection = selection
    }

    override fun clear() {
        lastSelection = null
    }
}

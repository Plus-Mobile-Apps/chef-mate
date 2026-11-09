package com.plusmobileapps.chefmate.recipe.core.addgrocery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.recipe.data.DEFAULT_INGREDIENT_SCALE
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow

interface AddRecipeToGroceryListBloc : BackClickBloc, ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        AddRecipeToGroceryListScreen(bloc = this, modifier = modifier)
    }

    fun onIngredientToggled(ingredient: Int)

    fun onGroceryListSelected(listId: Long)

    /**
     * Pick a new ingredient scale factor (e.g. `2.0` for 2×). Shares the per-recipe preference with
     * the recipe detail screen and Cook Mode, and is applied to the amounts added to the list.
     */
    fun onScaleChanged(scale: Double)

    fun onSaveClicked()

    data class GroceryListItem(
        val id: Long,
        val name: String,
        /** The remote id of the family this list belongs to, or null for a personal list. */
        val familyId: String? = null,
        /** The family's name, for the section header; null when unknown or for a personal list. */
        val familyName: String? = null,
        val isShared: Boolean = false,
    ) {
        val isFamily: Boolean
            get() = familyId != null
    }

    /** A titled run of lists in the grocery list picker. */
    data class GroceryListSection(val header: Header, val lists: ImmutableList<GroceryListItem>) {
        sealed class Header {
            data object Personal : Header()

            /** One per family; [name] is null when the family's name isn't known yet. */
            data class Family(val name: String?) : Header()

            data object SharedWithYou : Header()
        }
    }

    data class IngredientGroup(val category: GroceryCategory, val items: ImmutableList<ListItem>)

    data class ListItem(
        val id: Int,
        val name: String,
        val displayName: String,
        val quantity: String? = null,
        val isSelected: Boolean,
    )

    data class Model(
        val isLoading: Boolean,
        val isAdding: Boolean,
        val groupedIngredients: ImmutableList<IngredientGroup>,
        val groceryLists: ImmutableList<GroceryListItem> = persistentListOf(),
        val selectedGroceryList: GroceryListItem? = null,
        /** The factor the listed amounts are scaled by (1× = the recipe author's amounts). */
        val ingredientScale: Double = DEFAULT_INGREDIENT_SCALE,
    ) {
        val hasSelectedIngredients: Boolean
            get() = groupedIngredients.any { group -> group.items.any { it.isSelected } }

        val isEmpty: Boolean
            get() = groupedIngredients.isEmpty()

        /**
         * [groceryLists] split into Personal / one-per-family / Shared with you, in that order,
         * mirroring the grocery list screen's own picker. Empty sections are dropped.
         */
        val groceryListSections: ImmutableList<GroceryListSection>
            get() {
                val personal = groceryLists.filter { !it.isFamily && !it.isShared }
                val families = groceryLists.filter { it.isFamily }.groupBy { it.familyId }
                val shared = groceryLists.filter { it.isShared && !it.isFamily }
                return buildList {
                    if (personal.isNotEmpty()) {
                        add(
                            GroceryListSection(
                                GroceryListSection.Header.Personal,
                                personal.toImmutableList(),
                            ),
                        )
                    }
                    families.values.forEach { lists ->
                        add(
                            GroceryListSection(
                                GroceryListSection.Header.Family(lists.first().familyName),
                                lists.toImmutableList(),
                            ),
                        )
                    }
                    if (shared.isNotEmpty()) {
                        add(
                            GroceryListSection(
                                GroceryListSection.Header.SharedWithYou,
                                shared.toImmutableList(),
                            ),
                        )
                    }
                }
                    .toImmutableList()
            }
    }

    sealed class Output {
        data object Finished : Output()

        data object Added : Output()
    }

    fun interface Factory {
        fun create(
            context: BlocContext,
            recipeId: Long,
            output: Consumer<Output>,
        ): AddRecipeToGroceryListBloc
    }
}

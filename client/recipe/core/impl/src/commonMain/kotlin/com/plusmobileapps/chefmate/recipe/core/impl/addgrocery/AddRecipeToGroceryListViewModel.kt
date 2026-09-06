package com.plusmobileapps.chefmate.recipe.core.impl.addgrocery

import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.combineStates
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import com.plusmobileapps.chefmate.grocery.data.IngredientParser
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.GroceryListItem
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.IngredientGroup
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.ListItem
import com.plusmobileapps.chefmate.recipe.data.DEFAULT_INGREDIENT_SCALE
import com.plusmobileapps.chefmate.recipe.data.IngredientScalePreferences
import com.plusmobileapps.chefmate.recipe.data.IngredientScaler
import com.plusmobileapps.chefmate.recipe.data.IngredientSection
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.ui.text.withoutLineBreakTags
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
class AddRecipeToGroceryListViewModel(
    @Assisted private val recipeId: Long,
    @Main mainContext: CoroutineContext,
    private val recipeRepository: RecipeRepository,
    private val groceryRepository: GroceryRepository,
    private val scalePreferences: IngredientScalePreferences,
    private val settings: Settings,
) : ViewModel(mainContext) {
    private val _output = Channel<Output>(Channel.BUFFERED)
    val output: Flow<Output> = _output.receiveAsFlow()
    private val _state = MutableStateFlow(State())

    // The ingredient rows are derived rather than stored: the persisted scale for this recipe is
    // folded in here so the amounts shown — and the ones handed to the grocery list on save —
    // always reflect whatever factor was last chosen, including one set over on recipe detail or
    // in Cook Mode. Selection lives in the raw state as ids, so it survives a rescale.
    val state: StateFlow<State> =
        combineStates(_state, scalePreferences.scaleFor(recipeId)) { state, scale ->
            state.copy(
                ingredientScale = scale,
                groupedIngredients =
                    groupIngredients(
                        lines = state.ingredientLines,
                        deselectedIds = state.deselectedIngredientIds,
                        scale = scale,
                    ),
            )
        }

    init {
        loadGroceryLists()
        loadRecipe()
    }

    override fun onCleared() {
        super.onCleared()
        _output.close()
    }

    fun toggleIngredient(ingredientId: Int) {
        _state.update {
            val deselected = it.deselectedIngredientIds
            it.copy(
                deselectedIngredientIds =
                    if (ingredientId in deselected) {
                        deselected - ingredientId
                    } else {
                        deselected + ingredientId
                    }
            )
        }
    }

    /**
     * Persist the chosen ingredient [scale] for this recipe; the state flow reflects it in turn.
     */
    fun setScale(scale: Double) {
        scalePreferences.setScale(recipeId, scale)
    }

    fun onGroceryListSelected(listId: Long) {
        val list = _state.value.groceryLists.firstOrNull { it.id == listId } ?: return
        _state.update { it.copy(selectedGroceryList = list) }
        settings.putLong(KEY_LAST_GROCERY_LIST_ID, listId)
    }

    fun save() {
        val ingredients =
            state.value.groupedIngredients
                .flatMap { it.items }
                .filter { it.isSelected }
                .map { it.name }
        val selectedListId = state.value.selectedGroceryList?.id
        val recipeName = state.value.recipe.title.takeIf { it.isNotBlank() }
        _state.update { it.copy(isAdding = true) }
        scope.launch {
            if (ingredients.isNotEmpty()) {
                if (selectedListId != null) {
                    groceryRepository.addGroceries(selectedListId, ingredients, recipeName)
                } else {
                    groceryRepository.addGroceries(ingredients)
                }
            }
            _output.send(Output.Added)
        }
    }

    private fun loadGroceryLists() {
        scope.launch {
            groceryRepository.ensureDefaultList()
            groceryRepository.getGroceryLists().collect { lists ->
                val groceryListItems = lists.map { GroceryListItem(id = it.id, name = it.name) }
                val lastSelectedId = settings.getLongOrNull(KEY_LAST_GROCERY_LIST_ID)
                val selected =
                    groceryListItems.firstOrNull { it.id == lastSelectedId }
                        ?: groceryListItems.firstOrNull()
                _state.update {
                    it.copy(
                        groceryLists = groceryListItems,
                        selectedGroceryList = it.selectedGroceryList ?: selected,
                    )
                }
            }
        }
    }

    private fun loadRecipe() {
        scope.launch {
            val recipe =
                recipeRepository.getRecipe(recipeId).first()
                    ?: run {
                        _output.send(Output.Finished)
                        return@launch
                    }
            val lines =
                recipe.ingredients.withoutLineBreakTags().split("\n").filter {
                    it.isNotBlank() && !IngredientSection.isHeader(it)
                }
            _state.update { it.copy(isLoading = false, recipe = recipe, ingredientLines = lines) }
        }
    }

    /**
     * Builds the category-grouped rows for [lines] at [scale]. Each row's `name` is the scaled line
     * — that's what gets added to the grocery list — and its display name and quantity come from
     * parsing that same scaled text, so the sheet and the resulting grocery item agree.
     */
    private fun groupIngredients(
        lines: List<String>,
        deselectedIds: Set<Int>,
        scale: Double,
    ): List<IngredientGroup> =
        lines
            .mapIndexed { index, raw ->
                val scaled = IngredientScaler.scale(raw, scale)
                val parsed = IngredientParser.parse(scaled)
                ListItem(
                    id = index,
                    name = scaled,
                    displayName = parsed.name,
                    quantity = parsed.quantity,
                    isSelected = index !in deselectedIds,
                ) to parsed.category
            }
            .groupBy { it.second }
            .entries
            .sortedBy { it.key.ordinal }
            .map { (category, pairs) ->
                IngredientGroup(
                    category = category,
                    items = pairs.map { it.first }.toImmutableList(),
                )
            }

    data class State(
        val isLoading: Boolean = true,
        val isAdding: Boolean = false,
        val recipe: Recipe = Recipe.Empty,
        /** The recipe's ingredient lines, headers and blanks dropped, at the author's amounts. */
        val ingredientLines: List<String> = emptyList(),
        /** Ids (indices into [ingredientLines]) the user unchecked; everything else is selected. */
        val deselectedIngredientIds: Set<Int> = emptySet(),
        /** Derived from [ingredientLines] and [ingredientScale] — never set directly. */
        val groupedIngredients: List<IngredientGroup> = emptyList(),
        val ingredientScale: Double = DEFAULT_INGREDIENT_SCALE,
        val groceryLists: List<GroceryListItem> = emptyList(),
        val selectedGroceryList: GroceryListItem? = null,
    )

    sealed class Output {
        data object Finished : Output()

        data object Added : Output()
    }

    @AssistedFactory
    fun interface Factory {
        fun create(recipeId: Long): AddRecipeToGroceryListViewModel
    }

    companion object {
        private const val KEY_LAST_GROCERY_LIST_ID = "last_grocery_list_id"
    }
}

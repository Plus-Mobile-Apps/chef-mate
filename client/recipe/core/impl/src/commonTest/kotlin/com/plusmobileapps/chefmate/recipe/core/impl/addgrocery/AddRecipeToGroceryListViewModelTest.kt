@file:Suppress("FunctionName")
@file:OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.recipe.core.impl.addgrocery

import com.plusmobileapps.chefmate.grocery.data.GroceryListModel
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryRepository
import com.plusmobileapps.chefmate.recipe.core.addgrocery.AddRecipeToGroceryListBloc.GroceryListItem
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.testing.FakeIngredientScalePreferences
import com.plusmobileapps.chefmate.recipe.data.testing.FakeRecipeRepository
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class AddRecipeToGroceryListViewModelTest {

    private val recipes = MutableStateFlow<List<Recipe>>(emptyList())
    private val recipeRepository = FakeRecipeRepository(recipes)
    private val groceryRepository = FakeGroceryRepository()
    private val scalePreferences = FakeIngredientScalePreferences()

    private fun recipe(ingredients: String): Recipe =
        Recipe(
            id = 1L,
            title = "Soup",
            description = null,
            ingredients = ingredients,
            directions = "cook",
            imageUrl = null,
            sourceUrl = null,
            servings = 2,
            prepTime = 5,
            cookTime = 10,
            totalTime = 15,
            calories = 100,
            starRating = null,
            isFavorite = false,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2024-01-02T00:00:00Z"),
        )

    private fun createViewModel(recipe: Recipe): AddRecipeToGroceryListViewModel {
        recipes.value = listOf(recipe)
        return AddRecipeToGroceryListViewModel(
            recipeId = recipe.id,
            mainContext = UnconfinedTestDispatcher(),
            recipeRepository = recipeRepository,
            groceryRepository = groceryRepository,
            scalePreferences = scalePreferences,
            settings = MapSettings(),
        )
    }

    @Test
    fun When_recipe_has_duplicate_ingredient_lines_Then_each_item_has_a_unique_id() {
        // Two identical lines previously collided on raw.hashCode(), producing a duplicate
        // LazyColumn key and crashing the screen.
        val viewModel = createViewModel(recipe(ingredients = "Salt\nSalt"))

        val items = viewModel.state.value.groupedIngredients.flatMap { it.items }

        items.size shouldBe 2
        items.map { it.id }.toSet().size shouldBe 2
    }

    @Test
    fun When_one_of_two_duplicate_ingredients_toggled_Then_only_that_item_changes() {
        // Sharing an id between duplicates would have toggled both rows at once.
        val viewModel = createViewModel(recipe(ingredients = "Salt\nSalt"))
        val items = viewModel.state.value.groupedIngredients.flatMap { it.items }

        viewModel.toggleIngredient(items.first().id)

        val updated = viewModel.state.value.groupedIngredients.flatMap { it.items }
        updated.map { it.isSelected } shouldContainExactly listOf(false, true)
    }

    @Test
    fun When_the_recipe_already_has_a_persisted_scale_Then_the_sheet_opens_at_that_scale() {
        // Whatever factor was chosen on recipe detail (or in Cook Mode) is what this sheet shows.
        scalePreferences.setScale(recipeId = 1L, scale = 2.0)

        val viewModel = createViewModel(recipe(ingredients = "200g spaghetti\n2 large eggs"))

        val state = viewModel.state.value
        state.ingredientScale shouldBe 2.0
        // Rows come out grouped by category — eggs (dairy) sort ahead of spaghetti (grains).
        state.groupedIngredients.flatMap { it.items }.map { it.name } shouldContainExactly
            listOf("4 large eggs", "400g spaghetti")
    }

    @Test
    fun When_the_scale_changes_Then_the_displayed_amounts_scale_with_it() {
        val viewModel = createViewModel(recipe(ingredients = "200g spaghetti"))

        viewModel.setScale(0.5)

        val item = viewModel.state.value.groupedIngredients.flatMap { it.items }.single()
        item.quantity shouldBe "100g"
        item.displayName shouldBe "spaghetti"
    }

    @Test
    fun When_the_scale_changes_Then_the_chosen_ingredients_stay_chosen() {
        // Selection is tracked by id rather than baked into the rows, so re-deriving the rows at a
        // new scale must not silently re-check what the user unchecked.
        val viewModel = createViewModel(recipe(ingredients = "200g spaghetti\n2 large eggs"))
        val first = viewModel.state.value.groupedIngredients.flatMap { it.items }.first()
        viewModel.toggleIngredient(first.id)

        viewModel.setScale(2.0)

        val items = viewModel.state.value.groupedIngredients.flatMap { it.items }
        items.single { it.id == first.id }.isSelected shouldBe false
        items.filter { it.id != first.id }.map { it.isSelected } shouldContainExactly listOf(true)
    }

    @Test
    fun When_lists_include_family_and_shared_lists_Then_they_carry_that_through() {
        groceryRepository.setGroceryLists(
            listOf(
                GroceryListModel(id = 1L, name = "Mine"),
                GroceryListModel(id = 2L, name = "Ours", familyId = "f1", familyName = "Smiths"),
                GroceryListModel(id = 3L, name = "Theirs", isShared = true),
            ),
        )

        val viewModel = createViewModel(recipe(ingredients = "Salt"))

        viewModel.state.value.groceryLists shouldContainExactly
            listOf(
                GroceryListItem(id = 1L, name = "Mine"),
                GroceryListItem(id = 2L, name = "Ours", familyId = "f1", familyName = "Smiths"),
                GroceryListItem(id = 3L, name = "Theirs", isShared = true),
            )
    }

    @Test
    fun When_saving_at_a_scale_Then_the_scaled_amounts_are_added_to_the_list() = runTest {
        val viewModel = createViewModel(recipe(ingredients = "200g spaghetti\n2 large eggs"))
        viewModel.setScale(2.0)

        viewModel.save()

        val added = groceryRepository.getGroceries().first()
        added.map { it.name } shouldContainExactly listOf("4 large eggs", "400g spaghetti")
        added.map { it.quantity } shouldContainExactly listOf("4 large", "400g")
    }
}

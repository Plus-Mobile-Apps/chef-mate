@file:OptIn(ExperimentalTime::class, ExperimentalCoilApi::class)

package com.plusmobileapps.chefmate.ui.screenshot

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.plusmobileapps.chefmate.cook.CookModeBloc
import com.plusmobileapps.chefmate.cook.impl.ui.previewCookBlocSplit
import com.plusmobileapps.chefmate.cook.ui.CookModeScreen
import com.plusmobileapps.chefmate.grocery.core.impl.list.ui.previewGroceryListBlocFamily
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListBloc.GroceryGroup
import com.plusmobileapps.chefmate.grocery.core.list.GroceryListScreen
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.SyncStatus as GrocerySyncStatus
import com.plusmobileapps.chefmate.meal.core.MealPlanBloc
import com.plusmobileapps.chefmate.meal.core.MealPlanScreen
import com.plusmobileapps.chefmate.meal.core.impl.ui.previewMealPlanBlocFamilyPlan
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.meal.data.SyncStatus as MealSyncStatus
import com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailBloc
import com.plusmobileapps.chefmate.recipe.core.detail.RecipeDetailScreen
import com.plusmobileapps.chefmate.recipe.core.detail.previewRecipeDetailBloc
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.SyncStatus as RecipeSyncStatus
import com.plusmobileapps.chefmate.recipe.list.RecipeListBloc
import com.plusmobileapps.chefmate.recipe.list.RecipeListItem
import com.plusmobileapps.chefmate.recipe.list.RecipeListScreen
import com.plusmobileapps.chefmate.recipe.list.impl.ui.previewRecipeListBloc
import com.plusmobileapps.chefmate.text.FixedString
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

// Real recipes from the ChefMate account, so the onboarding captures look like the app in use
// rather than placeholder data. Andrew's Guacamole is Andrew's own recipe and photo; the slaw and
// farro salad were imported from other sites, so only their titles and stats appear here (with
// descriptions in our own words and no photo).

private const val GUACAMOLE_IMAGE_URL = "capture://andrews_guacamole"

/** Bundled photo for each capture-only image URL, under this module's `res/drawable-nodpi`. */
private val captureImageResources =
    mapOf(GUACAMOLE_IMAGE_URL to R.drawable.capture_andrews_guacamole)

/**
 * Provides a Coil preview handler that serves bundled photos for the capture image URLs: the
 * screenshot renderer has no network, so recipe photos in the capture data load from resources.
 */
@Composable
internal fun ProvideCaptureImages(content: @Composable () -> Unit) {
    val resources = LocalContext.current.resources
    val handler =
        remember(resources) {
            AsyncImagePreviewHandler { request ->
                captureImageResources[request.data]?.let { id ->
                    BitmapFactory.decodeResource(resources, id)?.asImage()
                }
            }
        }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides handler, content = content)
}

private val guacamole =
    Recipe(
        id = 1L,
        title = "Andrew's Guacamole",
        description =
            "A creamy, fresh guacamole with a spicy kick from serrano peppers, brightened with " +
                "lime and cilantro. Ripe avocados are mashed and mixed with minced garlic, finely " +
                "chopped onion, diced roma tomato, and fresh cilantro, then seasoned with lime " +
                "juice and salt to balance the heat and richness. Simple, vibrant, and perfect " +
                "for dipping or topping your favorite dishes.",
        ingredients =
            """
            2 serrano peppers
            4 avocados
            2-3 garlic cloves minced
            2 ½ tsp of cilantro chopped
            1/4 yellow onion finely chopped
            1 roma tomato, diced
            1 1/2 limes
            salt
            """
                .trimIndent(),
        directions =
            """
            Deseed and mash the avocados with a fork until only slightly chunky. Keep the seeds for later to put in the guac if you would like to keep it fresher than a day.
            Throw in garlic, cilantro, onion, tomato, and lime.
            With the serrano pepper, if you do not want a spicy guac cut pepper in half and use a spoon to deseed/remove the rind. Then for more flavor, cover in oil and roast in a pan over med/high heat until you get a nice char. Mince and add to the guac.
            Begin to mix everything together and salt to taste.
            Enjoy!
            """
                .trimIndent(),
        imageUrl = GUACAMOLE_IMAGE_URL,
        sourceUrl = null,
        servings = 8,
        prepTime = 20,
        cookTime = 0,
        totalTime = 20,
        calories = 200,
        starRating = 5,
        categories =
            setOf(
                Category(id = 5L, name = "Side", builtinId = BuiltinCategory.SIDE.id),
                Category(id = 7L, name = "Snack", builtinId = BuiltinCategory.SNACK.id),
            ),
        syncStatus = RecipeSyncStatus.SYNCED,
        createdAt = Instant.parse("2026-07-18T17:35:58Z"),
        updatedAt = Instant.parse("2026-07-22T22:51:54Z"),
    )

private val recipeListItems =
    persistentListOf(
        RecipeListItem(
            id = 1L,
            title = guacamole.title,
            description = guacamole.description,
            imageUrl = GUACAMOLE_IMAGE_URL,
            starRating = 5,
            totalTime = 20,
            formattedTotalTime = FixedString("20 min"),
            servings = 8,
            calories = 200,
            isFavorite = false,
            syncStatus = RecipeSyncStatus.SYNCED,
        ),
        RecipeListItem(
            id = 2L,
            title = "Farro Salad",
            description = "Chewy farro, crisp apple, arugula, and walnuts in a tangy vinaigrette.",
            imageUrl = null,
            starRating = 5,
            totalTime = 30,
            formattedTotalTime = FixedString("30 min"),
            servings = 4,
            calories = null,
            isFavorite = false,
            syncStatus = RecipeSyncStatus.SYNCED,
        ),
        RecipeListItem(
            id = 3L,
            title = "Simple Healthy Slaw",
            description = "Two kinds of cabbage and toasted seeds with a bright lemon dressing.",
            imageUrl = null,
            starRating = null,
            totalTime = 20,
            formattedTotalTime = FixedString("20 min"),
            servings = 4,
            calories = 214,
            isFavorite = false,
            syncStatus = RecipeSyncStatus.SYNCED,
        ),
    )

/** Recipe detail for Andrew's Guacamole. */
internal val captureRecipeDetailBloc: RecipeDetailBloc =
    object : RecipeDetailBloc by previewRecipeDetailBloc {
        override val state =
            MutableStateFlow(
                previewRecipeDetailBloc.state.value.copy(
                    recipe = guacamole,
                    createdAt = FixedString("July 18, 2026"),
                    updatedAt = FixedString("July 22, 2026"),
                )
            )

        @Composable override fun Content(modifier: Modifier) = RecipeDetailScreen(this, modifier)
    }

/** The recipe list showing the real recipes. */
internal val captureRecipeListBloc: RecipeListBloc =
    object : RecipeListBloc by previewRecipeListBloc {
        override val state =
            MutableStateFlow(
                previewRecipeListBloc.state.value.copy(
                    recipes = recipeListItems,
                    totalRecipeCount = recipeListItems.size,
                )
            )

        @Composable override fun Content(modifier: Modifier) = RecipeListScreen(this, modifier)
    }

/** Cook mode on Andrew's Guacamole. */
internal val captureCookModeBloc: CookModeBloc =
    object : CookModeBloc by previewCookBlocSplit {
        override val state =
            MutableStateFlow(
                previewCookBlocSplit.state.value.copy(
                    activeRecipe = guacamole,
                    activeSessions =
                        persistentListOf(
                            CookModeBloc.Model.Chip(guacamole.id, guacamole.title, isActive = true)
                        ),
                )
            )

        @Composable override fun Content(modifier: Modifier) = CookModeScreen(this, modifier)
    }

private fun meal(id: Long, recipe: RecipeListItem, date: String): MealPlanItem =
    MealPlanItem(
        id = id,
        recipeId = recipe.id,
        recipeTitle = recipe.title,
        recipeImageUrl = recipe.imageUrl,
        date = date,
        mealType = MealType.DINNER,
        familyId = "f1",
        syncStatus = MealSyncStatus.SYNCED,
    )

/** A week of dinners on the family's shared meal plan. */
internal val captureMealPlanBloc: MealPlanBloc =
    object : MealPlanBloc by previewMealPlanBlocFamilyPlan {
        override val state =
            MutableStateFlow(
                previewMealPlanBlocFamilyPlan.state.value.copy(
                    viewMode = MealPlanBloc.ViewMode.WEEK,
                    dateLabel = FixedString("Oct 18 – Oct 24, 2026"),
                    weekMeals =
                        persistentListOf(
                            MealPlanBloc.DayGroup(
                                dateLabel = FixedString("Sun, Oct 18"),
                                meals =
                                    persistentListOf(meal(1L, recipeListItems[0], "2026-10-18")),
                            ),
                            MealPlanBloc.DayGroup(
                                dateLabel = FixedString("Mon, Oct 19"),
                                meals =
                                    persistentListOf(meal(2L, recipeListItems[2], "2026-10-19")),
                            ),
                            MealPlanBloc.DayGroup(
                                dateLabel = FixedString("Wed, Oct 21"),
                                meals =
                                    persistentListOf(meal(3L, recipeListItems[1], "2026-10-21")),
                            ),
                        ),
                )
            )

        @Composable override fun Content(modifier: Modifier) = MealPlanScreen(this, modifier)
    }

private fun groceryItem(
    id: Long,
    name: String,
    quantity: String,
    category: GroceryCategory,
    recipeName: String,
    isChecked: Boolean = false,
) =
    GroceryItem(
        id = id,
        name = name,
        quantity = quantity,
        category = category,
        isChecked = isChecked,
        syncStatus = GrocerySyncStatus.SYNCED,
        recipeName = recipeName,
    )

/** The family's "Weekly shop" list, filled from the week's recipes. */
internal val captureGroceryListBloc: GroceryListBloc =
    object : GroceryListBloc by previewGroceryListBlocFamily {
        override val state =
            MutableStateFlow(
                previewGroceryListBlocFamily.state.value.copy(
                    groupedItems =
                        persistentListOf(
                            GroceryGroup(
                                category = GroceryCategory.PRODUCE,
                                items =
                                    persistentListOf(
                                        groceryItem(
                                            1L,
                                            "Avocados",
                                            "4",
                                            GroceryCategory.PRODUCE,
                                            guacamole.title,
                                        ),
                                        groceryItem(
                                            2L,
                                            "Serrano peppers",
                                            "2",
                                            GroceryCategory.PRODUCE,
                                            guacamole.title,
                                            isChecked = true,
                                        ),
                                        groceryItem(
                                            3L,
                                            "Honeycrisp apple",
                                            "1",
                                            GroceryCategory.PRODUCE,
                                            "Farro Salad",
                                        ),
                                    ),
                            )
                        )
                )
            )

        @Composable
        override fun Content(modifier: Modifier) =
            GroceryListScreen(bloc = this, modifier = modifier)
    }

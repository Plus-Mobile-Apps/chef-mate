package com.plusmobileapps.chefmate.recipe.list.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.recipe.data.SyncStatus
import com.plusmobileapps.chefmate.recipe.list.RecipeBookPickerContent
import com.plusmobileapps.chefmate.recipe.list.RecipeFilterOption
import com.plusmobileapps.chefmate.recipe.list.RecipeListBloc
import com.plusmobileapps.chefmate.recipe.list.RecipeListItem
import com.plusmobileapps.chefmate.recipe.list.RecipeListScreen
import com.plusmobileapps.chefmate.recipe.list.RecipeSortOption
import com.plusmobileapps.chefmate.recipebook.data.RecipeBook
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookInvite
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRole
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

private val sampleRecipes =
    persistentListOf(
        RecipeListItem(
            id = 1L,
            title = "Pasta Carbonara",
            description = "A **classic Roman** pasta with eggs, cheese, and _cured pork_.",
            imageUrl = null,
            starRating = 5,
            totalTime = 25,
            formattedTotalTime = FixedString("25 min"),
            servings = 2,
            calories = 620,
            isFavorite = true,
            syncStatus = SyncStatus.SYNCED,
        ),
        RecipeListItem(
            id = 2L,
            title = "Caesar Salad",
            description = "Crisp romaine, parmesan, and a creamy anchovy dressing.",
            imageUrl = null,
            starRating = 4,
            totalTime = 15,
            formattedTotalTime = FixedString("15 min"),
            servings = 4,
            calories = 320,
            isFavorite = false,
            syncStatus = SyncStatus.SYNCED,
        ),
        RecipeListItem(
            id = 3L,
            title = "Sourdough Pancakes",
            description = "Tangy, fluffy pancakes that use up your starter discard.",
            imageUrl = null,
            starRating = 3,
            totalTime = 30,
            formattedTotalTime = FixedString("30 min"),
            servings = 4,
            calories = 280,
            isFavorite = false,
            syncStatus = SyncStatus.NOT_SYNCED,
        ),
    )

private fun recipeListBloc(model: RecipeListBloc.Model): RecipeListBloc =
    object : RecipeListBloc {
        override val state = MutableStateFlow(model)

        override fun onRecipeClicked(recipe: RecipeListItem) = Unit

        override fun onAddRecipeClicked() = Unit

        override fun onScanRecipePhotoPicked(bytes: ByteArray, fileExtension: String) = Unit

        override fun onScanErrorDismissed() = Unit

        override fun onDeleteRecipe(recipe: RecipeListItem) = Unit

        override fun onToggleFavorite(recipe: RecipeListItem) = Unit

        override fun onSortOptionSelected(option: RecipeSortOption) = Unit

        override fun onFilterToggled(filter: RecipeFilterOption) = Unit

        override fun onToggleViewMode() = Unit

        override fun onCoachMarkDismissed(id: String) = Unit

        override fun onSearchQueryChanged(query: String) = Unit

        override fun onClearFilters() = Unit

        override fun onApplySortAndFilters(
            sort: RecipeSortOption,
            filters: Set<RecipeFilterOption>,
            categories: Set<BuiltinCategory>,
            userCategoryIds: Set<Long>,
        ) = Unit

        override fun onBrowseRecipesClicked() = Unit

        override fun onSyncClicked() = Unit

        override fun onContinueCookingClicked() = Unit

        override fun onDoneCookingClicked() = Unit

        override fun onDoneCookingConfirmed() = Unit

        override fun onDoneCookingDismissed() = Unit

        override fun onEnterSelectionMode() = Unit

        override fun onExitSelectionMode() = Unit

        override fun onToggleRecipeSelected(recipe: RecipeListItem) = Unit

        override fun onRecipeLongClicked(recipe: RecipeListItem) = Unit

        override fun onToggleSelectAllVisible() = Unit

        override fun onAddToBookClicked() = Unit

        override fun onBulkBookPickerDismissed() = Unit

        override fun onAddSelectedToBook(bookId: Long) = Unit

        override fun onAddToCategoryClicked() = Unit

        override fun onBulkCategoryPickerDismissed() = Unit

        override fun onAddSelectedToBuiltinCategory(category: BuiltinCategory) = Unit

        override fun onAddSelectedToUserCategory(categoryId: Long) = Unit

        override fun onExportClicked() = Unit

        override fun onExportFinished() = Unit

        override fun onBookSelectorClicked() = Unit

        override fun onBookPickerDismissed() = Unit

        override fun onBookSelected(bookId: Long) = Unit

        override fun onAllRecipesSelected() = Unit

        override fun onCreateBookClicked() = Unit

        override fun onEditBookClicked(bookId: Long) = Unit

        override fun onCollaborateClicked() = Unit

        override fun onAcceptInvite(memberId: String) = Unit

        override fun onDeclineInvite(memberId: String) = Unit
    }

val previewRecipeListBloc: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            recipeBooks = RecipeBook.Samples,
            activeBook = RecipeBook.Sample,
        )
    )

/** "All recipes" cross-book view active — the selector reads "All recipes" with no single book. */
val previewRecipeListBlocAllRecipes: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            recipeBooks = RecipeBook.Samples,
            activeBook = null,
            isAllRecipesSelected = true,
        )
    )

/**
 * Search inside a single book returned nothing — exercises the empty state's "Search all recipes"
 * broaden action (offered only when a single book is active).
 */
val previewRecipeListBlocSearchEmpty: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = persistentListOf(),
            totalRecipeCount = sampleRecipes.size,
            recipeBooks = RecipeBook.Samples,
            activeBook = RecipeBook.Sample,
            searchQuery = "tonkotsu",
            isSearchActive = true,
        )
    )

/**
 * Recipe list with an active cooking session — exercises the Continue/Done Cooking FAB stack, which
 * previously double-applied safe-area insets and rendered off the bottom edge of its container (see
 * fix for issue #150).
 */
val previewRecipeListBlocCooking: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            cookingRecipeCount = 2,
        )
    )

/** Recipe list with one active category filter — exercises the filter-icon badge indicator. */
val previewRecipeListBlocCategoryFiltered: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            activeCategories = setOf(BuiltinCategory.BREAKFAST),
        )
    )

/**
 * Recipe list with a quick filter and a category already applied — the fixture for the
 * expanded-width filter rail, which highlights active options in place rather than behind a sheet.
 */
val previewRecipeListBlocFilterSidebar: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            activeFilters = setOf(RecipeFilterOption.FAVORITES),
            activeCategories = setOf(BuiltinCategory.DINNER),
            availableUserCategories =
                persistentListOf(
                    Category(id = 42L, name = "Weeknight"),
                    Category(id = 43L, name = "Meal Prep"),
                ),
        )
    )

/** Recipe list in multi-select mode with two recipes picked — exercises the selection top bar. */
val previewRecipeListBlocSelectionMode: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            isSelectionMode = true,
            selectedRecipeIds = setOf(1L, 3L),
        )
    )

/** Multi-select mode with no recipes picked — locks in the disabled export icon state. */
val previewRecipeListBlocSelectionModeEmpty: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            isSelectionMode = true,
            selectedRecipeIds = emptySet(),
        )
    )

/** Photo scan in progress — shows the non-cancellable scanning dialog over the list. */
val previewRecipeListBlocScanning: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            isScanning = true,
        )
    )

/** Recipe list with a pending recipe-book invite — exercises the accept/decline invite banner. */
val previewRecipeListBlocPendingInvite: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            recipeBooks = RecipeBook.Samples,
            activeBook = RecipeBook.Sample,
            pendingInvites =
                listOf(
                    RecipeBookInvite(
                        memberId = "invite-1",
                        bookName = "Weeknight Dinners",
                        role = RecipeBookRole.EDITOR,
                    )
                ),
        )
    )

/** Photo scan failed — shows the scan-error dialog. */
val previewRecipeListBlocScanError: RecipeListBloc =
    recipeListBloc(
        RecipeListBloc.Model(
            recipes = sampleRecipes,
            totalRecipeCount = sampleRecipes.size,
            scanError =
                FixedString(
                    "We couldn’t read a recipe from that photo. Try a clearer shot of the full " +
                        "recipe."
                ),
        )
    )

/**
 * Books for the picker previews: the user's own books plus two shared with them by collaborators,
 * so the "Shared with you" section renders.
 */
val previewRecipeBooksWithShared: List<RecipeBook> =
    RecipeBook.Samples +
        listOf(
            RecipeBook.Sample.copy(
                id = 4L,
                name = "Grandma’s Classics",
                isDefault = false,
                isOwnedByCurrentUser = false,
            ),
            RecipeBook.Sample.copy(
                id = 5L,
                name = "Book Club Bakes",
                isDefault = false,
                isOwnedByCurrentUser = false,
            ),
        )

/** [previewRecipeBooksWithShared] plus two books shared with the user's family. */
val previewRecipeBooksWithFamily: List<RecipeBook> =
    previewRecipeBooksWithShared +
        listOf(
            RecipeBook.Sample.copy(
                id = 6L,
                name = "Sunday Dinners",
                isDefault = false,
                isOwnedByCurrentUser = false,
                familyId = "family-1",
                familyName = "The Smiths",
            ),
            RecipeBook.Sample.copy(
                id = 7L,
                name = "Holiday Baking",
                isDefault = false,
                isOwnedByCurrentUser = false,
                familyId = "family-1",
                familyName = "The Smiths",
            ),
        )

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun RecipeListPreview() {
    ChefMateTheme { RecipeListScreen(bloc = previewRecipeListBloc) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun RecipeListCookingPreview() {
    ChefMateTheme { RecipeListScreen(bloc = previewRecipeListBlocCooking) }
}

@Preview(showBackground = true, heightDp = 1100, widthDp = 800)
@Composable
internal fun RecipeListCookingTabletPreview() {
    ChefMateTheme { RecipeListScreen(bloc = previewRecipeListBlocCooking) }
}

/** Expanded window — the filter rail sits to the left of the list. */
@Preview(showBackground = true, heightDp = 900, widthDp = 1000)
@Composable
internal fun RecipeListFilterSidebarPreview() {
    ChefMateTheme { RecipeListScreen(bloc = previewRecipeListBlocFilterSidebar) }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
internal fun RecipeListSelectionPreview() {
    ChefMateTheme { RecipeListScreen(bloc = previewRecipeListBlocSelectionMode) }
}

@Preview(showBackground = true, heightDp = 700)
@Composable
internal fun RecipeBookPickerPreview() {
    ChefMateTheme {
        RecipeBookPickerContent(
            books = previewRecipeBooksWithShared,
            activeBookId = RecipeBook.Sample.id,
            isAllRecipesSelected = false,
            onBookSelected = {},
            onAllRecipesSelected = {},
            onEditBook = {},
            onCreateBook = {},
        )
    }
}

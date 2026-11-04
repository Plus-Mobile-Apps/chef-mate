package com.plusmobileapps.chefmate.recipe.list

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData
import com.plusmobileapps.chefmate.recipebook.data.RecipeBook
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookInvite
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

interface RecipeListBloc : ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        RecipeListScreen(bloc = this, modifier = modifier)
    }

    fun onRecipeClicked(recipe: RecipeListItem)

    fun onAddRecipeClicked()

    /**
     * The user picked a photo to scan a recipe from. [fileExtension] is the picked file's extension
     * (e.g. `jpg`); the bloc derives the Gemini mime type from it and reuses the bytes as the new
     * recipe's image. While extraction runs [Model.isScanning] is true; on success the bloc emits
     * [Output.OpenScannedRecipe], on failure it surfaces [Model.scanError].
     */
    fun onScanRecipePhotoPicked(bytes: ByteArray, fileExtension: String)

    /** Dismisses the scan-failed message. */
    fun onScanErrorDismissed()

    fun onDeleteRecipe(recipe: RecipeListItem)

    fun onToggleFavorite(recipe: RecipeListItem)

    fun onSortOptionSelected(option: RecipeSortOption)

    fun onFilterToggled(filter: RecipeFilterOption)

    fun onToggleViewMode()

    fun onSearchQueryChanged(query: String)

    fun onClearFilters()

    fun onApplySortAndFilters(
        sort: RecipeSortOption,
        filters: Set<RecipeFilterOption>,
        categories: Set<BuiltinCategory>,
        userCategoryIds: Set<Long>,
    )

    fun onBrowseRecipesClicked()

    fun onSyncClicked()

    fun onContinueCookingClicked()

    fun onDoneCookingClicked()

    fun onDoneCookingConfirmed()

    fun onDoneCookingDismissed()

    fun onEnterSelectionMode()

    fun onExitSelectionMode()

    fun onToggleRecipeSelected(recipe: RecipeListItem)

    /**
     * Long-press on a recipe row. Outside selection mode this enters it with [recipe] pre-selected
     * (the standard mobile "long-press to multi-select" gesture); inside selection mode it toggles
     * the row like a tap.
     */
    fun onRecipeLongClicked(recipe: RecipeListItem)

    fun onToggleSelectAllVisible()

    /** Opens the picker to file the selected recipes under a recipe book. */
    fun onAddToBookClicked()

    fun onBulkBookPickerDismissed()

    /** Files every selected recipe under book [bookId], then leaves selection mode. */
    fun onAddSelectedToBook(bookId: Long)

    /** Opens the picker to tag the selected recipes with a category. */
    fun onAddToCategoryClicked()

    fun onBulkCategoryPickerDismissed()

    /** Tags every selected recipe with the built-in [category], then leaves selection mode. */
    fun onAddSelectedToBuiltinCategory(category: BuiltinCategory)

    /**
     * Tags every selected recipe with the user category [categoryId], then leaves selection mode.
     */
    fun onAddSelectedToUserCategory(categoryId: Long)

    fun onExportClicked()

    /**
     * Called by the parent navigator after the exporter signals a successful save. Drops the screen
     * out of multi-select mode so the user lands back on a clean list. A *cancelled* trip to the
     * exporter (back/dismiss with nothing exported) does **not** call this — the selection is
     * preserved so the user can try again.
     */
    fun onExportFinished()

    fun onBookSelectorClicked()

    fun onBookPickerDismissed()

    fun onBookSelected(bookId: Long)

    /**
     * Selects the cross-book "All recipes" view so the list and search span every recipe book. Also
     * used by the search-empty state's broaden action.
     */
    fun onAllRecipesSelected()

    fun onCreateBookClicked()

    fun onEditBookClicked(bookId: Long)

    /** Opens the edit screen for the currently active book (the app-bar "Collaborate" action). */
    fun onCollaborateClicked()

    /** Accepts the pending recipe-book invite with remote [memberId]. */
    fun onAcceptInvite(memberId: String)

    /** Declines the pending recipe-book invite with remote [memberId]. */
    fun onDeclineInvite(memberId: String)

    /** Marks the first-run coach mark with [id] as seen so it never shows again. */
    fun onCoachMarkDismissed(id: String)

    data class Model(
        val recipes: ImmutableList<RecipeListItem> = persistentListOf(),
        val totalRecipeCount: Int = 0,
        val isLoading: Boolean = false,
        val isSyncing: Boolean = false,
        val currentSort: RecipeSortOption = RecipeSortOption.RECENTLY_ADDED,
        val activeFilters: Set<RecipeFilterOption> = emptySet(),
        val activeCategories: Set<BuiltinCategory> = emptySet(),
        /** Selected user-category IDs from the filter sheet. Disjoint from [activeCategories]. */
        val activeUserCategoryIds: Set<Long> = emptySet(),
        /** All user-created categories — surfaced so the filter sheet can render them as chips. */
        val availableUserCategories: ImmutableList<Category> = persistentListOf(),
        val isGridView: Boolean = false,
        val searchQuery: String = "",
        val isSearchActive: Boolean = false,
        val cookingRecipeCount: Int = 0,
        val showDoneCookingDialog: Boolean = false,
        val isSelectionMode: Boolean = false,
        val selectedRecipeIds: Set<Long> = emptySet(),
        /** True while the "add selected recipes to a book" picker is open. */
        val isBulkBookPickerOpen: Boolean = false,
        /** True while the "add selected recipes to a category" picker is open. */
        val isBulkCategoryPickerOpen: Boolean = false,
        /** True while a picked photo is being scanned into a recipe via Gemini vision. */
        val isScanning: Boolean = false,
        /** Non-null when the most recent photo scan failed. */
        val scanError: TextData? = null,
        /**
         * When true, the add button opens a chooser offering "Scan from photo"; when false it opens
         * the blank editor directly. Driven by the `scan_recipe_from_photo` feature flag.
         */
        val isScanFromPhotoEnabled: Boolean = false,
        val recipeBooks: List<RecipeBook> = emptyList(),
        val activeBook: RecipeBook? = null,
        /** True when the cross-book "All recipes" view is active (no single book selected). */
        val isAllRecipesSelected: Boolean = false,
        val isBookPickerOpen: Boolean = false,
        /** Pending recipe-book invites addressed to the current user (the list banner). */
        val pendingInvites: List<RecipeBookInvite> = emptyList(),
        /** Id of the first-run coach mark currently allowed to show, or null when none. */
        val activeCoachMark: String? = null,
    ) {
        /** Total number of active filter chips: legacy filters + preset + user category filters. */
        val totalActiveFilterCount: Int
            get() = activeFilters.size + activeCategories.size + activeUserCategoryIds.size
    }

    sealed class Output {
        /**
         * Open a recipe's detail. [title] is what the list already shows, handed on so the detail
         * screen can show it while the full recipe loads.
         */
        data class OpenRecipe(val recipeId: Long, val title: String? = null) : Output()

        object AddNewRecipe : Output()

        /** A recipe was extracted from a scanned photo; open the pre-filled editor. */
        data class OpenScannedRecipe(val extracted: ExtractedRecipeData) : Output()

        object OpenBrowser : Output()

        data class OpenCookMode(val recipeId: Long) : Output()

        /**
         * Open the recipe-exporter screen. [recipeIds] is null when the user picked "Export all
         * recipes" from the overflow menu, and a non-empty set when they launched export from
         * selection mode.
         */
        data class OpenExportRecipes(val recipeIds: Set<Long>?) : Output()

        /** Open the create/edit recipe-book modal. [bookId] is null to create a new book. */
        data class OpenEditRecipeBook(val bookId: Long?) : Output()
    }

    interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): RecipeListBloc
    }
}

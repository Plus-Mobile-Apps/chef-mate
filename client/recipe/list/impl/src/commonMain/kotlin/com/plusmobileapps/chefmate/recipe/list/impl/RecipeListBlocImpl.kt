package com.plusmobileapps.chefmate.recipe.list.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.list.RecipeFilterOption
import com.plusmobileapps.chefmate.recipe.list.RecipeListBloc
import com.plusmobileapps.chefmate.recipe.list.RecipeListBloc.Output
import com.plusmobileapps.chefmate.recipe.list.RecipeListItem
import com.plusmobileapps.chefmate.recipe.list.RecipeSortOption
import com.plusmobileapps.chefmate.toast.ToastService
import com.plusmobileapps.chefmate.util.TimeFormatterUtil
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = RecipeListBloc.Factory::class,
)
class RecipeListBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    private val viewModelFactory: () -> RecipeListViewModel,
    private val timeFormatterUtil: TimeFormatterUtil,
    private val toastService: ToastService,
) : RecipeListBloc, BlocContext by context {
    private val viewModel: RecipeListViewModel = instanceKeeper.getViewModel { viewModelFactory() }
    private val scope = createScope()

    init {
        viewModel.scannedRecipe
            .onEach { output.onNext(Output.OpenScannedRecipe(it)) }
            .launchIn(scope)
        viewModel.bulkActionMessage.onEach { toastService.show(it) }.launchIn(scope)
    }

    override val state: StateFlow<RecipeListBloc.Model> =
        viewModel.state.mapState {
            RecipeListBloc.Model(
                isLoading = it.isLoading,
                isSyncing = it.isSyncing,
                recipes =
                    it.displayRecipes.map { recipe -> recipe.toRecipeListItem() }.toImmutableList(),
                totalRecipeCount = it.recipes.size,
                currentSort = it.currentSort,
                activeFilters = it.activeFilters,
                activeCategories = it.activeCategories,
                activeUserCategoryIds = it.activeUserCategoryIds,
                availableUserCategories = it.availableUserCategories.toImmutableList(),
                isGridView = it.isGridView,
                searchQuery = it.searchQuery,
                isSearchActive = it.isSearchActive,
                cookingRecipeCount = it.cookingRecipeIds.size,
                showDoneCookingDialog = it.showDoneCookingDialog,
                isSelectionMode = it.isSelectionMode,
                selectedRecipeIds = it.selectedRecipeIds,
                isBulkBookPickerOpen = it.isBulkBookPickerOpen,
                isBulkCategoryPickerOpen = it.isBulkCategoryPickerOpen,
                isScanning = it.isScanning,
                scanError = it.scanError,
                isScanFromPhotoEnabled = it.isScanFromPhotoEnabled,
                recipeBooks = it.recipeBooks,
                activeBook = it.activeBook,
                isAllRecipesSelected = it.isAllRecipesSelected,
                isBookPickerOpen = it.isBookPickerOpen,
                pendingInvites = it.pendingInvites,
                activeCoachMark = it.activeCoachMark,
            )
        }

    override fun onRecipeClicked(recipe: RecipeListItem) {
        if (viewModel.state.value.isSelectionMode) {
            viewModel.toggleRecipeSelected(recipe.id)
        } else {
            output.onNext(Output.OpenRecipe(recipe.id))
        }
    }

    override fun onAddRecipeClicked() {
        viewModel.dismissCoachMark(CoachMarkId.RECIPE_LIST_ADD)
        output.onNext(Output.AddNewRecipe)
    }

    override fun onScanRecipePhotoPicked(bytes: ByteArray, fileExtension: String) {
        viewModel.dismissCoachMark(CoachMarkId.RECIPE_LIST_ADD)
        viewModel.scanRecipeFromPhoto(bytes = bytes, fileExtension = fileExtension)
    }

    override fun onScanErrorDismissed() {
        viewModel.dismissScanError()
    }

    override fun onDeleteRecipe(recipe: RecipeListItem) {
        viewModel.deleteRecipe(recipe.id)
    }

    override fun onToggleFavorite(recipe: RecipeListItem) {
        viewModel.toggleFavorite(recipe.id)
    }

    override fun onSortOptionSelected(option: RecipeSortOption) {
        viewModel.updateSort(option)
    }

    override fun onFilterToggled(filter: RecipeFilterOption) {
        viewModel.toggleFilter(filter)
    }

    override fun onToggleViewMode() {
        viewModel.dismissCoachMark(CoachMarkId.RECIPE_LIST_VIEW_MODE)
        viewModel.toggleViewMode()
    }

    override fun onSearchQueryChanged(query: String) {
        viewModel.updateSearchQuery(query)
    }

    override fun onClearFilters() {
        viewModel.clearFilters()
    }

    override fun onApplySortAndFilters(
        sort: RecipeSortOption,
        filters: Set<RecipeFilterOption>,
        categories: Set<BuiltinCategory>,
        userCategoryIds: Set<Long>,
    ) {
        viewModel.applySortAndFilters(sort, filters, categories, userCategoryIds)
    }

    override fun onBrowseRecipesClicked() {
        output.onNext(Output.OpenBrowser)
    }

    override fun onSyncClicked() {
        viewModel.onSyncClicked()
    }

    override fun onContinueCookingClicked() {
        viewModel.state.value.cookingRecipeIds.firstOrNull()?.let { recipeId ->
            output.onNext(Output.OpenCookMode(recipeId))
        }
    }

    override fun onDoneCookingClicked() {
        viewModel.showDoneCookingDialog()
    }

    override fun onDoneCookingConfirmed() {
        viewModel.confirmDoneCooking()
    }

    override fun onDoneCookingDismissed() {
        viewModel.dismissDoneCookingDialog()
    }

    override fun onEnterSelectionMode() {
        viewModel.enterSelectionMode()
    }

    override fun onExitSelectionMode() {
        viewModel.exitSelectionMode()
    }

    override fun onToggleRecipeSelected(recipe: RecipeListItem) {
        viewModel.toggleRecipeSelected(recipe.id)
    }

    override fun onRecipeLongClicked(recipe: RecipeListItem) {
        if (viewModel.state.value.isSelectionMode) {
            viewModel.toggleRecipeSelected(recipe.id)
        } else {
            viewModel.enterSelectionModeWith(recipe.id)
        }
    }

    override fun onToggleSelectAllVisible() {
        viewModel.toggleSelectAllVisible()
    }

    override fun onAddToBookClicked() {
        viewModel.openBulkBookPicker()
    }

    override fun onBulkBookPickerDismissed() {
        viewModel.dismissBulkBookPicker()
    }

    override fun onAddSelectedToBook(bookId: Long) {
        viewModel.addSelectedToBook(bookId)
    }

    override fun onAddToCategoryClicked() {
        viewModel.openBulkCategoryPicker()
    }

    override fun onBulkCategoryPickerDismissed() {
        viewModel.dismissBulkCategoryPicker()
    }

    override fun onAddSelectedToBuiltinCategory(category: BuiltinCategory) {
        viewModel.addSelectedToBuiltinCategory(category)
    }

    override fun onAddSelectedToUserCategory(categoryId: Long) {
        viewModel.addSelectedToUserCategory(categoryId)
    }

    override fun onExportClicked() {
        val state = viewModel.state.value
        val ids = if (state.isSelectionMode) state.selectedRecipeIds else null
        output.onNext(Output.OpenExportRecipes(ids))
        // Selection mode persists across the trip to the exporter so the user can come back to the
        // same picks if they bail out. It only clears via [onExportFinished] when the exporter
        // signals a successful save.
    }

    override fun onExportFinished() {
        viewModel.exitSelectionMode()
    }

    override fun onBookSelectorClicked() {
        viewModel.openBookPicker()
    }

    override fun onBookPickerDismissed() {
        viewModel.dismissBookPicker()
    }

    override fun onBookSelected(bookId: Long) {
        viewModel.selectBook(bookId)
    }

    override fun onAllRecipesSelected() {
        viewModel.selectAllRecipes()
    }

    override fun onCreateBookClicked() {
        viewModel.dismissBookPicker()
        output.onNext(Output.OpenEditRecipeBook(bookId = null))
    }

    override fun onEditBookClicked(bookId: Long) {
        viewModel.dismissBookPicker()
        output.onNext(Output.OpenEditRecipeBook(bookId = bookId))
    }

    override fun onCollaborateClicked() {
        val activeBookId = viewModel.state.value.activeBook?.id
        output.onNext(Output.OpenEditRecipeBook(bookId = activeBookId))
    }

    override fun onAcceptInvite(memberId: String) {
        viewModel.acceptInvite(memberId)
    }

    override fun onDeclineInvite(memberId: String) {
        viewModel.declineInvite(memberId)
    }

    override fun onCoachMarkDismissed(id: String) {
        viewModel.dismissCoachMark(id)
    }

    private fun Recipe.toRecipeListItem(): RecipeListItem =
        RecipeListItem(
            id = id,
            title = title,
            description = description,
            imageUrl = imageUrl,
            starRating = starRating,
            totalTime = totalTime,
            formattedTotalTime = totalTime?.let { timeFormatterUtil.formatMinutes(it) },
            servings = servings,
            calories = calories,
            isFavorite = isFavorite,
            syncStatus = syncStatus,
        )
}

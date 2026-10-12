@file:OptIn(ExperimentalTime::class)

package com.plusmobileapps.chefmate.recipe.core.impl.edit

import co.touchlab.kermit.Logger
import com.plusmobileapps.chefmate.ChefMateUrls
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.combineStates
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.di.MarkdownEditorModeRepository
import com.plusmobileapps.chefmate.recipe.data.BuiltinCategory
import com.plusmobileapps.chefmate.recipe.data.Category
import com.plusmobileapps.chefmate.recipe.data.CategoryRepository
import com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData
import com.plusmobileapps.chefmate.recipe.data.PendingRecipePhotoStore
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.RecipePhotoStorage
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.recipebook.data.RecipeBook
import com.plusmobileapps.chefmate.recipebook.data.RecipeBookRepository
import com.plusmobileapps.chefmate.review.InAppReviewService
import com.plusmobileapps.chefmate.review.ReviewMilestone
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
class EditRecipeViewModel(
    @Assisted private val recipeId: Long?,
    @Assisted extractedRecipe: ExtractedRecipeData?,
    @Assisted fromAi: Boolean,
    @Assisted consumePendingPhoto: Boolean,
    @Main mainContext: CoroutineContext,
    private val repository: RecipeRepository,
    private val categoryRepository: CategoryRepository,
    private val recipeBookRepository: RecipeBookRepository,
    private val photoStorage: RecipePhotoStorage,
    private val pendingRecipePhotoStore: PendingRecipePhotoStore,
    private val markdownEditorModeRepository: MarkdownEditorModeRepository,
    private val coachMarkController: CoachMarkController,
    private val inAppReviewService: InAppReviewService,
) : ViewModel(mainContext) {
    private val _output = Channel<Output>(Channel.BUFFERED)
    val output: Flow<Output> = _output.receiveAsFlow()

    // One-shot recipe links resolved from the picker, handed to the screen to splice into the field
    // that requested them (caret-aware insertion lives in the editor, not here).
    private val _recipeLinkPicked = Channel<RecipeLinkSelection>(Channel.BUFFERED)
    val recipeLinkPicked: Flow<RecipeLinkSelection> = _recipeLinkPicked.receiveAsFlow()

    private val _state = MutableStateFlow(State())

    // Fold the shared controller's active mark into state so the screen shows at most one coach
    // mark at a time across the whole app.
    val state: StateFlow<State> =
        combineStates(_state, coachMarkController.activeCoachMark) { state, activeCoachMark ->
            state.copy(activeCoachMark = activeCoachMark)
        }

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _ingredients = MutableStateFlow("")
    val ingredients: StateFlow<String> = _ingredients.asStateFlow()

    private val _directions = MutableStateFlow("")
    val directions: StateFlow<String> = _directions.asStateFlow()

    private val _imageUrl = MutableStateFlow("")
    val imageUrl: StateFlow<String> = _imageUrl.asStateFlow()

    private val _sourceUrl = MutableStateFlow("")
    val sourceUrl: StateFlow<String> = _sourceUrl.asStateFlow()

    private val _servings = MutableStateFlow("")
    val servings: StateFlow<String> = _servings.asStateFlow()

    private val _prepTime = MutableStateFlow("")
    val prepTime: StateFlow<String> = _prepTime.asStateFlow()

    private val _cookTime = MutableStateFlow("")
    val cookTime: StateFlow<String> = _cookTime.asStateFlow()

    private val _totalTime = MutableStateFlow("")
    val totalTime: StateFlow<String> = _totalTime.asStateFlow()

    private val _calories = MutableStateFlow("")
    val calories: StateFlow<String> = _calories.asStateFlow()

    private val _starRating = MutableStateFlow<Int?>(null)
    val starRating: StateFlow<Int?> = _starRating.asStateFlow()

    private val _categories = MutableStateFlow<Set<Category>>(emptySet())
    val categories: StateFlow<Set<Category>> = _categories.asStateFlow()

    val availableUserCategories: StateFlow<List<Category>> =
        categoryRepository
            .observeUserCategories()
            .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    val recipeBooks: StateFlow<List<RecipeBook>> =
        recipeBookRepository
            .getRecipeBooks()
            .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    private val _selectedBookIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedBookIds: StateFlow<Set<Long>> = _selectedBookIds.asStateFlow()

    // Re-entrancy guard for [createUserCategoryAndAttach]; not exposed to the bloc because the
    // picker UI doesn't need a loading flag (offline-first inserts are effectively instant, and
    // routing a fast true→false transition through StateFlow could be conflated and leave the
    // UI stuck on a spinner).
    private val _isCreatingCategory = MutableStateFlow(false)

    private val _pendingPhotoBytes = MutableStateFlow<ByteArray?>(null)
    val pendingPhotoBytes: StateFlow<ByteArray?> = _pendingPhotoBytes.asStateFlow()
    private var pendingPhotoExtension: String? = null

    /** App-wide editor-mode preference, backed by [MarkdownEditorModeRepository]. */
    val richTextEditorMode: StateFlow<Boolean> = markdownEditorModeRepository.richTextMode

    init {
        // Queue every edit-recipe coach mark in order; the controller shows them one at a time.
        CoachMarkId.editRecipeSequence.forEach(coachMarkController::request)
        // A new recipe defaults to the active book; an existing one loads its current membership.
        if (recipeId == null) {
            scope.launch {
                val activeBook =
                    recipeBookRepository.activeBookId.value
                        ?: recipeBookRepository.getDefaultBookId()
                _selectedBookIds.update { it.ifEmpty { setOf(activeBook) } }
            }
        }
        when {
            recipeId != null -> {
                _state.update { it.copy(isLoading = false) }
                scope.launch { loadRecipe(recipeId) }
            }
            extractedRecipe != null -> {
                _title.value = extractedRecipe.title
                _description.value = extractedRecipe.description.orEmpty()
                _ingredients.value = extractedRecipe.ingredients.joinToString("\n")
                _directions.value = extractedRecipe.directions.joinToString("\n")
                _imageUrl.value = extractedRecipe.imageUrl.orEmpty()
                _sourceUrl.value = extractedRecipe.sourceUrl
                _servings.value = extractedRecipe.servings?.toString().orEmpty()
                _prepTime.value = extractedRecipe.prepTime?.toString().orEmpty()
                _cookTime.value = extractedRecipe.cookTime?.toString().orEmpty()
                _totalTime.value = extractedRecipe.totalTime?.toString().orEmpty()
                _calories.value = extractedRecipe.calories?.toString().orEmpty()
                if (fromAi) attachBuiltin(BuiltinCategory.AI)
                // The photo picked for extraction is handed off out-of-band (it can't ride in the
                // serializable nav config); pull it in as the pending recipe image.
                if (consumePendingPhoto) {
                    pendingRecipePhotoStore.consume()?.let {
                        setPendingPhoto(bytes = it.bytes, fileExtension = it.fileExtension)
                    }
                }
            }
        }
    }

    fun updateTitle(value: String) {
        _title.value = value
    }

    fun updateDescription(value: String) {
        _description.value = value
    }

    fun updateIngredients(value: String) {
        _ingredients.value = value
    }

    fun updateDirections(value: String) {
        _directions.value = value
    }

    /**
     * Resolve a recipe chosen in the link picker to a `[Title](chefmate://recipe/<clientId>)` link
     * and emit it for the screen to splice into the targeted field. A recipe with no [clientId] yet
     * (an older row awaiting sync backfill) can't be linked stably, so it is skipped.
     */
    fun onRecipeLinkPicked(recipeId: Long) {
        scope.launch {
            val recipe = repository.getRecipe(recipeId).first() ?: return@launch
            val clientId = recipe.clientId ?: return@launch
            _recipeLinkPicked.send(
                RecipeLinkSelection(label = recipe.title, url = ChefMateUrls.recipeLink(clientId))
            )
        }
    }

    fun updateImageUrl(value: String) {
        _imageUrl.value = value
    }

    fun updateSourceUrl(value: String) {
        _sourceUrl.value = value
    }

    fun updateServings(value: String) {
        _servings.value = value
    }

    fun updatePrepTime(value: String) {
        _prepTime.value = value
    }

    fun updateCookTime(value: String) {
        _cookTime.value = value
    }

    fun updateTotalTime(value: String) {
        _totalTime.value = value
    }

    fun updateCalories(value: String) {
        _calories.value = value
    }

    fun updateRichTextEditorMode(value: Boolean) {
        markdownEditorModeRepository.setRichTextMode(value)
    }

    /**
     * Mark a coach mark seen, but only if it's the one currently showing, so tapping a control
     * dismisses its own tip without consuming tips further down the queue.
     */
    fun dismissCoachMark(id: String) {
        if (coachMarkController.activeCoachMark.value == id) {
            coachMarkController.dismiss(id)
        }
    }

    fun updateStarRating(value: Int?) {
        _starRating.value = value
    }

    fun updateCategories(value: Set<Category>) {
        _categories.value = value
    }

    fun toggleBook(bookId: Long) {
        _selectedBookIds.update { current ->
            if (bookId in current) current - bookId else current + bookId
        }
    }

    fun attachBuiltin(builtin: BuiltinCategory) {
        scope.launch {
            val materialized = categoryRepository.materializeBuiltin(builtin)
            _categories.update { it.upsertById(materialized) }
        }
    }

    fun attachCategory(category: Category) {
        _categories.update { it.upsertById(category) }
    }

    fun detachCategory(category: Category) {
        _categories.update { current -> current.filterNot { it.id == category.id }.toSet() }
    }

    /** Renames a user-created category in place. The local DB write is offline-first. */
    fun renameUserCategory(id: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        scope.launch {
            val renamed = categoryRepository.renameCategory(id, trimmed)
            _categories.update { current ->
                if (current.any { it.id == id }) current.upsertById(renamed) else current
            }
        }
    }

    /**
     * Deletes a user-created category. If the category was attached to this recipe, it's removed
     * from the in-memory selection immediately so the chip disappears on save. Other recipes that
     * referenced it lose their attachment via Supabase's ON DELETE CASCADE on recipe_categories.
     */
    fun deleteUserCategory(id: Long) {
        scope.launch {
            _categories.update { current -> current.filterNot { it.id == id }.toSet() }
            categoryRepository.deleteCategory(id)
        }
    }

    /**
     * Creates a new user category and immediately attaches it to the recipe being edited. The local
     * DB insert is synchronous (offline-first); the remote push is fire-and-forget inside the repo,
     * so the new row appears in [availableUserCategories] right away and the picker dismisses its
     * inline create field immediately on submit.
     */
    fun createUserCategoryAndAttach(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank() || _isCreatingCategory.value) return
        scope.launch {
            _isCreatingCategory.value = true
            try {
                val created = categoryRepository.createUserCategory(trimmed)
                _categories.update { it.upsertById(created) }
            } finally {
                _isCreatingCategory.value = false
            }
        }
    }

    /**
     * Adds [category] to the set, replacing any existing entry with the same id. Set's structural
     * equality treats two Category snapshots with different `syncStatus` as distinct, which would
     * let the same row appear twice when the background remote push mutates the underlying DB row
     * between two attaches.
     */
    private fun Set<Category>.upsertById(category: Category): Set<Category> =
        filterNot { it.id == category.id }.toSet() + category

    fun tryToClose() {
        val originalRecipe = _state.value.recipe
        val currentRecipe = currentRecipe()
        when {
            shouldShowDiscardChangesDialog(originalRecipe, currentRecipe) -> {
                _state.update { it.copy(showDiscardChangesDialog = true) }
            }
            else -> {
                scope.launch { _output.send(Output.Cancelled) }
            }
        }
    }

    fun dismissDiscardChangesDialog() {
        _state.update { it.copy(showDiscardChangesDialog = false) }
    }

    fun setPendingPhoto(bytes: ByteArray, fileExtension: String) {
        _pendingPhotoBytes.value = bytes
        pendingPhotoExtension = fileExtension
        _state.update { it.copy(uploadError = null) }
    }

    fun dismissUploadError() {
        _state.update { it.copy(uploadError = null) }
    }

    fun save() {
        if (_state.value.isSaving) return
        _state.update { it.copy(isSaving = true, uploadError = null) }
        scope.launch {
            val pendingBytes = _pendingPhotoBytes.value
            val pendingExt = pendingPhotoExtension
            if (pendingBytes != null && pendingExt != null) {
                try {
                    val url =
                        photoStorage.uploadPhoto(bytes = pendingBytes, fileExtension = pendingExt)
                    _imageUrl.value = url
                    _pendingPhotoBytes.value = null
                    pendingPhotoExtension = null
                } catch (t: Throwable) {
                    Logger.e(throwable = t, tag = "EditRecipeViewModel") {
                        "Failed to upload photo"
                    }
                    _state.update { it.copy(isSaving = false, uploadError = t) }
                    return@launch
                }
            }
            val originalRecipe = _state.value.recipe
            val currentRecipe = currentRecipe()
            val savedRecipe =
                if (originalRecipe != null) {
                    repository.updateRecipe(currentRecipe)
                } else {
                    repository.createRecipe(currentRecipe).also {
                        inAppReviewService.onMilestone(ReviewMilestone.RecipeSaved)
                    }
                }
            _output.send(Output.Finished(savedRecipe.id))
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Leaving the screen without dismissing frees the queue so other coach marks can show.
        CoachMarkId.editRecipeSequence.forEach(coachMarkController::release)
        _output.close()
        _recipeLinkPicked.close()
    }

    private suspend fun loadRecipe(id: Long) {
        val recipe = repository.getRecipe(id).first()
        if (recipe == null) {
            _output.send(Output.Cancelled)
            return
        }
        _title.value = recipe.title
        _description.value = recipe.description.orEmpty()
        _ingredients.value = recipe.ingredients
        _directions.value = recipe.directions
        _imageUrl.value = recipe.imageUrl.orEmpty()
        _sourceUrl.value = recipe.sourceUrl.orEmpty()
        _servings.value = recipe.servings?.toString().orEmpty()
        _prepTime.value = recipe.prepTime?.toString().orEmpty()
        _cookTime.value = recipe.cookTime?.toString().orEmpty()
        _totalTime.value = recipe.totalTime?.toString().orEmpty()
        _calories.value = recipe.calories?.toString().orEmpty()
        _starRating.value = recipe.starRating
        _categories.value = recipe.categories
        _selectedBookIds.value = recipe.recipeBookIds
        _state.update { it.copy(isLoading = false, recipe = recipe) }
    }

    private fun shouldShowDiscardChangesDialog(
        originalRecipe: Recipe?,
        currentRecipe: Recipe,
    ): Boolean {
        if (_pendingPhotoBytes.value != null) return true
        return when {
            originalRecipe != null -> originalRecipe.isDirty()
            else ->
                currentRecipe.title.isNotBlank() ||
                    currentRecipe.description?.isNotBlank() == true ||
                    currentRecipe.ingredients.isNotBlank() ||
                    currentRecipe.directions.isNotBlank() ||
                    currentRecipe.imageUrl?.isNotBlank() == true ||
                    currentRecipe.sourceUrl?.isNotBlank() == true ||
                    currentRecipe.servings != null ||
                    currentRecipe.prepTime != null ||
                    currentRecipe.cookTime != null ||
                    currentRecipe.totalTime != null ||
                    currentRecipe.calories != null ||
                    currentRecipe.starRating != null ||
                    currentRecipe.categories.isNotEmpty()
        }
    }

    private fun Recipe.isDirty(): Boolean =
        title != _title.value ||
            description.orEmpty() != _description.value ||
            ingredients != _ingredients.value ||
            directions != _directions.value ||
            imageUrl.orEmpty() != _imageUrl.value ||
            sourceUrl.orEmpty() != _sourceUrl.value ||
            servings?.toString().orEmpty() != _servings.value ||
            prepTime?.toString().orEmpty() != _prepTime.value ||
            cookTime?.toString().orEmpty() != _cookTime.value ||
            totalTime?.toString().orEmpty() != _totalTime.value ||
            calories?.toString().orEmpty() != _calories.value ||
            starRating != _starRating.value ||
            categories != _categories.value ||
            recipeBookIds != _selectedBookIds.value

    private fun currentRecipe(): Recipe =
        Recipe(
            id = recipeId ?: -1,
            title = _title.value,
            description = _description.value.ifBlank { null },
            ingredients = _ingredients.value,
            directions = _directions.value,
            imageUrl = _imageUrl.value.ifBlank { null },
            sourceUrl = _sourceUrl.value.ifBlank { null },
            servings = _servings.value.toIntOrNull(),
            prepTime = _prepTime.value.toIntOrNull(),
            cookTime = _cookTime.value.toIntOrNull(),
            totalTime = _totalTime.value.toIntOrNull(),
            calories = _calories.value.toIntOrNull(),
            starRating = _starRating.value,
            isFavorite = state.value.recipe?.isFavorite ?: false,
            categories = _categories.value,
            recipeBookIds = _selectedBookIds.value,
            createdAt = Instant.DISTANT_PAST,
            updatedAt = Instant.DISTANT_PAST,
        )

    data class State(
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val showDiscardChangesDialog: Boolean = false,
        val recipe: Recipe? = null,
        val uploadError: Throwable? = null,
        val activeCoachMark: String? = null,
    )

    sealed class Output {
        data object Cancelled : Output()

        data class Finished(val recipeId: Long) : Output()
    }

    /** A resolved recipe link (`label` = recipe title, `url` = `chefmate://recipe/<clientId>`). */
    data class RecipeLinkSelection(val label: String, val url: String)

    @AssistedFactory
    fun interface Factory {
        fun create(
            recipeId: Long?,
            extractedRecipe: ExtractedRecipeData?,
            fromAi: Boolean,
            consumePendingPhoto: Boolean,
        ): EditRecipeViewModel
    }
}

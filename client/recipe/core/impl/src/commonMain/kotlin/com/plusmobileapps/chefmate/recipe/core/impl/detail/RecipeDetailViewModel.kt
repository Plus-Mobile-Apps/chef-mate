package com.plusmobileapps.chefmate.recipe.core.impl.detail

import chefmate.client.recipe.core.public.generated.resources.Res
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_linked_recipe_not_found
import chefmate.client.recipe.core.public.generated.resources.recipe_detail_share_sign_in_required
import com.plusmobileapps.chefmate.ChefMateUrls
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.combineStates
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.featureflag.FeatureFlags
import com.plusmobileapps.chefmate.featureflag.isEnabled
import com.plusmobileapps.chefmate.recipe.data.DEFAULT_INGREDIENT_SCALE
import com.plusmobileapps.chefmate.recipe.data.IngredientScalePreferences
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.subscription.SubscriptionRepository
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.toast.ToastService
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
class RecipeDetailViewModel(
    @Assisted private val recipeId: Long,
    @Main mainContext: CoroutineContext,
    private val repository: RecipeRepository,
    private val coachMarkController: CoachMarkController,
    private val toastService: ToastService,
    private val scalePreferences: IngredientScalePreferences,
    featureFlags: FeatureFlags,
    subscriptionRepository: SubscriptionRepository,
) : ViewModel(mainContext) {

    private val _output = Channel<Output>(Channel.BUFFERED)
    val output: Flow<Output> = _output.receiveAsFlow()

    private val _shareLink = Channel<String>(Channel.BUFFERED)
    val shareLink: Flow<String> = _shareLink.receiveAsFlow()

    private val _state = MutableStateFlow(State())

    private val showAiChat = featureFlags.isEnabled(FeatureFlagRegistry.AiChat)

    private val isSubscribed = subscriptionRepository.isSubscribed

    // Surface the shared controller's active mark so the screen can show one coach mark at a time;
    // only ids in CoachMarkId.recipeDetailSequence are anchored on this screen. The persisted
    // ingredient scale for this recipe is folded in so the picker reflects (and shares) whatever
    // factor was last chosen — including one set over in Cook Mode.
    val state: StateFlow<State> =
        combineStates(
            combineStates(
                combineStates(
                    combineStates(_state, coachMarkController.activeCoachMark) {
                        state,
                        activeCoachMark ->
                        state.copy(activeCoachMark = activeCoachMark)
                    },
                    showAiChat,
                ) { state, showChat ->
                    state.copy(showAiChat = showChat)
                },
                isSubscribed,
            ) { state, subscribed ->
                state.copy(isSubscribed = subscribed)
            },
            scalePreferences.scaleFor(recipeId),
        ) { state, scale ->
            state.copy(ingredientScale = scale)
        }

    init {
        // Queue every recipe-detail coach mark in order; the controller shows them one at a time.
        CoachMarkId.recipeDetailSequence.forEach(coachMarkController::request)
        scope.launch { observeRecipe() }
    }

    private suspend fun observeRecipe() {
        repository.getRecipe(recipeId).collect { recipe ->
            _state.update { it.copy(isLoading = false, recipe = recipe ?: Recipe.Empty) }
        }
    }

    fun showDeleteConfirmationDialog() {
        _state.update { it.copy(showDeleteConfirmationDialog = true) }
    }

    fun dismissDeleteConfirmationDialog() {
        _state.update { it.copy(showDeleteConfirmationDialog = false) }
    }

    fun confirmDelete() {
        _state.update { it.copy(showDeleteConfirmationDialog = false, isDeleting = true) }
        scope.launch {
            repository.deleteRecipe(recipeId)
            _output.send(Output.RecipeDeleted)
        }
    }

    fun toggleFavorite() {
        scope.launch {
            val recipe = repository.getRecipe(recipeId).first() ?: return@launch
            repository.updateRecipe(recipe.copy(isFavorite = !recipe.isFavorite))
        }
    }

    fun onShareLinkClicked() {
        val recipe = _state.value.recipe
        val remoteId = recipe.remoteId
        // Already shared: skip the confirmation and share the existing link straight away.
        if (recipe.isPublic && remoteId != null) {
            scope.launch { _shareLink.send(ChefMateUrls.recipeShareUrl(remoteId)) }
        } else {
            _state.update { it.copy(showShareConfirmation = true) }
        }
    }

    fun onShareConfirmed() {
        _state.update { it.copy(showShareConfirmation = false) }
        scope.launch {
            val remoteId = repository.setRecipePublic(recipeId, isPublic = true)
            if (remoteId != null) {
                _shareLink.send(ChefMateUrls.recipeShareUrl(remoteId))
            } else {
                // setRecipePublic returns null when the recipe can't be synced — the common cause
                // is being signed out, since a share link needs a remote (global) recipe id.
                toastService.show(ResourceString(Res.string.recipe_detail_share_sign_in_required))
            }
        }
    }

    fun onShareDismissed() {
        _state.update { it.copy(showShareConfirmation = false) }
    }

    fun onStopSharing() {
        scope.launch { repository.setRecipePublic(recipeId, isPublic = false) }
    }

    /**
     * Persist the chosen ingredient [scale] for this recipe; the state flow reflects it in turn.
     */
    fun setScale(scale: Double) {
        scalePreferences.setScale(recipeId, scale)
    }

    /**
     * Resolve a tapped recipe-to-recipe link's [clientId] to a locally-stored recipe and open it. A
     * miss (the target isn't in this device's collection) surfaces a toast instead of navigating.
     */
    fun onRecipeLinkClicked(clientId: String) {
        scope.launch {
            val target = repository.getRecipeByClientId(clientId)
            if (target != null) {
                _output.send(Output.OpenRecipe(target.id))
            } else {
                toastService.show(ResourceString(Res.string.recipe_detail_linked_recipe_not_found))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Leaving the screen without dismissing frees the queue so other coach marks can show.
        CoachMarkId.recipeDetailSequence.forEach(coachMarkController::release)
        _output.close()
        _shareLink.close()
    }

    /**
     * Gate for the AI chat button. Returns true when the caller should emit the open-chat output;
     * otherwise it has raised the premium upsell and the caller should do nothing.
     *
     * Read from [state] rather than `_state` — the entitlement is folded in downstream by
     * [combineStates], so `_state` never carries it.
     */
    fun requestAiChat(): Boolean {
        if (state.value.isSubscribed) return true
        _state.update { it.copy(showPremiumRequiredDialog = true) }
        return false
    }

    fun dismissPremiumRequiredDialog() {
        _state.update { it.copy(showPremiumRequiredDialog = false) }
    }

    /**
     * Mark a coach mark seen, but only if it's the one currently showing. This lets a button's tap
     * dismiss its own tip while it's visible, without prematurely consuming tips further down the
     * queue when their button is tapped early.
     */
    fun dismissCoachMark(id: String) {
        if (coachMarkController.activeCoachMark.value == id) {
            coachMarkController.dismiss(id)
        }
    }

    data class State(
        val isLoading: Boolean = true,
        val isDeleting: Boolean = false,
        val showDeleteConfirmationDialog: Boolean = false,
        val showShareConfirmation: Boolean = false,
        val recipe: Recipe = Recipe.Empty,
        val ingredientScale: Double = DEFAULT_INGREDIENT_SCALE,
        val activeCoachMark: String? = null,
        val showAiChat: Boolean = false,
        val isSubscribed: Boolean = false,
        val showPremiumRequiredDialog: Boolean = false,
    )

    sealed class Output {
        data object RecipeDeleted : Output()

        /** A tapped recipe link resolved to the local recipe with this id. */
        data class OpenRecipe(val recipeId: Long) : Output()
    }

    @AssistedFactory
    fun interface Factory {
        fun create(recipeId: Long): RecipeDetailViewModel
    }
}

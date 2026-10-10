package com.plusmobileapps.chefmate.cook.impl

import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.combineStates
import com.plusmobileapps.chefmate.cook.CookModeBloc
import com.plusmobileapps.chefmate.cook.data.CookingSessionRepository
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.di.KeepScreenOnRepository
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.featureflag.FeatureFlags
import com.plusmobileapps.chefmate.featureflag.isEnabled
import com.plusmobileapps.chefmate.recipe.data.DEFAULT_INGREDIENT_SCALE
import com.plusmobileapps.chefmate.recipe.data.IngredientScalePreferences
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.review.InAppReviewService
import com.plusmobileapps.chefmate.review.ReviewMilestone
import com.plusmobileapps.chefmate.subscription.SubscriptionRepository
import com.russhwolf.settings.Settings
import com.russhwolf.settings.boolean
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@AssistedInject
class CookModeViewModel(
    @Assisted private val initialRecipeId: Long,
    @Main mainContext: CoroutineContext,
    private val recipeRepository: RecipeRepository,
    private val sessionRepository: CookingSessionRepository,
    settings: Settings,
    private val scalePreferences: IngredientScalePreferences,
    private val keepScreenOnRepository: KeepScreenOnRepository,
    private val coachMarkController: CoachMarkController,
    featureFlags: FeatureFlags,
    subscriptionRepository: SubscriptionRepository,
    private val inAppReviewService: InAppReviewService,
) : ViewModel(mainContext) {

    private val showAiChat = featureFlags.isEnabled(FeatureFlagRegistry.AiChat)

    private val isSubscribed = subscriptionRepository.isSubscribed

    // Split view is the default; users can toggle to the scrolling (Stacked) list, which persists.
    private var splitLayoutPref by settings.boolean(KEY_LAYOUT_SPLIT, defaultValue = true)

    private val _state =
        MutableStateFlow(
            State(
                layoutMode =
                    if (splitLayoutPref) CookModeBloc.LayoutMode.Split
                    else CookModeBloc.LayoutMode.Stacked,
                keepScreenOn = keepScreenOnRepository.keepScreenOn.value,
            )
        )

    // Fold the shared controller's active mark into state so the screen shows at most one coach
    // mark at a time across the whole app.
    val state: StateFlow<State> =
        combineStates(
            combineStates(
                combineStates(_state, coachMarkController.activeCoachMark) { state, activeCoachMark
                    ->
                    state.copy(activeCoachMark = activeCoachMark)
                },
                showAiChat,
            ) { state, showChat ->
                state.copy(showAiChat = showChat)
            },
            isSubscribed,
        ) { state, subscribed ->
            state.copy(isSubscribed = subscribed)
        }

    init {
        // Queue every cook-mode coach mark in order; the controller shows them one at a time.
        CoachMarkId.cookModeSequence.forEach(coachMarkController::request)
        scope.launch {
            sessionRepository.start(initialRecipeId)
            sessionRepository.markSelected(initialRecipeId)
        }
        scope.launch {
            combine(sessionRepository.observeRecipeIds(), recipeRepository.getRecipes()) {
                    ids,
                    allRecipes ->
                    val byId = allRecipes.associateBy { it.id }
                    ids.mapNotNull { byId[it] }
                }
                .collect { recipes ->
                    if (recipes.size >= 2) {
                        inAppReviewService.onMilestone(ReviewMilestone.MultipleRecipesInCookMode)
                    }
                    // Recipes arrive ordered by lastSelectedAt DESC, so the first is active.
                    _state.update { current ->
                        current.copy(
                            isLoading = false,
                            cookingRecipes = recipes,
                            activeRecipeId = recipes.firstOrNull()?.id,
                        )
                    }
                }
        }
        scope.launch {
            keepScreenOnRepository.keepScreenOn.collect { keepScreenOn ->
                _state.update { it.copy(keepScreenOn = keepScreenOn) }
            }
        }
        // Track the persisted scale of whichever recipe is active, switching the source flow as the
        // user changes recipes so each shows its own saved factor (and one set here persists back).
        scope.launch {
            _state
                .map { it.activeRecipeId }
                .distinctUntilChanged()
                .flatMapLatest { id ->
                    if (id == null) MutableStateFlow(DEFAULT_INGREDIENT_SCALE)
                    else scalePreferences.scaleFor(id)
                }
                .collect { scale -> _state.update { it.copy(ingredientScale = scale) } }
        }
    }

    fun selectRecipe(recipeId: Long) {
        scope.launch { sessionRepository.markSelected(recipeId) }
    }

    /** Persist the chosen ingredient [scale] for the active recipe; the state flow reflects it. */
    fun setScale(scale: Double) {
        val recipeId = _state.value.activeRecipeId ?: return
        scalePreferences.setScale(recipeId, scale)
    }

    /**
     * Clears every cooking session, then invokes [onDone] so the caller can finish the screen. The
     * callback runs only after the clear completes so it isn't cancelled by the screen tearing
     * down.
     */
    fun finishCooking(onDone: () -> Unit) {
        scope.launch {
            sessionRepository.stopAll()
            onDone()
        }
    }

    fun toggleLayout() {
        val next =
            if (_state.value.layoutMode == CookModeBloc.LayoutMode.Stacked) {
                CookModeBloc.LayoutMode.Split
            } else {
                CookModeBloc.LayoutMode.Stacked
            }
        splitLayoutPref = next == CookModeBloc.LayoutMode.Split
        _state.update { it.copy(layoutMode = next) }
    }

    fun toggleKeepScreenOn() {
        keepScreenOnRepository.toggle()
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

    override fun onCleared() {
        super.onCleared()
        // Leaving the screen without dismissing frees the queue so other coach marks can show.
        CoachMarkId.cookModeSequence.forEach(coachMarkController::release)
    }

    data class State(
        val isLoading: Boolean = true,
        val cookingRecipes: List<Recipe> = emptyList(),
        val activeRecipeId: Long? = null,
        val ingredientScale: Double = DEFAULT_INGREDIENT_SCALE,
        val layoutMode: CookModeBloc.LayoutMode = CookModeBloc.LayoutMode.Split,
        val keepScreenOn: Boolean = true,
        val activeCoachMark: String? = null,
        val showAiChat: Boolean = false,
        val isSubscribed: Boolean = false,
        val showPremiumRequiredDialog: Boolean = false,
    )

    @AssistedFactory
    fun interface Factory {
        fun create(initialRecipeId: Long): CookModeViewModel
    }

    private companion object {
        const val KEY_LAYOUT_SPLIT = "cook_mode_layout_split"
    }
}

package com.plusmobileapps.chefmate.cook

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.cook.ui.CookModeScreen
import com.plusmobileapps.chefmate.recipe.data.DEFAULT_INGREDIENT_SCALE
import com.plusmobileapps.chefmate.recipe.data.Recipe
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

interface CookModeBloc : BackClickBloc, ComposeScreen {
    val state: StateFlow<Model>

    /**
     * What's Cooking sub-bloc — kept alive for the lifetime of Cook Mode so the inline mobile sheet
     * and the tablet modal share its state (selection, select-mode toggle, etc.).
     */
    val whatsCookingBloc: WhatsCookingBloc

    fun onCloseClicked()

    /** Clears every recipe from cook mode and finishes the session. */
    fun onFinishClicked()

    fun onRecipeChipClicked(recipeId: Long)

    /**
     * Pick a new ingredient scale factor (e.g. `2.0` for 2×) for the active recipe. Persisted per
     * recipe so it carries over from recipe detail and survives leaving Cook Mode.
     */
    fun onScaleChanged(scale: Double)

    /**
     * Open the AI chat grounded in the active recipe. Only surfaced when [Model.showAiChat]. For a
     * non-subscriber this opens the premium upsell instead — see [Model.isSubscribed].
     */
    fun onAiChatClicked()

    /** Close Cook Mode and open the active recipe's detail screen in its place. */
    fun onViewRecipeClicked()

    /** Dismiss the "ChefMate Premium required" dialog. */
    fun onPremiumRequiredDismissed()

    fun onLayoutToggled()

    fun onKeepScreenOnToggled()

    /** Marks the first-run coach mark with [id] as seen so it never shows again. */
    fun onCoachMarkDismissed(id: String)

    @Composable
    override fun Content(modifier: Modifier) {
        CookModeScreen(bloc = this, modifier = modifier)
    }

    enum class LayoutMode {
        Stacked,
        Split,
    }

    data class Model(
        val isLoading: Boolean = true,
        val activeRecipe: Recipe? = null,
        /**
         * Scale factor applied to the active recipe's ingredient amounts (1× = author's amounts).
         */
        val ingredientScale: Double = DEFAULT_INGREDIENT_SCALE,
        val activeSessions: ImmutableList<Chip> = persistentListOf(),
        val layoutMode: LayoutMode = LayoutMode.Split,
        val keepScreenOn: Boolean = true,
        /** Id of the first-run coach mark currently allowed to show, or null when none. */
        val activeCoachMark: String? = null,
        /** Gated on the AI Chat feature flag — hides the chat button entirely when off. */
        val showAiChat: Boolean = false,
        /**
         * ChefMate Premium entitlement. Unlike [showAiChat] this doesn't hide the button: a
         * non-subscriber still sees it, marked as premium, and tapping it opens the upsell.
         */
        val isSubscribed: Boolean = false,
        /** True while the premium upsell dialog is shown. */
        val showPremiumRequiredDialog: Boolean = false,
    ) {
        data class Chip(val recipeId: Long, val title: String, val isActive: Boolean)
    }

    sealed class Output {
        data object Finished : Output()

        /** Open the AI chat grounded in the recipe with this id. */
        data class OpenAiChat(val recipeId: Long) : Output()

        /** Close Cook Mode and show the detail screen for the recipe with this id. */
        data class OpenRecipeDetail(val recipeId: Long) : Output()
    }

    fun interface Factory {
        fun create(
            context: BlocContext,
            initialRecipeId: Long,
            output: Consumer<Output>,
        ): CookModeBloc
    }
}

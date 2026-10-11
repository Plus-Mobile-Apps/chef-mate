package com.plusmobileapps.chefmate.review.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.review.InAppReviewService
import com.plusmobileapps.chefmate.review.ReviewMilestone
import com.russhwolf.settings.Settings
import com.russhwolf.settings.boolean
import com.russhwolf.settings.int
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Prompts once the user has saved [RECIPES_SAVED_THRESHOLD] new recipes or finished a cook-mode
 * session, whichever comes first, and never again. The OS quotas its own sheet, but a persisted
 * flag keeps us from re-asking every launch — and on Windows, where the prompt is our own dialog,
 * it is the only thing stopping a nag.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class InAppReviewServiceImpl(settings: Settings) : InAppReviewService {

    private var hasPrompted by settings.boolean(KEY_HAS_PROMPTED, defaultValue = false)

    private var recipesSaved by settings.int(KEY_RECIPES_SAVED, defaultValue = 0)

    private val _isPromptRequested = MutableStateFlow(false)
    override val isPromptRequested: StateFlow<Boolean> = _isPromptRequested.asStateFlow()

    override fun onMilestone(milestone: ReviewMilestone) {
        if (hasPrompted) return
        val isDue =
            when (milestone) {
                ReviewMilestone.RecipeSaved -> ++recipesSaved >= RECIPES_SAVED_THRESHOLD
                ReviewMilestone.CookingFinished -> true
            }
        if (!isDue) return
        hasPrompted = true
        _isPromptRequested.value = true
    }

    override fun onPromptFinished() {
        _isPromptRequested.value = false
    }

    companion object {
        internal const val RECIPES_SAVED_THRESHOLD = 3
        private const val KEY_HAS_PROMPTED = "in_app_review_has_prompted"
        private const val KEY_RECIPES_SAVED = "in_app_review_recipes_saved"
    }
}

package com.plusmobileapps.chefmate.review

import kotlinx.coroutines.flow.StateFlow

/** A moment of user success after which it is a good time to ask for a store review. */
enum class ReviewMilestone {
    /** The user saved a new recipe of their own. Prompts once enough have been saved. */
    RecipeSaved,

    /** The user finished a cook-mode session. */
    CookingFinished,
}

/**
 * App-scoped gate for the in-app store review prompt. BLoCs and ViewModels report
 * [ReviewMilestone]s; the service decides whether one should prompt (at most once per install) and
 * raises [isPromptRequested], which a single [InAppReviewHost] near the app root observes to show
 * the platform's native review flow.
 */
interface InAppReviewService {
    /** True while a prompt is waiting to be shown. Observed by [InAppReviewHost]. */
    val isPromptRequested: StateFlow<Boolean>

    /** Records that the user reached [milestone], requesting a prompt if one is still due. */
    fun onMilestone(milestone: ReviewMilestone)

    /** Called by the host once the platform flow has finished (or was skipped). */
    fun onPromptFinished()
}

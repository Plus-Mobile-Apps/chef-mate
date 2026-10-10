package com.plusmobileapps.chefmate.review.testing

import com.plusmobileapps.chefmate.review.InAppReviewService
import com.plusmobileapps.chefmate.review.ReviewMilestone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * [InAppReviewService] for tests. Records every reported milestone in [milestones] and never
 * requests a prompt, so BLoC/ViewModel tests can assert what was reported without a host.
 */
class FakeInAppReviewService : InAppReviewService {

    override val isPromptRequested: StateFlow<Boolean> = MutableStateFlow(false)

    /** Every milestone passed to [onMilestone], in order. */
    val milestones: List<ReviewMilestone>
        get() = _milestones

    private val _milestones = mutableListOf<ReviewMilestone>()

    override fun onMilestone(milestone: ReviewMilestone) {
        _milestones += milestone
    }

    override fun onPromptFinished() = Unit
}

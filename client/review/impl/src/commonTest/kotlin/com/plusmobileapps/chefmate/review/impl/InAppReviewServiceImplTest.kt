@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.review.impl

import com.plusmobileapps.chefmate.review.ReviewMilestone
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InAppReviewServiceImplTest {

    private val settings = MapSettings()

    @Test
    fun When_created_Then_no_prompt_requested() {
        InAppReviewServiceImpl(settings).isPromptRequested.value shouldBe false
    }

    @Test
    fun When_first_milestone_reached_Then_prompt_requested() {
        val service = InAppReviewServiceImpl(settings)

        service.onMilestone(ReviewMilestone.RecipeSaved)

        service.isPromptRequested.value shouldBe true
    }

    @Test
    fun When_prompt_finished_Then_request_cleared() {
        val service = InAppReviewServiceImpl(settings)
        service.onMilestone(ReviewMilestone.RecipeSaved)

        service.onPromptFinished()

        service.isPromptRequested.value shouldBe false
    }

    @Test
    fun When_second_milestone_reached_after_prompt_Then_not_requested_again() {
        val service = InAppReviewServiceImpl(settings)
        service.onMilestone(ReviewMilestone.RecipeSaved)
        service.onPromptFinished()

        service.onMilestone(ReviewMilestone.MultipleRecipesInCookMode)

        service.isPromptRequested.value shouldBe false
    }

    @Test
    fun When_already_prompted_in_earlier_launch_Then_not_requested_again() {
        InAppReviewServiceImpl(settings).onMilestone(ReviewMilestone.RecipeSaved)
        val relaunched = InAppReviewServiceImpl(settings)

        relaunched.onMilestone(ReviewMilestone.MultipleRecipesInCookMode)

        relaunched.isPromptRequested.value shouldBe false
    }
}

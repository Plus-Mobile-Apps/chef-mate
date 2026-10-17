@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.review.impl

import com.plusmobileapps.chefmate.review.ReviewMilestone
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InAppReviewServiceImplTest {

    private val settings = MapSettings()

    private fun InAppReviewServiceImpl.saveRecipes(count: Int) =
        repeat(count) {
            onMilestone(ReviewMilestone.RecipeSaved)
        }

    @Test
    fun When_created_Then_no_prompt_requested() {
        InAppReviewServiceImpl(settings).isPromptRequested.value shouldBe false
    }

    @Test
    fun When_fewer_than_three_recipes_saved_Then_no_prompt_requested() {
        val service = InAppReviewServiceImpl(settings)

        service.saveRecipes(2)

        service.isPromptRequested.value shouldBe false
    }

    @Test
    fun When_third_recipe_saved_Then_prompt_requested() {
        val service = InAppReviewServiceImpl(settings)

        service.saveRecipes(3)

        service.isPromptRequested.value shouldBe true
    }

    @Test
    fun When_recipes_saved_across_launches_Then_count_carries_over() {
        InAppReviewServiceImpl(settings).saveRecipes(2)
        val relaunched = InAppReviewServiceImpl(settings)

        relaunched.saveRecipes(1)

        relaunched.isPromptRequested.value shouldBe true
    }

    @Test
    fun When_cooking_finished_Then_prompt_requested() {
        val service = InAppReviewServiceImpl(settings)

        service.onMilestone(ReviewMilestone.CookingFinished)

        service.isPromptRequested.value shouldBe true
    }

    @Test
    fun When_prompt_finished_Then_request_cleared() {
        val service = InAppReviewServiceImpl(settings)
        service.onMilestone(ReviewMilestone.CookingFinished)

        service.onPromptFinished()

        service.isPromptRequested.value shouldBe false
    }

    @Test
    fun When_already_prompted_Then_not_requested_again() {
        val service = InAppReviewServiceImpl(settings)
        service.onMilestone(ReviewMilestone.CookingFinished)
        service.onPromptFinished()

        service.saveRecipes(3)

        service.isPromptRequested.value shouldBe false
    }

    @Test
    fun When_already_prompted_in_earlier_launch_Then_not_requested_again() {
        InAppReviewServiceImpl(settings).onMilestone(ReviewMilestone.CookingFinished)
        val relaunched = InAppReviewServiceImpl(settings)

        relaunched.onMilestone(ReviewMilestone.CookingFinished)

        relaunched.isPromptRequested.value shouldBe false
    }
}

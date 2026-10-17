@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.waitUntilDoesNotExist
import com.plusmobileapps.chefmate.aichat.AiChatTestTags
import com.plusmobileapps.chefmate.aichat.robots.aiChat
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.fixtures.TestRecipes
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.recipe.core.robots.recipeDetail
import com.plusmobileapps.chefmate.recipe.list.robots.recipeList
import kotlin.test.Test

/**
 * Opening the AI chat from a recipe reopens that recipe's most recent conversation instead of
 * always starting fresh; the new-conversation button starts over.
 */
@OptIn(ExperimentalTestApi::class)
class AiChatRecipeReopenUiTest {

    @Test
    fun reopening_from_a_recipe_shows_the_prior_conversation() = runRootBlocTest { component ->
        component.testFeatureFlags.set(FeatureFlagRegistry.AiChat, true)
        component.testSubscriptionRepository.setSubscribed(true)
        component.fakeGeminiClient.deltas = listOf("Bake it at 400°F for 25 minutes.")

        recipeList().clickRecipe(TestRecipes.fullyPopulated.title)
        recipeDetail().awaitDisplayed().tapAiChat()

        // Start a conversation for this recipe, then close the chat.
        aiChat().awaitShown().typeMessage("How long do I bake it?").tapSend()
        aiChat().awaitMessageShown("Bake it at 400").close()

        // Reopening from the same recipe reopens that conversation with its messages.
        recipeDetail().awaitDisplayed().tapAiChat()
        aiChat().awaitShown().awaitMessageShown("How long do I bake it?")
    }

    @Test
    fun new_conversation_button_clears_the_reopened_conversation() = runRootBlocTest { component ->
        component.testFeatureFlags.set(FeatureFlagRegistry.AiChat, true)
        component.testSubscriptionRepository.setSubscribed(true)
        component.fakeGeminiClient.deltas = listOf("Sure, here's how.")

        recipeList().clickRecipe(TestRecipes.fullyPopulated.title)
        recipeDetail().awaitDisplayed().tapAiChat()
        aiChat().awaitShown().typeMessage("First question?").tapSend()
        aiChat().awaitMessageShown("Sure, here's how.")

        // Starting a new conversation clears the prior messages.
        aiChat().tapNewChat()
        waitUntilDoesNotExist(
            hasText("First question?", substring = true) and
                hasAnyAncestor(hasTestTag(AiChatTestTags.SCREEN))
        )
    }
}

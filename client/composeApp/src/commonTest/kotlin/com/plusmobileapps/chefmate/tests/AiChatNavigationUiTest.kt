@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import com.plusmobileapps.chefmate.aichat.robots.aiChat
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.recipe.bottomnav.robots.bottomNav
import com.plusmobileapps.chefmate.recipe.core.robots.editRecipe
import com.plusmobileapps.chefmate.recipe.data.ExtractedRecipeData
import com.plusmobileapps.chefmate.settings.robots.more
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AiChatNavigationUiTest {

    private val extractedRecipe =
        ExtractedRecipeData(
            title = "Lemon-Roasted Chicken Thighs",
            description = "From the chat",
            ingredients = listOf("4 chicken thighs", "2 lemons", "1 tbsp olive oil"),
            directions = listOf("Sear skin-side down", "Roast at 425°F for 25 minutes"),
            imageUrl = null,
            sourceUrl = "",
            servings = 4,
            prepTime = 5,
            cookTime = 25,
            totalTime = 30,
            calories = null,
        )

    @Test
    fun add_recipe_pill_opens_edit_form_pre_filled_with_extracted_recipe() =
        runRootBlocTest { component ->
            component.testFeatureFlags.set(FeatureFlagRegistry.AiChat, true)
            component.testSubscriptionRepository.setSubscribed(true)
            component.fakeGeminiClient.deltas = listOf("Try lemon-roasted chicken thighs!")
            component.fakeGeminiRecipeExtractor.response = extractedRecipe

            bottomNav().clickMoreTab()
            more().awaitDisplayed().clickAiChatRow()

            aiChat()
                .typeMessage("What can I do with chicken thighs?")
                .tapSend()
                .awaitMessageShown("Try lemon-roasted chicken thighs!")
                .tapAddRecipe()

            editRecipe().awaitDisplayed().assertTitleShown(extractedRecipe.title)
        }
}

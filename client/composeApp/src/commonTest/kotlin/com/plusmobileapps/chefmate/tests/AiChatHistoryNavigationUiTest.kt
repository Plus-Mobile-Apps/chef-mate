@file:OptIn(ExperimentalTestApi::class)

package com.plusmobileapps.chefmate.tests

import androidx.compose.ui.test.ExperimentalTestApi
import com.plusmobileapps.chefmate.aichat.robots.aiChat
import com.plusmobileapps.chefmate.aichat.robots.aiChatHistory
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.harness.runRootBlocTest
import com.plusmobileapps.chefmate.recipe.bottomnav.robots.bottomNav
import com.plusmobileapps.chefmate.settings.robots.more
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AiChatHistoryNavigationUiTest {

    @Test
    fun history_button_opens_history_screen_with_existing_conversation() =
        runRootBlocTest { component ->
            component.testFeatureFlags.set(FeatureFlagRegistry.AiChat, true)
            component.testSubscriptionRepository.setSubscribed(true)
            component.fakeGeminiClient.deltas = listOf("model reply about chicken")

            bottomNav().clickMoreTab()
            more().awaitDisplayed().clickAiChatRow()

            aiChat()
                .typeMessage("How do I roast a chicken?")
                .tapSend()
                .awaitMessageShown("model reply about chicken")
                .tapHistory()

            aiChatHistory().awaitDisplayed().assertConversationShown("How do I roast a chicken?")
        }
}

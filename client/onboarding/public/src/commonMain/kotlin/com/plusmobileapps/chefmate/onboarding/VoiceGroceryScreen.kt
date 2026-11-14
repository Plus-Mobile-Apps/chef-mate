package com.plusmobileapps.chefmate.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import chefmate.client.onboarding.public.generated.resources.Res
import chefmate.client.onboarding.public.generated.resources.onboarding_next
import chefmate.client.onboarding.public.generated.resources.onboarding_voice_grocery_message_gemini
import chefmate.client.onboarding.public.generated.resources.onboarding_voice_grocery_message_siri
import chefmate.client.onboarding.public.generated.resources.onboarding_voice_grocery_title

@Composable
fun VoiceGroceryScreen(bloc: VoiceGroceryBloc, modifier: Modifier = Modifier) {
    OnboardingIconLayout(
        icon = Icons.Default.Mic,
        title = Res.string.onboarding_voice_grocery_title,
        message =
            when (bloc.assistant) {
                VoiceAssistant.SIRI -> Res.string.onboarding_voice_grocery_message_siri
                VoiceAssistant.GEMINI -> Res.string.onboarding_voice_grocery_message_gemini
            },
        buttonText = Res.string.onboarding_next,
        onButtonClick = bloc::onNextClicked,
        screenTestTag = OnboardingTestTags.VOICE_GROCERY_SCREEN,
        buttonTestTag = OnboardingTestTags.VOICE_GROCERY_NEXT_BUTTON,
        modifier = modifier,
    )
}

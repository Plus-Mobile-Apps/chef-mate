package com.plusmobileapps.chefmate.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.ui.ComposeScreen

/**
 * Onboarding step showing how to add items to the grocery list by voice with the device's
 * [assistant] — Siri on iOS, Gemini on Android. Only shown when a [VoiceAssistantProvider] reports
 * one is available.
 */
interface VoiceGroceryBloc : ComposeScreen {
    val assistant: VoiceAssistant

    fun onNextClicked()

    @Composable
    override fun Content(modifier: Modifier) {
        VoiceGroceryScreen(bloc = this, modifier = modifier)
    }

    sealed class Output {
        /** Advance to the next onboarding step. */
        data object Next : Output()
    }

    fun interface Factory {
        fun create(
            context: BlocContext,
            assistant: VoiceAssistant,
            output: Consumer<Output>,
        ): VoiceGroceryBloc
    }
}

package com.plusmobileapps.chefmate.onboarding.impl

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.onboarding.VoiceAssistant
import com.plusmobileapps.chefmate.onboarding.VoiceAssistantProvider
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

@Inject
@ContributesBinding(AppScope::class)
class PlatformVoiceAssistantProvider : VoiceAssistantProvider {
    override fun voiceAssistant(): VoiceAssistant? = platformVoiceAssistant()
}

/**
 * The voice assistant the running platform can use to add grocery items, matching where the app
 * actually integrates one: the iOS 27 reminders App Schema for Siri and the API 36 AppFunctions
 * service for Gemini. Desktop has no assistant integration.
 */
internal expect fun platformVoiceAssistant(): VoiceAssistant?

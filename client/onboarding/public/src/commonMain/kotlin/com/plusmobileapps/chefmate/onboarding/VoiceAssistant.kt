package com.plusmobileapps.chefmate.onboarding

/** The voice assistant that can add items to a ChefMate grocery list on the running device. */
enum class VoiceAssistant {
    /** Apple Intelligence Siri on iOS 27+, via the reminders App Schema. */
    SIRI,

    /** Gemini on Android 16+, via Android AppFunctions. */
    GEMINI,
}

/**
 * Reports which [VoiceAssistant] the onboarding flow should advertise, or null to leave the voice
 * step out entirely (desktop, older OS versions).
 */
fun interface VoiceAssistantProvider {
    fun voiceAssistant(): VoiceAssistant?
}

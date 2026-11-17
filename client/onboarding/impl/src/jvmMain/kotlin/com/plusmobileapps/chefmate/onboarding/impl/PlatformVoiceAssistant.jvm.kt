package com.plusmobileapps.chefmate.onboarding.impl

import com.plusmobileapps.chefmate.onboarding.VoiceAssistant

// Desktop has no voice assistant integration, so the voice step is skipped.
internal actual fun platformVoiceAssistant(): VoiceAssistant? = null

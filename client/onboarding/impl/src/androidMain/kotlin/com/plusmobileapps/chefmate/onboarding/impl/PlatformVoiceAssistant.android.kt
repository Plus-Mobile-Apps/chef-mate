package com.plusmobileapps.chefmate.onboarding.impl

import android.os.Build
import com.plusmobileapps.chefmate.onboarding.VoiceAssistant

// The Gemini AppFunctions service (BaseChefMateAppFunctionService) requires API 36 (Android 16).
internal actual fun platformVoiceAssistant(): VoiceAssistant? =
    if (Build.VERSION.SDK_INT >= 36) VoiceAssistant.GEMINI else null

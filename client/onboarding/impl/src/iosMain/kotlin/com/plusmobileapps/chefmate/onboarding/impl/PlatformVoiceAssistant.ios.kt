package com.plusmobileapps.chefmate.onboarding.impl

import com.plusmobileapps.chefmate.onboarding.VoiceAssistant
import platform.UIKit.UIDevice

// Only advertise the iOS 27 Apple Intelligence Siri (reminders App Schema), which takes any item
// free-form. Older iOS still has the App Shortcut phrases, but they're too limited to feature here.
internal actual fun platformVoiceAssistant(): VoiceAssistant? {
    val major = UIDevice.currentDevice.systemVersion.substringBefore('.').toIntOrNull() ?: 0
    return if (major >= 27) VoiceAssistant.SIRI else null
}

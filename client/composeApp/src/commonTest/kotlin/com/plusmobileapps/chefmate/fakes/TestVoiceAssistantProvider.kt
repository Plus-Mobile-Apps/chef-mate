package com.plusmobileapps.chefmate.fakes

import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.onboarding.VoiceAssistant
import com.plusmobileapps.chefmate.onboarding.VoiceAssistantProvider
import com.plusmobileapps.chefmate.onboarding.impl.PlatformVoiceAssistantProvider
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Replaces the OS-version check so onboarding flows don't depend on the device/simulator the tests
 * run on. Defaults to no assistant (voice step skipped); set [assistant] before the content is
 * created to include the voice step.
 */
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(scope = AppScope::class, replaces = [PlatformVoiceAssistantProvider::class])
class TestVoiceAssistantProvider : VoiceAssistantProvider {
    var assistant: VoiceAssistant? = null

    override fun voiceAssistant(): VoiceAssistant? = assistant
}

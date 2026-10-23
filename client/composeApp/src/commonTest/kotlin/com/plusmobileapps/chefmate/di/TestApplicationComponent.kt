package com.plusmobileapps.chefmate.di

import com.plusmobileapps.chefmate.ApplicationComponent
import com.plusmobileapps.chefmate.browser.BrowserBookmarkRepository
import com.plusmobileapps.chefmate.fakes.FakeDatabase
import com.plusmobileapps.chefmate.fakes.FakeGeminiClient
import com.plusmobileapps.chefmate.fakes.FakeGeminiRecipeExtractor
import com.plusmobileapps.chefmate.fakes.TestAuthenticationRepository
import com.plusmobileapps.chefmate.fakes.TestFamilyRecipeBookRepository
import com.plusmobileapps.chefmate.fakes.TestFamilyRepository
import com.plusmobileapps.chefmate.fakes.TestFeatureFlags
import com.plusmobileapps.chefmate.fakes.TestSubscriptionRepository
import com.plusmobileapps.chefmate.fakes.TestVoiceAssistantProvider

interface TestApplicationComponent : ApplicationComponent {
    val fakeDatabase: FakeDatabase
    val testAuthenticationRepository: TestAuthenticationRepository
    val fakeGeminiClient: FakeGeminiClient
    val fakeGeminiRecipeExtractor: FakeGeminiRecipeExtractor
    val testFeatureFlags: TestFeatureFlags
    val testFamilyRepository: TestFamilyRepository
    val testFamilyRecipeBookRepository: TestFamilyRecipeBookRepository
    val testSubscriptionRepository: TestSubscriptionRepository
    val browserBookmarkRepository: BrowserBookmarkRepository
    val testVoiceAssistantProvider: TestVoiceAssistantProvider
}

expect fun createTestApplicationComponent(): TestApplicationComponent

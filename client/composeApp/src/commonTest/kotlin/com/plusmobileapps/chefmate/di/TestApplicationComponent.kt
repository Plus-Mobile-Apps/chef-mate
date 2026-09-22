package com.plusmobileapps.chefmate.di

import com.plusmobileapps.chefmate.ApplicationComponent
import com.plusmobileapps.chefmate.fakes.FakeDatabase
import com.plusmobileapps.chefmate.fakes.FakeGeminiClient
import com.plusmobileapps.chefmate.fakes.FakeGeminiRecipeExtractor
import com.plusmobileapps.chefmate.fakes.TestAuthenticationRepository
import com.plusmobileapps.chefmate.fakes.TestFamilyRepository
import com.plusmobileapps.chefmate.fakes.TestFeatureFlags
import com.plusmobileapps.chefmate.fakes.TestSubscriptionRepository

interface TestApplicationComponent : ApplicationComponent {
    val fakeDatabase: FakeDatabase
    val testAuthenticationRepository: TestAuthenticationRepository
    val fakeGeminiClient: FakeGeminiClient
    val fakeGeminiRecipeExtractor: FakeGeminiRecipeExtractor
    val testFeatureFlags: TestFeatureFlags
    val testFamilyRepository: TestFamilyRepository
    val testSubscriptionRepository: TestSubscriptionRepository
}

expect fun createTestApplicationComponent(): TestApplicationComponent

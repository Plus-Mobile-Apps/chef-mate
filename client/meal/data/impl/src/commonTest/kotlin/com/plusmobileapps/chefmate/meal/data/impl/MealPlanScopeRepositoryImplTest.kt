@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.meal.data.impl

import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.data.testing.FakeFamilyRepository
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.featureflag.testing.FakeFeatureFlags
import com.plusmobileapps.chefmate.meal.data.FamilyPlan
import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class MealPlanScopeRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val families = FakeFamilyRepository()
    private val auth = FakeAuthenticationRepository()
    private val flags = FakeFeatureFlags(mapOf(FeatureFlagRegistry.FamilyMealPlans to true))
    private val settings = MapSettings()

    private fun repository() =
        MealPlanScopeRepositoryImpl(
            ioContext = testDispatcher,
            familyRepository = families,
            authRepository = auth,
            featureFlags = flags,
            settings = settings,
        )

    @Test
    fun loads_the_users_families_once_signed_in() =
        runTest(testDispatcher) {
            val familyId = families.seedFamily("Smiths")
            val repo = repository()
            repo.familyPlans.value shouldBe emptyList()

            auth.setAuthenticated()

            repo.familyPlans.value shouldBe listOf(FamilyPlan(familyId, "Smiths"))
            repo.selectedScope.value shouldBe MealPlanScope.Personal
        }

    @Test
    fun remembers_the_selected_family_across_launches() =
        runTest(testDispatcher) {
            val familyId = families.seedFamily("Smiths")
            auth.setAuthenticated()
            repository().select(MealPlanScope.Family(familyId))

            repository().selectedScope.value shouldBe MealPlanScope.Family(familyId)
        }

    @Test
    fun ignores_selecting_a_family_the_user_is_not_in() =
        runTest(testDispatcher) {
            auth.setAuthenticated()
            val repo = repository()

            repo.select(MealPlanScope.Family("someone-elses"))

            repo.selectedScope.value shouldBe MealPlanScope.Personal
        }

    @Test
    fun falls_back_to_personal_after_leaving_the_selected_family() =
        runTest(testDispatcher) {
            val familyId = families.seedFamily("Smiths", myRole = FamilyRole.MEMBER)
            auth.setAuthenticated()
            val repo = repository()
            repo.select(MealPlanScope.Family(familyId))

            families.leaveFamily(familyId)
            repo.refresh()

            repo.familyPlans.value shouldBe emptyList()
            repo.selectedScope.value shouldBe MealPlanScope.Personal
            repository().selectedScope.value shouldBe MealPlanScope.Personal
        }

    @Test
    fun keeps_the_selection_when_offline() =
        runTest(testDispatcher) {
            val familyId = families.seedFamily("Smiths")
            auth.setAuthenticated()
            val repo = repository()
            repo.select(MealPlanScope.Family(familyId))

            families.failure = RuntimeException("offline")
            repo.refresh()

            repo.familyPlans.value shouldBe listOf(FamilyPlan(familyId, "Smiths"))
            repo.selectedScope.value shouldBe MealPlanScope.Family(familyId)
        }

    @Test
    fun signing_out_returns_to_the_personal_plan() =
        runTest(testDispatcher) {
            val familyId = families.seedFamily("Smiths")
            auth.setAuthenticated()
            val repo = repository()
            repo.select(MealPlanScope.Family(familyId))

            auth.setState(AuthState.Unauthenticated)

            repo.familyPlans.value shouldBe emptyList()
            repo.selectedScope.value shouldBe MealPlanScope.Personal
        }

    @Test
    fun stays_personal_with_no_families_listed_while_the_flag_is_off() =
        runTest(testDispatcher) {
            flags.set(FeatureFlagRegistry.FamilyMealPlans, false)
            families.seedFamily("Smiths")
            auth.setAuthenticated()
            val repo = repository()

            repo.refresh()

            repo.familyPlans.value shouldBe emptyList()
            repo.selectedScope.value shouldBe MealPlanScope.Personal
        }
}

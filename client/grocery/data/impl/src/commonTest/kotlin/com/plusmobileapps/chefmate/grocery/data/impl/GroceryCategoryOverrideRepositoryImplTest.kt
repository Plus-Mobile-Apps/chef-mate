@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.grocery.data.impl

import app.cash.turbine.test
import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.database.Database
import com.plusmobileapps.chefmate.database.testing.createTestDatabase
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.util.testing.FakeUnique
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class GroceryCategoryOverrideRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val db: Database = createTestDatabase()
    private val fakeAuth = FakeAuthenticationRepository()

    private val repository =
        GroceryCategoryOverrideRepositoryImpl(
            db = db.groceryCategoryOverrideQueries,
            ioContext = testDispatcher,
            unique = FakeUnique(),
            authRepository = fakeAuth,
        )

    @Test
    fun setOverride_inserts_a_rule_surfaced_by_observeOverrides() =
        runTest(testDispatcher) {
            repository.setOverride("Cold Brew", GroceryCategory.BEVERAGES)

            repository.observeOverrides().test {
                val rules = awaitItem()
                rules.size shouldBe 1
                rules.first().name shouldBe "Cold Brew"
                rules.first().category shouldBe GroceryCategory.BEVERAGES
            }
        }

    @Test
    fun setOverride_trims_and_ignores_blanks() =
        runTest(testDispatcher) {
            repository.setOverride("   ", GroceryCategory.PRODUCE)

            repository.observeOverrides().test { awaitItem() shouldBe emptyList() }
        }

    @Test
    fun setOverride_updates_the_aisle_for_an_existing_name_case_insensitively() =
        runTest(testDispatcher) {
            repository.setOverride("Cold Brew", GroceryCategory.BEVERAGES)
            repository.setOverride("cold brew", GroceryCategory.SNACKS)

            repository.observeOverrides().test {
                val rules = awaitItem()
                rules.size shouldBe 1
                rules.first().category shouldBe GroceryCategory.SNACKS
            }
        }

    @Test
    fun observeOverrideMap_keys_are_lowercased_names() =
        runTest(testDispatcher) {
            repository.setOverride("Cold Brew", GroceryCategory.BEVERAGES)

            repository.observeOverrideMap().test {
                awaitItem() shouldBe mapOf("cold brew" to GroceryCategory.BEVERAGES)
            }
        }

    @Test
    fun removeOverride_removes_it_from_observed_list() =
        runTest(testDispatcher) {
            repository.setOverride("Cold Brew", GroceryCategory.BEVERAGES)
            val id = repository.observeOverrides().first().first().id

            repository.removeOverride(id)

            repository.observeOverrides().test { awaitItem() shouldBe emptyList() }
        }

    @Test
    fun removeOverrideByName_removes_matching_rule_case_insensitively() =
        runTest(testDispatcher) {
            repository.setOverride("Cold Brew", GroceryCategory.BEVERAGES)

            repository.removeOverrideByName("cold brew")

            repository.observeOverrides().test { awaitItem() shouldBe emptyList() }
        }

    @Test
    fun clearLocalData_removes_everything() =
        runTest(testDispatcher) {
            repository.setOverride("Cold Brew", GroceryCategory.BEVERAGES)
            repository.setOverride("Seltzer", GroceryCategory.BEVERAGES)

            repository.clearLocalData()

            repository.observeOverrides().test { awaitItem() shouldBe emptyList() }
        }

    @Test
    fun unrecognized_categoryKey_rows_are_dropped() =
        runTest(testDispatcher) {
            // Simulate a Phase 2 custom-aisle row synced from a newer client this build can't
            // decode.
            db.groceryCategoryOverrideQueries.upsert(
                name = "Firewood",
                categoryKey = "custom:some-uuid",
                clientId = "client-1",
                ownerId = null,
            )

            repository.observeOverrides().test { awaitItem() shouldBe emptyList() }
        }
}

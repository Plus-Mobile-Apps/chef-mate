@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.auth.usecase.impl

import com.plusmobileapps.chefmate.auth.data.testing.FakeAuthenticationRepository
import com.plusmobileapps.chefmate.auth.usecase.SignOutUseCase
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryCategoryOverrideRepository
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class DeleteAccountUseCaseImplTest {

    private val authenticationRepository = FakeAuthenticationRepository()
    private var signedOut = false
    private val signOutUseCase = SignOutUseCase { signedOut = true }
    private val groceryCategoryOverrideRepository = FakeGroceryCategoryOverrideRepository()

    private val useCase =
        DeleteAccountUseCaseImpl(
            authenticationRepository = authenticationRepository,
            signOutUseCase = signOutUseCase,
            groceryCategoryOverrideRepository = groceryCategoryOverrideRepository,
        )

    @Test
    fun When_remote_deletion_succeeds_Then_user_is_signed_out() = runTest {
        val result = useCase()

        result.isSuccess shouldBe true
        authenticationRepository.deleteAccountCallCount shouldBe 1
        signedOut shouldBe true
    }

    @Test
    fun When_remote_deletion_fails_Then_user_is_not_signed_out() = runTest {
        authenticationRepository.deleteAccountResult = Result.failure(RuntimeException("boom"))

        val result = useCase()

        result.isFailure shouldBe true
        signedOut shouldBe false
    }

    @Test
    fun When_account_is_deleted_Then_grocery_category_rules_are_wiped() = runTest {
        // Sign-out preserves the device-local rules, but deleting the account is explicit erasure.
        groceryCategoryOverrideRepository.setOverride("Cold brew", GroceryCategory.BEVERAGES)

        useCase()

        groceryCategoryOverrideRepository.observeOverrides().first() shouldBe emptyList()
    }

    @Test
    fun When_remote_deletion_fails_Then_grocery_category_rules_are_preserved() = runTest {
        groceryCategoryOverrideRepository.setOverride("Cold brew", GroceryCategory.BEVERAGES)
        authenticationRepository.deleteAccountResult = Result.failure(RuntimeException("boom"))

        useCase()

        groceryCategoryOverrideRepository.observeOverrideMap().first() shouldBe
            mapOf("cold brew" to GroceryCategory.BEVERAGES)
    }
}

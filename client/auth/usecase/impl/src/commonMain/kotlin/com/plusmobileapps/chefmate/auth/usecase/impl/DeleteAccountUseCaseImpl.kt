package com.plusmobileapps.chefmate.auth.usecase.impl

import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.auth.usecase.DeleteAccountUseCase
import com.plusmobileapps.chefmate.auth.usecase.SignOutUseCase
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.grocery.data.GroceryCategoryOverrideRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class DeleteAccountUseCaseImpl(
    private val authenticationRepository: AuthenticationRepository,
    private val signOutUseCase: SignOutUseCase,
    private val groceryCategoryOverrideRepository: GroceryCategoryOverrideRepository,
) : DeleteAccountUseCase {
    override suspend fun invoke(): Result<Unit> {
        // Delete the remote account first. Only if that succeeds do we tear down the local session
        // and data — otherwise we'd leave the user signed out with their cloud account intact.
        val result = authenticationRepository.deleteAccount()
        if (result.isFailure) return result
        signOutUseCase()
        // Sign-out deliberately preserves the device-local grocery category rules, but account
        // deletion is explicit erasure — so wipe them here.
        groceryCategoryOverrideRepository.clearLocalData()
        return Result.success(Unit)
    }
}

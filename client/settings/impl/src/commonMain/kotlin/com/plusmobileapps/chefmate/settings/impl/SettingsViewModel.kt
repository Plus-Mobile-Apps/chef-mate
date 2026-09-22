package com.plusmobileapps.chefmate.settings.impl

import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.auth.usecase.SignOutUseCase
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.featureflag.FeatureFlags
import com.plusmobileapps.chefmate.featureflag.isEnabled
import com.plusmobileapps.chefmate.subscription.SubscriptionRepository
import dev.zacsweers.metro.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
class SettingsViewModel(
    @Main mainContext: CoroutineContext,
    private val authenticationRepository: AuthenticationRepository,
    private val signOutUseCase: SignOutUseCase,
    featureFlags: FeatureFlags,
    subscriptionRepository: SubscriptionRepository,
) : ViewModel(mainContext) {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val aiChatEnabled: StateFlow<Boolean> =
        featureFlags.isEnabled(FeatureFlagRegistry.AiChat)

    private val manageFamilyEnabled: StateFlow<Boolean> =
        featureFlags.isEnabled(FeatureFlagRegistry.ManageFamily)

    private val isSubscribed: StateFlow<Boolean> = subscriptionRepository.isSubscribed

    init {
        observeAuthState()
        observeAiChatFlag()
        observeManageFamilyFlag()
        observeSubscription()
    }

    private fun observeManageFamilyFlag() {
        manageFamilyEnabled
            .onEach { enabled -> _state.update { it.copy(isManageFamilyEnabled = enabled) } }
            .launchIn(scope)
    }

    private fun observeAiChatFlag() {
        aiChatEnabled
            .onEach { enabled -> _state.update { it.copy(isAiChatEnabled = enabled) } }
            .launchIn(scope)
    }

    private fun observeSubscription() {
        isSubscribed
            .onEach { subscribed -> _state.update { it.copy(isSubscribed = subscribed) } }
            .launchIn(scope)
    }

    /**
     * Gate for the AI chat row. Returns true when the caller should emit the open-chat output;
     * otherwise it has raised the premium upsell and the caller should do nothing.
     */
    fun requestAiChat(): Boolean {
        if (_state.value.isSubscribed) return true
        _state.update { it.copy(showPremiumRequiredDialog = true) }
        return false
    }

    fun dismissPremiumRequiredDialog() {
        _state.update { it.copy(showPremiumRequiredDialog = false) }
    }

    private fun observeAuthState() {
        authenticationRepository.state
            .onEach { authState ->
                when (authState) {
                    is AuthState.Authenticated -> {
                        val isAnonymous = authState.user.isAnonymous
                        // Anon: no greeting at all. Real user: prefer userName, fall back to
                        // email, and only set a non-null display name when at least one is
                        // non-blank — otherwise the greeting renders as "Hello !" (the empty
                        // string is truthy in the let { ... } we used previously).
                        val displayName =
                            when {
                                isAnonymous -> null
                                authState.user.userName.isNotBlank() -> authState.user.userName
                                authState.user.userEmail.isNotBlank() -> authState.user.userEmail
                                else -> null
                            }
                        _state.update {
                            it.authState(
                                isAuthenticated = true,
                                isAnonymous = isAnonymous,
                                userName = displayName,
                                emailAwaitingVerification = null,
                            )
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        _state.update {
                            it.authState(
                                isAuthenticated = false,
                                isAnonymous = false,
                                userName = null,
                                emailAwaitingVerification = null,
                            )
                        }
                    }
                    is AuthState.AwaitingEmailVerification -> {
                        _state.update {
                            it.authState(
                                isAuthenticated = false,
                                isAnonymous = false,
                                userName = null,
                                emailAwaitingVerification = authState.email,
                            )
                        }
                    }
                }
            }
            .launchIn(scope)
    }

    fun showSignOutConfirmationDialog() {
        _state.update { it.copy(showSignOutConfirmationDialog = true) }
    }

    fun dismissSignOutConfirmationDialog() {
        _state.update { it.copy(showSignOutConfirmationDialog = false) }
    }

    fun signOut() {
        _state.update { it.copy(showSignOutConfirmationDialog = false) }
        scope.launch { signOutUseCase() }
    }

    data class State(
        val isAuthenticated: Boolean = false,
        val isAnonymous: Boolean = false,
        val userName: String? = null,
        val emailAwaitingVerification: String? = null,
        val showSignOutConfirmationDialog: Boolean = false,
        val isAiChatEnabled: Boolean = false,
        val isManageFamilyEnabled: Boolean = false,
        val isSubscribed: Boolean = false,
        val showPremiumRequiredDialog: Boolean = false,
    ) {
        /**
         * Apply an auth-state change, leaving everything the auth stream doesn't own alone. Written
         * as a copy rather than a fresh [State] because the feature flags and the premium
         * entitlement arrive on their own streams — rebuilding the object here used to silently
         * reset them to `false` whenever auth emitted after they had resolved.
         */
        fun authState(
            isAuthenticated: Boolean,
            isAnonymous: Boolean,
            userName: String?,
            emailAwaitingVerification: String?,
        ): State =
            copy(
                isAuthenticated = isAuthenticated,
                isAnonymous = isAnonymous,
                userName = userName,
                emailAwaitingVerification = emailAwaitingVerification,
                // An auth change closes a pending sign-out confirmation, as it did before.
                showSignOutConfirmationDialog = false,
            )
    }
}

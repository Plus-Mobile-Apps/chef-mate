package com.plusmobileapps.chefmate.subscription.impl

import co.touchlab.kermit.Logger
import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.devsettings.DeveloperPreferences
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.isDebugBuild
import com.plusmobileapps.chefmate.subscription.SubscriptionRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Store-backed [SubscriptionRepository]: entitlement comes from RevenueCat's `premium` entitlement,
 * kept live by the SDK's customer-info updates and re-resolved whenever the signed-in ChefMate user
 * changes.
 *
 * In debug builds the Developer Settings override is OR-ed over the real entitlement so QA can
 * reach premium surfaces without a sandbox purchase. Release builds ignore it entirely, so a stale
 * persisted override can never unlock premium for a real user.
 *
 * The primary constructor takes the [EntitlementGateway] so `commonTest` can inject a fake; the
 * [Inject]-annotated secondary constructor is the one Metro uses and builds the platform gateway
 * itself, keeping the gateway internal to this module.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class RevenueCatSubscriptionRepository
internal constructor(
    private val gateway: EntitlementGateway,
    authenticationRepository: AuthenticationRepository,
    preferences: DeveloperPreferences,
    ioContext: CoroutineContext,
    includeDeveloperOverride: Boolean,
) : SubscriptionRepository {

    @Inject
    constructor(
        authenticationRepository: AuthenticationRepository,
        preferences: DeveloperPreferences,
        @IO ioContext: CoroutineContext,
    ) : this(
        gateway = createEntitlementGateway(),
        authenticationRepository = authenticationRepository,
        preferences = preferences,
        ioContext = ioContext,
        includeDeveloperOverride = isDebugBuild,
    )

    private val scope = CoroutineScope(ioContext + SupervisorJob())

    private val entitled = MutableStateFlow(false)

    private val developerOverride: StateFlow<Boolean> =
        if (includeDeveloperOverride) preferences.isSubscribed else MutableStateFlow(false)

    override val isSubscribed: StateFlow<Boolean> =
        combine(entitled, developerOverride) { store, override -> store || override }
            .stateIn(scope, SharingStarted.Eagerly, developerOverride.value)

    init {
        scope.launch { gateway.entitlementUpdates().collect { entitled.value = it } }
        scope.launch {
            authenticationRepository.state
                .map { (it as? AuthState.Authenticated)?.user?.userId }
                .distinctUntilChanged()
                .collect { userId -> syncIdentity(userId) }
        }
    }

    private suspend fun syncIdentity(userId: String?) {
        try {
            entitled.value = if (userId != null) gateway.identify(userId) else gateway.reset()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline or a store hiccup: keep the last known entitlement rather than locking a
            // paying user out. The SDK pushes a fresh value through entitlementUpdates() once it
            // can reach RevenueCat again.
            Logger.w(e) { "Failed to sync RevenueCat identity" }
        }
    }
}

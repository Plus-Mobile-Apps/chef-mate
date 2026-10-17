package com.plusmobileapps.chefmate.devsettings

import com.plusmobileapps.chefmate.Environment
import com.plusmobileapps.chefmate.EnvironmentProvider
import kotlinx.coroutines.flow.StateFlow

const val DEV_ENVIRONMENT_KEY = "dev.environment"
const val DEV_USER_INDEX_KEY = "dev.user_index"
const val DEV_SUBSCRIBED_KEY = "dev.subscribed"

interface DeveloperPreferences : EnvironmentProvider {
    override val environment: StateFlow<Environment>

    val selectedUserIndex: StateFlow<Int?>

    /**
     * Developer override standing in for a real store entitlement. Read by the subscription
     * repository, so flipping it takes effect immediately everywhere premium is gated — no restart.
     * Defaults to `false` so an untouched build behaves like a free user.
     */
    val isSubscribed: StateFlow<Boolean>

    fun setEnvironment(environment: Environment)

    fun setSelectedUserIndex(index: Int?)

    fun setSubscribed(subscribed: Boolean)
}

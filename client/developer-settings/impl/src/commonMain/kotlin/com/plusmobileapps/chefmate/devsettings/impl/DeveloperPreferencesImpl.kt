package com.plusmobileapps.chefmate.devsettings.impl

import com.plusmobileapps.chefmate.Environment
import com.plusmobileapps.chefmate.EnvironmentProvider
import com.plusmobileapps.chefmate.devsettings.DEV_ENVIRONMENT_KEY
import com.plusmobileapps.chefmate.devsettings.DEV_SUBSCRIBED_KEY
import com.plusmobileapps.chefmate.devsettings.DEV_USER_INDEX_KEY
import com.plusmobileapps.chefmate.devsettings.DeveloperPreferences
import com.plusmobileapps.chefmate.di.AppScope
import com.russhwolf.settings.Settings
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
@SingleIn(AppScope::class)
class DeveloperPreferencesImpl(private val settings: Settings) : DeveloperPreferences {

    private val _environment =
        MutableStateFlow(
            settings.getStringOrNull(DEV_ENVIRONMENT_KEY)?.let { stored ->
                Environment.entries.firstOrNull { it.name == stored }
            } ?: Environment.PROD
        )

    override val environment: StateFlow<Environment> = _environment.asStateFlow()

    private val _selectedUserIndex =
        MutableStateFlow(
            if (settings.hasKey(DEV_USER_INDEX_KEY)) {
                settings.getInt(DEV_USER_INDEX_KEY, defaultValue = -1).takeIf { it >= 0 }
            } else {
                null
            }
        )

    override val selectedUserIndex: StateFlow<Int?> = _selectedUserIndex.asStateFlow()

    private val _isSubscribed =
        MutableStateFlow(settings.getBoolean(DEV_SUBSCRIBED_KEY, defaultValue = false))

    override val isSubscribed: StateFlow<Boolean> = _isSubscribed.asStateFlow()

    override fun setEnvironment(environment: Environment) {
        settings.putString(DEV_ENVIRONMENT_KEY, environment.name)
        _environment.value = environment
    }

    override fun setSelectedUserIndex(index: Int?) {
        if (index == null) {
            settings.remove(DEV_USER_INDEX_KEY)
        } else {
            settings.putInt(DEV_USER_INDEX_KEY, index)
        }
        _selectedUserIndex.value = index
    }

    override fun setSubscribed(subscribed: Boolean) {
        settings.putBoolean(DEV_SUBSCRIBED_KEY, subscribed)
        _isSubscribed.value = subscribed
    }
}

@BindingContainer
@ContributesTo(AppScope::class)
object DeveloperPreferencesBindingModule {
    @Provides
    fun provideDeveloperPreferences(impl: DeveloperPreferencesImpl): DeveloperPreferences = impl

    @Provides
    fun provideEnvironmentProvider(impl: DeveloperPreferencesImpl): EnvironmentProvider = impl
}

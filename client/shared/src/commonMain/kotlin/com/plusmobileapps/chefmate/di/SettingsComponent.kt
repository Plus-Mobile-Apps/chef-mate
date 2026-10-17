package com.plusmobileapps.chefmate.di

import com.russhwolf.settings.Settings
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
@ContributesTo(AppScope::class)
object SettingsComponent {
    @Provides @SingleIn(AppScope::class) fun settings(): Settings = Settings()
}

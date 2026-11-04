package com.plusmobileapps.chefmate.di

import com.plusmobileapps.chefmate.ApplicationComponent
import com.plusmobileapps.chefmate.WatchDataBridge
import com.plusmobileapps.chefmate.client.database.DriverFactory
import com.plusmobileapps.chefmate.featureflag.FeatureFlags
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import platform.UIKit.UIApplication

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
abstract class IosApplicationComponent : ApplicationComponent {
    /** Serves grocery data to the watch companion and applies its actions. */
    abstract val watchDataBridge: WatchDataBridge

    /** Read by [com.plusmobileapps.chefmate.RootBlocProvider] to pick the launch UI shell. */
    abstract val featureFlags: FeatureFlags

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides application: UIApplication): IosApplicationComponent
    }

    @Provides fun driverFactory(): DriverFactory = DriverFactory()
}

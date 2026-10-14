package com.plusmobileapps.chefmate.di

import android.content.Context
import com.plusmobileapps.chefmate.ApplicationComponent
import com.plusmobileapps.chefmate.client.database.DriverFactory
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
abstract class AndroidApplicationComponent : ApplicationComponent {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides context: Context): AndroidApplicationComponent
    }

    /** Read by `BaseChefMateAppFunctionService`, which the platform binds outside any activity. */
    abstract val groceryRepository: GroceryRepository

    @Provides fun driverFactory(context: Context): DriverFactory = DriverFactory(context = context)
}

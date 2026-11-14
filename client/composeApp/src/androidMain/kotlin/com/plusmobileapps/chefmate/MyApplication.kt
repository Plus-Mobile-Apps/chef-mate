package com.plusmobileapps.chefmate

import android.app.Application
import com.plusmobileapps.chefmate.buildconfig.BuildConfig
import com.plusmobileapps.chefmate.di.AndroidApplicationComponent
import com.plusmobileapps.chefmate.subscription.impl.SubscriptionInitializer
import dev.zacsweers.metro.createGraphFactory

class MyApplication : Application() {
    lateinit var appComponent: AndroidApplicationComponent

    override fun onCreate() {
        super.onCreate()
        BugsnagInitializer(this).initialize(BuildConfig.BUGSNAG_API_KEY)
        // Before the graph: the subscription repository resolves its RevenueCat gateway on
        // creation.
        SubscriptionInitializer.initialize()
        appComponent = createGraphFactory<AndroidApplicationComponent.Factory>().create(this)
    }
}

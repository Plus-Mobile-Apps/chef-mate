package com.plusmobileapps.chefmate.featureflag.impl

import com.plusmobileapps.chefmate.di.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
@ContributesTo(AppScope::class)
object FeatureFlagEvaluatorModule {
    @Provides
    @SingleIn(AppScope::class)
    fun provideEvaluator(): FeatureFlagEvaluator = FeatureFlagEvaluator()
}

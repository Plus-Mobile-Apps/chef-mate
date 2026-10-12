package com.plusmobileapps.chefmate.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Qualifier
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Qualifier
@Target(
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class IO

@Qualifier
@Target(
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class Main

@Qualifier
@Target(
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class CPU

@Qualifier
@Target(
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.TYPE,
)
annotation class Unconfined

@BindingContainer
@ContributesTo(AppScope::class)
object CoroutinesComponent {
    @Provides @SingleIn(AppScope::class) @IO fun io(): CoroutineContext = Dispatchers.IO

    @Provides @SingleIn(AppScope::class) @Main fun main(): CoroutineContext = Dispatchers.Main

    @Provides @SingleIn(AppScope::class) @CPU fun default(): CoroutineContext = Dispatchers.Default

    @Provides
    @SingleIn(AppScope::class)
    @Unconfined
    fun unconfined(): CoroutineContext = Dispatchers.Unconfined
}

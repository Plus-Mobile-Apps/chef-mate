@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.testing

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ComponentContextFactory
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.essenty.instancekeeper.InstanceKeeperOwner
import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.statekeeper.StateKeeperOwner
import com.plusmobileapps.chefmate.BlocContext
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class TestBlocContext(
    componentContext: ComponentContext,
    override val ioContext: CoroutineContext = UnconfinedTestDispatcher(),
    override val defaultContext: CoroutineContext = UnconfinedTestDispatcher(),
    override val mainContext: CoroutineContext = UnconfinedTestDispatcher(),
    override val unconfinedContext: CoroutineContext = UnconfinedTestDispatcher(),
) :
    BlocContext,
    LifecycleOwner by componentContext,
    StateKeeperOwner by componentContext,
    InstanceKeeperOwner by componentContext,
    BackHandlerOwner by componentContext {
    override val componentContextFactory: ComponentContextFactory<BlocContext> =
        ComponentContextFactory { lifecycle, stateKeeper, instanceKeeper, backHandler ->
            val ctx =
                componentContext.componentContextFactory(
                    lifecycle,
                    stateKeeper,
                    instanceKeeper,
                    backHandler,
                )
            TestBlocContext(componentContext = ctx)
        }

    override fun createScope(): CoroutineScope = coroutineScope(mainContext + SupervisorJob())

    companion object {
        fun create(
            lifecycle: LifecycleRegistry = LifecycleRegistry().apply { resume() },
            mainContext: CoroutineContext = UnconfinedTestDispatcher(),
        ): TestBlocContext =
            TestBlocContext(
                componentContext = DefaultComponentContext(lifecycle = lifecycle),
                mainContext = mainContext,
            )
    }
}

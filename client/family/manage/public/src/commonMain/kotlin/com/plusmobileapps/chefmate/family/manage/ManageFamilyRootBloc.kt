package com.plusmobileapps.chefmate.family.manage

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.ui.ComposeScreen
import com.plusmobileapps.chefmate.ui.Content
import com.plusmobileapps.chefmate.ui.backAnimation

/**
 * The Manage Family flow opened from the More tab: the list of the user's families, and a detail
 * screen per family for managing its members.
 */
interface ManageFamilyRootBloc : BackHandlerOwner, BackClickBloc, ComposeScreen {
    val routerState: Value<ChildStack<*, Child>>

    @Composable
    override fun Content(modifier: Modifier) {
        Children(
            modifier = modifier.fillMaxSize(),
            stack = routerState,
            animation = backAnimation(backHandler = backHandler, onBack = ::onBackClicked),
        ) { child ->
            child.instance.bloc.Content()
        }
    }

    sealed class Child {
        abstract val bloc: ComposeScreen

        data class FamilyList(override val bloc: FamilyListBloc) : Child()

        data class FamilyDetail(override val bloc: FamilyDetailBloc) : Child()
    }

    sealed class Output {
        data object Back : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): ManageFamilyRootBloc
    }
}

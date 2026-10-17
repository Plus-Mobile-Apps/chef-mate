@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.plusmobileapps.chefmate.browser

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.plusmobileapps.chefmate.ui.components.LocalBottomNavBarInset

@Composable
fun BrowserRootScreen(bloc: BrowserRootBloc, modifier: Modifier = Modifier) {
    val childStack by bloc.routerState.subscribeAsState()

    // The browser tab is the one tab that can't scroll under the floating nav pill: the page is a
    // platform WebView — an interop view drawn outside Compose's draw pass — so it can neither take
    // a content inset nor be captured as a backdrop to blur. Inset the whole tab instead, which
    // also keeps the address bar and the Download row clear.
    SharedTransitionLayout(modifier = modifier.padding(bottom = LocalBottomNavBarInset.current)) {
        AnimatedContent(
            targetState = childStack.active.instance,
            transitionSpec = {
                fadeIn(tween(durationMillis = 400)) togetherWith
                    fadeOut(tween(durationMillis = 250))
            },
            label = "browser-root",
        ) { instance ->
            when (instance) {
                is BrowserRootBloc.Child.SelectEngine ->
                    BrowserSelectEngineScreen(
                        bloc = instance.bloc,
                        modifier = Modifier.fillMaxSize(),
                    )
                is BrowserRootBloc.Child.Landing ->
                    BrowserLandingScreen(
                        bloc = instance.bloc,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        modifier = Modifier.fillMaxSize(),
                    )
                is BrowserRootBloc.Child.EditQuery ->
                    BrowserEditQueryScreen(
                        bloc = instance.bloc,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        modifier = Modifier.fillMaxSize(),
                    )
                is BrowserRootBloc.Child.Browser ->
                    BrowserScreen(
                        bloc = instance.bloc,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this,
                        modifier = Modifier.fillMaxSize(),
                    )
            }
        }
    }
}

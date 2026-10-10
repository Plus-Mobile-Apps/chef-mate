@file:OptIn(ExperimentalDecomposeApi::class, ExperimentalComposeUiApi::class)

package com.plusmobileapps.chefmate

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureIcon
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.plusmobileapps.chefmate.root.RootBloc

object MainViewController {
    fun create(rootBloc: RootBloc, backDispatcher: BackDispatcher) =
        ComposeUIViewController(configure = { parallelRendering = true }) {
            PredictiveBackGestureOverlay(
                backDispatcher = backDispatcher,
                backIcon = { progress, _ ->
                    PredictiveBackGestureIcon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        progress = progress,
                    )
                },
                modifier = Modifier.fillMaxSize(),
            ) {
                App(
                    rootBloc = rootBloc,
                    toastService = RootBlocProvider.toastService,
                    inAppReviewService = RootBlocProvider.inAppReviewService,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
}

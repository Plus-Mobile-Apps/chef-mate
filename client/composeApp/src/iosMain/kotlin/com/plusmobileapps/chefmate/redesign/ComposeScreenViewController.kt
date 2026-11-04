@file:OptIn(ExperimentalComposeUiApi::class)

package com.plusmobileapps.chefmate.redesign

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.plusmobileapps.chefmate.RootBlocProvider
import com.plusmobileapps.chefmate.toast.LocalToastService
import com.plusmobileapps.chefmate.ui.ComposeScreen
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import platform.UIKit.UIViewController

/**
 * Hosts one not-yet-redesigned Compose screen inside the SwiftUI redesign shell (Swift's
 * `ComposeScreenHost`). The shell owns navigation; this only renders [screen] with the theme and
 * image loader that `App` would otherwise provide. Toasts are drawn by the shell's native toast
 * host (Swift `ToastHost`), so this only provides [LocalToastService] for composables that enqueue
 * one — a Compose `ToastScaffold` per hosted screen would race the native host for the same queue.
 */
object ComposeScreenViewController {
    fun create(screen: ComposeScreen): UIViewController =
        ComposeUIViewController(
            configure = {
                parallelRendering = true
                onFocusBehavior = OnFocusBehavior.DoNothing
            },
        ) {
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context).components { add(KtorNetworkFetcherFactory()) }.build()
            }
            ChefMateTheme {
                CompositionLocalProvider(LocalToastService provides RootBlocProvider.toastService) {
                    screen.Content(Modifier.fillMaxSize())
                }
            }
        }
}

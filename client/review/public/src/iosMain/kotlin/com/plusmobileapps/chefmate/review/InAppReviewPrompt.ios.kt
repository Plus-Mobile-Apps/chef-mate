@file:Suppress("ktlint:standard:filename")

package com.plusmobileapps.chefmate.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIWindowScene

@Composable
internal actual fun InAppReviewPrompt(onFinished: () -> Unit) {
    val currentOnFinished = rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        val scene =
            UIApplication.sharedApplication.connectedScenes
                .filterIsInstance<UIWindowScene>()
                .firstOrNull {
                    it.activationState == UISceneActivationStateForegroundActive
                }
        // StoreKit 2's AppStore.requestReview(in:) is Swift-only; this is its Objective-C
        // counterpart, deprecated in iOS 18 but still honored.
        @Suppress("DEPRECATION")
        if (scene != null) SKStoreReviewController.requestReviewInScene(scene)
        currentOnFinished.value()
    }
}

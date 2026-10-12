package com.plusmobileapps.chefmate.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Shows the platform review prompt whenever [service] requests one. Wired once at the app root,
 * next to the toast host, so a prompt raised from any screen survives that screen closing.
 */
@Composable
fun InAppReviewHost(service: InAppReviewService) {
    val isPromptRequested by service.isPromptRequested.collectAsState()
    if (isPromptRequested) {
        InAppReviewPrompt(onFinished = service::onPromptFinished)
    }
}

/**
 * Runs the platform's store review flow, then calls [onFinished]. Platforms whose store forbids or
 * lacks an in-app prompt call [onFinished] straight away. The OS decides whether a review sheet is
 * actually shown (both Google Play and the App Store quota it), so callers get no result.
 */
@Composable internal expect fun InAppReviewPrompt(onFinished: () -> Unit)

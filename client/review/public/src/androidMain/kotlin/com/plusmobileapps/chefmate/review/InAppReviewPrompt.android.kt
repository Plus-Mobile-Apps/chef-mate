@file:Suppress("ktlint:standard:filename")

package com.plusmobileapps.chefmate.review

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.CancellationException

@Composable
internal actual fun InAppReviewPrompt(onFinished: () -> Unit) {
    val activity = LocalActivity.current
    val currentOnFinished = rememberUpdatedState(onFinished)
    LaunchedEffect(activity) {
        if (activity != null) {
            try {
                val manager = ReviewManagerFactory.create(activity)
                manager.launchReview(activity, manager.requestReview())
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Play Services missing or quota hit — the review is best-effort, never surfaced.
            }
        }
        currentOnFinished.value()
    }
}

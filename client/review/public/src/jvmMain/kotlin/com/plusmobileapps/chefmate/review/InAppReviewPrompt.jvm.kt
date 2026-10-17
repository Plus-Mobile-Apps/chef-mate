@file:Suppress("ktlint:standard:filename")

package com.plusmobileapps.chefmate.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalUriHandler
import chefmate.client.review.public.generated.resources.Res
import chefmate.client.review.public.generated.resources.review_prompt_message
import chefmate.client.review.public.generated.resources.review_prompt_not_now
import chefmate.client.review.public.generated.resources.review_prompt_rate
import chefmate.client.review.public.generated.resources.review_prompt_title
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.ui.components.PlusDialog

@Composable
internal actual fun InAppReviewPrompt(onFinished: () -> Unit) {
    val reviewUrl = remember { DesktopStoreReview.reviewUrl() }
    if (reviewUrl == null) {
        val currentOnFinished = rememberUpdatedState(onFinished)
        LaunchedEffect(Unit) { currentOnFinished.value() }
        return
    }
    val uriHandler = LocalUriHandler.current
    PlusDialog(
        title = ResourceString(Res.string.review_prompt_title),
        message = ResourceString(Res.string.review_prompt_message),
        confirmButtonText = ResourceString(Res.string.review_prompt_rate),
        dismissButtonText = ResourceString(Res.string.review_prompt_not_now),
        onConfirmClick = {
            runCatching { uriHandler.openUri(reviewUrl) }
            onFinished()
        },
        onDismissRequest = onFinished,
    )
}

/**
 * Where desktop sends a user who agrees to review. Only the Microsoft Store is supported:
 * - **Windows** has no in-app review API reachable from the JVM, but the Store permits an app's own
 *   prompt that deep links to its review page.
 * - **macOS** ships through the Mac App Store, whose guideline 5.6.1 allows only StoreKit's native
 *   prompt — unreachable from the JVM without a native bridge — so it is a no-op for now.
 * - **Linux** has no store listing.
 */
internal object DesktopStoreReview {
    /**
     * Microsoft Store product ID (the same value as the `STORE_PRODUCT_ID` CI secret). Windows
     * skips the prompt while this is blank.
     */
    const val MICROSOFT_STORE_PRODUCT_ID: String = ""

    fun reviewUrl(
        osName: String = System.getProperty("os.name").orEmpty(),
        productId: String = MICROSOFT_STORE_PRODUCT_ID,
    ): String? =
        if (osName.startsWith("Windows", ignoreCase = true) && productId.isNotBlank()) {
            "ms-windows-store://review/?ProductId=$productId"
        } else {
            null
        }
}

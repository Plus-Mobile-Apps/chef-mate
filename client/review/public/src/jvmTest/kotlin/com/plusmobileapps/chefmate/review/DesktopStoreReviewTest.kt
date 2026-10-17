@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.review

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DesktopStoreReviewTest {

    @Test
    fun When_windows_with_product_id_Then_store_review_url() {
        DesktopStoreReview.reviewUrl(osName = "Windows 11", productId = "9NBLGGH4R32N") shouldBe
            "ms-windows-store://review/?ProductId=9NBLGGH4R32N"
    }

    @Test
    fun When_windows_without_product_id_Then_null() {
        DesktopStoreReview.reviewUrl(osName = "Windows 11", productId = "") shouldBe null
    }

    @Test
    fun When_macos_Then_null() {
        DesktopStoreReview.reviewUrl(osName = "Mac OS X", productId = "9NBLGGH4R32N") shouldBe null
    }

    @Test
    fun When_linux_Then_null() {
        DesktopStoreReview.reviewUrl(osName = "Linux", productId = "9NBLGGH4R32N") shouldBe null
    }
}

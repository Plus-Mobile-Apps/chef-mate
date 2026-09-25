package com.plusmobileapps.chefmate.toast.impl

import com.plusmobileapps.chefmate.text.FixedString
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class ToastServiceImplTest {

    @Test
    fun WHEN_show_called_THEN_message_enqueued() = runTest {
        val service = ToastServiceImpl()

        service.show(FixedString("Saved"))

        service.queue.value.head?.text shouldBe FixedString("Saved")
    }

    @Test
    fun WHEN_multiple_shown_THEN_queued_in_order_with_distinct_ids() = runTest {
        val service = ToastServiceImpl()

        service.show(FixedString("First"))
        service.show(FixedString("Second"))

        val messages = service.queue.value.messages
        messages.map { it.text } shouldBe listOf(FixedString("First"), FixedString("Second"))
        (messages[0].id == messages[1].id) shouldBe false
    }

    @Test
    fun WHEN_show_called_with_onDismiss_THEN_callback_carried_on_message() = runTest {
        val service = ToastServiceImpl()
        var dismissed = false

        service.show(FixedString("Deleted"), onDismiss = { dismissed = true })
        service.queue.value.head?.onDismiss?.invoke()

        dismissed shouldBe true
    }

    @Test
    fun WHEN_onShown_THEN_only_that_message_dequeued() = runTest {
        val service = ToastServiceImpl()
        service.show(FixedString("First"))
        service.show(FixedString("Second"))
        val firstId = service.queue.value.head?.id ?: error("expected a queued message")

        service.onShown(firstId)

        service.queue.value.head?.text shouldBe FixedString("Second")
    }
}

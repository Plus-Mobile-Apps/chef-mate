@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.browser.impl

import com.plusmobileapps.chefmate.browser.BrowserBookmark
import com.plusmobileapps.chefmate.browser.BrowserBookmarksBloc
import com.plusmobileapps.chefmate.browser.testing.FakeBrowserBookmarkRepository
import com.plusmobileapps.chefmate.testing.TestBlocContext
import com.plusmobileapps.chefmate.testing.TestConsumer
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class BrowserBookmarksBlocImplTest {

    private val context = TestBlocContext.create()
    private val output = TestConsumer<BrowserBookmarksBloc.Output>()
    private val first = BrowserBookmark(id = 1, url = "https://example.com/", title = "Example")
    private val second = BrowserBookmark(id = 2, url = "https://budgetbytes.com/", title = "Budget")
    private val third = BrowserBookmark(id = 3, url = "https://seriouseats.com/", title = "Serious")
    private val repository = FakeBrowserBookmarkRepository(listOf(first, second, third))
    private val bloc =
        BrowserBookmarksBlocImpl(
            context = context,
            output = output,
            viewModelFactory = { BrowserBookmarksViewModel(context.mainContext, repository) },
        )

    @Test
    fun When_opened_Then_bookmarks_loaded_without_selection() {
        bloc.state.value.bookmarks shouldBe listOf(first, second, third)
        bloc.state.value.isLoading shouldBe false
        bloc.state.value.selectionMode shouldBe false
    }

    @Test
    fun When_bookmark_long_clicked_Then_selection_starts_with_it() {
        bloc.onBookmarkLongClicked(second)
        bloc.state.value.selectionMode shouldBe true
        bloc.state.value.selectedIds shouldBe setOf(second.id)
    }

    @Test
    fun When_bookmark_clicked_outside_selection_Then_selection_starts_with_it() {
        bloc.onBookmarkClicked(first)
        bloc.state.value.selectionMode shouldBe true
        bloc.state.value.selectedIds shouldBe setOf(first.id)
    }

    @Test
    fun Given_selected_When_clicked_again_Then_deselected_but_still_selecting() {
        bloc.onBookmarkClicked(first)
        bloc.onBookmarkClicked(first)
        bloc.state.value.selectionMode shouldBe true
        bloc.state.value.selectedIds shouldBe emptySet()
    }

    @Test
    fun When_select_mode_clicked_Then_selection_starts_empty() {
        bloc.onSelectModeClicked()
        bloc.state.value.selectionMode shouldBe true
        bloc.state.value.selectedIds shouldBe emptySet()
    }

    @Test
    fun When_select_all_clicked_twice_Then_all_selected_then_none() {
        bloc.onSelectModeClicked()
        bloc.onSelectAllClicked()
        bloc.state.value.selectedIds shouldBe setOf(first.id, second.id, third.id)
        bloc.state.value.allSelected shouldBe true

        bloc.onSelectAllClicked()
        bloc.state.value.selectedIds shouldBe emptySet()
    }

    @Test
    fun Given_selection_When_delete_clicked_Then_confirmation_shows_count() {
        bloc.onBookmarkClicked(first)
        bloc.onBookmarkClicked(third)
        bloc.onDeleteSelectedClicked()
        bloc.state.value.pendingDeleteCount shouldBe 2
    }

    @Test
    fun Given_nothing_selected_When_delete_clicked_Then_no_confirmation() {
        bloc.onSelectModeClicked()
        bloc.onDeleteSelectedClicked()
        bloc.state.value.pendingDeleteCount shouldBe null
    }

    @Test
    fun When_delete_confirmed_Then_selected_bookmarks_removed_and_selection_exits() {
        bloc.onBookmarkClicked(first)
        bloc.onBookmarkClicked(third)
        bloc.onDeleteSelectedClicked()
        bloc.onDeleteSelectedConfirmed()

        repository.bookmarks.value shouldBe listOf(second)
        bloc.state.value.bookmarks shouldBe listOf(second)
        bloc.state.value.selectionMode shouldBe false
        bloc.state.value.pendingDeleteCount shouldBe null
    }

    @Test
    fun When_delete_dismissed_Then_nothing_removed_and_selection_kept() {
        bloc.onBookmarkClicked(first)
        bloc.onDeleteSelectedClicked()
        bloc.onDeleteSelectedDismissed()

        repository.bookmarks.value shouldBe listOf(first, second, third)
        bloc.state.value.pendingDeleteCount shouldBe null
        bloc.state.value.selectedIds shouldBe setOf(first.id)
    }

    @Test
    fun Given_selected_bookmark_When_removed_elsewhere_Then_dropped_from_selection() {
        bloc.onBookmarkClicked(first)
        bloc.onBookmarkClicked(second)
        repository.bookmarks.value = listOf(second, third)
        bloc.state.value.selectedIds shouldBe setOf(second.id)
    }

    @Test
    fun Given_selecting_When_back_clicked_Then_selection_cancelled_without_output() {
        bloc.onBookmarkClicked(first)
        bloc.onBackClicked()
        bloc.state.value.selectionMode shouldBe false
        bloc.state.value.selectedIds shouldBe emptySet()
        output.values shouldBe emptyList()
    }

    @Test
    fun When_back_clicked_Then_back_output_emitted() {
        bloc.onBackClicked()
        output.lastValue shouldBe BrowserBookmarksBloc.Output.Back
    }
}

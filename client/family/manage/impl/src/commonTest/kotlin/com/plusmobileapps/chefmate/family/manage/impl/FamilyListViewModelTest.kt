@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.family.manage.impl

import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.data.testing.FakeFamilyRepository
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

class FamilyListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun viewModel(
        repository: FakeFamilyRepository = FakeFamilyRepository(),
        onFamilyCreated: (String) -> Unit = {},
    ) =
        FamilyListViewModel(
            onFamilyCreated = onFamilyCreated,
            mainContext = testDispatcher,
            repository = repository,
        )

    @Test
    fun starts_loading_until_the_first_refresh() =
        runTest(testDispatcher) {
            val vm = viewModel()

            vm.state.value.isLoading shouldBe true
            vm.state.value.hasLoaded shouldBe false
        }

    @Test
    fun refresh_lists_the_families_sorted_by_name() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()
            repository.seedFamily("Smiths")
            repository.seedFamily("Bakers", myRole = FamilyRole.MEMBER)
            val vm = viewModel(repository)

            vm.refresh()

            vm.state.value.families.map { it.name } shouldBe listOf("Bakers", "Smiths")
            vm.state.value.isLoading shouldBe false
            vm.state.value.loadError shouldBe null
        }

    @Test
    fun a_failed_first_load_shows_the_error_state() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()
            repository.failure = IllegalStateException("offline")
            val vm = viewModel(repository)

            vm.refresh()

            vm.state.value.isLoading shouldBe false
            vm.state.value.loadError shouldNotBe null
        }

    @Test
    fun a_failed_refresh_keeps_the_list_that_is_already_on_screen() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()
            repository.seedFamily("Smiths")
            val vm = viewModel(repository)
            vm.refresh()

            repository.failure = IllegalStateException("offline")
            vm.refresh()

            vm.state.value.families.map { it.name } shouldBe listOf("Smiths")
            vm.state.value.loadError shouldBe null
            vm.state.value.isLoading shouldBe false
        }

    @Test
    fun a_refresh_after_the_first_load_does_not_blank_the_screen() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()
            repository.seedFamily("Smiths")
            val vm = viewModel(repository)
            vm.refresh()

            vm.refresh()

            vm.state.value.isLoading shouldBe false
            repository.getFamiliesCount shouldBe 2
        }

    @Test
    fun the_create_dialog_only_confirms_with_a_name() =
        runTest(testDispatcher) {
            val vm = viewModel()

            vm.onCreateFamilyClicked()
            vm.state.value.createDialog?.canConfirm shouldBe false

            vm.onNewFamilyNameChanged("  ")
            vm.state.value.createDialog?.canConfirm shouldBe false

            vm.onNewFamilyNameChanged("Smiths")
            vm.state.value.createDialog?.canConfirm shouldBe true

            vm.onCreateDismissed()
            vm.state.value.createDialog shouldBe null
        }

    @Test
    fun creating_a_family_opens_it() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()
            var opened: String? = null
            val vm = viewModel(repository, onFamilyCreated = { opened = it })

            vm.onCreateFamilyClicked()
            vm.onNewFamilyNameChanged("Smiths")
            vm.onCreateConfirmed()

            opened shouldNotBe null
            repository.getFamilies().single().name shouldBe "Smiths"
            vm.state.value.createDialog shouldBe null
        }

    @Test
    fun a_failed_create_keeps_the_dialog_open_with_an_error() =
        runTest(testDispatcher) {
            val repository = FakeFamilyRepository()
            var opened: String? = null
            val vm = viewModel(repository, onFamilyCreated = { opened = it })
            vm.onCreateFamilyClicked()
            vm.onNewFamilyNameChanged("Smiths")

            repository.failure = IllegalStateException("offline")
            vm.onCreateConfirmed()

            opened shouldBe null
            vm.state.value.createDialog?.error shouldNotBe null
            vm.state.value.createDialog?.isSaving shouldBe false
        }
}

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.plusmobileapps.chefmate.grocery.categoryrules.impl

import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.CreateState
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.DialogState
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryCategoryOverride
import com.plusmobileapps.chefmate.grocery.data.testing.FakeGroceryCategoryOverrideRepository
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class GroceryCategoryRulesViewModelTest {

    private val overrides = MutableStateFlow<List<GroceryCategoryOverride>>(emptyList())
    private val repository = FakeGroceryCategoryOverrideRepository(overrides)
    private val mainContext = UnconfinedTestDispatcher()

    private fun createViewModel() = GroceryCategoryRulesViewModel(mainContext, repository)

    @Test
    fun loads_existing_rules() {
        overrides.value =
            listOf(GroceryCategoryOverride(id = 1L, name = "Cold brew", GroceryCategory.BEVERAGES))
        val vm = createViewModel()

        vm.state.value.rules.map { it.name to it.category } shouldBe
            listOf("Cold brew" to GroceryCategory.BEVERAGES)
        vm.state.value.isLoading shouldBe false
    }

    @Test
    fun open_create_then_submit_adds_the_trimmed_rule_with_selected_category() {
        val vm = createViewModel()
        vm.openCreateField()
        vm.updateCreateName("  Paper towels  ")
        vm.updateCreateCategory(GroceryCategory.OTHER)
        vm.submitCreate()

        vm.state.value.createState shouldBe CreateState.Hidden
        vm.state.value.rules.map { it.name to it.category } shouldBe
            listOf("Paper towels" to GroceryCategory.OTHER)
    }

    @Test
    fun submit_blank_name_is_ignored() {
        val vm = createViewModel()
        vm.openCreateField()
        vm.updateCreateName("   ")
        vm.submitCreate()

        vm.state.value.rules shouldBe emptyList()
    }

    @Test
    fun delete_flow_removes_the_rule() {
        overrides.value =
            listOf(GroceryCategoryOverride(id = 7L, name = "Cold brew", GroceryCategory.BEVERAGES))
        val vm = createViewModel()
        val rule = vm.state.value.rules.first()

        vm.showDeleteDialog(rule)
        vm.state.value.dialog shouldBe DialogState.Delete(rule)
        vm.confirmDelete()

        vm.state.value.dialog shouldBe DialogState.None
        vm.state.value.rules shouldBe emptyList()
    }
}

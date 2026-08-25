package com.plusmobileapps.chefmate.grocery.categoryrules.impl

import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.CreateState
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.DialogState
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.Model
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.Rule
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryCategoryOverrideRepository
import dev.zacsweers.metro.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
class GroceryCategoryRulesViewModel(
    @Main mainContext: CoroutineContext,
    private val repository: GroceryCategoryOverrideRepository,
) : ViewModel(mainContext) {

    private val _state = MutableStateFlow(Model())
    val state: StateFlow<Model> = _state.asStateFlow()

    init {
        scope.launch {
            repository.observeOverrides().collect { overrides ->
                _state.update { current ->
                    current.copy(
                        rules =
                            overrides
                                .map { Rule(id = it.id, name = it.name, category = it.category) }
                                .toImmutableList(),
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun openCreateField() {
        _state.update { current ->
            if (current.createState is CreateState.Editing) current
            else current.copy(createState = CreateState.Editing(name = ""))
        }
    }

    fun closeCreateField() {
        _state.update { it.copy(createState = CreateState.Hidden) }
    }

    fun updateCreateName(name: String) {
        _state.update { current ->
            val editing = current.createState as? CreateState.Editing ?: return@update current
            current.copy(createState = editing.copy(name = name))
        }
    }

    fun updateCreateCategory(category: GroceryCategory) {
        _state.update { current ->
            val editing = current.createState as? CreateState.Editing ?: return@update current
            current.copy(createState = editing.copy(category = category))
        }
    }

    fun submitCreate() {
        val editing = _state.value.createState as? CreateState.Editing ?: return
        val name = editing.name.trim()
        if (name.isBlank()) return
        _state.update { it.copy(createState = CreateState.Hidden) }
        scope.launch { repository.setOverride(name, editing.category) }
    }

    fun showDeleteDialog(rule: Rule) {
        _state.update { it.copy(dialog = DialogState.Delete(rule)) }
    }

    fun confirmDelete() {
        val target = (_state.value.dialog as? DialogState.Delete)?.target ?: return
        _state.update { it.copy(dialog = DialogState.None) }
        scope.launch { repository.removeOverride(target.id) }
    }

    fun dismissDialog() {
        _state.update { it.copy(dialog = DialogState.None) }
    }
}

package com.plusmobileapps.chefmate.grocery.core.impl.detail

import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryCategoryOverrideRepository
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.grocery.data.GroceryRepository
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@AssistedInject
class GroceryDetailViewModel(
    @Assisted id: Long,
    @Main mainContext: CoroutineContext,
    private val repository: GroceryRepository,
    private val categoryOverrideRepository: GroceryCategoryOverrideRepository,
) : ViewModel(mainContext) {
    private val _state = MutableStateFlow(State())
    private val output = Channel<Output>(Channel.BUFFERED)

    val state: StateFlow<State> = _state.asStateFlow()

    val outputs: Flow<Output> = output.receiveAsFlow()

    init {
        loadGrocery(id)
    }

    override fun onCleared() {
        super.onCleared()
        output.close()
    }

    fun onGroceryNameChanged(name: String) {
        _state.value =
            _state.value.copy(groceryItem = _state.value.groceryItem.copy(displayName = name))
    }

    fun onGroceryQuantityChanged(quantity: String) {
        _state.value =
            _state.value.copy(
                groceryItem = _state.value.groceryItem.copy(quantity = quantity.ifBlank { null })
            )
    }

    fun onGroceryCheckedChanged(isChecked: Boolean) {
        _state.value =
            _state.value.copy(groceryItem = _state.value.groceryItem.copy(isChecked = isChecked))
    }

    fun onAisleChanged(category: GroceryCategory) {
        val current = _state.value
        _state.value = current.copy(groceryItem = current.groceryItem.copy(category = category))
        // Keep an active rule pointing at whatever aisle is now selected.
        if (current.alwaysFileHere) {
            val name = current.groceryItem.displayName
            scope.launch { categoryOverrideRepository.setOverride(name, category) }
        }
    }

    fun onAlwaysFileHereToggled(enabled: Boolean) {
        val current = _state.value
        val name = current.groceryItem.displayName
        if (name.isBlank()) return
        _state.value = current.copy(alwaysFileHere = enabled)
        scope.launch {
            if (enabled) {
                categoryOverrideRepository.setOverride(name, current.groceryItem.category)
            } else {
                categoryOverrideRepository.removeOverrideByName(name)
            }
        }
    }

    fun save() {
        val item = _state.value.groceryItem
        val combinedName = combineName(quantity = item.quantity, displayName = item.displayName)
        if (combinedName.isBlank()) return
        scope.launch {
            repository.updateGrocery(item.copy(name = combinedName))
            output.send(Output.Finished)
        }
    }

    private fun combineName(quantity: String?, displayName: String): String {
        val q = quantity?.trim().orEmpty()
        val n = displayName.trim()
        return if (q.isEmpty()) n else "$q $n".trim()
    }

    private fun loadGrocery(id: Long) {
        scope.launch {
            val grocery = repository.getGrocery(id) ?: GroceryItem.empty
            val overrides = categoryOverrideRepository.observeOverrideMap().first()
            // The checkbox is on only when a rule for this name points at the aisle now shown.
            val alwaysFileHere = overrides[grocery.displayName.lowercase()] == grocery.category
            _state.value =
                State(
                    isLoading = false,
                    groceryItem = grocery,
                    alwaysFileHere = alwaysFileHere,
                )
        }
    }

    data class State(
        val isLoading: Boolean = true,
        val groceryItem: GroceryItem = GroceryItem.empty,
        val alwaysFileHere: Boolean = false,
    )

    sealed class Output {
        data object Finished : Output()
    }

    @AssistedFactory
    fun interface Factory {
        fun create(id: Long): GroceryDetailViewModel
    }
}

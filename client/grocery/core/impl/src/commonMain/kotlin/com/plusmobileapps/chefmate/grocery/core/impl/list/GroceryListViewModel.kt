package com.plusmobileapps.chefmate.grocery.core.impl.list

import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.grocery.core.impl.list.GroceryListPresenter.Event
import com.plusmobileapps.chefmate.grocery.core.impl.list.GroceryListPresenter.State
import dev.zacsweers.metro.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Retained host for [GroceryListPresenter]. Decompose's `instanceKeeper` keeps this alive across
 * configuration changes, so the molecule (and every subscription inside it) survives rotation and
 * is torn down by [onCleared] cancelling [scope].
 */
@Inject
class GroceryListViewModel(@Main mainContext: CoroutineContext, presenter: GroceryListPresenter) :
    ViewModel(mainContext) {

    // Unlimited buffer: an event sent before the presenter's collector starts is queued, not lost.
    private val events = Channel<Event>(Channel.UNLIMITED)
    // Created once: a new Flow instance per recomposition would restart the presenter's collector.
    private val eventFlow = events.receiveAsFlow()

    private val _newGroceryItemName = MutableStateFlow("")
    val newGroceryItemName: StateFlow<String> = _newGroceryItemName.asStateFlow()

    // Immediate mode recomposes as soon as state changes, without needing a platform frame clock.
    val state: StateFlow<State> =
        scope.launchMolecule(RecompositionMode.Immediate) {
            presenter.present(events = eventFlow, query = newGroceryItemName)
        }

    fun take(event: Event) {
        events.trySend(event)
    }

    fun onNewGroceryItemNameChange(name: String) {
        _newGroceryItemName.value = name
    }

    fun saveGroceryItem() {
        val name = _newGroceryItemName.value
        if (name.isBlank()) return
        take(Event.AddItem(name))
        _newGroceryItemName.value = ""
    }
}

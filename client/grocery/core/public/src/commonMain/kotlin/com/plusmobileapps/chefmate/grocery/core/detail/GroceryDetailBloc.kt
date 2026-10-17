package com.plusmobileapps.chefmate.grocery.core.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.grocery.data.GroceryItem
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.coroutines.flow.StateFlow

interface GroceryDetailBloc : BackClickBloc, ComposeScreen {
    val models: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        GroceryDetailSheetContent(bloc = this, modifier = modifier)
    }

    fun onGroceryNameChanged(name: String)

    fun onGroceryQuantityChanged(quantity: String)

    /** Bumps the quantity's leading amount up by one, leaving any unit in place. */
    fun onQuantityIncrementClicked()

    /** Bumps the quantity's leading amount down by one, never below one. */
    fun onQuantityDecrementClicked()

    fun onGroceryCheckedChanged(isChecked: Boolean)

    fun onAisleChanged(category: GroceryCategory)

    /**
     * Toggles a persistent "always file this item's name under the selected aisle" rule. When on,
     * every future item with the same name lands in this aisle by default.
     */
    fun onAlwaysFileHereToggled(enabled: Boolean)

    fun onSaveClicked()

    sealed class Model {
        object Loading : Model()

        /**
         * @param alwaysFileHere whether a name→aisle rule for this item's name currently points at
         *   the selected aisle (drives the "always file here" checkbox).
         */
        data class Loaded(val item: GroceryItem, val alwaysFileHere: Boolean = false) : Model()
    }

    sealed class Output {
        data object Finished : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, id: Long, output: Consumer<Output>): GroceryDetailBloc
    }
}

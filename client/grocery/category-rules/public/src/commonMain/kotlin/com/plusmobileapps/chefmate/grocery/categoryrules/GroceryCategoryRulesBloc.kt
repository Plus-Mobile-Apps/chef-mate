package com.plusmobileapps.chefmate.grocery.categoryrules

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.StateFlow

/**
 * Settings screen for managing persistent "always file <name> under <aisle>" rules. Each rule
 * retags every future grocery item whose parsed name matches, overriding the built-in
 * `IngredientParser` guess.
 */
interface GroceryCategoryRulesBloc : BackHandlerOwner, BackClickBloc, ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        GroceryCategoryRulesScreen(bloc = this, modifier = modifier)
    }

    fun onCreateClicked()

    fun onCreateCancelled()

    fun onCreateNameChanged(name: String)

    fun onCreateCategorySelected(category: GroceryCategory)

    fun onCreateSubmitted()

    fun onDeleteRequested(rule: Rule)

    fun onDeleteConfirmed()

    fun onDeleteDismissed()

    data class Model(
        val rules: ImmutableList<Rule> = persistentListOf(),
        val isLoading: Boolean = true,
        val createState: CreateState = CreateState.Hidden,
        val dialog: DialogState = DialogState.None,
    )

    /** One user-defined name→aisle rule. */
    data class Rule(val id: Long, val name: String, val category: GroceryCategory)

    sealed class CreateState {
        data object Hidden : CreateState()

        /** The in-progress new rule: a name and the aisle it will file under. */
        data class Editing(
            val name: String,
            val category: GroceryCategory = GroceryCategory.OTHER,
        ) : CreateState()
    }

    sealed class DialogState {
        data object None : DialogState()

        data class Delete(val target: Rule) : DialogState()
    }

    sealed class Output {
        data object Back : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): GroceryCategoryRulesBloc
    }
}

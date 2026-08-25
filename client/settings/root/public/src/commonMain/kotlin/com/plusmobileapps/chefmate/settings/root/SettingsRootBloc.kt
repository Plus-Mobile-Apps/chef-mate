package com.plusmobileapps.chefmate.settings.root

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.plusmobileapps.chefmate.BackClickBloc
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.grocery.autocomplete.GroceryAutocompleteBloc
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavOrderBloc
import com.plusmobileapps.chefmate.recipe.categories.RecipeCategoriesBloc
import com.plusmobileapps.chefmate.recipe.exporter.ExportRecipesBloc
import com.plusmobileapps.chefmate.recipe.importer.ImportRecipesBloc
import com.plusmobileapps.chefmate.settings.AppSettingsBloc
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.serialization.Serializable

interface SettingsRootBloc : BackHandlerOwner, BackClickBloc, ComposeScreen {
    val routerState: Value<ChildStack<*, Child>>

    @Composable
    override fun Content(modifier: Modifier) {
        SettingsRootScreen(bloc = this, modifier = modifier)
    }

    sealed class Child {

        abstract val bloc: ComposeScreen

        data class AppSettings(override val bloc: AppSettingsBloc) : Child()

        data class BottomNavOrder(override val bloc: BottomNavOrderBloc) : Child()

        data class ExportRecipes(override val bloc: ExportRecipesBloc) : Child()

        data class ImportRecipes(override val bloc: ImportRecipesBloc) : Child()

        data class RecipeCategories(override val bloc: RecipeCategoriesBloc) : Child()

        data class GroceryAutocomplete(override val bloc: GroceryAutocompleteBloc) : Child()

        data class GroceryCategoryRules(override val bloc: GroceryCategoryRulesBloc) : Child()
    }

    sealed class Output {
        data object Back : Output()
    }

    /** Which screen the settings stack opens on. */
    @Serializable
    sealed class Props {
        /** Land on the top-level app settings list (the normal entry point). */
        @Serializable data object Default : Props()

        /** Deep link straight to the saved autocomplete items, with app settings underneath. */
        @Serializable data object GroceryAutocomplete : Props()
    }

    fun interface Factory {
        fun create(
            context: BlocContext,
            props: Props,
            output: Consumer<Output>,
        ): SettingsRootBloc
    }
}

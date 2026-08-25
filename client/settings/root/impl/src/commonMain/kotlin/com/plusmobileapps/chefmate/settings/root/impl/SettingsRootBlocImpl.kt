@file:OptIn(DelicateDecomposeApi::class)

package com.plusmobileapps.chefmate.settings.root.impl

import com.arkivanov.decompose.DelicateDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.grocery.autocomplete.GroceryAutocompleteBloc
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavOrderBloc
import com.plusmobileapps.chefmate.recipe.categories.RecipeCategoriesBloc
import com.plusmobileapps.chefmate.recipe.exporter.ExportRecipesBloc
import com.plusmobileapps.chefmate.recipe.importer.ImportRecipesBloc
import com.plusmobileapps.chefmate.settings.AppSettingsBloc
import com.plusmobileapps.chefmate.settings.root.SettingsRootBloc
import com.plusmobileapps.chefmate.settings.root.SettingsRootBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.serialization.Serializable

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = SettingsRootBloc.Factory::class,
)
class SettingsRootBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val props: SettingsRootBloc.Props,
    @Assisted private val output: Consumer<Output>,
    private val appSettings: AppSettingsBloc.Factory,
    private val bottomNavOrder: BottomNavOrderBloc.Factory,
    private val importRecipes: ImportRecipesBloc.Factory,
    private val exportRecipes: ExportRecipesBloc.Factory,
    private val recipeCategories: RecipeCategoriesBloc.Factory,
    private val groceryAutocomplete: GroceryAutocompleteBloc.Factory,
    private val groceryCategoryRules: GroceryCategoryRulesBloc.Factory,
) : SettingsRootBloc, BlocContext by context {

    private val navigation = StackNavigation<Configuration>()

    private val stack =
        childStack(
            source = navigation,
            serializer = Configuration.serializer(),
            initialStack = { initialStackFor(props) },
            handleBackButton = true,
            key = "SettingsRootRouter",
            childFactory = ::createChild,
        )

    // Deep links keep AppSettings underneath the requested screen so Back returns there rather than
    // straight out of settings.
    private fun initialStackFor(props: SettingsRootBloc.Props): List<Configuration> =
        when (props) {
            SettingsRootBloc.Props.Default -> listOf(Configuration.AppSettings)
            SettingsRootBloc.Props.GroceryAutocomplete ->
                listOf(Configuration.AppSettings, Configuration.GroceryAutocomplete)
        }

    override val routerState: Value<ChildStack<*, SettingsRootBloc.Child>> = stack

    override fun onBackClicked() {
        navigation.pop()
    }

    private fun createChild(config: Configuration, context: BlocContext): SettingsRootBloc.Child =
        when (config) {
            Configuration.AppSettings ->
                SettingsRootBloc.Child.AppSettings(
                    bloc = appSettings.create(context = context, output = ::handleAppSettingsOutput)
                )

            Configuration.BottomNavOrder ->
                SettingsRootBloc.Child.BottomNavOrder(
                    bloc =
                        bottomNavOrder.create(
                            context = context,
                            output = ::handleBottomNavOrderOutput,
                        )
                )

            Configuration.ImportRecipes ->
                SettingsRootBloc.Child.ImportRecipes(
                    bloc =
                        importRecipes.create(
                            context = context,
                            output = ::handleImportRecipesOutput,
                        )
                )

            Configuration.ExportRecipes ->
                SettingsRootBloc.Child.ExportRecipes(
                    bloc =
                        exportRecipes.create(
                            context = context,
                            props = ExportRecipesBloc.Props.All,
                            output = ::handleExportRecipesOutput,
                        )
                )

            Configuration.RecipeCategories ->
                SettingsRootBloc.Child.RecipeCategories(
                    bloc =
                        recipeCategories.create(
                            context = context,
                            output = ::handleRecipeCategoriesOutput,
                        )
                )

            Configuration.GroceryAutocomplete ->
                SettingsRootBloc.Child.GroceryAutocomplete(
                    bloc =
                        groceryAutocomplete.create(
                            context = context,
                            output = ::handleGroceryAutocompleteOutput,
                        )
                )

            Configuration.GroceryCategoryRules ->
                SettingsRootBloc.Child.GroceryCategoryRules(
                    bloc =
                        groceryCategoryRules.create(
                            context = context,
                            output = ::handleGroceryCategoryRulesOutput,
                        )
                )
        }

    private fun handleAppSettingsOutput(output: AppSettingsBloc.Output) {
        when (output) {
            AppSettingsBloc.Output.Back -> this.output.onNext(Output.Back)
            AppSettingsBloc.Output.OpenBottomNavOrder ->
                navigation.bringToFront(Configuration.BottomNavOrder)
            AppSettingsBloc.Output.OpenImportRecipes ->
                navigation.bringToFront(Configuration.ImportRecipes)
            AppSettingsBloc.Output.OpenExportRecipes ->
                navigation.bringToFront(Configuration.ExportRecipes)
            AppSettingsBloc.Output.OpenRecipeCategories ->
                navigation.bringToFront(Configuration.RecipeCategories)
            AppSettingsBloc.Output.OpenGroceryAutocomplete ->
                navigation.bringToFront(Configuration.GroceryAutocomplete)
            AppSettingsBloc.Output.OpenGroceryCategoryRules ->
                navigation.bringToFront(Configuration.GroceryCategoryRules)
        }
    }

    private fun handleBottomNavOrderOutput(output: BottomNavOrderBloc.Output) {
        when (output) {
            BottomNavOrderBloc.Output.Back -> navigation.pop()
        }
    }

    private fun handleImportRecipesOutput(output: ImportRecipesBloc.Output) {
        when (output) {
            ImportRecipesBloc.Output.Back -> navigation.pop()
        }
    }

    private fun handleExportRecipesOutput(output: ExportRecipesBloc.Output) {
        when (output) {
            // Settings hosts the exporter without any list-side selection, so success and cancel
            // both just pop back to the Backup screen.
            ExportRecipesBloc.Output.Back,
            ExportRecipesBloc.Output.Finished -> navigation.pop()
        }
    }

    private fun handleRecipeCategoriesOutput(output: RecipeCategoriesBloc.Output) {
        when (output) {
            RecipeCategoriesBloc.Output.Back -> navigation.pop()
        }
    }

    private fun handleGroceryAutocompleteOutput(output: GroceryAutocompleteBloc.Output) {
        when (output) {
            GroceryAutocompleteBloc.Output.Back -> navigation.pop()
        }
    }

    private fun handleGroceryCategoryRulesOutput(output: GroceryCategoryRulesBloc.Output) {
        when (output) {
            GroceryCategoryRulesBloc.Output.Back -> navigation.pop()
        }
    }

    @Serializable
    private sealed class Configuration {
        @Serializable data object AppSettings : Configuration()

        @Serializable data object BottomNavOrder : Configuration()

        @Serializable data object ImportRecipes : Configuration()

        @Serializable data object ExportRecipes : Configuration()

        @Serializable data object RecipeCategories : Configuration()

        @Serializable data object GroceryAutocomplete : Configuration()

        @Serializable data object GroceryCategoryRules : Configuration()
    }
}

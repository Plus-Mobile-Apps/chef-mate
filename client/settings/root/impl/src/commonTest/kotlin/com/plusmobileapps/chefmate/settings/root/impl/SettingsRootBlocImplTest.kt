@file:Suppress("FunctionName")

package com.plusmobileapps.chefmate.settings.root.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.grocery.autocomplete.GroceryAutocompleteBloc
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavOrderBloc
import com.plusmobileapps.chefmate.recipe.categories.RecipeCategoriesBloc
import com.plusmobileapps.chefmate.recipe.exporter.ExportRecipesBloc
import com.plusmobileapps.chefmate.recipe.importer.ImportRecipesBloc
import com.plusmobileapps.chefmate.settings.AppSettingsBloc
import com.plusmobileapps.chefmate.settings.root.SettingsRootBloc
import com.plusmobileapps.chefmate.testing.TestBlocContext
import dev.mokkery.mock
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.instanceOf
import kotlin.test.Test

class SettingsRootBlocImplTest {
    val context = TestBlocContext.create()

    var appSettingsOutput: Consumer<AppSettingsBloc.Output> = Consumer {}
    var bottomNavOrderOutput: Consumer<BottomNavOrderBloc.Output> = Consumer {}
    var importRecipesOutput: Consumer<ImportRecipesBloc.Output> = Consumer {}
    var exportRecipesOutput: Consumer<ExportRecipesBloc.Output> = Consumer {}
    var recipeCategoriesOutput: Consumer<RecipeCategoriesBloc.Output> = Consumer {}
    var groceryAutocompleteOutput: Consumer<GroceryAutocompleteBloc.Output> = Consumer {}
    var groceryCategoryRulesOutput: Consumer<GroceryCategoryRulesBloc.Output> = Consumer {}

    var rootOutput: SettingsRootBloc.Output? = null

    val bloc = createBloc()

    // Each bloc needs its own context — Decompose registers the child stack under a fixed key, so
    // two blocs sharing one context would clash.
    private fun createBloc(
        props: SettingsRootBloc.Props = SettingsRootBloc.Props.Default,
        context: BlocContext = this.context,
    ) =
        SettingsRootBlocImpl(
            context = context,
            props = props,
            output = { rootOutput = it },
            appSettings = { _, output ->
                appSettingsOutput = output
                mock()
            },
            bottomNavOrder = { _, output ->
                bottomNavOrderOutput = output
                mock()
            },
            importRecipes = { _, output ->
                importRecipesOutput = output
                mock()
            },
            exportRecipes = { _, _, output ->
                exportRecipesOutput = output
                mock()
            },
            recipeCategories = { _, output ->
                recipeCategoriesOutput = output
                mock()
            },
            groceryAutocomplete = { _, output ->
                groceryAutocompleteOutput = output
                mock()
            },
            groceryCategoryRules = { _, output ->
                groceryCategoryRulesOutput = output
                mock()
            },
        )

    fun SettingsRootBloc.instance(): SettingsRootBloc.Child = routerState.value.active.instance

    @Test
    fun When_initialized_Then_app_settings_is_shown() {
        bloc.instance() should instanceOf<SettingsRootBloc.Child.AppSettings>()
        bloc.routerState.value.backStack.size shouldBe 0
    }

    @Test
    fun When_app_settings_opens_bottom_nav_order_Then_bottom_nav_order_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenBottomNavOrder)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.BottomNavOrder>()
        bloc.routerState.value.backStack.size shouldBe 1
    }

    @Test
    fun When_app_settings_opens_import_recipes_Then_import_recipes_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenImportRecipes)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.ImportRecipes>()
        bloc.routerState.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_bottom_nav_order_When_it_outputs_back_Then_app_settings_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenBottomNavOrder)
        bottomNavOrderOutput.onNext(BottomNavOrderBloc.Output.Back)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.AppSettings>()
    }

    @Test
    fun Given_import_recipes_When_it_outputs_back_Then_app_settings_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenImportRecipes)
        importRecipesOutput.onNext(ImportRecipesBloc.Output.Back)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.AppSettings>()
    }

    @Test
    fun When_app_settings_opens_export_recipes_Then_export_recipes_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenExportRecipes)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.ExportRecipes>()
        bloc.routerState.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_export_recipes_When_it_outputs_back_Then_app_settings_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenExportRecipes)
        exportRecipesOutput.onNext(ExportRecipesBloc.Output.Back)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.AppSettings>()
    }

    @Test
    fun When_app_settings_opens_recipe_categories_Then_recipe_categories_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenRecipeCategories)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.RecipeCategories>()
        bloc.routerState.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_recipe_categories_When_it_outputs_back_Then_app_settings_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenRecipeCategories)
        recipeCategoriesOutput.onNext(RecipeCategoriesBloc.Output.Back)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.AppSettings>()
    }

    @Test
    fun When_app_settings_opens_grocery_autocomplete_Then_grocery_autocomplete_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenGroceryAutocomplete)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.GroceryAutocomplete>()
        bloc.routerState.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_grocery_autocomplete_When_it_outputs_back_Then_app_settings_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenGroceryAutocomplete)
        groceryAutocompleteOutput.onNext(GroceryAutocompleteBloc.Output.Back)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.AppSettings>()
    }

    @Test
    fun When_started_with_grocery_autocomplete_props_Then_it_opens_over_app_settings() {
        val deepLinked =
            createBloc(SettingsRootBloc.Props.GroceryAutocomplete, TestBlocContext.create())
        deepLinked.instance() should instanceOf<SettingsRootBloc.Child.GroceryAutocomplete>()
        // App settings sits underneath so Back returns there rather than out of settings.
        deepLinked.routerState.value.backStack.size shouldBe 1
    }

    @Test
    fun When_app_settings_opens_grocery_category_rules_Then_grocery_category_rules_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenGroceryCategoryRules)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.GroceryCategoryRules>()
        bloc.routerState.value.backStack.size shouldBe 1
    }

    @Test
    fun Given_grocery_category_rules_When_it_outputs_back_Then_app_settings_is_shown() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.OpenGroceryCategoryRules)
        groceryCategoryRulesOutput.onNext(GroceryCategoryRulesBloc.Output.Back)
        bloc.instance() should instanceOf<SettingsRootBloc.Child.AppSettings>()
    }

    @Test
    fun When_app_settings_outputs_back_Then_root_back_is_emitted() {
        appSettingsOutput.onNext(AppSettingsBloc.Output.Back)
        rootOutput shouldBe SettingsRootBloc.Output.Back
    }
}

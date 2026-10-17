package com.plusmobileapps.chefmate.grocery.categoryrules

import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackHandler
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.CreateState
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.Model
import com.plusmobileapps.chefmate.grocery.categoryrules.GroceryCategoryRulesBloc.Rule
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow

private fun bloc(model: Model): GroceryCategoryRulesBloc =
    object : GroceryCategoryRulesBloc {
        override val backHandler: BackHandler = BackDispatcher()
        override val state = MutableStateFlow(model)

        override fun onBackClicked() = Unit

        override fun onCreateClicked() = Unit

        override fun onCreateCancelled() = Unit

        override fun onCreateNameChanged(name: String) = Unit

        override fun onCreateCategorySelected(category: GroceryCategory) = Unit

        override fun onCreateSubmitted() = Unit

        override fun onDeleteRequested(rule: Rule) = Unit

        override fun onDeleteConfirmed() = Unit

        override fun onDeleteDismissed() = Unit
    }

private val sampleRules =
    persistentListOf(
        Rule(id = 1L, name = "Cold brew", category = GroceryCategory.BEVERAGES),
        Rule(id = 2L, name = "Paper towels", category = GroceryCategory.OTHER),
        Rule(id = 3L, name = "Protein powder", category = GroceryCategory.BAKING),
    )

val previewGroceryCategoryRulesBloc: GroceryCategoryRulesBloc =
    bloc(Model(rules = sampleRules, isLoading = false))

val previewGroceryCategoryRulesBlocEmpty: GroceryCategoryRulesBloc =
    bloc(Model(rules = persistentListOf(), isLoading = false))

val previewGroceryCategoryRulesBlocCreating: GroceryCategoryRulesBloc =
    bloc(
        Model(
            rules = sampleRules,
            isLoading = false,
            createState =
                CreateState.Editing(name = "Sparkling water", category = GroceryCategory.BEVERAGES),
        )
    )

val previewGroceryCategoryRulesBlocDeleteDialog: GroceryCategoryRulesBloc =
    bloc(
        Model(
            rules = sampleRules,
            isLoading = false,
            dialog = GroceryCategoryRulesBloc.DialogState.Delete(sampleRules.first()),
        )
    )

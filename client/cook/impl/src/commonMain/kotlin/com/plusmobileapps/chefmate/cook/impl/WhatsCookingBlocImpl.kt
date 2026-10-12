package com.plusmobileapps.chefmate.cook.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.cook.WhatsCookingBloc
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = WhatsCookingBloc.Factory::class,
)
class WhatsCookingBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<WhatsCookingBloc.Output>,
    viewModelFactory: () -> WhatsCookingViewModel,
) : WhatsCookingBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<WhatsCookingBloc.Model> =
        viewModel.state.mapState { vm ->
            WhatsCookingBloc.Model(
                recipes = vm.recipes.toImmutableList(),
                isSelectMode = vm.isSelectMode,
                selectedRecipeIds = vm.selectedRecipeIds,
            )
        }

    override fun onRecipeClicked(recipeId: Long) {
        output.onNext(WhatsCookingBloc.Output.RecipeSelected(recipeId))
    }

    override fun onSelectModeToggled() {
        viewModel.toggleSelectMode()
    }

    override fun onSelectionToggled(recipeId: Long) {
        viewModel.toggleSelection(recipeId)
    }

    override fun onDeleteSelectedClicked() {
        viewModel.deleteSelected()
    }

    override fun onCloseClicked() {
        output.onNext(WhatsCookingBloc.Output.Closed)
    }
}

package com.plusmobileapps.chefmate.recipe.core.impl.addmeal

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.chefmate.recipe.core.addmeal.RecipePickerBloc
import com.plusmobileapps.chefmate.recipe.core.addmeal.RecipePickerBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = RecipePickerBloc.Factory::class,
)
class RecipePickerBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    private val viewModelFactory: () -> RecipePickerViewModel,
) : RecipePickerBloc, BlocContext by context {

    private val viewModel: RecipePickerViewModel = instanceKeeper.getViewModel {
        viewModelFactory()
    }

    override val state: StateFlow<RecipePickerBloc.Model> =
        viewModel.state.mapState {
            RecipePickerBloc.Model(
                recipes = it.displayRecipes.toImmutableList(),
                searchQuery = it.searchQuery,
                isLoading = it.isLoading,
            )
        }

    override fun onSearchQueryChanged(query: String) {
        viewModel.updateSearchQuery(query)
    }

    override fun onRecipeSelected(item: RecipePickerBloc.RecipePickerItem) {
        output.onNext(Output.RecipeSelected(recipeId = item.id))
    }
}

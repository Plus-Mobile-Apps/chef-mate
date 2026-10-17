package com.plusmobileapps.chefmate.grocery.core.impl.detail

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.grocery.core.detail.GroceryDetailBloc
import com.plusmobileapps.chefmate.grocery.core.detail.GroceryDetailBloc.Output
import com.plusmobileapps.chefmate.grocery.data.GroceryCategory
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = GroceryDetailBloc.Factory::class,
)
class GroceryDetailBlocImpl(
    @Assisted context: BlocContext,
    @Assisted id: Long,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: GroceryDetailViewModel.Factory,
) : GroceryDetailBloc, BlocContext by context {

    private val scope = createScope()

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory.create(id) }

    override val models: StateFlow<GroceryDetailBloc.Model> =
        viewModel.state.mapState {
            if (it.isLoading) {
                GroceryDetailBloc.Model.Loading
            } else {
                GroceryDetailBloc.Model.Loaded(
                    item = it.groceryItem,
                    alwaysFileHere = it.alwaysFileHere,
                )
            }
        }

    init {
        scope.launch {
            viewModel.outputs.collect {
                when (it) {
                    GroceryDetailViewModel.Output.Finished -> output.onNext(Output.Finished)
                }
            }
        }
    }

    override fun onGroceryNameChanged(name: String) {
        viewModel.onGroceryNameChanged(name)
    }

    override fun onGroceryQuantityChanged(quantity: String) {
        viewModel.onGroceryQuantityChanged(quantity)
    }

    override fun onQuantityIncrementClicked() {
        viewModel.onQuantityIncrement()
    }

    override fun onQuantityDecrementClicked() {
        viewModel.onQuantityDecrement()
    }

    override fun onGroceryCheckedChanged(isChecked: Boolean) {
        viewModel.onGroceryCheckedChanged(isChecked)
    }

    override fun onAisleChanged(category: GroceryCategory) {
        viewModel.onAisleChanged(category)
    }

    override fun onAlwaysFileHereToggled(enabled: Boolean) {
        viewModel.onAlwaysFileHereToggled(enabled)
    }

    override fun onSaveClicked() {
        viewModel.save()
    }

    override fun onBackClicked() {
        output.onNext(Output.Finished)
    }
}

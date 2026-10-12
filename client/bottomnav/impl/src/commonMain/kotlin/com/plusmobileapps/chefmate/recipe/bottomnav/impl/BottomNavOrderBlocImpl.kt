package com.plusmobileapps.chefmate.recipe.bottomnav.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavOrderBloc
import com.plusmobileapps.chefmate.recipe.bottomnav.BottomNavOrderBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = BottomNavOrderBloc.Factory::class,
)
class BottomNavOrderBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: () -> BottomNavOrderViewModel,
) : BottomNavOrderBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<BottomNavOrderBloc.Model> = viewModel.state

    override fun onMove(from: Int, to: Int) {
        viewModel.move(from, to)
    }

    override fun onSave() {
        viewModel.save()
        output.onNext(Output.Back)
    }

    override fun onBack() {
        output.onNext(Output.Back)
    }
}

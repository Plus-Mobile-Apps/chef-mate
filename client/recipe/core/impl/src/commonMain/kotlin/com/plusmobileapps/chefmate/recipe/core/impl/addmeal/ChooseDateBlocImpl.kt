package com.plusmobileapps.chefmate.recipe.core.impl.addmeal

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseDateBloc
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseDateBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = ChooseDateBloc.Factory::class,
)
class ChooseDateBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    private val viewModelFactory: () -> ChooseDateViewModel,
) : ChooseDateBloc, BlocContext by context {

    private val viewModel: ChooseDateViewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<ChooseDateBloc.Model> =
        viewModel.state.mapState {
            ChooseDateBloc.Model(
                firstDayOfMonth = it.firstDayOfMonth,
                daysWithMeals = it.daysWithMeals,
                monthLabel = it.monthLabel,
                selectedDate = it.selectedDate,
                formattedSelectedDate = it.formattedSelectedDate,
                selectedDayMeals = it.selectedDayMeals.toImmutableList(),
            )
        }

    override fun onDaySelected(date: LocalDate) {
        viewModel.onDaySelected(date)
    }

    override fun onNextClicked() {
        val selectedDate = viewModel.state.value.selectedDate ?: return
        output.onNext(Output.DateSelected(selectedDate))
    }

    override fun onPreviousMonth() {
        viewModel.onPreviousMonth()
    }

    override fun onNextMonth() {
        viewModel.onNextMonth()
    }

    override fun onTodayClicked() {
        viewModel.onTodayClicked()
    }

    override fun onBackClicked() {
        output.onNext(Output.Back)
    }
}

package com.plusmobileapps.chefmate.recipe.core.impl.addmeal

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseDateBloc
import com.plusmobileapps.chefmate.recipe.core.addmeal.ChooseDateBloc.Output
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = ChooseDateBloc.Factory::class,
)
class ChooseDateBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val recipeId: Long,
    @Assisted private val output: Consumer<Output>,
    private val viewModelFactory: ChooseDateViewModel.Factory,
) : ChooseDateBloc, BlocContext by context {

    private val scope = createScope()

    private val viewModel: ChooseDateViewModel = instanceKeeper.getViewModel {
        viewModelFactory.create(recipeId)
    }

    override val state: StateFlow<ChooseDateBloc.Model> =
        viewModel.state.mapState {
            ChooseDateBloc.Model(
                firstDayOfMonth = it.firstDayOfMonth,
                daysWithMeals = it.daysWithMeals,
                monthLabel = it.monthLabel,
                selectedDate = it.selectedDate,
                formattedSelectedDate = it.formattedSelectedDate,
                selectedDayMeals = it.selectedDayMeals.toImmutableList(),
                selectedMealType = it.selectedMealType,
                isSaving = it.isSaving,
                recipeTitle = it.recipeTitle,
            )
        }

    init {
        scope.launch {
            viewModel.output.collect {
                when (it) {
                    ChooseDateViewModel.Output.Finished -> output.onNext(Output.Finished)
                }
            }
        }
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

    override fun onMealTypeSelected(mealType: MealType) {
        viewModel.onMealTypeSelected(mealType)
    }

    override fun onSaveClicked() {
        viewModel.save()
    }

    override fun onBackClicked() {
        output.onNext(Output.Back)
    }
}

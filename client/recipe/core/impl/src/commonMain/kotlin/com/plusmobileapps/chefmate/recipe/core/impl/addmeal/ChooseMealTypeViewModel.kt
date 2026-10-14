package com.plusmobileapps.chefmate.recipe.core.impl.addmeal

import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.meal.data.MealPlanScopeRepository
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
class ChooseMealTypeViewModel(
    @Assisted private val recipeId: Long,
    @Assisted private val date: String,
    @Main mainContext: CoroutineContext,
    private val recipeRepository: RecipeRepository,
    private val mealPlanRepository: MealPlanRepository,
    private val scopeRepository: MealPlanScopeRepository,
) : ViewModel(mainContext) {

    @AssistedFactory
    fun interface Factory {
        fun create(recipeId: Long, date: String): ChooseMealTypeViewModel
    }

    private val _output = Channel<Output>(Channel.BUFFERED)
    val output: Flow<Output> = _output.receiveAsFlow()

    private val _state = MutableStateFlow(State(selectedDate = date))
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        loadRecipe()
    }

    override fun onCleared() {
        super.onCleared()
        _output.close()
    }

    fun onMealTypeSelected(mealType: MealType) {
        _state.update { it.copy(selectedMealType = mealType) }
    }

    fun save() {
        val currentState = _state.value
        if (currentState.isSaving) return
        _state.update { it.copy(isSaving = true) }
        scope.launch {
            mealPlanRepository.addMeal(
                recipeId = recipeId,
                date = currentState.selectedDate,
                mealType = currentState.selectedMealType,
                // The plan picked on the previous (date) step, or the one the planner is showing.
                scope = scopeRepository.selectedScope.value,
            )
            _output.send(Output.Finished)
        }
    }

    private fun loadRecipe() {
        scope.launch {
            val recipe = recipeRepository.getRecipe(recipeId).first() ?: return@launch
            _state.update { it.copy(recipeTitle = recipe.title) }
        }
    }

    data class State(
        val isSaving: Boolean = false,
        val selectedDate: String = "",
        val selectedMealType: MealType = MealType.DINNER,
        val recipeTitle: String = "",
    )

    sealed class Output {
        data object Finished : Output()
    }
}

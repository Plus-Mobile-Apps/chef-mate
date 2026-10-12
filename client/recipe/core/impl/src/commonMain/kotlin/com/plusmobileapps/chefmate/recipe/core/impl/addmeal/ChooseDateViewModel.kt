package com.plusmobileapps.chefmate.recipe.core.impl.addmeal

import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.recipe.data.RecipeRepository
import com.plusmobileapps.chefmate.util.DateTimeUtil
import com.russhwolf.settings.Settings
import com.russhwolf.settings.string
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@AssistedInject
class ChooseDateViewModel(
    @Assisted private val recipeId: Long,
    @Main mainContext: CoroutineContext,
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val dateTimeUtil: DateTimeUtil,
    settings: Settings,
) : ViewModel(mainContext) {

    @AssistedFactory
    fun interface Factory {
        fun create(recipeId: Long): ChooseDateViewModel
    }

    private val _output = Channel<Output>(Channel.BUFFERED)
    val output: Flow<Output> = _output.receiveAsFlow()

    private var lastSelectedDatePref by settings.string(KEY_LAST_SELECTED_DATE, "")

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var observeJob: Job? = null

    init {
        val today = dateTimeUtil.today()
        val initialDate = resolveInitialDate(today)
        val firstOfMonth = LocalDate(initialDate.year, initialDate.month, 1)
        val mealsForDay = emptyList<MealPlanItem>()
        _state.update {
            it.copy(
                firstDayOfMonth = firstOfMonth,
                monthLabel = buildMonthLabel(firstOfMonth),
                selectedDate = initialDate,
                formattedSelectedDate = dateTimeUtil.formatMediumDate(initialDate),
                selectedDayMeals = mealsForDay,
            )
        }
        observeMeals()
        loadRecipe()
    }

    override fun onCleared() {
        super.onCleared()
        _output.close()
    }

    private fun resolveInitialDate(today: LocalDate): LocalDate {
        val saved = lastSelectedDatePref
        if (saved.isNotEmpty()) {
            try {
                val parsed = LocalDate.parse(saved)
                if (parsed >= today) return parsed
            } catch (_: Exception) {}
        }
        return today
    }

    fun onDaySelected(date: LocalDate) {
        lastSelectedDatePref = date.toString()
        _state.update { current ->
            val mealsForDay = current.allMeals.filter { it.date == date.toString() }
            current.copy(
                selectedDate = date,
                formattedSelectedDate = dateTimeUtil.formatMediumDate(date),
                selectedDayMeals = mealsForDay,
            )
        }
    }

    fun onPreviousMonth() {
        val current = _state.value
        val newFirst = current.firstDayOfMonth.minus(1, DateTimeUnit.MONTH)
        _state.update {
            it.copy(
                firstDayOfMonth = newFirst,
                monthLabel = buildMonthLabel(newFirst),
                selectedDate = null,
                formattedSelectedDate = "",
                selectedDayMeals = emptyList(),
            )
        }
        observeMeals()
    }

    fun onNextMonth() {
        val current = _state.value
        val newFirst = current.firstDayOfMonth.plus(1, DateTimeUnit.MONTH)
        _state.update {
            it.copy(
                firstDayOfMonth = newFirst,
                monthLabel = buildMonthLabel(newFirst),
                selectedDate = null,
                formattedSelectedDate = "",
                selectedDayMeals = emptyList(),
            )
        }
        observeMeals()
    }

    fun onTodayClicked() {
        val today = dateTimeUtil.today()
        val firstOfMonth = LocalDate(today.year, today.month, 1)
        lastSelectedDatePref = today.toString()
        _state.update {
            it.copy(
                firstDayOfMonth = firstOfMonth,
                monthLabel = buildMonthLabel(firstOfMonth),
                selectedDate = today,
                formattedSelectedDate = dateTimeUtil.formatMediumDate(today),
                selectedDayMeals = emptyList(),
            )
        }
        observeMeals()
    }

    fun onMealTypeSelected(mealType: MealType) {
        _state.update { it.copy(selectedMealType = mealType) }
    }

    fun save() {
        val currentState = _state.value
        val date = currentState.selectedDate ?: return
        if (currentState.isSaving) return
        _state.update { it.copy(isSaving = true) }
        scope.launch {
            mealPlanRepository.addMeal(
                recipeId = recipeId,
                date = date.toString(),
                mealType = currentState.selectedMealType,
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

    private fun observeMeals() {
        observeJob?.cancel()
        observeJob = scope.launch {
            val first = _state.value.firstDayOfMonth
            val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            mealPlanRepository.getMealsByDateRange(first.toString(), last.toString()).collect {
                meals ->
                val daysWithMeals = meals.map { it.date }.toSet()
                _state.update { current ->
                    val selectedDayMeals =
                        current.selectedDate?.let { date ->
                            meals.filter { it.date == date.toString() }
                        } ?: emptyList()
                    current.copy(
                        allMeals = meals,
                        daysWithMeals = daysWithMeals,
                        selectedDayMeals = selectedDayMeals,
                    )
                }
            }
        }
    }

    private fun buildMonthLabel(date: LocalDate): String {
        val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$monthName ${date.year}"
    }

    data class State(
        val firstDayOfMonth: LocalDate = LocalDate(2000, 1, 1),
        val daysWithMeals: Set<String> = emptySet(),
        val monthLabel: String = "",
        val selectedDate: LocalDate? = null,
        val formattedSelectedDate: String = "",
        val selectedDayMeals: List<MealPlanItem> = emptyList(),
        val allMeals: List<MealPlanItem> = emptyList(),
        val selectedMealType: MealType = MealType.DINNER,
        val isSaving: Boolean = false,
        val recipeTitle: String = "",
    )

    sealed class Output {
        data object Finished : Output()
    }

    companion object {
        private const val KEY_LAST_SELECTED_DATE = "choose_date_last_selected"
    }
}

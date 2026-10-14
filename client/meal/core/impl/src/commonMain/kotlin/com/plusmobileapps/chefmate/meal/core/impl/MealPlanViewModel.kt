package com.plusmobileapps.chefmate.meal.core.impl

import chefmate.client.meal.core.public.generated.resources.Res
import chefmate.client.meal.core.public.generated.resources.meal_plan_added_to_cook_mode_multiple
import chefmate.client.meal.core.public.generated.resources.meal_plan_added_to_cook_mode_single
import chefmate.client.meal.core.public.generated.resources.meal_plan_replaced_cook_mode_multiple
import chefmate.client.meal.core.public.generated.resources.meal_plan_replaced_cook_mode_single
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.combineStates
import com.plusmobileapps.chefmate.cook.data.CookingSessionRepository
import com.plusmobileapps.chefmate.di.CoachMarkController
import com.plusmobileapps.chefmate.di.CoachMarkId
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.meal.core.MealPlanBloc
import com.plusmobileapps.chefmate.meal.data.FamilyPlan
import com.plusmobileapps.chefmate.meal.data.MealPlanItem
import com.plusmobileapps.chefmate.meal.data.MealPlanRepository
import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.plusmobileapps.chefmate.meal.data.MealPlanScopeRepository
import com.plusmobileapps.chefmate.meal.data.MealType
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.toast.ToastService
import com.plusmobileapps.chefmate.util.DateTimeUtil
import com.russhwolf.settings.Settings
import com.russhwolf.settings.string
import dev.zacsweers.metro.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.StringResource

@Inject
class MealPlanViewModel(
    @Main mainContext: CoroutineContext,
    private val repository: MealPlanRepository,
    private val scopeRepository: MealPlanScopeRepository,
    private val cookingSessionRepository: CookingSessionRepository,
    private val dateTimeUtil: DateTimeUtil,
    private val coachMarkController: CoachMarkController,
    private val toastService: ToastService,
    settings: Settings,
) : ViewModel(mainContext) {

    // Persisted so the day/week/month tab the user last picked is still selected next launch.
    private var viewModePref by settings.string(KEY_VIEW_MODE, MealPlanBloc.ViewMode.DAY.name)

    private val initialViewMode =
        MealPlanBloc.ViewMode.entries.find { it.name == viewModePref } ?: MealPlanBloc.ViewMode.DAY

    private val _state = MutableStateFlow(State(viewMode = initialViewMode))

    // Fold the shared controller's active mark into state so the screen shows at most one coach
    // mark at a time across the whole app.
    val state: StateFlow<State> =
        combineStates(_state, coachMarkController.activeCoachMark) { state, activeCoachMark ->
            state.copy(activeCoachMark = activeCoachMark)
        }

    private var observeJob: Job? = null

    init {
        // Queue every meal-plan coach mark in order; the controller shows them one at a time.
        CoachMarkId.mealPlanSequence.forEach(coachMarkController::request)
        val today = dateTimeUtil.today()
        _state.update {
            it.copy(
                currentDate = today,
                selectedMonthDay = today,
                planScope = scopeRepository.selectedScope.value,
            )
        }
        observeMeals()
        scope.launch {
            scopeRepository.selectedScope.collect { planScope ->
                if (planScope == _state.value.planScope) return@collect
                _state.update { it.copy(planScope = planScope, isLoading = true) }
                observeMeals()
            }
        }
        scope.launch {
            scopeRepository.familyPlans.collect { plans ->
                _state.update { it.copy(familyPlans = plans) }
            }
        }
        scope.launch {
            cookingSessionRepository.observeRecipeIds().collect { ids ->
                _state.update { it.copy(cookingRecipeIds = ids) }
            }
        }
    }

    fun selectViewMode(mode: MealPlanBloc.ViewMode) {
        viewModePref = mode.name
        _state.update { it.copy(viewMode = mode) }
        observeMeals()
    }

    fun goNext() {
        val current = _state.value
        val newDate =
            when (current.viewMode) {
                MealPlanBloc.ViewMode.DAY -> current.currentDate.plus(1, DateTimeUnit.DAY)
                MealPlanBloc.ViewMode.WEEK -> current.currentDate.plus(7, DateTimeUnit.DAY)
                MealPlanBloc.ViewMode.MONTH -> {
                    val firstOfMonth =
                        LocalDate(current.currentDate.year, current.currentDate.month, 1)
                    firstOfMonth.plus(1, DateTimeUnit.MONTH)
                }
            }
        val newSelectedMonthDay =
            if (current.viewMode == MealPlanBloc.ViewMode.MONTH) newDate
            else current.selectedMonthDay
        _state.update { it.copy(currentDate = newDate, selectedMonthDay = newSelectedMonthDay) }
        observeMeals()
    }

    fun goPrevious() {
        val current = _state.value
        val newDate =
            when (current.viewMode) {
                MealPlanBloc.ViewMode.DAY -> current.currentDate.minus(1, DateTimeUnit.DAY)
                MealPlanBloc.ViewMode.WEEK -> current.currentDate.minus(7, DateTimeUnit.DAY)
                MealPlanBloc.ViewMode.MONTH -> {
                    val firstOfMonth =
                        LocalDate(current.currentDate.year, current.currentDate.month, 1)
                    firstOfMonth.minus(1, DateTimeUnit.MONTH)
                }
            }
        val newSelectedMonthDay =
            if (current.viewMode == MealPlanBloc.ViewMode.MONTH) newDate
            else current.selectedMonthDay
        _state.update { it.copy(currentDate = newDate, selectedMonthDay = newSelectedMonthDay) }
        observeMeals()
    }

    fun onMonthDaySelected(date: LocalDate) {
        _state.update { it.copy(selectedMonthDay = date) }
    }

    /**
     * Mark a coach mark seen, but only if it's the one currently showing, so tapping a control
     * dismisses its own tip without consuming tips further down the queue.
     */
    fun dismissCoachMark(id: String) {
        if (coachMarkController.activeCoachMark.value == id) {
            coachMarkController.dismiss(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Leaving the screen without dismissing frees the queue so other coach marks can show.
        CoachMarkId.mealPlanSequence.forEach(coachMarkController::release)
    }

    /**
     * Reloads the user's families, and pulls a family plan's latest meals — other members add to
     * it, and nothing else syncs between sign-ins.
     */
    fun onResume() {
        scope.launch {
            scopeRepository.refresh()
            if (_state.value.planScope is MealPlanScope.Family) {
                repository.syncAllUnsynced()
            }
        }
    }

    fun openPlanPicker() {
        _state.update { it.copy(isPlanPickerOpen = true) }
    }

    fun dismissPlanPicker() {
        _state.update { it.copy(isPlanPickerOpen = false) }
    }

    fun selectPlan(familyId: String?) {
        _state.update { it.copy(isPlanPickerOpen = false) }
        scopeRepository.select(
            if (familyId == null) MealPlanScope.Personal else MealPlanScope.Family(familyId)
        )
    }

    fun requestRemoveMeal(item: MealPlanItem) {
        _state.update { it.copy(mealToDelete = item) }
    }

    fun confirmRemoveMeal() {
        val item = _state.value.mealToDelete ?: return
        _state.update { it.copy(mealToDelete = null) }
        scope.launch { repository.removeMeal(item.id) }
    }

    fun dismissRemoveMeal() {
        _state.update { it.copy(mealToDelete = null) }
    }

    fun onSyncClicked() {
        scope.launch {
            _state.update { it.copy(isSyncing = true) }
            try {
                repository.syncAllUnsynced()
            } finally {
                _state.update { it.copy(isSyncing = false) }
            }
        }
    }

    fun requestReplaceCookMode(meals: List<MealPlanItem>) {
        if (meals.isEmpty()) return
        _state.update { it.copy(pendingReplaceCookMode = meals) }
    }

    fun confirmReplaceCookMode() {
        val meals = _state.value.pendingReplaceCookMode ?: return
        _state.update { it.copy(pendingReplaceCookMode = null) }
        toastService.show(replacedCookModeMessage(meals))
        val recipeIds = meals.map { it.recipeId }.distinct()
        scope.launch { cookingSessionRepository.replaceAll(recipeIds) }
    }

    fun dismissReplaceCookMode() {
        _state.update { it.copy(pendingReplaceCookMode = null) }
    }

    fun addToCookMode(meals: List<MealPlanItem>) {
        val recipeIds = meals.map { it.recipeId }.distinct()
        if (recipeIds.isEmpty()) return
        toastService.show(addedToCookModeMessage(meals))
        scope.launch {
            recipeIds.forEach { cookingSessionRepository.start(it) }
            cookingSessionRepository.markSelected(recipeIds.first())
        }
    }

    private fun addedToCookModeMessage(meals: List<MealPlanItem>): TextData =
        cookModeMessage(
            meals,
            single = Res.string.meal_plan_added_to_cook_mode_single,
            multiple = Res.string.meal_plan_added_to_cook_mode_multiple,
        )

    private fun replacedCookModeMessage(meals: List<MealPlanItem>): TextData =
        cookModeMessage(
            meals,
            single = Res.string.meal_plan_replaced_cook_mode_single,
            multiple = Res.string.meal_plan_replaced_cook_mode_multiple,
        )

    private fun cookModeMessage(
        meals: List<MealPlanItem>,
        single: StringResource,
        multiple: StringResource,
    ): TextData {
        val uniqueByRecipe = meals.distinctBy { it.recipeId }
        return if (uniqueByRecipe.size == 1) {
            PhraseModel(single, "name" to FixedString(uniqueByRecipe.first().recipeTitle))
        } else {
            PhraseModel(multiple, "count" to FixedString(uniqueByRecipe.size.toString()))
        }
    }

    fun showDoneCookingDialog() {
        _state.update { it.copy(showDoneCookingDialog = true) }
    }

    fun dismissDoneCookingDialog() {
        _state.update { it.copy(showDoneCookingDialog = false) }
    }

    fun confirmDoneCooking() {
        _state.update { it.copy(showDoneCookingDialog = false) }
        scope.launch { cookingSessionRepository.stopAll() }
    }

    fun getDateLabel(): String {
        val current = _state.value
        return when (current.viewMode) {
            MealPlanBloc.ViewMode.DAY -> dateTimeUtil.formatLocalDate(current.currentDate)
            MealPlanBloc.ViewMode.WEEK -> {
                val (start, end) = getWeekRange(current.currentDate)
                val startStr = dateTimeUtil.formatLocalDate(start)
                val endStr = dateTimeUtil.formatLocalDate(end)
                "$startStr - $endStr"
            }
            MealPlanBloc.ViewMode.MONTH -> {
                val monthName =
                    current.currentDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
                "$monthName ${current.currentDate.year}"
            }
        }
    }

    fun buildDayMeals(meals: List<MealPlanItem>): MealPlanBloc.DayMeals =
        MealPlanBloc.DayMeals(
            breakfast = meals.filter { it.mealType == MealType.BREAKFAST }.toImmutableList(),
            lunch = meals.filter { it.mealType == MealType.LUNCH }.toImmutableList(),
            dinner = meals.filter { it.mealType == MealType.DINNER }.toImmutableList(),
            snacks = meals.filter { it.mealType == MealType.SNACKS }.toImmutableList(),
        )

    fun buildWeekMeals(meals: List<MealPlanItem>): List<MealPlanBloc.DayGroup> {
        val (start, _) = getWeekRange(_state.value.currentDate)
        return (0..6).map { dayOffset ->
            val date = start.plus(dayOffset, DateTimeUnit.DAY)
            val dateStr = date.toString()
            val dayMeals = meals.filter { it.date == dateStr }
            MealPlanBloc.DayGroup(
                dateLabel = FixedString(dateTimeUtil.formatShortDayDate(date)),
                meals = dayMeals.toImmutableList(),
            )
        }
    }

    fun buildMonthModel(
        meals: List<MealPlanItem>,
        selectedDay: LocalDate,
    ): MealPlanBloc.MonthModel {
        val current = _state.value
        val firstDayOfMonth = LocalDate(current.currentDate.year, current.currentDate.month, 1)
        val daysWithMeals = meals.map { it.date }.toSet()
        val selectedDayMeals = meals.filter { it.date == selectedDay.toString() }
        return MealPlanBloc.MonthModel(
            firstDayOfMonth = firstDayOfMonth,
            selectedDay = selectedDay,
            daysWithMeals = daysWithMeals,
            selectedDayMeals = buildDayMeals(selectedDayMeals),
        )
    }

    private fun observeMeals() {
        observeJob?.cancel()
        observeJob = scope.launch {
            val current = _state.value
            val planScope = current.planScope
            val flow =
                when (current.viewMode) {
                    MealPlanBloc.ViewMode.DAY ->
                        repository.getMealsByDate(current.currentDate.toString(), planScope)
                    MealPlanBloc.ViewMode.WEEK -> {
                        val (start, end) = getWeekRange(current.currentDate)
                        repository.getMealsByDateRange(start.toString(), end.toString(), planScope)
                    }
                    MealPlanBloc.ViewMode.MONTH -> {
                        val (start, end) = getMonthRange(current.currentDate)
                        repository.getMealsByDateRange(start.toString(), end.toString(), planScope)
                    }
                }
            flow.collect { meals -> _state.update { it.copy(isLoading = false, meals = meals) } }
        }
    }

    private fun getWeekRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val daysFromSunday = date.dayOfWeek.ordinal.let { if (it == 6) 0 else it + 1 }
        val start = date.minus(daysFromSunday, DateTimeUnit.DAY)
        val end = start.plus(6, DateTimeUnit.DAY)
        return start to end
    }

    private fun getMonthRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        val start = LocalDate(date.year, date.month, 1)
        val end = start.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        return start to end
    }

    data class State(
        val isLoading: Boolean = true,
        val isSyncing: Boolean = false,
        val viewMode: MealPlanBloc.ViewMode = MealPlanBloc.ViewMode.DAY,
        val currentDate: LocalDate = LocalDate(2000, 1, 1),
        val selectedMonthDay: LocalDate = LocalDate(2000, 1, 1),
        val meals: List<MealPlanItem> = emptyList(),
        val mealToDelete: MealPlanItem? = null,
        val cookingRecipeIds: List<Long> = emptyList(),
        val showDoneCookingDialog: Boolean = false,
        val pendingReplaceCookMode: List<MealPlanItem>? = null,
        val activeCoachMark: String? = null,
        val planScope: MealPlanScope = MealPlanScope.Personal,
        val familyPlans: List<FamilyPlan> = emptyList(),
        val isPlanPickerOpen: Boolean = false,
    )

    companion object {
        private const val KEY_VIEW_MODE = "meal_plan_view_mode"
    }
}

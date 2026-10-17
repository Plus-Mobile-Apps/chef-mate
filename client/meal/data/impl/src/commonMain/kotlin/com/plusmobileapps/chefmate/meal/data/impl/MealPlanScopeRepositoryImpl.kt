package com.plusmobileapps.chefmate.meal.data.impl

import com.plusmobileapps.chefmate.auth.data.AuthState
import com.plusmobileapps.chefmate.auth.data.AuthenticationRepository
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.di.IO
import com.plusmobileapps.chefmate.family.data.FamilyRepository
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.featureflag.FeatureFlags
import com.plusmobileapps.chefmate.featureflag.isEnabled
import com.plusmobileapps.chefmate.meal.data.FamilyPlan
import com.plusmobileapps.chefmate.meal.data.MealPlanScope
import com.plusmobileapps.chefmate.meal.data.MealPlanScopeRepository
import com.plusmobileapps.chefmate.meal.data.MealPlanSelectionRepository
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class MealPlanScopeRepositoryImpl(
    @IO ioContext: CoroutineContext,
    private val familyRepository: FamilyRepository,
    private val authRepository: AuthenticationRepository,
    featureFlags: FeatureFlags,
    private val selectionRepository: MealPlanSelectionRepository,
) : MealPlanScopeRepository {

    private val scope = CoroutineScope(ioContext + SupervisorJob())
    private val enabled = featureFlags.isEnabled(FeatureFlagRegistry.FamilyMealPlans)

    private val _familyPlans = MutableStateFlow<List<FamilyPlan>>(emptyList())
    override val familyPlans: StateFlow<List<FamilyPlan>> = _familyPlans.asStateFlow()

    private val _selectedScope =
        MutableStateFlow(
            if (enabled.value) selectionRepository.lastSelectedScope else MealPlanScope.Personal
        )
    override val selectedScope: StateFlow<MealPlanScope> = _selectedScope.asStateFlow()

    init {
        // Signing out, or the flag going off, drops the user back onto their personal plan.
        scope.launch {
            combine(authRepository.state.map { it is AuthState.Authenticated }, enabled) {
                    signedIn,
                    on ->
                    signedIn && on
                }
                .distinctUntilChanged()
                .collect { available ->
                    if (available) {
                        refresh()
                    } else {
                        _familyPlans.value = emptyList()
                        _selectedScope.value = MealPlanScope.Personal
                    }
                }
        }
    }

    override fun select(scope: MealPlanScope) {
        val familyId = scope.familyId
        if (familyId != null && _familyPlans.value.none { it.familyId == familyId }) return
        selectionRepository.select(scope)
        _selectedScope.value = scope
    }

    override suspend fun refresh() {
        if (!enabled.value || authRepository.state.value !is AuthState.Authenticated) return
        val plans =
            try {
                familyRepository.getFamilies().map { FamilyPlan(familyId = it.id, name = it.name) }
            } catch (_: Exception) {
                // Offline: keep the last known list and selection rather than bouncing the user.
                return
            }
        _familyPlans.value = plans
        // The selected family was deleted or the user left it: fall back to the personal plan.
        _selectedScope.update { current ->
            val familyId = current.familyId ?: selectionRepository.lastSelectedScope.familyId
            if (familyId != null && plans.any { it.familyId == familyId }) {
                MealPlanScope.Family(familyId)
            } else {
                selectionRepository.clear()
                MealPlanScope.Personal
            }
        }
    }
}

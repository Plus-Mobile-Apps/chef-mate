package com.plusmobileapps.chefmate.family.manage.impl

import chefmate.client.family.manage.public.generated.resources.Res
import chefmate.client.family.manage.public.generated.resources.family_create_error
import chefmate.client.family.manage.public.generated.resources.family_list_load_error
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.family.data.Family
import com.plusmobileapps.chefmate.family.data.FamilyRepository
import com.plusmobileapps.chefmate.family.manage.FamilyListBloc
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.TextData
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
class FamilyListViewModel(
    @Assisted private val onFamilyCreated: (String) -> Unit,
    @Main mainContext: CoroutineContext,
    private val repository: FamilyRepository,
) : ViewModel(mainContext) {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var loadJob: Job? = null

    /**
     * Loads the families. Called on every resume, so a family created, left or deleted on the
     * detail screen is reflected on the way back. Only the very first load blanks the screen with a
     * spinner; later ones leave the current list up until the new one arrives.
     */
    fun refresh() {
        loadJob?.cancel()
        _state.update { it.copy(isLoading = !it.hasLoaded, loadError = null) }
        loadJob = scope.launch {
            try {
                val families = repository.getFamilies()
                _state.update {
                    it.copy(
                        families = families,
                        isLoading = false,
                        hasLoaded = true,
                        loadError = null,
                    )
                }
            } catch (_: Throwable) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        // A failed refresh with a list already on screen keeps the list; the
                        // error state is only for having nothing to show.
                        loadError =
                            if (it.hasLoaded && it.families.isNotEmpty()) null
                            else ResourceString(Res.string.family_list_load_error),
                    )
                }
            }
        }
    }

    fun onCreateFamilyClicked() {
        _state.update { it.copy(createDialog = FamilyListBloc.CreateDialog()) }
    }

    fun onNewFamilyNameChanged(name: String) {
        _state.update { state ->
            state.createDialog?.let {
                state.copy(createDialog = it.copy(name = name, error = null))
            } ?: state
        }
    }

    fun onCreateDismissed() {
        _state.update { it.copy(createDialog = null) }
    }

    fun onCreateConfirmed() {
        val dialog = _state.value.createDialog ?: return
        if (!dialog.canConfirm) return
        val name = dialog.name.trim()
        _state.update { it.copy(createDialog = dialog.copy(isSaving = true, error = null)) }
        scope.launch {
            try {
                val familyId = repository.createFamily(name)
                _state.update { it.copy(createDialog = null) }
                // Open the new family straight away — it has nobody in it yet, so inviting is
                // the only thing the user could want to do next.
                onFamilyCreated(familyId)
            } catch (_: Throwable) {
                _state.update {
                    it.copy(
                        createDialog =
                            it.createDialog?.copy(
                                isSaving = false,
                                error = ResourceString(Res.string.family_create_error),
                            )
                    )
                }
            }
        }
    }

    data class State(
        val families: List<Family> = emptyList(),
        val isLoading: Boolean = true,
        val loadError: TextData? = null,
        val createDialog: FamilyListBloc.CreateDialog? = null,
        /** True once a load has completed, so later refreshes don't blank the screen. */
        val hasLoaded: Boolean = false,
    )

    @AssistedFactory
    fun interface Factory {
        fun create(onFamilyCreated: (String) -> Unit): FamilyListViewModel
    }
}

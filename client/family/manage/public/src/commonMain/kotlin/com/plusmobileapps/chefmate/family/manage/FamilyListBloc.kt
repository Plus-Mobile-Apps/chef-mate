package com.plusmobileapps.chefmate.family.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.family.data.Family
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.coroutines.flow.StateFlow

/** Lists the families the user belongs to and lets them create a new one. */
interface FamilyListBloc : ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        FamilyListScreen(bloc = this, modifier = modifier)
    }

    fun onBackClicked()

    fun onRetryClicked()

    fun onFamilyClicked(familyId: String)

    /** Opens the "New family" dialog. */
    fun onCreateFamilyClicked()

    fun onNewFamilyNameChanged(name: String)

    fun onCreateConfirmed()

    fun onCreateDismissed()

    data class Model(
        val families: List<Family> = emptyList(),
        /** True only for the first load; later refreshes keep the current list on screen. */
        val isLoading: Boolean = true,
        /** Non-null when the list couldn't be loaded and there's nothing to show. */
        val loadError: TextData? = null,
        /** Non-null while the "New family" dialog is shown. */
        val createDialog: CreateDialog? = null,
    ) {
        val isEmpty: Boolean
            get() = !isLoading && loadError == null && families.isEmpty()
    }

    data class CreateDialog(
        val name: String = "",
        val isSaving: Boolean = false,
        val error: TextData? = null,
    ) {
        val canConfirm: Boolean
            get() = name.isNotBlank() && !isSaving
    }

    sealed class Output {
        data object Back : Output()

        data class OpenFamily(val familyId: String) : Output()
    }

    fun interface Factory {
        fun create(context: BlocContext, output: Consumer<Output>): FamilyListBloc
    }
}

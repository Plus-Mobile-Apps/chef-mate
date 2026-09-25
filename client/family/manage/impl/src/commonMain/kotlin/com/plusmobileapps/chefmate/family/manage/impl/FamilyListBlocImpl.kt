package com.plusmobileapps.chefmate.family.manage.impl

import com.arkivanov.essenty.lifecycle.doOnResume
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.family.manage.FamilyListBloc
import com.plusmobileapps.chefmate.family.manage.FamilyListBloc.Model
import com.plusmobileapps.chefmate.family.manage.FamilyListBloc.Output
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = FamilyListBloc.Factory::class,
)
class FamilyListBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: FamilyListViewModel.Factory,
) : FamilyListBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel {
        viewModelFactory.create { familyId -> output.onNext(Output.OpenFamily(familyId)) }
    }

    init {
        // Loading on resume rather than in init covers both the first show and coming back from
        // the detail screen, where the family may have been renamed, left or deleted.
        lifecycle.doOnResume { viewModel.refresh() }
    }

    override val state: StateFlow<Model> =
        viewModel.state.mapState {
            Model(
                families = it.families,
                isLoading = it.isLoading,
                loadError = it.loadError,
                createDialog = it.createDialog,
            )
        }

    override fun onBackClicked() {
        output.onNext(Output.Back)
    }

    override fun onRetryClicked() = viewModel.refresh()

    override fun onFamilyClicked(familyId: String) {
        output.onNext(Output.OpenFamily(familyId))
    }

    override fun onCreateFamilyClicked() = viewModel.onCreateFamilyClicked()

    override fun onNewFamilyNameChanged(name: String) = viewModel.onNewFamilyNameChanged(name)

    override fun onCreateConfirmed() = viewModel.onCreateConfirmed()

    override fun onCreateDismissed() = viewModel.onCreateDismissed()
}

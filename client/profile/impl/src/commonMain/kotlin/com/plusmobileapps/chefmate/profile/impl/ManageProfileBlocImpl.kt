package com.plusmobileapps.chefmate.profile.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.profile.ManageProfileBloc
import com.plusmobileapps.chefmate.profile.ManageProfileBloc.Model
import com.plusmobileapps.chefmate.profile.ManageProfileBloc.Output
import com.plusmobileapps.chefmate.util.PickedImage
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = ManageProfileBloc.Factory::class,
)
class ManageProfileBlocImpl(
    @Assisted context: BlocContext,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: () -> ManageProfileViewModel,
) : ManageProfileBloc, BlocContext by context {

    private val scope = createScope()

    private val viewModel = instanceKeeper.getViewModel { viewModelFactory() }

    override val state: StateFlow<Model> = viewModel.state

    init {
        scope.launch {
            viewModel.outputs.collect {
                when (it) {
                    ManageProfileViewModel.Output.Saved -> output.onNext(Output.Back)
                    ManageProfileViewModel.Output.Deleted -> output.onNext(Output.AccountDeleted)
                }
            }
        }
    }

    override fun onBack() {
        output.onNext(Output.Back)
    }

    override fun onDisplayNameChanged(displayName: String) {
        viewModel.setDisplayName(displayName)
    }

    override fun onPhotoPicked(image: PickedImage) {
        viewModel.setPhoto(image)
    }

    override fun onSaveClicked() {
        viewModel.save()
    }

    override fun onDeleteAccountClicked() {
        viewModel.showDeleteDialog()
    }

    override fun onDeleteConfirmationChanged(confirmation: String) {
        viewModel.setDeleteConfirmation(confirmation)
    }

    override fun onDeleteConfirmed() {
        viewModel.deleteAccount()
    }

    override fun onDeleteDismissed() {
        viewModel.dismissDeleteDialog()
    }
}

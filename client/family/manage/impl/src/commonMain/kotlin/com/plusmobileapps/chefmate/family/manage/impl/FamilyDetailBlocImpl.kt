package com.plusmobileapps.chefmate.family.manage.impl

import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.di.AppScope
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.Model
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.Output
import com.plusmobileapps.chefmate.getViewModel
import com.plusmobileapps.chefmate.mapState
import com.plusmobileapps.metro.extensions.assistedfactory.ContributesAssistedFactory
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.StateFlow

@AssistedInject
@ContributesAssistedFactory(
    scope = AppScope::class,
    assistedFactory = FamilyDetailBloc.Factory::class,
)
class FamilyDetailBlocImpl(
    @Assisted context: BlocContext,
    @Assisted familyId: String,
    @Assisted private val output: Consumer<Output>,
    viewModelFactory: FamilyDetailViewModel.Factory,
) : FamilyDetailBloc, BlocContext by context {

    private val viewModel = instanceKeeper.getViewModel {
        viewModelFactory.create(familyId) { output.onNext(Output.Closed) }
    }

    override val state: StateFlow<Model> =
        viewModel.state.mapState {
            Model(
                name = it.name,
                myRole = it.myRole,
                isLoading = it.isLoading,
                loadError = it.loadError,
                members = it.members,
                invites = it.invites,
                invitableRoles = it.invitableRoles,
                canRename = it.canRename,
                canLeave = it.canLeave,
                canDelete = it.canDelete,
                inviteEmail = it.inviteEmail,
                inviteRole = it.inviteRole,
                isInviting = it.isInviting,
                inviteError = it.inviteError,
                isWorking = it.isWorking,
                dialog = it.dialog,
            )
        }

    override fun onBackClicked() {
        output.onNext(Output.Back)
    }

    override fun onRetryClicked() = viewModel.load()

    override fun onInviteEmailChanged(email: String) = viewModel.onInviteEmailChanged(email)

    override fun onInviteRoleChanged(role: FamilyRole) = viewModel.onInviteRoleChanged(role)

    override fun onInviteClicked() = viewModel.onInviteClicked()

    override fun onRemoveMemberClicked(memberId: String) = viewModel.onRemoveMemberClicked(memberId)

    override fun onPromoteClicked(memberId: String) = viewModel.onPromoteClicked(memberId)

    override fun onDemoteClicked(memberId: String) = viewModel.onDemoteClicked(memberId)

    override fun onRenameClicked() = viewModel.onRenameClicked()

    override fun onRenameNameChanged(name: String) = viewModel.onRenameNameChanged(name)

    override fun onLeaveClicked() = viewModel.onLeaveClicked()

    override fun onDeleteClicked() = viewModel.onDeleteClicked()

    override fun onDialogConfirmed() = viewModel.onDialogConfirmed()

    override fun onDialogDismissed() = viewModel.onDialogDismissed()
}

package com.plusmobileapps.chefmate.family.manage.impl

import chefmate.client.family.manage.public.generated.resources.Res
import chefmate.client.family.manage.public.generated.resources.family_detail_action_error
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_email_error
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_error
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_sent
import chefmate.client.family.manage.public.generated.resources.family_detail_load_error
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.family.data.FamilyMember
import com.plusmobileapps.chefmate.family.data.FamilyMemberStatus
import com.plusmobileapps.chefmate.family.data.FamilyPermissions
import com.plusmobileapps.chefmate.family.data.FamilyRepository
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.Dialog
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.MemberItem
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.toast.ToastService
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AssistedInject
class FamilyDetailViewModel(
    @Assisted private val familyId: String,
    @Assisted private val onClosed: () -> Unit,
    @Main mainContext: CoroutineContext,
    private val repository: FamilyRepository,
    private val toastService: ToastService,
) : ViewModel(mainContext) {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = it.myRole == null, loadError = null) }
        scope.launch {
            try {
                val family = repository.getFamily(familyId)
                if (family == null) {
                    // Someone removed us (or deleted the family) while we weren't looking. There's
                    // nothing left to render, so surface it as a load failure rather than an
                    // empty screen.
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loadError = ResourceString(Res.string.family_detail_load_error),
                        )
                    }
                    return@launch
                }
                val rows = repository.getMembers(familyId)
                _state.update { state ->
                    state.loaded(name = family.name, role = family.myRole, rows = rows)
                }
            } catch (_: Throwable) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadError = ResourceString(Res.string.family_detail_load_error),
                    )
                }
            }
        }
    }

    fun onInviteEmailChanged(email: String) {
        _state.update { it.copy(inviteEmail = email, inviteError = null) }
    }

    fun onInviteRoleChanged(role: FamilyRole) {
        _state.update { it.copy(inviteRole = role) }
    }

    fun onInviteClicked() {
        val current = _state.value
        if (current.isInviting) return
        val email = current.inviteEmail.trim()
        if (!email.isValidEmail()) {
            _state.update {
                it.copy(inviteError = ResourceString(Res.string.family_detail_invite_email_error))
            }
            return
        }
        _state.update { it.copy(isInviting = true, inviteError = null) }
        scope.launch {
            try {
                repository.invite(familyId, email, current.inviteRole)
                _state.update { it.copy(inviteEmail = "", isInviting = false) }
                toastService.show(
                    PhraseModel(Res.string.family_detail_invite_sent, "email" to FixedString(email))
                )
                load()
            } catch (_: Throwable) {
                _state.update {
                    it.copy(
                        isInviting = false,
                        inviteError = ResourceString(Res.string.family_detail_invite_error),
                    )
                }
            }
        }
    }

    fun onRemoveMemberClicked(memberId: String) {
        val member = _state.value.rowFor(memberId) ?: return
        _state.update { it.copy(dialog = Dialog.RemoveMember(member)) }
    }

    fun onPromoteClicked(memberId: String) = changeRole(memberId, FamilyRole.ADMIN)

    fun onDemoteClicked(memberId: String) = changeRole(memberId, FamilyRole.MEMBER)

    fun onRenameClicked() {
        if (!_state.value.canRename) return
        _state.update { it.copy(dialog = Dialog.Rename(it.name)) }
    }

    fun onRenameNameChanged(name: String) {
        _state.update { state ->
            (state.dialog as? Dialog.Rename)?.let { state.copy(dialog = Dialog.Rename(name)) }
                ?: state
        }
    }

    fun onLeaveClicked() {
        if (!_state.value.canLeave) return
        _state.update { it.copy(dialog = Dialog.Leave) }
    }

    fun onDeleteClicked() {
        if (!_state.value.canDelete) return
        _state.update { it.copy(dialog = Dialog.Delete) }
    }

    fun onDialogDismissed() {
        _state.update { it.copy(dialog = null) }
    }

    fun onDialogConfirmed() {
        val current = _state.value
        val dialog = current.dialog ?: return
        if (current.isWorking) return
        if (dialog is Dialog.Rename && !dialog.canConfirm) return
        _state.update { it.copy(dialog = null, isWorking = true) }
        scope.launch {
            // Leaving and deleting take the family away from the user, so they pop the screen
            // instead of reloading it.
            val closesScreen = dialog is Dialog.Leave || dialog is Dialog.Delete
            try {
                when (dialog) {
                    is Dialog.Rename -> repository.renameFamily(familyId, dialog.name.trim())
                    is Dialog.RemoveMember -> repository.removeMember(dialog.member.id)
                    Dialog.Leave -> repository.leaveFamily(familyId)
                    Dialog.Delete -> repository.deleteFamily(familyId)
                }
                _state.update { it.copy(isWorking = false) }
                if (closesScreen) onClosed() else load()
            } catch (_: Throwable) {
                _state.update { it.copy(isWorking = false) }
                toastService.show(ResourceString(Res.string.family_detail_action_error))
            }
        }
    }

    private fun changeRole(memberId: String, role: FamilyRole) {
        val current = _state.value
        if (current.isWorking || current.rowFor(memberId) == null) return
        _state.update { it.copy(isWorking = true) }
        scope.launch {
            try {
                repository.setRole(memberId, role)
                _state.update { it.copy(isWorking = false) }
                load()
            } catch (_: Throwable) {
                _state.update { it.copy(isWorking = false) }
                toastService.show(ResourceString(Res.string.family_detail_action_error))
            }
        }
    }

    private fun String.isValidEmail(): Boolean =
        isNotBlank() && contains('@') && substringAfter('@').contains('.')

    data class State(
        val name: String = "",
        val myRole: FamilyRole? = null,
        val isLoading: Boolean = true,
        val loadError: TextData? = null,
        val members: List<MemberItem> = emptyList(),
        val invites: List<MemberItem> = emptyList(),
        val invitableRoles: List<FamilyRole> = emptyList(),
        val canRename: Boolean = false,
        val canLeave: Boolean = false,
        val canDelete: Boolean = false,
        val inviteEmail: String = "",
        val inviteRole: FamilyRole = FamilyRole.MEMBER,
        val isInviting: Boolean = false,
        val inviteError: TextData? = null,
        val isWorking: Boolean = false,
        val dialog: Dialog? = null,
    ) {
        fun rowFor(memberId: String): FamilyMember? =
            (members + invites).firstOrNull { it.member.id == memberId }?.member

        /**
         * Folds a fresh load into the state, deriving every per-row action from [FamilyPermissions]
         * so the UI only offers what the server would accept.
         */
        fun loaded(name: String, role: FamilyRole, rows: List<FamilyMember>): State {
            val items = rows.map { member ->
                MemberItem(
                    member = member,
                    canRemove = FamilyPermissions.canRemove(role, member),
                    canPromote = FamilyPermissions.canPromote(role, member),
                    canDemote = FamilyPermissions.canDemote(role, member),
                )
            }
            val invitableRoles = FamilyPermissions.invitableRoles(role)
            return copy(
                name = name,
                myRole = role,
                isLoading = false,
                loadError = null,
                members = items.filter { it.member.status == FamilyMemberStatus.ACCEPTED },
                invites = items.filterNot { it.member.status == FamilyMemberStatus.ACCEPTED },
                invitableRoles = invitableRoles,
                canRename = FamilyPermissions.canRename(role),
                canLeave = FamilyPermissions.canLeave(role),
                canDelete = FamilyPermissions.canDelete(role),
                // Keep whatever the user has already picked if it's still offered; otherwise fall
                // back to the first role they may invite as.
                inviteRole =
                    inviteRole.takeIf { it in invitableRoles }
                        ?: invitableRoles.firstOrNull()
                        ?: inviteRole,
            )
        }
    }

    @AssistedFactory
    fun interface Factory {
        fun create(familyId: String, onClosed: () -> Unit): FamilyDetailViewModel
    }
}

package com.plusmobileapps.chefmate.family.manage.impl

import chefmate.client.family.manage.public.generated.resources.Res
import chefmate.client.family.manage.public.generated.resources.family_detail_action_error
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_lists_load_error
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_email_error
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_error
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_sent
import chefmate.client.family.manage.public.generated.resources.family_detail_load_error
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_books_load_error
import com.plusmobileapps.chefmate.ViewModel
import com.plusmobileapps.chefmate.di.Main
import com.plusmobileapps.chefmate.family.data.FamilyMember
import com.plusmobileapps.chefmate.family.data.FamilyMemberStatus
import com.plusmobileapps.chefmate.family.data.FamilyPermissions
import com.plusmobileapps.chefmate.family.data.FamilyRepository
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.Dialog
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.MemberItem
import com.plusmobileapps.chefmate.featureflag.FeatureFlagRegistry
import com.plusmobileapps.chefmate.featureflag.FeatureFlags
import com.plusmobileapps.chefmate.featureflag.isEnabled
import com.plusmobileapps.chefmate.grocery.data.FamilyGroceryList
import com.plusmobileapps.chefmate.grocery.data.FamilyGroceryListRepository
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBook
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBookRepository
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.ResourceString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.toast.ToastService
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
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
    private val groceryListRepository: FamilyGroceryListRepository,
    private val recipeBookRepository: FamilyRecipeBookRepository,
    private val toastService: ToastService,
    featureFlags: FeatureFlags,
) : ViewModel(mainContext) {

    private val groceryListsEnabled = featureFlags.isEnabled(FeatureFlagRegistry.FamilyGroceryLists)
    private val recipeBooksEnabled = featureFlags.isEnabled(FeatureFlagRegistry.FamilyRecipeBooks)

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
                val groceryLists = loadGroceryLists()
                val recipeBooks = loadRecipeBooks()
                _state.update { state ->
                    state
                        .loaded(name = family.name, role = family.myRole, rows = rows)
                        .withGroceryLists(groceryLists)
                        .withRecipeBooks(recipeBooks)
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

    fun onInviteSomeoneClicked() {
        val current = _state.value
        if (!current.canInvite || current.dialog != null) return
        // Start every invite fresh, defaulting to the first role the user may invite as.
        _state.update {
            it.copy(
                dialog = Dialog.Invite,
                inviteEmail = "",
                inviteError = null,
                inviteRole = it.invitableRoles.first(),
            )
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
                _state.update {
                    it.copy(
                        inviteEmail = "",
                        isInviting = false,
                        dialog = it.dialog.takeUnless { dialog -> dialog == Dialog.Invite },
                    )
                }
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

    fun onCreateGroceryListClicked() {
        if (!_state.value.canManageGroceryLists) return
        _state.update { it.copy(dialog = Dialog.CreateGroceryList()) }
    }

    fun onRenameGroceryListClicked(listId: String) {
        val current = _state.value
        if (!current.canManageGroceryLists) return
        val list = current.groceryLists.firstOrNull { it.id == listId } ?: return
        _state.update { it.copy(dialog = Dialog.RenameGroceryList(list, list.name)) }
    }

    fun onDeleteGroceryListClicked(listId: String) {
        val current = _state.value
        if (!current.canManageGroceryLists) return
        val list = current.groceryLists.firstOrNull { it.id == listId } ?: return
        _state.update { it.copy(dialog = Dialog.DeleteGroceryList(list)) }
    }

    fun onGroceryListNameChanged(name: String) {
        _state.update { state ->
            when (val dialog = state.dialog) {
                is Dialog.CreateGroceryList -> state.copy(dialog = dialog.copy(name = name))
                is Dialog.RenameGroceryList -> state.copy(dialog = dialog.copy(name = name))
                else -> state
            }
        }
    }

    fun onCreateRecipeBookClicked() {
        if (!_state.value.canManageRecipeBooks) return
        _state.update { it.copy(dialog = Dialog.CreateRecipeBook()) }
    }

    fun onRenameRecipeBookClicked(bookId: String) {
        val current = _state.value
        if (!current.canManageRecipeBooks) return
        val book = current.recipeBooks.firstOrNull { it.id == bookId } ?: return
        _state.update { it.copy(dialog = Dialog.RenameRecipeBook(book, book.name)) }
    }

    fun onDeleteRecipeBookClicked(bookId: String) {
        val current = _state.value
        if (!current.canManageRecipeBooks) return
        val book = current.recipeBooks.firstOrNull { it.id == bookId } ?: return
        _state.update { it.copy(dialog = Dialog.DeleteRecipeBook(book)) }
    }

    fun onRecipeBookNameChanged(name: String) {
        _state.update { state ->
            when (val dialog = state.dialog) {
                is Dialog.CreateRecipeBook -> state.copy(dialog = dialog.copy(name = name))
                is Dialog.RenameRecipeBook -> state.copy(dialog = dialog.copy(name = name))
                else -> state
            }
        }
    }

    fun onDialogDismissed() {
        _state.update { it.copy(dialog = null, inviteError = null) }
    }

    fun onDialogConfirmed() {
        val current = _state.value
        val dialog = current.dialog ?: return
        // The invite dialog stays open while sending so a rejected invite can show inline.
        if (dialog == Dialog.Invite) return onInviteClicked()
        if (current.isWorking) return
        val canConfirm =
            when (dialog) {
                is Dialog.Rename -> dialog.canConfirm
                is Dialog.CreateGroceryList -> dialog.canConfirm
                is Dialog.RenameGroceryList -> dialog.canConfirm
                is Dialog.CreateRecipeBook -> dialog.canConfirm
                is Dialog.RenameRecipeBook -> dialog.canConfirm
                else -> true
            }
        if (!canConfirm) return
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
                    is Dialog.CreateGroceryList ->
                        groceryListRepository.createList(familyId, dialog.name.trim())
                    is Dialog.RenameGroceryList ->
                        groceryListRepository.renameList(dialog.list.id, dialog.name.trim())
                    is Dialog.DeleteGroceryList -> groceryListRepository.deleteList(dialog.list.id)
                    is Dialog.CreateRecipeBook ->
                        recipeBookRepository.createBook(familyId, dialog.name.trim())
                    is Dialog.RenameRecipeBook ->
                        recipeBookRepository.renameBook(dialog.book.id, dialog.name.trim())
                    is Dialog.DeleteRecipeBook -> recipeBookRepository.deleteBook(dialog.book.id)
                    // Handled by onInviteClicked above.
                    Dialog.Invite -> Unit
                }
                _state.update { it.copy(isWorking = false) }
                if (closesScreen) {
                    // Drop the family's lists and books now rather than on the next sync.
                    groceryListRepository.refreshLocalLists()
                    recipeBookRepository.refreshLocalBooks()
                    onClosed()
                } else {
                    load()
                }
            } catch (_: Throwable) {
                _state.update { it.copy(isWorking = false) }
                toastService.show(ResourceString(Res.string.family_detail_action_error))
            }
        }
    }

    /**
     * The family's grocery lists, or null when the section is off. A failure here is reported on
     * the section rather than failing the whole screen.
     */
    private suspend fun loadGroceryLists(): Result<List<FamilyGroceryList>>? {
        if (!groceryListsEnabled.value) return null
        return try {
            Result.success(groceryListRepository.getLists(familyId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    /**
     * The family's recipe books, or null when the section is off. Fails like [loadGroceryLists].
     */
    private suspend fun loadRecipeBooks(): Result<List<FamilyRecipeBook>>? {
        if (!recipeBooksEnabled.value) return null
        return try {
            Result.success(recipeBookRepository.getBooks(familyId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Result.failure(e)
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
        val showGroceryLists: Boolean = false,
        val groceryLists: List<FamilyGroceryList> = emptyList(),
        val groceryListsError: TextData? = null,
        val canManageGroceryLists: Boolean = false,
        val showRecipeBooks: Boolean = false,
        val recipeBooks: List<FamilyRecipeBook> = emptyList(),
        val recipeBooksError: TextData? = null,
        val canManageRecipeBooks: Boolean = false,
    ) {
        val canInvite: Boolean
            get() = invitableRoles.isNotEmpty()

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
                canManageGroceryLists = FamilyPermissions.canManageGroceryLists(role),
                canManageRecipeBooks = FamilyPermissions.canManageRecipeBooks(role),
                // Keep whatever the user has already picked if it's still offered; otherwise fall
                // back to the first role they may invite as.
                inviteRole =
                    inviteRole.takeIf { it in invitableRoles }
                        ?: invitableRoles.firstOrNull()
                        ?: inviteRole,
            )
        }
    }

    /** Folds the grocery lists load into the state; null means the section is off. */
    private fun State.withGroceryLists(result: Result<List<FamilyGroceryList>>?): State =
        when {
            result == null ->
                copy(showGroceryLists = false, groceryLists = emptyList(), groceryListsError = null)
            result.isSuccess ->
                copy(
                    showGroceryLists = true,
                    groceryLists = result.getOrThrow(),
                    groceryListsError = null,
                )
            // Keep the last lists we had so a flaky reload doesn't blank the section.
            else ->
                copy(
                    showGroceryLists = true,
                    groceryListsError =
                        ResourceString(Res.string.family_detail_grocery_lists_load_error),
                )
        }

    /** Folds the recipe books load into the state; null means the section is off. */
    private fun State.withRecipeBooks(result: Result<List<FamilyRecipeBook>>?): State =
        when {
            result == null ->
                copy(showRecipeBooks = false, recipeBooks = emptyList(), recipeBooksError = null)
            result.isSuccess ->
                copy(
                    showRecipeBooks = true,
                    recipeBooks = result.getOrThrow(),
                    recipeBooksError = null,
                )
            // Keep the last books we had so a flaky reload doesn't blank the section.
            else ->
                copy(
                    showRecipeBooks = true,
                    recipeBooksError =
                        ResourceString(Res.string.family_detail_recipe_books_load_error),
                )
        }

    @AssistedFactory
    fun interface Factory {
        fun create(familyId: String, onClosed: () -> Unit): FamilyDetailViewModel
    }
}

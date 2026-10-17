package com.plusmobileapps.chefmate.family.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.plusmobileapps.chefmate.BlocContext
import com.plusmobileapps.chefmate.Consumer
import com.plusmobileapps.chefmate.family.data.FamilyMember
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.grocery.data.FamilyGroceryList
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBook
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.ui.ComposeScreen
import kotlinx.coroutines.flow.StateFlow

/**
 * One family's members and invites. What the user can do depends on their role — see
 * [com.plusmobileapps.chefmate.family.data.FamilyPermissions].
 */
interface FamilyDetailBloc : ComposeScreen {
    val state: StateFlow<Model>

    @Composable
    override fun Content(modifier: Modifier) {
        FamilyDetailScreen(bloc = this, modifier = modifier)
    }

    fun onBackClicked()

    fun onRetryClicked()

    /** Opens the invite dialog. */
    fun onInviteSomeoneClicked()

    fun onInviteEmailChanged(email: String)

    fun onInviteRoleChanged(role: FamilyRole)

    /** Sends the invite from the invite dialog. */
    fun onInviteClicked()

    /** Asks to confirm removing the member (or cancelling the invite) with [memberId]. */
    fun onRemoveMemberClicked(memberId: String)

    fun onPromoteClicked(memberId: String)

    fun onDemoteClicked(memberId: String)

    /** Opens the rename dialog. */
    fun onRenameClicked()

    fun onRenameNameChanged(name: String)

    fun onLeaveClicked()

    fun onDeleteClicked()

    /** Opens the "New grocery list" dialog. */
    fun onCreateGroceryListClicked()

    fun onRenameGroceryListClicked(listId: String)

    /** Asks to confirm deleting the family grocery list with [listId]. */
    fun onDeleteGroceryListClicked(listId: String)

    /** Edits the name in the create / rename grocery list dialog. */
    fun onGroceryListNameChanged(name: String)

    /** Opens the "New recipe book" dialog. */
    fun onCreateRecipeBookClicked()

    fun onRenameRecipeBookClicked(bookId: String)

    /** Asks to confirm deleting the family recipe book with [bookId]. */
    fun onDeleteRecipeBookClicked(bookId: String)

    /** Edits the name in the create / rename recipe book dialog. */
    fun onRecipeBookNameChanged(name: String)

    /** Confirms whichever [Model.dialog] is showing. */
    fun onDialogConfirmed()

    fun onDialogDismissed()

    data class Model(
        val name: String = "",
        /** The user's role; null until loaded. */
        val myRole: FamilyRole? = null,
        val isLoading: Boolean = true,
        val loadError: TextData? = null,
        /** Accepted members, owner first. */
        val members: List<MemberItem> = emptyList(),
        /** Pending and declined invites. */
        val invites: List<MemberItem> = emptyList(),
        /** Roles the user may invite as; empty hides the invite button. */
        val invitableRoles: List<FamilyRole> = emptyList(),
        val canRename: Boolean = false,
        val canLeave: Boolean = false,
        val canDelete: Boolean = false,
        val inviteEmail: String = "",
        val inviteRole: FamilyRole = FamilyRole.MEMBER,
        val isInviting: Boolean = false,
        val inviteError: TextData? = null,
        /** True while a remove/role change/rename/leave/delete is in flight. */
        val isWorking: Boolean = false,
        val dialog: Dialog? = null,
        /** Whether the family's grocery lists section is shown at all. */
        val showGroceryLists: Boolean = false,
        val groceryLists: List<FamilyGroceryList> = emptyList(),
        /** Set when the lists couldn't load; the rest of the screen still works. */
        val groceryListsError: TextData? = null,
        /** Whether the user may create, rename and delete the family's grocery lists. */
        val canManageGroceryLists: Boolean = false,
        /** Whether the family's recipe books section is shown at all. */
        val showRecipeBooks: Boolean = false,
        val recipeBooks: List<FamilyRecipeBook> = emptyList(),
        /** Set when the books couldn't load; the rest of the screen still works. */
        val recipeBooksError: TextData? = null,
        /** Whether the user may create, rename and delete the family's recipe books. */
        val canManageRecipeBooks: Boolean = false,
    ) {
        val canInvite: Boolean
            get() = invitableRoles.isNotEmpty()
    }

    /** A member row plus the actions the current user may take on it. */
    data class MemberItem(
        val member: FamilyMember,
        val canRemove: Boolean = false,
        val canPromote: Boolean = false,
        val canDemote: Boolean = false,
    ) {
        val hasActions: Boolean
            get() = canRemove || canPromote || canDemote
    }

    sealed class Dialog {
        data class Rename(val name: String) : Dialog() {
            val canConfirm: Boolean
                get() = name.isNotBlank()
        }

        data class RemoveMember(val member: FamilyMember) : Dialog()

        /** The invite form; its fields live on [Model] (inviteEmail, inviteRole, ...). */
        data object Invite : Dialog()

        data object Leave : Dialog()

        data object Delete : Dialog()

        data class CreateGroceryList(val name: String = "") : Dialog() {
            val canConfirm: Boolean
                get() = name.isNotBlank()
        }

        data class RenameGroceryList(val list: FamilyGroceryList, val name: String) : Dialog() {
            val canConfirm: Boolean
                get() = name.isNotBlank() && name.trim() != list.name
        }

        data class DeleteGroceryList(val list: FamilyGroceryList) : Dialog()

        data class CreateRecipeBook(val name: String = "") : Dialog() {
            val canConfirm: Boolean
                get() = name.isNotBlank()
        }

        data class RenameRecipeBook(val book: FamilyRecipeBook, val name: String) : Dialog() {
            val canConfirm: Boolean
                get() = name.isNotBlank() && name.trim() != book.name
        }

        data class DeleteRecipeBook(val book: FamilyRecipeBook) : Dialog()
    }

    sealed class Output {
        data object Back : Output()

        /** The user left or deleted the family; it's no longer theirs to view. */
        data object Closed : Output()
    }

    fun interface Factory {
        fun create(
            context: BlocContext,
            familyId: String,
            output: Consumer<Output>,
        ): FamilyDetailBloc
    }
}

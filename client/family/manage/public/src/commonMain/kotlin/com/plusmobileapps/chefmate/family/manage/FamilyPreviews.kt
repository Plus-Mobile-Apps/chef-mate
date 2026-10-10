package com.plusmobileapps.chefmate.family.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import chefmate.client.family.manage.public.generated.resources.Res
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_error
import chefmate.client.family.manage.public.generated.resources.family_list_load_error
import com.plusmobileapps.chefmate.family.data.Family
import com.plusmobileapps.chefmate.family.data.FamilyMember
import com.plusmobileapps.chefmate.family.data.FamilyMemberStatus
import com.plusmobileapps.chefmate.family.data.FamilyPermissions
import com.plusmobileapps.chefmate.family.data.FamilyRole
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.Dialog
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.MemberItem
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import kotlinx.coroutines.flow.MutableStateFlow

// Preview BLoCs are public so :client:ui:screenshot-test can reuse them for snapshot references.

private fun familyListBloc(model: FamilyListBloc.Model): FamilyListBloc =
    object : FamilyListBloc {
        override val state = MutableStateFlow(model)

        override fun onBackClicked() = Unit

        override fun onRetryClicked() = Unit

        override fun onFamilyClicked(familyId: String) = Unit

        override fun onCreateFamilyClicked() = Unit

        override fun onNewFamilyNameChanged(name: String) = Unit

        override fun onCreateConfirmed() = Unit

        override fun onCreateDismissed() = Unit
    }

private fun familyDetailBloc(model: FamilyDetailBloc.Model): FamilyDetailBloc =
    object : FamilyDetailBloc {
        override val state = MutableStateFlow(model)

        override fun onBackClicked() = Unit

        override fun onRetryClicked() = Unit

        override fun onInviteEmailChanged(email: String) = Unit

        override fun onInviteRoleChanged(role: FamilyRole) = Unit

        override fun onInviteClicked() = Unit

        override fun onRemoveMemberClicked(memberId: String) = Unit

        override fun onPromoteClicked(memberId: String) = Unit

        override fun onDemoteClicked(memberId: String) = Unit

        override fun onRenameClicked() = Unit

        override fun onRenameNameChanged(name: String) = Unit

        override fun onLeaveClicked() = Unit

        override fun onDeleteClicked() = Unit

        override fun onDialogConfirmed() = Unit

        override fun onDialogDismissed() = Unit
    }

private val sampleFamilies =
    listOf(
        Family(id = "f1", name = "The Smiths", myRole = FamilyRole.OWNER, memberCount = 4),
        Family(id = "f2", name = "Lake House", myRole = FamilyRole.MEMBER, memberCount = 2),
    )

private val sampleMembers =
    listOf(
        FamilyMember(
            id = "m1",
            email = "jordan@example.com",
            role = FamilyRole.OWNER,
            status = FamilyMemberStatus.ACCEPTED,
            name = "Jordan Smith",
        ),
        FamilyMember(
            id = "m2",
            email = "riley@example.com",
            role = FamilyRole.ADMIN,
            status = FamilyMemberStatus.ACCEPTED,
            name = "Riley Smith",
        ),
        FamilyMember(
            id = "m3",
            email = "sam@example.com",
            role = FamilyRole.MEMBER,
            status = FamilyMemberStatus.ACCEPTED,
            name = "Sam Smith",
        ),
        FamilyMember(
            id = "m4",
            email = "casey@example.com",
            role = FamilyRole.MEMBER,
            status = FamilyMemberStatus.PENDING,
        ),
        FamilyMember(
            id = "m5",
            email = "alex@example.com",
            role = FamilyRole.MEMBER,
            status = FamilyMemberStatus.REJECTED,
        ),
    )

/**
 * Builds a detail model as [role] would see it: the viewer is whichever accepted row holds that
 * role (so "(you)" lands on the right person), and every per-row action comes from
 * [FamilyPermissions] rather than being hand-set.
 */
private fun detailModel(
    role: FamilyRole,
    members: List<FamilyMember> = sampleMembers,
): FamilyDetailBloc.Model {
    val selfId = members.first { it.role == role && it.status == FamilyMemberStatus.ACCEPTED }.id
    val items =
        members
            .map { it.copy(isSelf = it.id == selfId) }
            .map { member ->
                MemberItem(
                    member = member,
                    canRemove = FamilyPermissions.canRemove(role, member),
                    canPromote = FamilyPermissions.canPromote(role, member),
                    canDemote = FamilyPermissions.canDemote(role, member),
                )
            }
    return FamilyDetailBloc.Model(
        name = "The Smiths",
        myRole = role,
        isLoading = false,
        members = items.filter { it.member.status == FamilyMemberStatus.ACCEPTED },
        invites = items.filterNot { it.member.status == FamilyMemberStatus.ACCEPTED },
        invitableRoles = FamilyPermissions.invitableRoles(role),
        canRename = FamilyPermissions.canRename(role),
        canLeave = FamilyPermissions.canLeave(role),
        canDelete = FamilyPermissions.canDelete(role),
    )
}

val previewFamilyListBloc: FamilyListBloc =
    familyListBloc(FamilyListBloc.Model(families = sampleFamilies, isLoading = false))

val previewFamilyListEmptyBloc: FamilyListBloc =
    familyListBloc(FamilyListBloc.Model(isLoading = false))

val previewFamilyListLoadingBloc: FamilyListBloc = familyListBloc(FamilyListBloc.Model())

val previewFamilyListErrorBloc: FamilyListBloc =
    familyListBloc(
        FamilyListBloc.Model(
            isLoading = false,
            loadError = Res.string.family_list_load_error.asTextData(),
        )
    )

val previewFamilyListCreateDialogBloc: FamilyListBloc =
    familyListBloc(
        FamilyListBloc.Model(
            families = sampleFamilies,
            isLoading = false,
            createDialog = FamilyListBloc.CreateDialog(name = "Lake House"),
        )
    )

/** The owner's view: every action offered, including the admin/member role picker. */
val previewFamilyDetailOwnerBloc: FamilyDetailBloc = familyDetailBloc(detailModel(FamilyRole.OWNER))

/** An admin: can invite members and rename, can leave, can't delete or manage admins. */
val previewFamilyDetailAdminBloc: FamilyDetailBloc = familyDetailBloc(detailModel(FamilyRole.ADMIN))

/** A plain member: read-only apart from leaving. */
val previewFamilyDetailMemberBloc: FamilyDetailBloc =
    familyDetailBloc(detailModel(FamilyRole.MEMBER))

val previewFamilyDetailInviteErrorBloc: FamilyDetailBloc =
    familyDetailBloc(
        detailModel(FamilyRole.OWNER)
            .copy(
                inviteEmail = "casey@example.com",
                inviteError = Res.string.family_detail_invite_error.asTextData(),
            )
    )

val previewFamilyDetailDeleteDialogBloc: FamilyDetailBloc =
    familyDetailBloc(detailModel(FamilyRole.OWNER).copy(dialog = Dialog.Delete))

@Preview
@Composable
internal fun FamilyListPreview() {
    ChefMateTheme { FamilyListScreen(bloc = previewFamilyListBloc) }
}

@Preview
@Composable
internal fun FamilyListEmptyPreview() {
    ChefMateTheme { FamilyListScreen(bloc = previewFamilyListEmptyBloc) }
}

@Preview
@Composable
internal fun FamilyListErrorPreview() {
    ChefMateTheme { FamilyListScreen(bloc = previewFamilyListErrorBloc) }
}

@Preview
@Composable
internal fun FamilyDetailOwnerPreview() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailOwnerBloc) }
}

@Preview
@Composable
internal fun FamilyDetailMemberPreview() {
    ChefMateTheme { FamilyDetailScreen(bloc = previewFamilyDetailMemberBloc) }
}

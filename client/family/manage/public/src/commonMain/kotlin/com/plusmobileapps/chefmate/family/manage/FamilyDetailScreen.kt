@file:OptIn(ExperimentalLayoutApi::class)

package com.plusmobileapps.chefmate.family.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import chefmate.client.family.manage.public.generated.resources.Res
import chefmate.client.family.manage.public.generated.resources.family_cancel
import chefmate.client.family.manage.public.generated.resources.family_detail_cancel_invite
import chefmate.client.family.manage.public.generated.resources.family_detail_declined
import chefmate.client.family.manage.public.generated.resources.family_detail_delete_button
import chefmate.client.family.manage.public.generated.resources.family_detail_delete_confirm
import chefmate.client.family.manage.public.generated.resources.family_detail_delete_message
import chefmate.client.family.manage.public.generated.resources.family_detail_delete_title
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_as
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_button
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_email_label
import chefmate.client.family.manage.public.generated.resources.family_detail_invite_section
import chefmate.client.family.manage.public.generated.resources.family_detail_invites
import chefmate.client.family.manage.public.generated.resources.family_detail_leave_button
import chefmate.client.family.manage.public.generated.resources.family_detail_leave_confirm
import chefmate.client.family.manage.public.generated.resources.family_detail_leave_message
import chefmate.client.family.manage.public.generated.resources.family_detail_leave_title
import chefmate.client.family.manage.public.generated.resources.family_detail_make_admin
import chefmate.client.family.manage.public.generated.resources.family_detail_make_member
import chefmate.client.family.manage.public.generated.resources.family_detail_member_actions_a11y
import chefmate.client.family.manage.public.generated.resources.family_detail_members
import chefmate.client.family.manage.public.generated.resources.family_detail_pending
import chefmate.client.family.manage.public.generated.resources.family_detail_remove
import chefmate.client.family.manage.public.generated.resources.family_detail_remove_confirm
import chefmate.client.family.manage.public.generated.resources.family_detail_remove_message
import chefmate.client.family.manage.public.generated.resources.family_detail_remove_title
import chefmate.client.family.manage.public.generated.resources.family_detail_rename_a11y
import chefmate.client.family.manage.public.generated.resources.family_detail_rename_confirm
import chefmate.client.family.manage.public.generated.resources.family_detail_rename_title
import chefmate.client.family.manage.public.generated.resources.family_detail_you
import chefmate.client.family.manage.public.generated.resources.family_list_title
import com.plusmobileapps.chefmate.family.data.FamilyMemberStatus
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.Dialog
import com.plusmobileapps.chefmate.family.manage.FamilyDetailBloc.MemberItem
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusAvatar
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusButtonVariant
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusHeaderContainer
import com.plusmobileapps.chefmate.ui.components.PlusHeaderData
import com.plusmobileapps.chefmate.ui.components.PlusTextField
import com.plusmobileapps.chefmate.ui.theme.ChefMateTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun FamilyDetailScreen(bloc: FamilyDetailBloc, modifier: Modifier = Modifier) {
    val model by bloc.state.collectAsState()

    PlusHeaderContainer(
        modifier = modifier.testTag(FamilyDetailTestTags.SCREEN).imePadding(),
        data =
            PlusHeaderData.Child(
                title =
                    if (model.name.isNotBlank()) FixedString(model.name)
                    else Res.string.family_list_title.asTextData(),
                onBackClick = bloc::onBackClicked,
                trailingAccessory =
                    if (model.canRename) {
                        PlusHeaderData.TrailingAccessory.Icon(
                            icon = Icons.Default.Edit,
                            contentDesc = Res.string.family_detail_rename_a11y.asTextData(),
                            onClick = bloc::onRenameClicked,
                        )
                    } else {
                        null
                    },
            ),
    ) {
        val loadError = model.loadError
        when {
            model.isLoading -> CenteredLoading()
            loadError != null -> LoadError(message = loadError, onRetry = bloc::onRetryClicked)
            else -> {
                SectionTitle(Res.string.family_detail_members.asTextData())
                model.members.forEach { item -> MemberRow(item = item, bloc = bloc) }

                if (model.invites.isNotEmpty()) {
                    SectionTitle(
                        Res.string.family_detail_invites.asTextData(),
                        modifier = Modifier.padding(top = ChefMateTheme.dimens.paddingSmall),
                    )
                    model.invites.forEach { item -> MemberRow(item = item, bloc = bloc) }
                }

                if (model.canInvite) {
                    InviteSection(model = model, bloc = bloc)
                }

                if (model.canLeave || model.canDelete) {
                    DangerZone(model = model, bloc = bloc)
                }
            }
        }
    }

    FamilyDetailDialog(model = model, bloc = bloc)
}

@Composable
private fun MemberRow(item: MemberItem, bloc: FamilyDetailBloc) {
    val member = item.member
    val inactive = member.status != FamilyMemberStatus.ACCEPTED
    val primary =
        if (member.isSelf) {
            PhraseModel(Res.string.family_detail_you, "name" to FixedString(member.displayName))
                .localized()
        } else {
            member.displayName
        }
    val secondary =
        listOfNotNull(
                member.email.takeIf { member.displayName != member.email },
                member.role.label().localized(),
                stringResource(Res.string.family_detail_pending).takeIf {
                    member.status == FamilyMemberStatus.PENDING
                },
                stringResource(Res.string.family_detail_declined).takeIf {
                    member.status == FamilyMemberStatus.REJECTED
                },
            )
            .joinToString(" · ")
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = ChefMateTheme.dimens.rowHeight)
                .padding(start = ChefMateTheme.dimens.paddingNormal)
                .testTag(FamilyDetailTestTags.MEMBER_ROW),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
    ) {
        PlusAvatar(
            imageUrl = member.avatarUrl,
            contentDescription = null,
            fallbackText = member.displayName,
            modifier = Modifier.alpha(if (inactive) 0.5f else 1f),
        )
        Column(modifier = Modifier.weight(1f).alpha(if (inactive) 0.6f else 1f)) {
            Text(text = primary, style = ChefMateTheme.typography.bodyLarge)
            Text(
                text = secondary,
                style = ChefMateTheme.typography.bodySmall,
                color = ChefMateTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.hasActions) {
            MemberActionsMenu(item = item, bloc = bloc)
        } else {
            // Keeps text aligned with rows that do have a menu.
            Box(modifier = Modifier.padding(end = ChefMateTheme.dimens.paddingNormal))
        }
    }
}

@Composable
private fun MemberActionsMenu(item: MemberItem, bloc: FamilyDetailBloc) {
    var expanded by remember { mutableStateOf(false) }
    val memberId = item.member.id
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription =
                    PhraseModel(
                            Res.string.family_detail_member_actions_a11y,
                            "name" to FixedString(item.member.displayName),
                        )
                        .localized(),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (item.canPromote) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.family_detail_make_admin)) },
                    onClick = {
                        expanded = false
                        bloc.onPromoteClicked(memberId)
                    },
                )
            }
            if (item.canDemote) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.family_detail_make_member)) },
                    onClick = {
                        expanded = false
                        bloc.onDemoteClicked(memberId)
                    },
                )
            }
            if (item.canRemove) {
                val removeLabel =
                    if (item.member.status == FamilyMemberStatus.ACCEPTED) {
                        Res.string.family_detail_remove
                    } else {
                        Res.string.family_detail_cancel_invite
                    }
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(removeLabel),
                            color = ChefMateTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        expanded = false
                        bloc.onRemoveMemberClicked(memberId)
                    },
                )
            }
        }
    }
}

@Composable
private fun InviteSection(model: FamilyDetailBloc.Model, bloc: FamilyDetailBloc) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = ChefMateTheme.dimens.paddingNormal,
                    vertical = ChefMateTheme.dimens.paddingSmall,
                ),
        verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall),
    ) {
        HorizontalDivider()
        SectionTitle(
            Res.string.family_detail_invite_section.asTextData(),
            modifier = Modifier.padding(top = ChefMateTheme.dimens.paddingSmall),
        )
        PlusTextField(
            value = model.inviteEmail,
            onValueChange = bloc::onInviteEmailChanged,
            modifier = Modifier.fillMaxWidth().testTag(FamilyDetailTestTags.INVITE_EMAIL_FIELD),
            label = { Text(stringResource(Res.string.family_detail_invite_email_label)) },
            singleLine = true,
            error = model.inviteError,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Send,
                ),
            keyboardActions =
                KeyboardActions(
                    onSend = {
                        bloc.onInviteClicked()
                        focusManager.clearFocus()
                    }
                ),
        )
        // Only the owner can invite admins, so admins get no role choice at all.
        if (model.invitableRoles.size > 1) {
            Text(
                text = stringResource(Res.string.family_detail_invite_as),
                style = ChefMateTheme.typography.labelLarge,
                color = ChefMateTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall)
            ) {
                model.invitableRoles.forEach { role ->
                    FilterChip(
                        selected = model.inviteRole == role,
                        onClick = { bloc.onInviteRoleChanged(role) },
                        label = { Text(role.label().localized()) },
                    )
                }
            }
        }
        PlusButton(
            text = Res.string.family_detail_invite_button.asTextData(),
            variant = PlusButtonVariant.SECONDARY,
            isLoading = model.isInviting,
            enabled = model.inviteEmail.isNotBlank(),
            onClick = {
                bloc.onInviteClicked()
                focusManager.clearFocus()
            },
            modifier = Modifier.fillMaxWidth().testTag(FamilyDetailTestTags.INVITE_BUTTON),
        )
    }
}

@Composable
private fun DangerZone(model: FamilyDetailBloc.Model, bloc: FamilyDetailBloc) {
    Column(
        modifier =
            Modifier.fillMaxWidth()
                .padding(
                    horizontal = ChefMateTheme.dimens.paddingNormal,
                    vertical = ChefMateTheme.dimens.paddingLarge,
                ),
        verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingNormal),
    ) {
        HorizontalDivider()
        if (model.canLeave) {
            PlusButton(
                text = Res.string.family_detail_leave_button.asTextData(),
                variant = PlusButtonVariant.DESTRUCTIVE,
                enabled = !model.isWorking,
                onClick = bloc::onLeaveClicked,
                modifier = Modifier.fillMaxWidth().testTag(FamilyDetailTestTags.LEAVE_BUTTON),
            )
        }
        if (model.canDelete) {
            PlusButton(
                text = Res.string.family_detail_delete_button.asTextData(),
                variant = PlusButtonVariant.DESTRUCTIVE,
                enabled = !model.isWorking,
                onClick = bloc::onDeleteClicked,
                modifier = Modifier.fillMaxWidth().testTag(FamilyDetailTestTags.DELETE_BUTTON),
            )
        }
    }
}

@Composable
private fun FamilyDetailDialog(model: FamilyDetailBloc.Model, bloc: FamilyDetailBloc) {
    val familyName = "name" to FixedString(model.name)
    when (val dialog = model.dialog) {
        null -> Unit
        is Dialog.Rename ->
            FamilyNameDialog(
                title = Res.string.family_detail_rename_title.asTextData(),
                confirmText = Res.string.family_detail_rename_confirm.asTextData(),
                name = dialog.name,
                canConfirm = dialog.canConfirm && !model.isWorking,
                isSaving = model.isWorking,
                onNameChange = bloc::onRenameNameChanged,
                onConfirm = bloc::onDialogConfirmed,
                onDismiss = bloc::onDialogDismissed,
            )
        is Dialog.RemoveMember ->
            PlusDialog(
                title = Res.string.family_detail_remove_title.asTextData(),
                message =
                    PhraseModel(
                        Res.string.family_detail_remove_message,
                        "name" to FixedString(dialog.member.displayName),
                    ),
                confirmButtonText = Res.string.family_detail_remove_confirm.asTextData(),
                dismissButtonText = Res.string.family_cancel.asTextData(),
                onConfirmClick = bloc::onDialogConfirmed,
                onDismissRequest = bloc::onDialogDismissed,
            )
        Dialog.Leave ->
            PlusDialog(
                title = Res.string.family_detail_leave_title.asTextData(),
                message = PhraseModel(Res.string.family_detail_leave_message, familyName),
                confirmButtonText = Res.string.family_detail_leave_confirm.asTextData(),
                dismissButtonText = Res.string.family_cancel.asTextData(),
                onConfirmClick = bloc::onDialogConfirmed,
                onDismissRequest = bloc::onDialogDismissed,
            )
        Dialog.Delete ->
            PlusDialog(
                title = Res.string.family_detail_delete_title.asTextData(),
                message = PhraseModel(Res.string.family_detail_delete_message, familyName),
                confirmButtonText = Res.string.family_detail_delete_confirm.asTextData(),
                dismissButtonText = Res.string.family_cancel.asTextData(),
                onConfirmClick = bloc::onDialogConfirmed,
                onDismissRequest = bloc::onDialogDismissed,
            )
    }
}

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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
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
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_create_confirm
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_create_title
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_delete
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_delete_message
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_delete_title
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_items
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_name_label
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_new
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_rename
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_list_rename_title
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_lists
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_lists_empty
import chefmate.client.family.manage.public.generated.resources.family_detail_grocery_lists_hint
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
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_book_create_title
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_book_delete_message
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_book_delete_title
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_book_name_label
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_book_new
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_book_recipes
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_book_rename_title
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_books
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_books_empty
import chefmate.client.family.manage.public.generated.resources.family_detail_recipe_books_hint
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
import com.plusmobileapps.chefmate.recipebook.data.FamilyRecipeBook
import com.plusmobileapps.chefmate.text.FixedString
import com.plusmobileapps.chefmate.text.PhraseModel
import com.plusmobileapps.chefmate.text.PluralResourceString
import com.plusmobileapps.chefmate.text.TextData
import com.plusmobileapps.chefmate.text.asTextData
import com.plusmobileapps.chefmate.ui.components.PlusAvatar
import com.plusmobileapps.chefmate.ui.components.PlusButton
import com.plusmobileapps.chefmate.ui.components.PlusButtonVariant
import com.plusmobileapps.chefmate.ui.components.PlusDialog
import com.plusmobileapps.chefmate.ui.components.PlusDialogScaffold
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
                MembersHeader(model = model, bloc = bloc)
                model.members.forEach { item -> MemberRow(item = item, bloc = bloc) }

                if (model.invites.isNotEmpty()) {
                    SectionTitle(
                        Res.string.family_detail_invites.asTextData(),
                        modifier = Modifier.padding(top = ChefMateTheme.dimens.paddingSmall),
                    )
                    model.invites.forEach { item -> MemberRow(item = item, bloc = bloc) }
                }

                if (model.showGroceryLists) {
                    GroceryListsSection(model = model, bloc = bloc)
                }

                if (model.showRecipeBooks) {
                    RecipeBooksSection(model = model, bloc = bloc)
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
private fun GroceryListsSection(model: FamilyDetailBloc.Model, bloc: FamilyDetailBloc) {
    FamilyCollectionSection(
        title = Res.string.family_detail_grocery_lists.asTextData(),
        hint =
            model.groceryListsError
                ?: if (model.groceryLists.isEmpty()) {
                    Res.string.family_detail_grocery_lists_empty.asTextData()
                } else {
                    Res.string.family_detail_grocery_lists_hint.asTextData()
                },
        isError = model.groceryListsError != null,
        newButtonText = Res.string.family_detail_grocery_list_new.asTextData(),
        newButtonTestTag = FamilyDetailTestTags.NEW_GROCERY_LIST_BUTTON,
        canManage = model.canManageGroceryLists,
        isWorking = model.isWorking,
        onNewClick = bloc::onCreateGroceryListClicked,
    ) {
        model.groceryLists.forEach { list ->
            FamilyCollectionRow(
                name = list.name,
                subtitle =
                    PluralResourceString(
                        Res.plurals.family_detail_grocery_list_items,
                        list.itemCount,
                        mapOf("quantity" to FixedString(list.itemCount.toString())),
                    ),
                canManage = model.canManageGroceryLists,
                testTag = FamilyDetailTestTags.GROCERY_LIST_ROW,
                onRename = { bloc.onRenameGroceryListClicked(list.id) },
                onDelete = { bloc.onDeleteGroceryListClicked(list.id) },
            )
        }
    }
}

@Composable
private fun RecipeBooksSection(model: FamilyDetailBloc.Model, bloc: FamilyDetailBloc) {
    FamilyCollectionSection(
        title = Res.string.family_detail_recipe_books.asTextData(),
        hint =
            model.recipeBooksError
                ?: if (model.recipeBooks.isEmpty()) {
                    Res.string.family_detail_recipe_books_empty.asTextData()
                } else {
                    Res.string.family_detail_recipe_books_hint.asTextData()
                },
        isError = model.recipeBooksError != null,
        newButtonText = Res.string.family_detail_recipe_book_new.asTextData(),
        newButtonTestTag = FamilyDetailTestTags.NEW_RECIPE_BOOK_BUTTON,
        canManage = model.canManageRecipeBooks,
        isWorking = model.isWorking,
        onNewClick = bloc::onCreateRecipeBookClicked,
    ) {
        model.recipeBooks.forEach { book: FamilyRecipeBook ->
            FamilyCollectionRow(
                name = book.name,
                subtitle =
                    PluralResourceString(
                        Res.plurals.family_detail_recipe_book_recipes,
                        book.recipeCount,
                        mapOf("quantity" to FixedString(book.recipeCount.toString())),
                    ),
                canManage = model.canManageRecipeBooks,
                testTag = FamilyDetailTestTags.RECIPE_BOOK_ROW,
                onRename = { bloc.onRenameRecipeBookClicked(book.id) },
                onDelete = { bloc.onDeleteRecipeBookClicked(book.id) },
            )
        }
    }
}

/**
 * A section of things the family shares (grocery lists, recipe books): a title, a hint (or the load
 * error), the [rows], and for owners and admins a button to add one.
 */
@Composable
private fun FamilyCollectionSection(
    title: TextData,
    hint: TextData,
    isError: Boolean,
    newButtonText: TextData,
    newButtonTestTag: String,
    canManage: Boolean,
    isWorking: Boolean,
    onNewClick: () -> Unit,
    rows: @Composable () -> Unit,
) {
    val dimens = ChefMateTheme.dimens
    HorizontalDivider(modifier = Modifier.padding(horizontal = dimens.paddingNormal))
    SectionTitle(title, modifier = Modifier.padding(top = dimens.paddingSmall))
    Text(
        text = hint.localized(),
        style = ChefMateTheme.typography.bodySmall,
        color =
            if (isError) ChefMateTheme.colorScheme.error
            else ChefMateTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = dimens.paddingNormal),
    )
    rows()
    if (canManage) {
        PlusButton(
            text = newButtonText,
            variant = PlusButtonVariant.SECONDARY,
            enabled = !isWorking,
            onClick = onNewClick,
            modifier =
                Modifier.fillMaxWidth()
                    .padding(horizontal = dimens.paddingNormal, vertical = dimens.paddingSmall)
                    .testTag(newButtonTestTag),
        )
    }
}

@Composable
private fun FamilyCollectionRow(
    name: String,
    subtitle: TextData,
    canManage: Boolean,
    testTag: String,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .heightIn(min = ChefMateTheme.dimens.rowHeight)
                .padding(start = ChefMateTheme.dimens.paddingNormal)
                .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = ChefMateTheme.typography.bodyLarge)
            Text(
                text = subtitle.localized(),
                style = ChefMateTheme.typography.bodySmall,
                color = ChefMateTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canManage) {
            FamilyCollectionActionsMenu(name = name, onRename = onRename, onDelete = onDelete)
        } else {
            Box(modifier = Modifier.padding(end = ChefMateTheme.dimens.paddingNormal))
        }
    }
}

@Composable
private fun FamilyCollectionActionsMenu(name: String, onRename: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription =
                    PhraseModel(
                            Res.string.family_detail_member_actions_a11y,
                            "name" to FixedString(name),
                        )
                        .localized(),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.family_detail_grocery_list_rename)) },
                onClick = {
                    expanded = false
                    onRename()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(Res.string.family_detail_grocery_list_delete),
                        color = ChefMateTheme.colorScheme.error,
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

/** The Members title, with the invite button trailing it for users who may invite. */
@Composable
private fun MembersHeader(model: FamilyDetailBloc.Model, bloc: FamilyDetailBloc) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = ChefMateTheme.dimens.paddingExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(
            Res.string.family_detail_members.asTextData(),
            modifier = Modifier.weight(1f),
        )
        if (model.canInvite) {
            IconButton(
                onClick = bloc::onInviteSomeoneClicked,
                modifier = Modifier.testTag(FamilyDetailTestTags.INVITE_OPEN_BUTTON),
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = stringResource(Res.string.family_detail_invite_section),
                    tint = ChefMateTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun InviteDialog(model: FamilyDetailBloc.Model, bloc: FamilyDetailBloc) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val canSend = model.inviteEmail.isNotBlank() && !model.isInviting
    PlusDialogScaffold(
        onDismissRequest = bloc::onDialogDismissed,
        header = { Text(stringResource(Res.string.family_detail_invite_section)) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall)) {
                PlusTextField(
                    value = model.inviteEmail,
                    onValueChange = bloc::onInviteEmailChanged,
                    modifier =
                        Modifier.fillMaxWidth().testTag(FamilyDetailTestTags.INVITE_EMAIL_FIELD),
                    label = { Text(stringResource(Res.string.family_detail_invite_email_label)) },
                    singleLine = true,
                    error = model.inviteError,
                    focusRequester = focusRequester,
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Send,
                        ),
                    keyboardActions =
                        KeyboardActions(onSend = { if (canSend) bloc.onInviteClicked() }),
                )
                // Only the owner can invite admins, so admins get no role choice at all.
                if (model.invitableRoles.size > 1) {
                    Text(
                        text = stringResource(Res.string.family_detail_invite_as),
                        style = ChefMateTheme.typography.labelLarge,
                        color = ChefMateTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement =
                            Arrangement.spacedBy(ChefMateTheme.dimens.paddingSmall)
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
            }
        },
        footer = {
            Row(horizontalArrangement = Arrangement.spacedBy(ChefMateTheme.dimens.paddingNormal)) {
                PlusButton(
                    text = Res.string.family_cancel.asTextData(),
                    variant = PlusButtonVariant.SECONDARY,
                    onClick = bloc::onDialogDismissed,
                )
                PlusButton(
                    text = Res.string.family_detail_invite_button.asTextData(),
                    isLoading = model.isInviting,
                    enabled = canSend,
                    onClick = bloc::onInviteClicked,
                    modifier = Modifier.testTag(FamilyDetailTestTags.INVITE_BUTTON),
                )
            }
        },
    )
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
        Dialog.Invite -> InviteDialog(model = model, bloc = bloc)
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
        is Dialog.CreateGroceryList ->
            FamilyNameDialog(
                title = Res.string.family_detail_grocery_list_create_title.asTextData(),
                confirmText = Res.string.family_detail_grocery_list_create_confirm.asTextData(),
                name = dialog.name,
                canConfirm = dialog.canConfirm && !model.isWorking,
                isSaving = model.isWorking,
                onNameChange = bloc::onGroceryListNameChanged,
                onConfirm = bloc::onDialogConfirmed,
                onDismiss = bloc::onDialogDismissed,
                label = Res.string.family_detail_grocery_list_name_label.asTextData(),
                fieldTestTag = FamilyDetailTestTags.GROCERY_LIST_NAME_FIELD,
            )
        is Dialog.RenameGroceryList ->
            FamilyNameDialog(
                title = Res.string.family_detail_grocery_list_rename_title.asTextData(),
                confirmText = Res.string.family_detail_rename_confirm.asTextData(),
                name = dialog.name,
                canConfirm = dialog.canConfirm && !model.isWorking,
                isSaving = model.isWorking,
                onNameChange = bloc::onGroceryListNameChanged,
                onConfirm = bloc::onDialogConfirmed,
                onDismiss = bloc::onDialogDismissed,
                label = Res.string.family_detail_grocery_list_name_label.asTextData(),
                fieldTestTag = FamilyDetailTestTags.GROCERY_LIST_NAME_FIELD,
            )
        is Dialog.DeleteGroceryList ->
            PlusDialog(
                title = Res.string.family_detail_grocery_list_delete_title.asTextData(),
                message =
                    PhraseModel(
                        Res.string.family_detail_grocery_list_delete_message,
                        "name" to FixedString(dialog.list.name),
                    ),
                confirmButtonText = Res.string.family_detail_delete_confirm.asTextData(),
                dismissButtonText = Res.string.family_cancel.asTextData(),
                onConfirmClick = bloc::onDialogConfirmed,
                onDismissRequest = bloc::onDialogDismissed,
            )
        is Dialog.CreateRecipeBook ->
            FamilyNameDialog(
                title = Res.string.family_detail_recipe_book_create_title.asTextData(),
                confirmText = Res.string.family_detail_grocery_list_create_confirm.asTextData(),
                name = dialog.name,
                canConfirm = dialog.canConfirm && !model.isWorking,
                isSaving = model.isWorking,
                onNameChange = bloc::onRecipeBookNameChanged,
                onConfirm = bloc::onDialogConfirmed,
                onDismiss = bloc::onDialogDismissed,
                label = Res.string.family_detail_recipe_book_name_label.asTextData(),
                fieldTestTag = FamilyDetailTestTags.RECIPE_BOOK_NAME_FIELD,
            )
        is Dialog.RenameRecipeBook ->
            FamilyNameDialog(
                title = Res.string.family_detail_recipe_book_rename_title.asTextData(),
                confirmText = Res.string.family_detail_rename_confirm.asTextData(),
                name = dialog.name,
                canConfirm = dialog.canConfirm && !model.isWorking,
                isSaving = model.isWorking,
                onNameChange = bloc::onRecipeBookNameChanged,
                onConfirm = bloc::onDialogConfirmed,
                onDismiss = bloc::onDialogDismissed,
                label = Res.string.family_detail_recipe_book_name_label.asTextData(),
                fieldTestTag = FamilyDetailTestTags.RECIPE_BOOK_NAME_FIELD,
            )
        is Dialog.DeleteRecipeBook ->
            PlusDialog(
                title = Res.string.family_detail_recipe_book_delete_title.asTextData(),
                message =
                    PhraseModel(
                        Res.string.family_detail_recipe_book_delete_message,
                        "name" to FixedString(dialog.book.name),
                    ),
                confirmButtonText = Res.string.family_detail_delete_confirm.asTextData(),
                dismissButtonText = Res.string.family_cancel.asTextData(),
                onConfirmClick = bloc::onDialogConfirmed,
                onDismissRequest = bloc::onDialogDismissed,
            )
    }
}

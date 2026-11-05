import SwiftUI
import ComposeApp

/// The redesigned family detail, mirroring the Android `FamilyDetailScreen`: members (with a
/// per-member actions menu), invites, the family's shared grocery lists and recipe books, and the
/// leave / delete actions. Form dialogs (rename, invite, create / rename a list or book) are sheets;
/// confirmations are alerts. What the user may do comes from the BLoC (`FamilyPermissions`).
struct FamilyDetailView: View {
    let bloc: FamilyDetailBloc
    @StateObject private var model: ObservedFlow<FamilyDetailBlocModel>

    init(bloc: FamilyDetailBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        content(state)
            .navigationTitle(state.name.isEmpty ? ComposeStrings["family_list_title"] : state.name)
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    if state.isWorking {
                        ProgressView()
                    }
                    if state.canRename {
                        Button(action: bloc.onRenameClicked) {
                            Image(systemName: "pencil")
                        }
                        .disabled(state.isWorking)
                        .accessibilityLabel(ComposeStrings["family_detail_rename_a11y"])
                    }
                }
            }
            .tint(ChefTheme.primary)
            .sheet(isPresented: dialogBinding(isSheet)) {
                FamilyDetailSheet(state: model.value, bloc: bloc)
            }
            .alert(
                alertTitle(state.dialog),
                isPresented: dialogBinding(isAlert)
            ) {
                Button(alertConfirmText(state.dialog), role: .destructive, action: bloc.onDialogConfirmed)
                Button(ComposeStrings["family_cancel"], role: .cancel, action: bloc.onDialogDismissed)
            } message: {
                Text(alertMessage(state))
            }
    }

    @ViewBuilder
    private func content(_ state: FamilyDetailBlocModel) -> some View {
        if state.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let error = state.loadError {
            FamilyLoadError(message: error, onRetry: bloc.onRetryClicked)
        } else {
            List {
                Section {
                    ForEach(state.members, id: \.member.id) { MemberRow(item: $0, bloc: bloc) }
                } header: {
                    HStack {
                        Text(ComposeStrings["family_detail_members"])
                        Spacer()
                        if state.canInvite {
                            Button(action: bloc.onInviteSomeoneClicked) {
                                Image(systemName: "person.badge.plus")
                                    .font(.body)
                            }
                            .accessibilityLabel(ComposeStrings["family_detail_invite_section"])
                        }
                    }
                }

                if !state.invites.isEmpty {
                    Section(ComposeStrings["family_detail_invites"]) {
                        ForEach(state.invites, id: \.member.id) { MemberRow(item: $0, bloc: bloc) }
                    }
                }

                if state.showGroceryLists {
                    CollectionSection(
                        title: ComposeStrings["family_detail_grocery_lists"],
                        hint: state.groceryListsError?.localized ?? ComposeStrings[
                            state.groceryLists.isEmpty ? "family_detail_grocery_lists_empty" : "family_detail_grocery_lists_hint"
                        ],
                        isError: state.groceryListsError != nil,
                        newButtonText: ComposeStrings["family_detail_grocery_list_new"],
                        canManage: state.canManageGroceryLists,
                        isWorking: state.isWorking,
                        rows: state.groceryLists.map { list in
                            CollectionRow(
                                id: list.id,
                                name: list.name,
                                subtitle: ComposeStrings.shared.plural(key: "family_detail_grocery_list_items", quantity: list.itemCount),
                                onRename: { bloc.onRenameGroceryListClicked(listId: list.id) },
                                onDelete: { bloc.onDeleteGroceryListClicked(listId: list.id) }
                            )
                        },
                        onNew: bloc.onCreateGroceryListClicked
                    )
                }

                if state.showRecipeBooks {
                    CollectionSection(
                        title: ComposeStrings["family_detail_recipe_books"],
                        hint: state.recipeBooksError?.localized ?? ComposeStrings[
                            state.recipeBooks.isEmpty ? "family_detail_recipe_books_empty" : "family_detail_recipe_books_hint"
                        ],
                        isError: state.recipeBooksError != nil,
                        newButtonText: ComposeStrings["family_detail_recipe_book_new"],
                        canManage: state.canManageRecipeBooks,
                        isWorking: state.isWorking,
                        rows: state.recipeBooks.map { book in
                            CollectionRow(
                                id: book.id,
                                name: book.name,
                                subtitle: ComposeStrings.shared.plural(key: "family_detail_recipe_book_recipes", quantity: book.recipeCount),
                                onRename: { bloc.onRenameRecipeBookClicked(bookId: book.id) },
                                onDelete: { bloc.onDeleteRecipeBookClicked(bookId: book.id) }
                            )
                        },
                        onNew: bloc.onCreateRecipeBookClicked
                    )
                }

                if state.canLeave || state.canDelete {
                    Section {
                        if state.canLeave {
                            Button(ComposeStrings["family_detail_leave_button"], role: .destructive, action: bloc.onLeaveClicked)
                                .frame(maxWidth: .infinity)
                        }
                        if state.canDelete {
                            Button(ComposeStrings["family_detail_delete_button"], role: .destructive, action: bloc.onDeleteClicked)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .disabled(state.isWorking)
                }
            }
            .listStyle(.insetGrouped)
        }
    }

    // MARK: Dialogs

    /// Presents while the BLoC's dialog matches `matches`. Confirming clears the dialog in the BLoC
    /// first, so the setter only reports a user dismissal (swipe down, tap outside).
    private func dialogBinding(_ matches: @escaping (FamilyDetailBlocDialog?) -> Bool) -> Binding<Bool> {
        Binding(
            get: { matches(model.current.dialog) },
            set: { if !$0 && matches(model.current.dialog) { bloc.onDialogDismissed() } }
        )
    }

    private func isAlert(_ dialog: FamilyDetailBlocDialog?) -> Bool {
        dialog is FamilyDetailBlocDialog.RemoveMember
            || dialog is FamilyDetailBlocDialog.Leave
            || dialog is FamilyDetailBlocDialog.Delete
            || dialog is FamilyDetailBlocDialog.DeleteGroceryList
            || dialog is FamilyDetailBlocDialog.DeleteRecipeBook
    }

    private func isSheet(_ dialog: FamilyDetailBlocDialog?) -> Bool {
        dialog != nil && !isAlert(dialog)
    }

    private func alertTitle(_ dialog: FamilyDetailBlocDialog?) -> String {
        switch dialog {
        case is FamilyDetailBlocDialog.RemoveMember: ComposeStrings["family_detail_remove_title"]
        case is FamilyDetailBlocDialog.Leave: ComposeStrings["family_detail_leave_title"]
        case is FamilyDetailBlocDialog.Delete: ComposeStrings["family_detail_delete_title"]
        case is FamilyDetailBlocDialog.DeleteGroceryList: ComposeStrings["family_detail_grocery_list_delete_title"]
        case is FamilyDetailBlocDialog.DeleteRecipeBook: ComposeStrings["family_detail_recipe_book_delete_title"]
        default: ""
        }
    }

    private func alertConfirmText(_ dialog: FamilyDetailBlocDialog?) -> String {
        switch dialog {
        case is FamilyDetailBlocDialog.RemoveMember: ComposeStrings["family_detail_remove_confirm"]
        case is FamilyDetailBlocDialog.Leave: ComposeStrings["family_detail_leave_confirm"]
        default: ComposeStrings["family_detail_delete_confirm"]
        }
    }

    private func alertMessage(_ state: FamilyDetailBlocModel) -> String {
        switch state.dialog {
        case let remove as FamilyDetailBlocDialog.RemoveMember:
            familyPhrase("family_detail_remove_message", ["name": remove.member.displayName])
        case is FamilyDetailBlocDialog.Leave:
            familyPhrase("family_detail_leave_message", ["name": state.name])
        case is FamilyDetailBlocDialog.Delete:
            familyPhrase("family_detail_delete_message", ["name": state.name])
        case let delete as FamilyDetailBlocDialog.DeleteGroceryList:
            familyPhrase("family_detail_grocery_list_delete_message", ["name": delete.list.name])
        case let delete as FamilyDetailBlocDialog.DeleteRecipeBook:
            familyPhrase("family_detail_recipe_book_delete_message", ["name": delete.book.name])
        default: ""
        }
    }
}

/// The sheet for whichever form dialog the BLoC is showing.
private struct FamilyDetailSheet: View {
    let state: FamilyDetailBlocModel
    let bloc: FamilyDetailBloc

    var body: some View {
        let saving = state.isWorking
        switch state.dialog {
        case is FamilyDetailBlocDialog.Invite:
            InviteSheet(state: state, bloc: bloc)
        case let rename as FamilyDetailBlocDialog.Rename:
            nameSheet(
                title: "family_detail_rename_title", confirm: "family_detail_rename_confirm", label: "family_name_label",
                name: rename.name, canConfirm: rename.canConfirm && !saving,
                onNameChange: { bloc.onRenameNameChanged(name: $0) }
            )
        case let create as FamilyDetailBlocDialog.CreateGroceryList:
            nameSheet(
                title: "family_detail_grocery_list_create_title", confirm: "family_detail_grocery_list_create_confirm",
                label: "family_detail_grocery_list_name_label",
                name: create.name, canConfirm: create.canConfirm && !saving,
                onNameChange: { bloc.onGroceryListNameChanged(name: $0) }
            )
        case let rename as FamilyDetailBlocDialog.RenameGroceryList:
            nameSheet(
                title: "family_detail_grocery_list_rename_title", confirm: "family_detail_rename_confirm",
                label: "family_detail_grocery_list_name_label",
                name: rename.name, canConfirm: rename.canConfirm && !saving,
                onNameChange: { bloc.onGroceryListNameChanged(name: $0) }
            )
        case let create as FamilyDetailBlocDialog.CreateRecipeBook:
            nameSheet(
                title: "family_detail_recipe_book_create_title", confirm: "family_detail_grocery_list_create_confirm",
                label: "family_detail_recipe_book_name_label",
                name: create.name, canConfirm: create.canConfirm && !saving,
                onNameChange: { bloc.onRecipeBookNameChanged(name: $0) }
            )
        case let rename as FamilyDetailBlocDialog.RenameRecipeBook:
            nameSheet(
                title: "family_detail_recipe_book_rename_title", confirm: "family_detail_rename_confirm",
                label: "family_detail_recipe_book_name_label",
                name: rename.name, canConfirm: rename.canConfirm && !saving,
                onNameChange: { bloc.onRecipeBookNameChanged(name: $0) }
            )
        default:
            EmptyView()
        }
    }

    private func nameSheet(
        title: String, confirm: String, label: String,
        name: String, canConfirm: Bool, onNameChange: @escaping (String) -> Void
    ) -> FamilyNameSheet {
        FamilyNameSheet(
            title: ComposeStrings[title],
            confirmText: ComposeStrings[confirm],
            label: ComposeStrings[label],
            name: name,
            canConfirm: canConfirm,
            isSaving: state.isWorking,
            error: nil,
            onNameChange: onNameChange,
            onConfirm: bloc.onDialogConfirmed,
            onDismiss: bloc.onDialogDismissed
        )
    }
}

/// Invite by email, with a role choice for owners (admins can only invite members).
private struct InviteSheet: View {
    let state: FamilyDetailBlocModel
    let bloc: FamilyDetailBloc
    @FocusState private var focused: Bool

    var body: some View {
        let canSend = !state.inviteEmail.trimmingCharacters(in: .whitespaces).isEmpty && !state.isInviting
        NavigationStack {
            Form {
                Section {
                    TextField(
                        ComposeStrings["family_detail_invite_email_label"],
                        text: Binding(get: { state.inviteEmail }, set: { bloc.onInviteEmailChanged(email: $0) })
                    )
                    .keyboardType(.emailAddress)
                    .textContentType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .submitLabel(.send)
                    .focused($focused)
                    .onSubmit { if canSend { bloc.onInviteClicked() } }
                } footer: {
                    if let error = state.inviteError {
                        LocalizedText(text: error).foregroundStyle(.red)
                    }
                }
                if state.invitableRoles.count > 1 {
                    Section(ComposeStrings["family_detail_invite_as"]) {
                        Picker(
                            ComposeStrings["family_detail_invite_as"],
                            selection: Binding(get: { state.inviteRole }, set: { bloc.onInviteRoleChanged(role: $0) })
                        ) {
                            ForEach(state.invitableRoles, id: \.self) { role in
                                Text(role.labelText).tag(role)
                            }
                        }
                        .pickerStyle(.segmented)
                        .labelsHidden()
                    }
                }
            }
            .navigationTitle(ComposeStrings["family_detail_invite_section"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(ComposeStrings["family_cancel"], action: bloc.onDialogDismissed)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if state.isInviting {
                        ProgressView()
                    } else {
                        Button(ComposeStrings["family_detail_invite_button"], action: bloc.onInviteClicked)
                            .disabled(!canSend)
                    }
                }
            }
        }
        .tint(ChefTheme.primary)
        .presentationDetents([.medium])
        .interactiveDismissDisabled(state.isInviting)
        .onAppear { focused = true }
    }
}

private struct MemberRow: View {
    let item: FamilyDetailBlocMemberItem
    let bloc: FamilyDetailBloc

    var body: some View {
        let member = item.member
        let inactive = member.status != .accepted
        let primary = member.isSelf
            ? familyPhrase("family_detail_you", ["name": member.displayName])
            : member.displayName
        let secondary = [
            member.displayName != member.email ? member.email : nil,
            member.role.labelText,
            member.status == .pending ? ComposeStrings["family_detail_pending"] : nil,
            member.status == .rejected ? ComposeStrings["family_detail_declined"] : nil,
        ].compactMap { $0 }.joined(separator: " · ")

        HStack(spacing: ChefTheme.Spacing.normal - 4) {
            FamilyAvatar(name: member.displayName, imageUrl: member.avatarUrl)
                .opacity(inactive ? 0.5 : 1)
            VStack(alignment: .leading, spacing: 2) {
                Text(primary)
                Text(secondary)
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
            .opacity(inactive ? 0.6 : 1)
            Spacer(minLength: 0)
            if item.hasActions {
                Menu {
                    if item.canPromote {
                        Button(ComposeStrings["family_detail_make_admin"], systemImage: "person.badge.shield.checkmark") {
                            bloc.onPromoteClicked(memberId: member.id)
                        }
                    }
                    if item.canDemote {
                        Button(ComposeStrings["family_detail_make_member"], systemImage: "person") {
                            bloc.onDemoteClicked(memberId: member.id)
                        }
                    }
                    if item.canRemove {
                        Button(
                            ComposeStrings[member.status == .accepted ? "family_detail_remove" : "family_detail_cancel_invite"],
                            systemImage: member.status == .accepted ? "person.badge.minus" : "xmark",
                            role: .destructive
                        ) {
                            bloc.onRemoveMemberClicked(memberId: member.id)
                        }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .font(.title3)
                }
                .accessibilityLabel(familyPhrase("family_detail_member_actions_a11y", ["name": member.displayName]))
            }
        }
        .frame(minHeight: ChefTheme.Spacing.rowHeight - 12)
    }
}

/// One grocery list / recipe book row in a `CollectionSection`.
private struct CollectionRow: Identifiable {
    let id: String
    let name: String
    let subtitle: String
    let onRename: () -> Void
    let onDelete: () -> Void
}

/// A section of things the family shares (grocery lists, recipe books): the rows, a hint (or the
/// load error) as the footer, and for owners and admins a button to add one.
private struct CollectionSection: View {
    let title: String
    let hint: String
    let isError: Bool
    let newButtonText: String
    let canManage: Bool
    let isWorking: Bool
    let rows: [CollectionRow]
    let onNew: () -> Void

    var body: some View {
        Section {
            ForEach(rows) { row in
                HStack {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(row.name)
                        Text(row.subtitle)
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                    if canManage {
                        Menu {
                            Button(ComposeStrings["family_detail_grocery_list_rename"], systemImage: "pencil", action: row.onRename)
                            Button(ComposeStrings["family_detail_grocery_list_delete"], systemImage: "trash", role: .destructive, action: row.onDelete)
                        } label: {
                            Image(systemName: "ellipsis.circle")
                                .font(.title3)
                        }
                        .accessibilityLabel(familyPhrase("family_detail_member_actions_a11y", ["name": row.name]))
                    }
                }
                .frame(minHeight: ChefTheme.Spacing.rowHeight - 12)
            }
            if canManage {
                Button(action: onNew) {
                    Label(newButtonText, systemImage: "plus")
                }
                .disabled(isWorking)
            }
        } header: {
            Text(title)
        } footer: {
            Text(hint).foregroundStyle(isError ? Color.red : Color.secondary)
        }
    }
}

#Preview("Owner") {
    NavigationStack { FamilyDetailView(bloc: FamilyPreviewsKt.previewFamilyDetailOwnerBloc) }
}

#Preview("Member") {
    NavigationStack { FamilyDetailView(bloc: FamilyPreviewsKt.previewFamilyDetailMemberBloc) }
}

#Preview("Shared sections") {
    NavigationStack { FamilyDetailView(bloc: FamilyPreviewsKt.previewFamilyDetailSharedSectionsBloc) }
}

#Preview("Invite") {
    NavigationStack { FamilyDetailView(bloc: FamilyPreviewsKt.previewFamilyDetailInviteDialogBloc) }
}

#Preview("Delete") {
    NavigationStack { FamilyDetailView(bloc: FamilyPreviewsKt.previewFamilyDetailDeleteDialogBloc) }
}

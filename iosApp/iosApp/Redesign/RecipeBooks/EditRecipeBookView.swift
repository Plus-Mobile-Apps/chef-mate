import SwiftUI
import ComposeApp

/// The redesigned create / edit recipe book form, mirroring the Android `EditRecipeBookScreen`:
/// name and Save, then (for a shared book) collaborators grouped by role with owner-only invite
/// controls, and the delete-or-leave action last. Presented as a sheet (the Android modal) with its
/// own navigation bar, so it carries a Close button instead of Back.
struct EditRecipeBookView: View {
    let bloc: EditRecipeBookBloc
    @StateObject private var model: ObservedFlow<EditRecipeBookBlocModel>
    @FocusState private var focusedField: Field?

    private enum Field { case name, inviteEmail }

    init(bloc: EditRecipeBookBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        Form {
            Section {
                TextField(
                    ComposeStrings["edit_recipe_book_name_label"],
                    text: Binding(get: { model.current.name }, set: { bloc.onNameChanged(name: $0) })
                )
                .focused($focusedField, equals: .name)
                .submitLabel(.done)
                .onSubmit { if model.current.canSave { bloc.onSaveClicked() } }
            } footer: {
                if let error = state.nameError {
                    LocalizedText(text: error).foregroundStyle(.red)
                }
            }

            if !state.members.isEmpty {
                CollaboratorSections(bloc: bloc, model: model, focusedField: $focusedField, inviteField: .inviteEmail)
            }

            if state.canDeleteBook || state.canLeaveBook {
                Section {
                    Button(role: .destructive) {
                        if state.canDeleteBook { bloc.onDeleteBookClicked() } else { bloc.onLeaveBookClicked() }
                    } label: {
                        HStack {
                            Text(ComposeStrings[state.canDeleteBook ? "edit_recipe_book_delete_button" : "edit_recipe_book_leave_button"])
                            if state.isRemovingBook {
                                Spacer()
                                ProgressView()
                            }
                        }
                    }
                    .disabled(state.isRemovingBook)
                } footer: {
                    if let error = state.bookActionError {
                        LocalizedText(text: error).foregroundStyle(.red)
                    }
                }
            }
        }
        .navigationTitle(state.title.localized)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(action: bloc.onCloseClicked) {
                    Label(ComposeStrings["edit_recipe_book_close"], systemImage: "xmark")
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                if state.isSaving {
                    ProgressView()
                } else {
                    Button(ComposeStrings["edit_recipe_book_save"], action: bloc.onSaveClicked)
                        .disabled(!state.canSave)
                }
            }
        }
        .tint(ChefTheme.primary)
        .onAppear {
            // A new book starts with the keyboard up, like the Android form.
            if state.isCreate { focusedField = .name }
        }
        .alert(
            ComposeStrings[state.pendingBookAction == .delete_ ? "edit_recipe_book_delete_title" : "edit_recipe_book_leave_title"],
            isPresented: Binding(
                get: { model.value.pendingBookAction != nil },
                set: { if !$0 && model.current.pendingBookAction != nil { bloc.onDismissBookAction() } }
            )
        ) {
            let isDelete = state.pendingBookAction == .delete_
            Button(
                ComposeStrings[isDelete ? "edit_recipe_book_delete_confirm" : "edit_recipe_book_leave_confirm"],
                role: .destructive,
                action: bloc.onConfirmBookAction
            )
            Button(ComposeStrings["edit_recipe_book_cancel"], role: .cancel, action: bloc.onDismissBookAction)
        } message: {
            let key = state.pendingBookAction == .delete_ ? "edit_recipe_book_delete_message" : "edit_recipe_book_leave_message"
            Text(ComposeStrings[key, ["name": state.name]])
        }
        .alert(
            ComposeStrings["edit_recipe_book_remove_title"],
            isPresented: Binding(
                get: { model.value.removingMember != nil },
                set: { if !$0 && model.current.removingMember != nil { bloc.onDismissRemoveMember() } }
            )
        ) {
            Button(ComposeStrings["edit_recipe_book_remove_confirm"], role: .destructive, action: bloc.onConfirmRemoveMember)
            Button(ComposeStrings["edit_recipe_book_remove_cancel"], role: .cancel, action: bloc.onDismissRemoveMember)
        } message: {
            if let member = state.removingMember {
                Text(ComposeStrings["edit_recipe_book_remove_message", ["name": member.displayName]])
            }
        }
    }
}

/// Collaborators grouped by role (owner, editors, viewers), then the owner-only invite controls.
/// Pending invites keep their invited role, so they land in the matching group, dimmed.
private struct CollaboratorSections<FocusValue: Hashable>: View {
    let bloc: EditRecipeBookBloc
    @ObservedObject var model: ObservedFlow<EditRecipeBookBlocModel>
    var focusedField: FocusState<FocusValue?>.Binding
    let inviteField: FocusValue

    var body: some View {
        let state = model.value
        let members = state.members
        let groups: [(String, [RecipeBookMember])] = [
            ("edit_recipe_book_group_owner", members.filter { $0.isOwner || $0.role == .owner }),
            ("edit_recipe_book_group_editors", members.filter { !$0.isOwner && $0.role == .editor }),
            ("edit_recipe_book_group_viewers", members.filter { !$0.isOwner && $0.role == .viewer }),
        ]
        ForEach(groups.filter { !$0.1.isEmpty }, id: \.0) { key, groupMembers in
            Section {
                ForEach(groupMembers, id: \.email) { member in
                    MemberRow(member: member, canManage: state.canManageCollaborators) { id in
                        bloc.onRemoveMemberClicked(memberId: id)
                    }
                }
            } header: {
                if key == groups.first(where: { !$0.1.isEmpty })?.0 {
                    // The first group also carries the section title, like the Android header.
                    VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
                        Text(ComposeStrings["edit_recipe_book_collaborators"])
                            .font(.headline)
                            .foregroundStyle(.primary)
                            .textCase(nil)
                        Text(ComposeStrings[key])
                    }
                } else {
                    Text(ComposeStrings[key])
                }
            }
        }

        if state.canManageCollaborators {
            Section {
                TextField(
                    ComposeStrings["edit_recipe_book_invite_email_label"],
                    text: Binding(get: { model.current.inviteEmail }, set: { bloc.onInviteEmailChanged(email: $0) })
                )
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.send)
                .focused(focusedField, equals: inviteField)
                .onSubmit {
                    bloc.onInviteClicked()
                    focusedField.wrappedValue = nil
                }

                Picker(ComposeStrings["edit_recipe_book_role_editor"], selection: Binding(
                    get: { model.current.inviteRole },
                    set: { bloc.onInviteRoleChanged(role: $0) }
                )) {
                    Text(ComposeStrings["edit_recipe_book_role_editor"]).tag(RecipeBookRole.editor)
                    Text(ComposeStrings["edit_recipe_book_role_viewer"]).tag(RecipeBookRole.viewer)
                }
                .pickerStyle(.segmented)
                .labelsHidden()

                Button {
                    bloc.onInviteClicked()
                    focusedField.wrappedValue = nil
                } label: {
                    HStack {
                        Text(ComposeStrings["edit_recipe_book_invite_button"])
                        if state.isInviting {
                            Spacer()
                            ProgressView()
                        }
                    }
                }
                .disabled(state.inviteEmail.trimmingCharacters(in: .whitespaces).isEmpty || state.isInviting)
            } footer: {
                if let error = state.inviteError {
                    LocalizedText(text: error).foregroundStyle(.red)
                }
            }
        }
    }
}

private struct MemberRow: View {
    let member: RecipeBookMember
    let canManage: Bool
    let onRemove: (String) -> Void

    var body: some View {
        let name = member.name.flatMap { $0.trimmingCharacters(in: .whitespaces).isEmpty ? nil : $0 }
        let pending = member.status == .pending
        let declined = member.status == .rejected
        let secondary = [
            name != nil ? member.email : nil,
            pending ? ComposeStrings["edit_recipe_book_member_pending"] : nil,
            declined ? ComposeStrings["edit_recipe_book_member_declined"] : nil,
        ].compactMap { $0 }.joined(separator: " · ")

        HStack(spacing: ChefTheme.Spacing.normal - 4) {
            LetterAvatar(name: name ?? member.email, imageUrl: member.avatarUrl)
                .frame(width: 36, height: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(name ?? member.email)
                if !secondary.isEmpty {
                    Text(secondary)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 0)
            if canManage && !member.isOwner, let id = member.id {
                Button { onRemove(id) } label: {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(ComposeStrings["edit_recipe_book_remove_member"].replacingOccurrences(of: "%1$@", with: member.email))
            }
        }
        .opacity(pending || declined ? 0.5 : 1)
        .accessibilityElement(children: .combine)
    }
}

private extension RecipeBookMember {
    var displayName: String {
        if let name, !name.trimmingCharacters(in: .whitespaces).isEmpty { return name }
        return email
    }
}

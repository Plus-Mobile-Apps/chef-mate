import SwiftUI
import ComposeApp

/// The redesigned grocery list settings, mirroring the Android `EditGroceryListScreen`: rename
/// (owner only), collaborators grouped by role with owner-only invite controls (or a sign-in
/// prompt when signed out), and delete last. Presented as a sheet (the Android modal) with its own
/// navigation bar, so it carries a Close button instead of Back.
struct EditGroceryListView: View {
    let bloc: EditGroceryListBloc
    @StateObject private var model: ObservedFlow<EditGroceryListBlocModel>
    @FocusState private var inviteFocused: Bool

    init(bloc: EditGroceryListBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        Form {
            Section {
                TextField(
                    ComposeStrings["grocery_list_name_label"],
                    text: Binding(get: { model.current.name }, set: { bloc.onNameChanged(name: $0) })
                )
                .disabled(!state.isOwner)
                .submitLabel(.done)
                .onSubmit { if isNameValid { bloc.onRenameSubmitted() } }
                if state.isOwner {
                    Button(ComposeStrings["grocery_rename_save"], action: bloc.onRenameSubmitted)
                        .disabled(!isNameValid)
                }
            }

            if state.isAuthenticated {
                collaborators(state)
            } else {
                Section(ComposeStrings["grocery_collaborators"]) {
                    VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
                        Text(ComposeStrings["grocery_collab_signed_out_title"]).font(.headline)
                        Text(ComposeStrings["grocery_collab_signed_out_message"])
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        HStack {
                            Button(ComposeStrings["sign_in"], action: bloc.onSignInClicked)
                                .buttonStyle(.borderedProminent)
                            Button(ComposeStrings["sign_up"], action: bloc.onSignUpClicked)
                                .buttonStyle(.bordered)
                        }
                        .padding(.top, ChefTheme.Spacing.extraSmall)
                    }
                    .padding(.vertical, ChefTheme.Spacing.extraSmall)
                }
            }

            if state.isOwner {
                Section {
                    Button(ComposeStrings["grocery_delete_list"], role: .destructive, action: bloc.onDeleteClicked)
                }
            }
        }
        .navigationTitle(ComposeStrings["grocery_edit_list_title"])
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button(action: bloc.onBackClicked) {
                    Label(ComposeStrings["grocery_cancel"], systemImage: "xmark")
                }
            }
        }
        .tint(ChefTheme.primary)
        .alert(
            ComposeStrings["grocery_delete_list_confirm_title"],
            isPresented: Binding(
                get: { model.value.showDeleteConfirm },
                set: { if !$0 && model.current.showDeleteConfirm { bloc.onDeleteDismissed() } }
            )
        ) {
            Button(ComposeStrings["grocery_delete_list_confirm"], role: .destructive, action: bloc.onDeleteConfirmed)
            Button(ComposeStrings["grocery_cancel"], role: .cancel, action: bloc.onDeleteDismissed)
        } message: {
            Text(ComposeStrings["grocery_delete_list_confirm_message"])
        }
        .alert(
            ComposeStrings["grocery_remove_collaborator_title"],
            isPresented: Binding(
                get: { model.value.collaboratorPendingRemoval != nil },
                set: { if !$0 && model.current.collaboratorPendingRemoval != nil { bloc.onDismissRemoveCollaborator() } }
            )
        ) {
            Button(ComposeStrings["grocery_remove_collaborator"], role: .destructive, action: bloc.onConfirmRemoveCollaborator)
            Button(ComposeStrings["grocery_cancel"], role: .cancel, action: bloc.onDismissRemoveCollaborator)
        } message: {
            Text(ComposeStrings["grocery_remove_collaborator_message"])
        }
    }

    private var isNameValid: Bool {
        !model.current.name.trimmingCharacters(in: .whitespaces).isEmpty
    }

    /// Members grouped by role (pending invites keep their invited role and show dimmed), then the
    /// owner-only invite controls.
    @ViewBuilder
    private func collaborators(_ state: EditGroceryListBlocModel) -> some View {
        let groups: [(String, [ListCollaborator])] = [
            ("grocery_collab_group_owners", state.collaborators.filter { $0.role == .owner }),
            ("grocery_collab_group_editors", state.collaborators.filter { $0.role == .editor }),
            ("grocery_collab_group_viewers", state.collaborators.filter { $0.role == .viewer }),
        ].filter { !$0.1.isEmpty }
        let firstKey = groups.first?.0
        ForEach(groups, id: \.0) { key, members in
            Section {
                ForEach(members, id: \.email) { member in
                    CollaboratorRow(collaborator: member, canManage: state.isOwner) {
                        bloc.onRemoveCollaboratorClicked(collaborator: member)
                    }
                }
            } header: {
                if key == firstKey {
                    VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
                        Text(ComposeStrings["grocery_collaborators"])
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

        if state.isOwner {
            Section {
                TextField(
                    ComposeStrings["grocery_invite_hint"],
                    text: Binding(get: { model.current.inviteEmail }, set: { bloc.onInviteEmailChanged(email: $0) })
                )
                .keyboardType(.emailAddress)
                .textContentType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.send)
                .focused($inviteFocused)
                .onSubmit {
                    bloc.onInviteClicked()
                    inviteFocused = false
                }

                Picker(ComposeStrings["grocery_role_editor"], selection: Binding(
                    get: { model.current.inviteRole },
                    set: { bloc.onInviteRoleChanged(role_: $0) }
                )) {
                    Text(ComposeStrings["grocery_role_editor"]).tag(ListRole.editor)
                    Text(ComposeStrings["grocery_role_viewer"]).tag(ListRole.viewer)
                }
                .pickerStyle(.segmented)
                .labelsHidden()

                Button {
                    bloc.onInviteClicked()
                    inviteFocused = false
                } label: {
                    HStack {
                        Text(ComposeStrings["grocery_invite"])
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

private struct CollaboratorRow: View {
    let collaborator: ListCollaborator
    let canManage: Bool
    let onRemove: () -> Void

    var body: some View {
        let name = collaborator.displayName.flatMap { $0.trimmingCharacters(in: .whitespaces).isEmpty ? nil : $0 }
        let pending = collaborator.status == .pending
        let declined = collaborator.status == .rejected
        let secondary = [
            name != nil ? collaborator.email : nil,
            pending ? ComposeStrings["grocery_collab_pending"] : nil,
            declined ? ComposeStrings["grocery_collab_declined"] : nil,
        ].compactMap { $0 }.joined(separator: " · ")

        HStack(spacing: ChefTheme.Spacing.normal - 4) {
            LetterAvatar(name: name ?? collaborator.email, imageUrl: collaborator.avatarUrl)
                .frame(width: 36, height: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(name ?? collaborator.email)
                if !secondary.isEmpty {
                    Text(secondary)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }
            Spacer(minLength: 0)
            if canManage && collaborator.role != .owner {
                Button(action: onRemove) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel(ComposeStrings["grocery_remove_collaborator"])
            }
        }
        .opacity(pending || declined ? 0.5 : 1)
    }
}

import SwiftUI
import PhotosUI
import ComposeApp

/// The redesigned Manage Profile screen, mirroring the Android `ManageProfileScreen`: avatar with
/// "Change photo", display name, the read-only account email, Save, and Delete Account. Save lives
/// in the toolbar; the BLoC pops the screen once it succeeds. Deleting asks the user to type their
/// email in an alert text field, and the Delete button stays disabled until it matches
/// (`canConfirmDelete`), like the Compose dialog.
///
/// A picked photo is re-encoded as JPEG and handed to the BLoC as a `PickedImage`; it's previewed
/// from the BLoC's `pickedPhoto` bytes and only uploaded on save.
struct ManageProfileView: View {
    let bloc: ManageProfileBloc
    @StateObject private var model: ObservedFlow<ManageProfileBlocModel>
    @State private var photoItem: PhotosPickerItem?

    init(bloc: ManageProfileBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        let isBusy = state.isSaving || state.isDeleting
        Form {
            Section {
                PhotosPicker(selection: $photoItem, matching: .images) {
                    VStack(spacing: ChefTheme.Spacing.small) {
                        ProfileAvatar(pickedPhoto: state.pickedPhoto, photoUrl: state.photoUrl)
                            .frame(width: 96, height: 96)
                            .accessibilityLabel(ComposeStrings["manage_profile_avatar_content_description"])
                        Text(ComposeStrings["manage_profile_change_photo"])
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(ChefTheme.primary)
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.plain)
                .disabled(isBusy)
                .listRowBackground(Color.clear)
            }

            Section {
                TextField(ComposeStrings["manage_profile_display_name_hint"], text: displayNameBinding)
                    .textContentType(.name)
                    .textInputAutocapitalization(.words)
                    .submitLabel(.done)
                    .onSubmit { if model.current.canSave { bloc.onSaveClicked() } }
                    .disabled(isBusy)
            } header: {
                Text(ComposeStrings["manage_profile_display_name_label"])
            } footer: {
                if let error = state.saveError {
                    Text(error.localized).foregroundStyle(.red)
                }
            }

            Section(ComposeStrings["manage_profile_email_label"]) {
                Text(state.email)
                    .foregroundStyle(.secondary)
                    .textSelection(.enabled)
            }

            Section {
                Button(role: .destructive, action: bloc.onDeleteAccountClicked) {
                    HStack {
                        Text(ComposeStrings["manage_profile_delete_account"])
                        if state.isDeleting {
                            Spacer()
                            ProgressView()
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
                .disabled(isBusy)
            } footer: {
                if let error = state.deleteError {
                    Text(error.localized)
                        .foregroundStyle(.red)
                        .frame(maxWidth: .infinity)
                        .multilineTextAlignment(.center)
                }
            }
        }
        .navigationTitle(ComposeStrings["manage_profile_title"])
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                if state.isSaving {
                    ProgressView()
                } else {
                    Button(ComposeStrings["manage_profile_save"], action: bloc.onSaveClicked)
                        .fontWeight(.semibold)
                        .disabled(!state.canSave)
                }
            }
        }
        .tint(ChefTheme.primary)
        .alert(ComposeStrings["manage_profile_delete_dialog_title"], isPresented: deleteDialogBinding) {
            TextField(ComposeStrings["manage_profile_delete_dialog_confirmation_hint"], text: deleteConfirmationBinding)
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
            Button(ComposeStrings["manage_profile_delete_dialog_cancel"], role: .cancel, action: bloc.onDeleteDismissed)
            Button(ComposeStrings["manage_profile_delete_dialog_confirm"], role: .destructive, action: bloc.onDeleteConfirmed)
                .disabled(!model.value.canConfirmDelete)
        } message: {
            Text(
                ComposeStrings["manage_profile_delete_dialog_message"] + "\n\n"
                    + ComposeStrings["manage_profile_delete_dialog_confirmation_prompt", ["email": state.email]]
            )
        }
        .onChange(of: photoItem) {
            guard let item = photoItem else { return }
            photoItem = nil
            Task { await deliverPhoto(item) }
        }
    }

    // MARK: Bindings and actions

    private var displayNameBinding: Binding<String> {
        Binding(
            get: { model.current.displayName },
            set: { bloc.onDisplayNameChanged(displayName: $0) }
        )
    }

    private var deleteConfirmationBinding: Binding<String> {
        Binding(
            get: { model.current.deleteConfirmation },
            set: { bloc.onDeleteConfirmationChanged(confirmation: $0) }
        )
    }

    /// The BLoC owns the dialog; SwiftUI only reports dismissal. Confirm and Cancel already close
    /// it through the BLoC, so only a dismissal it hasn't seen yet is forwarded.
    private var deleteDialogBinding: Binding<Bool> {
        Binding(
            get: { model.current.showDeleteDialog },
            set: { if !$0 && model.current.showDeleteDialog { bloc.onDeleteDismissed() } }
        )
    }

    /// Loads the picked photo as JPEG (HEIC and friends included) and hands it to the BLoC, which
    /// previews it and uploads it on save.
    @MainActor
    private func deliverPhoto(_ item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        guard let jpeg = UIImage(data: data)?.jpegData(compressionQuality: 0.9) else { return }
        bloc.onPhotoPicked(image: PublicPickedImage(bytes: NativeBytes.shared.fromData(data: jpeg), fileExtension: "jpg"))
    }
}

/// The profile photo: the just-picked bytes, else the remote avatar, else a person glyph.
private struct ProfileAvatar: View {
    let pickedPhoto: KotlinByteArray?
    let photoUrl: String?

    var body: some View {
        Group {
            if let pickedPhoto, let image = UIImage(data: NativeBytes.shared.toData(bytes: pickedPhoto)) {
                Image(uiImage: image).resizable().scaledToFill()
            } else if let url = photoUrl.flatMap(URL.init(string:)) {
                AsyncImage(url: url) { $0.resizable().scaledToFill() } placeholder: { placeholder }
            } else {
                placeholder
            }
        }
        .clipShape(Circle())
    }

    private var placeholder: some View {
        ZStack {
            Color(.secondarySystemFill)
            Image(systemName: "person.fill")
                .font(.system(size: 40))
                .foregroundStyle(.secondary)
        }
    }
}

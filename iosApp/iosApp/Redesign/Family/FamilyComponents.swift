import SwiftUI
import ComposeApp

/// Fills `{placeholder}` arguments of a Compose phrase string, e.g.
/// `familyPhrase("family_detail_you", ["name": "Sam"])`.
func familyPhrase(_ key: String, _ args: [String: String]) -> String {
    args.reduce(ComposeStrings[key]) { text, arg in
        text.replacingOccurrences(of: "{\(arg.key)}", with: arg.value)
    }
}

extension FamilyRole {
    /// The localized role label, matching Kotlin's `FamilyRole.label()`.
    var labelText: String {
        if self == .owner { return ComposeStrings["family_role_owner"] }
        if self == .admin { return ComposeStrings["family_role_admin"] }
        return ComposeStrings["family_role_member"]
    }
}

/// A member's photo, or the first letter of their name on the brand color.
struct FamilyAvatar: View {
    let name: String
    let imageUrl: String?

    var body: some View {
        AsyncImage(url: imageUrl.flatMap(URL.init(string:))) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            ZStack {
                ChefTheme.primary
                Text(name.first.map { String($0).uppercased() } ?? "")
                    .font(.headline)
                    .foregroundStyle(.white)
            }
        }
        .frame(width: 40, height: 40)
        .clipShape(Circle())
        .accessibilityHidden(true)
    }
}

/// A sheet with a single name field, shared by "New family", "Rename family" and the family grocery
/// list / recipe book dialogs (Android's `FamilyNameDialog`). The name lives in the BLoC.
struct FamilyNameSheet: View {
    let title: String
    let confirmText: String
    let label: String
    let name: String
    let canConfirm: Bool
    let isSaving: Bool
    let error: TextData?
    let onNameChange: (String) -> Void
    let onConfirm: () -> Void
    let onDismiss: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(label, text: Binding(get: { name }, set: onNameChange))
                        .textInputAutocapitalization(.words)
                        .submitLabel(.done)
                        .focused($focused)
                        .onSubmit { if canConfirm { onConfirm() } }
                } footer: {
                    if let error {
                        LocalizedText(text: error).foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(ComposeStrings["family_cancel"], action: onDismiss)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button(confirmText, action: onConfirm).disabled(!canConfirm)
                    }
                }
            }
        }
        .tint(ChefTheme.primary)
        .presentationDetents([.medium])
        .interactiveDismissDisabled(isSaving)
        .onAppear { focused = true }
    }
}

/// Centered load error with a retry button (Android's `LoadError`).
struct FamilyLoadError: View {
    let message: TextData
    let onRetry: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(message.localized, systemImage: "wifi.exclamationmark")
        } actions: {
            Button(ComposeStrings["family_retry"], action: onRetry)
                .buttonStyle(.borderedProminent)
                .tint(ChefTheme.primary)
        }
    }
}

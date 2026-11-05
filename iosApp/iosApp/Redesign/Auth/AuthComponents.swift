import SwiftUI
import ComposeApp

/// A labeled text field with an inline error, the native counterpart of `PlusAutofillTextField`.
/// `kind` sets the keyboard and the autofill hint; password fields get a show/hide toggle.
struct AuthTextField: View {
    enum Kind { case email, password, oneTimeCode }

    let label: String
    @Binding var text: String
    var error: TextData?
    let kind: Kind
    @State private var isRevealed = false

    var body: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(error == nil ? Color.secondary : Color.red)
            HStack(spacing: ChefTheme.Spacing.small) {
                field
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .frame(maxWidth: .infinity, minHeight: ChefTheme.Spacing.rowHeight - ChefTheme.Spacing.small)
                if kind == .password {
                    Button {
                        isRevealed.toggle()
                    } label: {
                        Image(systemName: isRevealed ? "eye" : "eye.slash")
                            .foregroundStyle(.secondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(ComposeStrings["auth_password_toggle"])
                }
            }
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay {
                if error != nil {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .strokeBorder(Color.red, lineWidth: 1)
                }
            }
            if let error {
                LocalizedText(text: error)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }

    @ViewBuilder
    private var field: some View {
        switch kind {
        case .email:
            TextField("", text: $text)
                .keyboardType(.emailAddress)
                .textContentType(.username)
                .accessibilityLabel(label)
        case .password:
            Group {
                if isRevealed {
                    TextField("", text: $text)
                } else {
                    SecureField("", text: $text)
                }
            }
            .textContentType(.password)
            .accessibilityLabel(label)
        case .oneTimeCode:
            TextField("", text: $text)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .accessibilityLabel(label)
        }
    }
}

/// The floating "✓ Sign In" / "✓ Verify" button, the native counterpart of the Android extended
/// FAB. Sits in the bottom safe-area inset so it rides above the keyboard.
struct SubmitButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        HStack {
            Spacer()
            Button(action: action) {
                Label(title, systemImage: "checkmark")
                    .font(.headline)
                    .padding(.horizontal, ChefTheme.Spacing.large)
                    .padding(.vertical, ChefTheme.Spacing.normal - 2)
            }
            .buttonStyle(.plain)
            .foregroundStyle(ChefTheme.primary)
            .chefGlass()
        }
        .padding(.horizontal, ChefTheme.Spacing.normal)
        .padding(.bottom, ChefTheme.Spacing.small)
    }
}

/// A blocking progress card, the native counterpart of `PlusLoadingDialog`.
struct AuthLoadingOverlay: View {
    let message: String

    var body: some View {
        ZStack {
            Color.black.opacity(0.2).ignoresSafeArea()
            HStack(spacing: ChefTheme.Spacing.normal) {
                ProgressView()
                Text(message)
                    .font(.body)
            }
            .padding(ChefTheme.Spacing.large)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
        .accessibilityElement(children: .combine)
    }
}

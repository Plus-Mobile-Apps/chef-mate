import SwiftUI
import ComposeApp

/// The redesigned sign-in / create-account screen, mirroring the Android `AuthenticationScreen`:
/// email and password (plus confirm password when signing up), the forgot-password and
/// email-me-a-code links, the mode switcher, legal links, and a floating submit button. Fields
/// carry iOS autofill hints so iCloud Keychain and third-party password managers can fill them.
struct AuthenticationView: View {
    let bloc: AuthenticationBloc
    @StateObject private var model: ObservedFlow<AuthenticationBlocModel>
    @StateObject private var email: ObservedFlow<NSString>
    @StateObject private var password: ObservedFlow<NSString>
    @StateObject private var confirmPassword: ObservedFlow<NSString>
    @FocusState private var focus: Field?
    /// Privacy policy / terms of use, shown in a Safari sheet instead of the in-app browser.
    @State private var webPage: WebPage?

    private enum Field { case email, password, confirmPassword }

    init(bloc: AuthenticationBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.models))
        _email = StateObject(wrappedValue: ObservedFlow(bloc.email))
        _password = StateObject(wrappedValue: ObservedFlow(bloc.password))
        _confirmPassword = StateObject(wrappedValue: ObservedFlow(bloc.confirmPassword))
    }

    var body: some View {
        let state = model.value
        let isSignIn = state.mode == .signin
        ScrollView {
            VStack(spacing: 0) {
                AuthTextField(
                    label: ComposeStrings["auth_label_email"],
                    text: Binding(get: { email.current as String }, set: { bloc.onEmailChanged(email: $0) }),
                    error: state.emailError,
                    kind: .email
                )
                .focused($focus, equals: .email)
                .submitLabel(.next)
                .onSubmit { focus = .password }

                Spacer().frame(height: ChefTheme.Spacing.large)

                // Both modes hint a plain login password (not "new password") so third-party
                // managers offer the existing credential — see the Compose screen for the why.
                AuthTextField(
                    label: ComposeStrings["auth_label_password"],
                    text: Binding(get: { password.current as String }, set: { bloc.onPasswordChanged(password: $0) }),
                    error: state.passwordError,
                    kind: .password
                )
                .focused($focus, equals: .password)
                .submitLabel(isSignIn ? .go : .next)
                .onSubmit {
                    if isSignIn { submit() } else { focus = .confirmPassword }
                }

                Spacer().frame(height: ChefTheme.Spacing.normal)

                if isSignIn {
                    VStack(spacing: ChefTheme.Spacing.extraSmall) {
                        Button(ComposeStrings["auth_button_forgot_password"], action: bloc.onForgotPasswordClicked)
                        Button(ComposeStrings["auth_button_email_me_a_code"], action: bloc.onEmailMeACodeClicked)
                    }
                    .padding(.vertical, ChefTheme.Spacing.small)
                    .transition(.opacity.combined(with: .move(edge: .top)))
                } else {
                    AuthTextField(
                        label: ComposeStrings["auth_label_confirm_password"],
                        text: Binding(
                            get: { confirmPassword.current as String },
                            set: { bloc.onConfirmPasswordChanged(confirmPassword: $0) }
                        ),
                        error: state.confirmPasswordError,
                        kind: .password
                    )
                    .focused($focus, equals: .confirmPassword)
                    .submitLabel(.go)
                    .onSubmit(submit)
                    .transition(.opacity.combined(with: .move(edge: .top)))
                }

                Spacer().frame(height: ChefTheme.Spacing.large)

                ModeSwitcher(isSignIn: isSignIn) {
                    focus = nil
                    bloc.onToggleMode()
                }

                Spacer().frame(height: ChefTheme.Spacing.large)

                HStack(spacing: ChefTheme.Spacing.small) {
                    Button(ComposeStrings["auth_privacy_policy"]) {
                        open("https://chefmate.plusmobileapps.com/privacy-policy/")
                    }
                    Button(ComposeStrings["auth_terms_of_use"]) {
                        open("https://chefmate.plusmobileapps.com/terms-of-use/")
                    }
                }
                .font(.footnote)
            }
            .padding(.top, ChefTheme.Spacing.extraLarge)
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .animation(.default, value: isSignIn)
        }
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle(ComposeStrings[isSignIn ? "auth_screen_title_sign_in" : "auth_screen_title_sign_up"])
        .navigationBarTitleDisplayMode(.large)
        .safeAreaInset(edge: .bottom) {
            SubmitButton(
                title: ComposeStrings[isSignIn ? "auth_button_sign_in" : "auth_button_sign_up"],
                action: submit
            )
        }
        .tint(ChefTheme.primary)
        .overlay {
            if state.isLoading {
                AuthLoadingOverlay(
                    message: ComposeStrings[isSignIn ? "auth_loading_signing_in" : "auth_loading_creating_account"]
                )
            }
        }
        .alert(
            state.errorMessage?.localized ?? "",
            isPresented: Binding(
                get: { model.current.errorMessage != nil },
                set: { if !$0 { bloc.onDismissError() } }
            )
        ) {
            Button(ComposeStrings["auth_button_okay"], action: bloc.onDismissError)
        }
        .alert(
            ComposeStrings["auth_discard_guest_title"],
            isPresented: Binding(
                get: { model.current.pendingGuestDataDiscard != nil },
                set: { if !$0 { bloc.onDiscardGuestDataCancelled() } }
            )
        ) {
            Button(ComposeStrings["auth_discard_guest_confirm"], role: .destructive, action: bloc.onDiscardGuestDataConfirmed)
            Button(ComposeStrings["auth_discard_guest_cancel"], role: .cancel, action: bloc.onDiscardGuestDataCancelled)
        } message: {
            Text(discardGuestMessage(count: Int(state.pendingGuestDataDiscard?.guestRecipeCount ?? 0)))
        }
        .safariSheet(item: $webPage)
    }

    private func submit() {
        focus = nil
        bloc.onSubmitClicked()
    }

    private func open(_ link: String) {
        if let url = URL(string: link) { webPage = WebPage(url: url) }
    }

    private func discardGuestMessage(count: Int) -> String {
        count == 1
            ? ComposeStrings["auth_discard_guest_message_one"]
            : ComposeStrings["auth_discard_guest_message_other"].replacingOccurrences(of: "{count}", with: String(count))
    }
}

/// "Don't have an account? / Create Account" under a divider, or the reverse when signing up.
private struct ModeSwitcher: View {
    let isSignIn: Bool
    let onToggle: () -> Void

    var body: some View {
        VStack(spacing: ChefTheme.Spacing.normal) {
            Divider()
            Text(ComposeStrings[isSignIn ? "auth_switch_to_sign_up" : "auth_switch_to_sign_in"])
                .font(.headline)
            Button(ComposeStrings[isSignIn ? "auth_button_sign_up" : "auth_button_sign_in"], action: onToggle)
        }
        .frame(maxWidth: .infinity)
    }
}

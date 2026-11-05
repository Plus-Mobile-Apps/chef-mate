import SwiftUI
import ComposeApp

/// The redesigned one-time-code screen, mirroring the Android `OtpScreen`: instructions naming the
/// email, the code field (with iOS one-time-code autofill from Mail/Messages), an info line, the
/// resend button with its countdown, and a floating Verify button.
struct OtpView: View {
    let bloc: OtpBloc
    @StateObject private var model: ObservedFlow<OtpBlocModel>
    @StateObject private var code: ObservedFlow<NSString>
    @FocusState private var isCodeFocused: Bool

    init(bloc: OtpBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.models))
        _code = StateObject(wrappedValue: ObservedFlow(bloc.code))
    }

    var body: some View {
        let state = model.value
        let countdown = Int(state.resendCountdownSeconds)
        ScrollView {
            VStack(spacing: 0) {
                Text(ComposeStrings["auth_otp_instructions"].replacingOccurrences(of: "{email}", with: state.email))
                    .font(.body)
                    .multilineTextAlignment(.center)

                Spacer().frame(height: ChefTheme.Spacing.large)

                AuthTextField(
                    label: ComposeStrings["auth_otp_label_code"],
                    text: Binding(get: { code.current as String }, set: { bloc.onCodeChanged(code: $0) }),
                    kind: .oneTimeCode
                )
                .focused($isCodeFocused)

                Spacer().frame(height: ChefTheme.Spacing.normal)

                if let info = state.infoMessage {
                    LocalizedText(text: info)
                        .font(.callout)
                        .foregroundStyle(ChefTheme.primary)
                        .multilineTextAlignment(.center)
                    Spacer().frame(height: ChefTheme.Spacing.normal)
                }

                Button(
                    countdown > 0
                        ? ComposeStrings["auth_otp_button_resend_countdown"].replacingOccurrences(of: "{seconds}", with: String(countdown))
                        : ComposeStrings["auth_otp_button_resend"],
                    action: bloc.onResendClicked
                )
                .disabled(countdown > 0 || state.isLoading)
                .monospacedDigit()
            }
            .padding(.top, ChefTheme.Spacing.extraLarge)
            .padding(.horizontal, ChefTheme.Spacing.normal)
        }
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle(ComposeStrings[titleKey(state.flow)])
        .navigationBarTitleDisplayMode(.large)
        .safeAreaInset(edge: .bottom) {
            SubmitButton(title: ComposeStrings["auth_otp_button_verify"]) {
                isCodeFocused = false
                bloc.onVerifyClicked()
            }
        }
        .tint(ChefTheme.primary)
        .overlay {
            if state.isLoading {
                AuthLoadingOverlay(message: ComposeStrings["auth_otp_loading_verifying"])
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
        .onAppear { isCodeFocused = true }
    }

    private func titleKey(_ flow: OtpFlow) -> String {
        switch flow {
        case .passwordlesssignin: "auth_otp_screen_title_passwordless"
        case .emailchange: "auth_otp_screen_title_email_change"
        default: "auth_otp_screen_title_signup"
        }
    }
}

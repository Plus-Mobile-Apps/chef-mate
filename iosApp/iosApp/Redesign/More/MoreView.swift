import SwiftUI
import ComposeApp

/// The redesigned More tab, mirroring the Android `SettingsScreen`: profile header or auth
/// prompts, navigation rows, Sign Out, and the version label. Developer settings (debug builds)
/// stays reachable here so the redesign flag can always be switched back off.
struct MoreView: View {
    let bloc: SettingsBloc
    @StateObject private var model: ObservedFlow<SettingsBlocModel>
    @SwiftUI.Environment(\.openURL) private var openURL

    init(bloc: SettingsBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        let hasProfile = state.isAuthenticated && !state.isAnonymous
        List {
            Section {
                if hasProfile {
                    ProfileHeader(state: state, onTap: bloc.onManageProfileClicked)
                    MoreRow("Manage Profile", systemImage: "person.crop.circle", action: bloc.onManageProfileClicked)
                } else {
                    if state.isAnonymous {
                        Text("You’re using ChefMate as a guest. Sign up to back up your recipes across devices.")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    } else if let message = state.verificationMessage {
                        LocalizedText(text: message)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    AuthButtons(onSignIn: bloc.onSignInClicked, onSignUp: bloc.onSignUpClicked)
                }
            }

            Section {
                if hasProfile {
                    MoreRow("Notifications", systemImage: "bell", action: bloc.onNotificationsClicked)
                }
                if state.isManageFamilyEnabled {
                    MoreRow("Manage Family", systemImage: "person.2", action: bloc.onManageFamilyClicked)
                }
                MoreRow("Settings", systemImage: "gearshape", action: bloc.onAppSettingsClicked)
                MoreRow("View Onboarding Again", systemImage: "arrow.counterclockwise", action: bloc.onReplayOnboardingClicked)
                if state.isAiChatEnabled {
                    MoreRow("AI Chat", systemImage: "sparkles", action: bloc.onAiChatClicked) {
                        if !state.isSubscribed {
                            Image(systemName: "lock.fill")
                                .foregroundStyle(ChefTheme.primary)
                        }
                    }
                }
            }

            Section {
                MoreRow("Privacy Policy", systemImage: "hand.raised") {
                    bloc.onUrlClicked(url: "https://chefmate.plusmobileapps.com/privacy-policy/")
                }
                MoreRow("Terms of Use", systemImage: "doc.text") {
                    bloc.onUrlClicked(url: "https://chefmate.plusmobileapps.com/terms-of-use/")
                }
                MoreRow("About", systemImage: "info.circle") {
                    bloc.onUrlClicked(url: "https://chefmate.plusmobileapps.com/about/")
                }
                MoreRow("Contact Us", systemImage: "envelope") {
                    if let url = URL(string: "mailto:support@plusmobileapps.com") { openURL(url) }
                }
                if state.isDebugBuild {
                    MoreRow("Developer Settings", systemImage: "chevron.left.forwardslash.chevron.right", action: bloc.onDeveloperSettingsClicked)
                }
            }

            if hasProfile {
                Section {
                    Button("Sign Out", role: .destructive, action: bloc.onSignOutClicked)
                        .frame(maxWidth: .infinity)
                }
            }

            Section {
                Text("Version \(state.versionName)")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("More")
        .confirmationDialog(
            "Sign Out",
            isPresented: Binding(
                get: { model.current.showSignOutConfirmationDialog },
                set: { if !$0 { bloc.onSignOutDismissed() } }
            ),
            titleVisibility: .visible
        ) {
            Button("Sign Out", role: .destructive, action: bloc.onSignOutConfirmed)
            Button("Cancel", role: .cancel, action: bloc.onSignOutDismissed)
        } message: {
            Text("Are you sure you want to sign out?")
        }
        .alert(
            "ChefMate Premium",
            isPresented: Binding(
                get: { model.current.showPremiumRequiredDialog },
                set: { if !$0 { bloc.onPremiumRequiredDismissed() } }
            )
        ) {
            Button("OK", action: bloc.onPremiumRequiredDismissed)
        } message: {
            Text("AI Chat is part of ChefMate Premium.")
        }
    }
}

private struct ProfileHeader: View {
    let state: SettingsBlocModel
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: ChefTheme.Spacing.small) {
                Avatar(name: state.profileName ?? "", imageUrl: state.profileImageUrl)
                    .frame(width: 96, height: 96)
                if let greeting = state.greeting {
                    LocalizedText(text: greeting)
                        .font(.title2.weight(.semibold))
                        .foregroundStyle(.primary)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, ChefTheme.Spacing.normal)
        }
        .buttonStyle(.plain)
        .listRowBackground(Color.clear)
    }
}

private struct Avatar: View {
    let name: String
    let imageUrl: String?

    var body: some View {
        AsyncImage(url: imageUrl.flatMap(URL.init(string:))) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            ZStack {
                ChefTheme.primary
                Text(name.first.map { String($0).uppercased() } ?? "")
                    .font(.largeTitle.weight(.semibold))
                    .foregroundStyle(.white)
            }
        }
        .clipShape(Circle())
    }
}

private struct AuthButtons: View {
    let onSignIn: () -> Void
    let onSignUp: () -> Void

    var body: some View {
        HStack(spacing: ChefTheme.Spacing.normal) {
            Button("Sign In", action: onSignIn)
                .buttonStyle(.bordered)
            Button("Sign Up", action: onSignUp)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
        .controlSize(.large)
        .tint(ChefTheme.primary)
    }
}

/// A tappable settings row: leading icon, title, optional trailing accessory, and a chevron.
private struct MoreRow<Trailing: View>: View {
    let title: String
    let systemImage: String
    let action: () -> Void
    let trailing: Trailing

    init(
        _ title: String,
        systemImage: String,
        action: @escaping () -> Void,
        @ViewBuilder trailing: () -> Trailing = { EmptyView() }
    ) {
        self.title = title
        self.systemImage = systemImage
        self.action = action
        self.trailing = trailing()
    }

    var body: some View {
        Button(action: action) {
            HStack {
                // Explicit colors: inside a List, a Button would otherwise tint the whole label.
                Label {
                    Text(title).foregroundStyle(Color.primary)
                } icon: {
                    Image(systemName: systemImage).foregroundStyle(ChefTheme.primary)
                }
                Spacer()
                trailing
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
            }
        }
    }
}

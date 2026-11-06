import SwiftUI
import ComposeApp

/// The redesigned More tab, mirroring the Android `SettingsScreen`: profile header or auth
/// prompts, navigation rows, Sign Out, and the version label. There is no title: once the large
/// profile header scrolls away, a compact avatar and the greeting take its place in the bar.
/// Developer settings (debug builds) stays reachable here so the redesign flag can always be
/// switched back off.
struct MoreView: View {
    let bloc: SettingsBloc
    @StateObject private var model: ObservedFlow<SettingsBlocModel>
    @SwiftUI.Environment(\.openURL) private var openURL
    @State private var headerScrolledAway = false

    init(bloc: SettingsBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        let hasProfile = state.isAuthenticated && !state.isAnonymous
        List {
            if hasProfile {
                // Its own section, so the Manage profile card below keeps all four corners rounded.
                Section {
                    ProfileHeader(state: state, onTap: bloc.onManageProfileClicked)
                }
                .listSectionSpacing(.compact)
            }

            Section {
                if hasProfile {
                    MoreRow(ComposeStrings["manage_profile"], systemImage: "person.crop.circle", action: bloc.onManageProfileClicked)
                } else {
                    if state.isAnonymous {
                        Text(ComposeStrings["settings_guest_banner"])
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
                    MoreRow(ComposeStrings["settings_notifications"], systemImage: "bell", action: bloc.onNotificationsClicked)
                }
                if state.isManageFamilyEnabled {
                    MoreRow(ComposeStrings["settings_manage_family"], systemImage: "person.2", action: bloc.onManageFamilyClicked)
                }
                MoreRow(ComposeStrings["settings"], systemImage: "gearshape", action: bloc.onAppSettingsClicked)
                MoreRow(ComposeStrings["settings_replay_onboarding"], systemImage: "arrow.counterclockwise", action: bloc.onReplayOnboardingClicked)
                if state.isAiChatEnabled {
                    MoreRow(ComposeStrings["settings_ai_chat"], systemImage: "sparkles", action: bloc.onAiChatClicked) {
                        if !state.isSubscribed {
                            Image(systemName: "lock.fill")
                                .foregroundStyle(ChefTheme.primary)
                        }
                    }
                }
            }

            Section {
                MoreRow(ComposeStrings["privacy_policy"], systemImage: "hand.raised") {
                    bloc.onUrlClicked(url: "https://chefmate.plusmobileapps.com/privacy-policy/")
                }
                MoreRow(ComposeStrings["terms_of_use"], systemImage: "doc.text") {
                    bloc.onUrlClicked(url: "https://chefmate.plusmobileapps.com/terms-of-use/")
                }
                MoreRow(ComposeStrings["about"], systemImage: "info.circle") {
                    bloc.onUrlClicked(url: "https://chefmate.plusmobileapps.com/about/")
                }
                MoreRow(ComposeStrings["contact_us"], systemImage: "envelope") {
                    if let url = URL(string: "mailto:support@plusmobileapps.com") { openURL(url) }
                }
                if state.isDebugBuild {
                    MoreRow(ComposeStrings["developer_settings"], systemImage: "chevron.left.forwardslash.chevron.right", action: bloc.onDeveloperSettingsClicked)
                }
            }

            if hasProfile {
                Section {
                    Button(ComposeStrings["sign_out"], role: .destructive, action: bloc.onSignOutClicked)
                        .frame(maxWidth: .infinity)
                }
            }

            Section {
                Text(ComposeStrings["version_label"].replacingOccurrences(of: "{version}", with: state.versionName))
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
        .onScrollGeometryChange(for: Bool.self) { geometry in
            geometry.contentOffset.y + geometry.contentInsets.top > ProfileHeader.collapseOffset
        } action: { _, scrolledAway in
            withAnimation(.snappy) { headerScrolledAway = scrolledAway }
        }
        .toolbar {
            if hasProfile && headerScrolledAway {
                // The title slot: full width and no glass capsule, unlike a leading item.
                ToolbarItem(placement: .principal) {
                    CompactProfile(state: state, onTap: bloc.onManageProfileClicked)
                }
            }
        }
        .confirmationDialog(
            ComposeStrings["sign_out_confirmation_title"],
            isPresented: Binding(
                get: { model.current.showSignOutConfirmationDialog },
                set: { if !$0 { bloc.onSignOutDismissed() } }
            ),
            titleVisibility: .visible
        ) {
            Button(ComposeStrings["sign_out_confirmation_confirm"], role: .destructive, action: bloc.onSignOutConfirmed)
            Button(ComposeStrings["sign_out_confirmation_cancel"], role: .cancel, action: bloc.onSignOutDismissed)
        } message: {
            Text(ComposeStrings["sign_out_confirmation_message"])
        }
        .alert(
            ComposeStrings["premium_required_title"],
            isPresented: Binding(
                get: { model.current.showPremiumRequiredDialog },
                set: { if !$0 { bloc.onPremiumRequiredDismissed() } }
            )
        ) {
            Button(ComposeStrings["premium_required_confirm"], action: bloc.onPremiumRequiredDismissed)
        } message: {
            Text(ComposeStrings["premium_required_message"])
        }
    }
}

private struct ProfileHeader: View {
    let state: SettingsBlocModel
    let onTap: () -> Void

    /// How far the list scrolls before the header is mostly hidden under the bar and the compact
    /// profile replaces it.
    static let collapseOffset: CGFloat = 150

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: ChefTheme.Spacing.small) {
                Avatar(name: state.profileName ?? "", imageUrl: state.profileImageUrl)
                    .frame(width: 128, height: 128)
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

/// The profile photo and greeting shown in the navigation bar once the header scrolls away.
private struct CompactProfile: View {
    let state: SettingsBlocModel
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: ChefTheme.Spacing.small) {
                Avatar(name: state.profileName ?? "", imageUrl: state.profileImageUrl, initialFont: .subheadline.weight(.semibold))
                    .frame(width: 32, height: 32)
                if let greeting = state.greeting {
                    LocalizedText(text: greeting)
                        .font(.headline)
                        .foregroundStyle(.primary)
                        .lineLimit(1)
                }
            }
        }
        .buttonStyle(.plain)
        .transition(.opacity.combined(with: .move(edge: .bottom)))
    }
}

private struct Avatar: View {
    let name: String
    let imageUrl: String?
    var initialFont: Font = .largeTitle.weight(.semibold)

    var body: some View {
        AsyncImage(url: imageUrl.flatMap(URL.init(string:))) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            ZStack {
                ChefTheme.primary
                Text(name.first.map { String($0).uppercased() } ?? "")
                    .font(initialFont)
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
            Button(ComposeStrings["sign_in"], action: onSignIn)
                .buttonStyle(.bordered)
            Button(ComposeStrings["sign_up"], action: onSignUp)
                .buttonStyle(.borderedProminent)
        }
        .frame(maxWidth: .infinity)
        .controlSize(.large)
        .tint(ChefTheme.primary)
    }
}

/// A tappable settings row: leading icon, title, optional trailing accessory, and a chevron.
struct MoreRow<Trailing: View>: View {
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

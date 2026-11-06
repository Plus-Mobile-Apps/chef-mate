import SwiftUI
import ComposeApp

/// The redesigned Developer Settings (More → Developer settings, debug builds), mirroring the
/// Compose `DeveloperSettingsScreen`: environment and test-user pickers, sign out, the Feature
/// flags drill-in (how the `ios_swiftui_redesign` flag is turned back off), the fake Premium
/// switch, and coach-mark reset. The pickers are model-driven sheets with a checkmark on the
/// current choice; the restart, sign-in error and coach-mark notices are alerts.
struct DeveloperSettingsView: View {
    let bloc: DeveloperSettingsBloc
    @StateObject private var model: ObservedFlow<DeveloperSettingsBlocModel>

    init(bloc: DeveloperSettingsBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        List {
            Section {
                DevRow(ComposeStrings["dev_environment"], systemImage: "server.rack", action: bloc.onEnvironmentClicked) {
                    Text(state.currentEnvironment.label)
                }
                DevRow(ComposeStrings["dev_login_as_user"], systemImage: "person.badge.key", action: bloc.onLoginAsUserClicked) {
                    Text(state.currentUserEmail ?? ComposeStrings["dev_signed_out"])
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
                if state.isAuthenticated {
                    Button(role: .destructive, action: bloc.onSignOutTestUserClicked) {
                        Label(ComposeStrings["dev_sign_out_test_user"], systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
            }

            Section {
                DevRow(ComposeStrings["dev_feature_flags"], systemImage: "flag", action: bloc.onFeatureFlagsClicked)
                Toggle(isOn: Binding(
                    get: { model.current.isSubscribed },
                    set: { bloc.onSubscribedToggled(subscribed: $0) }
                )) {
                    Label {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(ComposeStrings["dev_premium_subscription"])
                            Text(ComposeStrings["dev_premium_subscription_subtitle"])
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    } icon: {
                        Image(systemName: "crown")
                    }
                }
                DevRow(ComposeStrings["dev_reset_coach_marks"], systemImage: "lightbulb", action: bloc.onClearCoachMarksClicked)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(ComposeStrings["developer_settings"])
        .navigationBarTitleDisplayMode(.inline)
        .tint(ChefTheme.primary)
        .sheet(isPresented: Binding(
            get: { model.value.showEnvironmentPicker },
            set: { shown in
                if !shown && model.current.showEnvironmentPicker { bloc.onEnvironmentPickerDismissed() }
            }
        )) {
            EnvironmentPickerSheet(
                current: model.current.currentEnvironment,
                onSelected: { bloc.onEnvironmentSelected(environment: $0) },
                onDismiss: bloc.onEnvironmentPickerDismissed
            )
        }
        .sheet(isPresented: Binding(
            get: { model.value.showUserPicker },
            set: { shown in
                if !shown && model.current.showUserPicker { bloc.onUserPickerDismissed() }
            }
        )) {
            UserPickerSheet(
                users: model.current.availableUsers,
                selectedIndex: model.current.selectedUserIndex?.int32Value,
                onSelected: { bloc.onUserSelected(user: $0) },
                onDismiss: bloc.onUserPickerDismissed
            )
        }
        .alert(
            ComposeStrings["dev_restart_required_title"],
            isPresented: Binding(
                get: { model.value.showRestartPrompt },
                set: { shown in
                    if !shown && model.current.showRestartPrompt { bloc.onRestartPromptDismissed() }
                }
            )
        ) {
            Button(ComposeStrings["okay"], action: bloc.onRestartPromptDismissed)
        } message: {
            Text(ComposeStrings["dev_restart_required_message"])
        }
        .alert(
            ComposeStrings["dev_sign_in_error_title"],
            isPresented: Binding(
                get: { model.value.signInError != nil },
                set: { shown in
                    if !shown && model.current.signInError != nil { bloc.onSignInErrorDismissed() }
                }
            )
        ) {
            Button(ComposeStrings["okay"], action: bloc.onSignInErrorDismissed)
        } message: {
            if let raw = state.signInError {
                let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
                Text(trimmed.isEmpty ? ComposeStrings["dev_sign_in_error_unknown"] : raw)
            }
        }
        .alert(
            ComposeStrings["dev_coach_marks_reset_title"],
            isPresented: Binding(
                get: { model.value.showCoachMarksResetConfirmation },
                set: { shown in
                    if !shown && model.current.showCoachMarksResetConfirmation {
                        bloc.onCoachMarksResetConfirmationDismissed()
                    }
                }
            )
        ) {
            Button(ComposeStrings["okay"], action: bloc.onCoachMarksResetConfirmationDismissed)
        } message: {
            Text(ComposeStrings["dev_coach_marks_reset_message"])
        }
    }
}

/// The environment radio list as a sheet: tap a row to switch (the BLoC closes it), Cancel to
/// dismiss.
private struct EnvironmentPickerSheet: View {
    let current: ComposeApp.Environment
    let onSelected: (ComposeApp.Environment) -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            List {
                ForEach(ComposeApp.Environment.entries, id: \.ordinal) { env in
                    CheckRow(title: env.label, isSelected: env == current) { onSelected(env) }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(ComposeStrings["dev_environment"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(ComposeStrings["dev_picker_dismiss"], action: onDismiss)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .tint(ChefTheme.primary)
    }
}

/// The test-user radio list as a sheet, or the setup instructions when none are configured.
private struct UserPickerSheet: View {
    let users: [TestUser]
    let selectedIndex: Int32?
    let onSelected: (TestUser) -> Void
    let onDismiss: () -> Void

    var body: some View {
        NavigationStack {
            List {
                if users.isEmpty {
                    VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
                        Text(ComposeStrings["dev_no_test_users"])
                            .font(.headline)
                        Text(ComposeStrings["dev_no_test_users_message"])
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .textSelection(.enabled)
                    }
                    .padding(.vertical, ChefTheme.Spacing.extraSmall)
                } else {
                    ForEach(users, id: \.index) { user in
                        CheckRow(
                            title: ComposeStrings["dev_user_label_format", ["index": "\(user.index)", "email": user.email]],
                            isSelected: user.index == selectedIndex
                        ) { onSelected(user) }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle(ComposeStrings["dev_login_as_user"])
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(ComposeStrings["dev_picker_dismiss"], action: onDismiss)
                }
            }
        }
        .presentationDetents([.medium, .large])
        .tint(ChefTheme.primary)
    }
}

/// A single-choice row: title with a trailing checkmark when selected.
private struct CheckRow: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack {
                Text(title).foregroundStyle(Color.primary)
                Spacer(minLength: 0)
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(ChefTheme.primary)
                }
            }
            .contentShape(Rectangle())
        }
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// A tappable drill-in row: leading icon, title, optional trailing value, and a chevron.
private struct DevRow<Trailing: View>: View {
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
                    Image(systemName: systemImage)
                }
                Spacer(minLength: ChefTheme.Spacing.small)
                trailing.foregroundStyle(Color.secondary)
                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(Color(.tertiaryLabel))
            }
            .contentShape(Rectangle())
        }
    }
}

private extension ComposeApp.Environment {
    var label: String {
        switch self {
        case .prod: ComposeStrings["dev_env_prod"]
        case .testing: ComposeStrings["dev_env_testing"]
        case .fake: ComposeStrings["dev_env_fake"]
        default: name
        }
    }
}

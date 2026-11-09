import SwiftUI
import ComposeApp

/// The redesigned Notifications screen, mirroring the Android `NotificationsScreen`: pending
/// invites (grocery list, recipe book, family), each with Decline / Accept, plus the empty state
/// and a sign-in prompt for signed-out or anonymous users. Rows also offer Accept and Decline as
/// swipe actions. While an invite's accept/decline is in flight (`processing`) its actions are
/// replaced by a spinner; failures are reported by the BLoC as toasts.
///
/// The `AppNotification` subclasses aren't visible to Swift, so each row's text comes from the
/// shared Kotlin `notificationMessage(notification:)`.
struct NotificationsView: View {
    let bloc: NotificationsBloc
    @StateObject private var model: ObservedFlow<NotificationsBlocModel>

    init(bloc: NotificationsBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        content(state)
            .navigationTitle(ComposeStrings["notifications_title"])
            .tint(ChefTheme.primary)
    }

    @ViewBuilder
    private func content(_ state: NotificationsBlocModel) -> some View {
        if state.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if !state.isSignedIn {
            StatusBlock(
                systemImage: "person.crop.circle",
                title: ComposeStrings["notifications_signed_out_title"],
                message: ComposeStrings["notifications_signed_out"]
            ) {
                HStack(spacing: ChefTheme.Spacing.normal) {
                    Button(ComposeStrings["sign_in"], action: bloc.onSignInClicked)
                        .buttonStyle(.bordered)
                    Button(ComposeStrings["sign_up"], action: bloc.onSignUpClicked)
                        .buttonStyle(.borderedProminent)
                }
                .controlSize(.large)
            }
        } else if state.notifications.isEmpty {
            StatusBlock(
                systemImage: "bell",
                title: ComposeStrings["notifications_empty_title"],
                message: ComposeStrings["notifications_empty_message"]
            )
        } else {
            List {
                ForEach(state.notifications, id: \.key) { notification in
                    NotificationRow(
                        message: NotificationMessageKt.notificationMessage(notification: notification).localized,
                        isProcessing: state.processing.contains(notification.key),
                        onAccept: { bloc.onAccept(notification: notification) },
                        onDecline: { bloc.onDecline(notification: notification) }
                    )
                }
            }
            .listStyle(.insetGrouped)
        }
    }
}

/// One invite: its message, then Decline / Accept (or a spinner while either is in flight).
private struct NotificationRow: View {
    let message: String
    let isProcessing: Bool
    let onAccept: () -> Void
    let onDecline: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.normal) {
            Text(message)
                .font(.body)
            HStack(spacing: ChefTheme.Spacing.small) {
                Spacer()
                if isProcessing {
                    ProgressView()
                } else {
                    Button(ComposeStrings["notifications_decline"], action: onDecline)
                        .buttonStyle(.bordered)
                    Button(ComposeStrings["notifications_accept"], action: onAccept)
                        .buttonStyle(.borderedProminent)
                }
            }
        }
        .padding(.vertical, ChefTheme.Spacing.extraSmall)
        .swipeActions(edge: .leading) {
            if !isProcessing {
                Button(action: onAccept) {
                    Label(ComposeStrings["notifications_accept"], systemImage: "checkmark")
                }
                .tint(ChefTheme.primary)
            }
        }
        .swipeActions(edge: .trailing) {
            if !isProcessing {
                Button(action: onDecline) {
                    Label(ComposeStrings["notifications_decline"], systemImage: "xmark")
                }
                .tint(.red)
            }
        }
    }
}

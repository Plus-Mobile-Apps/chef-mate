import SwiftUI
import ComposeApp

/// The redesigned AI chat history, mirroring the Android `AiChatHistoryScreen`: a "New
/// conversation" row, then past conversations; delete-all in the navigation bar. Pushed inside the
/// AI chat's full-screen cover, so the native back button and swipe-back return to the chat.
///
/// iOS conventions over the Android ones: a single conversation is deleted with a swipe (or the
/// context menu) instead of a trailing icon, and the empty state is a `ContentUnavailableView`.
/// Both deletes still confirm first, like the Android dialogs.
struct AiChatHistoryView: View {
    let bloc: AiChatHistoryBloc
    @StateObject private var model: ObservedFlow<AiChatHistoryBlocModel>
    @State private var pendingDeleteId: Int64?
    @State private var isConfirmingDeleteAll = false

    init(bloc: AiChatHistoryBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let conversations = model.value.conversations
        Group {
            if conversations.isEmpty {
                ContentUnavailableView {
                    Label(ComposeStrings["aichat_history_empty_title"], systemImage: "bubble.left.and.bubble.right")
                } description: {
                    Text(ComposeStrings["aichat_history_empty_description"])
                } actions: {
                    Button { bloc.onNewConversationClick() } label: {
                        Label(ComposeStrings["aichat_history_new_conversation"], systemImage: "plus")
                    }
                }
            } else {
                List {
                    Section {
                        Button { bloc.onNewConversationClick() } label: {
                            Label(ComposeStrings["aichat_history_new_conversation"], systemImage: "plus")
                                .foregroundStyle(ChefTheme.primary)
                        }
                    }
                    Section {
                        ForEach(conversations, id: \.id) { conversation in
                            ConversationRow(conversation: conversation) {
                                bloc.onConversationClick(conversationId: conversation.id)
                            }
                            // Not `role: .destructive`: that removes the row right away, before
                            // the confirmation.
                            .swipeActions {
                                Button { pendingDeleteId = conversation.id } label: {
                                    Label(ComposeStrings["aichat_history_delete_conversation"], systemImage: "trash")
                                }
                                .tint(.red)
                            }
                            .contextMenu {
                                Button(role: .destructive) { pendingDeleteId = conversation.id } label: {
                                    Label(ComposeStrings["aichat_history_delete_conversation"], systemImage: "trash")
                                }
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle(ComposeStrings["aichat_history_title"])
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            if !conversations.isEmpty {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(role: .destructive) { isConfirmingDeleteAll = true } label: {
                        Label(ComposeStrings["aichat_history_delete_all"], systemImage: "trash")
                    }
                }
            }
        }
        .tint(ChefTheme.primary)
        .alert(ComposeStrings["aichat_history_delete_all_dialog_title"], isPresented: $isConfirmingDeleteAll) {
            Button(ComposeStrings["aichat_history_dialog_confirm"], role: .destructive) { bloc.onDeleteAllClick() }
            Button(ComposeStrings["aichat_history_dialog_cancel"], role: .cancel) {}
        } message: {
            Text(ComposeStrings["aichat_history_delete_all_dialog_message"])
        }
        .alert(
            ComposeStrings["aichat_history_delete_dialog_title"],
            isPresented: Binding(get: { pendingDeleteId != nil }, set: { if !$0 { pendingDeleteId = nil } }),
            presenting: pendingDeleteId
        ) { id in
            Button(ComposeStrings["aichat_history_dialog_confirm"], role: .destructive) {
                bloc.onDeleteConversationClick(conversationId: id)
            }
            Button(ComposeStrings["aichat_history_dialog_cancel"], role: .cancel) {}
        } message: { _ in
            Text(ComposeStrings["aichat_history_delete_dialog_message"])
        }
    }
}

private struct ConversationRow: View {
    let conversation: AiChatConversation
    let onClick: () -> Void

    var body: some View {
        let title = conversation.title.flatMap { $0.trimmingCharacters(in: .whitespaces).isEmpty ? nil : $0 }
        Button(action: onClick) {
            Label {
                Text(title ?? ComposeStrings["aichat_history_untitled"])
                    .lineLimit(2)
                    .foregroundStyle(Color.primary)
            } icon: {
                Image(systemName: "bubble.left")
                    .foregroundStyle(.secondary)
            }
        }
    }
}

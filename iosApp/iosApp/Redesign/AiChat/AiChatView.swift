import SwiftUI
import PhotosUI
import UniformTypeIdentifiers
import ComposeApp

/// The redesigned AI chat, mirroring the Android `AiChatScreen` as presented from the root stack (a
/// full-screen modal): close (X) on the leading edge, history and new conversation trailing, the
/// recipe-context chip on top, the message log, then the error banner, the "Add recipe" pill and
/// the input bar at the bottom.
///
/// iOS conventions over the Android ones: the log is a bottom-anchored `ScrollView` that drags the
/// keyboard down interactively (no "Done" button), the input is a growing glass field with an
/// iMessage-style send button, the photo picker is `PhotosPicker`, and web links open in a Safari
/// sheet. Replies render inline markdown with `AttributedString` and appear as they stream, without
/// the Android typewriter reveal.
struct AiChatView: View {
    let bloc: AiChatBloc
    @StateObject private var model: ObservedFlow<AiChatBlocModel>
    @StateObject private var input: ObservedFlow<NSString>
    @State private var photoItem: PhotosPickerItem?
    @State private var webPage: WebPage?

    init(bloc: AiChatBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
        _input = StateObject(wrappedValue: ObservedFlow(bloc.inputText))
    }

    var body: some View {
        let state = model.value
        VStack(spacing: 0) {
            if let title = state.recipeContextTitle {
                RecipeContextChip(title: title)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, ChefTheme.Spacing.normal)
                    .padding(.top, ChefTheme.Spacing.small)
                    .padding(.bottom, ChefTheme.Spacing.extraSmall)
            }
            if state.messages.isEmpty {
                EmptyChatView()
            } else {
                MessageLog(messages: state.messages, isSending: state.isSending)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .safeAreaInset(edge: .bottom, spacing: 0) { bottomBar(state) }
        .navigationTitle(ComposeStrings["aichat_title"])
        .navigationBarTitleDisplayMode(.inline)
        .toolbar { toolbar }
        .tint(ChefTheme.primary)
        .environment(\.openURL, OpenURLAction { url in
            guard ["http", "https"].contains(url.scheme?.lowercased()) else { return .systemAction }
            webPage = WebPage(url: url)
            return .handled
        })
        .sheet(item: $webPage) { page in
            SafariView(url: page.url)
                .ignoresSafeArea()
        }
        .onChange(of: photoItem) { _, item in
            guard let item else { return }
            photoItem = nil
            Task { await deliverPhoto(item) }
        }
    }

    @ToolbarContentBuilder
    private var toolbar: some ToolbarContent {
        ToolbarItem(placement: .topBarLeading) {
            Button { bloc.onBackClicked() } label: {
                Label(ComposeStrings["aichat_close"], systemImage: "xmark")
            }
        }
        ToolbarItemGroup(placement: .topBarTrailing) {
            Button { bloc.onHistoryClick() } label: {
                Label(ComposeStrings["aichat_history"], systemImage: "clock.arrow.circlepath")
            }
            Button { bloc.onNewChatClick() } label: {
                Label(ComposeStrings["aichat_new_chat"], systemImage: "square.and.pencil")
            }
        }
    }

    private func bottomBar(_ state: AiChatBlocModel) -> some View {
        VStack(spacing: ChefTheme.Spacing.small) {
            if let error = state.error {
                ErrorBanner(text: error.localized)
            }
            if state.canAddRecipe {
                AddRecipePill(isLoading: state.isExtractingRecipe, onClick: bloc.onAddRecipeClick)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.horizontal, ChefTheme.Spacing.normal)
            }
            InputBar(
                text: Binding(
                    get: { input.current as String },
                    set: { bloc.onInputChange(text: $0) }
                ),
                isSending: state.isSending,
                isExtracting: state.isExtractingRecipe,
                photoItem: $photoItem,
                onSend: bloc.onSendClick
            )
        }
        .animation(.snappy, value: state.error != nil)
        .animation(.snappy, value: state.canAddRecipe)
    }

    /// Loads the picked photo and hands it to the BLoC, which extracts a recipe from it and keeps
    /// the bytes as the new recipe's image. HEIC is transcoded to JPEG by the picker
    /// (`.compatible`); anything that still isn't PNG or JPEG is re-encoded as JPEG.
    private func deliverPhoto(_ item: PhotosPickerItem) async {
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        let types = item.supportedContentTypes
        let photo: (Data, String)?
        if types.contains(where: { $0.conforms(to: .png) }) {
            photo = (data, "png")
        } else if types.contains(where: { $0.conforms(to: .jpeg) }) {
            photo = (data, "jpg")
        } else {
            photo = UIImage(data: data)?.jpegData(compressionQuality: 0.9).map { ($0, "jpg") }
        }
        guard let (bytes, fileExtension) = photo else { return }
        await MainActor.run {
            bloc.onPhotoPicked(bytes: NativeBytes.shared.fromData(data: bytes), fileExtension: fileExtension)
        }
    }
}

// MARK: Message log

/// The conversation, newest at the bottom. Anchored to the bottom so it opens on the latest message
/// and a streaming reply keeps its newest line in view as it grows, while scrolling back up to read
/// earlier messages still works.
private struct MessageLog: View {
    let messages: [ChatMessage]
    let isSending: Bool

    var body: some View {
        // Until the reply's first token arrives there is no model message yet (the repository
        // defers it), so a "Gemini is thinking" bubble stands in where the reply will land.
        let showThinking = isSending && !messages.contains { $0.role == .model && $0.isStreaming }
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(messages, id: \.id) { message in
                    MessageBubble(message: message)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                }
                if showThinking {
                    ThinkingBubble()
                        .transition(.opacity)
                }
            }
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .padding(.vertical, 12)
            .animation(.smooth(duration: 0.35), value: messages.map(\.id))
            .animation(.smooth, value: showThinking)
        }
        .defaultScrollAnchor(.bottom)
        .scrollDismissesKeyboard(.interactively)
    }
}

private struct MessageBubble: View {
    let message: ChatMessage

    var body: some View {
        let isUser = message.role == .user
        let content = message.content
        VStack(alignment: isUser ? .trailing : .leading, spacing: 2) {
            RoleLabel(text: ComposeStrings[isUser ? "aichat_role_you" : "aichat_role_gemini"])
            HStack(alignment: .lastTextBaseline, spacing: 6) {
                Group {
                    if content.isEmpty && message.isStreaming {
                        // Streaming has started but nothing has arrived yet.
                        Text(verbatim: "…")
                    } else if isUser {
                        Text(verbatim: content)
                    } else {
                        Text(ChatMarkdown.attributed(content))
                    }
                }
                .textSelection(.enabled)
                if message.isStreaming && !content.isEmpty {
                    ProgressView()
                        .controlSize(.mini)
                        .tint(.secondary)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .foregroundStyle(isUser ? Color.white : Color.primary)
            .background(
                isUser ? AnyShapeStyle(ChefTheme.primary) : AnyShapeStyle(Color(.secondarySystemBackground)),
                in: RoundedRectangle(cornerRadius: 16, style: .continuous)
            )
        }
        // Leave a gutter on the other side so a long message still reads as one side's bubble.
        .padding(isUser ? .leading : .trailing, 40)
        .frame(maxWidth: .infinity, alignment: isUser ? .trailing : .leading)
    }
}

/// A model bubble with an animated ellipsis in place of text while a reply is in flight.
private struct ThinkingBubble: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            RoleLabel(text: ComposeStrings["aichat_role_gemini"])
            Image(systemName: "ellipsis")
                .font(.title3.weight(.semibold))
                .symbolEffect(.variableColor.iterative.dimInactiveLayers, options: .repeating)
                .foregroundStyle(.secondary)
                .frame(height: 22)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

private struct RoleLabel: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.caption2.weight(.semibold))
            .foregroundStyle(.secondary)
            .padding(.horizontal, 4)
    }
}

private struct EmptyChatView: View {
    var body: some View {
        ContentUnavailableView {
            Label {
                Text(ComposeStrings["aichat_empty_title"])
            } icon: {
                Image(systemName: "sparkles")
                    .foregroundStyle(ChefTheme.primary)
            }
        } description: {
            Text(ComposeStrings["aichat_empty_description"])
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        // Tapping the empty state dismisses the keyboard (there is no log to drag it down with).
        .contentShape(Rectangle())
        .onTapGesture { UIApplication.shared.dismissKeyboard() }
    }
}

// MARK: Chrome

/// The recipe this chat is grounded in. Informational only, like the Android disabled chip.
private struct RecipeContextChip: View {
    let title: String

    var body: some View {
        Label {
            Text(title)
                .lineLimit(1)
                .truncationMode(.tail)
        } icon: {
            Image(systemName: "fork.knife")
                .foregroundStyle(ChefTheme.primary)
        }
        .font(.subheadline)
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .overlay(Capsule().strokeBorder(Color(.separator)))
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(ComposeStrings["aichat_recipe_context"]): \(title)")
    }
}

private struct ErrorBanner: View {
    let text: String

    var body: some View {
        Label(text, systemImage: "exclamationmark.circle")
            .font(.subheadline)
            .foregroundStyle(.red)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .padding(.vertical, ChefTheme.Spacing.small)
            .background(Color.red.opacity(0.12))
            .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}

/// Turns the conversation into a recipe (the Android assist chip).
private struct AddRecipePill: View {
    let isLoading: Bool
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            HStack(spacing: 6) {
                if isLoading {
                    ProgressView()
                        .controlSize(.small)
                } else {
                    Image(systemName: "doc.badge.plus")
                }
                Text(ComposeStrings[isLoading ? "aichat_extracting_recipe" : "aichat_add_recipe"])
            }
            .font(.subheadline.weight(.medium))
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .chefGlass()
        }
        .buttonStyle(.plain)
        .foregroundStyle(ChefTheme.primary)
        .disabled(isLoading)
        .transition(.move(edge: .bottom).combined(with: .opacity))
    }
}

/// Photo button, a growing glass text field, and the send button.
private struct InputBar: View {
    @Binding var text: String
    let isSending: Bool
    let isExtracting: Bool
    @Binding var photoItem: PhotosPickerItem?
    let onSend: () -> Void

    var body: some View {
        let canSend = !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isSending
        HStack(alignment: .bottom, spacing: ChefTheme.Spacing.small) {
            PhotosPicker(selection: $photoItem, matching: .images, preferredItemEncoding: .compatible) {
                Group {
                    if isExtracting {
                        ProgressView()
                    } else {
                        Image(systemName: "photo.badge.plus")
                            .font(.title3)
                    }
                }
                .frame(width: 44, height: 44)
                .chefGlass(in: Circle())
            }
            .disabled(isExtracting)
            .accessibilityLabel(ComposeStrings["aichat_attach_photo"])

            HStack(alignment: .bottom, spacing: ChefTheme.Spacing.extraSmall) {
                TextField(ComposeStrings["aichat_input_hint"], text: $text, axis: .vertical)
                    .lineLimit(1...6)
                    .textInputAutocapitalization(.sentences)
                    .padding(.vertical, 11)
                Button(action: onSend) {
                    Image(systemName: "arrow.up.circle.fill")
                        .font(.system(size: 30))
                        .symbolRenderingMode(.hierarchical)
                }
                .disabled(!canSend)
                .padding(.bottom, 6)
                .accessibilityLabel(ComposeStrings["aichat_send"])
            }
            .padding(.leading, ChefTheme.Spacing.normal)
            .padding(.trailing, 7)
            .frame(minHeight: 44)
            .chefGlass(in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
        .padding(.horizontal, 12)
        .padding(.top, ChefTheme.Spacing.extraSmall)
        .padding(.bottom, ChefTheme.Spacing.small)
    }
}

// MARK: Markdown

/// Renders a Gemini reply. `AttributedString` only parses inline markdown, so block syntax is
/// mapped first: list markers become bullets and headings become bold lines. Partial syntax while a
/// reply streams in (an unclosed `**`) simply shows as typed until it closes.
enum ChatMarkdown {
    static func attributed(_ markdown: String) -> AttributedString {
        let lines = markdown.split(separator: "\n", omittingEmptySubsequences: false).map(blockLine)
        let source = lines.joined(separator: "\n")
        let options = AttributedString.MarkdownParsingOptions(
            interpretedSyntax: .inlineOnlyPreservingWhitespace,
            failurePolicy: .returnPartiallyParsedIfPossible
        )
        return (try? AttributedString(markdown: source, options: options)) ?? AttributedString(markdown)
    }

    private static func blockLine(_ line: Substring) -> String {
        let indent = line.prefix { $0 == " " }
        let body = line.dropFirst(indent.count)
        // Unordered list item: "- ", "* " or "+ " → bullet, keeping nested indentation.
        if let marker = body.first, "-*+".contains(marker), body.dropFirst().first == " " {
            return String(indent) + "• " + String(body.dropFirst(2))
        }
        // ATX heading: "## Title" → bold line.
        let hashes = body.prefix { $0 == "#" }
        if (1...6).contains(hashes.count), body.dropFirst(hashes.count).first == " " {
            let title = body.dropFirst(hashes.count + 1).trimmingCharacters(in: .whitespaces)
            return title.isEmpty ? "" : "**\(title)**"
        }
        return String(line)
    }
}

extension UIApplication {
    func dismissKeyboard() {
        sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

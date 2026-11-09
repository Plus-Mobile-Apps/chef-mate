import SwiftUI

/// A recipe photo filling its frame, with a fork-and-knife placeholder while loading or when the
/// recipe has none.
struct RecipeImage: View {
    let url: String?

    var body: some View {
        AsyncImage(url: url.flatMap(URL.init(string:))) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            ZStack {
                Color(.tertiarySystemFill)
                Image(systemName: "fork.knife")
                    .foregroundStyle(.secondary)
            }
        }
    }
}

/// A circular avatar: the photo at `imageUrl`, or the first letter of `name` on the brand color.
struct LetterAvatar: View {
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
        .clipShape(Circle())
    }
}

/// A selectable capsule, the iOS take on a Material `FilterChip`.
struct ChoiceChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: ChefTheme.Spacing.extraSmall) {
                if isSelected {
                    Image(systemName: "checkmark")
                        .font(.caption.weight(.bold))
                }
                Text(title)
                    .font(.subheadline.weight(.medium))
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 7)
            .foregroundStyle(isSelected ? ChefTheme.primary : Color.primary)
            .background(
                Capsule().fill(isSelected ? ChefTheme.primary.opacity(0.15) : Color(.secondarySystemFill))
            )
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
    }
}

/// A centered status block for empty, loading, done and error states: optional icon, title, body
/// and actions stacked in the middle of the screen.
struct StatusBlock<Actions: View>: View {
    var systemImage: String?
    var title: String?
    var message: String?
    var isError = false
    var showsProgress = false
    @ViewBuilder var actions: Actions

    var body: some View {
        VStack(spacing: ChefTheme.Spacing.normal) {
            if showsProgress {
                ProgressView()
                    .controlSize(.large)
            }
            if let systemImage {
                Image(systemName: systemImage)
                    .font(.system(size: 44))
                    .foregroundStyle(ChefTheme.primary)
            }
            if let title {
                Text(title)
                    .font(.title3.weight(.semibold))
                    .multilineTextAlignment(.center)
            }
            if let message {
                Text(message)
                    .font(isError ? .body : .subheadline)
                    .foregroundStyle(isError ? Color.red : Color.secondary)
                    .multilineTextAlignment(.center)
            }
            actions
        }
        .padding(ChefTheme.Spacing.large)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

extension StatusBlock where Actions == EmptyView {
    init(systemImage: String? = nil, title: String? = nil, message: String? = nil, isError: Bool = false, showsProgress: Bool = false) {
        self.init(systemImage: systemImage, title: title, message: message, isError: isError, showsProgress: showsProgress) { EmptyView() }
    }
}

/// The cook-session controls over the Recipes and Meals tabs: a centered "Continue cooking" glass
/// capsule with a round checkmark "Done cooking" button just to its right. The checkmark hangs off
/// the capsule's trailing edge, so the capsule itself stays centered. The capsule is the zoom
/// source cook mode opens from (see `CookModeTransition`).
struct CookingSessionBar: View {
    let continueTitle: String
    let doneTitle: String
    let onContinue: () -> Void
    let onDone: () -> Void
    @SwiftUI.Environment(\.cookModeTransition) private var cookModeTransition

    var body: some View {
        continueButton
            .overlay(alignment: .trailing) {
                Button(action: onDone) {
                    Image(systemName: "checkmark")
                        .font(.headline)
                        .frame(width: 50, height: 50)
                        .contentShape(Circle())
                }
                .buttonStyle(.plain)
                .foregroundStyle(.secondary)
                .chefGlass(in: Circle())
                .accessibilityLabel(doneTitle)
                .alignmentGuide(.trailing) { $0[.leading] - ChefTheme.Spacing.small }
            }
            .frame(maxWidth: .infinity)
            .padding(.bottom, ChefTheme.Spacing.normal)
    }

    @ViewBuilder
    private var continueButton: some View {
        let button = Button(action: onContinue) {
            Label(continueTitle, systemImage: "frying.pan")
                .font(.headline)
                .padding(.horizontal, ChefTheme.Spacing.large)
                .frame(height: 50)
        }
        .buttonStyle(.plain)
        .foregroundStyle(ChefTheme.primary)
        .chefGlass()
        if let cookModeTransition {
            button.matchedTransitionSource(id: CookModeTransition.sourceId, in: cookModeTransition)
        } else {
            button
        }
    }
}

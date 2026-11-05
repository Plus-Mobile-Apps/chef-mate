import SwiftUI
import ComposeApp

/// The redesigned onboarding flow (`OnboardingRootScreen`): every visited step is a page of a paged
/// `TabView`, under a native toolbar with the back arrow, step progress, and Skip.
///
/// `OnboardingRootBloc`'s stack only holds the steps visited so far, so its depth is the current
/// step and there is never a page ahead of it: swiping only goes back. A settled swipe back pops
/// the steps it passed; the back arrow animates to the previous page first and pops once it lands,
/// so the page being left is still there while it slides away. Next/Get started push through the
/// step BLoCs, and the new page slides in once the stack grows.
struct OnboardingView: View {
    let bloc: OnboardingRootBloc
    @StateObject private var stack: ObservedValue<ChildStack<AnyObject, OnboardingRootBlocChild>>
    @State private var page: Int
    /// True while `page` is animating to a target set in code rather than by a swipe.
    @State private var isPaging = false

    init(bloc: OnboardingRootBloc) {
        self.bloc = bloc
        _stack = StateObject(wrappedValue: ObservedValue(bloc.routerState))
        _page = State(initialValue: bloc.routerState.value.items.count - 1)
    }

    var body: some View {
        let items = stack.value.items
        let currentStep = items.count - 1
        TabView(selection: $page) {
            ForEach(Array(items.enumerated()), id: \.offset) { index, item in
                OnboardingStepView(child: item.instance)
                    .tag(index)
            }
        }
        .tabViewStyle(.page(indexDisplayMode: .never))
        .background(Color(.systemBackground))
        .onChange(of: currentStep) { _, step in
            if step > page {
                animatePage(to: step)
            } else if step < page {
                // Popped from outside the pager: the page is already gone, so just follow.
                page = step
            }
        }
        .onChange(of: page) { _, _ in
            if !isPaging { popToPage() }
        }
        .navigationBarBackButtonHidden()
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                // Hidden on the first step, unless the flow was re-entered from the app, where it
                // backs out of onboarding.
                if page > 0 {
                    backButton { animatePage(to: page - 1, then: popToPage) }
                } else if bloc.isDismissible {
                    backButton { bloc.onDismissClicked() }
                }
            }
            ToolbarItem(placement: .principal) {
                OnboardingProgress(current: page, total: Int(bloc.totalSteps))
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button(ComposeStrings["onboarding_skip"]) { bloc.onSkipClicked() }
            }
        }
        .tint(ChefTheme.primary)
    }

    private func backButton(action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: "chevron.backward")
        }
        .accessibilityLabel(ComposeStrings["onboarding_back"])
    }

    private func animatePage(to target: Int, then completion: @escaping () -> Void = {}) {
        isPaging = true
        withAnimation {
            page = target
        } completion: {
            isPaging = false
            completion()
        }
    }

    /// Pops the steps past the visible page, after a swipe or the back arrow lands on it.
    private func popToPage() {
        let excess = stack.value.items.count - 1 - page
        for _ in 0..<max(excess, 0) { bloc.onBackClicked() }
    }
}

/// One onboarding step, picked by the child's type.
private struct OnboardingStepView: View {
    let child: OnboardingRootBlocChild

    var body: some View {
        switch child {
        case let step as OnboardingRootBlocChild.Welcome:
            WelcomeStep(bloc: step.bloc)
        case let step as OnboardingRootBlocChild.ShareRecipes:
            // iOS always shows the mobile copy and its own share-sheet capture.
            OnboardingInfoStep(
                title: "onboarding_share_recipes_title",
                message: "onboarding_share_recipes_message_mobile",
                image: "onboarding_preview_share_ios",
                onNext: step.bloc.onNextClicked
            )
        case let step as OnboardingRootBlocChild.SaveRecipes:
            OnboardingInfoStep(
                title: "onboarding_save_recipes_title",
                message: "onboarding_save_recipes_message",
                image: "onboarding_preview_save_recipes",
                onNext: step.bloc.onNextClicked
            )
        case let step as OnboardingRootBlocChild.AddToGrocery:
            OnboardingInfoStep(
                title: "onboarding_add_to_grocery_title",
                message: "onboarding_add_to_grocery_message",
                image: "onboarding_preview_add_to_grocery",
                onNext: step.bloc.onNextClicked
            )
        case let step as OnboardingRootBlocChild.MealPlanning:
            OnboardingInfoStep(
                title: "onboarding_meal_planning_title",
                message: "onboarding_meal_planning_message",
                image: "onboarding_preview_meal_planning",
                onNext: step.bloc.onNextClicked
            )
        case let step as OnboardingRootBlocChild.CookMode:
            OnboardingInfoStep(
                title: "onboarding_cook_mode_title",
                message: "onboarding_cook_mode_message",
                image: "onboarding_preview_cook_mode",
                onNext: step.bloc.onNextClicked
            )
        case let step as OnboardingRootBlocChild.RecipeBooks:
            OnboardingInfoStep(
                title: "onboarding_recipe_books_title",
                message: "onboarding_recipe_books_message",
                image: "onboarding_preview_recipe_books",
                onNext: step.bloc.onNextClicked
            )
        case let step as OnboardingRootBlocChild.GroceryLists:
            OnboardingInfoStep(
                title: "onboarding_grocery_lists_title",
                message: "onboarding_grocery_lists_message",
                image: "onboarding_preview_grocery_lists",
                onNext: step.bloc.onNextClicked
            )
        case let step as OnboardingRootBlocChild.FamilySharing:
            OnboardingInfoStep(
                title: "onboarding_family_sharing_title",
                message: "onboarding_family_sharing_message",
                image: "onboarding_preview_family_sharing",
                onNext: step.bloc.onNextClicked
            )
        case let step as OnboardingRootBlocChild.StartCooking:
            StartCookingStep(bloc: step.bloc)
        default:
            ComposeScreenHost(screen: child.bloc)
        }
    }
}

private struct WelcomeStep: View {
    let bloc: WelcomeBloc

    var body: some View {
        OnboardingStepLayout {
            OnboardingAppIcon()
            OnboardingTitle(key: "onboarding_welcome_title")
            OnboardingMessage(key: "onboarding_welcome_message")
        } footer: {
            OnboardingButton(key: "onboarding_welcome_get_started", action: bloc.onGetStartedClicked)
            // Hidden when an already signed-in user re-enters the flow; they can't sign in again.
            if bloc.showSignIn {
                OnboardingButton(key: "onboarding_welcome_sign_in", prominent: false, action: bloc.onSignInClicked)
            }
        }
    }
}

/// The informational steps (`OnboardingInfoLayout`): title, message, then a framed capture of the
/// real feature, placed last so a tall step visibly scrolls.
private struct OnboardingInfoStep: View {
    let title: String
    let message: String
    let image: String
    let onNext: () -> Void

    var body: some View {
        OnboardingStepLayout {
            OnboardingTitle(key: title)
            OnboardingMessage(key: message)
            OnboardingPreviewImage(name: image)
        } footer: {
            OnboardingButton(key: "onboarding_next", action: onNext)
        }
    }
}

private struct StartCookingStep: View {
    let bloc: StartCookingBloc

    var body: some View {
        OnboardingStepLayout {
            OnboardingAppIcon()
            Image(systemName: "party.popper")
                .font(.system(size: 56))
                .foregroundStyle(ChefTheme.primary)
                .accessibilityHidden(true)
            OnboardingTitle(key: "onboarding_start_cooking_title")
            OnboardingMessage(key: "onboarding_start_cooking_message")
        } footer: {
            // Hidden when an already signed-in user re-enters the flow; they can't sign up again.
            if bloc.showSignUp {
                OnboardingButton(key: "onboarding_start_cooking_sign_up", action: bloc.onSignUpClicked)
            }
            OnboardingButton(key: "onboarding_start_cooking_button", prominent: false, action: bloc.onStartCookingClicked)
        }
    }
}

// MARK: - Shared layout

/// `PlusHeaderContainerDefaults.MaxContentWidth`.
private let maxContentWidth: CGFloat = 600
private let resourcesPackage = "chefmate.client.onboarding.public.generated.resources"

/// `OnboardingStepScaffold`: the content scrolls and stays vertically centered while it fits; the
/// footer floats over the bottom, and the content scrolls behind it, clear of it at the end.
private struct OnboardingStepLayout<Content: View, Footer: View>: View {
    @ViewBuilder let content: Content
    @ViewBuilder let footer: Footer
    @State private var visibleHeight: CGFloat = 0

    var body: some View {
        ScrollView {
            VStack(spacing: ChefTheme.Spacing.normal) {
                content
            }
            .multilineTextAlignment(.center)
            .frame(maxWidth: maxContentWidth)
            .padding(ChefTheme.Spacing.large)
            .frame(maxWidth: .infinity, minHeight: visibleHeight)
        }
        .scrollBounceBehavior(.basedOnSize)
        .onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.height - proxy.safeAreaInsets.top - proxy.safeAreaInsets.bottom
        } action: { visibleHeight = max($0, 0) }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            VStack(spacing: ChefTheme.Spacing.small) {
                footer
            }
            .frame(maxWidth: maxContentWidth)
            .padding(.horizontal, ChefTheme.Spacing.large)
            .padding(.bottom, ChefTheme.Spacing.normal)
        }
    }
}

private struct OnboardingTitle: View {
    let key: String

    var body: some View {
        Text(ComposeStrings[key])
            .font(.title.bold())
            .accessibilityAddTraits(.isHeader)
    }
}

private struct OnboardingMessage: View {
    let key: String

    var body: some View {
        Text(ComposeStrings[key])
            .font(.body)
            .foregroundStyle(.secondary)
    }
}

/// `PlusButton`: primary is filled in the brand color, secondary is tonal.
private struct OnboardingButton: View {
    let key: String
    var prominent = true
    let action: () -> Void

    var body: some View {
        let button = Button(action: action) {
            Text(ComposeStrings[key])
                .font(.headline)
                .frame(maxWidth: .infinity)
        }
        .controlSize(.large)
        .tint(ChefTheme.primary)
        if prominent {
            button.buttonStyle(.borderedProminent).foregroundStyle(ChefTheme.onPrimary)
        } else {
            button.buttonStyle(.bordered)
        }
    }
}

private struct OnboardingAppIcon: View {
    var body: some View {
        if let icon = ComposeDrawables.image("onboarding_app_icon", package: resourcesPackage) {
            Image(uiImage: icon)
                .resizable()
                .frame(width: 200, height: 200)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .accessibilityHidden(true)
        }
    }
}

/// `OnboardingPreviewImage`: a framed capture of the real feature screen, the dark capture in dark
/// mode, capped at about a phone's width and never cropped.
private struct OnboardingPreviewImage: View {
    let name: String
    // Qualified: ComposeApp exports its own `Environment`.
    @SwiftUI.Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let shape = RoundedRectangle(cornerRadius: 20, style: .continuous)
        if let image = ComposeDrawables.image(colorScheme == .dark ? "\(name)_dark" : name, package: resourcesPackage) {
            Image(uiImage: image)
                .resizable()
                .aspectRatio(contentMode: .fit)
                .clipShape(shape)
                .overlay(shape.strokeBorder(Color(.separator), lineWidth: 1))
                .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
                .frame(maxWidth: 560)
                .accessibilityHidden(true)
        }
    }
}

/// One dot per step, the current one a wider pill. Decorative, like the Compose indicator.
private struct OnboardingProgress: View {
    let current: Int
    let total: Int

    var body: some View {
        HStack(spacing: 6) {
            ForEach(0..<total, id: \.self) { index in
                let active = index == current
                Capsule()
                    .fill(active ? AnyShapeStyle(ChefTheme.primary) : AnyShapeStyle(.quaternary))
                    .frame(width: active ? 24 : 8, height: 8)
            }
        }
        .animation(.default, value: current)
        .accessibilityHidden(true)
    }
}

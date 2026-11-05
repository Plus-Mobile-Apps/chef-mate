import SwiftUI
import ComposeApp

/// The redesigned Cook Mode, mirroring the Android `CookModeScreen`. Presented as a full-screen
/// cover (see `RootStackModel.modalEntries`). The active recipe's ingredients and directions show in
/// one of two layouts the BLoC remembers: stacked (one list under pinned headers) or split (swipeable
/// Ingredients / Directions pages on iPhone, two resizable columns on iPad). A floating glass bar
/// switches between the recipes being cooked and opens What's Cooking.
///
/// Ingredient cross-offs and the highlighted step are screen-local, like on Android: they survive
/// layout toggles and reset when the active recipe changes.
struct CookModeView: View {
    let bloc: CookModeBloc
    @StateObject private var model: ObservedFlow<CookModeBlocModel>
    @State private var showFinishConfirm = false
    @State private var showWhatsCooking = false
    @State private var crossedOut = Set<Int>()
    @State private var highlightedStep: Int?
    @SwiftUI.Environment(\.horizontalSizeClass) private var sizeClass

    init(bloc: CookModeBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        content(state)
            .navigationTitle(state.activeRecipe?.title ?? "")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { toolbar(state) }
            .safeAreaInset(edge: .top, spacing: 0) {
                CoachMarkBanner(activeId: state.activeCoachMark, onDismiss: { bloc.onCoachMarkDismissed(id: $0) })
            }
            .safeAreaInset(edge: .bottom, spacing: 0) {
                CookingBar(
                    chips: state.activeSessions,
                    onWhatsCookingClicked: { showWhatsCooking = true },
                    onChipClicked: { bloc.onRecipeChipClicked(recipeId: $0) }
                )
            }
            .tint(ChefTheme.primary)
            .sheet(isPresented: $showWhatsCooking) {
                WhatsCookingSheet(bloc: bloc.whatsCookingBloc, onDismiss: { showWhatsCooking = false })
                    .presentationDetents([.medium, .large])
                    .presentationDragIndicator(.visible)
            }
            .alert(ComposeStrings["cook_mode_finish_confirm_title"], isPresented: $showFinishConfirm) {
                Button(ComposeStrings["cook_mode_finish_confirm_button"], role: .destructive, action: bloc.onFinishClicked)
                Button(ComposeStrings["cook_mode_finish_cancel_button"], role: .cancel) {}
            } message: {
                Text(ComposeStrings["cook_mode_finish_confirm_message"])
            }
            .alert(
                ComposeStrings["premium_required_title"],
                isPresented: Binding(
                    get: { model.value.showPremiumRequiredDialog },
                    set: { shown in
                        if !shown && model.current.showPremiumRequiredDialog { bloc.onPremiumRequiredDismissed() }
                    }
                )
            ) {
                Button(ComposeStrings["premium_required_confirm"], role: .cancel, action: bloc.onPremiumRequiredDismissed)
            } message: {
                Text(ComposeStrings["premium_required_message"])
            }
            .keepScreenOn(state.keepScreenOn)
            // Switching recipes starts with a clean slate.
            .onChange(of: state.activeRecipe?.id) {
                crossedOut = []
                highlightedStep = nil
            }
            .animation(.snappy, value: state.activeCoachMark)
    }

    // MARK: Body

    @ViewBuilder
    private func content(_ state: CookModeBlocModel) -> some View {
        if state.isLoading {
            VStack(spacing: ChefTheme.Spacing.normal) {
                ProgressView()
                Text(ComposeStrings["cook_mode_loading"])
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let recipe = state.activeRecipe {
            let formatter = RecipeTextFormatter.shared
            let lines = CookLines(
                ingredients: formatter.ingredientLines(recipe: recipe, scale: state.ingredientScale),
                directions: formatter.directionLines(recipe: recipe),
                crossedOut: $crossedOut,
                highlightedStep: $highlightedStep
            )
            switch state.layoutMode {
            case .stacked:
                StackedBody(lines: lines)
            default:
                if sizeClass == .regular {
                    SplitWideBody(lines: lines)
                } else {
                    SplitCompactBody(lines: lines)
                }
            }
        } else {
            Text(ComposeStrings["cook_mode_no_active_recipe"])
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }

    // MARK: Toolbar

    @ToolbarContentBuilder
    private func toolbar(_ state: CookModeBlocModel) -> some ToolbarContent {
        ToolbarItem(placement: .cancellationAction) {
            Button { bloc.onCloseClicked() } label: {
                Label(ComposeStrings["cook_mode_close"], systemImage: "xmark")
            }
        }
        if sizeClass == .regular {
            // iPad has room for every control, like the Android tablet header.
            ToolbarItemGroup(placement: .topBarTrailing) {
                if state.activeRecipe != nil {
                    Button { bloc.onViewRecipeClicked() } label: {
                        Label(ComposeStrings["cook_mode_view_recipe"], systemImage: "book")
                    }
                }
                if state.showAiChat {
                    aiChatButton(state)
                }
                scaleMenu(state) {
                    Text(RecipeTextFormatter.shared.scaleLabel(factor: state.ingredientScale))
                }
                keepScreenOnButton(state)
                layoutButton(state)
            }
        } else {
            // Phone widths collapse everything but Finish behind one menu.
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    if state.activeRecipe != nil {
                        Button { bloc.onViewRecipeClicked() } label: {
                            Label(ComposeStrings["cook_mode_view_recipe"], systemImage: "book")
                        }
                    }
                    if state.showAiChat {
                        aiChatButton(state)
                    }
                    scaleMenu(state) {
                        Label(
                            "\(ComposeStrings["cook_mode_scale_ingredients"]) (\(RecipeTextFormatter.shared.scaleLabel(factor: state.ingredientScale)))",
                            systemImage: "scalemass"
                        )
                    }
                    keepScreenOnButton(state)
                    layoutButton(state)
                } label: {
                    Label(ComposeStrings["cook_mode_more_options"], systemImage: "ellipsis")
                }
            }
        }
        ToolbarItem(placement: .confirmationAction) {
            Button { showFinishConfirm = true } label: {
                Label(ComposeStrings["cook_mode_finish"], systemImage: "checkmark")
            }
        }
    }

    /// Still tappable for a non-subscriber: the BLoC opens the premium upsell instead.
    private func aiChatButton(_ state: CookModeBlocModel) -> some View {
        Button { bloc.onAiChatClicked() } label: {
            Label(
                ComposeStrings[state.isSubscribed ? "cook_mode_ai_chat" : "cook_mode_ai_chat_premium"],
                systemImage: state.isSubscribed ? "sparkles" : "lock.fill"
            )
        }
    }

    /// The ingredient scale picker (½× through 4×), persisted per recipe by the BLoC.
    private func scaleMenu<L: View>(_ state: CookModeBlocModel, @ViewBuilder label: () -> L) -> some View {
        let formatter = RecipeTextFormatter.shared
        return Menu {
            Picker(
                ComposeStrings["cook_mode_scale_ingredients"],
                selection: Binding(get: { model.current.ingredientScale }, set: { bloc.onScaleChanged(scale: $0) })
            ) {
                ForEach(formatter.scaleOptions.map(\.doubleValue), id: \.self) { option in
                    Text(formatter.scaleLabel(factor: option)).tag(option)
                }
            }
        } label: {
            label()
        }
        .accessibilityLabel(ComposeStrings["cook_mode_scale_ingredients"])
        .accessibilityValue(formatter.scaleLabel(factor: state.ingredientScale))
    }

    private func keepScreenOnButton(_ state: CookModeBlocModel) -> some View {
        Button { bloc.onKeepScreenOnToggled() } label: {
            Label(
                ComposeStrings[state.keepScreenOn ? "cook_mode_allow_screen_off" : "cook_mode_keep_screen_on"],
                systemImage: state.keepScreenOn ? "sun.max.fill" : "moon.zzz"
            )
        }
    }

    private func layoutButton(_ state: CookModeBlocModel) -> some View {
        let isStacked = state.layoutMode == .stacked
        return Button { bloc.onLayoutToggled() } label: {
            Label(
                ComposeStrings[isStacked ? "cook_mode_layout_split" : "cook_mode_layout_stacked"],
                systemImage: isStacked ? "rectangle.split.2x1" : "list.bullet"
            )
        }
    }
}

// MARK: - Layouts

/// The active recipe's lines plus the screen-local cooking progress, shared by every layout.
private struct CookLines {
    let ingredients: [RecipeTextLine]
    let directions: [RecipeTextLine]
    @Binding var crossedOut: Set<Int>
    @Binding var highlightedStep: Int?

    @ViewBuilder
    var ingredientRows: some View {
        ForEach(Array(ingredients.enumerated()), id: \.offset) { index, line in
            IngredientLine(line: line, isCrossedOut: crossedOut.contains(index)) {
                if crossedOut.contains(index) { crossedOut.remove(index) } else { crossedOut.insert(index) }
            }
        }
    }

    @ViewBuilder
    var directionRows: some View {
        ForEach(Array(directions.enumerated()), id: \.offset) { index, line in
            DirectionLine(line: line, isHighlighted: highlightedStep == index) {
                highlightedStep = highlightedStep == index ? nil : index
            }
        }
    }
}

/// Ingredients then directions in one scroll; the current section's header stays pinned.
private struct StackedBody: View {
    let lines: CookLines

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall, pinnedViews: [.sectionHeaders]) {
                Section {
                    lines.ingredientRows
                } header: {
                    CookSectionHeader(title: ComposeStrings["cook_mode_ingredients"])
                }
                Section {
                    lines.directionRows
                } header: {
                    CookSectionHeader(title: ComposeStrings["cook_mode_directions"])
                }
            }
            .padding(.bottom, ChefTheme.Spacing.normal)
        }
    }
}

/// iPhone split: a segmented control over swipeable Ingredients / Directions pages.
private struct SplitCompactBody: View {
    let lines: CookLines
    @State private var page = 0

    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $page.animation()) {
                Text(ComposeStrings["cook_mode_ingredients"]).tag(0)
                Text(ComposeStrings["cook_mode_directions"]).tag(1)
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .padding(.vertical, ChefTheme.Spacing.small)

            TabView(selection: $page) {
                pageContent { lines.ingredientRows }.tag(0)
                pageContent { lines.directionRows }.tag(1)
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
        }
    }

    private func pageContent<Rows: View>(@ViewBuilder rows: () -> Rows) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
                rows()
            }
            .padding(.vertical, ChefTheme.Spacing.small)
        }
    }
}

/// iPad split: ingredients and directions side by side, the divider draggable from 20% to 80%.
private struct SplitWideBody: View {
    let lines: CookLines
    @State private var ingredientsFraction: CGFloat = 0.5
    @State private var dragStartFraction: CGFloat?

    var body: some View {
        GeometryReader { geometry in
            let handleWidth: CGFloat = 16
            let available = max(geometry.size.width - handleWidth, 1)
            HStack(spacing: 0) {
                column(title: ComposeStrings["cook_mode_ingredients"]) { lines.ingredientRows }
                    .frame(width: available * ingredientsFraction)
                dragHandle(available: available)
                    .frame(width: handleWidth)
                column(title: ComposeStrings["cook_mode_directions"]) { lines.directionRows }
            }
        }
    }

    private func column<Rows: View>(title: String, @ViewBuilder rows: () -> Rows) -> some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall, pinnedViews: [.sectionHeaders]) {
                Section {
                    rows()
                } header: {
                    CookSectionHeader(title: title)
                }
            }
            .padding(.bottom, ChefTheme.Spacing.normal)
        }
    }

    private func dragHandle(available: CGFloat) -> some View {
        Capsule()
            .fill(Color(.separator))
            .frame(width: 4, height: 40)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
            .gesture(
                DragGesture()
                    .onChanged { value in
                        let start = dragStartFraction ?? ingredientsFraction
                        dragStartFraction = start
                        ingredientsFraction = min(max(start + value.translation.width / available, 0.2), 0.8)
                    }
                    .onEnded { _ in dragStartFraction = nil }
            )
            .accessibilityHidden(true)
    }
}

/// A pinned section header on the bar material, so lines scroll under it.
private struct CookSectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.headline)
            .foregroundStyle(ChefTheme.primary)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .padding(.vertical, 12)
            .background(.bar)
            .overlay(alignment: .bottom) { Divider() }
            .accessibilityAddTraits(.isHeader)
    }
}

// MARK: - Chrome

/// The floating glass bar: What's Cooking plus a chip per recipe in cook mode, the active one
/// filled. The Android phone layout uses a peeking bottom sheet; iOS uses this bar on every size
/// and opens What's Cooking as a native sheet.
private struct CookingBar: View {
    let chips: [CookModeBlocModel.Chip]
    let onWhatsCookingClicked: () -> Void
    let onChipClicked: (Int64) -> Void

    var body: some View {
        HStack(spacing: ChefTheme.Spacing.extraSmall) {
            Button(action: onWhatsCookingClicked) {
                Image(systemName: "fork.knife")
                    .font(.title3)
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .foregroundStyle(ChefTheme.primary)
            .accessibilityLabel(ComposeStrings["cook_mode_whats_cooking"])

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: ChefTheme.Spacing.small) {
                    ForEach(chips, id: \.recipeId) { chip in
                        Button { onChipClicked(chip.recipeId) } label: {
                            Text(chip.title)
                                .font(.subheadline.weight(.medium))
                                .lineLimit(1)
                                .frame(maxWidth: 200)
                                .padding(.horizontal, 12)
                                .padding(.vertical, 8)
                                .foregroundStyle(chip.isActive ? Color.white : Color.primary)
                                .background(
                                    chip.isActive ? AnyShapeStyle(ChefTheme.primary) : AnyShapeStyle(Color(.tertiarySystemFill)),
                                    in: Capsule()
                                )
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(chip.isActive ? .isSelected : [])
                    }
                }
                .padding(.trailing, ChefTheme.Spacing.small)
            }
            .scrollClipDisabled()
            .clipShape(Capsule())
        }
        .padding(.leading, 6)
        .padding(.vertical, 6)
        .chefGlass()
        .padding(.horizontal, ChefTheme.Spacing.normal)
        .padding(.bottom, ChefTheme.Spacing.small)
    }
}

/// The first-run coach marks for the header controls, as a dismissible glass callout under the
/// navigation bar (on iPhone those controls sit in the overflow menu, so there is no single button
/// to point at).
private struct CoachMarkBanner: View {
    let activeId: String?
    let onDismiss: (String) -> Void

    var body: some View {
        if let id = activeId, let message = Self.message(for: id) {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: "lightbulb")
                    .foregroundStyle(ChefTheme.primary)
                Text(message)
                    .font(.subheadline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                Button { onDismiss(id) } label: {
                    Image(systemName: "xmark")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(ComposeStrings["coach_mark_dismiss"])
            }
            .padding(12)
            .chefGlass(in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .padding(.horizontal, ChefTheme.Spacing.normal)
            .padding(.vertical, ChefTheme.Spacing.small)
            .transition(.move(edge: .top).combined(with: .opacity))
            .onTapGesture { onDismiss(id) }
        }
    }

    private static func message(for id: String) -> String? {
        switch id {
        case CoachMarkId.shared.COOK_MODE_KEEP_SCREEN_ON: ComposeStrings["cook_mode_keep_screen_on_onboarding"]
        case CoachMarkId.shared.COOK_MODE_LAYOUT: ComposeStrings["cook_mode_layout_onboarding"]
        default: nil
        }
    }
}

extension View {
    /// Keeps the display awake while this view is on screen and `enabled` is true — the native
    /// counterpart of Compose's `KeepScreenOn()`.
    func keepScreenOn(_ enabled: Bool) -> some View {
        onAppear { UIApplication.shared.isIdleTimerDisabled = enabled }
            .onChange(of: enabled) { _, on in UIApplication.shared.isIdleTimerDisabled = on }
            .onDisappear { UIApplication.shared.isIdleTimerDisabled = false }
    }
}

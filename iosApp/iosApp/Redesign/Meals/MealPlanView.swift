import SwiftUI
import ComposeApp

/// The redesigned Meals tab, mirroring the Android `MealPlanScreen`: a Day / Week / Month segmented
/// control, a previous / next date row, and the planned meals grouped by meal type (or by day in
/// the week view; under a month calendar in the month view). Swipe a meal to remove it; tap it to
/// open the recipe. With family plans, the navigation title becomes a menu to switch plans.
///
/// Not yet ported (docs/ios-swiftui-redesign.md, PR 10): the first-run coach marks.
struct MealPlanView: View {
    let bloc: MealPlanBloc
    @StateObject private var model: ObservedFlow<MealPlanBlocModel>

    init(bloc: MealPlanBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        VStack(spacing: 0) {
            controls(state)
            content(state)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                // Let the last meal scroll clear of the cook-mode buttons.
                .contentMargins(.bottom, state.cookingRecipeCount > 0 ? 130 : 0, for: .scrollContent)
        }
        .navigationTitle(title(state))
        // The title menu (the personal / family plan switcher) only shows on an inline title.
        .navigationBarTitleDisplayMode(state.planSelector != nil ? .inline : .large)
        .toolbarTitleMenu { planMenu(state) }
        .toolbar {
            ToolbarItemGroup(placement: .topBarTrailing) {
                Button(action: bloc.onSyncClicked) {
                    if state.isSyncing {
                        ProgressView()
                    } else {
                        Label(ComposeStrings["meal_plan_sync_action"], systemImage: "arrow.triangle.2.circlepath")
                    }
                }
                .disabled(state.isSyncing)
                Button(action: bloc.onAddMealClicked) {
                    Label(ComposeStrings["meal_plan_add_meal"], systemImage: "plus")
                }
            }
        }
        .overlay(alignment: .bottomTrailing) {
            if state.cookingRecipeCount > 0 {
                CookingButtons(onContinue: bloc.onContinueCookingClicked, onDone: bloc.onDoneCookingClicked)
            }
        }
        .tint(ChefTheme.primary)
        .alert(
            ComposeStrings["meal_plan_delete_title"],
            isPresented: alertBinding({ $0.mealToDelete != nil }, dismiss: bloc.onDeleteMealDismissed)
        ) {
            Button(ComposeStrings["meal_plan_delete_confirm"], role: .destructive, action: bloc.onDeleteMealConfirmed)
            Button(ComposeStrings["meal_plan_delete_cancel"], role: .cancel, action: bloc.onDeleteMealDismissed)
        } message: {
            Text(ComposeStrings["meal_plan_delete_message"])
        }
        .alert(
            ComposeStrings["meal_plan_replace_cook_mode_title"],
            isPresented: alertBinding({ $0.pendingReplaceCookMode != nil }, dismiss: bloc.onReplaceCookModeDismissed)
        ) {
            Button(ComposeStrings["meal_plan_replace_cook_mode_confirm"], action: bloc.onReplaceCookModeConfirmed)
            Button(ComposeStrings["meal_plan_replace_cook_mode_cancel"], role: .cancel, action: bloc.onReplaceCookModeDismissed)
        } message: {
            Text(ComposeStrings["meal_plan_replace_cook_mode_message"])
        }
        .alert(
            ComposeStrings["meal_plan_done_cooking_title"],
            isPresented: alertBinding({ $0.showDoneCookingDialog }, dismiss: bloc.onDoneCookingDismissed)
        ) {
            Button(ComposeStrings["meal_plan_done_cooking_confirm"], role: .destructive, action: bloc.onDoneCookingConfirmed)
            Button(ComposeStrings["meal_plan_done_cooking_cancel"], role: .cancel, action: bloc.onDoneCookingDismissed)
        } message: {
            Text(ComposeStrings["meal_plan_done_cooking_message"])
        }
    }

    // MARK: Header

    private func title(_ state: MealPlanBlocModel) -> String {
        if let selector = state.planSelector {
            mealPlanName(families: selector.families, selectedFamilyId: selector.selectedFamilyId)
        } else {
            ComposeStrings["meal_plan_title"]
        }
    }

    /// The plan switcher, shown as the title menu only when the user has family plans.
    @ViewBuilder
    private func planMenu(_ state: MealPlanBlocModel) -> some View {
        if let selector = state.planSelector {
            MealPlanMenuItems(
                families: selector.families,
                selectedFamilyId: selector.selectedFamilyId,
                onSelect: { bloc.onPlanSelected(familyId: $0) }
            )
        }
    }

    private func controls(_ state: MealPlanBlocModel) -> some View {
        VStack(spacing: ChefTheme.Spacing.small) {
            Picker(
                ComposeStrings["meal_plan_title"],
                selection: Binding(get: { model.current.viewMode }, set: { bloc.onViewModeSelected(mode: $0) })
            ) {
                Text(ComposeStrings["meal_plan_day"]).tag(MealPlanBlocViewMode.day)
                Text(ComposeStrings["meal_plan_week"]).tag(MealPlanBlocViewMode.week)
                Text(ComposeStrings["meal_plan_month"]).tag(MealPlanBlocViewMode.month)
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, ChefTheme.Spacing.normal)

            HStack {
                Button(action: bloc.onPreviousClicked) {
                    Image(systemName: "chevron.left")
                        .frame(width: 44, height: 44)
                }
                Spacer()
                Text(state.dateLabel.localized)
                    .font(.headline)
                    .multilineTextAlignment(.center)
                    // The week range ("November 01, 2026 - November 07, 2026") is long; shrink it
                    // onto one line rather than wrapping.
                    .lineLimit(1)
                    .minimumScaleFactor(0.7)
                    .contentTransition(.numericText())
                    .animation(.default, value: state.dateLabel.localized)
                Spacer()
                Button(action: bloc.onNextClicked) {
                    Image(systemName: "chevron.right")
                        .frame(width: 44, height: 44)
                }
            }
            .padding(.horizontal, ChefTheme.Spacing.small)
        }
        .padding(.top, ChefTheme.Spacing.small)
    }

    // MARK: Content

    @ViewBuilder
    private func content(_ state: MealPlanBlocModel) -> some View {
        if state.isLoading {
            ProgressView()
        } else {
            switch state.viewMode {
            case .week:
                weekList(state.weekMeals ?? [])
            case .month:
                monthList(state.monthModel)
            default:
                dayList(state.dayMeals)
            }
        }
    }

    private func dayList(_ meals: MealPlanBlocDayMeals?) -> some View {
        Group {
            if let meals, !meals.isEmpty {
                List {
                    mealSections(meals)
                }
                .listStyle(.plain)
            } else {
                EmptyMeals()
            }
        }
        .refreshable { bloc.onSyncClicked() }
    }

    private func weekList(_ days: [MealPlanBlocDayGroup]) -> some View {
        let nonEmpty = days.filter { !$0.meals.isEmpty }
        return Group {
            if nonEmpty.isEmpty {
                EmptyMeals()
            } else {
                List {
                    ForEach(Array(nonEmpty.enumerated()), id: \.offset) { _, day in
                        Section {
                            ForEach(day.meals, id: \.id) { meal in
                                mealRow(meal, showsMealType: true)
                            }
                        } header: {
                            MealSectionHeader(title: day.dateLabel.localized)
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
        .refreshable { bloc.onSyncClicked() }
    }

    @ViewBuilder
    private func monthList(_ month: MealPlanBlocMonthModel?) -> some View {
        if let month {
            List {
                MealMonthCalendar(
                    firstDayOfMonth: month.firstDayOfMonth,
                    selectedKey: calendarKey(month.selectedDay),
                    daysWithMeals: month.daysWithMeals,
                    onSelect: { cell in
                        if let date = cell.date { bloc.onMonthDaySelected(date: date) }
                    }
                )
                .listRowSeparator(.hidden)
                .padding(.vertical, ChefTheme.Spacing.small)

                if month.selectedDayMeals.isEmpty {
                    Text(ComposeStrings["meal_plan_no_meals"])
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, minHeight: 120)
                        .listRowSeparator(.hidden)
                } else {
                    mealSections(month.selectedDayMeals)
                }
            }
            .listStyle(.plain)
            .refreshable { bloc.onSyncClicked() }
        } else {
            EmptyMeals()
        }
    }

    /// Breakfast, lunch, dinner and snacks sections, each with the cook-mode actions in its header.
    @ViewBuilder
    private func mealSections(_ meals: MealPlanBlocDayMeals) -> some View {
        ForEach(meals.sections, id: \.type) { section in
            Section {
                ForEach(section.meals, id: \.id) { meal in
                    mealRow(meal, showsMealType: false)
                }
            } header: {
                MealSectionHeader(title: section.type.title) {
                    Button { bloc.onReplaceCookModeClicked(meals: section.meals) } label: {
                        Label(ComposeStrings["meal_plan_replace_cook_mode"], systemImage: "arrow.triangle.2.circlepath")
                    }
                    Button { bloc.onAddToCookModeClicked(meals: section.meals) } label: {
                        Label(ComposeStrings["meal_plan_add_to_cook_mode"], systemImage: "text.badge.plus")
                    }
                }
                .buttonStyle(.borderless)
            }
        }
    }

    private func mealRow(_ meal: MealPlanItem, showsMealType: Bool) -> some View {
        Button { bloc.onMealClicked(item: meal) } label: {
            HStack(spacing: ChefTheme.Spacing.normal) {
                MealThumbnail(url: meal.recipeImageUrl, size: showsMealType ? 40 : 48)
                if showsMealType {
                    Text(meal.mealType.title)
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(ChefTheme.primary)
                }
                Text(meal.recipeTitle)
                    .font(.body)
                    .foregroundStyle(Color.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                MealSyncStatusIcon(status: meal.syncStatus.name)
                    .font(.footnote)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .swipeActions(edge: .trailing) {
            Button(role: .destructive) { bloc.onDeleteMealClicked(item: meal) } label: {
                Label(ComposeStrings["meal_plan_delete"], systemImage: "trash")
            }
            .tint(.red)
        }
        .contextMenu {
            Button(role: .destructive) { bloc.onDeleteMealClicked(item: meal) } label: {
                Label(ComposeStrings["meal_plan_delete"], systemImage: "trash")
            }
        }
    }

    // MARK: Bindings

    /// An alert's visibility, driven by the model. Dismissing it (including after a button tap)
    /// only notifies the BLoC if it is still showing, so a confirm isn't followed by a dismiss.
    private func alertBinding(_ isShown: @escaping (MealPlanBlocModel) -> Bool, dismiss: @escaping () -> Void) -> Binding<Bool> {
        Binding(
            get: { isShown(model.value) },
            set: { shown in
                if !shown && isShown(model.current) { dismiss() }
            }
        )
    }
}

/// The cook-mode session buttons, stacked at the bottom trailing corner like the Android FABs.
private struct CookingButtons: View {
    let onContinue: () -> Void
    let onDone: () -> Void

    var body: some View {
        VStack(alignment: .trailing, spacing: ChefTheme.Spacing.small) {
            Button(action: onDone) {
                Label(ComposeStrings["meal_plan_done_cooking"], systemImage: "checkmark")
                    .font(.subheadline.weight(.semibold))
                    .padding(.horizontal, ChefTheme.Spacing.normal)
                    .padding(.vertical, ChefTheme.Spacing.small + 2)
            }
            .buttonStyle(.plain)
            .chefGlass()

            Button(action: onContinue) {
                Label(ComposeStrings["meal_plan_continue_cooking"], systemImage: "frying.pan.fill")
                    .font(.headline)
                    .foregroundStyle(.white)
                    .padding(.horizontal, ChefTheme.Spacing.large - 4)
                    .padding(.vertical, ChefTheme.Spacing.normal - 2)
                    .background(ChefTheme.primary, in: Capsule())
            }
            .buttonStyle(.plain)
        }
        .padding(ChefTheme.Spacing.normal)
        // Keep toasts (e.g. "added to cook mode") from covering the buttons.
        .toastClearance(120)
    }
}

private struct EmptyMeals: View {
    var body: some View {
        ScrollView {
            ContentUnavailableView(ComposeStrings["meal_plan_no_meals"], systemImage: "calendar")
                .containerRelativeFrame(.vertical)
        }
    }
}

/// A day's meals grouped by type, in day order, skipping empty types.
struct MealTypeSection {
    let type: MealType
    let meals: [MealPlanItem]
}

extension MealPlanBlocDayMeals {
    var sections: [MealTypeSection] {
        [
            MealTypeSection(type: .breakfast, meals: breakfast),
            MealTypeSection(type: .lunch, meals: lunch),
            MealTypeSection(type: .dinner, meals: dinner),
            MealTypeSection(type: .snacks, meals: snacks),
        ]
        .filter { !$0.meals.isEmpty }
    }

    var isEmpty: Bool { sections.isEmpty }
}

import SwiftUI
import ComposeApp

/// The add-meal flow (`MealPlannerRootBloc`) as a sheet with its own `NavigationStack`, mirroring
/// the Android `MealPlannerRootScreen`: pick a recipe (when opened from the Meals tab), choose a
/// date, then a meal type. On a regular-width layout the meal type is picked beside the calendar
/// and Save replaces Next, like the Compose combined layout.
///
/// The stack mirrors the BLoC's `routerState`; a swipe-back or back button only asks the BLoC to
/// pop. Closing the sheet pops the whole `MealPlanner` root child (see `RedesignRootView`).
struct MealPlannerSheet: View {
    let bloc: MealPlannerRootBloc
    @StateObject private var stack: ObservedValue<ChildStack<AnyObject, MealPlannerRootBlocChild>>

    init(bloc: MealPlannerRootBloc) {
        self.bloc = bloc
        _stack = StateObject(wrappedValue: ObservedValue(bloc.routerState))
    }

    var body: some View {
        let entries = stack.value.items.map { PlannerEntry(child: $0.instance) }
        if let base = entries.first {
            let pushed = Array(entries.dropFirst())
            NavigationStack(path: Binding(
                get: { pushed },
                set: { newPath in
                    // Only pops originate from SwiftUI; pushes come from the BLoC.
                    for _ in 0..<max(0, pushed.count - newPath.count) { bloc.onBackClicked() }
                }
            )) {
                PlannerChildView(child: base.child, isRoot: true, onClose: bloc.onBackClicked)
                    .navigationDestination(for: PlannerEntry.self) {
                        PlannerChildView(child: $0.child, isRoot: false, onClose: bloc.onBackClicked)
                    }
            }
            .tint(ChefTheme.primary)
        }
    }
}

/// One add-meal step, identified by its (stable) Decompose child instance.
private struct PlannerEntry: Hashable {
    let child: MealPlannerRootBlocChild

    static func == (lhs: PlannerEntry, rhs: PlannerEntry) -> Bool { lhs.child === rhs.child }
    func hash(into hasher: inout Hasher) { hasher.combine(ObjectIdentifier(child)) }
}

private struct PlannerChildView: View {
    let child: MealPlannerRootBlocChild
    /// The sheet's first screen gets a close button instead of a back button.
    let isRoot: Bool
    let onClose: () -> Void

    var body: some View {
        Group {
            switch child {
            case let picker as MealPlannerRootBlocChild.RecipePicker:
                RecipePickerView(bloc: picker.bloc)
            case let date as MealPlannerRootBlocChild.ChooseDate:
                ChooseDateView(bloc: date.bloc)
            case let mealType as MealPlannerRootBlocChild.ChooseMealType:
                ChooseMealTypeView(bloc: mealType.bloc)
            default:
                ComposeScreenHost(screen: child.bloc)
            }
        }
        .toolbar {
            if isRoot {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: onClose) {
                        Label(ComposeStrings["close"], systemImage: "xmark")
                    }
                }
            }
        }
    }
}

// MARK: - Recipe picker

/// Searchable recipe list, mirroring `RecipePickerScreen`.
private struct RecipePickerView: View {
    let bloc: RecipePickerBloc
    @StateObject private var model: ObservedFlow<RecipePickerBlocModel>

    init(bloc: RecipePickerBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        Group {
            if state.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if state.recipes.isEmpty && !state.searchQuery.isEmpty {
                ContentUnavailableView.search(text: state.searchQuery)
            } else {
                List(state.recipes, id: \.id) { recipe in
                    Button { bloc.onRecipeSelected(item: recipe) } label: {
                        HStack(spacing: ChefTheme.Spacing.normal) {
                            MealThumbnail(url: recipe.imageUrl)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(recipe.title)
                                    .foregroundStyle(Color.primary)
                                if !recipe.bookNames.isEmpty {
                                    Text(recipe.bookNames.joined(separator: ", "))
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            Spacer(minLength: 0)
                            Image(systemName: "chevron.right")
                                .font(.footnote.weight(.semibold))
                                .foregroundStyle(.tertiary)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                .listStyle(.plain)
            }
        }
        .navigationTitle(ComposeStrings["add_meal_plan_select_recipe"])
        .navigationBarTitleDisplayMode(.inline)
        .searchable(
            text: Binding(get: { model.current.searchQuery }, set: { bloc.onSearchQueryChanged(query: $0) }),
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: ComposeStrings["add_meal_plan_search_recipes"]
        )
    }
}

// MARK: - Choose date

/// Month calendar to pick the day, with that day's planned meals below, mirroring
/// `ChooseDateScreen`. Regular width folds the meal-type step in (the Compose combined layout).
private struct ChooseDateView: View {
    let bloc: ChooseDateBloc
    @StateObject private var model: ObservedFlow<ChooseDateBlocModel>
    @SwiftUI.Environment(\.horizontalSizeClass) private var sizeClass

    init(bloc: ChooseDateBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    private var combined: Bool { sizeClass == .regular }

    var body: some View {
        let state = model.value
        List {
            if !state.familyPlans.isEmpty {
                Section {
                    PlanPickerRow(
                        families: state.familyPlans,
                        selectedFamilyId: state.selectedFamilyId,
                        onSelect: { bloc.onPlanSelected(familyId: $0) }
                    )
                }
            }

            Section {
                HStack {
                    Button(action: bloc.onPreviousMonth) {
                        Image(systemName: "chevron.left").frame(width: 44, height: 44)
                    }
                    Spacer()
                    Text(state.monthLabel)
                        .font(.headline)
                    Spacer()
                    Button(action: bloc.onNextMonth) {
                        Image(systemName: "chevron.right").frame(width: 44, height: 44)
                    }
                }
                .buttonStyle(.borderless)
                .listRowSeparator(.hidden)

                MealMonthCalendar(
                    firstDayOfMonth: state.firstDayOfMonth,
                    selectedKey: calendarKey(state.selectedDate),
                    daysWithMeals: state.daysWithMeals,
                    onSelect: { cell in
                        if let date = cell.date { bloc.onDaySelected(date: date) }
                    }
                )
                .padding(.vertical, ChefTheme.Spacing.small)

                if state.selectedDate != nil {
                    HStack {
                        Text(state.formattedSelectedDate)
                            .font(.body.weight(.medium))
                            .foregroundStyle(ChefTheme.primary)
                        Spacer()
                        Button(ComposeStrings["add_meal_plan_today"], action: bloc.onTodayClicked)
                            .buttonStyle(.borderless)
                    }
                }
            }

            if combined {
                Section {
                    MealTypePicker(selected: state.selectedMealType) { bloc.onMealTypeSelected(mealType: $0) }
                } header: {
                    Text(ComposeStrings["add_meal_plan_meal_type"])
                } footer: {
                    if !state.recipeTitle.isEmpty { Text(state.recipeTitle) }
                }
            }

            if state.selectedDate != nil {
                plannedMeals(state.selectedDayMeals)
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(ComposeStrings[combined ? "add_meal_plan_choose_date_and_meal_type" : "add_meal_plan_choose_date"])
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                if combined {
                    SaveButton(isSaving: state.isSaving, action: bloc.onSaveClicked)
                        .disabled(state.selectedDate == nil)
                } else {
                    Button(ComposeStrings["add_meal_plan_next"], action: bloc.onNextClicked)
                        .disabled(state.selectedDate == nil)
                }
            }
        }
    }

    /// The selected day's existing meals, grouped by type, so the user sees what's already planned.
    @ViewBuilder
    private func plannedMeals(_ meals: [MealPlanItem]) -> some View {
        if meals.isEmpty {
            Section {
                Text(ComposeStrings["add_meal_plan_no_meals"])
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
            }
        } else {
            ForEach(MealType.ordered, id: \.self) { type in
                let ofType = meals.filter { $0.mealType == type }
                if !ofType.isEmpty {
                    Section {
                        ForEach(ofType, id: \.id) { meal in
                            HStack(spacing: ChefTheme.Spacing.normal) {
                                MealThumbnail(url: meal.recipeImageUrl, size: 40)
                                Text(meal.recipeTitle)
                                    .font(.subheadline)
                            }
                        }
                    } header: {
                        MealSectionHeader(title: type.title)
                    }
                }
            }
        }
    }
}

/// "Add to" row with a menu of the personal and family plans.
private struct PlanPickerRow: View {
    let families: [FamilyPlan]
    let selectedFamilyId: String?
    let onSelect: (String?) -> Void

    var body: some View {
        LabeledContent(ComposeStrings["meal_plan_picker_add_to"]) {
            Menu {
                MealPlanMenuItems(families: families, selectedFamilyId: selectedFamilyId, onSelect: onSelect)
            } label: {
                HStack(spacing: 4) {
                    Text(mealPlanName(families: families, selectedFamilyId: selectedFamilyId))
                    Image(systemName: "chevron.up.chevron.down")
                        .font(.caption2.weight(.semibold))
                }
            }
            .accessibilityLabel(ComposeStrings["meal_plan_picker_selector"])
        }
    }
}

// MARK: - Choose meal type

/// The meal-type step, mirroring `ChooseMealTypeScreen`: the recipe and date for context, then a
/// checkmark list of meal types and Save.
private struct ChooseMealTypeView: View {
    let bloc: ChooseMealTypeBloc
    @StateObject private var model: ObservedFlow<ChooseMealTypeBlocModel>

    init(bloc: ChooseMealTypeBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        List {
            Section {
                MealTypePicker(selected: state.selectedMealType) { bloc.onMealTypeSelected(mealType: $0) }
            } header: {
                if !state.recipeTitle.isEmpty {
                    Text(state.recipeTitle)
                        .font(.title3.weight(.semibold))
                        .foregroundStyle(Color.primary)
                        .textCase(nil)
                    .padding(.bottom, ChefTheme.Spacing.small)
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(ComposeStrings["add_meal_plan_choose_meal_type"])
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                SaveButton(isSaving: state.isSaving, action: bloc.onSaveClicked)
            }
        }
    }
}

/// Meal types as a single-choice checkmark list (the Compose radio list).
private struct MealTypePicker: View {
    let selected: MealType
    let onSelect: (MealType) -> Void

    var body: some View {
        ForEach(MealType.ordered, id: \.self) { type in
            Button { onSelect(type) } label: {
                HStack {
                    Text(type.addMealTitle)
                        .foregroundStyle(Color.primary)
                    Spacer()
                    if type == selected {
                        Image(systemName: "checkmark")
                            .font(.body.weight(.semibold))
                            .foregroundStyle(ChefTheme.primary)
                    }
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(type == selected ? [.isButton, .isSelected] : .isButton)
        }
        .sensoryFeedback(.selection, trigger: selected)
    }
}

private struct SaveButton: View {
    let isSaving: Bool
    let action: () -> Void

    var body: some View {
        if isSaving {
            ProgressView()
        } else {
            Button(ComposeStrings["add_meal_plan_save"], action: action)
                .fontWeight(.semibold)
        }
    }
}

private extension MealType {
    /// Display name from the add-meal strings.
    var addMealTitle: String {
        switch self {
        case .breakfast: ComposeStrings["add_meal_plan_breakfast"]
        case .lunch: ComposeStrings["add_meal_plan_lunch"]
        case .snacks: ComposeStrings["add_meal_plan_snacks"]
        default: ComposeStrings["add_meal_plan_dinner"]
        }
    }
}

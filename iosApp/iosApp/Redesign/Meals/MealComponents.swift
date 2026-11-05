import SwiftUI
import ComposeApp

/// Pieces shared by the Meals tab and the add-meal flow: the month calendar, meal rows, and the
/// personal/family plan menu.

/// A month grid, Sunday first, laid out by Kotlin's `MonthCalendarGrid` so it matches the Compose
/// calendars. Days with meals get a dot; the selected day a filled circle.
struct MealMonthCalendar: View {
    let cells: [CalendarCell]
    let selectedKey: String?
    let daysWithMeals: Set<String>
    let onSelect: (CalendarCell) -> Void

    init(
        firstDayOfMonth: Kotlinx_datetimeLocalDate,
        selectedKey: String?,
        daysWithMeals: Set<String>,
        onSelect: @escaping (CalendarCell) -> Void
    ) {
        cells = MonthCalendarGrid.shared.cells(firstDayOfMonth: firstDayOfMonth)
        self.selectedKey = selectedKey
        self.daysWithMeals = daysWithMeals
        self.onSelect = onSelect
    }

    var body: some View {
        // A plain (non-lazy) grid: inside a List row, a LazyVGrid over-reports its height.
        Grid(horizontalSpacing: 0, verticalSpacing: ChefTheme.Spacing.extraSmall) {
            GridRow {
                ForEach(Array(MonthCalendarGrid.shared.weekdayInitials.enumerated()), id: \.offset) { _, initial in
                    Text(initial)
                        .font(.caption2.weight(.bold))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity)
                }
            }
            ForEach(0..<cells.count / 7, id: \.self) { week in
                GridRow {
                    ForEach(week * 7..<week * 7 + 7, id: \.self) { index in
                        let cell = cells[index]
                        if cell.date == nil {
                            Color.clear.frame(maxWidth: .infinity, maxHeight: 40)
                        } else {
                            DayCell(
                                day: Int(cell.day),
                                isSelected: cell.key == selectedKey,
                                hasMeals: daysWithMeals.contains(cell.key)
                            ) { onSelect(cell) }
                        }
                    }
                }
            }
        }
        .frame(maxWidth: 420)
        .frame(maxWidth: .infinity)
        .sensoryFeedback(.selection, trigger: selectedKey)
    }

    private struct DayCell: View {
        let day: Int
        let isSelected: Bool
        let hasMeals: Bool
        let action: () -> Void

        var body: some View {
            Button(action: action) {
                VStack(spacing: 2) {
                    Text("\(day)")
                        .font(.subheadline.weight(isSelected ? .semibold : .regular))
                        .monospacedDigit()
                        .foregroundStyle(isSelected ? Color.white : Color.primary)
                        .frame(width: 32, height: 32)
                        .background {
                            if isSelected { Circle().fill(ChefTheme.primary) }
                        }
                    Circle()
                        .fill(hasMeals ? ChefTheme.primary : .clear)
                        .frame(width: 4, height: 4)
                }
                .frame(maxWidth: .infinity, minHeight: 40)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityAddTraits(isSelected ? [.isButton, .isSelected] : .isButton)
        }
    }
}

/// A Kotlin `LocalDate`'s grid key (ISO date), for comparing against `CalendarCell.key`.
func calendarKey(_ date: Kotlinx_datetimeLocalDate?) -> String? {
    MonthCalendarGrid.shared.key(date: date)
}

/// Header row over a group of meals: a bold primary title, like the Compose `MealSectionHeader`.
struct MealSectionHeader<Trailing: View>: View {
    let title: String
    @ViewBuilder var trailing: Trailing

    var body: some View {
        HStack(spacing: ChefTheme.Spacing.normal) {
            Text(title)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(ChefTheme.primary)
                .textCase(nil)
            Spacer(minLength: 0)
            trailing
                .labelStyle(.iconOnly)
                .foregroundStyle(ChefTheme.primary)
        }
    }
}

extension MealSectionHeader where Trailing == EmptyView {
    init(title: String) {
        self.init(title: title) { EmptyView() }
    }
}

/// A recipe photo thumbnail with a fork-and-knife placeholder.
struct MealThumbnail: View {
    let url: String?
    var size: CGFloat = 48

    var body: some View {
        Color.clear
            .frame(width: size, height: size)
            .overlay {
                AsyncImage(url: url.flatMap(URL.init(string:))) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    ZStack {
                        Color(.tertiarySystemFill)
                        Image(systemName: "fork.knife")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: 8))
    }
}

/// A meal's local/remote sync state, like the Compose `SyncStatusIcon`.
struct MealSyncStatusIcon: View {
    /// The Kotlin `SyncStatus` entry's `name`. Several exported modules declare their own
    /// `SyncStatus`, so its Swift type name (`SyncStatus`, `SyncStatus_`, …) depends on export order.
    let status: String

    var body: some View {
        switch status {
        case "SYNCING":
            ProgressView()
                .controlSize(.mini)
                .accessibilityLabel(ComposeStrings["meal_plan_sync_syncing"])
        case "SYNCED":
            Image(systemName: "checkmark.icloud")
                .foregroundStyle(.secondary)
                .accessibilityLabel(ComposeStrings["meal_plan_sync_synced"])
        default:
            Image(systemName: "icloud.slash")
                .foregroundStyle(.secondary)
                .accessibilityLabel(ComposeStrings["meal_plan_sync_not_synced"])
        }
    }
}

extension MealType {
    /// Display name from the meal plan strings.
    var title: String {
        switch self {
        case .breakfast: ComposeStrings["meal_plan_breakfast"]
        case .lunch: ComposeStrings["meal_plan_lunch"]
        case .snacks: ComposeStrings["meal_plan_snacks"]
        default: ComposeStrings["meal_plan_dinner"]
        }
    }

    /// Meal types in day order.
    static var ordered: [MealType] { [.breakfast, .lunch, .dinner, .snacks] }
}

/// Menu listing the personal plan, then each family's shared plan, with a check on the selected
/// one — the native form of the Compose `MealPlanSelector` dropdown / bottom sheet.
struct MealPlanMenuItems: View {
    let families: [FamilyPlan]
    let selectedFamilyId: String?
    let onSelect: (String?) -> Void

    var body: some View {
        Button { onSelect(nil) } label: {
            Label(ComposeStrings["meal_plan_picker_personal_plan"], systemImage: selectedFamilyId == nil ? "checkmark" : "person")
        }
        Section(ComposeStrings["meal_plan_picker_family_plans"]) {
            ForEach(families, id: \.familyId) { family in
                Button { onSelect(family.familyId) } label: {
                    Label(family.name, systemImage: family.familyId == selectedFamilyId ? "checkmark" : "person.2")
                }
            }
        }
    }
}

/// The selected plan's display name: the family's name, or "My meals" for the personal plan.
func mealPlanName(families: [FamilyPlan], selectedFamilyId: String?) -> String {
    families.first { $0.familyId == selectedFamilyId }?.name ?? ComposeStrings["meal_plan_picker_personal_plan"]
}

import SwiftUI
import ComposeApp

/// The redesigned Feature Flags screen (Developer settings → Feature flags), mirroring the Compose
/// `FeatureFlagsScreen`: one section per registered flag with its key, description, resolved and
/// default values, and an override control — a Default / On / Off segmented picker for boolean
/// flags, a text field with Reset / Save for string flags. "Reset all overrides" sits in the
/// navigation bar. This is how `ios_swiftui_redesign` gets switched back off from inside the
/// redesign, so every override goes straight to the BLoC.
struct FeatureFlagsView: View {
    let bloc: FeatureFlagsBloc
    @StateObject private var model: ObservedFlow<FeatureFlagsBlocModel>

    init(bloc: FeatureFlagsBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let rows = model.value.rows
        List {
            if rows.isEmpty {
                Text(ComposeStrings["feature_flags_empty"])
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                ForEach(rows, id: \.flag.key) { row in
                    Section {
                        FlagHeader(row: row)
                        if let flag = row.flag as? BooleanFlag {
                            Picker(ComposeStrings["feature_flags_title"], selection: booleanSelection(flag)) {
                                Text(ComposeStrings["feature_flags_default"]).tag(BooleanChoice.default)
                                Text(ComposeStrings["feature_flags_on"]).tag(BooleanChoice.on)
                                Text(ComposeStrings["feature_flags_off"]).tag(BooleanChoice.off)
                            }
                            .pickerStyle(.segmented)
                            .labelsHidden()
                        } else if let flag = row.flag as? StringFlag {
                            StringFlagControls(
                                overrideText: row.override.stringOverride ?? "",
                                onSave: { bloc.onSetStringOverride(flag: flag, value: $0) },
                                onReset: { bloc.onClearOverride(flag: flag) }
                            )
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(ComposeStrings["feature_flags_title"])
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button(ComposeStrings["feature_flags_clear_all"], action: bloc.onClearAllOverrides)
                    .disabled(rows.isEmpty)
            }
        }
        .tint(ChefTheme.primary)
    }

    /// The segmented control's selection for `flag`, read from the live row so a tap never snaps
    /// back, and written straight to the BLoC.
    private func booleanSelection(_ flag: BooleanFlag) -> Binding<BooleanChoice> {
        Binding(
            get: {
                let row = model.current.rows.first { $0.flag.key == flag.key }
                return row.map { BooleanChoice($0.override) } ?? .default
            },
            set: { choice in
                let value: Override = switch choice {
                case .default: OverrideDefault.shared
                case .on: OverrideForceValue<KotlinBoolean>(value: KotlinBoolean(bool: true))
                case .off: OverrideForceValue<KotlinBoolean>(value: KotlinBoolean(bool: false))
                }
                bloc.onSetBooleanOverride(flag: flag, override: value)
            }
        )
    }
}

/// Key, description, and the resolved / default values for one flag.
private struct FlagHeader: View {
    let row: FeatureFlagsBlocRow

    var body: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
            Text(row.flag.key)
                .font(.headline.monospaced())
                .textSelection(.enabled)
            Text(row.flag.description_)
                .font(.footnote)
                .foregroundStyle(.secondary)
            Text(ComposeStrings["feature_flags_resolved", ["value": flagValueText(row.resolvedValue)]])
                .font(.subheadline)
                .padding(.top, ChefTheme.Spacing.extraSmall)
            Text(ComposeStrings["feature_flags_default_value", ["value": flagValueText(row.flag.defaultValue)]])
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, ChefTheme.Spacing.extraSmall)
    }
}

/// The string override editor: a text field seeded from the current override, plus Reset and
/// Save. The field follows the override when it changes underneath (Reset, Reset all).
private struct StringFlagControls: View {
    let overrideText: String
    let onSave: (String) -> Void
    let onReset: () -> Void
    @State private var input: String

    init(overrideText: String, onSave: @escaping (String) -> Void, onReset: @escaping () -> Void) {
        self.overrideText = overrideText
        self.onSave = onSave
        self.onReset = onReset
        _input = State(initialValue: overrideText)
    }

    var body: some View {
        TextField(ComposeStrings["feature_flags_string_placeholder"], text: $input)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
            .submitLabel(.done)
            .onSubmit { onSave(input) }
            .onChange(of: overrideText) { _, newValue in input = newValue }
        HStack {
            Spacer()
            Button(ComposeStrings["feature_flags_reset"], action: onReset)
                .buttonStyle(.borderless)
            Button(ComposeStrings["feature_flags_save"]) { onSave(input) }
                .buttonStyle(.borderless)
                .fontWeight(.semibold)
                .padding(.leading, ChefTheme.Spacing.normal)
        }
    }
}

/// The three states of a boolean override, as the segmented control's tags.
private enum BooleanChoice: Hashable {
    case `default`, on, off

    init(_ current: Override) {
        if let force = current as? OverrideForceValue<AnyObject> {
            self = (force.value as? KotlinBoolean)?.boolValue == true ? .on : .off
        } else {
            self = .default
        }
    }
}

private extension Override {
    /// The forced string, or nil for `Override.Default`.
    var stringOverride: String? {
        (self as? OverrideForceValue<AnyObject>)?.value as? String
    }
}

/// A flag value as Kotlin's `toString()` prints it (`true` / `false`, or the string itself).
private func flagValueText(_ value: Any) -> String {
    if let bool = value as? KotlinBoolean { return bool.boolValue ? "true" : "false" }
    if let string = value as? String { return string }
    return String(describing: value)
}

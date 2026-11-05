import SwiftUI
import ComposeApp

/// The redesigned shared-recipe preview, mirroring the Android `PublicRecipePreviewScreen`: a
/// read-only recipe opened from a share link, with a floating "Save to my recipes" button. The
/// source link opens in a Safari sheet.
struct PublicRecipeView: View {
    let bloc: PublicRecipeBloc
    @StateObject private var model: ObservedFlow<PublicRecipeBlocModel>
    @State private var webPage: WebPage?

    init(bloc: PublicRecipeBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        Group {
            if let loaded = state as? PublicRecipeBlocModelLoaded {
                LoadedRecipe(model: loaded, onSourceClicked: openSource)
                    .safeAreaInset(edge: .bottom) { saveButton(isSaving: loaded.isSaving) }
                    .toastClearance(64)
            } else if state is PublicRecipeBlocModelNotFound {
                StatusBlock(
                    systemImage: "eye.slash",
                    title: ComposeStrings["public_recipe_not_found_title"],
                    message: ComposeStrings["public_recipe_not_found_body"]
                )
            } else if state is PublicRecipeBlocModelOffline {
                StatusBlock(
                    systemImage: "wifi.slash",
                    title: ComposeStrings["public_recipe_offline_title"],
                    message: ComposeStrings["public_recipe_offline_body"]
                ) {
                    Button(ComposeStrings["public_recipe_retry"], action: bloc.onRetryClicked)
                        .buttonStyle(.bordered)
                }
            } else {
                ProgressView()
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .tint(ChefTheme.primary)
        .navigationTitle(ComposeStrings["public_recipe_title"])
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $webPage) { page in
            SafariView(url: page.url)
                .ignoresSafeArea()
        }
    }

    private func saveButton(isSaving: Bool) -> some View {
        Button(action: bloc.onSaveClicked) {
            HStack(spacing: ChefTheme.Spacing.small) {
                if isSaving {
                    ProgressView().tint(.white)
                    Text(ComposeStrings["public_recipe_saving"])
                } else {
                    Image(systemName: "square.and.arrow.down")
                    Text(ComposeStrings["public_recipe_save"])
                }
            }
            .font(.headline)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .disabled(isSaving)
        .padding(ChefTheme.Spacing.small)
        .chefGlass(in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .padding(.horizontal, ChefTheme.Spacing.normal)
        .padding(.bottom, ChefTheme.Spacing.small)
    }

    /// Web links open in a Safari sheet (the iOS convention); anything else goes to the BLoC.
    private func openSource(_ url: String) {
        if let parsed = URL(string: url), ["http", "https"].contains(parsed.scheme?.lowercased()) {
            webPage = WebPage(url: parsed)
        } else {
            bloc.onSourceUrlClicked(url: url)
        }
    }
}

private struct LoadedRecipe: View {
    let model: PublicRecipeBlocModelLoaded
    let onSourceClicked: (String) -> Void

    var body: some View {
        let recipe = model.recipe
        ScrollView {
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.normal) {
                if let imageUrl = recipe.imageUrl {
                    RecipeImage(url: imageUrl)
                        .frame(height: 220)
                        .frame(maxWidth: .infinity)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .accessibilityLabel(recipe.title)
                }
                Text(recipe.title)
                    .font(.title2.weight(.semibold))
                if let description = recipe.description_, !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(description)
                }
                DetailsCard(model: model, onSourceClicked: onSourceClicked)
                RecipeTextSection(title: ComposeStrings["public_recipe_ingredients"], text: recipe.ingredients)
                RecipeTextSection(title: ComposeStrings["public_recipe_directions"], text: recipe.directions)
            }
            .padding(ChefTheme.Spacing.normal)
            .frame(maxWidth: 720)
            .frame(maxWidth: .infinity)
        }
    }
}

/// Servings, prep / cook / total time, calories and the source link. Renders nothing when the
/// recipe has none of them.
private struct DetailsCard: View {
    let model: PublicRecipeBlocModelLoaded
    let onSourceClicked: (String) -> Void

    var body: some View {
        let recipe = model.recipe
        let rows: [(String, String, String)] = [
            recipe.servings.map { ("fork.knife", ComposeStrings["recipe_detail_servings"], "\($0)") },
            model.formattedPrepTime.map { ("timer", ComposeStrings["recipe_detail_prep_time"], $0.localized) },
            model.formattedCookTime.map { ("timer", ComposeStrings["recipe_detail_cook_time"], $0.localized) },
            model.formattedTotalTime.map { ("timer", ComposeStrings["recipe_detail_total_time"], $0.localized) },
            recipe.calories.map { ("flame", ComposeStrings["recipe_detail_calories"], ComposeStrings["recipe_detail_kcal", ["calories": "\($0)"]]) },
        ].compactMap { $0 }

        if !rows.isEmpty || recipe.sourceUrl != nil {
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
                Text(ComposeStrings["recipe_detail_details"])
                    .font(.headline)
                ForEach(rows, id: \.1) { icon, label, value in
                    HStack {
                        Label(label, systemImage: icon)
                            .foregroundStyle(.secondary)
                        Spacer()
                        Text(value)
                    }
                    .font(.subheadline)
                }
                if let sourceUrl = recipe.sourceUrl {
                    VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
                        Text(ComposeStrings["recipe_detail_source"])
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Button { onSourceClicked(sourceUrl) } label: {
                            Text(sourceUrl)
                                .font(.subheadline)
                                .underline()
                                .lineLimit(1)
                                .truncationMode(.middle)
                        }
                        .buttonStyle(.plain)
                        .foregroundStyle(ChefTheme.primary)
                    }
                }
            }
            .padding(ChefTheme.Spacing.normal)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
    }
}

private struct RecipeTextSection: View {
    let title: String
    let text: String

    var body: some View {
        if !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
                Text(title).font(.headline)
                Text(text)
            }
        }
    }
}

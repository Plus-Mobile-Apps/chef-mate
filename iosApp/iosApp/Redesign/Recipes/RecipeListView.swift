import SwiftUI
import ComposeApp

/// The redesigned Recipes tab, mirroring the Android `RecipeListScreen`: searchable list or grid of
/// recipe cards, a recipe-book picker, and the continue-cooking button.
///
/// Not yet ported (docs/ios-swiftui-redesign.md, PR 3): selection mode and bulk actions, the
/// sort/filter sheet, scan from photo, invite banner, export, and coach marks.
struct RecipeListView: View {
    let bloc: RecipeListBloc
    @StateObject private var model: ObservedFlow<RecipeListBlocModel>

    init(bloc: RecipeListBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        content(state)
            .navigationTitle(state.isAllRecipesSelected ? "All recipes" : (state.activeBook?.name ?? "Recipes"))
            .searchable(
                text: Binding(get: { model.current.searchQuery }, set: { bloc.onSearchQueryChanged(query: $0) }),
                prompt: "Search recipes…"
            )
            .refreshable { bloc.onSyncClicked() }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    BookMenu(state: state, bloc: bloc)
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button {
                        bloc.onToggleViewMode()
                    } label: {
                        Image(systemName: state.isGridView ? "list.bullet" : "square.grid.2x2")
                    }
                    Button(action: bloc.onAddRecipeClicked) {
                        Image(systemName: "plus")
                    }
                }
            }
            .overlay(alignment: .bottom) {
                if state.cookingRecipeCount > 0 {
                    Button(action: bloc.onContinueCookingClicked) {
                        Label("Continue cooking", systemImage: "frying.pan")
                            .font(.headline)
                            .padding(.horizontal, ChefTheme.Spacing.large)
                            .padding(.vertical, ChefTheme.Spacing.normal - 2)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(ChefTheme.primary)
                    .chefGlass()
                    .padding(.bottom, ChefTheme.Spacing.normal)
                }
            }
            .tint(ChefTheme.primary)
    }

    @ViewBuilder
    private func content(_ state: RecipeListBlocModel) -> some View {
        if state.recipes.isEmpty && state.isLoading {
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if state.recipes.isEmpty && state.searchQuery.isEmpty && state.totalRecipeCount == 0 {
            EmptyRecipesView(onBrowse: bloc.onBrowseRecipesClicked, onCreate: bloc.onAddRecipeClicked)
        } else if state.recipes.isEmpty {
            ContentUnavailableView.search(text: state.searchQuery)
        } else if state.isGridView {
            ScrollView {
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 160), spacing: ChefTheme.Spacing.normal)],
                          spacing: ChefTheme.Spacing.normal) {
                    ForEach(state.recipes, id: \.id) { recipe in
                        Button { bloc.onRecipeClicked(recipe: recipe) } label: {
                            RecipeGridCard(recipe: recipe)
                        }
                        .buttonStyle(.plain)
                        .contextMenu { recipeActions(recipe) }
                    }
                }
                .padding(ChefTheme.Spacing.normal)
            }
        } else {
            List(state.recipes, id: \.id) { recipe in
                Button { bloc.onRecipeClicked(recipe: recipe) } label: {
                    RecipeRow(recipe: recipe)
                }
                .buttonStyle(.plain)
                .contextMenu { recipeActions(recipe) }
                .swipeActions(edge: .trailing) {
                    Button(role: .destructive) { bloc.onDeleteRecipe(recipe: recipe) } label: {
                        Label("Delete", systemImage: "trash")
                    }
                }
                .swipeActions(edge: .leading) {
                    Button { bloc.onToggleFavorite(recipe: recipe) } label: {
                        Label(recipe.isFavorite ? "Unfavorite" : "Favorite",
                              systemImage: recipe.isFavorite ? "heart.slash" : "heart")
                    }
                    .tint(.pink)
                }
            }
            .listStyle(.plain)
        }
    }

    @ViewBuilder
    private func recipeActions(_ recipe: RecipeListItem) -> some View {
        Button { bloc.onToggleFavorite(recipe: recipe) } label: {
            Label(recipe.isFavorite ? "Unfavorite" : "Favorite",
                  systemImage: recipe.isFavorite ? "heart.slash" : "heart")
        }
        Button(role: .destructive) { bloc.onDeleteRecipe(recipe: recipe) } label: {
            Label("Delete", systemImage: "trash")
        }
    }
}

/// Toolbar menu to switch recipe books, like the Android book selector.
private struct BookMenu: View {
    let state: RecipeListBlocModel
    let bloc: RecipeListBloc

    var body: some View {
        Menu {
            Button {
                bloc.onAllRecipesSelected()
            } label: {
                if state.isAllRecipesSelected {
                    Label("All recipes", systemImage: "checkmark")
                } else {
                    Text("All recipes")
                }
            }
            Divider()
            ForEach(state.recipeBooks, id: \.id) { book in
                Button {
                    bloc.onBookSelected(bookId: book.id)
                } label: {
                    if !state.isAllRecipesSelected && state.activeBook?.id == book.id {
                        Label(book.name, systemImage: "checkmark")
                    } else {
                        Text(book.name)
                    }
                }
            }
            Divider()
            Button(action: bloc.onCreateBookClicked) {
                Label("New recipe book", systemImage: "plus")
            }
        } label: {
            Image(systemName: "books.vertical")
        }
    }
}

private struct RecipeRow: View {
    let recipe: RecipeListItem

    var body: some View {
        HStack(spacing: ChefTheme.Spacing.normal) {
            Color.clear
                .frame(width: 80, height: 80)
                .overlay { RecipeImage(url: recipe.imageUrl) }
                .clipShape(RoundedRectangle(cornerRadius: 12))
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
                HStack(spacing: ChefTheme.Spacing.small) {
                    Text(recipe.title)
                        .font(.headline)
                        .lineLimit(1)
                    Spacer(minLength: 0)
                    if recipe.isFavorite {
                        Image(systemName: "heart.fill")
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
                if let description = recipe.description_, !description.isEmpty {
                    Text(description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                StarRating(rating: recipe.starRating?.intValue)
                RecipeInfo(recipe: recipe)
            }
        }
        .padding(.vertical, ChefTheme.Spacing.extraSmall)
        .contentShape(Rectangle())
    }
}

private struct RecipeGridCard: View {
    let recipe: RecipeListItem

    var body: some View {
        VStack(alignment: .leading, spacing: ChefTheme.Spacing.small) {
            // A fixed-size box with the photo center-cropped into it, so the image's own aspect
            // ratio can never widen or heighten the card.
            Color.clear
                .frame(maxWidth: .infinity)
                .frame(height: 120)
                .overlay { RecipeImage(url: recipe.imageUrl) }
                .clipped()
            VStack(alignment: .leading, spacing: ChefTheme.Spacing.extraSmall) {
                HStack(alignment: .top) {
                    Text(recipe.title)
                        .font(.headline)
                        .lineLimit(2, reservesSpace: true)
                    Spacer(minLength: 0)
                    if recipe.isFavorite {
                        Image(systemName: "heart.fill")
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
                StarRating(rating: recipe.starRating?.intValue)
                RecipeInfo(recipe: recipe)
            }
            .padding([.horizontal, .bottom], ChefTheme.Spacing.small)
        }
        // Fill the grid row so cards beside each other match heights even when one has a rating
        // or more info than the other.
        .frame(maxHeight: .infinity, alignment: .top)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

private struct RecipeImage: View {
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

private struct StarRating: View {
    let rating: Int?

    var body: some View {
        if let rating, rating > 0 {
            HStack(spacing: 2) {
                ForEach(1...5, id: \.self) { index in
                    Image(systemName: index <= rating ? "star.fill" : "star")
                }
            }
            .font(.caption)
            .foregroundStyle(.orange)
        }
    }
}

private struct RecipeInfo: View {
    let recipe: RecipeListItem

    var body: some View {
        // Wraps instead of overflowing, so a narrow grid card never grows wider than its column.
        FlowLayout(spacing: ChefTheme.Spacing.small + 4) {
            if let time = recipe.formattedTotalTime {
                Label { LocalizedText(text: time) } icon: { Image(systemName: "clock") }
            }
            if let servings = recipe.servings {
                Label("\(servings.intValue) servings", systemImage: "fork.knife")
            }
            if let calories = recipe.calories {
                Label("\(calories.intValue) cal", systemImage: "flame")
            }
        }
        .font(.caption)
        .foregroundStyle(.secondary)
        .labelStyle(CompactLabelStyle())
    }
}

private struct CompactLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: 4) {
            configuration.icon
            configuration.title
        }
    }
}

private struct EmptyRecipesView: View {
    let onBrowse: () -> Void
    let onCreate: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label("No recipes yet", systemImage: "fork.knife")
        } description: {
            Text("Get started by finding recipes online or creating one from scratch.")
        } actions: {
            Button(action: onBrowse) {
                Label("Find Recipes Online", systemImage: "magnifyingglass")
            }
            .buttonStyle(.borderedProminent)
            Button(action: onCreate) {
                Label("Create New Recipe", systemImage: "plus")
            }
            .buttonStyle(.bordered)
        }
        .tint(ChefTheme.primary)
    }
}

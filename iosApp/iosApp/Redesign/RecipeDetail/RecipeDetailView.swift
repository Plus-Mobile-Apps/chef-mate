import SwiftUI
import SafariServices
import ComposeApp

/// The redesigned recipe detail, mirroring the Android `RecipeDetailScreen` compact layout: a hero
/// (photo beside the key details), categories, description, then ingredients and directions under
/// pinned section headers in one scroll. Actions sit in a native bottom toolbar (Liquid Glass on
/// iOS 26); share, edit and delete in the navigation bar. The photo opens full screen with a zoom
/// transition.
///
/// Not yet ported (docs/ios-swiftui-redesign.md, PR 5): coach marks, and the tablet two-column
/// layout. The add-to-grocery sheet still hosts its Compose screen until PR 7.
struct RecipeDetailView: View {
    let bloc: RecipeDetailBloc
    @StateObject private var model: ObservedFlow<RecipeDetailBlocModel>
    @StateObject private var sheet: ObservedValue<ChildSlot<AnyObject, RecipeDetailBlocSheet>>
    @StateObject private var fullImage: ObservedValue<ChildSlot<AnyObject, RecipeDetailBlocFullImage>>
    @State private var shareWatch: Cancellable?
    /// A web page shown in the Safari sheet (source link or a link inside the recipe text).
    @State private var webPage: WebPage?
    @Namespace private var imageNamespace

    init(bloc: RecipeDetailBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
        _sheet = StateObject(wrappedValue: ObservedValue(bloc.childSlot))
        _fullImage = StateObject(wrappedValue: ObservedValue(bloc.fullImageSlot))
    }

    var body: some View {
        let state = model.value
        let recipe = state.recipe
        Group {
            if state.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                RecipeDetailContent(
                    state: state,
                    imageNamespace: imageNamespace,
                    onImageClicked: bloc.onImageClicked,
                    onSourceClicked: { url in URL(string: url).map(handleLink) },
                    onScaleChanged: { bloc.onScaleChanged(scale: $0) }
                )
            }
        }
        .environment(\.openURL, OpenURLAction { url in
            handleLink(url)
            return .handled
        })
        .navigationTitle(recipe.title)
        .navigationBarTitleDisplayMode(.large)
        .toolbar { navigationActions(recipe) }
        .safeAreaInset(edge: .bottom) {
            if !state.isLoading { actionBar(state) }
        }
        // Keep toasts (e.g. "added to grocery list") above the action bar instead of over it.
        .toastClearance(64)
        .tint(ChefTheme.primary)
        .alert(
            ComposeStrings["recipe_detail_delete_title"],
            isPresented: alertBinding(\.showDeleteConfirmationDialog, dismiss: bloc.onDeleteDismissed)
        ) {
            Button(ComposeStrings["recipe_detail_delete_confirm"], role: .destructive, action: bloc.onDeleteConfirmed)
            Button(ComposeStrings["recipe_detail_delete_cancel"], role: .cancel, action: bloc.onDeleteDismissed)
        } message: {
            Text(ComposeStrings["recipe_detail_delete_message"].replacingOccurrences(of: "{recipe_name}", with: recipe.title))
        }
        .alert(
            ComposeStrings["recipe_detail_share_confirm_title"],
            isPresented: alertBinding(\.showShareConfirmation, dismiss: bloc.onShareDismissed)
        ) {
            Button(ComposeStrings["recipe_detail_share_confirm_button"], action: bloc.onShareConfirmed)
            Button(ComposeStrings["recipe_detail_share_cancel"], role: .cancel, action: bloc.onShareDismissed)
        } message: {
            Text(ComposeStrings["recipe_detail_share_confirm_body"])
        }
        .alert(
            ComposeStrings["premium_required_title"],
            isPresented: alertBinding(\.showPremiumRequiredDialog, dismiss: bloc.onPremiumRequiredDismissed)
        ) {
            Button(ComposeStrings["premium_required_confirm"], role: .cancel, action: bloc.onPremiumRequiredDismissed)
        } message: {
            Text(ComposeStrings["premium_required_message"])
        }
        .overlay {
            if state.isDeleting {
                DeletingOverlay()
            }
        }
        .sheet(isPresented: sheetBinding) {
            if let addToGrocery = sheet.value.child?.instance as? RecipeDetailBlocSheet.AddToGroceryList {
                ComposeScreenHost(screen: addToGrocery.bloc)
                    .presentationDragIndicator(.visible)
            }
        }
        .fullScreenCover(isPresented: fullImageBinding) {
            if let active = fullImage.value.child?.instance as? RecipeDetailBlocFullImage.Active {
                RecipeImageFullScreenView(imageUrl: active.imageUrl, title: active.title, onDismiss: bloc.onCloseFullImage)
                    .navigationTransition(.zoom(sourceID: RecipeHero.imageTransitionId, in: imageNamespace))
            }
        }
        .sheet(item: $webPage) { page in
            SafariView(url: page.url)
                .ignoresSafeArea()
        }
        .onAppear {
            // Share links arrive once the recipe has been made public; hand each to the share sheet.
            shareWatch = shareWatch ?? EventWatcher<NSString>(flow: bloc.shareLink).watch { url in
                SharePresenter.share(url as String)
            }
        }
        .onDisappear {
            shareWatch?.cancel()
            shareWatch = nil
        }
    }

    // MARK: Toolbars

    @ToolbarContentBuilder
    private func navigationActions(_ recipe: Recipe) -> some ToolbarContent {
        ToolbarItemGroup(placement: .topBarTrailing) {
            Menu {
                Button { bloc.onShareLinkClicked() } label: {
                    Label(ComposeStrings["recipe_detail_share_link"], systemImage: "link")
                }
                if let sourceUrl = recipe.sourceUrl {
                    Button { SharePresenter.share(sourceUrl) } label: {
                        Label(ComposeStrings["recipe_detail_share_url"], systemImage: "safari")
                    }
                }
                Button { SharePresenter.share(RecipeTextFormatter.shared.shareText(recipe: recipe)) } label: {
                    Label(ComposeStrings["recipe_detail_share_text"], systemImage: "doc.plaintext")
                }
                if recipe.isPublic {
                    Divider()
                    Button(role: .destructive) { bloc.onStopSharingClicked() } label: {
                        Label(ComposeStrings["recipe_detail_stop_sharing"], systemImage: "link.badge.minus")
                    }
                }
            } label: {
                Label(ComposeStrings["recipe_detail_share"], systemImage: "square.and.arrow.up")
            }
            Menu {
                Button { bloc.onEditClicked() } label: {
                    Label(ComposeStrings["recipe_detail_edit"], systemImage: "pencil")
                }
                Button(role: .destructive) { bloc.onDeleteClicked() } label: {
                    Label(ComposeStrings["recipe_detail_delete"], systemImage: "trash")
                }
            } label: {
                Label(ComposeStrings["recipe_detail_edit"], systemImage: "ellipsis")
            }
        }
    }

    /// The recipe actions as a floating glass bar above the tab bar — the Android `PlusToolbar`
    /// with its Cook mode FAB. A `.bottomBar` toolbar can't be used: UIKit puts it in the tab
    /// bar's slot, hidden behind it while the tab bar shows.
    private func actionBar(_ state: RecipeDetailBlocModel) -> some View {
        HStack(spacing: ChefTheme.Spacing.normal) {
            HStack(spacing: ChefTheme.Spacing.extraSmall) {
                ActionButton(ComposeStrings["recipe_detail_add_to_grocery"], systemImage: "cart.badge.plus") {
                    bloc.onAddToGroceryListClicked()
                }
                ActionButton(
                    ComposeStrings[state.recipe.isFavorite ? "recipe_detail_remove_favorite" : "recipe_detail_add_favorite"],
                    systemImage: state.recipe.isFavorite ? "heart.fill" : "heart"
                ) {
                    bloc.onFavoriteToggled()
                }
                ActionButton(ComposeStrings["recipe_detail_add_to_meal_plan"], systemImage: "calendar.badge.plus") {
                    bloc.onAddToMealPlanClicked()
                }
                if state.showAiChat {
                    // Still tappable for a non-subscriber: the BLoC opens the premium upsell instead.
                    ActionButton(
                        ComposeStrings[state.isSubscribed ? "recipe_detail_ai_chat" : "recipe_detail_ai_chat_premium"],
                        systemImage: state.isSubscribed ? "sparkles" : "lock.fill"
                    ) {
                        bloc.onAiChatClicked()
                    }
                }
            }
            .padding(.horizontal, ChefTheme.Spacing.small)
            .chefGlass()

            Spacer(minLength: 0)

            Button { bloc.onCookModeClicked() } label: {
                Image(systemName: "frying.pan.fill")
                    .font(.title3)
                    .foregroundStyle(.white)
                    .frame(width: 56, height: 56)
                    .background(ChefTheme.primary, in: Circle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(ComposeStrings["recipe_detail_cook_mode"])
        }
        .padding(.horizontal, ChefTheme.Spacing.normal)
        .padding(.bottom, ChefTheme.Spacing.small)
    }

    // MARK: Bindings

    /// An alert's visibility, driven by a model flag. Dismissing it (including after a button tap)
    /// only notifies the BLoC if the flag is still set, so a confirm isn't followed by a dismiss.
    private func alertBinding(_ flag: KeyPath<RecipeDetailBlocModel, Bool>, dismiss: @escaping () -> Void) -> Binding<Bool> {
        Binding(
            get: { model.value[keyPath: flag] },
            set: { shown in
                if !shown && model.current[keyPath: flag] { dismiss() }
            }
        )
    }

    private var sheetBinding: Binding<Bool> {
        Binding(
            get: { sheet.value.child != nil },
            set: { shown in
                // A swipe-down dismisses natively; tell the sheet's BLoC so the slot closes too.
                if !shown, let addToGrocery = sheet.value.child?.instance as? RecipeDetailBlocSheet.AddToGroceryList {
                    addToGrocery.bloc.onBackClicked()
                }
            }
        )
    }

    private var fullImageBinding: Binding<Bool> {
        Binding(
            get: { fullImage.value.child != nil },
            set: { shown in
                if !shown && fullImage.value.child != nil { bloc.onCloseFullImage() }
            }
        )
    }

    /// A `chefmate://recipe/<clientId>` link opens that recipe in-app; a web link opens in a Safari
    /// sheet over the recipe (the iOS convention, instead of the Android in-app browser screen);
    /// anything else goes to the BLoC like before.
    private func handleLink(_ url: URL) {
        if let clientId = ChefMateUrls.shared.recipeLinkClientId(url: url.absoluteString) {
            bloc.onRecipeLinkClicked(clientId: clientId)
        } else if ["http", "https"].contains(url.scheme?.lowercased()) {
            webPage = WebPage(url: url)
        } else {
            bloc.onSourceUrlClicked(url: url.absoluteString)
        }
    }
}

/// The scrolling body: hero, categories, description, ingredients, directions.
private struct RecipeDetailContent: View {
    let state: RecipeDetailBlocModel
    let imageNamespace: Namespace.ID
    let onImageClicked: () -> Void
    let onSourceClicked: (String) -> Void
    let onScaleChanged: (Double) -> Void

    /// Ingredient lines crossed off while cooking — screen-local, like on Android.
    @State private var crossedOut = Set<Int>()
    @State private var highlightedStep: Int?

    var body: some View {
        let recipe = state.recipe
        let formatter = RecipeTextFormatter.shared
        let ingredients = formatter.ingredientLines(recipe: recipe, scale: state.ingredientScale)
        let directions = formatter.directionLines(recipe: recipe)
        let categories = formatter.categoryLabels(recipe: recipe, lookup: ComposeStrings.shared)

        ScrollView {
            LazyVStack(alignment: .leading, spacing: ChefTheme.Spacing.small, pinnedViews: [.sectionHeaders]) {
                VStack(alignment: .leading, spacing: ChefTheme.Spacing.normal) {
                    RecipeHero(
                        state: state,
                        imageNamespace: imageNamespace,
                        onImageClicked: onImageClicked,
                        onSourceClicked: onSourceClicked
                    )
                    if !categories.isEmpty {
                        FlowLayout {
                            ForEach(categories, id: \.self) { label in
                                Text(label)
                                    .font(.caption.weight(.medium))
                                    .padding(.horizontal, 10)
                                    .padding(.vertical, 4)
                                    .background(ChefTheme.primary.opacity(0.12), in: RoundedRectangle(cornerRadius: 8))
                            }
                        }
                    }
                    if let description = recipe.description_, !description.isEmpty {
                        Text(formatter.inlineRuns(text: description).attributed)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.horizontal, ChefTheme.Spacing.normal)
                .padding(.bottom, ChefTheme.Spacing.small)

                Section {
                    ForEach(Array(ingredients.enumerated()), id: \.offset) { index, line in
                        IngredientLine(line: line, isCrossedOut: crossedOut.contains(index)) {
                            if crossedOut.contains(index) { crossedOut.remove(index) } else { crossedOut.insert(index) }
                        }
                    }
                } header: {
                    SectionHeader(title: ComposeStrings["recipe_detail_ingredients"]) {
                        ScaleMenu(scale: state.ingredientScale, onScaleChanged: onScaleChanged)
                    }
                }

                Section {
                    ForEach(Array(directions.enumerated()), id: \.offset) { index, line in
                        DirectionLine(line: line, isHighlighted: highlightedStep == index) {
                            highlightedStep = highlightedStep == index ? nil : index
                        }
                    }
                } header: {
                    SectionHeader(title: ComposeStrings["recipe_detail_directions"]) { EmptyView() }
                }
            }
            .padding(.bottom, ChefTheme.Spacing.large)
        }
        // A different recipe (e.g. via a recipe link) starts with a clean slate.
        .onChange(of: recipe.id) {
            crossedOut = []
            highlightedStep = nil
        }
    }
}

/// Photo on the left, key details stacked on the right — the Android compact hero.
private struct RecipeHero: View {
    static let imageTransitionId = "recipe-detail-image"

    let state: RecipeDetailBlocModel
    let imageNamespace: Namespace.ID
    let onImageClicked: () -> Void
    let onSourceClicked: (String) -> Void

    var body: some View {
        let recipe = state.recipe
        HStack(alignment: .top, spacing: 12) {
            Button(action: onImageClicked) {
                AsyncImage(url: recipe.imageUrl.flatMap(URL.init(string:))) { image in
                    image.resizable().scaledToFill()
                } placeholder: {
                    ZStack {
                        Color(.tertiarySystemFill)
                        Image(systemName: "fork.knife")
                            .font(.title)
                            .foregroundStyle(.secondary)
                    }
                }
                .frame(width: 140, height: 140)
                .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                .matchedTransitionSource(id: Self.imageTransitionId, in: imageNamespace)
            }
            .buttonStyle(.plain)
            .disabled(recipe.imageUrl == nil)
            .accessibilityLabel(recipe.title)

            VStack(alignment: .leading, spacing: 6) {
                if let rating = recipe.starRating?.intValue, rating > 0 {
                    HStack(spacing: 2) {
                        ForEach(1...5, id: \.self) { index in
                            Image(systemName: index <= rating ? "star.fill" : "star")
                                .foregroundStyle(index <= rating ? ChefTheme.primary : Color.secondary.opacity(0.5))
                        }
                    }
                    .font(.subheadline)
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("\(rating)/5")
                }
                if let servings = recipe.servings?.intValue {
                    DetailRow(label: ComposeStrings["recipe_detail_servings"], value: "\(servings)", systemImage: "fork.knife")
                }
                if let prep = state.formattedPrepTime {
                    DetailRow(label: ComposeStrings["recipe_detail_prep_time"], value: prep.localized, systemImage: "timer")
                }
                if let cook = state.formattedCookTime {
                    DetailRow(label: ComposeStrings["recipe_detail_cook_time"], value: cook.localized, systemImage: "timer")
                }
                if let total = state.formattedTotalTime {
                    DetailRow(label: ComposeStrings["recipe_detail_total_time"], value: total.localized, systemImage: "timer")
                }
                if let calories = recipe.calories?.intValue {
                    DetailRow(
                        label: ComposeStrings["recipe_detail_calories"],
                        value: ComposeStrings["recipe_detail_kcal"].replacingOccurrences(of: "{calories}", with: "\(calories)"),
                        systemImage: "flame"
                    )
                }
                if let sourceUrl = recipe.sourceUrl {
                    Button { onSourceClicked(sourceUrl) } label: {
                        Text(sourceUrl)
                            .font(.caption)
                            .underline()
                            .lineLimit(1)
                            .truncationMode(.middle)
                    }
                    .buttonStyle(.plain)
                    .foregroundStyle(ChefTheme.primary)
                }
                Group {
                    Text("\(ComposeStrings["recipe_detail_created"]) \(state.createdAt.localized)")
                    Text("\(ComposeStrings["recipe_detail_updated"]) \(state.updatedAt.localized)")
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
            }
        }
    }
}

private struct DetailRow: View {
    let label: String
    let value: String
    let systemImage: String

    var body: some View {
        HStack {
            Label(label, systemImage: systemImage)
                .foregroundStyle(.secondary)
                .labelStyle(DetailLabelStyle())
            Spacer(minLength: ChefTheme.Spacing.small)
            Text(value)
        }
        .font(.subheadline)
        .accessibilityElement(children: .combine)
    }
}

private struct DetailLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: ChefTheme.Spacing.small) {
            configuration.icon.frame(width: 18)
            configuration.title
        }
    }
}

/// A pinned section header on the bar material, so lines scroll under it.
private struct SectionHeader<Accessory: View>: View {
    let title: String
    @ViewBuilder let accessory: () -> Accessory

    var body: some View {
        HStack {
            Text(title)
                .font(.title3.weight(.semibold))
                .accessibilityAddTraits(.isHeader)
            Spacer()
            accessory()
        }
        .padding(.horizontal, ChefTheme.Spacing.normal)
        .frame(minHeight: ChefTheme.Spacing.rowHeight - 8)
        .background(.bar)
    }
}

/// The ingredient scale menu (½× through 4×), persisted per recipe by the BLoC.
private struct ScaleMenu: View {
    let scale: Double
    let onScaleChanged: (Double) -> Void

    var body: some View {
        let formatter = RecipeTextFormatter.shared
        Menu {
            Picker(ComposeStrings["recipe_scale_ingredients"], selection: Binding(get: { scale }, set: onScaleChanged)) {
                ForEach(formatter.scaleOptions.map(\.doubleValue), id: \.self) { option in
                    Text(formatter.scaleLabel(factor: option)).tag(option)
                }
            }
        } label: {
            HStack(spacing: 2) {
                Text(formatter.scaleLabel(factor: scale))
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption2)
            }
            .font(.subheadline.weight(.semibold))
        }
        .accessibilityLabel(ComposeStrings["recipe_scale_ingredients"])
        .accessibilityValue(formatter.scaleLabel(factor: scale))
    }
}

private struct IngredientLine: View {
    let line: RecipeTextLine
    let isCrossedOut: Bool
    let onTap: () -> Void

    var body: some View {
        if line.isHeader {
            SubsectionHeader(line: line)
        } else {
            Text(line.attributed)
                .strikethrough(isCrossedOut)
                .foregroundStyle(isCrossedOut ? .secondary : .primary)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.vertical, ChefTheme.Spacing.extraSmall)
                .padding(.horizontal, ChefTheme.Spacing.normal)
                .contentShape(Rectangle())
                .onTapGesture(perform: onTap)
                .accessibilityAddTraits(.isButton)
                .accessibilityValue(isCrossedOut ? "Crossed off" : "")
        }
    }
}

private struct DirectionLine: View {
    let line: RecipeTextLine
    let isHighlighted: Bool
    let onTap: () -> Void

    var body: some View {
        if line.isHeader {
            SubsectionHeader(line: line)
        } else {
            Text(line.attributed)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(ChefTheme.Spacing.extraSmall)
                .background(
                    isHighlighted ? Color(.secondarySystemFill) : .clear,
                    in: RoundedRectangle(cornerRadius: ChefTheme.Spacing.small)
                )
                .padding(.horizontal, ChefTheme.Spacing.normal - ChefTheme.Spacing.extraSmall)
                .contentShape(Rectangle())
                .onTapGesture(perform: onTap)
                .accessibilityAddTraits(isHighlighted ? [.isButton, .isSelected] : .isButton)
        }
    }
}

/// "For the sauce:" — bold, not tappable.
private struct SubsectionHeader: View {
    let line: RecipeTextLine

    var body: some View {
        Text(line.attributed)
            .font(.subheadline.weight(.bold))
            .padding(.top, ChefTheme.Spacing.small)
            .padding(.bottom, ChefTheme.Spacing.extraSmall)
            .padding(.horizontal, ChefTheme.Spacing.normal)
    }
}

private struct DeletingOverlay: View {
    var body: some View {
        ZStack {
            Color.black.opacity(0.2).ignoresSafeArea()
            VStack(spacing: ChefTheme.Spacing.normal) {
                ProgressView()
                Text(ComposeStrings["recipe_detail_deleting_title"])
                    .font(.headline)
                Text(ComposeStrings["recipe_detail_deleting_message"])
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(ChefTheme.Spacing.large)
            .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        }
    }
}

struct WebPage: Identifiable {
    let url: URL
    var id: URL { url }
}

/// `SFSafariViewController` for SwiftUI: Safari's reader, sign-ins and share sheet, presented as a
/// sheet over the screen.
struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        let controller = SFSafariViewController(url: url)
        controller.preferredControlTintColor = UIColor(ChefTheme.primary)
        return controller
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}

private struct ActionButton: View {
    let title: String
    let systemImage: String
    let action: () -> Void

    init(_ title: String, systemImage: String, action: @escaping () -> Void) {
        self.title = title
        self.systemImage = systemImage
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.title3)
                .frame(width: 48, height: 48)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .foregroundStyle(ChefTheme.primary)
        .accessibilityLabel(title)
    }
}

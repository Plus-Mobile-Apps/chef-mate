import SwiftUI
import PhotosUI
import ComposeApp

/// The redesigned recipe editor, mirroring the Android `EditRecipeScreen`: title, recipe books,
/// categories, rating, photo, a collapsible "More details" group, then ingredients and directions.
/// Every field reads and writes the BLoC's own `StateFlow`s, so drafts, dirty tracking and saving
/// stay in Kotlin.
///
/// Back always goes through the BLoC (the discard-changes prompt), so the system back button is
/// replaced and the edge swipe is disabled while editing.
///
/// Not yet ported (docs/ios-swiftui-redesign.md, PR 6): the WYSIWYG rich-text mode (iOS edits the
/// markdown with a preview), coach marks, and the wide two-pane layout. The recipe-link picker is
/// still the hosted Compose screen.
struct EditRecipeView: View {
    let bloc: EditRecipeBloc
    @StateObject private var model: ObservedFlow<EditRecipeBlocModel>
    @StateObject private var title: ObservedFlow<NSString>
    @StateObject private var descriptionText: ObservedFlow<NSString>
    @StateObject private var imageUrl: ObservedFlow<NSString>
    @StateObject private var ingredients: ObservedFlow<NSString>
    @StateObject private var directions: ObservedFlow<NSString>
    @StateObject private var sourceUrl: ObservedFlow<NSString>
    @StateObject private var servings: ObservedFlow<NSString>
    @StateObject private var prepTime: ObservedFlow<NSString>
    @StateObject private var cookTime: ObservedFlow<NSString>
    @StateObject private var totalTime: ObservedFlow<NSString>
    @StateObject private var calories: ObservedFlow<NSString>
    @StateObject private var starRating: ObservedOptionalFlow<KotlinInt>
    @StateObject private var categories: ObservedFlow<NSSet>
    @StateObject private var userCategories: ObservedFlow<NSArray>
    @StateObject private var books: ObservedFlow<NSArray>
    @StateObject private var selectedBookIds: ObservedFlow<NSSet>
    @StateObject private var pendingPhoto: ObservedOptionalFlow<KotlinByteArray>
    @StateObject private var linkPicker: ObservedValue<ChildSlot<AnyObject, RecipePickerBloc>>

    @State private var ingredientsSelection: TextSelection?
    @State private var directionsSelection: TextSelection?
    @State private var showMoreDetails = false
    @State private var showCategoryPicker = false
    @State private var photoItem: PhotosPickerItem?
    @State private var photoToCrop: CroppableImage?
    @State private var linkWatch: Cancellable?

    init(bloc: EditRecipeBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
        _title = StateObject(wrappedValue: ObservedFlow(bloc.title))
        _descriptionText = StateObject(wrappedValue: ObservedFlow(bloc.description_))
        _imageUrl = StateObject(wrappedValue: ObservedFlow(bloc.imageUrl))
        _ingredients = StateObject(wrappedValue: ObservedFlow(bloc.ingredients))
        _directions = StateObject(wrappedValue: ObservedFlow(bloc.directions))
        _sourceUrl = StateObject(wrappedValue: ObservedFlow(bloc.sourceUrl))
        _servings = StateObject(wrappedValue: ObservedFlow(bloc.servings))
        _prepTime = StateObject(wrappedValue: ObservedFlow(bloc.prepTime))
        _cookTime = StateObject(wrappedValue: ObservedFlow(bloc.cookTime))
        _totalTime = StateObject(wrappedValue: ObservedFlow(bloc.totalTime))
        _calories = StateObject(wrappedValue: ObservedFlow(bloc.calories))
        _starRating = StateObject(wrappedValue: ObservedOptionalFlow(bloc.starRating))
        _categories = StateObject(wrappedValue: ObservedFlow(bloc.categories))
        _userCategories = StateObject(wrappedValue: ObservedFlow(bloc.availableUserCategories))
        _books = StateObject(wrappedValue: ObservedFlow(bloc.recipeBooks))
        _selectedBookIds = StateObject(wrappedValue: ObservedFlow(bloc.selectedBookIds))
        _pendingPhoto = StateObject(wrappedValue: ObservedOptionalFlow(bloc.pendingPhotoBytes))
        _linkPicker = StateObject(wrappedValue: ObservedValue(bloc.recipePickerSlot))
    }

    var body: some View {
        let state = model.value
        Group {
            if state.isLoading {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                form
            }
        }
        .navigationTitle(state.title.localized)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .blocksSwipeBack()
        .toolbar {
            ToolbarItem(placement: .topBarLeading) {
                Button(action: bloc.onBackClicked) {
                    Label(ComposeStrings["edit_recipe_back"], systemImage: "chevron.backward")
                }
            }
            ToolbarItem(placement: .confirmationAction) {
                if state.isSaving {
                    ProgressView()
                } else {
                    Button(ComposeStrings["edit_recipe_save"], action: bloc.onSaveClicked)
                        .fontWeight(.semibold)
                }
            }
        }
        .tint(ChefTheme.primary)
        .alert(ComposeStrings["edit_recipe_discard_title"], isPresented: discardBinding) {
            Button(ComposeStrings["edit_recipe_discard_confirm"], role: .destructive, action: bloc.onDiscardChangesConfirmed)
            Button(ComposeStrings["edit_recipe_discard_cancel"], role: .cancel, action: bloc.onDiscardChangesCancelled)
        } message: {
            Text(ComposeStrings["edit_recipe_discard_message"])
        }
        .alert(state.uploadError?.localized ?? "", isPresented: uploadErrorBinding) {
            Button(ComposeStrings["edit_recipe_upload_photo_dismiss"], action: bloc.onUploadErrorDismissed)
        }
        .sheet(isPresented: $showCategoryPicker) {
            CategoryPickerView(bloc: bloc, selected: categories, userCategories: userCategories)
        }
        .sheet(isPresented: linkPickerBinding) {
            if let picker = linkPicker.value.child?.instance {
                ComposeScreenHost(screen: picker)
                    .presentationDragIndicator(.visible)
            }
        }
        .fullScreenCover(item: $photoToCrop) { item in
            PhotoCropView(image: item.image, onCancel: { photoToCrop = nil }) { data in
                bloc.onPhotoPicked(bytes: NativeBytes.shared.fromData(data: data), fileExtension: "jpg")
                photoToCrop = nil
            }
        }
        .onChange(of: photoItem) {
            guard let item = photoItem else { return }
            photoItem = nil
            Task {
                if let data = try? await item.loadTransferable(type: Data.self), let image = UIImage(data: data) {
                    photoToCrop = CroppableImage(image: image)
                }
            }
        }
        .onAppear {
            // A picked recipe link lands in whichever field asked for it, at its caret.
            linkWatch = linkWatch ?? EventWatcher<EditRecipeBlocLinkInsertion>(flow: bloc.linkInsertions).watch { insertion in
                insertLink(insertion)
            }
        }
        .onDisappear {
            linkWatch?.cancel()
            linkWatch = nil
        }
    }

    private var form: some View {
        Form {
            Section {
                TextField(ComposeStrings["edit_recipe_field_title"], text: textBinding(title, bloc.onTitleChanged(title:)),
                          prompt: Text(ComposeStrings["edit_recipe_field_title_placeholder"]))
                    .font(.title3.weight(.semibold))
            }

            Section(ComposeStrings["edit_recipe_field_books"]) { booksField }
            Section(ComposeStrings["edit_recipe_field_category"]) { categoriesField }
            Section("Rating") { ratingField }
            Section { photoField }

            Section {
                DisclosureGroup(ComposeStrings["edit_recipe_section_more_details"], isExpanded: $showMoreDetails) {
                    moreDetails
                }
            }

            Section(ComposeStrings["edit_recipe_field_ingredients"]) {
                MarkdownEditorField(
                    label: ComposeStrings["edit_recipe_field_ingredients"],
                    placeholder: ComposeStrings["edit_recipe_field_ingredients_placeholder"],
                    text: textBinding(ingredients, bloc.onIngredientsChanged(ingredients:)),
                    selection: $ingredientsSelection,
                    showListButtons: true,
                    minHeight: 240,
                    onLinkRecipe: { bloc.onLinkRecipeClicked(field: .ingredients) }
                )
            }
            Section(ComposeStrings["edit_recipe_field_directions"]) {
                MarkdownEditorField(
                    label: ComposeStrings["edit_recipe_field_directions"],
                    placeholder: ComposeStrings["edit_recipe_field_directions_placeholder"],
                    text: textBinding(directions, bloc.onDirectionsChanged(directions:)),
                    selection: $directionsSelection,
                    showListButtons: true,
                    minHeight: 240,
                    onLinkRecipe: { bloc.onLinkRecipeClicked(field: .directions) }
                )
            }
        }
        .scrollDismissesKeyboard(.interactively)
    }

    // MARK: Fields

    @ViewBuilder
    private var booksField: some View {
        let all = books.value.compactMap { $0 as? RecipeBook }
        let selected = Set(selectedBookIds.value.allObjects.compactMap { ($0 as? NSNumber)?.int64Value })
        if all.isEmpty {
            Text(ComposeStrings["edit_recipe_field_books_none"]).foregroundStyle(.secondary)
        } else {
            FlowLayout {
                ForEach(all, id: \.id) { book in
                    let isOn = selected.contains(book.id)
                    Chip(title: book.name, isOn: isOn, systemImage: isOn ? "checkmark" : nil) {
                        bloc.onToggleBook(bookId: book.id)
                    }
                }
            }
            .padding(.vertical, ChefTheme.Spacing.extraSmall)
        }
    }

    @ViewBuilder
    private var categoriesField: some View {
        let attached = categories.value.allObjects.compactMap { $0 as? ComposeApp.Category }
            .sorted { $0.name.localizedCompare($1.name) == .orderedAscending }
        if attached.isEmpty {
            Text(ComposeStrings["edit_recipe_field_category_none"]).foregroundStyle(.secondary)
        } else {
            FlowLayout {
                ForEach(attached, id: \.id) { category in
                    let label = RecipeTextFormatter.shared.categoryLabel(category: category, lookup: ComposeStrings.shared)
                    Chip(title: label, isOn: true, systemImage: "xmark") {
                        bloc.onDetachCategory(category: category)
                    }
                    .accessibilityLabel(String(format: ComposeStrings["edit_recipe_field_category_remove_a11y"], label))
                }
            }
            .padding(.vertical, ChefTheme.Spacing.extraSmall)
        }
        Button { showCategoryPicker = true } label: {
            Label(ComposeStrings["edit_recipe_field_category_add"], systemImage: "plus")
        }
    }

    private var ratingField: some View {
        let rating = starRating.value?.intValue ?? 0
        return HStack(spacing: ChefTheme.Spacing.small) {
            ForEach(1...5, id: \.self) { star in
                Button {
                    // Tapping the current rating clears it, like Android.
                    bloc.onStarRatingChanged(starRating: rating == star ? nil : KotlinInt(int: Int32(star)))
                } label: {
                    Image(systemName: star <= rating ? "star.fill" : "star")
                        .font(.title2)
                        .foregroundStyle(star <= rating ? ChefTheme.primary : Color.secondary.opacity(0.5))
                }
                .buttonStyle(.borderless)
                .accessibilityLabel("\(star) star\(star == 1 ? "" : "s")")
                .accessibilityAddTraits(star == rating ? .isSelected : [])
            }
        }
    }

    @ViewBuilder
    private var photoField: some View {
        let pendingImage = pendingPhoto.value.flatMap { UIImage(data: NativeBytes.shared.toData(bytes: $0)) }
        let url = (imageUrl.value as String).isEmpty ? nil : URL(string: imageUrl.value as String)
        if pendingImage != nil || url != nil {
            Color.clear
                .aspectRatio(1, contentMode: .fit)
                .overlay {
                    if let pendingImage {
                        Image(uiImage: pendingImage).resizable().scaledToFill()
                    } else {
                        AsyncImage(url: url) { $0.resizable().scaledToFill() } placeholder: { ProgressView() }
                    }
                }
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .listRowInsets(EdgeInsets(top: 12, leading: 12, bottom: 4, trailing: 12))
        }
        PhotosPicker(selection: $photoItem, matching: .images) {
            Label(ComposeStrings["edit_recipe_upload_photo"], systemImage: "photo.badge.plus")
        }
    }

    @ViewBuilder
    private var moreDetails: some View {
        MarkdownEditorField(
            label: ComposeStrings["edit_recipe_field_description"],
            placeholder: ComposeStrings["edit_recipe_field_description_placeholder"],
            text: textBinding(descriptionText, bloc.onDescriptionChanged(description:)),
            selection: .constant(nil),
            minHeight: 120
        )
        LabeledField(ComposeStrings["edit_recipe_field_image_url"], ComposeStrings["edit_recipe_field_image_url_placeholder"],
                     text: textBinding(imageUrl, bloc.onImageUrlChanged(imageUrl:)), keyboard: .URL)
        LabeledField(ComposeStrings["edit_recipe_field_source_url"], ComposeStrings["edit_recipe_field_source_url_placeholder"],
                     text: textBinding(sourceUrl, bloc.onSourceUrlChanged(sourceUrl:)), keyboard: .URL)
        LabeledField(ComposeStrings["edit_recipe_field_servings"], ComposeStrings["edit_recipe_field_servings_placeholder"],
                     text: textBinding(servings, bloc.onServingsChanged(servings:)), keyboard: .numberPad)
        LabeledField(ComposeStrings["edit_recipe_field_prep_time"], ComposeStrings["edit_recipe_field_prep_time_placeholder"],
                     text: textBinding(prepTime, bloc.onPrepTimeChanged(prepTime:)), keyboard: .numberPad)
        LabeledField(ComposeStrings["edit_recipe_field_cook_time"], ComposeStrings["edit_recipe_field_cook_time_placeholder"],
                     text: textBinding(cookTime, bloc.onCookTimeChanged(cookTime:)), keyboard: .numberPad)
        LabeledField(ComposeStrings["edit_recipe_field_total_time"], ComposeStrings["edit_recipe_field_total_time_placeholder"],
                     text: textBinding(totalTime, bloc.onTotalTimeChanged(totalTime:)), keyboard: .numberPad)
        LabeledField(ComposeStrings["edit_recipe_field_calories"], ComposeStrings["edit_recipe_field_calories_placeholder"],
                     text: textBinding(calories, bloc.onCaloriesChanged(calories:)), keyboard: .numberPad)
    }

    // MARK: Bindings and actions

    /// A text binding over a BLoC `StateFlow<String>`; reads `current` so typing never snaps back.
    private func textBinding(_ flow: ObservedFlow<NSString>, _ onChange: @escaping (String) -> Void) -> Binding<String> {
        Binding(get: { flow.current as String }, set: { onChange($0) })
    }

    private var discardBinding: Binding<Bool> {
        Binding(
            get: { model.value.showDiscardChangesDialog },
            set: { if !$0 && model.current.showDiscardChangesDialog { bloc.onDiscardChangesCancelled() } }
        )
    }

    private var uploadErrorBinding: Binding<Bool> {
        Binding(
            get: { model.value.uploadError != nil },
            set: { if !$0 && model.current.uploadError != nil { bloc.onUploadErrorDismissed() } }
        )
    }

    private var linkPickerBinding: Binding<Bool> {
        Binding(
            get: { linkPicker.value.child != nil },
            set: { if !$0 && linkPicker.value.child != nil { bloc.onLinkPickerDismissed() } }
        )
    }

    private func insertLink(_ insertion: EditRecipeBlocLinkInsertion) {
        switch insertion.field {
        case .ingredients:
            let (text, selection) = MarkdownEditorField.insertingLink(
                label: insertion.label, url: insertion.url, into: ingredients.current as String, at: ingredientsSelection)
            bloc.onIngredientsChanged(ingredients: text)
            ingredientsSelection = selection
        default:
            let (text, selection) = MarkdownEditorField.insertingLink(
                label: insertion.label, url: insertion.url, into: directions.current as String, at: directionsSelection)
            bloc.onDirectionsChanged(directions: text)
            directionsSelection = selection
        }
    }
}

private struct CroppableImage: Identifiable {
    let id = UUID()
    let image: UIImage
}

/// A tappable capsule chip: tinted when on, outlined when off.
private struct Chip: View {
    let title: String
    let isOn: Bool
    let systemImage: String?
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if let systemImage {
                    Image(systemName: systemImage).font(.caption.weight(.bold))
                }
                Text(title).font(.subheadline)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .foregroundStyle(isOn ? ChefTheme.primary : Color.primary)
            .background(isOn ? ChefTheme.primary.opacity(0.14) : Color.clear, in: Capsule())
            .overlay(Capsule().stroke(isOn ? Color.clear : Color.secondary.opacity(0.4)))
        }
        .buttonStyle(.borderless)
        .accessibilityAddTraits(isOn ? .isSelected : [])
    }
}

/// A caption-labelled text field for the "More details" group.
private struct LabeledField: View {
    let label: String
    let placeholder: String
    @Binding var text: String
    let keyboard: UIKeyboardType

    init(_ label: String, _ placeholder: String, text: Binding<String>, keyboard: UIKeyboardType) {
        self.label = label
        self.placeholder = placeholder
        _text = text
        self.keyboard = keyboard
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label).font(.caption).foregroundStyle(.secondary)
            TextField(placeholder, text: $text)
                .keyboardType(keyboard)
                .textInputAutocapitalization(keyboard == .URL ? .never : .sentences)
                .autocorrectionDisabled(keyboard == .URL)
        }
    }
}

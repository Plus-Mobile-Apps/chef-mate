import SwiftUI
import UniformTypeIdentifiers
import ComposeApp

/// The redesigned recipe importer, mirroring the Android `ImportRecipesScreen`: pick a .zip with
/// the system file importer, review the parsed recipes, choose the books to file them under, then
/// import.
struct ImportRecipesView: View {
    let bloc: ImportRecipesBloc
    @StateObject private var model: ObservedFlow<ImportRecipesBlocModel>
    @StateObject private var books: ObservedFlow<NSArray>
    @StateObject private var selectedBooks: ObservedFlow<NSSet>
    @State private var isPickingFile = false

    init(bloc: ImportRecipesBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
        _books = StateObject(wrappedValue: ObservedFlow(bloc.recipeBooks))
        _selectedBooks = StateObject(wrappedValue: ObservedFlow(bloc.selectedBookIds))
    }

    var body: some View {
        let phase = model.value.phase
        Group {
            if let review = phase as? ImportRecipesBlocPhaseReview {
                reviewList(review)
            } else if phase is ImportRecipesBlocPhaseParsing {
                StatusBlock(message: ComposeStrings["import_recipes_parsing"], showsProgress: true)
            } else if let done = phase as? ImportRecipesBlocPhaseDone {
                StatusBlock(
                    systemImage: "checklist.checked",
                    title: ComposeStrings["import_recipes_done_headline", ["count": "\(done.importedCount)"]]
                ) {
                    Button(ComposeStrings["import_recipes_done_button"], action: bloc.onBack)
                        .buttonStyle(.borderedProminent)
                    Button(ComposeStrings["import_recipes_start_over"], action: bloc.onStartOver)
                        .buttonStyle(.bordered)
                }
            } else if let error = phase as? ImportRecipesBlocPhaseError {
                StatusBlock(message: error.message.localized, isError: true) {
                    chooseFileButton
                }
            } else {
                StatusBlock(
                    systemImage: "doc.zipper",
                    title: ComposeStrings["import_recipes_empty_headline"],
                    message: ComposeStrings["import_recipes_empty_body"]
                ) {
                    Text(ComposeStrings["import_recipes_empty_format_hint"])
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                    chooseFileButton
                }
            }
        }
        .controlSize(.large)
        .tint(ChefTheme.primary)
        .navigationTitle(ComposeStrings["import_recipes_title"])
        .navigationBarTitleDisplayMode(.inline)
        .fileImporter(isPresented: $isPickingFile, allowedContentTypes: [.zip]) { result in
            guard case .success(let url) = result else { return }
            // Files outside the sandbox (iCloud Drive, other apps) need scoped access to read.
            let scoped = url.startAccessingSecurityScopedResource()
            defer { if scoped { url.stopAccessingSecurityScopedResource() } }
            guard let data = try? Data(contentsOf: url) else { return }
            bloc.onArchiveSelected(
                bytes: ByteArrayBridge.shared.fromData(data: data),
                fileName: url.lastPathComponent
            )
        }
    }

    private var chooseFileButton: some View {
        Button(ComposeStrings["import_recipes_choose_file"]) { isPickingFile = true }
            .buttonStyle(.borderedProminent)
    }

    private func reviewList(_ review: ImportRecipesBlocPhaseReview) -> some View {
        let recipes = review.recipes
        let selectedCount = recipes.filter(\.selected).count
        let allSelected = !recipes.isEmpty && selectedCount == recipes.count
        let bookList = (books.value as? [RecipeBook]) ?? []
        let selectedIds = Set(((selectedBooks.value as? Set<KotlinLong>) ?? []).map(\.int64Value))
        // A book is required so imported recipes always have a home; the default is preselected.
        let bookRequired = !bookList.isEmpty && selectedIds.isEmpty

        return List {
            Section {
                ForEach(recipes, id: \.id) { item in
                    SelectableRecipeRow(
                        title: item.title,
                        subtitle: item.subtitle.localized,
                        isSelected: item.selected,
                        trailing: { if item.hasImage { Image(systemName: "photo").foregroundStyle(.secondary) } }
                    ) {
                        bloc.onRecipeToggled(id: item.id)
                    }
                    .disabled(review.isImporting)
                }
            } header: {
                SelectAllHeader(
                    title: ComposeStrings["import_recipes_review_headline"],
                    toggleTitle: ComposeStrings[allSelected ? "import_recipes_deselect_all" : "import_recipes_select_all"],
                    isDisabled: review.isImporting,
                    action: bloc.onToggleSelectAll
                )
            }

            if !bookList.isEmpty {
                Section {
                    FlowLayout {
                        ForEach(bookList, id: \.id) { book in
                            ChoiceChip(title: book.name, isSelected: selectedIds.contains(book.id)) {
                                bloc.onToggleBook(bookId: book.id)
                            }
                            .disabled(review.isImporting)
                        }
                    }
                    .padding(.vertical, ChefTheme.Spacing.extraSmall)
                } header: {
                    Text(ComposeStrings["import_recipes_books_label"])
                } footer: {
                    if selectedIds.isEmpty {
                        Text(ComposeStrings["import_recipes_books_required"]).foregroundStyle(.red)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .safeAreaInset(edge: .bottom) {
            PrimaryBottomButton(
                title: ComposeStrings["import_recipes_import_button", ["count": "\(selectedCount)"]],
                systemImage: "square.and.arrow.down",
                isLoading: review.isImporting,
                isEnabled: selectedCount > 0 && !review.isImporting && !bookRequired,
                action: bloc.onImportClicked
            )
        }
    }
}

/// The redesigned recipe exporter, mirroring the Android `ExportRecipesScreen`: review and select
/// recipes, build the archive, then hand it to the system file exporter to save.
struct ExportRecipesView: View {
    let bloc: ExportRecipesBloc
    @StateObject private var model: ObservedFlow<ExportRecipesBlocModel>
    /// The archive currently offered to the file exporter, keyed by the BLoC's save token so a
    /// cancelled save can be retried with an identical archive.
    @State private var exportDocument: ZipDocument?
    @State private var exportToken: Int64?

    init(bloc: ExportRecipesBloc) {
        self.bloc = bloc
        _model = StateObject(wrappedValue: ObservedFlow(bloc.state))
    }

    var body: some View {
        let state = model.value
        let phase = state.phase
        Group {
            if let review = phase as? ExportRecipesBlocPhaseReview {
                reviewList(review)
            } else if phase is ExportRecipesBlocPhaseEmpty {
                StatusBlock(
                    systemImage: "tray",
                    title: ComposeStrings["export_recipes_empty_headline"],
                    message: ComposeStrings["export_recipes_empty_body"]
                )
            } else if let done = phase as? ExportRecipesBlocPhaseDone {
                StatusBlock(
                    systemImage: "checklist.checked",
                    title: ComposeStrings["export_recipes_done_headline", ["count": "\(done.exportedCount)"]]
                ) {
                    Button(ComposeStrings["export_recipes_done_button"], action: bloc.onBack)
                        .buttonStyle(.borderedProminent)
                    Button(ComposeStrings["export_recipes_start_over"], action: bloc.onStartOver)
                        .buttonStyle(.bordered)
                }
            } else if let error = phase as? ExportRecipesBlocPhaseError {
                StatusBlock(message: error.message.localized, isError: true) {
                    Button(ComposeStrings["export_recipes_start_over"], action: bloc.onStartOver)
                        .buttonStyle(.borderedProminent)
                }
            } else {
                StatusBlock(message: ComposeStrings["export_recipes_loading"], showsProgress: true)
            }
        }
        .controlSize(.large)
        .tint(ChefTheme.primary)
        .navigationTitle(ComposeStrings["export_recipes_title"])
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { offerPendingSave(state.pendingSave) }
        .onChange(of: state.pendingSave?.token) { _, _ in offerPendingSave(model.current.pendingSave) }
        .fileExporter(
            isPresented: Binding(
                get: { exportDocument != nil },
                // The completion below reports the outcome, a cancel included (as a failure).
                set: { if !$0 { exportDocument = nil } }
            ),
            document: exportDocument,
            contentType: .zip,
            defaultFilename: exportDocument?.fileName
        ) { result in
            exportDocument = nil
            if case .success = result {
                bloc.onSaveCompleted(saved: true)
            } else {
                bloc.onSaveCompleted(saved: false)
            }
        }
    }

    private func offerPendingSave(_ pending: ExportRecipesBlocPendingSave?) {
        guard let pending, pending.token != exportToken else { return }
        exportToken = pending.token
        exportDocument = ZipDocument(
            data: ByteArrayBridge.shared.toData(bytes: pending.archive),
            fileName: pending.fileName
        )
    }

    private func reviewList(_ review: ExportRecipesBlocPhaseReview) -> some View {
        let recipes = review.recipes
        let selectedCount = recipes.filter(\.selected).count
        let allSelected = !recipes.isEmpty && selectedCount == recipes.count

        return List {
            Section {
                ForEach(recipes, id: \.id) { item in
                    SelectableRecipeRow(
                        title: item.title,
                        subtitle: item.subtitle.localized,
                        isSelected: item.selected,
                        trailing: {
                            if let imageUrl = item.imageUrl {
                                RecipeImage(url: imageUrl)
                                    .frame(width: 48, height: 48)
                                    .clipShape(RoundedRectangle(cornerRadius: 8))
                            }
                        }
                    ) {
                        bloc.onRecipeToggled(id: item.id)
                    }
                    .disabled(review.isExporting)
                }
            } header: {
                SelectAllHeader(
                    title: ComposeStrings["export_recipes_review_headline"],
                    toggleTitle: ComposeStrings[allSelected ? "export_recipes_deselect_all" : "export_recipes_select_all"],
                    isDisabled: review.isExporting,
                    action: bloc.onToggleSelectAll
                )
            }
        }
        .listStyle(.insetGrouped)
        .safeAreaInset(edge: .bottom) {
            // The Android FAB only appears once something is selected.
            if selectedCount > 0 {
                PrimaryBottomButton(
                    title: review.isExporting
                        ? ComposeStrings["export_recipes_generating"]
                        : ComposeStrings["export_recipes_export_button", ["count": "\(selectedCount)"]],
                    systemImage: "square.and.arrow.up",
                    isLoading: review.isExporting,
                    isEnabled: !review.isExporting,
                    action: bloc.onExportClicked
                )
                .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .animation(.default, value: selectedCount > 0)
    }
}

/// A recipe archive handed to the system file exporter.
struct ZipDocument: FileDocument {
    static let readableContentTypes: [UTType] = [.zip]

    let data: Data
    let fileName: String

    init(data: Data, fileName: String) {
        self.data = data
        self.fileName = fileName
    }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
        fileName = configuration.file.filename ?? "recipes.zip"
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

/// A review row: a selection checkmark, the recipe title and subtitle, and an optional trailing view.
private struct SelectableRecipeRow<Trailing: View>: View {
    let title: String
    let subtitle: String
    let isSelected: Bool
    @ViewBuilder let trailing: Trailing
    let onToggle: () -> Void

    var body: some View {
        Button(action: onToggle) {
            HStack(spacing: ChefTheme.Spacing.normal - 4) {
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundStyle(isSelected ? ChefTheme.primary : Color.secondary)
                VStack(alignment: .leading, spacing: 2) {
                    Text(title).foregroundStyle(Color.primary)
                    Text(subtitle)
                        .font(.footnote)
                        .foregroundStyle(Color.secondary)
                }
                Spacer(minLength: 0)
                trailing
            }
            .contentShape(Rectangle())
        }
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

private struct SelectAllHeader: View {
    let title: String
    let toggleTitle: String
    let isDisabled: Bool
    let action: () -> Void

    var body: some View {
        HStack {
            Text(title)
            Spacer()
            Button(toggleTitle, action: action)
                .font(.subheadline.weight(.semibold))
                .textCase(nil)
                .disabled(isDisabled)
        }
    }
}

/// A full-width prominent button floating in glass above the bottom edge.
private struct PrimaryBottomButton: View {
    let title: String
    let systemImage: String
    let isLoading: Bool
    let isEnabled: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: ChefTheme.Spacing.small) {
                if isLoading {
                    ProgressView().tint(.white)
                } else {
                    Image(systemName: systemImage)
                }
                Text(title)
            }
            .font(.headline)
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        .controlSize(.large)
        .disabled(!isEnabled)
        .padding(ChefTheme.Spacing.small)
        .chefGlass(in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .padding(.horizontal, ChefTheme.Spacing.normal)
        .padding(.bottom, ChefTheme.Spacing.small)
    }
}

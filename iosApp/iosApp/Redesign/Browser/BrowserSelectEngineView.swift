import SwiftUI
import ComposeApp

/// First-run engine picker, mirroring the Android `BrowserSelectEngineScreen`: the user has to
/// choose a default search engine (none is preselected) before the landing screen appears.
struct BrowserSelectEngineView: View {
    let bloc: BrowserSelectEngineBloc

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(spacing: 0) {
                    Image(systemName: "fork.knife")
                        .font(.system(size: 56, weight: .regular))
                        .foregroundStyle(ChefTheme.primary)
                        .frame(width: 72, height: 72)
                        .accessibilityHidden(true)
                    Text(ComposeStrings["browser_select_engine_title"])
                        .font(.title2.bold())
                        .multilineTextAlignment(.center)
                        .padding(.top, ChefTheme.Spacing.large)
                    Text(ComposeStrings["browser_select_engine_subtitle"])
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.top, ChefTheme.Spacing.small)

                    VStack(spacing: ChefTheme.Spacing.small) {
                        ForEach(SearchEngine.entries, id: \.self) { engine in
                            Button { bloc.onEngineSelected(engine: engine) } label: {
                                HStack(spacing: ChefTheme.Spacing.normal) {
                                    Image(engine.logoImageName)
                                        .resizable()
                                        .scaledToFit()
                                        .frame(width: ChefTheme.Spacing.large, height: ChefTheme.Spacing.large)
                                    Text(engine.displayName)
                                        .font(.headline)
                                    Spacer()
                                    Image(systemName: "chevron.forward")
                                        .font(.footnote.weight(.semibold))
                                        .foregroundStyle(.tertiary)
                                }
                                .padding(.horizontal, ChefTheme.Spacing.normal)
                                .frame(height: ChefTheme.Spacing.rowHeight)
                                .background(
                                    Color(.secondarySystemBackground),
                                    in: RoundedRectangle(cornerRadius: ChefTheme.Spacing.normal, style: .continuous)
                                )
                                .contentShape(RoundedRectangle(cornerRadius: ChefTheme.Spacing.normal, style: .continuous))
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.top, ChefTheme.Spacing.extraLarge)
                }
                .padding(.horizontal, ChefTheme.Spacing.extraLarge)
                .padding(.vertical, ChefTheme.Spacing.large)
                .frame(maxWidth: 600)
                .frame(maxWidth: .infinity, minHeight: geometry.size.height)
            }
            .scrollBounceBehavior(.basedOnSize)
        }
    }
}

#Preview {
    BrowserSelectEngineView(bloc: BrowserPreviewsKt.previewBrowserSelectEngineBloc)
}

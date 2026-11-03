# iOS SwiftUI Redesign

Rebuild the iOS UI natively in SwiftUI, one screen per PR, behind the `ios_swiftui_redesign` flag.
The look follows the Android (Material 3) app — same layouts, hierarchy, copy, and colors — but uses
native iOS chrome: a `NavigationStack` for push/pop and the edge-swipe back gesture, `TabView` for
the bottom nav, and Liquid Glass on toolbars, the tab bar, sheets, and floating controls.

Business logic does **not** move. Every screen keeps binding to its existing Kotlin BLoC; only the
rendering layer changes on iOS. Android, desktop, and web keep their Compose UI.

## Feature flag

| | |
|---|---|
| Key | `ios_swiftui_redesign` (`FeatureFlagRegistry.IosSwiftUiRedesign`) |
| Default | `false` |
| Read | once, in `RootBlocProvider.buildRootBloc`, into `RootBlocProvider.useSwiftUiRedesign` |
| Consumed | `ContentView.swift` picks `RedesignRootView` vs the Compose root |

The value is snapshotted at launch (cached rows + dev overrides), so the shell never swaps
mid-session. Toggle it from **More → Developer settings → Feature flags** and relaunch the app. The
redesigned More tab keeps that path reachable at every stage, so the flag can always be turned back
off from inside the redesign.

Supabase row (iOS only, start at 0%):

```sql
insert into feature_flags (key, value_type, value, enabled, rollout_percent, platforms)
values ('ios_swiftui_redesign', 'bool', 'true', true, 0, array['IOS']);
```

## Architecture

### Hybrid shell: native where done, hosted Compose everywhere else

The redesign shell (`iosApp/iosApp/Redesign/`) renders the Decompose `RootBloc` stack itself. For
each child, `RootChildView` (root stack) or `TabChildView` (bottom-nav tabs) picks a view with a
`switch` over the child type:

- **Native** — a SwiftUI view bound to the child's BLoC.
- **Hosted** — anything not converted yet renders its existing Compose screen inside a
  `UIViewController` (`ComposeScreenHost` → Kotlin `ComposeScreenViewController.create(screen:)`),
  wrapped in `ChefMateTheme`.

So the flag is always usable end to end, and each screen PR just flips one case from hosted to
native.

### State interop

No new compiler plugins. Kotlin `iosMain` exposes:

- `FlowWatcher` — subscribes to a `StateFlow` on the main thread and returns a cancel handle.
- `TextDataResolver` — resolves a `TextData` to a `String` off the composition (via
  `getString`/`getPluralString`).

Swift wraps these in `ObservedFlow<T>` (an `ObservableObject`) and `ObservedValue<T>` (for
Decompose `Value`, e.g. `ChildStack`), and a `LocalizedText` view that resolves `TextData`
(`Redesign/Interop.swift`). Bindings whose setter calls the BLoC read `ObservedFlow.current` (the
live StateFlow value) rather than `value`, which publishes a beat later, so SwiftUI never snaps back
to a stale value.

`ImmutableList` model fields reach Swift as plain arrays, so no conversion is needed.

Each screen PR adds `export(projects.client.<feature>.public)` to the iOS framework (and an
`api(...)` for it in `composeApp`) so Swift sees the BLoC interface and its `Model`.

> SKIE would remove the hand-written bridge (Flows → `AsyncSequence`, sealed classes → Swift enums)
> but pins us to its supported Kotlin versions; we're on Kotlin 2.4.20. Revisit if the bridge gets
> painful.

### Navigation

- **Hosted screens hide the navigation bar** (they draw their own app bar). A
  `UINavigationController` extension in `ComposeScreenHost.swift` keeps the edge-swipe back gesture
  working anyway, since UIKit turns it off when the bar is hidden.
- **Root stack → `NavigationStack(path:)`.** The path mirrors `RootBloc.state.active` + backstack
  (minus the bottom-nav root). A user swipe-back or system back button shrinks the path, and the
  binding's setter calls `RootBloc.onBackClicked()` so Decompose stays the source of truth.
- **Modal children** (the Android `isModal` set: Browser, MealPlanner, CookMode, AiChat,
  EditRecipeBook, EditGroceryList) → `.fullScreenCover` / `.sheet` instead of a push. Not done
  yet: they push for now.
- **Nested stacks** (RecipeRoot, SettingsRoot, MealPlanner, ManageFamily, AiChat, Browser,
  Onboarding) start out hosted, nested navigation included. When one goes native, its children get
  flattened into the same root path: each entry records its owning stack, and a pop routes to the
  deepest owner's `onBackClicked`. Nested `NavigationStack`s are not allowed.
- **Bottom nav → `TabView`** with the iOS 18 `Tab` API, ordered by `BottomNavBloc.Model.tabs`;
  selection calls `onTabSelected`. Only the active Decompose child exists, so the other tabs render
  empty until selected.

### Liquid Glass

The deployment target stays **iOS 18.2**. Building with the iOS 26 SDK gives standard chrome
(tab bar, navigation bars, toolbars, sheets, menus) Liquid Glass for free on iOS 26 devices. Custom
floating elements (FABs, the recipe filter bar, the cook-mode controls) use
`.chefGlass()`, which is `.glassEffect()` on iOS 26 and `.regularMaterial` on 18.

### Theme

`ChefTheme.swift` mirrors `ChefMateTheme`: brand colors as `Color` tokens (light and dark),
spacing from `AppDimensions` (4/8/16/24/32, row 56), corner radii from `Shape.kt`. Use the native
type scale (`.title2`, `.headline`, `.body`) and SF Symbols for Material icons.

### Global hosts

The Compose `ToastScaffold` and the in-app review host only exist inside `App.kt`. The SwiftUI shell
needs native equivalents that watch `ToastService` and `InAppReviewService` (PR 1).

## PR sequence

Each PR: SwiftUI views under `iosApp/iosApp/Redesign/<Feature>/`, add a case to `RootChildView` /
`TabChildView`, add the framework `export`, add `#Preview`s fed by the Kotlin `preview*Bloc`
fakes from the feature's `Previews.kt`, and check the flag off and on in the simulator.

| # | PR | Screens | Notes |
|---|---|---|---|
| 0 | Flag + launch switch | — | Done. |
| 1 | Shell + interop | Root stack, `TabView`, hosted fallback, theme, `.chefGlass()`, flow bridge | Done (first cut). |
| 2 | More tab | `SettingsScreen` | Done (first cut): keeps Dev settings → Feature flags reachable. |
| 3 | Recipe list | `RecipeListScreen` | Done (first cut): search, list/grid, favorite, delete, add. Selection mode, filter sheet, book picker, scan from photo still to do. |
| 4 | Toast + review hosts | — | Native toast overlay on `ToastService`. |
| 5 | Recipe detail | `RecipeDetailScreen`, `RecipeImageFullScreenScreen` | First nested-stack flattening (RecipeRoot). Zoom transition for the image. |
| 6 | Edit recipe | `EditRecipeScreen` | Rich text (bold/italic markdown), image picker. |
| 7 | Recipe → grocery / meal flows | `AddRecipeToGroceryListScreen`, `RecipePickerScreen`, `ChooseDateScreen`, `ChooseMealTypeScreen`, `MealPlannerRootScreen` | MealPlanner becomes a sheet. |
| 8 | Grocery list | `GroceryListScreen` | Swipe to delete with undo, add row, sections. |
| 9 | Grocery detail + edit list | `GroceryDetailScreen`, `EditGroceryListScreen` | |
| 10 | Meal plan | `MealPlanScreen` | |
| 11 | Cook mode | `CookModeScreen`, `WhatsCookingScreen` | Full-screen cover, glass controls. |
| 12 | Settings root | `SettingsRootScreen`, `AppSettingsScreen`, `BottomNavOrderScreen`, grocery autocomplete + category rules | |
| 13 | Auth | `AuthenticationScreen`, `OtpScreen` | |
| 14 | Onboarding | `OnboardingRootScreen` + pages | `TabView(.page)`. |
| 15 | Browser | `BrowserRootScreen`, landing, bookmarks, edit query, select engine | Native `WKWebView`. |
| 16 | AI chat | `AiChatRootScreen`, `AiChatScreen`, `AiChatHistoryScreen` | |
| 17 | Profile + notifications | `ManageProfileScreen`, `NotificationsScreen` | |
| 18 | Manage family | `FamilyListScreen`, `FamilyDetailScreen` | |
| 19 | Recipe books & misc | `EditRecipeBookScreen`, `RecipeCategoriesScreen`, `ExportRecipesScreen`, `ImportRecipesScreen`, public recipe | |
| 20 | Dev tools | `DeveloperSettingsScreen`, `FeatureFlagsScreen` | Last on purpose: hosted Compose keeps the flag toggle working throughout. |
| 21 | Rollout + cleanup | — | Ramp the flag; once at 100%, delete the hosted fallback and the Compose iOS path. |

## Open questions

- **Snapshot tests for SwiftUI.** Repo rules require snapshots for UI changes, but the existing
  harness is Android-only. Proposal: add an iOS test target with `swift-snapshot-testing` in PR 4.
- **Forced light mode.** The Compose host sets `overrideUserInterfaceStyle = .light`. The redesign
  supports dark mode from the start; decide whether to keep that.
- **Robot UI tests** cover the Compose screens and keep running on Android/desktop. Native iOS
  screens would need XCUITest for equivalent coverage.

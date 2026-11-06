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
- `TextDataLocalizer` — resolves a `TextData` synchronously through a Swift-provided
  `StringLookup` (see Strings below).

Swift wraps these in `ObservedFlow<T>` (an `ObservableObject`) and `ObservedValue<T>` (for
Decompose `Value`, e.g. `ChildStack`) in `Redesign/Interop.swift`. Bindings whose setter calls the BLoC read `ObservedFlow.current` (the
live StateFlow value) rather than `value`, which publishes a beat later, so SwiftUI never snaps back
to a stale value.

`ImmutableList` model fields reach Swift as plain arrays, so no conversion is needed.

Each screen PR adds `export(projects.client.<feature>.public)` to the iOS framework (and an
`api(...)` for it in `composeApp`) so Swift sees the BLoC interface and its `Model`.

> SKIE would remove the hand-written bridge (Flows → `AsyncSequence`, sealed classes → Swift enums)
> but pins us to its supported Kotlin versions; we're on Kotlin 2.4.20. Revisit if the bridge gets
> painful.

### Strings

Compose resources stay the single source of truth. Swift reads them synchronously from an Xcode
String Catalog generated from them, rather than through moko-resources or a second copy of the
strings.

- `:client:composeApp:generateIosStringCatalog` (`build-logic/.../ios/GenerateIosStringCatalogTask.kt`)
  merges every module's `composeResources/values/strings.xml` into
  `iosApp/iosApp/Generated/ComposeStrings.xcstrings`. It runs before every Xcode build (hooked to
  `embedAndSignAppleFrameworkForXcode`) and is configuration-cache safe.
- **Naming:** one table keyed by resource name. The ~930 names are already unique app-wide; a name
  defined twice with different text fails the build.
- **Conversion:** escapes are unwrapped like Compose resources does (`\'`, `\n`, `\uXXXX`, XML
  entities), and Android specifiers become Foundation ones (`%1$s` → `%1$@`, `%d` → `%lld`).
- **Plurals:** become catalog plural variations. Xcode requires every variant to reference the number,
  so `{quantity}` becomes `%lld`, and a variant without `{quantity}` fails the build. Verified:
  0/1/2/5 → "0 items", "1 item", "2 items", "5 items".
- **Swift API** (`Redesign/ComposeStrings.swift`): `ComposeStrings["sign_in"]` for native screens;
  `textData.localized` / `LocalizedText(text:)` for BLoC `TextData`. `TextData` subclasses aren't
  visible to Swift, so Kotlin's `TextDataLocalizer` walks the tree and calls back into
  `ComposeStrings` for each key and plural.
- **Committed, not gitignored.** Xcode plans resources before the Gradle build phase runs, so a
  fresh clone without the file would build without strings. Edit `strings.xml` and the next iOS
  build rewrites the catalog; commit it alongside.

Known gaps:
- A missing key returns the key itself (`ComposeStrings["typo"]` shows `typo`). Next step: have
  the generator also emit typed Swift accessors (`ComposeStrings.signIn`) so typos fail to compile.
- English only today. Adding a locale means reading `values-<lang>/` into the same catalog.
- No CI check that the committed catalog matches `strings.xml` yet.

### Images

Compose drawables stay the single source too. The Kotlin framework build already copies every
module's `composeResources` into the app bundle (`compose-resources/composeResources/<package>/`),
so `ComposeDrawables.image("onboarding_app_icon", package: "chefmate.client.onboarding.public.generated.resources")`
(`Redesign/ComposeDrawables.swift`) loads the same PNG Compose's `painterResource` does, cached. No
asset-catalog copies. A `_dark` sibling is picked by the caller from `colorScheme`.

### Navigation

- **Hosted screens hide the navigation bar** (they draw their own app bar). A
  `UINavigationController` extension in `ComposeScreenHost.swift` keeps the edge-swipe back gesture
  working anyway, since UIKit turns it off when the bar is hidden.
- **Root stack → `NavigationStack(path:)`.** The path mirrors `RootBloc.state.active` + backstack
  (minus the bottom-nav root). A user swipe-back or system back button shrinks the path, and the
  binding's setter calls `RootBloc.onBackClicked()` so Decompose stays the source of truth.
- **Modal children** (the Android `isModal` set: Browser, MealPlanner, CookMode, AiChat,
  EditRecipeBook, EditGroceryList) → `.fullScreenCover` instead of a push, once native. The
  flattened entries split at the first modal root child (`RootStackModel.mainEntries` /
  `modalEntries`): everything from it up is presented in one full-screen cover with its own
  `NavigationStack`, so screens a modal opens (AI chat → "Add recipe" → recipe editor) push inside
  the cover. The cover is keyed by the modal's root child, so a nested stack replacing its own root
  (AI chat "New conversation") swaps the cover's root view instead of re-presenting it. Dismissal
  only goes through the BLoC (the close button), and toasts move to an overlay inside the cover
  while it shows. Native so far: AiChat, CookMode; the hosted ones still push.
  MealPlanner is the exception: a `.sheet` (`MealPlannerSheet`). `RootStackModel` stops `entries`
  at it and publishes its BLoC instead; `RedesignRootView` presents it with `.sheet(item:)` (so the
  content stays while the sheet animates away), and a swipe-down calls `RootBloc.onBackClicked`
  to pop the whole child. Inside the sheet, its nested stack gets its own `NavigationStack`
  mirroring `routerState` (a sheet is a new presentation, so this isn't a nested stack).
  A native modal can also opt in with `StackEntry.presentsAsSheet`: when it is the top entry,
  `RedesignRootView` leaves it out of the path and presents it in a `.sheet` with its own
  `NavigationStack`; a swipe-down pops it through its BLoC. So far: EditRecipeBook, EditGroceryList.
- **Nested stacks** (RecipeRoot, SettingsRoot, MealPlanner, ManageFamily, AiChat, Browser,
  Onboarding) start out hosted, nested navigation included. When one goes native, its children get
  flattened into the same root path (`RootStackModel` in `RedesignRootView.swift`, which also
  subscribes to each flattened child's own `routerState`). Each `StackEntry` records which BLoC pops
  it: a nested child pops through its stack's `onBackClicked`, except the nested stack's first
  child, which pops the whole root child through `RootBloc.onBackClicked` (a Decompose stack can't
  pop its last item). Nested `NavigationStack`s are not allowed. Flattened so far: RecipeRoot, AiChat, ManageFamily,
  SettingsRoot.
  Browser is the exception: its screens replace one another in place (a crossfade) rather than
  push, and the tab bar stays visible, so `BrowserRootView` renders its active child itself.
  Onboarding is the exception: its steps are pages of one `TabView(.page)` rather than pushes (see
  PR 14), so it stays a single root entry.
- **Bottom nav → `TabView`** with the iOS 18 `Tab` API, ordered by `BottomNavBloc.Model.tabs`;
  selection calls `onTabSelected`. Only the active Decompose child exists, so the other tabs render
  empty until selected.
- **The tab bar is the system `TabView` bar**, so iOS 26 devices get the native Liquid Glass bar
  (selection pill, badges via `.badge`, keyboard handling) for free. Like the Android bottom nav it
  only shows on tab roots: each pushed screen hides it with `.toolbar(.hidden, for: .tabBar)`.
- **The tab bar slides with push and pop.** SwiftUI's tab controller ignores UIKit's
  `hidesBottomBarWhenPushed`, so `.toolbar(.hidden, for: .tabBar)` alone snaps the bar away on a
  push and back only once a pop has finished. `TabBarPushTransition` (a background on each tab root)
  restores the UIKit motion: as a screen is pushed it pins a window snapshot of the bar to the tab
  root's view, so the bar leaves with the root and comes back with it on a pop, tracking the
  interactive swipe back (a cancelled swipe keeps the snapshot for the next try). Once the root is
  back on screen the real bar has returned and the snapshot is dropped. Overlays drawn above the
  `TabView` (toasts) clear the bar with `BottomNavView.tabBarHeight`.
- **Web links** in native screens open in an `SFSafariViewController` sheet instead of the in-app
  Browser screen (`.safariSheet(item:)` in `Redesign/SafariView.swift`).

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
has native equivalents in `Redesign/GlobalHosts.swift`, applied once at the root:

- **Toasts** — a glass card at the bottom (lifted over the tab bar while it shows) that drains
  `ToastService.queue` through Kotlin's `ToastBridge`. Each toast ends exactly once (timer: short 4s,
  long 10s, indefinite never; swipe down; or its action), and `ToastBridge.finish` runs the matching
  `onAction` / `onDismiss` and dequeues it. Hosted Compose screens only get `LocalToastService`, not
  a `ToastScaffold`, so they never race the native host for the same queue.
- **Store review** — watches `InAppReviewService.isPromptRequested` and calls SwiftUI's
  `requestReview`.

## PR sequence

Each PR: SwiftUI views under `iosApp/iosApp/Redesign/<Feature>/`, add a case to `RootChildView` /
`TabChildView`, add the framework `export`, add `#Preview`s fed by the Kotlin `preview*Bloc`
fakes from the feature's `Previews.kt`, and check the flag off and on in the simulator.

| # | PR | Screens | Notes |
|---|---|---|---|
| 0 | Flag + launch switch | — | Done. |
| 1 | Shell + interop | Root stack, `TabView`, hosted fallback, theme, `.chefGlass()`, flow bridge | Done (first cut). |
| 2 | More tab | `SettingsScreen` | Done (first cut): keeps Dev settings → Feature flags reachable. |
| 3 | Recipe list | `RecipeListScreen` | Done: search, list/grid, overflow menu (view mode, sync, select, export all, collaborate), sort & filter sheet, grouped book picker with edit, selection mode with bulk export / add to book / add to category, scan from photo (`PhotosPicker` → `NativeBytes`), invite banners, continue / done cooking, search / filter / empty states, sync status, inline-markdown descriptions. Swipe and context-menu delete confirm first. Coach marks and the tablet filter rail still to do. |
| 4 | Toast + review hosts | — | Done (first cut): native toast overlay on `ToastService`, StoreKit review prompt. |
| 5 | Recipe detail | `RecipeDetailScreen`, `RecipeImageFullScreenScreen` | Done (first cut): RecipeRoot flattened (Edit stays hosted), hero + pinned ingredient/direction sections, scale menu, links, share/edit/delete, bottom glass toolbar, zoom transition into a pinch-zoom image. Text parsing reuses Kotlin via `RecipeTextFormatter`. Coach marks and the tablet two-column layout still to do; the add-to-grocery sheet is native since PR 7. |
| 6 | Edit recipe | `EditRecipeScreen` | Done (first cut): every field bound to the BLoC's StateFlows; books and category chips with a native category picker (create/rename/delete); rating; PhotosPicker + square crop; markdown editors with selection-aware bold/italic/list buttons, recipe-link insertion at the caret, and a preview. Back and swipe-back go through the BLoC's discard prompt (`SwipeBackGate`). WYSIWYG rich-text mode, coach marks and the wide layout still to do; the recipe-link picker still hosts Compose. |
| 7 | Recipe → grocery / meal flows | `AddRecipeToGroceryListScreen`, `RecipePickerScreen`, `ChooseDateScreen`, `ChooseMealTypeScreen`, `MealPlannerRootScreen` | Done (first cut). Meal half (with PR 10): MealPlanner is a native sheet — searchable recipe picker, month calendar with the day's planned meals and the family plan menu, meal-type checklist; Next/Save in the nav bar. Regular width folds the meal type into the date step like the Compose combined layout. Add to grocery list: a native sheet from recipe detail with the list picker (grouped Personal / family / Shared), the shared scale menu, and ingredients by aisle with a check per row; Add in the nav bar. |
| 8 | Grocery list | `GroceryListScreen` | Done (first cut): pinned aisle sections, check-off, swipe to delete (Undo via the native toast host) and swipe to check, floating glass add row with autocomplete above it, list picker as the title menu, sort & filter sheet, delete/create dialogs, invite banners, sync popover tip. The item detail sheet is native since PR 9. |
| 9 | Grocery detail + edit list | `GroceryDetailScreen`, `EditGroceryListScreen` | Done (first cut): item detail as a native sheet (name, free-form quantity with stepper buttons, aisle picker with always-file-here, purchased; Save in the nav bar). Edit list is a native sheet like the recipe book form: rename (owner only), collaborators grouped by role, owner-only invite with role picker, signed-out prompt, delete with confirm. |
| 10 | Meal plan | `MealPlanScreen` | Done (first cut): Day/Week/Month segmented control, date paging, meals grouped by type (by day in Week) with swipe/context-menu remove + confirm, per-section cook-mode replace/add, month calendar (grid laid out by Kotlin's `MonthCalendarGrid`, shared with the add-meal date step), pull-to-refresh and sync button, family plan switcher as the title menu, cook-session buttons. Coach marks still to do. |
| 11 | Cook mode | `CookModeScreen`, `WhatsCookingScreen` | Done (first cut): full-screen cover with its own stack, stacked layout under pinned headers, split layout as segmented swipe pages (iPhone) or resizable columns (iPad), cross-off / highlight, scale menu, keep screen on (`isIdleTimerDisabled`), finish confirm, premium alert, glass recipe-chip bar, What's Cooking as a native sheet with select-and-remove. Coach marks are a glass callout under the nav bar, not anchored tooltips. Line views (`IngredientLine`, `DirectionLine`) are now shared with recipe detail in `RecipeText.swift`. |
| 12 | Settings root | `SettingsRootScreen`, `AppSettingsScreen`, `BottomNavOrderScreen`, grocery autocomplete + category rules | Done (first cut): every SettingsRoot child is native. App settings is a Form (the default search engine is a menu rather than the Compose picker dialog); tab order uses the system edit-mode reorder with Save in the nav bar; saved grocery items and aisle rules have inline create rows, swipe/context-menu delete with confirm, and read-only defaults; Settings → Bookmarks (`BrowserBookmarksScreen`) has selection mode with bulk delete. |
| 13 | Auth | `AuthenticationScreen`, `OtpScreen` | Done (first cut): email/password with iOS autofill hints (`.username`/`.password`, `.oneTimeCode` for the code), forgot-password / email-me-a-code, mode switch, guest-data discard alert, glass submit button above the keyboard, resend countdown. Swipe-back pops through each BLoC's own `onBackClicked` (`StackEntry.Popper.child`) so abandoning sign-in still clears the gated destination. Privacy/terms open in a Safari sheet. |
| 14 | Onboarding | `OnboardingRootScreen` + pages | Done (first cut): one root entry, every visited step a page of a `TabView(.page)` under a native toolbar (back, progress dots, Skip). The BLoC stack only holds visited steps, so there's no page ahead and swiping only goes back; a settled swipe or the back arrow pops through `OnboardingRootBloc.onBackClicked`. Previews and copy load from Compose resources (`ComposeDrawables`, `ComposeStrings`). Re-entered from More it pushes; the pager owns horizontal swipes, so the edge swipe pages back like any swipe and does nothing on the first step, where the back arrow exits (Dismissed). Not yet: `#Preview`s (the step fakes live in `onboarding:impl`, which isn't exported). |
| 15 | Browser | `BrowserRootScreen`, landing, edit query, select engine, browser | Done (first cut): native `WKWebView` kept in the BLoC's `InstanceKeeper` (page and history survive tab switches), crossfade between the browser's own screens with the address bar morphing via `matchedGeometryEffect`, edge-swipe page history, engine picker, bookmarks grid, search coach mark as a popover. The root-stack browser (links opened from elsewhere) pushes the same view with a nav bar. The Settings → Bookmarks screen (`BrowserBookmarksScreen`) is native since PR 12. |
| 16 | AI chat | `AiChatRootScreen`, `AiChatScreen`, `AiChatHistoryScreen` | Done (first cut): first native modal (full-screen cover, AiChat flattened). Bottom-anchored log, inline markdown via `AttributedString` (lists → bullets, headings → bold), thinking bubble, add-recipe pill, glass input with `PhotosPicker` (bytes cross via `NativeBytes`), web links in a Safari sheet. History: swipe/context-menu delete with confirmation, delete all. The Android typewriter reveal is not ported; replies show as they stream. |
| 17 | Profile + notifications | `ManageProfileScreen`, `NotificationsScreen` | Done (first cut): profile Form with `PhotosPicker` avatar, display name, read-only email, and delete account behind an email-confirmation alert. Notifications lists invites with Accept/Decline (also as swipe actions), plus signed-out and empty states; the invite text comes from a shared Kotlin `notificationMessage()` so `notifications:data` needn't be exported. |
| 18 | Manage family | `FamilyListScreen`, `FamilyDetailScreen` | Done (first cut): ManageFamily flattened into the root stack, both screens native. Form dialogs (new/rename family, invite, create/rename shared grocery list or recipe book) are sheets; confirmations are alerts; member and collection actions are menus. |
| 19 | Recipe books & misc | `EditRecipeBookScreen`, `RecipeCategoriesScreen`, `ExportRecipesScreen`, `ImportRecipesScreen`, public recipe | Done (first cut): SettingsRoot flattened, EditRecipeBook as the first native sheet (Form: name, collaborators by role, invite, delete/leave), categories with inline create, context menu / swipe rename-delete and bulk selection, import/export via `fileImporter` / `fileExporter` (bytes cross through `NativeBytes`), public recipe with a floating Save. Export opened from the recipe list is native too. |
| 20 | Dev tools | `DeveloperSettingsScreen`, `FeatureFlagsScreen` | Done (first cut): developer settings with environment / test-user pickers as sheets, premium toggle, coach-mark reset and restart alerts. Feature flags: Default / On / Off segmented control per boolean flag, text field with Reset / Save for string flags, Reset all in the nav bar. Still reachable from More → Developer settings, so the redesign flag can be turned off from inside it. |
| 21 | Rollout + cleanup | — | Ramp the flag; once at 100%, delete the hosted fallback and the Compose iOS path. |

## Open questions

- **Snapshot tests for SwiftUI.** Repo rules require snapshots for UI changes, but the existing
  harness is Android-only. Proposal: add an iOS test target with `swift-snapshot-testing` in PR 4.
- **Forced light mode.** The Compose host sets `overrideUserInterfaceStyle = .light`. The redesign
  supports dark mode from the start; decide whether to keep that.
- **Robot UI tests** cover the Compose screens and keep running on Android/desktop. Native iOS
  screens would need XCUITest for equivalent coverage.

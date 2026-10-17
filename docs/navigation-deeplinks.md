# Navigation Deeplinks

`chefmate://` deeplinks (and their `https://chefmate.plusmobileapps.com/...` web equivalents) open the app directly on a specific screen. They are used by invite emails and recipe share links, and also speed up iteration on screens that are normally several taps deep.

On a **cold start** the deeplink is parsed once and used as the initial Decompose stack (`RootBlocImpl.initialStackFor`). On a **warm start** the running app receives the URL through `RootBloc.handleDeepLink(url)`, which brings the destination to the front. Process-death restore keeps the user's saved stack, and the deeplink does **not** override it. Deeplinks are ignored until onboarding has been completed.

For platform configuration (Android App Links, iOS Universal Links and Associated Domains, desktop scheme registration), see [DEEP_LINKING_SETUP.md](DEEP_LINKING_SETUP.md).

## Supported URIs

| URI | Lands on |
|---|---|
| `chefmate://recipe/{id}` | Recipe detail for the given **local** recipe id (`Long`) — dev/internal navigation only; a local id is meaningless to another user |
| `chefmate://recipe/{remoteId}` / `https://chefmate.plusmobileapps.com/recipe/{remoteId}` | **Public recipe share link.** A non-numeric segment is treated as the recipe's global Supabase id (UUID). If the recipe is already in the user's library it opens the normal detail screen; otherwise it opens the read-only public preview with a "Save to my recipes" action. See [Public recipe sharing](#public-recipe-sharing). |
| `chefmate://groceries` | Bottom-nav `Groceries` tab |
| `chefmate://meal-planner` | Bottom-nav `Meals` tab |
| `chefmate://settings` | App settings root screen (the destination reached via Settings tab → "App Settings") |
| `chefmate://notifications` / `https://chefmate.plusmobileapps.com/notifications` | Notifications screen (target of the invite emails) |
| `chefmate://signin` | Authentication screen in sign-in mode |
| `chefmate://signup` | Authentication screen in sign-up mode |

Unknown URIs, a missing recipe segment (`chefmate://recipe`, `chefmate://recipe/`), and non-`chefmate://` / non-web-host URIs all fall back to `DeepLink.None` and the app launches normally on the recipes tab. Note `chefmate://recipe/abc` is **not** `None` — a non-numeric segment is parsed as a public-recipe share link (see below).

## Public recipe sharing

Recipes are private by default (RLS-scoped to the owner and accepted recipe-book collaborators). The **Share → "Share recipe link"** action on the recipe detail screen makes a recipe public (after a confirmation dialog) and shares `https://chefmate.plusmobileapps.com/recipe/{remoteId}`, where `remoteId` is the recipe's global Supabase UUID. "Stop sharing link" makes it private again. Backing pieces: `recipes.is_public` + an additive `SELECT` RLS policy (`supabase/migrations/20260713_add_recipe_public_sharing.sql`), and `RecipeRepository.setRecipePublic` / `fetchPublicRecipe`.

A recipient opening the link routes to `DeepLink.PublicRecipe(remoteId)` → `RootBloc.Child.PublicRecipe` (`PublicRecipeBloc`): if they already have the recipe locally it opens the normal detail screen; otherwise it fetches the public row and shows a read-only preview with **"Save to my recipes"** (which files an owned copy via `createRecipe`).

## Android

The `chefmate://` scheme is registered via an `ACTION_VIEW` intent-filter on `MainActivity` in `client/composeApp/src/androidMain/AndroidManifest.xml`. `MainActivity.onCreate` reads `intent.data` and passes the parsed `DeepLink` to `buildRootBloc`.

```bash
# Force-stop first so a fresh cold start picks up the new intent
adb shell am force-stop com.plusmobileapps.chefmate

# Launch with a deeplink
adb shell am start -W -a android.intent.action.VIEW -d "chefmate://recipe/1"
adb shell am start -W -a android.intent.action.VIEW -d "chefmate://groceries"
adb shell am start -W -a android.intent.action.VIEW -d "chefmate://meal-planner"
adb shell am start -W -a android.intent.action.VIEW -d "chefmate://settings"
adb shell am start -W -a android.intent.action.VIEW -d "chefmate://signin"
adb shell am start -W -a android.intent.action.VIEW -d "chefmate://signup"
```

Without `force-stop`, Android delivers the intent to the already-running activity via `onNewIntent`, which forwards it to `rootBloc.handleDeepLink(url)` (a warm start).

## Desktop (JVM)

### Dev loop (`./gradlew run`)

`main(args: Array<String>)` takes the first CLI argument and parses it as the cold-start deeplink.
Scheme registration and single-instance forwarding are no-ops under Gradle (there is no installed
launcher), so this is the way to exercise a deeplink during development:

```bash
./gradlew :client:composeApp:run --args="chefmate://recipe/1"
./gradlew :client:composeApp:run --args="chefmate://groceries"
./gradlew :client:composeApp:run --args="chefmate://meal-planner"
./gradlew :client:composeApp:run --args="chefmate://settings"
./gradlew :client:composeApp:run --args="chefmate://signin"
./gradlew :client:composeApp:run --args="chefmate://signup"
```

### Packaged builds (how a real deeplink reaches the app)

Unlike Android/iOS, a desktop app cannot claim an `https://` link — Windows and Linux have no App
Links / Universal Links, and the browser owns `https:`. So the invite-email `https://` link lands on
the [chefmate-site](https://chefmate.plusmobileapps.com) `/notifications` page, which redirects the
browser to `chefmate://notifications`. The desktop app registers that custom scheme so the OS routes
it back:

- **macOS** — declared via `CFBundleURLTypes` in the packaged Info.plist
  (`client/composeApp/build.gradle.kts`, `macOS { infoPlist { … } }`). LaunchServices registers it
  when the `.app` is installed. macOS delivers the URL as an Apple Event, caught by
  `Desktop.setOpenURIHandler` in `main.kt` (not as a command-line arg), for both cold and warm launches.
- **Windows / Linux** — self-registered at runtime by
  `client/composeApp/src/jvmMain/.../deeplink/SchemeRegistrar.kt` (a per-user registry entry / a
  `~/.local/share/applications` `.desktop` handler). No admin, idempotent, re-derived from the
  installed launcher each launch. The OS starts a **new process** per open, so
  `deeplink/SingleInstance.kt` elects one primary via a file lock + loopback socket and forwards
  later launches' links into it, raising the existing window instead of opening a second one.

Warm deliveries (macOS Apple Event, or a forwarded Windows/Linux launch) flow through
`deeplink/DeepLinkCoordinator.kt` into `RootBloc.handleDeepLink(url)`; cold-start links are still
applied as the initial navigation stack.

Test against an **installed** packaged build (`packageReleasePkg` / `packageReleaseMsi` /
`packageReleaseDeb`), with the app both closed and already open:

```bash
open "chefmate://notifications"                 # macOS
xdg-open "chefmate://notifications"             # Linux
start "" "chefmate://notifications"             # Windows (cmd)
```

## iOS

The `chefmate` URL scheme is registered in `iosApp/iosApp/Info.plist` under `CFBundleURLTypes`. `AppDelegate.application(_:didFinishLaunchingWithOptions:)` captures `launchOptions[.url]` (excluding the existing `chefmate://import?url=...` share flow) and forwards the URL string to `RootBlocProvider.buildRootBloc(..., deepLinkUrl:)`. Links that arrive after launch, including `https://` Universal Links, are handled by `.onOpenURL` / `.onContinueUserActivity` in `ContentView.swift`, which call `root.handleDeepLink(url:)`.

```bash
# Boot the simulator first if it isn't running
xcrun simctl boot "iPhone 15"   # adjust to your installed simulator

# Cold-start with a deeplink (simctl terminate ensures the next launch is cold)
xcrun simctl terminate booted com.plusmobileapps.chefmate
xcrun simctl openurl booted "chefmate://recipe/1"
xcrun simctl openurl booted "chefmate://groceries"
xcrun simctl openurl booted "chefmate://meal-planner"
xcrun simctl openurl booted "chefmate://settings"
xcrun simctl openurl booted "chefmate://signin"
xcrun simctl openurl booted "chefmate://signup"
```

The `chefmate://import?url=...` share flow is handled first in the same `onOpenURL` handler and is routed to `handleSharedUrl` instead.

## Adding a new deeplink

1. Add a new `data object` or `data class` to `DeepLink` in `client/root/public/src/commonMain/kotlin/com/plusmobileapps/chefmate/root/DeepLink.kt` and a matching branch in `DeepLink.parse`.
2. Extend `RootBlocImpl.initialStackFor` (cold start) and `RootBlocImpl.handleDeepLink` (warm start) in `client/root/impl/...` to map the new variant to a `Configuration` (or list of configurations).
3. If the target lives behind the bottom nav, also update the `initialBottomNavTab` mapping.
4. Add coverage in `DeepLinkTest` (parser) and `RootBlocTest` (initial stack).

Platform entry points already forward any parsed deeplink, so no changes are required on Android/JVM/iOS for additional `chefmate://` URIs.

### `https://` App Links / Universal Links

Opening an `https://chefmate.plusmobileapps.com/...` link from a browser or email (rather than the `chefmate://` scheme) additionally requires the path to be registered for verified deep linking:

- **Android:** add the path prefix to the `autoVerify` `<intent-filter>` in `AndroidManifest.xml` (e.g. `/recipe`, `/notifications`). Domain ownership is proven by the hosted `.well-known/assetlinks.json`, which is domain-level and needs no per-path change.
- **iOS:** the `applinks:chefmate.plusmobileapps.com` entitlement is not path-scoped, but the **externally hosted** `apple-app-site-association` file (in the `chefmate-site` repo, not this one) must list the path (e.g. add `/recipe/*`) for Universal Links to open the app. This is a separate deploy from the app.

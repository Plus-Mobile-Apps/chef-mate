# Chef Mate

A Kotlin Multiplatform app that is a mate to any chef in the kitchen managing recipes, grocery lists, and meal planning. It is available on Android, iOS, MacOS, Windows & Linux. 

[Download Page](https://chefmate.plusmobileapps.com/download/)

## Getting Started

This is a [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) project. The client app targets Android, iOS (plus a watchOS companion), and Desktop (JVM), and is backed by [Supabase](https://supabase.com/). A separate admin dashboard targets Web (WASM) and Desktop. It aims to share as much code as possible in a modular way, so the general file structure of the project is as follows: 

* [client](./client/) - all the shared client code and client application code
  * [composeApp](./client/composeApp/) - the compose multiplatform applications
  * [aichat](./client/aichat/) - AI chat and recipe extraction flows
  * [auth](./client/auth/) - authentication data, UI, and use cases
  * [bottomnav](./client/bottomnav/) - bottom navigation and tab ordering
  * [browser](./client/browser/) - in-app recipe browser and browser history
  * [cook](./client/cook/) - cook mode and active cooking sessions
  * [database](./client/database/) - client database
  * [developer-settings](./client/developer-settings/) - debug-only developer settings and test-user support
  * [family](./client/family/) - family groups, roles, and member management
  * [featureflag](./client/featureflag/) - feature flag evaluation, overrides, and admin-facing data
  * [notifications](./client/notifications/) - in-app notifications and invites
  * [onboarding](./client/onboarding/) - first-run onboarding flow (includes a standalone `demo` app)
  * [profile](./client/profile/) - user profile management
  * [root](./client/root/) - root BLoC (business logic component) to manage navigation of the whole app
  * [grocery](./client/grocery/) - grocery data plus list and detail BLoCs
  * [meal](./client/meal/) - meal planning data and UI logic
  * [recipe](./client/recipe/) - recipe data, list/detail flows, categories, import, and export
  * [recipebook](./client/recipebook/) - recipe book data and editing flows
  * [settings](./client/settings/) - settings root, app settings, and more menu flows
  * [shared](./client/shared/) - common client code
  * [subscription](./client/subscription/) - premium subscription state and gating
  * [testing](./client/testing/) - common testing helpers
  * [text](./client/text/) - shared text and resource helpers
  * [toast](./client/toast/) - app-wide toast/snackbar service
  * [ui](./client/ui/) - shared Compose UI components, theme, robots, and screenshot tests
  * [util](./client/util/) - shared utility APIs and implementations
* [admin](./admin/) - Compose Multiplatform admin app for managing feature flags
* [iosApp](./iosApp/) - iOS application shell, share extension, and watchOS companion app
* [supabase](./supabase/) - Supabase migrations and backend configuration
* [docs](./docs/) - architecture, deployment, deep linking, and feature documentation
* [build-logic](./build-logic/) - all the convention plugins to share logic of modular libraries
* [devkit](./devkit/) - shared Gradle snippets for module conventions
* [fastlane](./fastlane/) - store release automation
* [packaging](./packaging/) - desktop installer packaging assets
* [scripts](./scripts/) - local development and release helper scripts

### Android

```shell
./gradlew :client:composeApp:installDebug
```

### Desktop (JVM)

```shell
./gradlew :client:composeApp:run
```

### iOS

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

## Documentation

- [Architecture overview](docs/architecture.md)
- [Build configuration (Supabase + Bugsnag)](docs/buildconfig-setup.md)
- [Developer settings (debug-only env switch + test-user login)](docs/developer-settings.md)
- [Deployment (Android / iOS / Desktop)](docs/deployment.md)
- [Version bumps (CI)](docs/ci/VERSION_BUMP_SETUP.md)
- [Deep linking setup (Android / iOS / Desktop)](docs/DEEP_LINKING_SETUP.md)
- [Navigation deeplinks (supported URIs)](docs/navigation-deeplinks.md)
- [Email verification](docs/EMAIL_VERIFICATION_GUIDE.md)
- [Recipe browser architecture](docs/browser-architecture.md)
- [Recipe book collaboration](docs/collaboration.md)
- [Feature flags](docs/feature-flags.md)
- [Feature flag admin dashboard](docs/feature-flag-admin.md)

---

## Libraries Used

### Shared

* [Coroutines](https://github.com/Kotlin/kotlinx.coroutines) - asynchronous programming framework
* [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) - multiplatform serialization library
* [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) - multiplatform date time library
* [kotlinx.collections.immutable](https://github.com/Kotlin/kotlinx.collections.immutable) - immutable collections for stable Compose state
* [ktfmt](https://github.com/facebook/ktfmt) - kotlin code formatting and linter

### Client

* [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform) - shared UI for client
* [SqlDelight](https://sqldelight.github.io/sqldelight/2.1.0/) - sqlite database
* [Essenty](https://github.com/arkivanov/Essenty) - lifecycle, instance keeper, back handler, state keeper
* [Decompose](https://github.com/arkivanov/Decompose) - navigation
* [metro](https://zacsweers.github.io/metro/) - dependency injection
* [metro-extensions](https://github.com/Plus-Mobile-Apps/metro-extensions) - assisted factory code generation for Metro
* [kermit](https://github.com/touchlab/Kermit) - logging
* [Bugsnag](https://github.com/bugsnag/bugsnag-kotlin-multiplatform) - error logging
* [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings) - key value storage
* [Ktor client](https://ktor.io/docs/client-create-new-application.html) - HTTP networking
* [supabase-kt](https://github.com/supabase-community/supabase-kt) - Supabase auth, postgrest, realtime, storage, and edge functions client
* [Coil](https://coil-kt.github.io/coil/) - image loading
* [Compose WebView Multiplatform](https://github.com/KevinnZou/compose-webview-multiplatform) - in-app recipe browser
* [Ksoup](https://github.com/fleeksoft/ksoup) - HTML parsing for recipe import and extraction
* [Compose Rich Editor](https://github.com/MohamedRejeb/compose-rich-editor) - rich text editing
* [Multiplatform Markdown Renderer](https://github.com/mikepenz/multiplatform-markdown-renderer) - markdown rendering for AI chat
* [BuildKonfig](https://github.com/yshrsmz/BuildKonfig) - build-time configuration (Supabase keys, Bugsnag)

### Backend

* [Supabase](https://supabase.com/) - Postgres, auth, storage, realtime, and edge functions (see [supabase](./supabase/))

### Tooling

* [Compose Hot Reload](https://github.com/JetBrains/compose-hot-reload) - live UI reload on desktop
* [Kover](https://github.com/Kotlin/kotlinx-kover) - code coverage
* [fastlane](https://fastlane.tools/) - store release automation

### Testing

* [Kotlin test](https://kotlinlang.org/api/core/kotlin-test/) - KMP testing framework
* [Kotest](https://kotest.io/docs/assertions/assertions.html) - assertions
* [multiplatform-settings-test](https://github.com/russhwolf/multiplatform-settings) - in-memory settings for tests
* [Ktor MockEngine](https://ktor.io/docs/client-testing.html) - HTTP client mocking
* [Mokkery](https://mokkery.dev/) - mocking library
* [Turbine](https://github.com/cashapp/turbine) - coroutines testing library
* [Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing) - snapshot tests for `@Preview` composables (see `client/ui/screenshot-test/` and `.agents/seed.md` for the workflow)
* [Compose testing](https://kotlinlang.org/docs/multiplatform/compose-test.html) - official KMP Compose UI testing docs

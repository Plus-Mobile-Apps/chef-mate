# Chef Mate

A Kotlin Multiplatform app that is a mate to any chef in the kitchen managing recipes, grocery lists, and meal planning. It is available on Android, iOS, MacOS, Windows & Linux. 

[Download Page](https://chefmate.plusmobileapps.com/download/)

## Getting Started

This is a [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html) project targeting Android, iOS, Web, Desktop (JVM), Server. It aims to share as much code as possible in a modular way, so the general file structure of the project is as follows: 

* [client](./client/) - all the shared client code and client application code
  * [composeApp](./client/composeApp/) - the compose multiplatform applications
  * [aichat](./client/aichat/) - AI chat and recipe extraction flows
  * [auth](./client/auth/) - authentication data, UI, and use cases
  * [bottomnav](./client/bottomnav/) - bottom navigation and tab ordering
  * [browser](./client/browser/) - in-app recipe browser and browser history
  * [cook](./client/cook/) - cook mode and active cooking sessions
  * [database](./client/database/) - client database
  * [developer-settings](./client/developer-settings/) - debug-only developer settings and test-user support
  * [featureflag](./client/featureflag/) - feature flag evaluation, overrides, and admin-facing data
  * [root](./client/root/) - root BLoC (business logic component) to manage navigation of the whole app
  * [grocery](./client/grocery/) - grocery data plus list and detail BLoCs
  * [meal](./client/meal/) - meal planning data and UI logic
  * [recipe](./client/recipe/) - recipe data, list/detail flows, categories, import, and export
  * [recipebook](./client/recipebook/) - recipe book data and editing flows
  * [settings](./client/settings/) - settings root, app settings, and more menu flows
  * [shared](./client/shared/) - common client code
  * [testing](./client/testing/) - common testing helpers
  * [text](./client/text/) - shared text and resource helpers
  * [ui](./client/ui/) - shared Compose UI components, theme, robots, and screenshot tests
  * [util](./client/util/) - shared utility APIs and implementations
* [admin](./admin/) - Compose Multiplatform admin app for managing feature flags
* [iosApp](./iosApp/) - iOS application shell and share extension
* [supabase](./supabase/) - Supabase migrations and backend configuration
* [docs](./docs/) - architecture, deployment, deep linking, and feature documentation
* [build-logic](./build-logic/) - all the convention plugins to share logic of modular libraries
* [devkit](./devkit/) - shared Gradle snippets for module conventions
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
- [Local Supabase development (test env instead of prod)](docs/supabase-local-development.md)
- [Developer settings (debug-only env switch + test-user login)](docs/developer-settings.md)
- [Deployment (Android / iOS / Desktop)](docs/deployment.md)
- [Deep linking setup](docs/DEEP_LINKING_SETUP.md)
- [Email verification](docs/EMAIL_VERIFICATION_GUIDE.md)

---

## Libraries Used

### Shared

* [Coroutines](https://github.com/Kotlin/kotlinx.coroutines) - asynchronous programming framework
* [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) - multiplatform serialization library
* [kotlinx-datetime](https://github.com/Kotlin/kotlinx-datetime) - multiplatform date time library
* [ktfmt](https://github.com/facebook/ktfmt) - kotlin code formatting and linter

### Client

* [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform) - shared UI for client
* [SqlDelight](https://sqldelight.github.io/sqldelight/2.1.0/) - sqlite database
* [Essenty](https://github.com/arkivanov/Essenty) - lifecycle, instance keeper, back handler, state keeper
* [Decompose](https://github.com/arkivanov/Decompose) - navigation
* [metro](https://zacsweers.github.io/metro/) - dependency injection
* [kermit](https://github.com/touchlab/Kermit) - logging
* [Bugsnag](https://github.com/bugsnag/bugsnag-kotlin-multiplatform) - error logging
* [multiplatform-settings](https://github.com/russhwolf/multiplatform-settings) - key value storage

### Server

* [Supabase](https://supabase.com/)

### Testing

* [Kotlin test](https://kotlinlang.org/api/core/kotlin-test/) - KMP testing framework
* [Kotest](https://kotest.io/docs/assertions/assertions.html) - assertions
* [Mokkery](https://mokkery.dev/) - mocking library
* [Turbine](https://github.com/cashapp/turbine) - coroutines testing library
* [Compose Preview Screenshot Testing](https://developer.android.com/studio/preview/compose-screenshot-testing) - snapshot tests for `@Preview` composables (see `client/ui/screenshot-test/` and `CLAUDE.md` for the workflow)
* [Compose testing](https://kotlinlang.org/docs/multiplatform/compose-test.html) - official KMP Compose UI testing docs

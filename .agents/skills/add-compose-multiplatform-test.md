# add-compose-multiplatform-test

Add a Compose Multiplatform UI test that exercises a real BLoC tree end-to-end across JVM, iOS Simulator, and Android (instrumented) variants in `:client:composeApp`.

## Required input

Ask the user (if not provided):

1. **What flow to test** — e.g. "tap recipe in list → recipe detail appears". Identify the user-visible entry point and the assertion target.
2. **Which root to spin up** — almost always `RootBloc` (covers bottom-nav-hosted flows). Smaller scopes only make sense for screens not behind bottom nav.
3. **Which remote sources need to be faked** — by default, fake everything that calls Supabase on the path being exercised. Each fake is one `@ContributesBinding(replaces = …)` class.

## Architecture overview

The test infra lives entirely in `:client:composeApp`'s test source sets. Module deps are already wired in `client/composeApp/build.gradle.kts`:

| Source set | Purpose |
|---|---|
| `commonTest` | Test classes (`tests/`), test DI graph (`di/`), test doubles (`fakes/`), domain fixtures (`fixtures/`), and the entrypoint + scenario plumbing (`harness/`). All under `com.plusmobileapps.chefmate.<sub-package>` |
| `jvmTest` | `di/JvmTestApplicationComponent` + `fakes/ProvideTestDatabase.jvm.kt` (actual) |
| `iosTest` | `di/IosTestApplicationComponent` + `fakes/ProvideTestDatabase.ios.kt` (actual) |
| `androidInstrumentedTest` | `di/AndroidTestApplicationComponent` + `fakes/ProvideTestDatabase.android.kt` (actual) |

`commonTest` is routed into the Android **instrumented** variant (not unit test) via the source-set-tree split in `composeApp/build.gradle.kts`:

```kotlin
androidTarget {
    compilerOptions {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.unitTest)
    }
}
```

The unit-test variant gets its own (empty) `unitTest` tree, which keeps the common UI test class out of `testDebugUnitTest` (where Robolectric isn't initialised — see "Android: instrumented tests, not Robolectric" below).

**One test class per scenario** lives in `commonTest` and runs unchanged across JVM, iOS Simulator, and Android instrumented. Per-platform variation is confined to `actual fun createTestApplicationComponent()` and `actual fun provideTestDatabase()`.

## The pattern

### 1. Test class in `commonTest`

A single `@Test` class in `commonTest` runs on JVM, iOS Simulator, and Android (instrumented) unchanged. Seed data through `FakeDatabase` and `TestUserState` rather than calling `recipeQueries` directly. The test body uses domain `Recipe` models (from `TestRecipes`) and Robots (see "Robots" section below) for any node interaction. `runRootBlocTest` is the standard wrapper — it builds the test component, applies a default `TestUserState.Authenticated`, and renders `App` against a fresh root bloc.

```kotlin
// commonTest/.../RootNavigationUiTest.kt
@OptIn(ExperimentalTestApi::class)
class RootNavigationUiTest {

    @Test
    fun clicking_recipe_in_list_navigates_to_recipe_detail() = runRootBlocTest {
        recipeList().clickRecipe(TestRecipes.fullyPopulated.title)
        recipeDetail().awaitDisplayed().assertRecipeDisplayed(TestRecipes.fullyPopulated.title)
    }
}
```

`runRootBlocTest(userState, block)` lives in `commonTest/.../CreateRootBloc.kt`:

```kotlin
fun runRootBlocTest(
    userState: TestUserState =
        TestUserState.Authenticated(recipes = listOf(TestRecipes.fullyPopulated)),
    block: suspend ComposeUiTest.(TestApplicationComponent) -> Unit,
): TestResult = runComposeUiTest {
    val app = createTestApplicationComponent()
    app.applyUserState(userState)
    setContent { App(rootBloc = app.createRootBloc()) }
    block(app)
}
```

Two things to know:

- **Return `TestResult`** (not `Unit`). `runComposeUiTest` returns `kotlinx.coroutines.test.TestResult`; on JVM it's a `typealias` for `Unit`, but on K/N it's a Promise-like value that the test runner awaits. If your wrapper drops it, K/N tests exit before the suspending body completes.
- **The component is passed to `block`** so a robot or assertion can reach back into the graph (e.g. flipping auth state mid-test via `app.testAuthenticationRepository.setState(...)`). If a test doesn't need it, ignore it: `runRootBlocTest { … }`.

For tests that need different seeded state, override the parameter: `runRootBlocTest(TestUserState.NewUser) { app -> … }`.

If a test ever needs setup that doesn't fit the wrapper (e.g. it needs to mutate the component *before* `setContent`), drop back to `runComposeUiTest { val app = createTestApplicationComponent(); … }` for that one test rather than expanding the wrapper. `createRootBloc()` is the underlying `TestApplicationComponent` extension that does the bloc wiring.

### 2. `expect fun` + abstract base graph

The graph exposes the **fake** types (not the queries) so the test can configure state via high-level helpers rather than writing raw SQL params.

```kotlin
// commonTest/.../TestApplicationComponent.kt
interface TestApplicationComponent : ApplicationComponent {
    val fakeDatabase: FakeDatabase
    val testAuthenticationRepository: TestAuthenticationRepository
}

expect fun createTestApplicationComponent(): TestApplicationComponent
```

```kotlin
// commonTest/.../BaseTestApplicationComponent.kt — extracted shared @Provides
abstract class BaseTestApplicationComponent : TestApplicationComponent {
    @Provides @SingleIn(AppScope::class)
    fun providesFakeDatabase(): FakeDatabase = FakeDatabase()

    @Provides fun providesDatabase(fakeDatabase: FakeDatabase): Database = fakeDatabase

    @Provides fun providesRecipeQueries(db: Database): RecipeQueries = db.recipeQueries
    @Provides fun providesGroceryQueries(db: Database): GroceryQueries = db.groceryQueries
    // ... five more query bindings
}
```

Metro's processor walks the class hierarchy across source sets, so the `@Provides` methods on the abstract base are picked up by the concrete `@DependencyGraph` in each platform. The `Database` binding is satisfied by the `FakeDatabase` instance, so any production code that injects `Database` or its queries gets the in-memory fake automatically.

`DateTimeFormatterUtil` is bound in production by `client/util/impl/.../DateTimeFormatterComponent.kt` (a `@ContributesTo(AppScope::class)` `@BindingContainer` object), so the test graph picks it up automatically — no test-side `@Provides` needed.

### 3. Per-platform graph (4 lines each)

```kotlin
// jvmTest/.../JvmTestApplicationComponent.kt
@SingleIn(AppScope::class)
@DependencyGraph(scope = AppScope::class, excludes = [DatabaseComponent::class])
abstract class JvmTestApplicationComponent : BaseTestApplicationComponent()

actual fun createTestApplicationComponent(): TestApplicationComponent =
    createGraph<JvmTestApplicationComponent>()
```

```kotlin
// iosTest/.../IosTestApplicationComponent.kt
@SingleIn(AppScope::class)
@DependencyGraph(scope = AppScope::class, excludes = [DatabaseComponent::class])
abstract class IosTestApplicationComponent : BaseTestApplicationComponent()

actual fun createTestApplicationComponent(): TestApplicationComponent =
    createGraph<IosTestApplicationComponent>()
```

```kotlin
// androidInstrumentedTest/.../AndroidTestApplicationComponent.kt
@SingleIn(AppScope::class)
@DependencyGraph(scope = AppScope::class, excludes = [DatabaseComponent::class])
abstract class AndroidTestApplicationComponent : BaseTestApplicationComponent()

actual fun createTestApplicationComponent(): TestApplicationComponent =
    createGraph<AndroidTestApplicationComponent>()
```

`excludes = [DatabaseComponent::class]` is required because `DatabaseComponent` provides the production `Database` from `DriverFactory`, and we provide an in-memory one ourselves. Without the exclusion, Metro reports a duplicate binding.

### 4. Fakes via `@ContributesBinding(replaces = …)`

For each Supabase-touching production class on the test's code path:

```kotlin
// commonTest/.../FakeRecipeRemoteDataSource.kt
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(scope = AppScope::class, replaces = [SupabaseRecipeRemoteDataSource::class])
class FakeRecipeRemoteDataSource : RecipeRemoteDataSource {
    override suspend fun upsertRecipe(recipe: RemoteRecipe) = recipe
    override suspend fun deleteRecipe(remoteId: String) = Unit
    override suspend fun fetchAllRecipes(ownerId: String) = emptyList<RemoteRecipe>()
}
```

For repositories that don't have a separate remote-source seam (e.g. `SupabaseAuthenticationRepository` calls `supabaseClient.auth.…` directly), replace at the repository level. Hold the underlying fake by reference so the test can flip auth state at runtime:

```kotlin
@SingleIn(AppScope::class)
@Inject
@ContributesBinding(scope = AppScope::class, replaces = [SupabaseAuthenticationRepository::class])
class TestAuthenticationRepository(
    private val fake: FakeAuthenticationRepository = FakeAuthenticationRepository(),
) : AuthenticationRepository by fake {

    fun setState(state: AuthState) = fake.setState(state)
    fun setAuthenticated(user: ChefMateUser = FakeAuthenticationRepository.fakeUser()) =
        fake.setAuthenticated(user)
}
```

`FakeAuthenticationRepository` is `final`, so use `by` delegation rather than inheritance. Re-exposing `setState` / `setAuthenticated` on the test repo lets `applyUserState` flip auth state without needing to inject the fake separately.

### 5. `FakeDatabase` — `Database` by class delegation

The `Database` binding in DI is a `FakeDatabase` that wraps `provideTestDatabase()` via class delegation, plus high-level seed helpers that take domain `Recipe` models (no raw query args in tests).

```kotlin
// commonTest/.../FakeDatabase.kt
class FakeDatabase(private val delegate: Database = provideTestDatabase()) : Database by delegate {
    fun addRecipe(recipe: Recipe) {
        recipeQueries.create(
            title = recipe.title,
            description = recipe.description,
            ingredients = recipe.ingredients.takeIf { it.isNotEmpty() },
            directions = recipe.directions.takeIf { it.isNotEmpty() },
            // …
            createdAt = recipe.createdAt.toString(),
            updatedAt = recipe.updatedAt.toString(),
            clientId = null,
            ownerId = null,
        )
    }
    fun addRecipes(recipes: Iterable<Recipe>) = recipes.forEach(::addRecipe)
    fun addRecipes(vararg recipes: Recipe) = recipes.forEach(::addRecipe)
    fun clearRecipes() = recipeQueries.deleteAll()
}
```

The DB schema only requires `title`; the domain model also requires `ingredients` and `directions` to be non-null Strings (empty is valid). Pass empty strings for the title-only minimum; pass `null` to the SQL bind to leave the column NULL when the domain field is empty.

#### `provideTestDatabase()` — per-platform driver

`provideTestDatabase()` is an `expect fun` (commonTest) with a one-line `actual` per platform. JVM and iOS reuse the existing `client/database/testing` helper; Android needs `AndroidSqliteDriver` because `JdbcSqliteDriver` would try to load `libsqlitejdbc.so` and fail on a real device.

```kotlin
// commonTest/.../ProvideTestDatabase.kt
expect fun provideTestDatabase(): Database

// jvmTest/.../ProvideTestDatabase.jvm.kt
actual fun provideTestDatabase(): Database = createTestDatabase()

// iosTest/.../ProvideTestDatabase.ios.kt
actual fun provideTestDatabase(): Database = createTestDatabase()

// androidInstrumentedTest/.../ProvideTestDatabase.android.kt
actual fun provideTestDatabase(): Database {
    val driver = AndroidSqliteDriver(
        schema = Database.Schema,
        context = ApplicationProvider.getApplicationContext(),
        name = null, // in-memory
    )
    return Database(driver)
}
```

Add `libs.sqldelight.drivers.android` and `libs.androidx.test.core` (for `ApplicationProvider`) to `androidInstrumentedTest.dependencies` in `composeApp/build.gradle.kts`.

### 6. `TestRecipes` — shared fixtures with field permutations

```kotlin
// commonTest/.../TestRecipes.kt
object TestRecipes {
    val fullyPopulated: Recipe = Recipe(/* all fields populated */)
    val withoutDescriptionOrImage: Recipe = Recipe(/* description = null, imageUrl = null */)
    val ingredientsAndDirectionsOnly: Recipe = Recipe(/* no metadata, no image, no rating */)
    val titleOnly: Recipe = Recipe(/* title only — ingredients/directions empty Strings */)
    val defaults: List<Recipe> = listOf(fullyPopulated, withoutDescriptionOrImage, ingredientsAndDirectionsOnly, titleOnly)
}
```

The four permutations exercise the recipe-list rendering path for full → minimum field coverage. Tests that target one specific case use the named field directly; broader smoke tests use `TestRecipes.defaults`.

### 7. `TestUserState` + `applyUserState` — high-level scenarios

```kotlin
// commonTest/.../TestUserState.kt
sealed interface TestUserState {
    data class Authenticated(val recipes: List<Recipe> = TestRecipes.defaults) : TestUserState
    data object NewUser : TestUserState
    data class UnauthenticatedWithRecipes(val recipes: List<Recipe> = TestRecipes.defaults) : TestUserState
}

fun TestApplicationComponent.applyUserState(state: TestUserState) {
    when (state) {
        is TestUserState.Authenticated -> {
            testAuthenticationRepository.setAuthenticated()
            fakeDatabase.addRecipes(state.recipes)
        }
        TestUserState.NewUser -> testAuthenticationRepository.setAuthenticated()
        is TestUserState.UnauthenticatedWithRecipes -> fakeDatabase.addRecipes(state.recipes)
    }
}
```

`applyUserState` is the test's only setup call after building the component. Add new scenarios as new `TestUserState` arms; don't reach into `fakeDatabase` / `testAuthenticationRepository` from the test body unless the scenario truly is one-off.

### 8. Robots — one module per screen

UI tests interact with screens via Robot classes that live in a dedicated `impl-robots` module per screen. The robot owns the semantics-tree lookups and exposes a domain-level vocabulary (`clickRecipe(title)`, `awaitDisplayed()`, `assertRecipeDisplayed(title)`) so test bodies stay readable and shareable across scenarios.

**Module layout** — `client/<feature>/<area>/impl-robots`, plain KMP library:

```kotlin
// client/recipe/list/impl-robots/build.gradle.kts
plugins {
    alias(libs.plugins.kmpLibrary)
    alias(libs.plugins.compose)
}

kotlin {
    sourceSets { commonMain.dependencies { implementation(projects.client.recipe.list.public) } }
}

plusLibrary {
    namespace = "com.plusmobileapps.chefmate.recipe.list.robots"
    uiTest = true
}
```

`uiTest = true` is what wires the `compose.uiTest` `api` dependency into `commonMain` — no manual `@OptIn(ExperimentalComposeLibrary::class)` needed.

**Robot class** — takes `ComposeUiTest` in its constructor; an entry-point extension on `ComposeUiTest` is the only way tests construct it:

```kotlin
@file:OptIn(ExperimentalTestApi::class)

class RecipeListRobot(private val test: ComposeUiTest) {

    private val onScreen = hasAnyAncestor(hasTestTag(RecipeListTestTags.SCREEN))

    fun assertRecipeIsDisplayed(title: String): RecipeListRobot = apply {
        test.onNode(hasText(title) and onScreen).assertIsDisplayed()
    }

    fun clickRecipe(title: String): RecipeListRobot = apply {
        test.onNode(hasText(title) and onScreen).assertIsDisplayed().performClick()
    }
}

fun ComposeUiTest.recipeList(): RecipeListRobot = RecipeListRobot(this)
```

**Scope every lookup to the screen** via `hasAnyAncestor(hasTestTag(<Feature>TestTags.SCREEN))`. Without this, a recipe title rendered on the detail header satisfies `onNodeWithText(title)` from a list-robot call and the matcher silently picks the wrong node. The screen test tag itself is the one exception (asserted directly on the root container).

**Wire each robot module into `composeApp/build.gradle.kts`** under `commonTest.dependencies`:

```kotlin
implementation(projects.client.recipe.core.implRobots)
implementation(projects.client.recipe.list.implRobots)
```

### 9. Add a `testTag` to the screen root

Robots find their screen via a stable, typed test-tag constant. Add `Modifier.testTag(<Feature>TestTags.SCREEN)` to the outer container of every screen a robot covers, with the constant defined in the screen's `:public` module so the screen and its robot can never disagree on the tag string.

```kotlin
// client/recipe/core/public/.../RecipeDetailTestTags.kt
object RecipeDetailTestTags {
    const val SCREEN: String = "recipe_detail_screen"
}

// in the screen composable
Box(modifier = modifier.fillMaxSize().testTag(RecipeDetailTestTags.SCREEN)) { … }
```

Use semantic, screen-level tags (`recipe_detail_screen`, not `box_42`).

## Android: instrumented tests, not Robolectric

The `commonTest` `@Test` class can't run under Robolectric (`testDebugUnitTest`):

1. **`runComposeUiTest` needs Robolectric** to stub `Build.FINGERPRINT` etc. Robolectric only initialises when JUnit4 sees `@RunWith(RobolectricTestRunner::class)` (or `@RunWith(AndroidJUnit4::class)`) **directly** on the test class. Meta-annotations don't work — `RunnerBuilder` calls `klass.getAnnotation(RunWith.class)`, which returns `null` for meta-annotated classes. And `@RunWith` can't live in `commonTest` because `org.junit.runner.RunWith` is JVM-only.
2. Even with Robolectric initialised, `runComposeUiTest` internally calls `ActivityScenario.launch(ComponentActivity::class.java)`. Robolectric 4.12+ rejects this with `Unable to resolve activity … see robolectric/pull/4736`.

**The right path is instrumented tests** — they run on a real emulator/device where the activity is registered with the real Android `PackageManager`, not a Robolectric shadow.

```kotlin
// composeApp/build.gradle.kts
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.unitTest)
        }
    }
    // …
}

android {
    defaultConfig {
        // …
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

sourceSets {
    val androidInstrumentedTest by getting {
        dependencies {
            implementation(libs.sqldelight.drivers.android)
            implementation(libs.androidx.test.core)
        }
    }
}

dependencies {
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.android)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

`instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)` routes `commonTest` into the Android instrumented test APK, so the same `@Test` class runs unchanged on JVM, iOS Simulator, and Android. `unitTestVariant.sourceSetTree.set(KotlinSourceSetTree.unitTest)` then keeps the common UI test class out of `testDebugUnitTest` (where Robolectric isn't initialised) — without it, the task picks up the same class and NPEs on `Build.FINGERPRINT`. With both set, `testDebugUnitTest` is safe to run; it just doesn't see the common UI test.

`androidInstrumentedTest` needs `sqldelight-drivers-android` (for `AndroidSqliteDriver` in `provideTestDatabase()`) and `androidx-test-core` (for `ApplicationProvider.getApplicationContext()`).

CI runs `./gradlew :client:composeApp:connectedDebugAndroidTest` against an emulator — `reactivecircus/android-emulator-runner@v2` on a Linux runner with KVM.

## Gotchas — do not skip

1. **Instant string format** — `FakeDatabase.addRecipe` calls `recipe.createdAt.toString()`, which produces ISO-8601 with the required `Z` suffix when the input is a real `kotlin.time.Instant`. If you build a fixture by hand and pass a raw string for `createdAt`, it must end in `Z` — `RecipeRepositoryImpl.toRecipe` calls `Instant.parse` and throws `InstantFormatException` otherwise (the recipe list silently shows empty).
2. **Don't override `SupabaseModule`** — leaving the real `createSupabaseClient()` in place is fine as long as every production caller on the test's code path is faked. The client is constructed lazily and never makes network calls if nothing invokes it.
3. **iOS test linker** — `iosTarget.binaries.withType<TestExecutable> { linkerOpts.add("-lsqlite3") }` is required in `composeApp/build.gradle.kts` (the framework block already has `-lsqlite3`, but test executables are a separate binary). Without this, linking fails with `Undefined symbols: _sqlite3_*`.
4. **JVM DB pollution** — never use the production `DriverFactory` in tests. The JVM driver writes to `~/Library/Application Support/Chef Mate/chefmate.db` (the developer's real desktop-app DB). Always exclude `DatabaseComponent` and provide `provideTestDatabase()` instead.
5. **Settings on iOS** — `Settings()` on iOS is `NSUserDefaults`, which persists across simulator runs. With a single test it doesn't matter; once a second test exists, swap `SettingsComponent` for `MapSettings()` via the same `excludes = […]` mechanism.
6. **`FakeAuthenticationRepository` is final** — you cannot subclass it. Use `by` delegation in `TestAuthenticationRepository`.
7. **`expect fun createTestApplicationComponent()` must be top-level** — declaring it inside `TestApplicationComponent` won't compile because Metro's `createGraph<T>()` only works on the platform-specific concrete graph class.

## Verification checklist

Before reporting done:

1. `./gradlew ktfmtFormat` — pre-commit hook runs this anyway.
2. `./gradlew :client:composeApp:jvmTest --tests "*RootNavigationUiTest"` — fastest, run first.
3. `./gradlew :client:composeApp:iosSimulatorArm64Test` — Apple Silicon Mac required.
4. `./gradlew :client:composeApp:connectedDebugAndroidTest` — needs an emulator or attached device.

## Reference implementation in this repo

Test sources are split into semantic sub-packages — `tests/` for `@Test` classes, `di/` for the test graph, `fakes/` for production-binding replacements, `fixtures/` for sample domain data, `harness/` for the test entrypoint and scenario plumbing.

- `client/composeApp/src/commonTest/.../tests/RootNavigationUiTest.kt` — single `@Test` class (runs on JVM, iOS Simulator, and Android instrumented)
- `client/composeApp/src/commonTest/.../di/TestApplicationComponent.kt` — interface + `expect fun createTestApplicationComponent()`
- `client/composeApp/src/commonTest/.../di/BaseTestApplicationComponent.kt` — abstract base with all shared `@Provides`
- `client/composeApp/src/commonTest/.../harness/CreateRootBloc.kt` — `runRootBlocTest` wrapper + `TestApplicationComponent.createRootBloc()` primitive
- `client/composeApp/src/commonTest/.../harness/TestUserState.kt` — sealed scenarios + `applyUserState` extension
- `client/composeApp/src/commonTest/.../fakes/FakeDatabase.kt` — `Database by provideTestDatabase()` with `addRecipe` / `clearRecipes`
- `client/composeApp/src/commonTest/.../fakes/ProvideTestDatabase.kt` — `expect fun` for the per-platform driver, with three `actual` files (`jvmTest/.../fakes/`, `iosTest/.../fakes/`, `androidInstrumentedTest/.../fakes/`)
- `client/composeApp/src/commonTest/.../fakes/FakeRecipeRemoteDataSource.kt`, `fakes/TestAuthenticationRepository.kt` — fake bindings
- `client/composeApp/src/commonTest/.../fixtures/TestRecipes.kt` — `Recipe` permutations (full → title-only)
- `client/composeApp/src/{jvm,ios,androidInstrumented}Test/.../di/*TestApplicationComponent.kt` — per-platform graph + `actual fun`
- `client/recipe/list/impl-robots/.../RecipeListRobot.kt` + `client/recipe/list/public/.../RecipeListTestTags.kt` — robot + test-tag pair
- `client/recipe/core/impl-robots/.../RecipeDetailRobot.kt` + `client/recipe/core/.../detail/RecipeDetailTestTags.kt` — robot + test-tag pair
- `client/util/impl/.../DateTimeFormatterComponent.kt` — production `@ContributesTo(AppScope)` binding picked up by the test graph
- `client/composeApp/build.gradle.kts` — source-set-tree split (instrumented/unit), iOS test linker flag, robot-module wiring under `commonTest.dependencies`
- `.github/workflows/compose-ui-tests.yml` — three-job CI workflow (jvm / android / ios) with `dorny/test-reporter` per platform
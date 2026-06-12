# Developer Settings

A debug-only screen reachable from the bottom of the **More** tab that lets you switch the active backend environment and sign in as one of several pre-configured test users without typing credentials. Lives in the `client/developer-settings` module pair and is hidden from release builds via `BuildConfig.IS_DEBUG`.

## What it does

| Setting | Effect |
|---|---|
| **Environment** | Switches between `PROD`, `TESTING`, and `FAKE`. Persisted via `multiplatform-settings`. Selecting a new env signs you out, wipes the local DB (recipes, grocery, meal plans), seeds `Recipe.Samples` if `FAKE` is chosen, and prompts for an app restart so the Supabase client rebinds. |
| **Login as test user** | Lists pre-baked users sourced from Gradle env vars / properties and signs in via the existing Supabase auth repo. Persists which user is selected so the screen can show "currently signed in as User N" across restarts. If the credentials are wrong, an error dialog surfaces the Supabase error message. |
| **Sign out** | Visible only when authenticated. Signs the test user out **and** wipes the local DB so the next user starts from a clean cache. |

## Configuring the environment URLs

The Supabase URL/key for **TESTING** is read at build time. PROD already has its values (see [buildconfig-setup.md](buildconfig-setup.md) for the existing `supabase.url` / `supabase.key`). FAKE re-uses PROD.

> **Recommended TESTING backend:** a **local Supabase stack** (Docker) rather than a hosted staging project — it's free, isolated from prod, and works offline. See [supabase-local-development.md](supabase-local-development.md) for the full setup, including per-platform networking (`localhost` vs `10.0.2.2`) and seeded test users. The values below apply to either a local stack or a hosted staging project.

### `local.properties`

```properties
supabase.testing.url=https://your-staging-project.supabase.co
supabase.testing.key=your-staging-anon-public-key
```

### Environment variables

```bash
export SUPABASE_TESTING_URL=https://your-staging-project.supabase.co
export SUPABASE_TESTING_KEY=your-staging-anon-public-key
```

If `supabase.testing.*` is not set, the build falls back to the PROD values, so `TESTING` will be a no-op until you provide them.

## Configuring test users

Users are detected by **incrementing `n`** (starting at 1) until a pair (email + password) is missing. Provide as many as you like.

> **Heads-up on key naming:** `local.properties` and `gradle.properties` use **dotted lower-case keys** (`chefmate.user.1`, `chefmate.user.password.1`). The `CHEF_MATE_USER_1` / `CHEF_MATE_USER_PASSWORD_1` form is for **shell environment variables only** — putting that form in `local.properties` will silently do nothing because Java's `Properties` loader doesn't translate the underscored uppercase form into a dotted key.

Lookup order per index, first match wins:

1. `local.properties` (project root) — key form `chefmate.user.<n>` / `chefmate.user.password.<n>`
2. Gradle properties (`~/.gradle/gradle.properties`, `-P` flags, etc.) — same dotted key form
3. Environment variables — uppercase form `CHEF_MATE_USER_<n>` / `CHEF_MATE_USER_PASSWORD_<n>`

### `local.properties` (recommended for local-only secrets, gitignored)

```properties
chefmate.user.1=alice@chefmate.test
chefmate.user.password.1=hunter2
chefmate.user.2=bob@chefmate.test
chefmate.user.password.2=hunter3
```

After editing `local.properties`, re-run the JVM/Android target so Gradle regenerates `BuildConfig.TEST_USERS`:

```bash
./gradlew :client:composeApp:run            # desktop
./gradlew :client:composeApp:installDebug   # Android
```

### `~/.gradle/gradle.properties` (machine-wide, outside any repo)

```properties
chefmate.user.1=alice@chefmate.test
chefmate.user.password.1=hunter2
```

### Environment variables (CI / one-off shells)

```bash
export CHEF_MATE_USER_1=alice@chefmate.test
export CHEF_MATE_USER_PASSWORD_1=hunter2
export CHEF_MATE_USER_2=bob@chefmate.test
export CHEF_MATE_USER_PASSWORD_2=hunter3
```

### How it's plumbed

The build script in `client/shared/build.gradle.kts` collects every contiguous user pair and serializes them as a single BuildKonfig string field (`TEST_USERS = "email1|password1;email2|password2"`). At runtime, `TestUserProvider` parses the string and exposes a `List<TestUser>` to the dev-settings ViewModel. **A gap (e.g. `_1` defined, `_2` missing, `_3` defined) stops collection at the first miss** — define users contiguously.

## Debug-build gating

The "Developer Settings" row only appears when `BuildConfig.IS_DEBUG` is `true`. The flag is derived at configuration time from `gradle.startParameter.taskNames`: any task containing `Release` flips it to `false`. This is a UI-gate, not a security boundary — the dev-settings code itself ships in every binary.

| Build path | `IS_DEBUG` |
|---|---|
| `./gradlew :client:composeApp:installDebug` | `true` |
| `./gradlew :client:composeApp:run` (desktop) | `true` |
| `./gradlew :client:composeApp:packagePkg` (desktop debug package) | `true` |
| `./gradlew :client:composeApp:bundleRelease` | `false` |
| `./gradlew :client:composeApp:packageReleasePkg` (desktop release package) | `false` |
| IDE sync (no tasks) | `true` |

> **Desktop packaging caveat:** the plain `packagePkg` / `packageMsi` / `packageDeb` tasks do **not**
> contain "Release", so they ship with the developer UI visible. The desktop release workflow uses the
> `packageRelease*` variants (output under `build/compose/binaries/main-release/`) precisely so
> `IS_DEBUG` is `false`. See [deployment.md](deployment.md).

## Architecture notes

- **`client/shared`** owns the `Environment` enum and the read-only `EnvironmentProvider` contract, so production code (e.g. `SupabaseModule`) doesn't depend on the dev-settings module.
- **`client/developer-settings/public`** owns the `DeveloperSettingsBloc`, screen, and `DeveloperPreferences` interface (which extends `EnvironmentProvider`).
- **`client/developer-settings/impl`** provides `DeveloperPreferencesImpl` (russhwolf-`Settings`-backed), `TestUserProvider` (parses the BuildKonfig string), `FakeRecipeSeeder` (inserts `Recipe.Samples`), and the BLoC + ViewModel that orchestrate env changes.
- The Supabase client is `@SingleIn(AppScope::class)` and binds the URL/key at first injection. **Switching env requires an app restart** — the screen surfaces a dialog telling the user to do so.

## Verification checklist

1. Add at least one `chefmate.user.1` / `chefmate.user.password.1` and (optionally) `supabase.testing.*` to one of the lookup sources above.
2. `./gradlew :client:composeApp:installDebug` — open the More tab, confirm "Developer Settings" appears at the bottom.
3. Switch Environment → TESTING. Verify: signed out, recipe list empty, restart-required dialog. Force-stop + reopen, confirm sync now hits the testing URL.
4. Switch Environment → FAKE. Verify several seeded recipes appear after the wipe.
5. Tap "Login as test user" → User 1. Verify signed in; user persists across an app restart.
6. Tap "Login as test user" with a deliberately bad password (edit `local.properties`, rebuild). Verify an error dialog appears with the Supabase message.
7. Tap "Sign out". Verify recipes / meal plan / grocery list are wiped locally.
8. Build a release APK. Confirm the row is absent.

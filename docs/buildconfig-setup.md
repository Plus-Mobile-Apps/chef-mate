# BuildConfig Setup Guide

This project uses [BuildKonfig](https://github.com/yshrsmz/BuildKonfig) to securely manage build configuration variables like Supabase credentials.

## Configuration

### Option 1: Using Gradle properties (Recommended for local development)

1. Open (or create) `~/.gradle/gradle.properties` — keep real credentials out of the repo's `gradle.properties`
2. Add your Supabase credentials:

```properties
supabase.url=https://your-project-id.supabase.co
supabase.key=your-anon-public-key
bugsnag.apiKey=your-bugsnag-api-key
```

You can also pass them per build with `-Psupabase.url=...`. **Note:** these keys are read with Gradle's `findProperty`, so putting them in the project's `local.properties` has no effect — `local.properties` is only read for the developer-menu test users (`chefmate.user.*`, see [developer-settings.md](developer-settings.md)).

### Option 2: Using Environment Variables (Recommended for CI/CD)

Set the following environment variables:

```bash
export SUPABASE_URL=https://your-project-id.supabase.co
export SUPABASE_KEY=your-anon-public-key
export BUGSNAG_API_KEY=your-bugsnag-api-key
```

### Priority

The configuration uses the following priority (first found wins):
1. Gradle properties (`~/.gradle/gradle.properties`, `-P` flags, etc.)
2. Environment variables
3. Default placeholder values (will not work with real Supabase; `BUGSNAG_API_KEY` defaults to an empty string)

## Finding Your Supabase Credentials

1. Go to your [Supabase Dashboard](https://app.supabase.com/)
2. Select your project
3. Go to **Settings** → **API**
4. Copy:
   - **Project URL** → Use as `supabase.url` or `SUPABASE_URL`
   - **Project API key (anon public)** → Use as `supabase.key` or `SUPABASE_KEY`

## Usage in Code

The BuildConfig class is generated in the `client:shared` module and can be accessed from any module that depends on it:

```kotlin
import com.plusmobileapps.chefmate.buildconfig.BuildConfig

val supabaseUrl = BuildConfig.SUPABASE_URL
val supabaseKey = BuildConfig.SUPABASE_KEY
val bugsnagApiKey = BuildConfig.BUGSNAG_API_KEY
```

Generated fields: `SUPABASE_URL`, `SUPABASE_KEY`, `SUPABASE_PROD_URL`, `SUPABASE_PROD_KEY`, `SUPABASE_TESTING_URL`, `SUPABASE_TESTING_KEY`, `BUGSNAG_API_KEY`, `TEST_USERS`, `IS_DEBUG` (false only when the requested Gradle task name contains "Release" or Xcode's `CONFIGURATION` is `Release`), and `VERSION_NAME` (mirrored from `versionName` in `client/composeApp/build.gradle.kts`).

## Adding New Configuration Values

To add new build configuration values, edit `client/shared/build.gradle.kts`:

```kotlin
buildkonfig {
    packageName = "com.plusmobileapps.chefmate.buildconfig"
    objectName = "BuildConfig"
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        buildConfigField(STRING, "SUPABASE_URL", supabaseUrl)
        buildConfigField(STRING, "SUPABASE_KEY", supabaseKey)
        // Add your new field here
        buildConfigField(STRING, "NEW_FIELD", newValue)
    }
}
```

## Security Notes

- **Never commit real credentials** - keep them in `~/.gradle/gradle.properties` or environment variables, not the checked-in `gradle.properties` (`local.properties` is gitignored, but BuildKonfig doesn't read Supabase/Bugsnag keys from it)
- The anon/public key is safe to use in client-side code (it's meant to be public)
- For sensitive operations, always use Row Level Security (RLS) in your Supabase database

## Related

- [developer-settings.md](developer-settings.md) — how to wire up the staging Supabase URL (`supabase.testing.*` / `SUPABASE_TESTING_*`) and pre-baked test users for the in-app developer menu.

# Feature-Flag Admin Dashboard

A standalone Compose app (`:admin`) for managing the Supabase `feature_flags` table — create, edit, archive, and delete flags without writing SQL. It targets **wasmJs** (a static web app you can reach from a phone) and **jvm** (fast local dev + unit tests).

This is the write side of the [feature-flag system](./feature-flags.md); the shipping app only ever reads flags.

## Why it's a separate, self-contained module

The `kmpLibrary` convention plugin hardcodes Android + iOS + JVM, and no shared `client/*` module exposes a wasmJs target, so they can't be consumed from the web. Rather than add wasmJs across the whole UI stack, `:admin` applies the Compose + KMP plugins directly and is fully self-contained: plain Material 3, its own model, its own Supabase client, manual wiring (no Metro/Decompose). This keeps the wasm target isolated and leaves the shipping app's build untouched.

## Backend setup (one time)

1. **Apply the migration** `supabase/migrations/20260604_feature_flags_admin.sql` (Supabase Studio SQL editor or `supabase db push`). It versions the `feature_flags` table, adds the admin-only columns (`archived`, `description`, `created_at`, `updated_at` + trigger), creates the `admins` allow-list table, and installs RLS.

2. **Seed yourself as an admin.** Sign in to the dashboard once (this creates your `auth.users` row), then run in the SQL editor:

   ```sql
   insert into admins (user_id)
   select id from auth.users where email = 'you@example.com'
   on conflict do nothing;
   ```

   Without a row in `admins`, sign-in works but every write is rejected by RLS.

### Security model

The web bundle ships the Supabase **anon** key — that's fine, because authorization is enforced server-side by RLS, not key secrecy:

- Anyone (incl. the anon client) can read **non-archived** flags. Admins additionally read archived rows.
- Only members of `admins` can insert / update / delete.

Membership is checked through a `SECURITY DEFINER` `public.is_admin()` function. It runs as the table owner and so bypasses RLS on `admins` — querying `admins` directly inside a policy on `admins` would otherwise recurse infinitely (Postgres `42P17`).

## Run, build, deploy

```bash
# Local dev — web (opens a browser tab)
./gradlew :admin:wasmJsBrowserDevelopmentRun

# Local dev — desktop (fastest iteration)
./gradlew :admin:run

# Unit tests (run on the jvm target)
./gradlew :admin:jvmTest

# Production web bundle -> admin/build/dist/wasmJs/productionExecutable
./gradlew :admin:wasmJsBrowserDistribution
```

The Supabase URL + anon key come from the same build inputs as the app (`supabase.url` / `supabase.key` Gradle properties or `SUPABASE_URL` / `SUPABASE_KEY` env vars), wired via buildkonfig in `admin/build.gradle.kts`.

The `Admin Pages` workflow (`.github/workflows/admin-pages.yml`) builds this bundle with the `SUPABASE_URL` / `SUPABASE_KEY` repo secrets and deploys it to this repo's GitHub Pages (https://plus-mobile-apps.github.io/chef-mate/) on every push to `main` touching `admin/**`, or on manual dispatch. One-time setup: **Settings → Pages → Source = GitHub Actions**. For a custom domain (e.g. `admin.chefmate.plusmobileapps.com`), add a CNAME DNS record to `plus-mobile-apps.github.io` and set the domain under Settings → Pages.

The bundle is plain static files, so any other **HTTPS** host works too (HTTPS is required for Supabase auth).

## Signing in

Email one-time-code (OTP), the same passwordless flow as the app: enter your admin email, then the 6-digit code that's emailed to you. The session gates the dashboard; writes carry your JWT and are authorized by RLS.

## Using the dashboard

The flag list shows each flag's state (`enabled`/`disabled`, `type=value`, rollout %, platforms, user count, `ARCHIVED`). A **Show archived** toggle reveals retired flags. Each row has **Edit**, **Archive/Unarchive**, and **Delete** (delete asks for confirmation). **New flag** opens the editor.

### Editor fields

| Field | Maps to | Notes |
|---|---|---|
| Key | `key` | snake_case identifier; **immutable once created** (it's the primary key). |
| Type | `value_type` | Boolean or String. |
| Value | `value` | A switch for booleans (`true`/`false`), a text field for strings. |
| Enabled | `enabled` | Global kill switch. Off ⇒ everyone gets the in-code default. |
| Rollout | `rollout_percent` | 0–100% of users (stable per key+identity). |
| Platforms | `platforms` | None selected = all. Otherwise android/ios/desktop/web. |
| Min / Max version | `min_version` / `max_version` | Optional semver bounds. |
| User allowlist | `user_ids` | Supabase user ids (one per line / comma-separated) who get the flag **on top of** the rollout. |
| Description | `description` | Admin note; not read by the client. |

### Recipe: enable a flag for one specific user only

The allowlist is *additive* on the rollout, so to target exactly one person:

1. **Enabled** → ON (the kill switch must be on).
2. **Rollout** → **0%** (nobody is included by percentage).
3. **User allowlist** → that person's Supabase **user id** (UUID, from Authentication → Users — not their email).
4. Leave **Platforms** empty (or restrict if you also want a platform gate).
5. **Save**.

Result: 0% includes nobody, and the allowlist adds back only the listed user(s). For "this user **plus** a small % of everyone else," set the rollout above 0 instead.

Caveats: targeting matches the **signed-in** Supabase user id (logged-out clients bucket by a device UUID that's never on the list), and the allowlist still respects `enabled`, `platforms`, and version bounds.

### Archive vs delete

- **Archive** keeps the row but hides it from clients (the client read filters `archived = false`) and from the default list. Use it to retire a flag without losing history; **Unarchive** brings it back.
- **Delete** permanently removes the row.

## Troubleshooting

- **`42P17 infinite recursion detected in policy for relation "admins"`** — the `is_admin()` helper isn't in place. Re-apply the policy section of the migration.
- **Writes fail / list is empty after signing in** — you're authenticated but not in `admins`. Run the seed query above; check with `select public.is_admin();` (should return `true`).
- **`syntax error` running the migration** — make sure you're running the committed file; it deliberately contains no commented-out SQL or semicolons inside comments (some SQL runners mis-split those).

## Files of interest

| Concern | Path |
|---|---|
| Module build (targets, buildkonfig) | `admin/build.gradle.kts` |
| App entry + session gating | `admin/src/commonMain/.../AdminApp.kt` |
| Sign-in (OTP) | `admin/src/commonMain/.../SignInScreen.kt` + `AdminAuth.kt` |
| List + actions | `admin/src/commonMain/.../DashboardScreen.kt` |
| Editor + validation | `admin/src/commonMain/.../FlagEditorScreen.kt` + `FlagDraft.kt` |
| CRUD repository | `admin/src/commonMain/.../FeatureFlagAdminRepository.kt` |
| Schema + RLS migration | `supabase/migrations/20260604_feature_flags_admin.sql` |

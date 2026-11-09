# Feature Flags

Lightweight remote-config / feature-flag system that works on Android, iOS, and Desktop (JVM). Rules are stored in Supabase; values are fetched on app start, cached in `multiplatform-settings`, and merged with optional dev-only overrides at evaluation time.

## Backend

A single Postgres table in your existing Supabase project. Run this once via the SQL editor in Supabase Studio:

```sql
create table public.feature_flags (
    key text primary key,
    value_type text not null check (value_type in ('bool', 'string')),
    value text not null,
    enabled boolean not null default true,
    rollout_percent int not null default 0 check (rollout_percent between 0 and 100),
    platforms text[],
    min_version text,
    max_version text,
    user_ids text[],
    updated_at timestamptz not null default now()
);

alter table public.feature_flags enable row level security;

create policy "feature_flags read" on public.feature_flags
    for select to anon, authenticated using (true);
```

Writes go through the service role / Studio UI — clients only ever read. For a UI instead of raw SQL, use the [feature-flag admin dashboard](./feature-flag-admin.md) (the `:admin` module), which also manages archiving and per-user targeting. Apply its migration (`supabase/migrations/20260604_feature_flags_admin.sql`) too: it adds the `archived` column, and the client read filters on `archived = false`, so the fetch fails without it.

### Columns

- `key` — must match one of the `FeatureFlag.key` values defined in code.
- `value_type` — `'bool'` or `'string'`. If this disagrees with the in-code flag type, the client falls back to the default.
- `value` — for `bool` flags use `'true'` / `'false'`. For `string` flags, the literal value.
- `enabled` — global kill switch. `false` means everyone gets the default.
- `rollout_percent` — % of users for whom the flag is "active". Active users get `value`; the rest get the default. Bucketing is stable per (`key`, identity) and uses FNV-1a 32-bit so Android, iOS, and JVM agree.
- `platforms` — `null` / empty = all platforms. Otherwise an array of `'ANDROID'` / `'IOS'` / `'JVM'`.
- `min_version` / `max_version` — optional semver bounds, compared against the app's current `versionName` (only major/minor/patch are used; `-pre` suffixes are ignored).
- `user_ids` — `null` / empty = no allowlist. Otherwise an array of authenticated Supabase user ids who get the flag **regardless of the rollout bucket** (additive on top of `rollout_percent` — useful for force-enabling QA/beta users on a partial rollout). Allowlisted users still respect `enabled`, `platforms`, and the version bounds. Only takes effect for signed-in users: a logged-out client buckets by a device UUID that is never on the list (see Identity / bucketing below).

## Adding a flag

1. Add the flag to `FeatureFlagRegistry` in `client/featureflag/public/.../FeatureFlag.kt`:

   ```kotlin
   object MyNewFlag : BooleanFlag("my_new_flag", defaultValue = false, "What this flag controls.")
   ```

   Append it to `all`. The `defaultValue` is what every client returns when offline / before the first refresh / when no row exists — pick the safer of the two outcomes.

2. Insert a row in the `feature_flags` table with the same key.

3. Read the flag from any Bloc / repository:

   ```kotlin
   class MyViewModel(featureFlags: FeatureFlags) {
       val showNewFlow: StateFlow<Boolean> = featureFlags.isEnabled(FeatureFlagRegistry.MyNewFlag)
   }
   ```

   For string flags use `featureFlags.valueOf(flag)` directly.

## Refresh model

`FeatureFlags.refresh()` is called once on app start from `RootBlocImpl.init`. On failure (no network, malformed response) the call swallows the exception and the client keeps using whatever was in the local cache (or the in-code defaults if the cache is empty). There is no periodic poll — flags propagate on next cold start.

## Identity / bucketing

The bucket id is the authenticated Supabase user id when signed in, otherwise a UUID generated on first launch and stored in `multiplatform-settings` under `flags.bucket_id`. Signing in causes the bucket id to switch from the device UUID to the user id; a flag at 50% rollout may flip values across that boundary. This is intentional — once signed in a user gets the same experience across devices.

## Dev overrides

`FeatureFlagOverrides` lets a developer force any flag ON / OFF / Default. Overrides are persisted in `multiplatform-settings` under `flags.override.<key>.{present,value}`. Resolution order is **override → remote+rollout → in-code default**.

The override mechanism works in any build, but the UI for setting overrides is the Feature Flags screen (`FeatureFlagsBloc`) opened from the [developer-settings](./developer-settings.md) menu, which is itself gated by `isDebugBuild`. To open it manually for a one-off test, call `FeatureFlagOverrides.setOverride(flag, Override.ForceValue(true))` from anywhere with DI access.

## Files of interest

| Concern | Path |
|---|---|
| Public API + flag registry + overrides screen | `client/featureflag/public/` |
| Repo, evaluator, cache, overrides | `client/featureflag/impl/` |
| Fake for other modules' tests | `client/featureflag/testing/` |
| Stable cross-platform hash | `client/featureflag/impl/.../Fnv1a.kt` |
| App-start refresh hook | `client/root/impl/.../RootBlocImpl.kt` (init block) |
| `BuildConfig.VERSION_NAME` source | `client/shared/build.gradle.kts` (mirrors `versionName` from `client/composeApp/build.gradle.kts`) |

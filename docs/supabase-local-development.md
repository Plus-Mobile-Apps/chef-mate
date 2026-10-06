# Local Supabase Development

Develop against a **fully local Supabase stack** (Postgres + Auth + Storage + Edge Functions +
Studio, all in Docker) instead of testing against the production project. The app already has a
`TESTING` environment baked in — this guide stands up a local backend and points `TESTING` at it.

> **Status:** the CLI config, seed data and client wiring are in place, but `supabase db reset`
> does **not** succeed yet — the migration history is missing the base tables and has duplicate
> version prefixes. See [Known blockers](#️-known-blockers-before-db-reset-works) for exactly
> what's wrong and how to fix each one. The rest of the guide is accurate once those are cleared.

> **TL;DR** (after the blockers are resolved)
> ```bash
> supabase start                 # boot the local stack (first run pulls Docker images)
> supabase status                # copy the API URL + anon key
> # put them in local.properties as supabase.testing.* (see below), then:
> ./gradlew :client:composeApp:run            # desktop, talks to localhost:54321
> ```
> In the app: **More → Developer Settings → Environment → TESTING**, then restart.

---

## Why this works without app code changes

The client already has the wiring:

- `Environment` enum (`PROD` / `TESTING` / `FAKE`) + `EnvironmentProvider` in `client/shared`.
- `SupabaseModule` reads `BuildConfig.SUPABASE_TESTING_URL` / `_KEY` when the active env is
  `TESTING` ([SupabaseModule.kt](../client/auth/data/impl/src/commonMain/kotlin/com/plusmobileapps/chefmate/auth/data/impl/SupabaseModule.kt)).
- BuildKonfig reads `supabase.testing.url` / `supabase.testing.key` (falling back to the prod
  values if unset) — see [client/shared/build.gradle.kts](../client/shared/build.gradle.kts).
- The **Developer Settings** screen switches the active env and lets you log in as pre-baked
  test users — see [developer-settings.md](developer-settings.md).

So "develop against local" = point `supabase.testing.*` at the local stack and switch the app to
`TESTING`.

---

## Prerequisites

| Tool | Check | Install |
|---|---|---|
| Supabase CLI | `supabase --version` | `brew install supabase/tap/supabase` |
| Docker (running) | `docker info` | Docker Desktop / OrbStack |

The repo is already initialized for the CLI — `supabase/config.toml` is committed. You do **not**
need to run `supabase init` again.

---

## ⚠️ Known blockers before `db reset` works

`supabase/migrations/` is a **partial** history: the app's schema was originally built by hand on
the prod dashboard, and migration files only started being written later. Two things therefore
have to be fixed before a fresh local DB can be built. **Neither is fixed by this guide yet** —
they're written up here so whoever tackles local dev knows exactly what they're walking into.

### 1. The base tables have no migration

These tables are referenced or altered by migrations but **created by none of them**:

| Missing table | First referenced by |
|---|---|
| `recipes` | `20260425_add_collaboration.sql` |
| `grocery_lists` | `20260425_add_collaboration.sql` |
| `grocery_items` | `20260425_add_collaboration.sql` |
| `meal_plans` | `20260712_meal_plans_recipe_id_uuid.sql` |

So `supabase db reset` fails on the very first migration with
`relation "recipes" does not exist`.

**Fix:** add a **base-schema migration stamped earlier than `20260425`** (e.g.
`20260101000000_base_schema.sql`) that creates just those tables, so the reset order becomes
base schema → all 21 existing migrations. Generate it from prod rather than by hand:

```bash
supabase login
supabase link --project-ref <your-prod-ref>     # ref is in your dashboard URL

# Read-only snapshot of prod's schema — db dump never modifies prod, and unlike
# `db pull` it does not require the migration history to match.
supabase db dump --linked --schema public -f /tmp/prod-schema.sql
```

Then copy **only** the four tables above (plus their indexes/RLS policies) out of
`/tmp/prod-schema.sql` into `supabase/migrations/20260101000000_base_schema.sql`. Everything else
in that dump is already covered by an existing migration — pulling it all in would double-create
objects and break the replay.

> Don't use `supabase db pull` here. Because prod's migration history doesn't match the repo it
> errors out, and even after a `migration repair` it writes its diff with a *current* timestamp —
> i.e. **after** the migrations that need those tables, so the ordering stays broken.

### 2. Duplicate migration versions

Supabase keys `supabase_migrations.schema_migrations` on the numeric prefix before the first
underscore, so two files sharing one prefix collide on insert during `db reset`:

| Version | Files |
|---|---|
| `20260610` | `fix_recipe_rls_recursion`, `invite_email_via_auth_users`, `recipe_book_collaborators` |
| `20260711` | `grocery_items_replica_identity_full`, `grocery_lists_realtime` |
| `20260713` | `add_recipe_public_sharing`, `recipe_book_reject_invite` |

**Fix:** give each a distinct full `YYYYMMDDHHMMSS` version, preserving relative order (e.g.
`20260711000100_…`, `20260711000200_…`). These files are already applied to prod by hand, so
renaming them is safe *locally* — but if prod's history is ever reconciled (see the appendix),
the repaired versions must match the new names. Coordinate before renaming.

> **Note on storage buckets.** `db dump --schema public` doesn't include storage. The
> `recipe-photos` and `avatars` buckets + their RLS policies are recreated locally by
> [`supabase/seed.sql`](../supabase/seed.sql) (mirroring `docs/supabase-storage-setup.sql` /
> `docs/supabase-avatars-setup.sql`, which were pasted into the prod dashboard). Nothing extra to do.

---

## Start the stack

```bash
supabase start
```

First run pulls several GB of Docker images (slow); subsequent starts are seconds. When it
finishes it prints your local credentials. Re-print them any time with:

```bash
supabase status
```

Example output:

```
         API URL: http://127.0.0.1:54321
          DB URL: postgresql://postgres:postgres@127.0.0.1:54322/postgres
      Studio URL: http://127.0.0.1:54323
    Inbucket URL: http://127.0.0.1:54324
        anon key: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
service_role key: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

| Service | URL | Use |
|---|---|---|
| API (REST/Auth/Storage) | `http://localhost:54321` | what the app points at |
| Studio (dashboard) | `http://localhost:54323` | browse tables, run SQL, view storage |
| Inbucket (email capture) | `http://localhost:54324` | read confirmation/magic-link emails |
| Postgres | `localhost:54322` | direct `psql` access (user/pass `postgres`) |

> The local `anon key` is generated from the local JWT secret and is **stable across restarts**
> on your machine, but can differ between CLI versions — always copy it from `supabase status`
> rather than hardcoding.

Apply migrations + `seed.sql` to a clean database at any time:

```bash
supabase db reset
```

---

## Point the app at the local stack

Add the local API URL + anon key to **`local.properties`** (gitignored). The host you use
**depends on the target platform** (see the next section).

```properties
# Desktop (JVM) and iOS simulator can use localhost directly:
supabase.testing.url=http://localhost:54321
supabase.testing.key=<anon key from `supabase status`>

# Pre-baked test users for Developer Settings → "Login as test user".
# These must match the users seeded in supabase/seed.sql.
chefmate.user.1=alice@chefmate.test
chefmate.user.password.1=password123
chefmate.user.2=bob@chefmate.test
chefmate.user.password.2=password123
```

`supabase.testing.*` and `chefmate.user.*` are read at build time, so **rebuild/re-run** after
editing `local.properties`. Full reference: [buildconfig-setup.md](buildconfig-setup.md) and
[developer-settings.md](developer-settings.md).

### Per-platform networking

The local stack listens on your Mac's `localhost`. How each client reaches it differs:

| Target | `supabase.testing.url` | Notes |
|---|---|---|
| **Desktop (JVM)** | `http://localhost:54321` | Runs on the host — works directly. |
| **iOS simulator** | `http://localhost:54321` | Shares the host loopback. HTTP to loopback is exempt from App Transport Security, so no Info.plist change. |
| **Android emulator** | `http://10.0.2.2:54321` | `10.0.2.2` is the emulator's alias for host loopback. Cleartext HTTP is allowed in **debug** builds via [`network_security_config.xml`](../client/composeApp/src/debug/res/xml/network_security_config.xml). |
| **Physical device** | `http://<your-mac-LAN-IP>:54321` | e.g. `http://192.168.1.20:54321`. Device + Mac must be on the same network. You may need `supabase start` exposed on `0.0.0.0` (it binds all interfaces by default). Android still needs the debug cleartext config (the LAN IP is covered only if you add it to the config). |

> **Heads-up:** `supabase.testing.url` is a single build-time value, so building for the Android
> emulator (`10.0.2.2`) vs Desktop/iOS (`localhost`) means swapping the line in `local.properties`
> and rebuilding. Easiest day-to-day loop is **Desktop** or the **iOS simulator** with `localhost`.

---

## Switch the running app to TESTING

1. Build/run a **debug** build (`./gradlew :client:composeApp:run`, `installDebug`, or the iOS
   debug scheme).
2. **More tab → Developer Settings** (debug-only row at the bottom).
3. **Environment → TESTING.** This signs you out, wipes the local cache, and prompts for a
   restart (the Supabase client binds its URL at first injection, so a restart is required).
4. Reopen the app. Sync now hits your local stack.
5. **Login as test user → User 1** to sign in as `alice@chefmate.test` without typing creds.

---

## Edge Functions locally

The repo ships two functions (`delete-account`, `cleanup-avatars`). Serve them locally:

```bash
supabase functions serve            # serves all functions with hot reload
```

They're reachable at `http://localhost:54321/functions/v1/<name>` and the app's `Functions`
client picks them up automatically when pointed at the local API URL. `SUPABASE_URL` and
`SUPABASE_SERVICE_ROLE_KEY` are injected automatically for local serves.

---

## Everyday commands

| Goal | Command |
|---|---|
| Start the stack | `supabase start` |
| Stop it (keeps data) | `supabase stop` |
| Stop + wipe all local data | `supabase stop --no-backup` |
| Reset DB to migrations + seed | `supabase db reset` |
| Show URLs + keys | `supabase status` |
| Tail logs | `supabase logs` (or per service in Docker) |
| New migration from a Studio change | `supabase db diff -f <name>` |
| Serve edge functions | `supabase functions serve` |

---

## Troubleshooting

- **`db reset` fails on `relation "recipes" does not exist`** — the base tables have no migration.
  See [Known blockers](#️-known-blockers-before-db-reset-works) above.
- **`db reset` fails on a duplicate key in `schema_migrations`** — two migrations share a version
  prefix. See [Known blockers](#️-known-blockers-before-db-reset-works) above.
- **Android emulator: `CLEARTEXT communication ... not permitted`** — you're on a release build,
  or using a host other than the ones in the debug `network_security_config.xml`. Use a debug
  build and `10.0.2.2` (or add your host to the config).
- **App still hits prod after switching to TESTING** — the Supabase client binds at first
  injection. Fully restart (force-stop) the app after the env switch.
- **Login as test user fails** — confirm `supabase/seed.sql` ran (`supabase db reset`) and that
  `chefmate.user.*` in `local.properties` matches the seeded emails/passwords, then rebuild.
- **Email confirmation blocking signup** — local `config.toml` sets
  `[auth.email] enable_confirmations = false`; any emails that are sent are captured by Inbucket
  at `http://localhost:54324` (nothing leaves your machine).
- **Anonymous bootstrap fails** — `config.toml` has `enable_anonymous_sign_ins = true`; if you
  changed it, restart the stack.

---

## Alternative: a hosted staging project

If you'd rather not run Docker, create a **second Supabase cloud project** as staging:

1. Create the project in the dashboard; copy its URL + anon key.
2. `supabase link --project-ref <staging-ref>` then `supabase db push` to apply migrations
   (after the baseline migration exists). Run `docs/supabase-storage-setup.sql` and
   `docs/supabase-avatars-setup.sql` in its SQL editor, and enable **Anonymous Sign-ins** under
   Authentication → Providers.
3. Put the staging URL/key in `supabase.testing.*` and switch the app to `TESTING`.

Trade-offs: real email/OAuth and no Docker, but it uses cloud quota and needs a network. The
local stack is preferred for day-to-day work.

---

## Appendix: reconciling prod's migration history (for future CLI pushes)

SQL has been applied to prod **by hand**, so prod's migration-history table
(`supabase_migrations.schema_migrations`) doesn't know about the 21 files in
`supabase/migrations/`. If you ever run `supabase db push` against prod, the CLI will try to
**replay all of them from scratch** — which will error or duplicate objects.

Reconcile **once** so prod's history matches the repo. After `supabase link --project-ref <prod-ref>`:

```bash
# See what the CLI thinks is applied vs what's on disk.
supabase migration list

# Mark every already-applied migration as applied WITHOUT re-running it, using the exact
# version string `migration list` prints for each entry:
supabase migration repair --status applied <version>

# ...plus the base-schema migration from "Known blockers" above:
supabase migration repair --status applied 20260101000000

# Verify local and remote agree.
supabase migration list
```

> Do this **after** resolving the duplicate-version collisions, not before — otherwise the
> repaired versions won't match the renamed files and you'll have to repair again.

Once history is reconciled, `supabase db push` will only apply genuinely new migrations, and you
can stop pasting SQL into the dashboard. **This is optional and only needed when you want the CLI
to manage prod — it is not required for local development.**

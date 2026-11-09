# Version Bump & Release Automation

This document covers the two GitHub Actions workflows that automate versioning and
releasing across all platforms (Android, iOS, Desktop).

## How it works

```
Developer                   GitHub Actions
──────────                  ──────────────────────────────────────────────────
Run "Bump Version"          bump-version.yml
  (Actions UI)           ─► reads current version
                            computes new version
                            edits build.gradle.kts + Config.xcconfig
                            opens PR "chore: bump version to X.Y.Z (build N)"
                                │
Merge PR (squash)           build-release.yml  (triggered by push to main)
                         ─► creates git tag vX.Y.Z
                            builds signed release APK
                            publishes GitHub release with APK attached
                                │
                         (tag push triggers downstream workflows if RELEASE_TOKEN set)
                             ├─► android-release.yml  → uploads to Play Store
                             ├─► desktop-release.yml  → Mac App Store .pkg, MSI/MSIX, Deb
                             └─► ios-release.yml      → iOS release
```

## Files changed by the bump

| File | Fields |
|---|---|
| `client/composeApp/build.gradle.kts` | `versionCode`, `versionName`, `packageVersion` (JVM), `packageVersion` (macOS block) |
| `iosApp/Configuration/Config.xcconfig` | `CURRENT_PROJECT_VERSION`, `MARKETING_VERSION` |

`bump-version.yml` maps the macOS `packageVersion` to `1.x.y` when the app
version is `0.x.y`, because jpackage's macOS packaging requires `MAJOR > 0`
(a holdover from the retired DMG build; the app is already on `1.x`, so the
mapping is currently a no-op).

> **Note:** `scripts/bump-version.sh` (the interactive local script) has a
> `to_mac_version()` function that is currently a pass-through and does **not**
> apply this mapping. If you use the script locally, update `to_mac_version()`
> so it matches CI behaviour, or correct the macOS value manually after running.
> The script also uses BSD `sed -i ''` and `tail -r`, so it only runs on macOS.

## Required GitHub secrets

Configure these in **Settings → Secrets and variables → Actions → Repository secrets**.

### Always required

| Secret | Description |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded release keystore (`base64 -i release.jks`) |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias within the keystore |
| `ANDROID_KEY_PASSWORD` | Key password |
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_KEY` | Supabase anon/service key |
| `BUGSNAG_API_KEY` | Bugsnag API key |

### Required for downstream workflow triggering

| Secret | Description |
|---|---|
| `RELEASE_TOKEN` | Fine-grained PAT scoped to `Plus-Mobile-Apps/chef-mate` with **Contents: Read and write** + **Workflows: Read and write** (or a classic PAT with `repo` + `workflow`). Without this, `android-release`, `desktop-release`, and `ios-release` will **not** run after the tag is pushed, because the default `GITHUB_TOKEN` cannot trigger other workflows. See [Configuring `RELEASE_TOKEN`](#configuring-release_token) for the full creation/rotation walkthrough. |

### Required by existing workflows (already configured)

| Secret | Used by |
|---|---|
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | `android-release.yml` (Fastlane) |

The iOS / macOS (`MATCH_*`, `ASC_*`, `APPLE_TEAM_ID`) and Microsoft Store
(`PARTNER_CENTER_*`, `STORE_*`) secrets used by `ios-release.yml` and
`desktop-release.yml` are listed in [deployment.md](../deployment.md#github-secrets).

## Step-by-step: running your first bump

1. Go to **Actions → Bump Version → Run workflow**.
2. Choose a bump type:
   - `patch` — `0.1.21` → `0.1.22`, build `22`
   - `minor` — `0.1.21` → `0.2.0`, build `22`
   - `major` — `0.1.21` → `1.0.0`, build `22`
   - `custom` — enter an explicit `X.Y.Z` in the second field
3. Click **Run workflow**. The workflow will open a PR titled
   `chore: bump version to X.Y.Z (build N)`.
4. Review the PR (check the version table in the body), then **squash merge** it.
   - Squash merge ensures the commit message on `main` matches
     `chore: bump version to X.Y.Z (build N)`, which is what `build-release.yml`
     looks for.
5. `build-release.yml` triggers automatically on the push to `main`:
   - Creates and pushes git tag `vX.Y.Z`
   - Builds the signed release APK
   - Creates a GitHub release with the APK attached
6. If `RELEASE_TOKEN` is configured, the tag push also triggers:
   - `android-release.yml` → Fastlane uploads the AAB to Play Store
   - `desktop-release.yml` → uploads the macOS `.pkg` to App Store Connect, builds MSI/MSIX + Deb, and submits the MSIX to the Microsoft Store (once `STORE_PRODUCT_ID` is set)
   - `ios-release.yml` → iOS release

## Configuring `RELEASE_TOKEN`

`RELEASE_TOKEN` is a Personal Access Token used by `build-release.yml` to push
the `vX.Y.Z` tag. The push needs to come from a token (not `GITHUB_TOKEN`) so
that the downstream `android-release` / `desktop-release` / `ios-release`
workflows actually fire — GitHub deliberately does not let a workflow triggered
by `GITHUB_TOKEN` trigger another workflow.

### Creating the token

1. Open <https://github.com/settings/personal-access-tokens> (Settings →
   Developer settings → Personal access tokens → **Fine-grained tokens**).
2. **Generate new token** with:
   - **Token name**: `chef-mate RELEASE_TOKEN` (or similar)
   - **Resource owner**: **`Plus-Mobile-Apps`** — this is the critical field.
     The repo lives under the org, so a token issued under your personal
     account will hit `403 Permission denied to <user>` when it tries to push
     the tag, even with the right permissions.
   - **Expiration**: pick a date you can commit to and put it on your calendar.
     Max is one year. When it expires, repeat this whole section.
   - **Repository access**: **Only select repositories** → `Plus-Mobile-Apps/chef-mate`.
3. **Repository permissions** (leave everything else as "No access"):
   - **Contents**: **Read and write** — needed to push the release tag.
   - **Workflows**: **Read and write** — needed so the tag push can trigger the
     other release workflows.
   - **Metadata**: Read-only (auto-selected, can't be turned off).
4. **Generate token** and copy the value immediately — you can't see it again.
5. If the org has fine-grained PATs gated, the token will be in **Pending**
   state until an org owner approves it under `Plus-Mobile-Apps` → Settings →
   Personal access tokens → **Pending requests**.

### Storing it

6. Go to <https://github.com/Plus-Mobile-Apps/chef-mate/settings/secrets/actions>.
7. If `RELEASE_TOKEN` already exists, click it → **Update secret** and paste the
   new value. Otherwise click **New repository secret**, name it `RELEASE_TOKEN`,
   paste the value, save.

### Rotating (or reacting to a 403)

If a tag-push step fails with `remote: Permission to ... denied to <user>` or
`The requested URL returned error: 403`, the PAT has expired, been revoked, or
lost access (e.g. after a repo transfer). Generate a new token following the
steps above and update the secret. There's no need to change anything in
`build-release.yml`.

### Classic PAT alternative

If the org disables fine-grained PATs, fall back to a classic token:
**Settings → Developer settings → Tokens (classic)** → check the `repo` and
`workflow` scopes → after generating, click **Configure SSO** on the token's
page and authorize it for `Plus-Mobile-Apps`. Store as `RELEASE_TOKEN` the
same way.

## Setting up Android signing

The `build-release.yml` workflow calls `./gradlew :client:composeApp:assembleRelease`
with signing env vars that are read by the signing config in
`client/composeApp/build.gradle.kts`.

To prepare the `ANDROID_KEYSTORE_BASE64` secret:

```bash
# Encode your keystore file
base64 -i /path/to/release.jks | pbcopy   # copies to clipboard (macOS)
# Paste the output as the ANDROID_KEYSTORE_BASE64 secret value
```

## Validating locally

Run the test script before pushing changes to the workflow:

```bash
./scripts/test-version-bump.sh           # tests bump to 0.99.0 (build 99) by default
./scripts/test-version-bump.sh 1.0.0 42  # custom version and build number
```

The script exercises the same sed/awk patterns as the CI and exits non-zero if any
value is not set correctly.

## Common issues

### Workflow does not trigger after merging the bump PR

Check that you used **squash merge** and that the resulting commit message on `main`
starts with `chore: bump version`. If you used a regular merge commit, the commit
message is `Merge pull request #N from ...` and the `if:` condition won't match.

### `android-release` / `desktop-release` not triggered by the tag

The `RELEASE_TOKEN` secret is missing or has insufficient permissions. See
[Configuring `RELEASE_TOKEN`](#configuring-release_token) above.

### APK not found after build

The `assembleRelease` task might have failed silently, or the keystore env vars are
not set. Check the workflow logs for Gradle errors. Ensure all four signing secrets
are configured.

### Duplicate GitHub release

If `build-release.yml` and `desktop-release.yml` both try to create a release for the
same tag, `softprops/action-gh-release` will update the existing release rather than
fail — this is expected behaviour.

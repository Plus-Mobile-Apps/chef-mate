# Deployment Guide

This project uses **GitHub Actions** for CI and **Fastlane** for mobile build/upload orchestration. All three platforms (Android, iOS, Desktop) have automated release workflows triggered by git tags.

## Quick Start

The recommended path is the automated **Bump Version** workflow (see [Version Management](#version-management)) — it bumps versions, opens a PR, and tags the release on merge. To release manually, push a tag directly:

```bash
git tag v1.0.0
git push --tags
```

Either way, the tag triggers three independent workflows that run in parallel:

| Platform | Workflow | Destination |
|----------|----------|-------------|
| Android | `android-release.yml` | Play Store (internal track) |
| iOS | `ios-release.yml` | App Store Connect (TestFlight) |
| Desktop | `desktop-release.yml` | macOS → App Store Connect (Mac App Store); Linux `.deb` + `latest.json` feed on GitHub Releases; Windows → Microsoft Store (MSIX) |

All workflows also support manual triggering via the "Run workflow" button in the GitHub Actions UI (`workflow_dispatch`).

### Desktop update strategy

Desktop distribution + updates depend on platform:

| Platform | Distribution | Updates |
|----------|--------------|---------|
| macOS | **Mac App Store** (`.pkg` uploaded to App Store Connect) | **The Store** — the in-app updater is a no-op on macOS (Store policy forbids self-updating, and the sandbox can't replace its own install) |
| Linux | `.deb` on GitHub Releases | **In-app updater** (`DesktopUpdater`) polls the `latest.json` feed and prompts the user to download + install |
| Windows | **Microsoft Store** (MSIX) | **The Store** — the in-app updater is a no-op on Windows for the same reasons |

**Only Linux runs the in-app updater.** See [macOS: Mac App Store](#macos-mac-app-store) and
[Windows: Microsoft Store (MSIX)](#windows-microsoft-store-msix) for the store paths.

---

## Prerequisites

### Fastlane

Fastlane is managed via Bundler. Install it locally with:

```bash
bundle install
```

The `Gemfile` at the project root declares the Fastlane dependency.

### GitHub Secrets

The following repository secrets must be configured in **Settings > Secrets and variables > Actions**:

#### Build Configuration

| Secret | Description |
|--------|-------------|
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_KEY` | Supabase anon/public key |
| `BUGSNAG_API_KEY` | Bugsnag API key for crash reporting |

These are read by BuildKonfig at build time. See [buildconfig-setup.md](buildconfig-setup.md) for details.

#### Android Signing

| Secret | Description |
|--------|-------------|
| `ANDROID_KEYSTORE_BASE64` | Release keystore file, base64-encoded (see [Generating the keystore](#generating-the-android-keystore)) |
| `ANDROID_KEYSTORE_PASSWORD` | Password for the keystore |
| `ANDROID_KEY_ALIAS` | Alias of the signing key within the keystore |
| `ANDROID_KEY_PASSWORD` | Password for the signing key |

#### Play Store Upload

| Secret | Description |
|--------|-------------|
| `PLAY_STORE_SERVICE_ACCOUNT_JSON` | Google Play service account JSON key content (see [Play Store setup](#play-store-service-account)) |

#### iOS Code Signing (Fastlane Match)

| Secret | Description |
|--------|-------------|
| `MATCH_PASSWORD` | Encryption password for the match certificates repository |
| `MATCH_GIT_BASIC_AUTHORIZATION` | Base64-encoded `username:personal_access_token` for read access to the certificates repo. See [Fastlane Match (iOS Certificates)](#fastlane-match-ios-certificates) for the creation/rotation walkthrough. |

#### App Store Connect

| Secret | Description |
|--------|-------------|
| `ASC_KEY_ID` | App Store Connect API Key ID |
| `ASC_ISSUER_ID` | App Store Connect API Issuer ID |
| `ASC_API_KEY` | App Store Connect API private key (`.p8` content, base64-encoded) |
| `APPLE_TEAM_ID` | Apple Developer Team ID |

#### Desktop Code Signing

**macOS** desktop signing reuses the **existing iOS `MATCH_*` and `ASC_*` secrets** — no desktop-specific secrets are required. The `macos` job in `desktop-release.yml` runs `fastlane mac release`, which fetches the App Store certs + provisioning profile via Fastlane match, resolves the "Apple Distribution" identity into `MACOS_SIGN_IDENTITY`, builds a Store-signed `.pkg` with `packageReleasePkg`, and uploads it to App Store Connect. See [macOS: Mac App Store](#macos-mac-app-store).

**Windows** code signing is **not required** — the Microsoft Store signs MSIX packages on ingestion.

#### Microsoft Store (Partner Center API)

Required only for [automated MSIX submission](#automating-store-submission). Created from an Azure AD application associated with your Partner Center account.

| Secret | Description |
|--------|-------------|
| `PARTNER_CENTER_TENANT_ID` | Azure AD tenant ID |
| `PARTNER_CENTER_CLIENT_ID` | Azure AD application (client) ID |
| `PARTNER_CENTER_CLIENT_SECRET` | Client secret for that Azure AD application |
| `PARTNER_CENTER_SELLER_ID` | Seller ID from Partner Center → Account settings |
| `STORE_PRODUCT_ID` | The Store product/app ID for the reserved app — gates the `publish-store` job |
| `STORE_IDENTITY_NAME` | Partner Center **Package/Identity Name** — substituted into `AppxManifest.xml` (falls back to a placeholder if unset, so the MSIX still builds) |
| `STORE_PUBLISHER` | Partner Center **Publisher** `CN=...` string — substituted into `AppxManifest.xml` |

---

## Platform Details

### Android

**Workflow:** `.github/workflows/android-release.yml`

**What it does:**
1. Decodes the release keystore from the `ANDROID_KEYSTORE_BASE64` secret
2. Builds a signed AAB via `./gradlew :client:composeApp:bundleRelease`
3. Uploads the AAB to the Play Store **internal** track via Fastlane `supply`

**Fastlane lane:** `bundle exec fastlane android release`

**Signing configuration:** The `signingConfigs` block in `client/composeApp/build.gradle.kts` reads credentials from environment variables first (used in CI), then falls back to a `keystore.properties` file at the project root (for local builds):

| Environment Variable | keystore.properties Key | Description |
|---------------------|------------------------|-------------|
| `ANDROID_KEYSTORE_FILE` | `releaseKeyStore` | Path to the keystore file (defaults to `release.keystore`) |
| `ANDROID_KEYSTORE_PASSWORD` | `releaseStorePassword` | Password for the keystore |
| `ANDROID_KEY_ALIAS` | `releaseKeyAlias` | Alias of the signing key |
| `ANDROID_KEY_PASSWORD` | `releaseKeyPassword` | Password for the signing key |

For local builds, create a `keystore.properties` file in the project root (this file is gitignored):

```properties
releaseKeyAlias=upload
releaseKeyPassword=YOUR_KEY_PASSWORD
releaseKeyStore=/absolute/path/to/your/keystore
releaseStorePassword=YOUR_STORE_PASSWORD
```

**Note:** `releaseKeyStore` must be an absolute path. Relative paths are resolved from the `client/composeApp/` module directory, not the project root.

**Promoting releases:** Builds are uploaded to the `internal` track. Promote to higher tracks (alpha, beta, production) through the [Google Play Console](https://play.google.com/console).

### iOS

**Workflow:** `.github/workflows/ios-release.yml`

**What it does:**
1. Fetches signing certificates and provisioning profiles via Fastlane `match` (readonly mode)
2. Builds the IPA via `build_app` targeting `iosApp/iosApp.xcodeproj`
3. Uploads to App Store Connect via Fastlane `deliver`

**Fastlane lane:** `bundle exec fastlane ios release`

**Code signing:** Uses [Fastlane match](https://docs.fastlane.tools/actions/match/) with a private git repository to store encrypted certificates and provisioning profiles. The repository URL is configured in `fastlane/Matchfile`.

**App Store Connect API:** Uses an API key (recommended over Apple ID for CI). The key avoids 2FA prompts and is more reliable for automation.

### Desktop

**Workflow:** `.github/workflows/desktop-release.yml`

**What it does:**
1. The `build` job builds native packages in parallel on two runners:
   - `windows-latest` — builds `.msi` via `./gradlew :client:composeApp:packageReleaseMsi`
   - `ubuntu-latest` — builds `.deb` via `./gradlew :client:composeApp:packageReleaseDeb`

   These `packageRelease*` tasks (not the plain `package*` variants) are required so the requested
   task name contains "Release", which sets `BuildConfig.IS_DEBUG = false` and hides the developer-only
   UI. R8/ProGuard minification is disabled for the release build type in
   `client/composeApp/build.gradle.kts`, so the binary is functionally identical to the old debug
   packaging — it just carries the release marker.
2. The `macos` job (tag pushes only) runs `fastlane mac release`, which builds a Store-signed `.pkg`
   via `./gradlew :client:composeApp:packageReleasePkg` and uploads it to App Store Connect (see
   [macOS: Mac App Store](#macos-mac-app-store)).
3. On Windows, also builds the app image (`createReleaseDistributable`) and packs an MSIX via `makeappx`
4. Uploads the Windows/Linux packages (and the MSIX) as build artifacts. macOS emits no GitHub-Release
   artifact — its `.pkg` goes straight to App Store Connect.
5. Generates `latest.json` (the in-app update feed) with the **Linux** download URL only — macOS and
   Windows are omitted because both ship via their app stores and the updater is a no-op there.
6. Creates a GitHub Release from the tag with the Windows/Linux packages + `latest.json` attached
7. Auto-generates release notes from commits since the last tag
8. On tag pushes, the `publish-store` job submits the MSIX to the Microsoft Store via the MSStore CLI (no-ops until `STORE_PRODUCT_ID` is set)

**Package configuration:** Native distribution settings (package name, version, vendor, platform-specific options) are in `client/composeApp/build.gradle.kts` under the `compose.desktop.application.nativeDistributions` block. The macOS `signing { }` block and the provisioning-profile paths are gated on env vars (`MACOS_SIGN_IDENTITY`, `MACOS_PROVISIONING_PROFILE`) so a local `packageReleasePkg` works unsigned without certs.

#### In-app updater (Linux only)

The desktop app embeds `DesktopUpdater` (`client/composeApp/src/jvmMain/.../update/`). On Linux it polls the stable feed URL on launch:

```
https://github.com/Plus-Mobile-Apps/chef-mate/releases/latest/download/latest.json
```

GitHub always serves `releases/latest/download/<asset>` from the newest non-prerelease release, so no separate hosting is needed. The updater compares the feed `version` against `BuildConfig.VERSION_NAME`; if newer, it surfaces a banner to download the `.deb` and open it. It is a **no-op on macOS and Windows** by design — both ship through app stores that forbid self-updating and sandbox the install.

This is a *signed-installer* model (download + run the new signed package), **not** in-place jar patching.

It is also a no-op in the Flathub build (`isRunningInFlatpak`), where Flatpak owns updates and the sandbox is read-only. See [Linux: Flathub](#linux-flathub).

**`latest.json` shape:**

```json
{
  "version": "1.8.0",
  "notesUrl": "https://github.com/Plus-Mobile-Apps/chef-mate/releases/tag/v1.8.0",
  "downloads": {
    "linux": "https://github.com/.../releases/download/v1.8.0/chef-mate_1.8.0_amd64.deb"
  }
}
```

---

## Linux: Flathub

Linux ships two ways: the `.deb` on the GitHub Release (self-updating via the feed above), and a
Flatpak on Flathub.

The Flatpak manifest and its AppStream/desktop metadata live in [`packaging/linux/`](../packaging/linux/README.md),
which is also the runbook for building, linting, submitting, and maintaining it. In short:

- App id `com.plusmobileapps.chefmate`, runtime `org.gnome.Platform//50` (GTK 3 is needed by JavaFX).
- The manifest consumes `chef-mate-<version>-linux-x86_64.tar.gz` — the jpackage app image, bundled
  jlink runtime included — which the `Desktop Release` workflow builds and attaches to the release.
  It is **not** built from source on Flathub infrastructure; the reasons are in the packaging README.
- Once submitted, Flathub's external-data-checker opens the version-bump PR when a new release
  publishes a matching tarball. Nothing in this repo pushes to Flathub.

---

## Windows: Microsoft Store (MSIX)

Windows ships through the **Microsoft Store** as an MSIX package. This is the recommended Windows path because:

- **The Store signs the package for free** on ingestion — no code-signing certificate needed (no ~$10/month or ~$200+/year cert).
- **The Store handles auto-updates** — which is why the in-app updater no-ops on Windows.
- SmartScreen trusts Store apps, so users never see "unknown publisher" warnings.

> When creating the product in Partner Center, choose **"MSIX or PWA app"**, *not* "EXE or MSI app". The EXE/MSI option only provides a listing that points at your own self-hosted, self-signed installer — it gives you neither free signing nor Store-managed updates.

### One-time Partner Center setup

1. In [Partner Center](https://partner.microsoft.com/dashboard), create a **new MSIX product** and reserve the app name.
2. From the product's **Product Identity** page, record three values needed for the manifest:
   - **Package/Identity Name** (e.g. `12345PlusMobileApps.ChefMate`)
   - **Publisher** (the `CN=...` string, e.g. `CN=ABCD1234-...`)
   - **Publisher Display Name** (e.g. `Plus Mobile Apps`)
3. The **first submission must be done manually** in Partner Center to establish the listing. Automated submission (below) is for subsequent releases.

### Packaging sketch

jpackage / Compose Desktop does **not** emit MSIX, so we package the unpackaged *app image* ourselves. Compose Desktop's `createDistributable` task produces the app image (app + bundled JRE) at:

```
client/composeApp/build/compose/binaries/main/app/Chef Mate/
```

The MSIX is then built by laying that directory next to an `AppxManifest.xml` and running `makeappx` (part of the Windows SDK, preinstalled on `windows-latest` runners). **This is wired into `desktop-release.yml`** — the manifest lives at `packaging/windows/AppxManifest.xml` and uses `__TOKEN__` placeholders that the workflow substitutes at build time.

**`packaging/windows/AppxManifest.xml`** (the committed manifest uses tokens; the identity values come from Partner Center, and the version must be 4-part with a `.0` revision):

```xml
<?xml version="1.0" encoding="utf-8"?>
<Package
    xmlns="http://schemas.microsoft.com/appx/manifest/foundation/windows10"
    xmlns:uap="http://schemas.microsoft.com/appx/manifest/uap/windows10"
    xmlns:rescap="http://schemas.microsoft.com/appx/manifest/foundation/windows10/restrictedcapabilities">

  <Identity Name="[PACKAGE_IDENTITY_NAME]"
            Publisher="[CN=PUBLISHER_ID]"
            Version="1.7.1.0"
            ProcessorArchitecture="x64" />

  <Properties>
    <DisplayName>Chef Mate</DisplayName>
    <PublisherDisplayName>Plus Mobile Apps</PublisherDisplayName>
    <Logo>Assets\StoreLogo.png</Logo>
  </Properties>

  <Dependencies>
    <TargetDeviceFamily Name="Windows.Desktop" MinVersion="10.0.17763.0"
                        MaxVersionTested="10.0.22621.0" />
  </Dependencies>

  <Resources>
    <Resource Language="en-us" />
  </Resources>

  <Applications>
    <Application Id="ChefMate"
                 Executable="Chef Mate.exe"
                 EntryPoint="Windows.FullTrustApplication">
      <uap:VisualElements
          DisplayName="Chef Mate"
          Description="Chef Mate - Your AI Cooking Assistant"
          BackgroundColor="transparent"
          Square150x150Logo="Assets\Square150x150Logo.png"
          Square44x44Logo="Assets\Square44x44Logo.png" />
    </Application>
  </Applications>

  <Capabilities>
    <!-- Win32 / Desktop Bridge apps require the runFullTrust restricted capability -->
    <rescap:Capability Name="runFullTrust" />
  </Capabilities>
</Package>
```

`runFullTrust` is the restricted capability that lets a packaged Win32 app (which a bundled-JRE app is) run unsandboxed. The required logo assets (`StoreLogo`, `Square150x150Logo`, `Square44x44Logo`) are generated in CI by copying the existing app icon — replace these with correctly-sized PNGs before submitting for Store certification.

**Build step** (the `Build app image for MSIX` + `Package MSIX` steps on the Windows leg of `desktop-release.yml`). The packaging step substitutes the manifest tokens, copies the icon into the `Assets`, and packs with `makeappx`:

```yaml
  - name: Package MSIX (Windows)
    if: runner.os == 'Windows'
    shell: pwsh
    env:
      STORE_IDENTITY_NAME: ${{ secrets.STORE_IDENTITY_NAME }}
      STORE_PUBLISHER: ${{ secrets.STORE_PUBLISHER }}
    run: |
      # resolve version (tag → x.y.z), substitute __IDENTITY_NAME__/__PUBLISHER__/__VERSION__,
      # copy icon to Assets\{StoreLogo,Square150x150Logo,Square44x44Logo}.png, then:
      $makeappx = (Get-ChildItem "${env:ProgramFiles(x86)}\Windows Kits\10\bin\*\x64\makeappx.exe" |
                   Sort-Object FullName -Descending | Select-Object -First 1).FullName
      & $makeappx pack /d "$img" /p "$env:RUNNER_TEMP\ChefMate.msix" /o
```

> Because the MSIX is signed by the Store on submission, you do **not** sign it yourself in CI. (You only need a self-signed cert to *sideload* it for local testing.)

### Automating Store submission

**Yes — this is wired in** via Microsoft's [**MSStore CLI**](https://github.com/microsoft/msstore-cli), driven from the `publish-store` job in `desktop-release.yml`.

**Auth setup (one-time):**
1. In Partner Center → **Account settings → User management → Azure AD applications**, add (or create) an Azure AD application and grant it the **Manager** role.
2. Record the **tenant ID**, **client ID**, and create a **client secret**; grab the **seller ID** from Account settings. Store them as the `PARTNER_CENTER_*` secrets listed above.
3. Set `STORE_PRODUCT_ID` (and `STORE_IDENTITY_NAME` / `STORE_PUBLISHER`). The `publish-store` job no-ops while `STORE_PRODUCT_ID` is unset, so it's safe to leave dormant until you're ready.

**The job** (runs only on tag pushes; each step is gated on `STORE_PRODUCT_ID`):

```yaml
  publish-store:
    needs: build
    if: startsWith(github.ref, 'refs/tags/')
    runs-on: windows-latest
    env:
      STORE_PRODUCT_ID: ${{ secrets.STORE_PRODUCT_ID }}
    steps:
      - uses: actions/download-artifact@v4
        if: env.STORE_PRODUCT_ID != ''
        with: { name: desktop-windows-msix, path: msix }
      - name: Setup MSStore CLI
        if: env.STORE_PRODUCT_ID != ''
        uses: microsoft/setup-msstore-cli@v1
      - name: Configure MSStore CLI
        if: env.STORE_PRODUCT_ID != ''
        run: >
          msstore reconfigure
          --tenantId ${{ secrets.PARTNER_CENTER_TENANT_ID }}
          --sellerId ${{ secrets.PARTNER_CENTER_SELLER_ID }}
          --clientId ${{ secrets.PARTNER_CENTER_CLIENT_ID }}
          --clientSecret ${{ secrets.PARTNER_CENTER_CLIENT_SECRET }}
      - name: Publish to Microsoft Store
        if: env.STORE_PRODUCT_ID != ''
        shell: pwsh
        run: |
          $msix = (Get-ChildItem msix/*.msix | Select-Object -First 1).FullName
          msstore publish -i $msix -id $env:STORE_PRODUCT_ID
```

**Caveats / things to verify on first run:**
- The **first** submission must be made manually in Partner Center; the API updates an existing listing, it doesn't create one.
- Verify the `microsoft/setup-msstore-cli` action ref and the `msstore publish -id` value (Partner Center product ID vs. Store app ID) against the [current MSStore CLI docs](https://learn.microsoft.com/en-us/windows/apps/publish/msstore-dev-cli/commands) — the CLI is in preview and flags shift.
- Store **certification review** still applies to each submission and can take hours to a day, so Store releases lag the GitHub Release / mobile releases.
- The MSIX `Identity/Version` is derived from the release tag, so it increments automatically with the [version bump](#version-management).

> **Status:** the MSIX packaging steps and `publish-store` job are wired into `desktop-release.yml`. The MSIX builds on every run (with placeholder identity until the Store secrets are set); submission only fires on tag pushes once `STORE_PRODUCT_ID` is configured. The unsigned `.msi` is still built as a fallback/sideload artifact.

## macOS: Mac App Store

macOS ships through the **Mac App Store** as a sandboxed `.pkg` uploaded to App Store Connect
(TestFlight → release). The direct Developer-ID DMG has been retired. Compose builds the Store `.pkg`
with `macOS { appStore = true }` in `client/composeApp/build.gradle.kts` (`packageReleasePkg`); the
`macos` job in `desktop-release.yml` runs `fastlane mac release` to sign, build, and upload it.

**Bundle ID:** `com.plusmobileapps.chefmate.ChefMate` — the **same** as the iOS app id, so macOS ships
under the one "Chef Mate" App Store record (Universal Purchase / a single product page across iOS +
Mac) rather than a separate listing. The App ID already exists from iOS; you enable the **macOS
platform** on the existing app record instead of creating a new one.

> The App ID's iOS capabilities (App Groups, Associated Domains, share-extension/watch relationships)
> carry into the macOS App Store provisioning profile. That's normally fine — the profile is a
> superset and the Mac entitlements simply don't claim them — but it's worth confirming the profile
> builds cleanly on the first `mac certificates` run.

**Certificates & profile** (fetched via Fastlane `match`, `type: "appstore"`, `platform: "macos"`,
from the same private certificates repo used for iOS):
- **Mac App Distribution** cert (CN `3rd Party Mac Developer Application: …`) — signs the `.app`
  bundle. **Must be this legacy cert, not the unified "Apple Distribution".** Compose's signer only
  matches the legacy `3rd Party Mac Developer Application:` / `Developer ID Application:` names (true
  through Compose 1.12.0-beta), so an "Apple Distribution" cert fails with `Could not find certificate
  … in keychain []`. The lane passes **`generate_apple_certs: false`** to match so it mints this
  legacy cert instead of the modern unified one.
- **Mac Installer Distribution** cert (CN `3rd Party Mac Developer Installer: …`) — signs the `.pkg`
  installer (fetched via match `additional_cert_types: ["mac_installer_distribution"]`).
- **Mac App Store provisioning profile** — embedded in the `.app`. The `mac release` lane passes its
  path to Compose via `MACOS_PROVISIONING_PROFILE` / `MACOS_RUNTIME_PROVISIONING_PROFILE`.

**Signing:** The `mac release` lane resolves the **bare** cert name (org + team, no prefix) from the
`3rd Party Mac Developer Application` cert into `MACOS_SIGN_IDENTITY`. Compose prepends
`3rd Party Mac Developer Application: ` to sign the app; jpackage prepends
`3rd Party Mac Developer Installer: ` (via `--mac-signing-key-user-name`) to sign the `.pkg`. Passing
the bare name lets each tool add its own prefix. Local builds without these env vars stay unsigned.

> **Getting the legacy cert (one-time, fiddly).** Apple no longer *issues* the "Mac App
> Distribution" cert through the automated flow — even with `generate_apple_certs: false`, match
> reuses the unified "Apple Distribution" cert unless the legacy cert already exists in the repo. So
> it must be **created by hand once** and imported (see the one-time bootstrap below). After that,
> `generate_apple_certs: false` makes match resolve to it and generate a matching profile. The lane
> fails fast if the `3rd Party Mac Developer Application` cert is missing.

**App Sandbox (mandatory):** The Mac App Store requires App Sandbox. Entitlements live in
`packaging/macos/entitlements.plist` (app: sandbox + `network.client` + `files.user-selected.read-write`
+ JVM hardened-runtime exceptions) and `packaging/macos/runtime-entitlements.plist` (the nested JRE:
sandbox-inherit + JVM exceptions), wired via `entitlementsFile` / `runtimeEntitlementsFile`.

> **Known risk — validate before the first real submission.** This is a bundled-JRE app that uses a
> JavaFX WebView (the recipe browser), native file dialogs, and Supabase networking. Sandboxed
> JVM/JavaFX apps are a known source of runtime breakage and App Store review rejection, and the
> `disable-library-validation` / `allow-unsigned-executable-memory` entitlements are scrutinized in
> review. **Smoke-test the signed `.pkg` sandboxed** (WebView loads, file open/save works, sign-in +
> sync work, no sandbox denials in Console.app) before submitting, and be prepared to iterate on the
> entitlements or face rejection.

**One-time bootstrap:**
1. In **App Store Connect → the existing "Chef Mate" app → add the macOS platform** (Universal
   Purchase) so Mac builds land under the same record. No new app record or App ID is created — both
   already exist from iOS. The App ID needs **no extra capabilities** enabled (App Sandbox, network,
   and file access come from the build's entitlements, not the portal).
2. Create the **Mac App Distribution** certificate by hand (match won't — see the note above):
   - In **Keychain Access → Certificate Assistant → Request a Certificate from a Certificate
     Authority** ("Saved to disk"), generate a CSR (this also puts the private key in your login
     keychain).
   - At developer.apple.com → Certificates → ➕ → **Mac App Distribution**, upload the CSR, download
     the `.cer`, and double-click it to install.
3. Import that cert + its key into the match repo (its key isn't there yet since you made it by hand).
   The key must be a bare PKCS#1 PEM (same conversion the Developer ID cert used — see below), then:
   ```bash
   unset APP_STORE_CONNECT_API_KEY APP_STORE_CONNECT_API_KEY_ID APP_STORE_CONNECT_API_ISSUER_ID
   export MATCH_PASSWORD=<match repo passphrase>
   bundle exec fastlane match import --type appstore --platform macos \
     --git_url git@github.com:Plus-Mobile-Apps/certificates.git
   # Certificate (.cer): the Mac App Distribution cert; Private Key (.p12): the converted key;
   # Provisioning Profile: leave empty (generated next).
   ```
4. Generate the Mac App Store provisioning profile against the imported cert (re-export the `ASC_*`
   values first, since step 3 unset them):
   ```bash
   bundle exec fastlane mac certificates force:true
   ```
   Confirm the output installs `3rd Party Mac Developer Application` and the profile's Certificate
   Name is that cert (not Apple Distribution).
5. From then on CI consumes the stored assets read-only inside `fastlane mac release`.

**Reused secrets:** `MATCH_PASSWORD`, `MATCH_GIT_BASIC_AUTHORIZATION`, `ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_API_KEY`, `APPLE_TEAM_ID` (no new secrets required).

**Architecture — arm64 only (Intel Macs not supported).** CI builds on an Apple-Silicon runner, so
jpackage produces an **arm64-only** app with a bundled arm64 JRE. App Store Connect rejects an
arm64-only Mac app unless it declares a minimum macOS of 12.0+, so `macOS.minimumSystemVersion =
"12.0"` is set in `build.gradle.kts` (`LSMinimumSystemVersion`). Supporting Intel would require a
universal (arm64+x86_64) binary — a much larger change (build both arches + a universal JRE, `lipo`
them) that jpackage does not do natively.

**Native libraries must be bundled + signed (App Store sandbox).** A sandboxed App Store app can only
load code that's part of its signed bundle. `sqlite-jdbc` (via SQLDelight's JVM driver) otherwise
extracts its native `libsqlitejdbc.dylib` to a temp dir at runtime and `dlopen`s it, which the
sandbox blocks at launch ("could not verify … free of malware"). The `extractSqliteJdbcMacDylib` task
in `build.gradle.kts` stages the arm64 `.dylib` into Compose's app resources so jpackage signs it
into the bundle, and `DriverFactory.jvm.kt` points sqlite-jdbc at it via `org.sqlite.lib.path` so it
loads the signed copy instead of extracting. Any **other** JVM library that extracts native code at
runtime will hit the same wall and need the same treatment.

**Validating signing without a release.** The `macos` job also runs on **workflow_dispatch**
(Actions → Desktop Release → Run workflow). A manual run **builds + signs** the `.pkg` and uploads it
as a `desktop-macos-pkg` artifact, but sets `MAC_UPLOAD=false` so it does **not** push to App Store
Connect — use it to verify signing (e.g. after a cert rotation) without cutting a version tag. Only
tag pushes set `MAC_UPLOAD=true` and upload. The build runs with `-Pcompose.desktop.verbose=true` so
jpackage's internal `codesign` errors are visible in the log (otherwise they surface only as an
opaque `codesign … exited with 1` `IOException`).

**Signing pitfalls that bit us** (all handled by the `mac release` lane, noted here for cert
rotation): Compose only matches the legacy `3rd Party Mac Developer Application:` cert name, so the
lane resolves the **bare** identity and needs the **Mac App Distribution** cert (not the unified
Apple Distribution — created + imported by hand, see bootstrap). Compose embeds the app profile via
jpackage `--app-content`, which (a) splits the path on whitespace in its @arg-file and (b) keeps the
filename — so the lane copies match's profile to a **space-free path named exactly
`embedded.provisionprofile`** before building.

---

## First-Time Setup

### Android Setup Checklist

Follow these steps in order to go from zero to automated Play Store deployments:

1. **Generate a release keystore** (see [below](#generating-the-android-keystore)), or use an existing one.

2. **Create a `keystore.properties`** file in the project root with your keystore details (see the [signing configuration](#android) section above for the format). Use an absolute path for `releaseKeyStore`.

3. **Create your app on Google Play Console** — set up the app listing with the required store details (title, description, etc.).

4. **Upload the first AAB manually** — the Play Store API cannot create a new app, only upload to an existing one:
   ```bash
   ./gradlew :client:composeApp:bundleRelease
   ```
   Then upload `client/composeApp/build/outputs/bundle/release/composeApp-release.aab` through the Play Console.

5. **Create a Google Play service account** (see [below](#play-store-service-account)) and grant it access in the Play Console.

6. **Test Fastlane locally** to verify the service account works:
   ```bash
   export SUPPLY_JSON_KEY_DATA=$(cat /path/to/service-account-key.json)
   bundle exec fastlane android release
   ```

7. **Configure GitHub secrets** — add all required secrets listed in the [GitHub Secrets](#github-secrets) section above.

8. **Trigger a release** — commit your changes, create a tag, and push:
   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```

9. **After first publish** — once the app is live on any track, update `release_status` in `fastlane/Fastfile` from `"draft"` to `"completed"` so future uploads go live automatically.

### Generating the Android Keystore

If you don't already have a release keystore:

```bash
keytool -genkeypair \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore release.keystore \
  -storepass YOUR_STORE_PASSWORD \
  -keypass YOUR_KEY_PASSWORD \
  -dname "CN=Chef Mate, O=Plus Mobile Apps"
```

To base64-encode it for the GitHub secret:

```bash
base64 -i release.keystore | pbcopy   # macOS (copies to clipboard)
base64 -w 0 release.keystore          # Linux (prints to stdout)
```

Store the output as the `ANDROID_KEYSTORE_BASE64` secret.

### Play Store Service Account

1. Go to the [Google Cloud Console](https://console.cloud.google.com)
2. Create a service account with the "Service Account User" role
3. Generate a JSON key for the service account
4. In Google Play Console, go to **Settings > API access** and grant the service account access
5. Paste the entire JSON key content as the `PLAY_STORE_SERVICE_ACCOUNT_JSON` secret

**Important:** The first AAB must be uploaded manually through the Play Console to create the app listing. Fastlane `supply` can only upload to an existing app.

**Draft apps:** If your app has not yet been published on any track, the Play Store API requires uploads to have `release_status: "draft"`. This is configured in `fastlane/Fastfile`. Once the app is published for the first time (even to the internal track), you can change this to `"completed"` so releases go live automatically.

### Fastlane Match (iOS Certificates)

The certificates repo (`Plus-Mobile-Apps/certificates`) is configured in
`fastlane/Matchfile`. Match runs in `readonly: true` mode on CI, so the PAT
behind `MATCH_GIT_BASIC_AUTHORIZATION` only needs read access.

#### First-time setup

1. Create a **private** git repository under the org to store encrypted
   certificates (currently `Plus-Mobile-Apps/certificates`). If you move it,
   update `fastlane/Matchfile`.
2. Run match locally to generate and store certificates:

   ```bash
   bundle exec fastlane ios certificates
   ```

   This read-write lane authenticates with the App Store Connect API key (set
   the `APP_STORE_CONNECT_API_*` env vars, same as the release lane), registers
   the watchOS companion's App ID via `produce` if it doesn't exist yet (it
   ships inside the parent app, so no App Store Connect record is created), and
   then provisions every app id in `fastlane/Matchfile` — the app, the share
   extension, and the watchOS app (`com.plusmobileapps.chefmate.ChefMate.watchkitapp`).
   Re-run it after adding a new app id so its App Store profile is registered
   before the next release; the release lane / CI fetches profiles in `readonly`
   mode and will fail if one is missing.

   > Don't run `fastlane match appstore` directly — on its own it doesn't build
   > the API-key Hash and fails with `'api_key' value must be a Hash`. Use the
   > `certificates` lane, which sets it up.

3. Set the `MATCH_PASSWORD` secret to the encryption password you chose.

#### Creating / rotating `MATCH_GIT_BASIC_AUTHORIZATION`

`MATCH_GIT_BASIC_AUTHORIZATION` is **not** the raw PAT — it's a base64-encoded
`username:token` string that Fastlane passes as an HTTP Basic Auth header to
clone the cert repo. Rotate it whenever the underlying PAT expires.

1. Open <https://github.com/settings/personal-access-tokens> →
   **Generate new token** (fine-grained) with:
   - **Token name**: `chef-mate match certificates (read)`
   - **Resource owner**: **`Plus-Mobile-Apps`** (the org owns the cert repo;
     a personally-owned token will hit `403 Write access to repository not
     granted` even on a clone).
   - **Expiration**: pick a date and calendar it.
   - **Repository access**: **Only select repositories** → `Plus-Mobile-Apps/certificates`.
   - **Repository permissions** → **Contents: Read-only**, Metadata: Read-only.
   Approve via org settings if pending.
2. Base64-encode `username:token`. Use `printf`, not `echo` — a trailing
   newline silently breaks Basic Auth:

   ```bash
   printf 'Plus-Mobile-Apps:<PAT>' | base64
   ```

   The username field is essentially decorative for fine-grained PATs; what
   matters is that the token itself is valid.
3. Paste the entire base64 string into the `MATCH_GIT_BASIC_AUTHORIZATION`
   secret at
   <https://github.com/Plus-Mobile-Apps/chef-mate/settings/secrets/actions>.

If the `match` step fails with `Error cloning certificates git repo` or
`The requested URL returned error: 403`, the PAT has expired, been revoked,
or was issued under the wrong Resource owner. Regenerate and re-encode.

### App Store Connect API Key

1. Go to [App Store Connect > Users and Access > Integrations > App Store Connect API](https://appstoreconnect.apple.com/access/integrations/api)
2. Create a new key with "App Manager" role
3. Download the `.p8` file (you can only download it once)
4. Base64-encode the key content:

```bash
base64 -i AuthKey_XXXXXXXXXX.p8 | pbcopy
```

5. Set the three secrets:
   - `ASC_KEY_ID` — the Key ID shown in App Store Connect
   - `ASC_ISSUER_ID` — the Issuer ID shown at the top of the API keys page
   - `ASC_API_KEY` — the base64-encoded `.p8` content

---

## Local Testing

You can run the Fastlane lanes locally to test the build steps (uploading will require valid credentials).

If you have a `keystore.properties` file at the project root, no environment variables are needed for Android builds:

```bash
# Android — build the AAB locally (uses keystore.properties)
./gradlew :client:composeApp:bundleRelease

# iOS — run the full lane (requires match setup and Xcode)
bundle exec fastlane ios release

# Desktop — build release packages for your current OS (hides developer-only UI).
# Output lands in build/compose/binaries/main-release/. Use the plain package* tasks
# only for local debug builds where you want the Developer Settings row visible.
./gradlew :client:composeApp:packageReleasePkg    # macOS (Mac App Store .pkg; unsigned without certs)
./gradlew :client:composeApp:packageReleaseMsi    # Windows
./gradlew :client:composeApp:packageReleaseDeb    # Linux
```

## Version Management

Versions are defined in two files across three platforms:

| Platform | File | Fields |
|----------|------|--------|
| Android | `client/composeApp/build.gradle.kts` | `versionCode` / `versionName` |
| iOS | `iosApp/Configuration/Config.xcconfig` | `CURRENT_PROJECT_VERSION` / `MARKETING_VERSION` |
| Desktop | `client/composeApp/build.gradle.kts` | `packageVersion` (generic + macOS) |

### CI version bump (recommended)

The normal release path is fully automated and **does not require running anything locally or creating tags by hand**:

1. Trigger the **Bump Version** workflow (`bump-version.yml`) from the GitHub Actions UI (`workflow_dispatch`), choosing `major` / `minor` / `patch` / `custom`.
2. It updates all version fields (Android, iOS, Desktop) and opens a PR (`chore: bump version to X.Y.Z`).
3. Merging that PR triggers `build-release.yml`, which creates the `vX.Y.Z` tag and publishes the GitHub Release.
4. The tag push then triggers `android-release`, `desktop-release`, and `ios-release` (requires `RELEASE_TOKEN`).

See [ci/VERSION_BUMP_SETUP.md](ci/VERSION_BUMP_SETUP.md) for the required `RELEASE_TOKEN` configuration.

### Bump script (local alternative)

For local bumps (e.g. when not using the CI workflow), `scripts/bump-version.sh` updates all version fields at once. Run it with no arguments for interactive mode:

```bash
./scripts/bump-version.sh
```

This displays the current versions and presents four options:

1. **Increment build number only** — keeps the current version, bumps the build code
2. **Bump patch** — increments the patch version (e.g. `0.1.18` → `0.1.19`) and build number
3. **Custom version** — prompts for a version (`X.Y.Z`), auto-increments build number
4. **Custom version and build number** — prompts for both values

CLI flags are also available for non-interactive use:

```bash
./scripts/bump-version.sh --build                          # build number only
./scripts/bump-version.sh --version 0.2.0                  # set version, auto-increment build
./scripts/bump-version.sh --version 1.0.0 --build-number 1 # set both
```

## Known Limitations

- **Windows MSI and Linux DEB are unsigned.** macOS ships a Store-signed `.pkg` to the Mac App Store (see [macOS: Mac App Store](#macos-mac-app-store)) and Windows ships via the Microsoft Store (Store-signed), so this only affects the fallback MSI and the Linux DEB.
- **MSIX packaging logo assets are placeholders** — CI reuses the app icon for all three Store logos, which won't pass Store certification. Replace with correctly-sized PNGs (see [Windows: Microsoft Store (MSIX)](#windows-microsoft-store-msix)).
- **MSStore CLI is in preview** — the `publish-store` job's action ref and `publish` flags should be verified against current docs on first real submission.
- **Store releases lag** — Microsoft Store certification review delays Windows releases relative to the GitHub Release and mobile stores.
- **R8/ProGuard is disabled** — Android release builds are not minified. Enabling R8 requires ProGuard rules for KMP libraries (Ktor, Supabase, kotlinx.serialization, Decompose).

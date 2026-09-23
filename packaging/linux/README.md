# Flathub packaging

Flatpak/Flathub packaging for the Linux desktop client. The files here are the ones that get
committed to the app's Flathub repository; they live in this repo so they are reviewed and
version-bumped alongside the code they package.

| File                                     | Purpose                                          |
| ---------------------------------------- | ------------------------------------------------ |
| `com.plusmobileapps.chefmate.yml`         | Flatpak manifest (the submission entry point)     |
| `com.plusmobileapps.chefmate.metainfo.xml`| AppStream metadata — store listing, OARS, releases|
| `com.plusmobileapps.chefmate.desktop`     | Desktop entry exported by the sandbox             |
| `chef-mate.sh`                            | Launcher shim placed at `/app/bin/chef-mate`      |

The app id is `com.plusmobileapps.chefmate`, which Flathub allows because `plusmobileapps.com` is
under our control and serves the app's site at `https://chefmate.plusmobileapps.com`.

## Why this is not built from source

Flathub prefers manifests that compile the app inside the build sandbox, with no network access.
That is not achievable for this app today:

- Every `client/*` module applies the Android Gradle plugin, so the whole Gradle graph — including
  the JVM target — requires the Android SDK to configure. The SDK is not redistributable and cannot
  be a Flatpak source, so even a JVM-only entry point cannot resolve.
- Flathub builds are offline. A Gradle build of this size would need every Maven artifact, the
  Gradle distribution, and the Kotlin compiler pinned as manifest sources (via something like
  `flatpak-gradle-generator`), regenerated on each dependency change.

So the manifest consumes the jpackage app image tarball published by the `Desktop Release` workflow,
the same approach Flathub accepts for other large JVM apps (for example IntelliJ IDEA Community,
which is Apache-2.0 and still ships an upstream-built tarball). **Expect reviewers to ask about
this** — the answer is the two bullets above. If they insist on a source build, the prerequisite is
splitting a JVM-only variant of the module graph that never applies the Android plugin.

The tarball already contains a jlink'd JRE produced by jpackage, so the manifest needs no OpenJDK
SDK extension.

## Runtime choices

- **`org.gnome.Platform`, not `org.freedesktop.Platform`** — JavaFX (the in-app recipe browser's
  WebView) links against GTK 3, which the Freedesktop runtime does not ship.
- **`--socket=x11`, not `--socket=fallback-x11`** — Compose Desktop renders through AWT/Skiko and
  JavaFX through GTK; neither has a Wayland backend, so both need XWayland even in a Wayland
  session. `chef-mate.sh` pins `GDK_BACKEND=x11` so GDK does not pick Wayland out from under JavaFX.
- **`--filesystem=xdg-pictures`, `xdg-documents`, `xdg-download`** — the pickers are
  `java.awt.FileDialog`, which browses the sandbox filesystem directly instead of going through the
  document portal. Without these, photo import and recipe archive import/export cannot see anything.

## Code paths that are Flatpak-aware

- `platform/Flatpak.kt` — `isRunningInFlatpak`, detected from `FLATPAK_ID` / `/.flatpak-info`.
- `update/DesktopUpdater.kt` — the in-app updater is disabled under Flatpak. Flathub owns updates,
  and the sandbox is read-only, so downloading and running a `.deb` cannot work.
- `deeplink/SchemeRegistrar.kt` — self-registration of `chefmate://` is skipped; the exported
  `.desktop` file declares `x-scheme-handler/chefmate` instead.
- `database/DriverFactory.jvm.kt` — the Linux data directory honors `XDG_DATA_HOME`, which Flatpak
  points at `~/.var/app/com.plusmobileapps.chefmate/data`. The hardcoded `~/.local/share` is not
  writable inside the sandbox.
- `main.kt` — sets `sun.awt.X11.XToolkit.awtAppClassName` so `WM_CLASS` matches the `.desktop`
  file's `StartupWMClass` and the window binds to the right dock icon.

## Build and test locally (needs a Linux machine)

```bash
flatpak install -y flathub org.flatpak.Builder org.gnome.Platform//50 org.gnome.Sdk//50
```

```bash
flatpak run --command=flathub-build org.flatpak.Builder --install packaging/linux/com.plusmobileapps.chefmate.yml
```

```bash
flatpak run com.plusmobileapps.chefmate
```

```bash
flatpak run --command=flatpak-builder-lint org.flatpak.Builder manifest packaging/linux/com.plusmobileapps.chefmate.yml
```

```bash
flatpak run --command=flatpak-builder-lint org.flatpak.Builder appstream packaging/linux/com.plusmobileapps.chefmate.metainfo.xml
```

Both linters must pass clean before submitting.

## Before the first submission

1. **Screenshots.** `com.plusmobileapps.chefmate.metainfo.xml` points at
   `https://chefmate.plusmobileapps.com/flathub/*.png`, which do not exist yet. Capture them from
   the Linux build and host them at those URLs (or edit the URLs to wherever they land). At least
   one is required, and the first is the hero image on the store page.
2. **Tarball on the release.** Cut a release so
   `chef-mate-<version>-linux-x86_64.tar.gz` exists as a GitHub Release asset, then put its real
   `sha256sum` in the manifest — the checked-in value is a placeholder.
3. **Verify the runtime branch.** The manifest pins GNOME 50; confirm it is still current with
   `flatpak remote-ls flathub --runtime | grep org.gnome.Platform`.
4. **Read the Flathub requirements**, in particular the generative AI policy at
   <https://docs.flathub.org/docs/for-app-authors/requirements> — it applies to the app, the
   manifest, the PR description, and the review replies.

## Submitting

Per <https://docs.flathub.org/docs/for-app-authors/submission>:

1. Fork <https://github.com/flathub/flathub> with "Copy the master branch only" **unchecked**.
2. Clone the `new-pr` branch and create a branch named `com.plusmobileapps.chefmate`.
3. Add the four files from this directory at the repository root.
4. Open a PR against the `new-pr` base branch — **not** `master`.
5. Do not close the PR while addressing feedback, and do not merge `master` into the branch.

After approval Flathub creates `flathub/com.plusmobileapps.chefmate`. Accept the repo invite within
a week and enable 2FA on the GitHub account.

## Maintaining

The manifest carries `x-checker-data`, so Flathub's external-data-checker opens a version-bump PR
against the app repo when a new GitHub Release publishes a matching tarball. Merging that PR is what
ships the update — nothing in this repository publishes to Flathub directly.

To claim the app as verified, follow
<https://docs.flathub.org/docs/for-app-authors/verification> (a token under
`https://plusmobileapps.com/.well-known/org.flathub.VerifiedApps.txt`).

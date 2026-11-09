# Deep Linking Setup

This guide covers how each platform is configured so that links open Chef Mate. For the list of supported URIs, how they map to screens, and how to add a new one, see [navigation-deeplinks.md](navigation-deeplinks.md).

> Email verification no longer relies on deep links. Sign-up, passwordless sign-in, and email change all use an 8-digit code that the user enters in the app (`AuthenticationRepository.verifyEmailOtp` with an `OtpFlow`). See [EMAIL_VERIFICATION_GUIDE.md](EMAIL_VERIFICATION_GUIDE.md).

## Overview

Chef Mate accepts two link forms. Both are parsed by the same `DeepLink.parse` in `client/root/public/.../root/DeepLink.kt`:

| Form | Example | Used by |
|---|---|---|
| Custom scheme | `chefmate://recipe/42` | Dev/QA shortcuts, the iOS share extension (`chefmate://import?url=…`), the desktop apps |
| Verified web link | `https://chefmate.plusmobileapps.com/notifications` | Invite emails, public recipe share links |

The scheme and web host are defined once in `client/shared/src/commonMain/kotlin/com/plusmobileapps/chefmate/ChefMateUrls.kt` (`SCHEME`, `WEB_HOST`). For the web form, `DeepLink.parse` only accepts an exact host match, so a lookalike host such as `chefmate.plusmobileapps.com.evil.com` resolves to `DeepLink.None`.

Every platform sends links into `RootBloc` in one of two ways:

- **Cold start**: the link is parsed and passed to `RootBloc.Factory.create(deepLink = …)`, which uses it as the initial navigation stack.
- **Warm start**: the URL string goes to `RootBloc.handleDeepLink(url)`, which brings the destination to the front of the running stack.

Neither path does anything until onboarding has been completed.

## Android

**Files:** `client/composeApp/src/androidMain/AndroidManifest.xml`, `MainActivity.kt`, `res/values/strings.xml`

`MainActivity` uses `android:launchMode="singleTask"` and declares two `VIEW` intent filters:

```xml
<!-- chefmate:// deeplinks (dev convenience) -->
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="chefmate" />
</intent-filter>

<!-- Verified https App Links -->
<intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="chefmate.plusmobileapps.com" android:pathPrefix="/notifications" />
    <data android:scheme="https" android:host="chefmate.plusmobileapps.com" android:pathPrefix="/recipe" />
</intent-filter>
```

- **App Links verification**: the hosted `https://chefmate.plusmobileapps.com/.well-known/assetlinks.json` proves domain ownership. It is domain-level, so adding a new path only needs a new `pathPrefix` here.
- **Credential sharing**: the `asset_statements` `<meta-data>` (pointing at the same `assetlinks.json`) links the app to the website for password-manager autofill.
- **Cold start**: `MainActivity.onCreate` parses `intent.data` with `DeepLink.parse` and passes the result to `buildRootBloc(deepLink = …)`.
- **Warm start**: `onNewIntent` forwards `ACTION_VIEW` data to `rootBloc.handleDeepLink(url)`.
- `ACTION_SEND` `text/plain` intents (sharing a URL from a browser) are a separate flow and go to `rootBloc.handleSharedUrl`.

## iOS

**Files:** `iosApp/iosApp/Info.plist`, `iosApp/iosApp/iosApp.entitlements`, `iosApp/iosApp/iOSApp.swift`, `iosApp/iosApp/ContentView.swift`, `client/composeApp/src/iosMain/.../RootBlocProvider.kt`

iOS supports both the custom scheme and Universal Links.

**Custom scheme.** `Info.plist` registers `chefmate` under `CFBundleURLTypes`:

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLName</key>
        <string>com.plusmobileapps.chefmate</string>
        <key>CFBundleURLSchemes</key>
        <array><string>chefmate</string></array>
    </dict>
</array>
```

**Universal Links and Associated Domains.** `iosApp.entitlements` (set as `CODE_SIGN_ENTITLEMENTS` for both Debug and Release) declares:

```xml
<key>com.apple.developer.associated-domains</key>
<array>
    <string>webcredentials:chefmate.plusmobileapps.com</string>
    <string>applinks:chefmate.plusmobileapps.com</string>
</array>
```

`webcredentials:` enables password autofill. `applinks:` enables Universal Links. The entitlement is not scoped to paths. The paths that open the app are listed in the `apple-app-site-association` file, which is hosted in the separate `chefmate-site` repo. A new `https` path needs that file updated and redeployed, separately from the app release.

**URL handling:**

- **Cold start (custom scheme)**: `AppDelegate.application(_:didFinishLaunchingWithOptions:)` reads `launchOptions[.url]` (skipping `chefmate://import`) and passes it to `RootBlocProvider.buildRootBloc(…, deepLinkUrl:)`, which calls `DeepLink.parse`.
- **Any link, cold or warm**: `ContentView` attaches `.onOpenURL`, which receives both `chefmate://…` and `https://` Universal Links:
  - `chefmate://import?url=…` (from the share extension, `iosApp/ShareExtension/ShareViewController.swift`) → `root.handleSharedUrl(url:)`
  - anything else → `root.handleDeepLink(url:)`
- **Fallback**: `.onContinueUserActivity(NSUserActivityTypeBrowsingWeb)` also forwards `activity.webpageURL` to `handleDeepLink`, because some iOS versions deliver Universal Links that way. `handleDeepLink` is idempotent, so a link delivered through both paths is harmless.

## Desktop (JVM)

**Files:** `client/composeApp/build.gradle.kts` (`macOS { infoPlist { … } }`), `client/composeApp/src/jvmMain/.../main.kt`, `deeplink/SchemeRegistrar.kt`, `deeplink/SingleInstance.kt`, `deeplink/DeepLinkCoordinator.kt`

Desktop apps cannot claim `https://` links. Instead, the web pages on `chefmate.plusmobileapps.com` (for example `/notifications`) redirect the browser to the matching `chefmate://` URL, and the app registers that scheme:

- **macOS**: `CFBundleURLTypes` is set in the packaged Info.plist from `build.gradle.kts`. The URL arrives as an Apple Event, which `Desktop.setOpenURIHandler` in `main.kt` handles for both cold and warm launches.
- **Windows / Linux**: `SchemeRegistrar.registerIfPackaged()` registers the scheme for the current user at launch: a registry entry on Windows, a `.desktop` handler in `~/.local/share/applications` on Linux. It needs no admin rights, is safe to run repeatedly, and is skipped under `./gradlew run`. Each link opens a new process, so `SingleInstance` uses a file lock and a loopback socket to forward the link to the running instance.
- Warm links from every source go through `DeepLinkCoordinator` (a `SharedFlow` with `replay = 1`, which covers the macOS cold-launch race) and then to `rootBloc.handleDeepLink(url)`. The cold-start link comes from the first CLI argument.

## Testing

See [navigation-deeplinks.md](navigation-deeplinks.md) for per-platform commands (`adb shell am start`, `xcrun simctl openurl`, `./gradlew :client:composeApp:run --args=…`, `open` / `xdg-open`). To test verified links:

```bash
# Android: check App Links verification state
adb shell pm get-app-links com.plusmobileapps.chefmate

# Android: open a verified https link
adb shell am start -W -a android.intent.action.VIEW -d "https://chefmate.plusmobileapps.com/notifications"

# iOS simulator: open a Universal Link
xcrun simctl openurl booted "https://chefmate.plusmobileapps.com/notifications"
```

## Troubleshooting

**An `https` link opens the browser instead of the app**
- Android: run `pm get-app-links` and confirm the domain shows `verified`. Check that the path prefix is in the `autoVerify` filter and that `assetlinks.json` contains the signing certificate fingerprint for the build you installed (debug and release fingerprints are different).
- iOS: confirm the path is listed in the hosted `apple-app-site-association`. iOS caches this file, so reinstall the app after changing it. Universal Links do not open the app when typed into Safari's address bar; tap them from Notes, Mail, or Messages.

**The app opens but stays on the default screen**
- Onboarding has not been completed. Deep links are ignored until it is.
- The URI is not recognized and parsed to `DeepLink.None`. Check `DeepLinkTest` for the accepted forms.

## Related Files

- `client/root/public/.../root/DeepLink.kt` - URI model and parser
- `client/root/impl/.../root/RootBlocImpl.kt` - `initialStackFor` (cold) and `handleDeepLink` (warm)
- `client/shared/.../ChefMateUrls.kt` - scheme and web host constants
- `client/composeApp/src/androidMain/AndroidManifest.xml` - Android intent filters
- `iosApp/iosApp/Info.plist`, `iosApp/iosApp/iosApp.entitlements` - iOS scheme and Associated Domains
- `iosApp/iosApp/ContentView.swift` - iOS `onOpenURL` and `onContinueUserActivity` handlers
- `client/composeApp/src/jvmMain/.../deeplink/` - desktop scheme registration and single-instance forwarding

## References

- [Android App Links](https://developer.android.com/training/app-links)
- [Apple: Supporting Universal Links](https://developer.apple.com/documentation/xcode/supporting-universal-links-in-your-app)
- [Apple: Associated Domains](https://developer.apple.com/documentation/xcode/supporting-associated-domains)

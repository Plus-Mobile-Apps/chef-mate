package com.plusmobileapps.chefmate.platform

/**
 * Whether this process is running inside the Flatpak sandbox (the Flathub build of the Linux app).
 *
 * Flatpak exports `FLATPAK_ID` into every sandboxed process, and also creates `/.flatpak-info`
 * inside the sandbox; either is a reliable signal, and both are checked because `FLATPAK_ID` can be
 * dropped by a wrapper that scrubs the environment.
 *
 * Behavior that the sandbox owns rather than the app must be skipped when this is true — the app
 * cannot replace its own installation (Flathub publishes updates), and it cannot register OS-level
 * handlers itself (the exported `.desktop` file does that declaratively).
 */
val isRunningInFlatpak: Boolean by lazy {
    !System.getenv("FLATPAK_ID").isNullOrBlank() || java.io.File("/.flatpak-info").exists()
}

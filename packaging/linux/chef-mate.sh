#!/bin/sh
# Flatpak launcher shim. The jpackage app image lives at /app/chef-mate; its bin/chef-mate launcher
# resolves the bundled jlink runtime relative to itself, so it must be exec'd in place rather than
# symlinked into /app/bin.
set -e

# JavaFX (the in-app recipe browser's WebView) has no Wayland backend and aborts if GDK picks one,
# so pin GDK to X11 — the manifest grants --socket=x11, which gives XWayland in Wayland sessions.
export GDK_BACKEND=x11

exec /app/chef-mate/bin/chef-mate "$@"

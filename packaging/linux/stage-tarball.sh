#!/usr/bin/env bash
# Stages the tarball the Flathub manifest consumes: the jpackage app image (bundled jlink runtime
# included, so the flatpak needs no JDK extension), the hicolor icon sizes, and the license.
#
# Usage: packaging/linux/stage-tarball.sh <version> <output-dir>
# Run after `./gradlew :client:composeApp:createReleaseDistributable`. The Desktop Release workflow
# calls this; run it locally to build the flatpak against an unreleased app image.
set -euo pipefail

VERSION="$1"
OUT_DIR="$2"
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

# jpackage names the app image directory after the platform packageName, so glob for it instead of
# hardcoding a name the Gradle config could change out from under us.
APP_DIR=$(find "$ROOT/client/composeApp/build/compose/binaries/main-release/app" \
  -mindepth 1 -maxdepth 1 -type d | head -n1)
if [ -z "$APP_DIR" ]; then echo "No app image produced" >&2; exit 1; fi

# The launcher is named after the top-level packageName ("Chef Mate"), and jpackage resolves its
# lib/app/<name>.cfg from that name, so it can't be renamed. Fail here rather than in the flatpak
# build if it changes; chef-mate.sh execs it by this exact name.
if [ ! -x "$APP_DIR/bin/Chef Mate" ]; then
  echo "Expected launcher 'bin/Chef Mate' in $APP_DIR" >&2
  exit 1
fi

STAGE_PARENT=$(mktemp -d)
trap 'rm -rf "$STAGE_PARENT"' EXIT
STAGE="$STAGE_PARENT/chef-mate-${VERSION}"
mkdir -p "$STAGE/icons"
cp -a "$APP_DIR" "$STAGE/app"
cp "$ROOT/LICENSE" "$STAGE/LICENSE"

# Flathub wants icons at the standard hicolor sizes; the source is 584x584, which is not one of
# them, so resize rather than shipping a mis-binned icon.
IM=$(command -v magick || command -v convert)
for size in 64 128 256 512; do
  "$IM" "$ROOT/client/composeApp/src/jvmMain/resources/app-icon.png" \
    -resize "${size}x${size}" "$STAGE/icons/${size}.png"
done

mkdir -p "$OUT_DIR"
tar -czf "$OUT_DIR/chef-mate-${VERSION}-linux-x86_64.tar.gz" -C "$STAGE_PARENT" "chef-mate-${VERSION}"
echo "$OUT_DIR/chef-mate-${VERSION}-linux-x86_64.tar.gz"

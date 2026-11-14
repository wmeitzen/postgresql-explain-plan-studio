#!/usr/bin/env bash
# Build a drag-to-Applications macOS .app (+ .dmg) with a bundled JRE via jpackage.
#
# Must run on macOS (Intel → mac-x64, Apple Silicon → mac-arm64). Cannot be built on Windows/Linux.
#
# Prerequisites: JDK 17+ with jpackage on PATH (Temurin recommended).
#
# Usage (from repo root, preferably a local disk copy — not a VMware HGFS share):
#   ./scripts/pack-mac-app.sh
#   ./scripts/pack-mac-app.sh --skip-build   # reuse existing shaded jar
#
# Outputs (under target/):
#   PostgreSQLExplainPlanStudio.app/   (also under target/jpackage-mac/)
#   PostgreSQLExplainPlanStudio-<ver>-mac-<arch>.dmg
#   PostgreSQLExplainPlanStudio-<ver>-mac-<arch>.zip   (.app inside)
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

SKIP_BUILD=0
for arg in "$@"; do
  case "$arg" in
    --skip-build|-SkipBuild) SKIP_BUILD=1 ;;
    -h|--help)
      sed -n '2,20p' "$0"
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      exit 1
      ;;
  esac
done

if [[ "$(uname -s)" != "Darwin" ]]; then
  echo "pack-mac-app.sh must run on macOS (jpackage cannot produce a .app on $(uname -s))." >&2
  exit 1
fi

if ! command -v java >/dev/null 2>&1 || ! command -v jpackage >/dev/null 2>&1; then
  echo "JDK 17+ with jpackage is required. Install Temurin and ensure java/jpackage are on PATH." >&2
  exit 1
fi

# HGFS / shared folders often corrupt concurrent Maven outputs — warn loudly.
case "$REPO_ROOT" in
  /Volumes/*|/mnt/hgfs/*)
    echo "WARNING: Building on a VMware/network share ($REPO_ROOT)." >&2
    echo "Prefer a local copy (e.g. rsync to ~/peps) if the jar/.app looks truncated or fails to launch." >&2
    ;;
esac

log() { printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"; }

VERSION="$(
  # shellcheck disable=SC2016
  sed -n 's/.*<jpackage\.appVersion>\([^<]*\)<\/jpackage\.appVersion>.*/\1/p' pom.xml | head -1
)"
if [[ -z "${VERSION}" ]]; then
  VERSION="$(sed -n 's/.*<version>\([^<]*\)<\/version>.*/\1/p' pom.xml | head -1)"
fi
if [[ -z "${VERSION}" ]]; then
  echo "Could not read version from pom.xml" >&2
  exit 1
fi

ARCH_RAW="$(uname -m)"
case "$ARCH_RAW" in
  x86_64) ARCH_LABEL="x64" ;;
  arm64|aarch64) ARCH_LABEL="arm64" ;;
  *) ARCH_LABEL="$ARCH_RAW" ;;
esac

APP_NAME="PostgreSQLExplainPlanStudio"
DISPLAY_NAME="PostgreSQL Explain Plan Studio"
MAIN_CLASS="com.williammeitzen.peps.PostgreSQLExplainPlanStudio"
JAR_NAME="postgresql-explain-plan-studio-${VERSION}.jar"
JAR_PATH="$REPO_ROOT/target/$JAR_NAME"
ICON_PNG="$REPO_ROOT/src/main/resources/images/app-icon.png"
ICON_ICNS="$REPO_ROOT/packaging/mac/peps.icns"
ICONSET_DIR="$REPO_ROOT/target/peps.iconset"
JPACKAGE_DEST="$REPO_ROOT/target/jpackage-mac"
INPUT_DIR="$REPO_ROOT/target/jpackage-mac-input"
DMG_NAME="${APP_NAME}-${VERSION}-mac-${ARCH_LABEL}.dmg"
ZIP_NAME="${APP_NAME}-${VERSION}-mac-${ARCH_LABEL}.zip"
DMG_PATH="$REPO_ROOT/target/$DMG_NAME"
ZIP_PATH="$REPO_ROOT/target/$ZIP_NAME"

if [[ "$SKIP_BUILD" -eq 0 ]]; then
  log "Building shaded jar (./mvnw -DskipTests package)…"
  chmod +x mvnw 2>/dev/null || true
  ./mvnw -DskipTests package
else
  log "Skipping Maven package (--skip-build)"
fi

if [[ ! -f "$JAR_PATH" ]]; then
  echo "Missing shaded jar: $JAR_PATH" >&2
  echo "Run without --skip-build, or ./mvnw -DskipTests package first." >&2
  exit 1
fi

if [[ ! -f "$ICON_PNG" ]]; then
  echo "Missing app icon PNG: $ICON_PNG" >&2
  exit 1
fi

log "Generating packaging/mac/peps.icns from app-icon.png…"
rm -rf "$ICONSET_DIR"
mkdir -p "$ICONSET_DIR"
# iconutil expects specific filenames; generate from master PNG.
for size in 16 32 128 256 512; do
  sips -z "$size" "$size" "$ICON_PNG" --out "$ICONSET_DIR/icon_${size}x${size}.png" >/dev/null
  dbl=$((size * 2))
  sips -z "$dbl" "$dbl" "$ICON_PNG" --out "$ICONSET_DIR/icon_${size}x${size}@2x.png" >/dev/null
done
iconutil -c icns "$ICONSET_DIR" -o "$ICON_ICNS"
rm -rf "$ICONSET_DIR"

log "Preparing jpackage input…"
rm -rf "$INPUT_DIR" "$JPACKAGE_DEST"
mkdir -p "$INPUT_DIR" "$JPACKAGE_DEST"
cp -f "$JAR_PATH" "$INPUT_DIR/"

log "jpackage app-image ($ARCH_LABEL)…"
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --vendor "William Meitzen" \
  --description "$DISPLAY_NAME" \
  --icon "$ICON_ICNS" \
  --dest "$JPACKAGE_DEST" \
  --input "$INPUT_DIR" \
  --main-jar "$JAR_NAME" \
  --main-class "$MAIN_CLASS" \
  --mac-package-identifier "com.williammeitzen.peps" \
  --mac-package-name "$DISPLAY_NAME" \
  --java-options "-Dapple.awt.application.name=$DISPLAY_NAME"

APP_BUNDLE="$JPACKAGE_DEST/${APP_NAME}.app"
if [[ ! -d "$APP_BUNDLE" ]]; then
  echo "jpackage did not create $APP_BUNDLE" >&2
  exit 1
fi

log "jpackage DMG…"
# Build DMG from the app-image so we do not re-bundle the runtime.
rm -f "$JPACKAGE_DEST"/*.dmg "$DMG_PATH"
jpackage \
  --type dmg \
  --app-image "$APP_BUNDLE" \
  --name "$APP_NAME" \
  --app-version "$VERSION" \
  --vendor "William Meitzen" \
  --description "$DISPLAY_NAME" \
  --dest "$JPACKAGE_DEST" \
  --mac-package-identifier "com.williammeitzen.peps" \
  --mac-package-name "$DISPLAY_NAME"

shopt -s nullglob
dmgs=("$JPACKAGE_DEST"/*.dmg)
shopt -u nullglob
if [[ ${#dmgs[@]} -ne 1 ]]; then
  echo "Expected exactly one .dmg from jpackage; found ${#dmgs[@]}" >&2
  printf '  %s\n' "${dmgs[@]}" >&2
  exit 1
fi
mv -f "${dmgs[0]}" "$DMG_PATH"

log "Zipping .app → $ZIP_NAME…"
rm -f "$ZIP_PATH"
(
  cd "$JPACKAGE_DEST"
  ditto -c -k --sequesterRsrc --keepParent "$APP_NAME.app" "$ZIP_PATH"
)

log "Done."
echo "  App:  $APP_BUNDLE"
echo "  DMG:  $DMG_PATH"
echo "  ZIP:  $ZIP_PATH"
echo "Install: open the DMG and drag $APP_NAME.app to Applications."
echo "Note: unsigned builds may need right-click → Open the first time (Gatekeeper)."

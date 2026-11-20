#!/usr/bin/env bash
# Build a drag-to-Applications macOS .app (+ .dmg) with a bundled JRE.
# .app via jpackage; .dmg via hdiutil (Applications symlink — works over SSH, no SetFile/CLT).
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
#   PostgreSQL Explain Plan Studio.app/   (under target/jpackage-mac/; spaces OK for Finder)
#   PostgreSQLExplainPlanStudio-<ver>-mac-<arch>.dmg
#   PostgreSQLExplainPlanStudio-<ver>-mac-<arch>.zip   (.app inside; zip filename stays unspaced)
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

# macOS jpackage rejects app-version whose first integer is 0 (e.g. 0.0.19).
# Keep real project VERSION in artifact filenames; only --app-version is remapped.
MAC_APP_VERSION="$VERSION"
if [[ "$VERSION" =~ ^0\.([0-9]+(\.[0-9]+)?)$ ]]; then
  MAC_APP_VERSION="1.${BASH_REMATCH[1]}"
  log "macOS jpackage app-version: ${VERSION} -> ${MAC_APP_VERSION} (first component cannot be 0)"
elif [[ "$VERSION" =~ ^0\. ]]; then
  MAC_APP_VERSION="1.0.0"
  log "macOS jpackage app-version: ${VERSION} -> ${MAC_APP_VERSION} (first component cannot be 0)"
fi

ARCH_RAW="$(uname -m)"
case "$ARCH_RAW" in
  x86_64) ARCH_LABEL="x64" ;;
  arm64|aarch64) ARCH_LABEL="arm64" ;;
  *) ARCH_LABEL="$ARCH_RAW" ;;
esac

# Artifact / jpackage --name stay unspaced (paths, download URLs). Finder label uses DISPLAY_NAME.app.
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
  log "Building shaded jar (./mvnw -DskipTests package)..."
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

log "Generating packaging/mac/peps.icns from app-icon.png..."
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

log "Preparing jpackage input..."
rm -rf "$INPUT_DIR" "$JPACKAGE_DEST"
mkdir -p "$INPUT_DIR" "$JPACKAGE_DEST"
cp -f "$JAR_PATH" "$INPUT_DIR/"

log "jpackage app-image (${ARCH_LABEL})..."
jpackage \
  --type app-image \
  --name "$APP_NAME" \
  --app-version "$MAC_APP_VERSION" \
  --vendor "William Meitzen" \
  --description "$DISPLAY_NAME" \
  --icon "$ICON_ICNS" \
  --dest "$JPACKAGE_DEST" \
  --input "$INPUT_DIR" \
  --main-jar "$JAR_NAME" \
  --main-class "$MAIN_CLASS" \
  --mac-package-identifier "com.williammeitzen.peps" \
  --mac-package-name "${DISPLAY_NAME}"
# Do NOT pass -Dapple.awt.application.name=... via --java-options when the value
# has spaces: the launcher splits argv and Java loads main class "Explain".
# main() already sets apple.awt.application.name before AWT.
# jpackage may warn that mac-package-name is >16 characters — that is OK.

# jpackage creates an unspaced .app; rename so Finder matches QuickTime-style labels.
JPACKAGE_BUNDLE="${JPACKAGE_DEST}/${APP_NAME}.app"
APP_BUNDLE="${JPACKAGE_DEST}/${DISPLAY_NAME}.app"
if [[ ! -d "${JPACKAGE_BUNDLE}" ]]; then
  echo "jpackage did not create ${JPACKAGE_BUNDLE}" >&2
  exit 1
fi
rm -rf "${APP_BUNDLE}"
mv "${JPACKAGE_BUNDLE}" "${APP_BUNDLE}"
log "Renamed bundle -> ${DISPLAY_NAME}.app (Finder label)"

INFO_PLIST="${APP_BUNDLE}/Contents/Info.plist"
if [[ ! -f "${INFO_PLIST}" ]]; then
  echo "Missing Info.plist at ${INFO_PLIST}" >&2
  exit 1
fi
# Quote the value — unquoted spaces make PlistBuddy treat only the first word as the value.
log "Setting CFBundleDisplayName / CFBundleName to '${DISPLAY_NAME}'..."
/usr/libexec/PlistBuddy -c "Set :CFBundleDisplayName '${DISPLAY_NAME}'" "${INFO_PLIST}" 2>/dev/null \
  || /usr/libexec/PlistBuddy -c "Add :CFBundleDisplayName string '${DISPLAY_NAME}'" "${INFO_PLIST}"
/usr/libexec/PlistBuddy -c "Set :CFBundleName '${DISPLAY_NAME}'" "${INFO_PLIST}" 2>/dev/null \
  || /usr/libexec/PlistBuddy -c "Add :CFBundleName string '${DISPLAY_NAME}'" "${INFO_PLIST}"
log "Info.plist names:"
/usr/libexec/PlistBuddy -c "Print :CFBundleName" "${INFO_PLIST}" || true
/usr/libexec/PlistBuddy -c "Print :CFBundleDisplayName" "${INFO_PLIST}" || true

# DMG via hdiutil (not jpackage --type dmg). jpackage's DMG path calls SetFile, which
# needs Xcode CLT and pops a GUI installer dialog that hangs headless SSH builds.
# hdiutil needs no CLT and still gives the drag-.app-to-Applications volume.
# Set PEPS_SKIP_MAC_DMG=1 to skip and ship .app zip only.
SKIP_DMG=0
if [[ "${PEPS_SKIP_MAC_DMG:-0}" == "1" ]]; then
  SKIP_DMG=1
  log "Skipping DMG (PEPS_SKIP_MAC_DMG=1). Shipping .app zip instead."
fi

rm -f "${JPACKAGE_DEST}"/*.dmg "${DMG_PATH}"
if [[ "$SKIP_DMG" -eq 0 ]]; then
  log "Creating DMG with hdiutil (Applications symlink)..."
  DMG_STAGE="${JPACKAGE_DEST}/dmg-stage"
  rm -rf "${DMG_STAGE}"
  mkdir -p "${DMG_STAGE}"
  # Copy the .app (ditto preserves resource forks / Finder metadata better than cp -R).
  if ! ditto "${APP_BUNDLE}" "${DMG_STAGE}/${DISPLAY_NAME}.app"; then
    cp -R "${APP_BUNDLE}" "${DMG_STAGE}/${DISPLAY_NAME}.app"
  fi
  ln -s /Applications "${DMG_STAGE}/Applications"
  # Volname: keep short / no spaces for reliable /Volumes mount paths in scripts.
  DMG_VOLNAME="PEPS-${VERSION}"
  if hdiutil create \
      -volname "${DMG_VOLNAME}" \
      -srcfolder "${DMG_STAGE}" \
      -ov \
      -format UDZO \
      "${DMG_PATH}"
  then
    log "Wrote ${DMG_PATH}"
  else
    echo "WARNING: hdiutil DMG failed. Continuing with .app zip." >&2
    rm -f "${DMG_PATH}"
  fi
  rm -rf "${DMG_STAGE}"
fi

# Zip of the .app — required for remote/dev-release when DMG is skipped.
# Use ${ZIP_NAME} with braces — macOS bash 3.2 + set -u mishandles "$ZIP_NAME…" (Unicode ellipsis).
log "Zipping .app -> ${ZIP_NAME}..."
rm -f "${ZIP_PATH}"
if ditto -c -k --sequesterRsrc --keepParent \
    "${APP_BUNDLE}" "${ZIP_PATH}" 2>/dev/null; then
  log "Wrote ${ZIP_PATH}"
elif (
  cd "${JPACKAGE_DEST}" && zip -qry "${ZIP_PATH}" "${DISPLAY_NAME}.app"
); then
  log "Wrote ${ZIP_PATH} (via zip)"
else
  echo "Could not zip .app (ditto/zip failed)." >&2
  exit 1
fi

log "Done."
echo "  App:  ${APP_BUNDLE}"
if [[ -f "${DMG_PATH}" ]]; then
  echo "  DMG:  ${DMG_PATH}"
fi
echo "  ZIP:  ${ZIP_PATH}"
if [[ -f "${DMG_PATH}" ]]; then
  echo "Install: open \"${DMG_PATH}\" and drag \"${DISPLAY_NAME}.app\" to Applications."
else
  echo "Install: unzip \"${ZIP_PATH}\" and drag \"${DISPLAY_NAME}.app\" to Applications."
fi
echo "Finder label: ${DISPLAY_NAME}.app (download zip filename stays ${ZIP_NAME})."
echo "Note: unsigned builds may need right-click -> Open the first time (Gatekeeper)."

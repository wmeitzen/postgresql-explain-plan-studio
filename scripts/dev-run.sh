#!/usr/bin/env bash
# Launch PEPS from a *copy* of the shaded jar, waiting if a dev-watch build is in progress.
#
# Maven shade rewrites target/postgresql-explain-plan-studio-*.jar in place. If PEPS is
# already running from that jar, Batik SVG loads fail with ZipException (icons go black).
# We copy to target/run/peps-dev.jar and launch that instead.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# shellcheck source=lib-dev-lock.sh
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib-dev-lock.sh"

LOCK_FILE="$REPO_ROOT/target/dev-build.lock"
RUN_DIR="$REPO_ROOT/target/run"
RUN_JAR="$RUN_DIR/peps-dev.jar"
WAIT_TIMEOUT_SEC=300
POLL_SEC=0.5

log() {
  printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"
}

if ! command -v java >/dev/null 2>&1; then
  echo "java not found on PATH. Install JDK 17+ and retry." >&2
  exit 1
fi

# Wait only while a builder holds the lock. A leftover lock *file* after a
# finished/crashed build must not block launch (old watchers left the file behind).
mkdir -p "$REPO_ROOT/target"
announced=0
deadline=$((SECONDS + WAIT_TIMEOUT_SEC))
while true; do
  if peps_lock_try_clear_idle "$LOCK_FILE"; then
    break
  fi
  if (( SECONDS > deadline )); then
    echo "Timed out after ${WAIT_TIMEOUT_SEC}s waiting for recompile to finish ($LOCK_FILE still held)." >&2
    exit 1
  fi
  if [[ "$announced" -eq 0 ]]; then
    log "PEPS is recompiling — waiting…"
    announced=1
  fi
  sleep "$POLL_SEC"
done

jar="$(ls -1t "$REPO_ROOT"/target/postgresql-explain-plan-studio-*.jar 2>/dev/null | grep -v -- '-shaded\.jar$' | grep -v -- '-sources\.jar$' | grep -v -- '-javadoc\.jar$' | head -n 1 || true)"
if [[ -z "$jar" || ! -f "$jar" ]]; then
  echo "No runnable jar in target/. Start scripts/dev-watch.sh first (or run: ./mvnw -DskipTests package)." >&2
  exit 1
fi

mkdir -p "$RUN_DIR"
cp -f "$jar" "$RUN_JAR"
log "Starting peps-dev.jar (copy of $(basename "$jar"); safe while RECOMPILENOW rebuilds)"
JAVA_ARGS=()
if [[ "$(uname -s)" == "Darwin" ]]; then
  # Without -Xdock:icon, macOS shows the Java Duke icon for java -jar launches.
  DOCK_ICON="$REPO_ROOT/src/main/resources/images/app-icon.png"
  JAVA_ARGS+=(-Xdock:name="PostgreSQL Explain Plan Studio")
  if [[ -f "$DOCK_ICON" ]]; then
    JAVA_ARGS+=(-Xdock:icon="$DOCK_ICON")
  fi
fi
exec java "${JAVA_ARGS[@]}" -jar "$RUN_JAR"

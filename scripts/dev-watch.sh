#!/usr/bin/env bash
# Watch PEPS sources and rebuild the shaded jar on change (fast path; not jpackage).
# Keep this running in one terminal while developing. Launch with scripts/dev-run.sh.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

LOCK_FILE="$REPO_ROOT/target/dev-build.lock"
DEBOUNCE_SEC=0.5
BUILD_RUNNING=0
REBUILD_QUEUED=0

log() {
  printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"
}

need_java() {
  if ! command -v java >/dev/null 2>&1; then
    echo "java not found on PATH. Install JDK 17+ and retry." >&2
    exit 1
  fi
}

cleanup() {
  if [[ -f "$LOCK_FILE" ]]; then
    rm -f "$LOCK_FILE"
  fi
  log "Watcher stopped."
}
trap cleanup EXIT INT TERM

is_interesting_path() {
  case "$1" in
    *.java|*.xml|*/pom.xml|pom.xml) return 0 ;;
    *) return 1 ;;
  esac
}

run_build() {
  if [[ "$BUILD_RUNNING" -eq 1 ]]; then
    REBUILD_QUEUED=1
    log "Build already running — queued another rebuild."
    return 0
  fi

  BUILD_RUNNING=1
  while true; do
    REBUILD_QUEUED=0
    mkdir -p "$REPO_ROOT/target"
    printf 'pid=%s\nstarted=%s\n' "$$" "$(date +%Y-%m-%dT%H:%M:%S%z)" >"$LOCK_FILE"
    log "Building…"
    set +e
    ./mvnw -DskipTests package
    exit_code=$?
    set -e
    if [[ "$exit_code" -eq 0 ]]; then
      log "Build OK"
    else
      log "Build FAILED (exit $exit_code)"
    fi
    rm -f "$LOCK_FILE"
    if [[ "$REBUILD_QUEUED" -eq 0 ]]; then
      break
    fi
  done
  BUILD_RUNNING=0
}

watch_with_inotify() {
  log "Watching $REPO_ROOT (src + pom.xml) via inotifywait. Ctrl+C to stop."
  while true; do
    # Block until something changes, then debounce a burst of events.
    changed="$(inotifywait -r -e modify,create,delete,move --format '%w%f' \
      "$REPO_ROOT/src" "$REPO_ROOT/pom.xml" 2>/dev/null || true)"
    if [[ -n "$changed" ]] && ! is_interesting_path "$changed"; then
      continue
    fi
    sleep "$DEBOUNCE_SEC"
    # Drain further events until quiet for ~1s (inotifywait -t times out with status 2)
    while inotifywait -r -t 1 -e modify,create,delete,move \
      "$REPO_ROOT/src" "$REPO_ROOT/pom.xml" >/dev/null 2>&1; do
      sleep "$DEBOUNCE_SEC"
    done
    run_build
  done
}

watch_with_poll() {
  log "inotifywait not found — using poll fallback (every 2s). For event-driven watches:"
  log "  sudo apt install -y inotify-tools"
  local stamp_file
  stamp_file="$(mktemp)"
  touch "$stamp_file"
  while true; do
    if find "$REPO_ROOT/src" \( -name '*.java' -o -name '*.xml' \) -newer "$stamp_file" 2>/dev/null | grep -q . \
      || { [[ -f "$REPO_ROOT/pom.xml" ]] && [[ "$REPO_ROOT/pom.xml" -nt "$stamp_file" ]]; }; then
      sleep "$DEBOUNCE_SEC"
      touch "$stamp_file"
      run_build
      touch "$stamp_file"
    else
      sleep 2
    fi
  done
}

need_java
chmod +x "$REPO_ROOT/mvnw" 2>/dev/null || true

run_build

if command -v inotifywait >/dev/null 2>&1; then
  watch_with_inotify
else
  watch_with_poll
fi

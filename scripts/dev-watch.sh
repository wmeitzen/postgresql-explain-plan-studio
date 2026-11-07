#!/usr/bin/env bash
# Rebuild the shaded jar when RECOMPILENOW appears (fast path; not jpackage).
#
# Cursor creates RECOMPILENOW on the Windows repo (VMware share). When this script
# runs from a local disk copy (~/peps), it watches the *host share* for that flag,
# rsyncs into ~/peps, builds, then deletes the host flag.
#
# If you run it directly on /mnt/hgfs/..., it watches/builds there (slow) and leaves
# the flag for a peer unless PEPS_DELETE_SEMAPHORE=1.
#
# Launch the app with scripts/dev-run.sh.
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

LOCK_FILE="$REPO_ROOT/target/dev-build.lock"
POLL_SEC="${PEPS_WATCH_POLL:-0.5}"
HOST_REPO="${PEPS_HOST_REPO:-}"
REQUEST_STOP=0
CLEANED_UP=0
LAST_BUILT_STAMP=""

log() {
  printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"
}

need_java() {
  if ! command -v java >/dev/null 2>&1; then
    echo "java not found on PATH. Install JDK 17+ and retry." >&2
    exit 1
  fi
}

on_shared_host() {
  case "$REPO_ROOT" in
    /mnt/hgfs/*) return 0 ;;
    *) return 1 ;;
  esac
}

find_host_repo() {
  local candidate
  if [[ -n "${HOST_REPO}" ]]; then
    printf '%s\n' "$HOST_REPO"
    return 0
  fi
  while IFS= read -r candidate; do
    if [[ -f "$candidate/pom.xml" ]] \
      && [[ -d "$candidate/src/main/java/com/williammeitzen/peps" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done < <(find /mnt/hgfs -maxdepth 8 -type d -name 'postgresql-explain-plan-studio' 2>/dev/null)
  for candidate in /mnt/hgfs/peps /mnt/hgfs/PEPS; do
    if [[ -f "$candidate/pom.xml" ]] \
      && [[ -d "$candidate/src/main/java/com/williammeitzen/peps" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

sync_from_host() {
  local host="$1"
  local local="$2"
  mkdir -p "$local"
  rsync -a --delete \
    --exclude '.git/' \
    --exclude 'target/' \
    --exclude '*.sqlite' \
    --exclude '.idea/' \
    --exclude '*.iml' \
    --exclude '.classpath' \
    --exclude '.project' \
    --exclude '.settings/' \
    --exclude 'RECOMPILENOW' \
    "$host/" "$local/"
  chmod +x "$local/mvnw" "$local"/scripts/*.sh 2>/dev/null || true
}

semaphore_stamp() {
  local path="$1"
  if [[ ! -f "$path" ]]; then
    return 1
  fi
  stat -c '%Y %s' "$path" 2>/dev/null || stat -f '%m %z' "$path"
}

cleanup() {
  if [[ "$CLEANED_UP" -eq 1 ]]; then
    return 0
  fi
  CLEANED_UP=1
  log "Watcher stopped."
}

request_stop() {
  REQUEST_STOP=1
}

trap cleanup EXIT
trap request_stop INT TERM

interruptible_sleep() {
  sleep "$1" &
  wait $! 2>/dev/null || true
}

run_build_locked() {
  mkdir -p "$REPO_ROOT/target"
  exec 9>"$LOCK_FILE"
  if ! flock -n 9; then
    log "Another Maven build holds $LOCK_FILE — will retry next poll"
    exec 9>&-
    return 0
  fi
  printf 'pid=%s\nstarted=%s\n' "$$" "$(date +%Y-%m-%dT%H:%M:%S%z)" >"$LOCK_FILE"
  rm -f "$REPO_ROOT"/target/*-shaded.jar
  log "Building in $REPO_ROOT …"
  set +e
  ./mvnw -DskipTests package
  local exit_code=$?
  set -e
  flock -u 9
  exec 9>&-
  # Remove the lock file so dev-run.sh does not wait forever (Windows watcher does the same).
  rm -f "$LOCK_FILE"
  if [[ "$exit_code" -eq 0 ]]; then
    log "Build OK"
  else
    log "Build FAILED (exit $exit_code)"
  fi
  return "$exit_code"
}

# Resolve optional Windows share when running from ~/peps (local disk).
HOST_REPO=""
if ! on_shared_host; then
  if HOST_REPO="$(find_host_repo)"; then
    host_real="$(readlink -f "$HOST_REPO" 2>/dev/null || printf '%s' "$HOST_REPO")"
    local_real="$(readlink -f "$REPO_ROOT" 2>/dev/null || printf '%s' "$REPO_ROOT")"
    if [[ "$host_real" == "$local_real" ]]; then
      HOST_REPO=""
    fi
  else
    HOST_REPO=""
  fi
fi

need_java
chmod +x "$REPO_ROOT/mvnw" 2>/dev/null || true

# Clear a stale lock left by an older watcher (file present but flock not held).
mkdir -p "$REPO_ROOT/target"
if [[ -f "$LOCK_FILE" ]]; then
  exec 9>"$LOCK_FILE"
  if flock -n 9; then
    flock -u 9
    exec 9>&-
    rm -f "$LOCK_FILE"
    log "Removed stale $LOCK_FILE"
  else
    exec 9>&-
    log "Note: $LOCK_FILE is held — a build is in progress"
  fi
fi

if [[ -n "$HOST_REPO" ]]; then
  log "Local build tree: $REPO_ROOT"
  log "Watching host RECOMPILENOW: $HOST_REPO/RECOMPILENOW (poll ${POLL_SEC}s)"
  log "On flag: rsync host → local, package, delete host flag. Ctrl+C to stop."
else
  log "Watching for $REPO_ROOT/RECOMPILENOW (poll ${POLL_SEC}s). Ctrl+C to stop."
fi

while [[ "$REQUEST_STOP" -eq 0 ]]; do
  flag=""
  if [[ -n "$HOST_REPO" && -f "$HOST_REPO/RECOMPILENOW" ]]; then
    flag="$HOST_REPO/RECOMPILENOW"
  elif [[ -f "$REPO_ROOT/RECOMPILENOW" ]]; then
    flag="$REPO_ROOT/RECOMPILENOW"
  else
    LAST_BUILT_STAMP=""
    interruptible_sleep "$POLL_SEC"
    continue
  fi

  stamp="$(semaphore_stamp "$flag" || true)"
  if [[ -z "${stamp:-}" || "$stamp" == "$LAST_BUILT_STAMP" ]]; then
    interruptible_sleep "$POLL_SEC"
    continue
  fi

  if [[ -n "$HOST_REPO" && "$flag" == "$HOST_REPO/RECOMPILENOW" ]]; then
    log "Host RECOMPILENOW seen — syncing…"
    if ! command -v rsync >/dev/null 2>&1; then
      log "rsync not found — cannot sync from $HOST_REPO"
      LAST_BUILT_STAMP="$stamp"
      interruptible_sleep "$POLL_SEC"
      continue
    fi
    sync_from_host "$HOST_REPO" "$REPO_ROOT"
  else
    log "Local RECOMPILENOW seen — building…"
  fi

  if [[ "$REQUEST_STOP" -eq 1 ]]; then
    break
  fi

  run_build_locked || true
  LAST_BUILT_STAMP="$stamp"

  # Clear flags so the next Cursor create triggers again.
  if [[ -n "$HOST_REPO" ]]; then
    rm -f "$HOST_REPO/RECOMPILENOW"
    log "Removed host RECOMPILENOW"
  fi
  rm -f "$REPO_ROOT/RECOMPILENOW"
  LAST_BUILT_STAMP=""

  if [[ "$REQUEST_STOP" -eq 1 ]]; then
    break
  fi
  interruptible_sleep "$POLL_SEC"
done

exit 0

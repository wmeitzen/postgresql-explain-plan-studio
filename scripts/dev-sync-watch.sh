#!/usr/bin/env bash
# Sync PEPS sources from the Windows/VMware shared folder into a local
# work tree (Linux or macOS guest), then rebuild the shaded jar when host
# files change — or when the host repo contains a RECOMPILENOW semaphore
# (created by Cursor after edits).
# On start, syncs and packages once before waiting for changes / RECOMPILENOW.
# This script deletes RECOMPILENOW on the host after the guest build so Windows
# and the guest are not racing over who clears the flag.
#
# Why: shared-folder watches are unreliable; build on a local disk copy (~/peps)
# while still picking up edits made on the Windows host.
#
# Terminal 1 (keep running):
#   ./scripts/dev-sync-watch.sh
#   # or, first time / until local copy has this script:
#   bash /mnt/hgfs/peps/scripts/dev-sync-watch.sh
#   bash "/Volumes/peps/scripts/dev-sync-watch.sh"
#
# Terminal 2:
#   cd ~/peps && ./scripts/dev-run.sh
#
# Optional env:
#   PEPS_HOST_REPO   Host repo path as seen in the guest (/mnt/hgfs/… or /Volumes/…)
#   PEPS_LOCAL_REPO  Local build tree (default: ~/peps)
#   PEPS_SYNC_POLL   Seconds between host fingerprint checks (default: 2)
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib-dev-lock.sh
source "$SCRIPT_DIR/lib-dev-lock.sh"
# shellcheck source=lib-host-share.sh
source "$SCRIPT_DIR/lib-host-share.sh"
POLL_SEC="${PEPS_SYNC_POLL:-2}"
LOCAL_REPO="${PEPS_LOCAL_REPO:-$HOME/peps}"
HOST_REPO="${PEPS_HOST_REPO:-${1:-}}"

log() {
  printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"
}

die() {
  echo "$*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

find_host_repo() {
  peps_find_host_repo "${HOST_REPO}"
}

file_mtime_size() {
  local path="$1"
  # Linux: stat -c; macOS: stat -f
  stat -c '%Y %s' "$path" 2>/dev/null || stat -f '%m %z' "$path"
}

fingerprint_sources() {
  local root="$1"
  local listing
  # Portable fingerprint (GNU find -printf is Linux-only).
  listing="$(
    {
      find "$root/src" -type f \( -name '*.java' -o -name '*.xml' \) 2>/dev/null
      [[ -f "$root/pom.xml" ]] && printf '%s\n' "$root/pom.xml"
      find "$root/scripts" -type f -name '*.sh' 2>/dev/null
    } | sort | while IFS= read -r f; do
      [[ -n "$f" ]] || continue
      printf '%s\t%s\n' "${f#"$root"/}" "$(file_mtime_size "$f")"
    done
  )"
  if command -v md5sum >/dev/null 2>&1; then
    printf '%s' "$listing" | md5sum | awk '{print $1}'
  else
    printf '%s' "$listing" | md5 -q
  fi
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
    "$host/" "$local/"
  chmod +x "$local/mvnw" "$local"/scripts/*.sh 2>/dev/null || true
}

run_build() {
  local local="$1"
  local lock_file="$local/target/dev-build.lock"
  mkdir -p "$local/target"
  # Exclusive lock so systemd sync-watch and a manual ~/peps/dev-watch.sh cannot
  # shade-replace the same jar at once (maven-shade "Could not replace original artifact").
  if ! peps_lock_acquire "$lock_file"; then
    log "Another Maven build holds $lock_file — skipping this cycle"
    return 0
  fi
  # Clear half-finished shade renames from a prior crash/race.
  rm -f "$local"/target/*-shaded.jar
  log "Building in $local …"
  (
    cd "$local"
    set +e
    ./mvnw -DskipTests package
    local exit_code=$?
    set -e
    if [[ "$exit_code" -eq 0 ]]; then
      log "Build OK"
    else
      log "Build FAILED (exit $exit_code)"
    fi
    return "$exit_code"
  )
  local rc=$?
  peps_lock_release "$lock_file"
  return "$rc"
}

STOPPING=0
cleanup() {
  if [[ "$STOPPING" -eq 1 ]]; then
    return 0
  fi
  STOPPING=1
  if [[ -n "${LOCAL_REPO:-}" && -f "$LOCAL_REPO/target/dev-build.lock" ]]; then
    # Only remove our lock if it looks like ours (best-effort).
    rm -f "$LOCAL_REPO/target/dev-build.lock"
  fi
  log "Sync watcher stopped."
}
# INT/TERM must exit — otherwise Ctrl+C only runs cleanup and the poll loop continues.
trap cleanup EXIT
trap 'exit 130' INT TERM

need_cmd java
need_cmd rsync
need_cmd find
need_cmd stat
if ! command -v md5sum >/dev/null 2>&1 && ! command -v md5 >/dev/null 2>&1; then
  die "Required command not found: md5sum (Linux) or md5 (macOS)"
fi

HOST_REPO="$(find_host_repo)" || die "Could not find the Windows PEPS repo on a VMware share.
Mount / enable Shared Folders, then either:
  unset PEPS_HOST_REPO
  export PEPS_HOST_REPO=/mnt/hgfs/peps                              # Linux
  export PEPS_HOST_REPO=\"/Volumes/VMware Shared Folders/peps\"    # macOS Fusion
  ./scripts/dev-sync-watch.sh
or:
  ./scripts/dev-sync-watch.sh \"\$PEPS_HOST_REPO\"

Linux tip:  vmware-hgfsclient  &&  ls /mnt/hgfs
macOS tip:  ls /Volumes  &&  ls \"/Volumes/VMware Shared Folders\""

if [[ ! -f "$HOST_REPO/pom.xml" ]]; then
  die "PEPS_HOST_REPO is not a PEPS checkout: $HOST_REPO"
fi

# Avoid syncing a tree onto itself.
host_real="$(readlink -f "$HOST_REPO" 2>/dev/null || printf '%s' "$HOST_REPO")"
local_real="$(mkdir -p "$LOCAL_REPO"; readlink -f "$LOCAL_REPO" 2>/dev/null || printf '%s' "$LOCAL_REPO")"
if [[ "$host_real" == "$local_real" ]]; then
  die "Host and local repos are the same path ($local_real).
Use a local disk copy for PEPS_LOCAL_REPO (default ~/peps), not the share path."
fi

log "Host (Windows share): $HOST_REPO"
log "Local build tree:     $LOCAL_REPO"
log "Also watching host RECOMPILENOW (cleared here after build)."

# Always sync + compile once before waiting on changes / the semaphore.
log "Initial sync from host, then build (before watching)…"
sync_from_host "$HOST_REPO" "$LOCAL_REPO"
run_build "$LOCAL_REPO" || true
last_fp="$(fingerprint_sources "$HOST_REPO")"
last_recompile_stamp=""
if [[ -f "$HOST_REPO/RECOMPILENOW" ]]; then
  last_recompile_stamp="$(file_mtime_size "$HOST_REPO/RECOMPILENOW" 2>/dev/null || true)"
  rm -f "$HOST_REPO/RECOMPILENOW" "$LOCAL_REPO/RECOMPILENOW"
  log "Cleared host RECOMPILENOW after initial build"
fi

log "Polling every ${POLL_SEC}s. Ctrl+C to stop."

while true; do
  recompile_now=0
  recompile_stamp=""
  if [[ -f "$HOST_REPO/RECOMPILENOW" ]]; then
    recompile_stamp="$(file_mtime_size "$HOST_REPO/RECOMPILENOW" 2>/dev/null || true)"
    if [[ -n "$recompile_stamp" && "$recompile_stamp" != "$last_recompile_stamp" ]]; then
      recompile_now=1
    fi
  else
    last_recompile_stamp=""
  fi

  fp="$(fingerprint_sources "$HOST_REPO")"
  if [[ "$fp" != "$last_fp" || "$recompile_now" -eq 1 ]]; then
    if [[ "$recompile_now" -eq 1 ]]; then
      log "Host RECOMPILENOW seen — syncing…"
    else
      log "Host sources changed — syncing…"
    fi
    sync_from_host "$HOST_REPO" "$LOCAL_REPO"
    last_fp="$fp"
    run_build "$LOCAL_REPO" || true
    if [[ "$recompile_now" -eq 1 ]]; then
      last_recompile_stamp="$recompile_stamp"
      rm -f "$HOST_REPO/RECOMPILENOW" "$LOCAL_REPO/RECOMPILENOW"
      log "Removed host RECOMPILENOW"
    fi
  else
    sleep "$POLL_SEC"
  fi
done

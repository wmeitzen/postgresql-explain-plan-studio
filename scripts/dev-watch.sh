#!/usr/bin/env bash
# PEPS dev watcher — one entry point for Ubuntu/macOS guests and local-only checkouts.
#
# Linux/macOS + Windows VMware share (usual day-to-day command):
#   bash ~/peps/scripts/dev-watch.sh
#
# That single command:
#   1) mounts HGFS on Linux if VMware shares are advertised but /mnt/hgfs is empty
#      (macOS Fusion shares appear under /Volumes/… — no mount step)
#   2) re-execs the share copy when ~/peps is stale
#   3) syncs share → ~/peps
#   4) initial Maven package (via dev-sync-watch.sh)
#   5) watches host sources + RECOMPILENOW
#
# First boot on Linux (no ~/peps yet): mount once, then run from the share:
#   sudo mkdir -p /mnt/hgfs
#   sudo vmhgfs-fuse .host:/ /mnt/hgfs -o subtype=vmhgfs-fuse,allow_other
#   bash /mnt/hgfs/peps/scripts/dev-watch.sh
#
# First boot on macOS (Fusion Shared Folders already enabled):
#   bash "/Volumes/peps/scripts/dev-watch.sh"
#   # or: bash "/Volumes/VMware Shared Folders/peps/scripts/dev-watch.sh"
#
# Local-only checkout (no host share):
#   ./scripts/dev-watch.sh
#
# Launch the app with scripts/dev-run.sh (from ~/peps).
set -uo pipefail

_SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib-dev-lock.sh
source "$_SCRIPT_DIR/lib-dev-lock.sh"
# shellcheck source=lib-host-share.sh
source "$_SCRIPT_DIR/lib-host-share.sh"
unset _SCRIPT_DIR

log() {
  printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"
}

die() {
  echo "$*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1
Linux:  sudo apt install -y openjdk-17-jdk rsync
macOS:  brew install openjdk@17 rsync"
}

# Empty /mnt/hgfs is common before fuse mount — do not treat that as "OK".
hgfs_shares_visible() {
  [[ -d /mnt/hgfs ]] || return 1
  [[ -n "$(ls -A /mnt/hgfs 2>/dev/null || true)" ]]
}

vmware_shares_advertised() {
  command -v vmware-hgfsclient >/dev/null 2>&1 || return 1
  [[ -n "$(vmware-hgfsclient 2>/dev/null || true)" ]]
}

ensure_hgfs() {
  if hgfs_shares_visible; then
    return 0
  fi

  if mountpoint -q /mnt/hgfs 2>/dev/null; then
    die "HGFS is mounted at /mnt/hgfs but no shares are visible.
Run: vmware-hgfsclient
In VMware: enable Shared Folders and share the PEPS repo (often named \"peps\")."
  fi

  if ! command -v vmhgfs-fuse >/dev/null 2>&1; then
    die "vmhgfs-fuse not found. Install open-vm-tools / open-vm-tools-desktop, then retry."
  fi

  log "HGFS share not visible at /mnt/hgfs — attempting mount…"
  log "(sudo may prompt for your password)"
  sudo mkdir -p /mnt/hgfs
  if sudo vmhgfs-fuse .host:/ /mnt/hgfs -o subtype=vmhgfs-fuse,allow_other; then
    if hgfs_shares_visible; then
      log "Mounted VMware shared folders at /mnt/hgfs"
      return 0
    fi
    die "Mounted /mnt/hgfs but it is still empty.
Run: vmware-hgfsclient
Enable Shared Folders in VMware (share name often \"peps\")."
  fi

  die "Could not mount /mnt/hgfs.
In VMware: enable Shared Folders for this VM, share the PEPS repo (often named \"peps\").
Then on Ubuntu:
  sudo mkdir -p /mnt/hgfs
  sudo vmhgfs-fuse .host:/ /mnt/hgfs -o subtype=vmhgfs-fuse,allow_other
  ls /mnt/hgfs
  vmware-hgfsclient"
}

find_host_repo_path() {
  peps_find_host_repo "${PEPS_HOST_REPO:-}"
}

# ---------------------------------------------------------------------------
# From ~/peps: on Linux, mount HGFS when VMware advertises shares but /mnt/hgfs
# is empty; then re-exec the share script if this local copy is stale.
# ---------------------------------------------------------------------------
if [[ "${PEPS_SKIP_HGFS_BOOTSTRAP:-}" != 1 ]]; then
  _here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  if ! peps_path_on_host_share "$_here"; then
    if ! peps_is_darwin && ! hgfs_shares_visible && vmware_shares_advertised; then
      ensure_hgfs
    fi
  fi
  unset _here
fi

if [[ "${PEPS_SKIP_HOST_REEXEC:-}" != 1 ]]; then
  _here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
  if ! peps_path_on_host_share "$_here"; then
    _share_watch=""
    if _host="$(find_host_repo_path 2>/dev/null)"; then
      if [[ -f "$_host/scripts/dev-watch.sh" ]]; then
        _share_watch="$_host/scripts/dev-watch.sh"
      fi
    fi
    if [[ -n "$_share_watch" ]] \
      && { [[ "$_share_watch" -nt "${BASH_SOURCE[0]}" ]] || ! cmp -s "$_share_watch" "${BASH_SOURCE[0]}" 2>/dev/null; }; then
      log "Local watcher is stale — re-execing $_share_watch"
      exec env PEPS_SKIP_HOST_REEXEC=1 PEPS_SKIP_HGFS_BOOTSTRAP=1 bash "$_share_watch" "$@"
    fi
    unset _host _share_watch
  fi
  unset _here
fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

LOCAL_REPO="${PEPS_LOCAL_REPO:-$HOME/peps}"
LOCK_FILE="$REPO_ROOT/target/dev-build.lock"
POLL_SEC="${PEPS_WATCH_POLL:-0.5}"
REQUEST_STOP=0
CLEANED_UP=0
LAST_BUILT_STAMP=""

on_shared_host() {
  peps_path_on_host_share "$REPO_ROOT"
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
  if ! peps_lock_acquire "$LOCK_FILE"; then
    log "Another Maven build holds $LOCK_FILE — will retry next poll"
    return 0
  fi
  rm -f "$REPO_ROOT"/target/*-shaded.jar
  log "Building in $REPO_ROOT …"
  set +e
  ./mvnw -DskipTests package
  local exit_code=$?
  set -e
  peps_lock_release "$LOCK_FILE"
  if [[ "$exit_code" -eq 0 ]]; then
    log "Build OK"
  else
    log "Build FAILED (exit $exit_code)"
  fi
  return "$exit_code"
}

# ---------------------------------------------------------------------------
# Host share (Linux HGFS or macOS /Volumes): sync → ~/peps → hand off to
# sync-watch (initial compile happens inside sync-watch).
# ---------------------------------------------------------------------------
HOST_REPO=""
if on_shared_host || [[ -n "${PEPS_HOST_REPO:-}" ]] || find_host_repo_path >/dev/null 2>&1; then
  need_cmd java
  need_cmd rsync
  if ! peps_is_darwin; then
    ensure_hgfs
  fi
  HOST_REPO="$(find_host_repo_path)" || die "Could not find the Windows PEPS repo on a VMware share.
Linux: share the repo (often named \"peps\"), then:
  ls /mnt/hgfs
  export PEPS_HOST_REPO=/mnt/hgfs/peps
  bash /mnt/hgfs/peps/scripts/dev-watch.sh
macOS (Fusion Shared Folders):
  ls /Volumes
  ls \"/Volumes/VMware Shared Folders\"
  export PEPS_HOST_REPO=/Volumes/peps
  bash \"\$PEPS_HOST_REPO/scripts/dev-watch.sh\""

  host_real="$(readlink -f "$HOST_REPO" 2>/dev/null || printf '%s' "$HOST_REPO")"
  mkdir -p "$LOCAL_REPO"
  local_real="$(readlink -f "$LOCAL_REPO" 2>/dev/null || printf '%s' "$LOCAL_REPO")"
  if [[ "$host_real" == "$local_real" ]]; then
    die "Host and local repos are the same path ($local_real).
Keep PEPS_LOCAL_REPO on local disk (default ~/peps), not under /mnt/hgfs or /Volumes."
  fi

  log "Host (Windows share): $HOST_REPO"
  log "Local build tree:     $LOCAL_REPO"
  log "Syncing host → local (before watch)…"
  sync_from_host "$HOST_REPO" "$LOCAL_REPO"

  WATCHER="$LOCAL_REPO/scripts/dev-sync-watch.sh"
  if [[ ! -f "$WATCHER" ]]; then
    die "Missing $WATCHER after sync — check the Windows share checkout."
  fi

  log "Handing off to sync watcher (initial compile + RECOMPILENOW / host-change watch)."
  log "In another terminal:  cd $LOCAL_REPO && ./scripts/dev-run.sh"
  export PEPS_HOST_REPO="$HOST_REPO"
  export PEPS_LOCAL_REPO="$LOCAL_REPO"
  export PEPS_SKIP_HOST_REEXEC=1
  # Avoid EXIT trap printing "Watcher stopped" before the child takes over.
  CLEANED_UP=1
  exec bash "$WATCHER"
fi

# ---------------------------------------------------------------------------
# Local-only mode (no VMware share): package once, then watch RECOMPILENOW.
# ---------------------------------------------------------------------------
need_cmd java
chmod +x "$REPO_ROOT/mvnw" 2>/dev/null || true

mkdir -p "$REPO_ROOT/target"
if peps_lock_try_clear_idle "$LOCK_FILE"; then
  log "Cleared idle build lock (if any)"
else
  log "Note: $LOCK_FILE is held — a build is in progress"
fi

log "Local-only mode (no VMware host share detected under /mnt/hgfs or /Volumes)."
log "Initial build (before watching RECOMPILENOW)…"
run_build_locked || true

log "Watching for $REPO_ROOT/RECOMPILENOW (poll ${POLL_SEC}s). Ctrl+C to stop."

while [[ "$REQUEST_STOP" -eq 0 ]]; do
  flag=""
  if [[ -f "$REPO_ROOT/RECOMPILENOW" ]]; then
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

  log "Local RECOMPILENOW seen — building…"
  if [[ "$REQUEST_STOP" -eq 1 ]]; then
    break
  fi

  run_build_locked || true
  LAST_BUILT_STAMP="$stamp"
  rm -f "$REPO_ROOT/RECOMPILENOW"
  LAST_BUILT_STAMP=""

  if [[ "$REQUEST_STOP" -eq 1 ]]; then
    break
  fi
  interruptible_sleep "$POLL_SEC"
done

exit 0

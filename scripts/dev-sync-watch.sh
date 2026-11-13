#!/usr/bin/env bash
# Sync PEPS sources from the Windows/VMware shared folder into a local Ubuntu
# work tree, then rebuild the shaded jar when host files change — or when the
# host repo contains a RECOMPILENOW semaphore (created by Cursor after edits).
# On start, syncs and packages once before waiting for changes / RECOMPILENOW.
# This script deletes RECOMPILENOW on the host after the Ubuntu build so Windows
# and Ubuntu are not racing over who clears the flag.
#
# Why: /mnt/hgfs watches are unreliable; build on a local disk copy (~/peps)
# while still picking up edits made on the Windows host.
#
# Terminal 1 (keep running):
#   ./scripts/dev-sync-watch.sh
#   # or, first time / until local copy has this script:
#   bash /mnt/hgfs/<share>/.../postgresql-explain-plan-studio/scripts/dev-sync-watch.sh
#
# Terminal 2:
#   cd ~/peps && ./scripts/dev-run.sh
#
# Optional env:
#   PEPS_HOST_REPO   Windows repo path as seen in the guest (under /mnt/hgfs/...)
#   PEPS_LOCAL_REPO  Local build tree (default: ~/peps)
#   PEPS_SYNC_POLL   Seconds between host fingerprint checks (default: 2)
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
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
  local candidate
  if [[ -n "${HOST_REPO}" ]]; then
    printf '%s\n' "$HOST_REPO"
    return 0
  fi
  # Prefer an exact project folder under any mounted share.
  while IFS= read -r candidate; do
    if [[ -f "$candidate/pom.xml" ]] \
      && [[ -d "$candidate/src/main/java/com/williammeitzen/peps" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done < <(find /mnt/hgfs -maxdepth 8 -type d -name 'postgresql-explain-plan-studio' 2>/dev/null)

  # Fallback: share named "peps" that is already the repo root.
  for candidate in /mnt/hgfs/peps /mnt/hgfs/PEPS; do
    if [[ -f "$candidate/pom.xml" ]] \
      && [[ -d "$candidate/src/main/java/com/williammeitzen/peps" ]]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

fingerprint_sources() {
  local root="$1"
  # Stable fingerprint of sources the Windows watch also cares about.
  {
    find "$root/src" -type f \( -name '*.java' -o -name '*.xml' \) -printf '%P\t%T@\t%s\n' 2>/dev/null
    if [[ -f "$root/pom.xml" ]]; then
      printf 'pom.xml\t%s\t%s\n' "$(stat -c '%Y' "$root/pom.xml")" "$(stat -c '%s' "$root/pom.xml")"
    fi
    find "$root/scripts" -type f -name '*.sh' -printf 'scripts/%P\t%T@\t%s\n' 2>/dev/null
  } | sort | md5sum | awk '{print $1}'
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
  exec 9>"$lock_file"
  if ! flock -n 9; then
    log "Another Maven build holds $lock_file — skipping this cycle"
    return 0
  fi
  printf 'pid=%s\nstarted=%s\n' "$$" "$(date +%Y-%m-%dT%H:%M:%S%z)" >"$lock_file"
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
  flock -u 9
  exec 9>&-
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
need_cmd md5sum
need_cmd find
need_cmd stat

HOST_REPO="$(find_host_repo)" || die "Could not find Windows repo under /mnt/hgfs.
Mount shared folders, then either:
  export PEPS_HOST_REPO=/mnt/hgfs/<share>/path/to/postgresql-explain-plan-studio
  ./scripts/dev-sync-watch.sh
or:
  ./scripts/dev-sync-watch.sh /mnt/hgfs/<share>/.../postgresql-explain-plan-studio

Tip: run  vmware-hgfsclient  and  ls /mnt/hgfs  to see share names."

if [[ ! -f "$HOST_REPO/pom.xml" ]]; then
  die "PEPS_HOST_REPO is not a PEPS checkout: $HOST_REPO"
fi

# Avoid syncing a tree onto itself.
host_real="$(readlink -f "$HOST_REPO" 2>/dev/null || printf '%s' "$HOST_REPO")"
local_real="$(mkdir -p "$LOCAL_REPO"; readlink -f "$LOCAL_REPO" 2>/dev/null || printf '%s' "$LOCAL_REPO")"
if [[ "$host_real" == "$local_real" ]]; then
  die "Host and local repos are the same path ($local_real).
Use a local disk copy for PEPS_LOCAL_REPO (default ~/peps), not the hgfs path."
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
  last_recompile_stamp="$(stat -c '%Y %s' "$HOST_REPO/RECOMPILENOW" 2>/dev/null || true)"
  rm -f "$HOST_REPO/RECOMPILENOW" "$LOCAL_REPO/RECOMPILENOW"
  log "Cleared host RECOMPILENOW after initial build"
fi

log "Polling every ${POLL_SEC}s. Ctrl+C to stop."

while true; do
  recompile_now=0
  recompile_stamp=""
  if [[ -f "$HOST_REPO/RECOMPILENOW" ]]; then
    recompile_stamp="$(stat -c '%Y %s' "$HOST_REPO/RECOMPILENOW" 2>/dev/null || true)"
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

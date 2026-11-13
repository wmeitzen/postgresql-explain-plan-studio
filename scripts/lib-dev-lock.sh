#!/usr/bin/env bash
# Portable build lock for PEPS watch/run scripts.
# Prefers flock (Linux); falls back to mkdir locks on macOS (no util-linux flock).
#
# Usage (from another script in scripts/):
#   # shellcheck source=lib-dev-lock.sh
#   source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib-dev-lock.sh"
#   peps_lock_acquire "$LOCK_FILE" || { echo busy; return 0; }
#   ... build ...
#   peps_lock_release "$LOCK_FILE"

peps_have_flock() {
  command -v flock >/dev/null 2>&1
}

peps_lock_dir() {
  printf '%s.d\n' "$1"
}

# Return 0 if acquired, 1 if another builder holds the lock.
peps_lock_acquire() {
  local lock_file="$1"
  local lock_dir
  lock_dir="$(peps_lock_dir "$lock_file")"
  mkdir -p "$(dirname "$lock_file")"

  if peps_have_flock; then
    exec 9>"$lock_file"
    if ! flock -n 9; then
      exec 9>&-
      return 1
    fi
    printf 'pid=%s\nstarted=%s\n' "$$" "$(date +%Y-%m-%dT%H:%M:%S%z)" >"$lock_file"
    return 0
  fi

  # mkdir is atomic on macOS / POSIX — use as the exclusive lock.
  if mkdir "$lock_dir" 2>/dev/null; then
    printf '%s\n' "$$" >"$lock_dir/pid"
    printf 'pid=%s\nstarted=%s\n' "$$" "$(date +%Y-%m-%dT%H:%M:%S%z)" >"$lock_file"
    return 0
  fi

  # Stale lock from a killed watcher?
  if [[ -f "$lock_dir/pid" ]]; then
    local old_pid
    old_pid="$(tr -d '[:space:]' <"$lock_dir/pid" 2>/dev/null || true)"
    if [[ -n "$old_pid" ]] && ! kill -0 "$old_pid" 2>/dev/null; then
      rm -rf "$lock_dir"
      rm -f "$lock_file"
      if mkdir "$lock_dir" 2>/dev/null; then
        printf '%s\n' "$$" >"$lock_dir/pid"
        printf 'pid=%s\nstarted=%s\n' "$$" "$(date +%Y-%m-%dT%H:%M:%S%z)" >"$lock_file"
        return 0
      fi
    fi
  fi
  return 1
}

peps_lock_release() {
  local lock_file="$1"
  local lock_dir
  lock_dir="$(peps_lock_dir "$lock_file")"

  if peps_have_flock; then
    flock -u 9 2>/dev/null || true
    exec 9>&- 2>/dev/null || true
    rm -f "$lock_file"
    return 0
  fi

  rm -rf "$lock_dir"
  rm -f "$lock_file"
}

# Return 0 when no builder holds the lock (safe to launch / clear stale file).
peps_lock_try_clear_idle() {
  local lock_file="$1"
  local lock_dir
  lock_dir="$(peps_lock_dir "$lock_file")"
  mkdir -p "$(dirname "$lock_file")"

  if peps_have_flock; then
    exec 9>"$lock_file"
    if flock -n 9; then
      flock -u 9
      exec 9>&-
      rm -f "$lock_file"
      return 0
    fi
    exec 9>&-
    return 1
  fi

  if [[ ! -d "$lock_dir" ]]; then
    rm -f "$lock_file"
    return 0
  fi
  if [[ -f "$lock_dir/pid" ]]; then
    local old_pid
    old_pid="$(tr -d '[:space:]' <"$lock_dir/pid" 2>/dev/null || true)"
    if [[ -n "$old_pid" ]] && ! kill -0 "$old_pid" 2>/dev/null; then
      rm -rf "$lock_dir"
      rm -f "$lock_file"
      return 0
    fi
  fi
  return 1
}

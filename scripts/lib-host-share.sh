#!/usr/bin/env bash
# Shared helpers for locating the Windows/host PEPS checkout as seen from a
# Linux (/mnt/hgfs) or macOS (/Volumes/…) VMware guest.
#
# shellcheck shell=bash

peps_is_darwin() {
  [[ "$(uname -s 2>/dev/null || true)" == "Darwin" ]]
}

peps_is_peps_repo() {
  local dir="$1"
  [[ -f "$dir/pom.xml" ]] \
    && [[ -d "$dir/src/main/java/com/williammeitzen/peps" ]]
}

# Print candidate roots that may contain the host share (one path per line).
peps_share_search_roots() {
  local root
  if [[ -d /mnt/hgfs ]]; then
    printf '%s\n' /mnt/hgfs
  fi
  if [[ -d "/Volumes/VMware Shared Folders" ]]; then
    printf '%s\n' "/Volumes/VMware Shared Folders"
  fi
  if [[ -d /Volumes ]]; then
    for root in /Volumes/*; do
      [[ -d "$root" ]] || continue
      case "$root" in
        /Volumes/Macintosh\ HD|/Volumes/Macintosh\ HD\ *) continue ;;
      esac
      printf '%s\n' "$root"
    done
  fi
}

# Well-known share names that are already the repo root.
peps_known_share_repo_candidates() {
  local candidate
  for candidate in \
    /mnt/hgfs/peps \
    /mnt/hgfs/PEPS \
    /Volumes/peps \
    /Volumes/PEPS \
    "/Volumes/VMware Shared Folders/peps" \
    "/Volumes/VMware Shared Folders/PEPS"
  do
    printf '%s\n' "$candidate"
  done
}

# Resolve PEPS_HOST_REPO / $1 / auto-detect under HGFS or macOS Fusion shares.
# Prints the absolute-ish path on success (exit 0), else exit 1.
peps_find_host_repo() {
  local explicit="${1:-${PEPS_HOST_REPO:-}}"
  local candidate root

  if [[ -n "$explicit" ]]; then
    if peps_is_peps_repo "$explicit"; then
      printf '%s\n' "$explicit"
      return 0
    fi
    return 1
  fi

  while IFS= read -r root; do
    [[ -d "$root" ]] || continue
    while IFS= read -r candidate; do
      if peps_is_peps_repo "$candidate"; then
        printf '%s\n' "$candidate"
        return 0
      fi
    done < <(find "$root" -maxdepth 8 -type d -name 'postgresql-explain-plan-studio' 2>/dev/null)
  done < <(peps_share_search_roots)

  while IFS= read -r candidate; do
    if peps_is_peps_repo "$candidate"; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done < <(peps_known_share_repo_candidates)

  return 1
}

# True when path is on a VMware shared folder mount.
peps_path_on_host_share() {
  case "$1" in
    /mnt/hgfs/*) return 0 ;;
    /Volumes/VMware\ Shared\ Folders/*) return 0 ;;
    /Volumes/peps|/Volumes/peps/*|/Volumes/PEPS|/Volumes/PEPS/*) return 0 ;;
    *) return 1 ;;
  esac
}

#!/usr/bin/env bash
# Install PEPS sync+build so it starts when you log into the Ubuntu VM.
#
# Default: systemd --user service (recommended for a long-running watcher).
# Optional: cron @reboot  →  ./scripts/install-dev-sync-autostart.sh --cron
set -euo pipefail

MODE="systemd"
HOST_REPO="${PEPS_HOST_REPO:-/mnt/hgfs/peps}"
LOCAL_REPO="${PEPS_LOCAL_REPO:-$HOME/peps}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--systemd|--cron] [--host PATH] [--local PATH]

  --systemd   systemd --user service (default; restarts on failure)
  --cron      crontab @reboot entry writing to ~/peps-sync-watch.log
  --host PATH Windows share repo (default: /mnt/hgfs/peps)
  --local PATH local build tree (default: ~/peps)
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --systemd) MODE="systemd"; shift ;;
    --cron) MODE="cron"; shift ;;
    --host) HOST_REPO="$2"; shift 2 ;;
    --local) LOCAL_REPO="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

if [[ ! -f "$HOST_REPO/scripts/dev-watch.sh" ]]; then
  echo "Host script not found: $HOST_REPO/scripts/dev-watch.sh" >&2
  echo "Set --host or PEPS_HOST_REPO to your VMware share path." >&2
  exit 1
fi

install_systemd() {
  local unit_dir="$HOME/.config/systemd/user"
  local unit_path="$unit_dir/peps-dev-sync-watch.service"
  mkdir -p "$unit_dir"
  sed \
    -e "s|/mnt/hgfs/peps|${HOST_REPO}|g" \
    -e "s|Environment=PEPS_LOCAL_REPO=%h/peps|Environment=PEPS_LOCAL_REPO=${LOCAL_REPO}|g" \
    "$HOST_REPO/scripts/peps-dev-sync-watch.service" >"$unit_path"

  systemctl --user daemon-reload
  systemctl --user enable --now peps-dev-sync-watch.service

  echo "Installed systemd user service: peps-dev-sync-watch.service"
  echo "  status:  systemctl --user status peps-dev-sync-watch.service"
  echo "  logs:    journalctl --user -u peps-dev-sync-watch.service -f"
  echo "  stop:    systemctl --user stop peps-dev-sync-watch.service"
  echo "  disable: systemctl --user disable --now peps-dev-sync-watch.service"
  echo
  echo "If the service should run even before you open a desktop session:"
  echo "  loginctl enable-linger \"\$USER\""
}

install_cron() {
  local marker="# peps-dev-sync-watch"
  local log_file="$HOME/peps-sync-watch.log"
  local line="@reboot sleep 20 && PEPS_HOST_REPO=${HOST_REPO} PEPS_LOCAL_REPO=${LOCAL_REPO} /bin/bash ${HOST_REPO}/scripts/dev-watch.sh >>${log_file} 2>&1 ${marker}"
  local tmp
  tmp="$(mktemp)"
  crontab -l 2>/dev/null | grep -v "$marker" >"$tmp" || true
  printf '%s\n' "$line" >>"$tmp"
  crontab "$tmp"
  rm -f "$tmp"
  echo "Installed crontab @reboot entry."
  echo "  logs:  tail -f $log_file"
  echo "  list:  crontab -l"
  echo "  remove: crontab -l | grep -v '$marker' | crontab -"
}

case "$MODE" in
  systemd) install_systemd ;;
  cron) install_cron ;;
esac

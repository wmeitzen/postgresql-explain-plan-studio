#!/usr/bin/env bash
# Compatibility alias — use scripts/dev-watch.sh (same bootstrap + watch).
#
#   bash /mnt/hgfs/peps/scripts/dev-watch.sh
#   bash /mnt/hgfs/peps/scripts/ubuntu-dev-up.sh   # same thing
set -uo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bash "$SCRIPT_DIR/dev-watch.sh" "$@"

#!/usr/bin/env bash
# Rewrite a jpackage .deb Depends line so one package installs on Ubuntu 20.04+.
#
# jpackage (built on 20.04) emits exact package names from that host. Newer
# Ubuntu drops some of those (libpcre3) and renames others (*t64). Apt accepts
# Debian "pkgA | pkgB" alternatives, so we:
#   - drop libpcre3 (PCRE1 removed in Ubuntu 25.10/26.04; not required by our
#     bundled runtime — harfbuzz/glib pull what they need)
#   - rewrite known t64 transitions to "newt64 | old" so either side satisfies
#
# Usage: linux-deb-widen-depends.sh path/to/package.deb
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 package.deb" >&2
  exit 2
fi

DEB="$(readlink -f "$1")"
if [[ ! -f "$DEB" ]]; then
  echo "Not a file: $DEB" >&2
  exit 1
fi
if ! command -v dpkg-deb >/dev/null 2>&1; then
  echo "dpkg-deb is required" >&2
  exit 1
fi

WORKDIR="$(mktemp -d)"
cleanup() { rm -rf "$WORKDIR"; }
trap cleanup EXIT

dpkg-deb -R "$DEB" "$WORKDIR/root"
CONTROL="$WORKDIR/root/DEBIAN/control"
if [[ ! -f "$CONTROL" ]]; then
  echo "Missing DEBIAN/control in $DEB" >&2
  exit 1
fi

python3 - "$CONTROL" <<'PY'
import re, sys
from pathlib import Path

control_path = Path(sys.argv[1])
text = control_path.read_text(encoding="utf-8")
m = re.search(r"^Depends:\s*(.*)$", text, re.M)
if not m:
    raise SystemExit("No Depends: line in control")

raw = m.group(1).strip()
# Split on commas that are not inside parentheses (we have no versioned deps today).
parts = [p.strip() for p in raw.split(",") if p.strip()]

# Drop packages that no longer exist on newer Ubuntu and are not needed as
# direct Depends of a jpackage app-image.
DROP = {"libpcre3"}

# old_name -> preferred alternative (t64 first so newer apt picks the real pkg).
T64 = {
    "libasound2": "libasound2t64 | libasound2",
    "libglib2.0-0": "libglib2.0-0t64 | libglib2.0-0",
    "libpng16-16": "libpng16-16t64 | libpng16-16",
    "libgtk-3-0": "libgtk-3-0t64 | libgtk-3-0",
    "libgdk-pixbuf-2.0-0": "libgdk-pixbuf-2.0-0t64 | libgdk-pixbuf-2.0-0",
    "libcairo2": "libcairo2t64 | libcairo2",
    "libpango-1.0-0": "libpango-1.0-0t64 | libpango-1.0-0",
    "libatk1.0-0": "libatk1.0-0t64 | libatk1.0-0",
}

out = []
seen = set()
for p in parts:
    # Already an alternative, or versioned — keep as-is if not dropped.
    base = p.split("|", 1)[0].strip().split(" ", 1)[0]
    if base in DROP or p in DROP:
        continue
    rewritten = T64.get(base, p)
    key = rewritten
    if key in seen:
        continue
    seen.add(key)
    out.append(rewritten)

new_depends = "Depends: " + ", ".join(out)
new_text, n = re.subn(r"^Depends:\s*.*$", new_depends, text, count=1, flags=re.M)
if n != 1:
    raise SystemExit("Failed to rewrite Depends:")
control_path.write_text(new_text, encoding="utf-8")
print(new_depends)
PY

# Rebuild with root ownership typical for debs.
OUT="$WORKDIR/out.deb"
dpkg-deb -b "$WORKDIR/root" "$OUT" >/dev/null
# Preserve mode; replace atomically.
cp -f "$OUT" "$DEB"
echo "Rewrote Depends in $DEB"

#!/usr/bin/env bash
# Launch PEPS from the shaded jar sitting next to this script.
# Works on Intel and Apple Silicon Macs — needs JDK/JRE 17+ on PATH (no native .app).
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE"

if ! command -v java >/dev/null 2>&1; then
  echo "java not found on PATH. Install Temurin (or another) JDK 17+ and retry." >&2
  exit 1
fi

jar="$(ls -1t "$HERE"/postgresql-explain-plan-studio-*.jar 2>/dev/null | head -n 1 || true)"
if [[ -z "${jar}" || ! -f "${jar}" ]]; then
  echo "No postgresql-explain-plan-studio-*.jar next to peps.sh." >&2
  exit 1
fi

# -Xdock:name sets the Dock label; apple.awt.application.name is also set in main().
exec java \
  -Xdock:name="PostgreSQL Explain Plan Studio" \
  -jar "$jar" "$@"

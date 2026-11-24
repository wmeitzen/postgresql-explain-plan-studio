#!/usr/bin/env bash
# Optional Unix/macOS helper for the portable JAR zip (same folder as the shaded jar).
# Works on Intel and Apple Silicon Macs and other Unix hosts — needs JDK/JRE 17+ on PATH.
# On any OS you can also run: java -jar postgresql-explain-plan-studio-*.jar
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

# -Xdock:name sets the Dock label on macOS; ignored elsewhere.
# apple.awt.application.name is also set in main().
exec java \
  -Xdock:name="PostgreSQL Explain Plan Studio" \
  -jar "$jar" "$@"

#!/usr/bin/env bash
# Launch PEPS from the shaded jar, waiting if a dev-watch build is in progress.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

LOCK_FILE="$REPO_ROOT/target/dev-build.lock"
WAIT_TIMEOUT_SEC=300
POLL_SEC=0.5

log() {
  printf '[%s] %s\n' "$(date +%H:%M:%S)" "$*"
}

if ! command -v java >/dev/null 2>&1; then
  echo "java not found on PATH. Install JDK 17+ and retry." >&2
  exit 1
fi

announced=0
deadline=$((SECONDS + WAIT_TIMEOUT_SEC))
while [[ -f "$LOCK_FILE" ]]; do
  if (( SECONDS > deadline )); then
    echo "Timed out after ${WAIT_TIMEOUT_SEC}s waiting for recompile to finish ($LOCK_FILE still present)." >&2
    exit 1
  fi
  if [[ "$announced" -eq 0 ]]; then
    log "PEPS is recompiling — waiting…"
    announced=1
  fi
  sleep "$POLL_SEC"
done

jar="$(ls -1t "$REPO_ROOT"/target/postgresql-explain-plan-studio-*.jar 2>/dev/null | grep -v -- '-shaded\.jar$' | grep -v -- '-sources\.jar$' | grep -v -- '-javadoc\.jar$' | head -n 1 || true)"
if [[ -z "$jar" || ! -f "$jar" ]]; then
  echo "No runnable jar in target/. Start scripts/dev-watch.sh first (or run: ./mvnw -DskipTests package)." >&2
  exit 1
fi

log "Starting $(basename "$jar")"
exec java -jar "$jar"

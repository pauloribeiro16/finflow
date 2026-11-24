#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "=== 1/2: Running TLC model checks on formal/tla/*.tla ==="
if ! command -v tlc >/dev/null 2>&1; then
  echo "ERROR: 'tlc' not found on PATH. Install TLA+ tools or run via /statechart." >&2
  exit 2
fi

MODELS=(
  "DbEncryptionLifecycle"
  "SessionLockAuth"
  "UpdateInstallProtocol"
  "NotificationCaptureSecurity"
  "BackupAtomicRestore"
  "PortfolioMarketSync"
)

TLC_STATUS=0
for m in "${MODELS[@]}"; do
  echo ""
  echo "--- Model: $m ---"
  if tlc -workers 1 -cleanup -config "formal/tla/$m.cfg" "formal/tla/$m.tla"; then
    echo "[PASS] $m: all invariants hold"
  else
    echo "[FINDING] $m: invariant violation detected (inspect trace above or run /statechart)"
    TLC_STATUS=1
  fi
done

echo ""
echo "=== 2/2: Running Kotlin Formal Security & Domain Invariant Suites ==="
./gradlew :app:testProdDebugUnitTest \
  --tests "com.financas.app.security.SecurityFormalPropertiesTest" \
  --tests "com.financas.app.security.DomainAndSecurityInvariantsTest"

exit "$TLC_STATUS"

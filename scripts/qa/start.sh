#!/usr/bin/env bash
# Start the QA stack via PM2.
#   ./scripts/qa/start.sh                    # all services
#   ./scripts/qa/start.sh customer-service   # one service
set -euo pipefail

cd "$(dirname "$0")/../.."

# Pre-flight: ensure Docker infra is up (Postgres / Redis / Kafka / Temporal / Keycloak)
for c in ksa-postgres ksa-redis ksa-kafka ksa-temporal ksa-keycloak; do
  if ! docker ps --format '{{.Names}}' | grep -q "^$c$"; then
    echo "ERROR: Docker container '$c' is not running. Run ./scripts/start-infra.sh first." >&2
    exit 1
  fi
done

# Ensure QA databases exist
./scripts/qa/setup-databases.sh

# Ensure QA Temporal namespace exists
./scripts/qa/setup-temporal.sh || echo "WARN: Temporal namespace setup skipped/failed"

if [ $# -eq 0 ]; then
  pm2 start ecosystem.config.js
else
  pm2 start ecosystem.config.js --only "${1}-qa"
fi

pm2 save
pm2 ls

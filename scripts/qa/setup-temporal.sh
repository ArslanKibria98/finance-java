#!/usr/bin/env bash
# Register the `qa` Temporal namespace so PM2 services don't collide with Docker.
set -euo pipefail

TEMPORAL_CONTAINER="${TEMPORAL_CONTAINER:-ksa-temporal}"
NAMESPACE="${TEMPORAL_NAMESPACE:-qa}"

if docker exec "$TEMPORAL_CONTAINER" tctl --namespace "$NAMESPACE" namespace describe >/dev/null 2>&1; then
  echo "[skip] namespace '$NAMESPACE' already exists"
else
  docker exec "$TEMPORAL_CONTAINER" tctl --namespace "$NAMESPACE" namespace register \
    --retention 7 \
    --description "QA environment namespace (PM2-managed services)"
  echo "[create] Temporal namespace '$NAMESPACE'"
fi

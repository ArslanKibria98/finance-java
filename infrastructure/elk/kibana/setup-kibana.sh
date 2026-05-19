#!/bin/sh
###########################################################################
# Bootstraps Kibana with data views + sample dashboard.
# Idempotent — uses _import API with createNewCopies=false + overwrite=true.
###########################################################################

set -eu

KIBANA="${KIBANA_URL:-http://kibana:5601}"

echo "[kibana-setup] Waiting for Kibana at $KIBANA ..."
until curl -sf "$KIBANA/api/status" >/dev/null; do
  sleep 5
  echo "[kibana-setup] still waiting..."
done
echo "[kibana-setup] Kibana reachable."

echo "[kibana-setup] Importing data views + dashboards"
curl -sf -X POST "$KIBANA/api/saved_objects/_import?overwrite=true" \
  -H "kbn-xsrf: true" \
  -F file=@/setup/saved-objects.ndjson | head -c 800; echo

echo "[kibana-setup] Done. Open $KIBANA → Discover."

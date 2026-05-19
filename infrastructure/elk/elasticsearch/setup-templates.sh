#!/bin/sh
###########################################################################
# Bootstraps Elasticsearch with:
#   - ILM policy   ksa-logs-30d  (hot → warm → cold → delete @ 30d)
#   - Index template for app-logs-*
#   - Index template for api-audit-*
#
# Idempotent — safe to run multiple times.
# Triggered by docker-compose "es-setup" one-shot container.
###########################################################################

set -eu

ES="${ELASTICSEARCH_URL:-http://elasticsearch:9200}"

echo "[es-setup] Waiting for Elasticsearch at $ES ..."
until curl -sf "$ES/_cluster/health?wait_for_status=yellow&timeout=60s" >/dev/null; do
  sleep 5
  echo "[es-setup] still waiting..."
done
echo "[es-setup] Elasticsearch reachable."

echo "[es-setup] Installing ILM policy: ksa-logs-30d"
curl -sf -X PUT "$ES/_ilm/policy/ksa-logs-30d" \
  -H 'Content-Type: application/json' \
  --data-binary @/setup/ilm-policy.json | head -c 400; echo

echo "[es-setup] Installing index template: app-logs"
curl -sf -X PUT "$ES/_index_template/ksa-app-logs" \
  -H 'Content-Type: application/json' \
  --data-binary @/setup/index-template-app-logs.json | head -c 400; echo

echo "[es-setup] Installing index template: api-audit"
curl -sf -X PUT "$ES/_index_template/ksa-api-audit" \
  -H 'Content-Type: application/json' \
  --data-binary @/setup/index-template-api-audit.json | head -c 400; echo

echo "[es-setup] Done."

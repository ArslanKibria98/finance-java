#!/usr/bin/env bash
# Create QA databases inside the Docker postgres container.
# Idempotent — uses IF NOT EXISTS via PL/pgSQL.
set -euo pipefail

PG_CONTAINER="${PG_CONTAINER:-ksa-postgres}"
PG_USER="${PG_USER:-postgres}"

DBS=(
  identity_db_qa
  customer_db_qa
  global_profile_db_qa
  kyc_adapter_db_qa
  wallet_db_qa
  risk_service_qa
  product_service_db_qa
  fraud_service_db_qa
  ledger_db_qa
  notification_db_qa
  lending_db_qa
  collections_db_qa
)

for db in "${DBS[@]}"; do
  exists=$(docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -tAc \
    "SELECT 1 FROM pg_database WHERE datname='$db'")
  if [ "$exists" = "1" ]; then
    echo "[skip] $db already exists"
  else
    docker exec -i "$PG_CONTAINER" psql -U "$PG_USER" -c "CREATE DATABASE $db;"
    echo "[create] $db"
  fi
done

echo "Done. ${#DBS[@]} QA databases ready."

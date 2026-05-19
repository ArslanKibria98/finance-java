#!/usr/bin/env bash
# Show QA stack status + quick health probe on every service.
set -euo pipefail

pm2 ls

echo ""
echo "==> Health checks (actuator/health)"
declare -A PORTS=(
  [identity-service]=9183
  [customer-service]=9184
  [global-profile-service]=9185
  [kyc-adapter-service]=9187
  [wallet-service]=9188
  [risk-service]=9190
  [product-service]=9191
  [fraud-service]=9192
  [ledger-service]=9195
  [notification-service]=9196
  [lending-service]=9197
  [collections-service]=9199
)

for svc in "${!PORTS[@]}"; do
  port="${PORTS[$svc]}"
  status=$(curl -s -o /dev/null -w '%{http_code}' --max-time 2 "http://localhost:${port}/actuator/health" || echo "down")
  printf "  %-28s :%s  %s\n" "$svc" "$port" "$status"
done

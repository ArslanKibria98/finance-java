#!/bin/bash

# GL Reports Testing Script
# Usage: ./test-gl-reports.sh <jwt_token> [date]

if [ -z "$1" ]; then
  echo "Usage: $0 <jwt_token> [date]"
  echo ""
  echo "Get JWT token from Keycloak:"
  echo "  curl -X POST 'http://localhost:8080/realms/CompanyRealm/protocol/openid-connect/token' \\"
  echo "    -H 'Content-Type: application/x-www-form-urlencoded' \\"
  echo "    -d 'client_id=admin-dashboard' \\"
  echo "    -d 'client_secret=super-secret-key-change-in-prod' \\"
  echo "    -d 'grant_type=client_credentials' | jq -r '.access_token'"
  echo ""
  echo "Then run:"
  echo "  $0 '<token>' '2026-03-30'"
  exit 1
fi

TOKEN="$1"
DATE="${2:-2026-03-30}"
BASE_URL="http://localhost:8095/api/v1/reports"

echo "Testing GL Reports Endpoints"
echo "============================"
echo "Date: $DATE"
echo "Token: ${TOKEN:0:50}..."
echo ""

# Function to test endpoint
test_endpoint() {
  local name="$1"
  local endpoint="$2"

  echo "Testing: $name"
  echo "URL: $BASE_URL$endpoint"

  response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json")

  http_code=$(echo "$response" | tail -n 1)
  body=$(echo "$response" | sed '$d')

  if [ "$http_code" = "200" ]; then
    echo "✓ Status: $http_code (OK)"
    echo "$body" | jq . 2>/dev/null || echo "$body" | head -50
  else
    echo "✗ Status: $http_code"
    echo "$body"
  fi
  echo ""
}

# Test all endpoints
test_endpoint "Trial Balance" "/trial-balance?date=$DATE"
test_endpoint "Portfolio Summary" "/portfolio-summary?from=2026-01-01&to=$DATE"
test_endpoint "DPD Buckets" "/dpd-buckets?date=$DATE"
test_endpoint "Collections" "/collections?from=2026-03-01&to=$DATE"
test_endpoint "Profit Revenue" "/profit-revenue?period=2026-03"
test_endpoint "Write-off Provisions" "/write-off-provisions?period=2026-03"
test_endpoint "Cash Flow" "/cash-flow?date=$DATE"
test_endpoint "Reconciliation" "/reconciliation-detail?date=$DATE"

echo "Done!"

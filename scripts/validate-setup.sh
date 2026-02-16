#!/bin/bash

# Validate Infrastructure Setup
# This script validates that all configuration files are present and correct

set -e

echo "🔍 Validating KSA Islamic Financing Platform Infrastructure Setup..."
echo ""

# Navigate to the project root
cd "$(dirname "$0")/.."

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Validation counters
PASSED=0
FAILED=0

# Function to check file exists
check_file() {
  if [ -f "$1" ]; then
    echo -e "${GREEN}✓${NC} $1 exists"
    ((PASSED++))
  else
    echo -e "${RED}✗${NC} $1 missing"
    ((FAILED++))
  fi
}

# Function to check directory exists
check_dir() {
  if [ -d "$1" ]; then
    echo -e "${GREEN}✓${NC} $1 exists"
    ((PASSED++))
  else
    echo -e "${RED}✗${NC} $1 missing"
    ((FAILED++))
  fi
}

echo "📁 Checking configuration files..."
check_file "docker-compose.yml"
check_file "infrastructure/logstash/pipeline/logstash.conf"
check_file "infrastructure/prometheus/prometheus.yml"
check_file "infrastructure/README.md"

echo ""
echo "📜 Checking helper scripts..."
check_file "scripts/start-infra.sh"
check_file "scripts/stop-infra.sh"
check_file "scripts/logs.sh"

echo ""
echo "📂 Checking directories..."
check_dir "infrastructure"
check_dir "infrastructure/logstash"
check_dir "infrastructure/logstash/pipeline"
check_dir "infrastructure/prometheus"
check_dir "scripts"

echo ""
echo "🔒 Checking script permissions..."
if [ -x "scripts/start-infra.sh" ]; then
  echo -e "${GREEN}✓${NC} start-infra.sh is executable"
  ((PASSED++))
else
  echo -e "${RED}✗${NC} start-infra.sh is not executable"
  ((FAILED++))
fi

if [ -x "scripts/stop-infra.sh" ]; then
  echo -e "${GREEN}✓${NC} stop-infra.sh is executable"
  ((PASSED++))
else
  echo -e "${RED}✗${NC} stop-infra.sh is not executable"
  ((FAILED++))
fi

if [ -x "scripts/logs.sh" ]; then
  echo -e "${GREEN}✓${NC} logs.sh is executable"
  ((PASSED++))
else
  echo -e "${RED}✗${NC} logs.sh is not executable"
  ((FAILED++))
fi

echo ""
echo "📋 Validating docker-compose.yml structure..."

# Check for required services in docker-compose.yml
SERVICES=("postgres" "redis" "zookeeper" "kafka" "schema-registry" "temporal" "keycloak" "elasticsearch" "logstash" "kibana" "prometheus" "grafana" "jaeger" "fineract")

for service in "${SERVICES[@]}"; do
  if grep -q "^  $service:" docker-compose.yml; then
    echo -e "${GREEN}✓${NC} Service '$service' defined"
    ((PASSED++))
  else
    echo -e "${RED}✗${NC} Service '$service' missing"
    ((FAILED++))
  fi
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Validation Results:"
echo -e "   ${GREEN}Passed: $PASSED${NC}"
echo -e "   ${RED}Failed: $FAILED${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $FAILED -eq 0 ]; then
  echo -e "\n${GREEN}✅ All validation checks passed!${NC}"
  echo ""
  echo "Next steps:"
  echo "  1. Ensure Docker and Docker Compose are installed"
  echo "  2. Run './scripts/start-infra.sh' to start all services"
  echo "  3. Wait for services to become healthy (may take 2-3 minutes)"
  echo "  4. Access the UIs listed in infrastructure/README.md"
  exit 0
else
  echo -e "\n${RED}❌ Some validation checks failed!${NC}"
  echo "Please fix the issues above before proceeding."
  exit 1
fi

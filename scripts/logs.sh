#!/bin/bash

# View Logs for Infrastructure Services
# Usage: ./scripts/logs.sh <service-name>
# Example: ./scripts/logs.sh postgres

set -e

# Navigate to the project root
cd "$(dirname "$0")/.."

if [ -z "$1" ]; then
  echo "📋 Available services:"
  echo "  - postgres"
  echo "  - redis"
  echo "  - zookeeper"
  echo "  - kafka"
  echo "  - schema-registry"
  echo "  - temporal"
  echo "  - keycloak"
  echo "  - elasticsearch"
  echo "  - logstash"
  echo "  - kibana"
  echo "  - prometheus"
  echo "  - grafana"
  echo "  - jaeger"
  echo "  - fineract"
  echo ""
  echo "Usage: ./scripts/logs.sh <service-name>"
  echo "Example: ./scripts/logs.sh postgres"
  echo ""
  echo "To view all logs: docker-compose logs -f"
  exit 1
fi

echo "📜 Viewing logs for: $1"
echo "Press Ctrl+C to exit"
echo ""

# Follow logs for the specified service
docker-compose logs -f "$1"

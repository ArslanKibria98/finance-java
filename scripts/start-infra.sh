#!/bin/bash

# Start Infrastructure Services for KSA Islamic Financing Platform
# This script starts all infrastructure services required for local development

set -e

echo "🚀 Starting KSA Islamic Financing Platform Infrastructure..."
echo ""

# Navigate to the project root
cd "$(dirname "$0")/.."

# Start all infrastructure services
docker compose up -d \
  postgres \
  redis \
  zookeeper \
  kafka \
  schema-registry \
  temporal \
  keycloak \
  elasticsearch \
  logstash \
  kibana \
  prometheus \
  grafana \
  jaeger \
  fineract \
  portainer

echo ""
echo "⏳ Waiting for services to become healthy..."
echo ""

# Wait for services to be healthy
sleep 10

# Check service status
docker compose ps

echo ""
echo "✅ Infrastructure services started successfully!"
echo ""
echo "📊 Access the following UIs:"
echo "  - Keycloak:       http://localhost:8080 (admin/admin)"
echo "  - Kibana:         http://localhost:5601"
echo "  - Grafana:        http://localhost:3000 (admin/admin)"
echo "  - Temporal UI:    http://localhost:8233"
echo "  - Jaeger UI:      http://localhost:16686"
echo "  - Prometheus:     http://localhost:9090"
echo "  - Fineract:       https://localhost:8443"
echo ""
echo "💾 Database:"
echo "  - PostgreSQL:     localhost:5432 (postgres/postgres)"
echo "  - Redis:          localhost:6379"
echo ""
echo "📨 Messaging:"
echo "  - Kafka:          localhost:9092"
echo "  - Schema Registry: http://localhost:8081"
echo ""
echo "🔍 Monitoring:"
echo "  - Elasticsearch:  http://localhost:9200"
echo "  - Portainer:      http://localhost:9000 (admin/password)"
echo "  - Logstash:       tcp://localhost:5000"
echo ""
echo "Use './scripts/logs.sh <service-name>' to view logs"
echo "Use './scripts/stop-infra.sh' to stop all services"

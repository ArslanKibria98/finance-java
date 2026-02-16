# Local Development Infrastructure

Complete guide for setting up the KSA Islamic Financing Platform infrastructure on your local machine using Docker Compose.

> **[← Back to Documentation Index](../../README.md)**

### Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- At least 8GB RAM available for Docker
- Ports 3000, 5000, 5432, 5601, 6379, 7233, 8080, 8081, 8233, 8443, 9090, 9092, 9200, 14268, 16686 must be available

### Quick Start

```bash
# Start all infrastructure services
./scripts/start-infra.sh

# View logs for a specific service
./scripts/logs.sh postgres

# Stop all infrastructure services
./scripts/stop-infra.sh
```

### Services Included

| Service | Port | Credentials | Purpose |
|---------|------|-------------|---------|
| PostgreSQL | 5432 | postgres/postgres | Primary database |
| Redis | 6379 | - | Caching layer |
| Kafka | 9092 | - | Event streaming |
| Schema Registry | 8081 | - | Kafka schema management |
| Temporal | 7233, 8233 | - | Workflow orchestration |
| Keycloak | 8080 | admin/admin | Identity & access management |
| Elasticsearch | 9200 | - | Log storage |
| Logstash | 5000 | - | Log processing |
| Kibana | 5601 | - | Log visualization |
| Prometheus | 9090 | - | Metrics collection |
| Grafana | 3000 | admin/admin | Metrics visualization |
| Jaeger | 16686, 14268 | - | Distributed tracing |
| Apache Fineract | 8443 | - | Core banking system |

### Accessing UIs

- **Keycloak**: http://localhost:8080 (admin/admin)
- **Kibana**: http://localhost:5601
- **Grafana**: http://localhost:3000 (admin/admin)
- **Temporal UI**: http://localhost:8233
- **Jaeger UI**: http://localhost:16686
- **Prometheus**: http://localhost:9090
- **Fineract**: https://localhost:8443

### Health Checks

```bash
# Check all services status
docker-compose ps

# Check specific service health
docker-compose exec postgres pg_isready
docker-compose exec redis redis-cli ping
```

### Data Persistence

All data is persisted in Docker volumes:
- `postgres_data` - PostgreSQL data
- `elasticsearch_data` - Elasticsearch indices
- `prometheus_data` - Prometheus metrics
- `grafana_data` - Grafana dashboards

To completely remove all data:
```bash
docker-compose down -v
```

### Troubleshooting

#### Port Conflicts
If you get port conflict errors, ensure no other services are running on the required ports:
```bash
# Check what's using a port (example for port 5432)
lsof -i :5432
```

#### Out of Memory
If services are crashing, increase Docker's memory allocation:
- Docker Desktop: Settings → Resources → Memory (increase to 8GB+)

#### Service Won't Start
```bash
# View logs for the problematic service
./scripts/logs.sh <service-name>

# Restart a specific service
docker-compose restart <service-name>
```

### Configuration Files

- **[docker-compose.yml](../../../docker-compose.yml)** - Main Docker Compose configuration
- **[Logstash Pipeline](../../../infrastructure/logstash/pipeline/logstash.conf)** - Log processing configuration
- **[Prometheus Config](../../../infrastructure/prometheus/prometheus.yml)** - Metrics scrape configuration
- **[Environment Variables](../../../.env.example)** - Environment variable template

### Related Documentation

- **[Setup Complete](setup-complete.md)** - Detailed setup summary
- **[Checklist](checklist.md)** - Requirements verification
- **[Quick Start Guide](../../getting-started/quick-start.md)** - Get started quickly

## Production Deployment

Production deployment uses Kubernetes and is configured in the `infrastructure/kubernetes/` directory.

See [Kubernetes Deployment](../kubernetes/) for details (coming soon).

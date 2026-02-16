# KSA Islamic Financing Platform - Quick Start Guide

## Prerequisites

- Docker 20.10+ and Docker Compose 2.0+
- At least 8GB RAM available for Docker
- Ports available: 3000, 5000, 5432, 5601, 6379, 7233, 8080, 8081, 8233, 8443, 9090, 9092, 9200, 14268, 16686

## Start Infrastructure (3 Commands)

```bash
# 1. Navigate to project
cd /var/www/islamic-financing-platform

# 2. Start all infrastructure
./scripts/start-infra.sh

# 3. Wait 2-3 minutes, then check status
docker-compose ps
```

## Access UIs

| Service | URL | Credentials |
|---------|-----|-------------|
| Keycloak | http://localhost:8080 | admin/admin |
| Grafana | http://localhost:3000 | admin/admin |
| Kibana | http://localhost:5601 | - |
| Temporal | http://localhost:8233 | - |
| Jaeger | http://localhost:16686 | - |
| Prometheus | http://localhost:9090 | - |

## Common Commands

```bash
# View logs
./scripts/logs.sh <service-name>

# Stop all services
./scripts/stop-infra.sh

# Validate setup
./scripts/validate-setup.sh

# Check service health
docker-compose ps
```

## Database Connection

```
Host: localhost
Port: 5432
Database: ksa_financing
Username: postgres
Password: postgres
```

## Redis Connection

```
Host: localhost
Port: 6379
```

## Kafka Connection

```
Bootstrap Servers: localhost:9092
Schema Registry: http://localhost:8081
```

## Need Help?

- **Documentation Index**: [docs/README.md](../README.md)
- **Full Setup Guide**: [Infrastructure Local Development](../infrastructure/local-development/README.md)
- **Setup Details**: [Setup Complete](../infrastructure/local-development/setup-complete.md)
- **Verification Checklist**: [Checklist](../infrastructure/local-development/checklist.md)

## Stop Infrastructure

```bash
# Stop and keep data
./scripts/stop-infra.sh

# Stop and remove all data
docker-compose down -v
```

# Add Service to Docker Compose

You are the **Docker Engineer**. Add microservices to Docker Compose configuration.

## Input
- Service to add: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md`
2. `/var/www/islamic-financing-platform/docs/standards/ENVIRONMENT_CONFIG.md` (ALL ports and env vars)
3. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md`
4. Current: `/var/www/islamic-financing-platform/docker-compose.yml`

## CRITICAL: Zero Hardcoding
ALL ports from ENVIRONMENT_CONFIG.md port registry.
ALL config via ${ENV_VAR:-default} pattern.
ALL Keycloak config via ${KEYCLOAK_BASE_URL}, ${KEYCLOAK_REALM} vars.

## Template (env vars from ENVIRONMENT_CONFIG.md)
```yaml
  {service-name}:
    build:
      context: ./services/{service-name}
      dockerfile: Dockerfile
    container_name: {service-name}
    ports:
      - "${SERVICE_PORT:-{port}}:{port}"
    environment:
      - SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-dev}
      - SERVER_PORT={port}
      - DB_HOST=postgres
      - DB_PORT=5432
      - DB_NAME={service}_db
      - DB_USERNAME=${DB_USERNAME:-postgres}
      - DB_PASSWORD=${DB_PASSWORD:-postgres}
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - SCHEMA_REGISTRY_URL=http://schema-registry:8082
      - TEMPORAL_ADDRESS=temporal:7233
      - TEMPORAL_NAMESPACE=${TEMPORAL_NAMESPACE:-default}
      - KEYCLOAK_BASE_URL=http://keycloak:8080
      - KEYCLOAK_REALM=${KEYCLOAK_REALM:-islamic-financing}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - OTEL_EXPORTER_OTLP_ENDPOINT=http://jaeger:4317
    depends_on:
      postgres:
        condition: service_healthy
      kafka:
        condition: service_healthy
    networks:
      - financing-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:{port}/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    restart: unless-stopped
```

## Checklist
- [ ] Port from ENVIRONMENT_CONFIG.md port registry
- [ ] ALL config via env variables
- [ ] Credentials use ${VAR:-default} pattern
- [ ] Health check configured
- [ ] depends_on with service_healthy
- [ ] Database in postgres init.sql

# Environment Configuration - Zero Hardcoding Policy

> **RULE**: No URL, port, credential, or configuration value shall EVER be hardcoded in application code, commands, or agent instructions. ALL values come from environment variables with sensible defaults for local development only.

---

## Environment Variable Registry

### Database
| Variable | Default (Dev) | Description |
|----------|--------------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `DB_NAME` | `{service}_db` | Per-service database name |

### Kafka
| Variable | Default (Dev) | Description |
|----------|--------------|-------------|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `SCHEMA_REGISTRY_URL` | `http://localhost:8082` | Confluent Schema Registry |
| `KAFKA_CONSUMER_GROUP` | `${spring.application.name}` | Consumer group ID |

### Temporal
| Variable | Default (Dev) | Description |
|----------|--------------|-------------|
| `TEMPORAL_ADDRESS` | `localhost:7233` | Temporal server |
| `TEMPORAL_NAMESPACE` | `default` | Temporal namespace |
| `TEMPORAL_TASK_QUEUE` | `${spring.application.name}-queue` | Task queue name |

### Keycloak / Security
| Variable | Default (Dev) | Description |
|----------|--------------|-------------|
| `KEYCLOAK_BASE_URL` | `http://localhost:8080` | Keycloak base URL |
| `KEYCLOAK_REALM` | `islamic-financing` | Keycloak realm name |
| `KEYCLOAK_ISSUER_URI` | `${KEYCLOAK_BASE_URL}/realms/${KEYCLOAK_REALM}` | JWT issuer |
| `KEYCLOAK_JWK_SET_URI` | `${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/certs` | JWKS endpoint |

### Observability
| Variable | Default (Dev) | Description |
|----------|--------------|-------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://localhost:4317` | OpenTelemetry collector |
| `ELASTICSEARCH_URL` | `http://localhost:9200` | Elasticsearch |
| `LOGSTASH_HOST` | `localhost:5044` | Logstash |

### Redis
| Variable | Default (Dev) | Description |
|----------|--------------|-------------|
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |

### Fineract / Core Banking
| Variable | Default (Dev) | Description |
|----------|--------------|-------------|
| `FINERACT_BASE_URL` | `https://localhost:8443/fineract-provider/api/v1` | Fineract API |
| `FINERACT_TENANT_ID` | `default` | Fineract tenant |

### Application
| Variable | Default (Dev) | Description |
|----------|--------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active profile |
| `LOG_LEVEL` | `DEBUG` | Root log level |
| `SERVER_PORT` | `8080` | Service HTTP port |

---

## Service Port Registry (Dev Only)

> These ports are configured via `SERVER_PORT` env var per service. The defaults below are for local development Docker Compose only.

| Service | Dev Port | Env Var |
|---------|----------|---------|
| risk-service | 8090 | `SERVER_PORT=8090` |
| kyc-adapter-service | 8087 | `SERVER_PORT=8087` |
| customer-service | 8091 | `SERVER_PORT=8091` |
| wallet-service | 8092 | `SERVER_PORT=8092` |
| identity-service | 8093 | `SERVER_PORT=8093` |
| global-profile-service | 8094 | `SERVER_PORT=8094` |
| pii-vault-service | 8095 | `SERVER_PORT=8095` |
| onboarding-workflow-service | 8096 | `SERVER_PORT=8096` |
| lending-service | 8097 | `SERVER_PORT=8097` |
| product-service | 8098 | `SERVER_PORT=8098` |
| service-template | 8081 | `SERVER_PORT=8081` |

## Infrastructure Port Registry (Dev Only)

| Infrastructure | Dev Port | Purpose |
|---------------|----------|---------|
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache/idempotency |
| Kafka | 9092 | Message broker |
| Zookeeper | 2181 | Kafka coordination |
| Schema Registry | 8082 | Avro schemas |
| Keycloak | 8080 | Identity provider |
| Temporal Server | 7233 | Workflow orchestration |
| Temporal UI | 8233 | Workflow dashboard |
| Elasticsearch | 9200 | Search/logging |
| Logstash | 5044 | Log pipeline |
| Kibana | 5601 | Log visualization |
| Jaeger | 16686 | Distributed tracing |
| Prometheus | 9090 | Metrics |
| Grafana | 3000 | Dashboards |
| Kong Gateway | 8000 | API gateway |
| Fineract | 8443 | Core banking |
| Portainer | 9443 | Container management |

---

## application.yml Template (Zero Hardcoding)

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: ${SERVICE_NAME:my-service}
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:service_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    hikari:
      maximum-pool-size: ${DB_POOL_SIZE:10}
  jpa:
    hibernate:
      ddl-auto: validate  # NEVER change - Flyway handles schema
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: ${spring.application.name}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
    properties:
      schema.registry.url: ${SCHEMA_REGISTRY_URL:http://localhost:8082}
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_BASE_URL:http://localhost:8080}/realms/${KEYCLOAK_REALM:islamic-financing}
          jwk-set-uri: ${KEYCLOAK_BASE_URL:http://localhost:8080}/realms/${KEYCLOAK_REALM:islamic-financing}/protocol/openid-connect/certs
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}

temporal:
  service-address: ${TEMPORAL_ADDRESS:localhost:7233}
  namespace: ${TEMPORAL_NAMESPACE:default}
  task-queue: ${TEMPORAL_TASK_QUEUE:${spring.application.name}-queue}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized

logging:
  level:
    root: ${LOG_LEVEL:INFO}
    com.ksa.financing: ${APP_LOG_LEVEL:DEBUG}

ksa:
  fineract:
    base-url: ${FINERACT_BASE_URL:https://localhost:8443/fineract-provider/api/v1}
    tenant-id: ${FINERACT_TENANT_ID:default}
  error:
    additional-basenames: []
```

## CRITICAL RULES

1. **NEVER hardcode URLs** - Use `${ENV_VAR:default}` in application.yml
2. **NEVER hardcode ports** - Use `${SERVER_PORT:default}` pattern
3. **NEVER hardcode credentials** - Use `${DB_PASSWORD}` (no default for prod)
4. **NEVER hardcode Keycloak realm** - Use `${KEYCLOAK_REALM}` env var
5. **NEVER hardcode Kafka topics** - Define as constants in messaging-event-sdk
6. **NEVER hardcode API paths** - Define base paths in SDK or application.yml
7. **Profile-specific configs**: Use `application-{profile}.yml` for overrides
8. **Secrets in production**: Use Vault/K8s Secrets, never .env files

# ELK + APM — Centralized Observability Setup Guide

> Production-ready Elasticsearch + Logstash + Kibana + Elastic APM for the KSA Islamic Financing Platform (Java/Spring Boot microservices).

---

## 1. Architecture

```
┌─────────────────┐      stdout (JSON)        ┌──────────┐
│ Spring services │ ─────────────────────────▶│ Filebeat │──┐
│  (Logback)      │                            └──────────┘  │
│                 │      TCP 5000 (json_lines) ┌──────────┐  │
│                 │ ─────────────────────────▶│ Logstash │──┤
│                 │                            └──────────┘  │   ┌───────────────┐
│  + APM Agent    │      HTTP 8200 (traces)   ┌──────────┐  ├──▶│ Elasticsearch │◀── Kibana (5601)
│ (-javaagent)    │ ─────────────────────────▶│ APM      │──┘   └───────────────┘
└─────────────────┘                            │ Server   │
                                               └──────────┘
```

| Component        | Purpose                                                              | Port |
| ---------------- | -------------------------------------------------------------------- | ---- |
| Elasticsearch    | Stores logs + traces + metrics. Indexed and searchable.              | 9200 |
| Logstash         | Receives Logback TCP stream → routes to correct ES indices.          | 5000 |
| Filebeat         | Safety-net: ships every container stdout to ES.                      | —    |
| APM Server       | Receives traces/spans/errors from Java agent.                        | 8200 |
| Kibana           | UI for Discover, Dashboards, APM, Logs, alerting.                    | 5601 |
| es-setup         | One-shot: installs ILM policy + index templates on first boot.       | —    |
| kibana-setup     | One-shot: loads data views + sample dashboard.                       | —    |
| apm-agent-init   | One-shot: downloads agent jar into shared volume `apm_agent_data`.   | —    |

---

## 2. Why each piece?

- **APM Agent (Java)** — auto-instruments Spring MVC, JDBC, RestTemplate, WebClient, Kafka, Redis, Temporal client. You see: latency, slow queries, failed requests, distributed traces between services, stack traces of exceptions.
- **Logback LogstashEncoder + TCP appender** — emits structured JSON logs with `correlationId`, `traceId`, `spanId`, `tenantId` already attached (via MDC) so logs auto-link to APM traces in Kibana.
- **Filebeat** — backup channel. If Logstash TCP is down, container stdout is still harvested. Production resilience.
- **Logstash routing** — splits api-audit vs app-logs vs errors into separate indices (better retention + query speed).
- **ILM (Index Lifecycle Management)** — automatic hot/warm/cold/delete @ 30 days. No manual cleanup, no disk explosion.

---

## 3. Index strategy

| Index pattern                              | Source                | Contains                                  |
| ------------------------------------------ | --------------------- | ----------------------------------------- |
| `app-logs-{service}-YYYY.MM.dd`            | Logstash              | Normal application logs                   |
| `app-logs-errors-{service}-YYYY.MM.dd`     | Logstash (level=ERROR)| Error-level logs only (fast triage)       |
| `api-audit-inbound-{service}-YYYY.MM.dd`   | Logstash (logger=api-audit) | Inbound REST API audit                |
| `api-audit-outbound-{service}-YYYY.MM.dd`  | Logstash              | Outbound calls to internal services       |
| `api-audit-thirdparty-YYYY.MM`             | Logstash              | Third-party (gov / payment) calls         |
| `api-audit-errors-YYYY.MM.dd`              | Logstash (status≥400) | HTTP 4xx/5xx                              |
| `filebeat-docker-YYYY.MM.dd`               | Filebeat              | Raw container stdout (safety net)         |
| `traces-apm-*`, `apm-*`                    | APM Server            | Distributed traces, spans, errors, metrics|

All indices follow ILM policy `ksa-logs-30d` (hot 0–2d → warm 2–7d → cold 7–30d → delete).

---

## 4. Step-by-step setup

### 4.1 First boot
```bash
cd /var/www/islamic-financing-platform

# Build APM-agent init image (downloads agent jar once)
docker compose build apm-agent-init

# Start the ELK + APM stack (order matters but compose handles depends_on)
docker compose up -d elasticsearch kibana logstash apm-server filebeat \
                    es-setup kibana-setup apm-agent-init

# Watch setup containers complete:
docker logs ksa-es-setup
docker logs ksa-kibana-setup
docker logs ksa-apm-agent-init
```

Expected output:
```
[es-setup] Installing ILM policy: ksa-logs-30d
[es-setup] Installing index template: app-logs
[es-setup] Installing index template: api-audit
[es-setup] Done.

[kibana-setup] Importing data views + dashboards
{"successCount":5,...}

ksa-apm-agent-init  | elastic-apm-agent.jar  (~2 MB)
ksa-apm-agent-init  | APM agent ready in shared volume.
```

### 4.2 Start services
```bash
# Sample wired service:
docker compose up -d risk-service

# When you wire more services (see §6), bring them up the same way.
```

### 4.3 Open Kibana
URL: **http://46.62.226.94:5601** (or `http://localhost:5601` locally)

- **Discover** → pick data view `app-logs-*` → live tail your service logs.
- **APM** (left nav → Observability → APM) → service list → click `risk-service` → see request latency, transaction breakdown, errors, dependencies.
- **Dashboards** → "KSA — Platform Overview" (auto-imported).

---

## 5. URLs you can open

| What                 | URL                                       | Notes                               |
| -------------------- | ----------------------------------------- | ----------------------------------- |
| **Kibana UI**        | http://46.62.226.94:5601                  | Main UI — Discover, APM, Dashboards |
| Elasticsearch API    | http://localhost:9200                     | Bound to localhost only             |
| APM Server status    | http://46.62.226.94:8200                  | JSON `{ build_date, version }`      |
| Health (any service) | http://46.62.226.94:8090/actuator/health  | risk-service example                |

Once you log in and click left-nav → **Observability → APM**, you'll see:
- Service list with avg latency, throughput, error rate
- Transactions → click any → flame graph + waterfall
- Errors → grouped by exception class with stack traces
- Dependencies → calls to other services / Postgres / Redis / Kafka

---

## 6. Wire APM agent into any other service

The agent jar lives in a shared volume — **no Dockerfile changes**. Add this to each Java service's compose block:

```yaml
  customer-service:
    # ... existing fields ...
    environment:
      # ... existing ...
      LOGSTASH_HOST: logstash
      LOGSTASH_PORT: "5000"
      ENV: ${APM_ENVIRONMENT:-dev}
      JAVA_TOOL_OPTIONS: "-javaagent:/apm-agent/elastic-apm-agent.jar"
      ELASTIC_APM_SERVICE_NAME: customer-service
      ELASTIC_APM_SERVER_URLS: http://apm-server:8200
      ELASTIC_APM_ENVIRONMENT: ${APM_ENVIRONMENT:-dev}
      ELASTIC_APM_APPLICATION_PACKAGES: com.ksa.financing
      ELASTIC_APM_CAPTURE_BODY: errors
      ELASTIC_APM_TRANSACTION_SAMPLE_RATE: "1.0"
    volumes:
      - apm_agent_data:/apm-agent:ro
    depends_on:
      # ... existing ...
      apm-agent-init:
        condition: service_completed_successfully
      apm-server:
        condition: service_healthy
```

> **JAVA_TOOL_OPTIONS** is auto-picked up by the JVM — no entrypoint change required.

---

## 7. Logback config (already in foundational-infra-sdk)

Every service inherits `logback-ksa-base.xml` via:

```xml
<!-- services/{service}/src/main/resources/logback-spring.xml -->
<configuration>
    <include resource="logback-ksa-base.xml"/>
    <springProfile name="dev,docker,prod">
        <root level="${LOG_LEVEL:-INFO}">
            <appender-ref ref="KSA_JSON_CONSOLE"/>
            <appender-ref ref="KSA_ASYNC_LOGSTASH"/>
        </root>
    </springProfile>
</configuration>
```

The base file already attaches **MDC fields**: `correlationId`, `traceId`, `spanId`, `tenantId`. The APM Java agent automatically injects `trace.id` and `span.id` into MDC under those names, so logs in Discover are **clickable straight to APM traces**.

---

## 8. Correlation ID flow

```
Client request
   │  X-Correlation-ID: abc-123 (or auto-generated)
   ▼
CorrelationIdFilter (foundational-infra-sdk) ──► MDC.put("correlationId", id)
   │
   ▼
APM Agent intercepts span ──► MDC.put("trace.id", ...) / MDC.put("transaction.id", ...)
   │
   ▼
Logback JSON encoder includes BOTH in every log line
   │
   ▼
Logstash → Elasticsearch indexed as keyword fields
   │
   ▼
Kibana Discover: filter by correlationId = abc-123  → see ALL logs across services
Kibana APM:     click trace.id link inside a log row → opens distributed trace view
```

---

## 9. Sample log line (JSON)

```json
{
  "@timestamp": "2026-05-14T07:42:11.123Z",
  "service": "risk-service",
  "environment": "dev",
  "level": "INFO",
  "logger": "com.ksa.financing.risk.application.usecase.AssessRiskUseCaseImpl",
  "thread": "http-nio-8090-exec-3",
  "message": "Risk assessment completed: score=42 level=MEDIUM",
  "correlationId": "abc-123",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "tenantId": "00000000-0000-0000-0000-000000000001",
  "userId": "user-9988"
}
```

---

## 10. Sample API audit log (logger = `api-audit`)

```java
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

private static final org.slf4j.Logger AUDIT = LoggerFactory.getLogger("api-audit");

AUDIT.info("inbound API call",
    kv("audit_type", "api"),
    kv("direction", "inbound"),
    kv("method", "POST"),
    kv("path", "/api/v1/risk/assess"),
    kv("status_code", 200),
    kv("duration_ms", 142),
    kv("user_id", userId),
    kv("tenant_id", tenantId));
```

Routes automatically to `api-audit-inbound-risk-service-YYYY.MM.dd`.

---

## 11. Distributed tracing example

User calls `POST /api/v1/customers/{id}/risk-check`:

```
customer-service ──HTTP──▶ risk-service ──HTTP──▶ fraud-service
                                       └──JDBC──▶ Postgres
                                       └──Redis──▶ velocity counter
```

APM agent auto-creates **one trace** with spans for all hops. In Kibana → APM → Traces, you see:

```
POST /customers/{id}/risk-check      [customer-service]    320 ms ▓▓▓▓▓▓▓▓
  └─ HTTP POST /api/v1/risk/assess   [risk-service]        280 ms   ▓▓▓▓▓▓▓
       ├─ HTTP POST /api/v1/fraud    [fraud-service]        45 ms      ▓
       ├─ SELECT FROM risk_profiles  [Postgres]             92 ms       ▓▓
       └─ GET velocity:ip:1.2.3.4    [Redis]                 2 ms        ·
```

Click any span → see SQL query text, HTTP request/response headers (since `ELASTIC_APM_CAPTURE_BODY=errors`), exception stack.

---

## 12. Production / security hardening

1. **Enable X-Pack security** on ES:
   - Set `xpack.security.enabled=true`
   - Generate certificates, configure Kibana basic auth.
   - Add `ELASTIC_USERNAME` / `ELASTIC_PASSWORD` env vars to Logstash, Filebeat, APM Server.
2. **Don't expose ES directly** — it stays on `127.0.0.1:9200` (already configured). All access goes through Kibana.
3. **Secret token** for APM ingest: set `apm-server.auth.secret_token` + `ELASTIC_APM_SECRET_TOKEN` on each service.
4. **PII scrubbing** — agent option: `ELASTIC_APM_SANITIZE_FIELD_NAMES=password,token,*card*,authorization,*nid*` (already a default plus extra).
5. **Drop body capture in prod** if requests carry PII: `ELASTIC_APM_CAPTURE_BODY=off` (or only on errors).
6. **Network segmentation** — APM Server, Logstash, ES on internal docker network only. Expose Kibana through Nginx with TLS + Keycloak SSO.

---

## 13. Scaling recommendations

| When                                | Action                                                                 |
| ----------------------------------- | ---------------------------------------------------------------------- |
| Single-node ES at >70% disk         | Adjust ILM: `delete.min_age: 14d`; add more nodes; enable cold tier.   |
| Ingestion bursts                    | Add Kafka between Logstash and ES (Logstash kafka input).              |
| Too many transactions in APM        | `ELASTIC_APM_TRANSACTION_SAMPLE_RATE=0.1` (10%) in prod.               |
| Slow Discover queries               | Force-merge old indices (already in ILM warm phase).                   |
| Multi-AZ                            | ES cluster mode (`discovery.type` → seed_hosts, master nodes ≥ 3).     |
| Log volume > 50 GB/day              | Move from single-node to 3-node hot + 2-node warm; SSD for hot.        |

---

## 14. Troubleshooting

| Symptom                                   | Check                                                                  |
| ----------------------------------------- | ---------------------------------------------------------------------- |
| No logs in Discover                       | `docker logs ksa-logstash`. Is service emitting to logstash:5000?      |
| `app-logs-*` data view shows no fields    | Re-run `docker compose up kibana-setup`.                               |
| APM service not appearing                 | Confirm `JAVA_TOOL_OPTIONS` env reached the JVM: `docker exec ksa-risk-service env | grep JAVA_TOOL`. |
| `elastic-apm-agent.jar not found`         | `apm-agent-init` didn't run. Run `docker compose up apm-agent-init`.   |
| Filebeat permission denied                | `user: root` set in compose; ensure docker.sock mount.                 |
| ES yellow status                          | Single-node — yellow is normal. Replicas set to 0 in templates.        |
| Stack traces not parsed                   | Logback `LogstashEncoder` includes `stack_trace` by default — verify JSON output. |

```bash
# Quick diagnostic
curl -s http://localhost:9200/_cat/indices?v | head -20
curl -s http://localhost:9200/_ilm/policy/ksa-logs-30d | head -c 400
curl -s http://localhost:5601/api/saved_objects/_find?type=index-pattern
curl -s http://localhost:8200/                   # APM Server should return JSON
```

---

## 15. Best practices

1. **Always include `correlationId`** in API responses (already done by `CorrelationIdFilter`). Frontend can echo it back.
2. **Never log secrets** — JWT, passwords, full card numbers. Use APM sanitize list.
3. **Use structured logs**: `log.info("payment processed amount={} customer={}", amount, customerId)` — Logstash extracts fields.
4. **One logger per concern** — keep `api-audit` separate from app loggers.
5. **Sample heavily in prod** — 10% APM sample rate is enough to detect issues; you can dial up temporarily.
6. **Set up alerts in Kibana** — rule for error rate > X%, latency p95 > Y ms.
7. **Tag environment correctly** — `ENV=staging` vs `ENV=prod` — never mix in same indices.

---

## 16. Files in this setup

```
infrastructure/elk/
├── apm-server/
│   └── apm-server.yml              # APM Server config
├── filebeat/
│   └── filebeat.yml                # Docker logs harvester
├── elasticsearch/
│   ├── ilm-policy.json             # 30-day lifecycle
│   ├── index-template-app-logs.json
│   ├── index-template-api-audit.json
│   └── setup-templates.sh          # bootstrap script
├── kibana/
│   ├── saved-objects.ndjson        # data views + dashboard
│   └── setup-kibana.sh
└── apm-agent/
    └── Dockerfile                  # init container (downloads agent jar)

infrastructure/logstash/pipeline/
└── logstash.conf                   # routing rules

shared-libraries/foundational-infra-sdk/
└── src/main/resources/
    └── logback-ksa-base.xml        # included by all services

docs/observability/
└── ELK_APM_SETUP.md                # this file
```

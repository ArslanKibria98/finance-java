# Resilience Standards - Single Source of Truth

> Reference: Blueprint `06_ORCHESTRATION_PATTERNS.md`, `08_KSA_GOVERNMENT_APIS.md`, `13_HARDENED_SAGA_PATTERNS.md`

## Circuit Breaker (Resilience4j)

### Configuration per Call Type
| Target | Failure Threshold | Wait Duration | Sliding Window | Fallback |
|--------|------------------|---------------|----------------|----------|
| Internal REST | 50% | 30s | 10 calls | Return cached/default |
| Fineract API | 50% | 60s | 10 calls | Queue for retry |
| Nafath | 40% | 120s | 5 calls | Return pending status |
| Yakeen | 40% | 120s | 5 calls | Manual fallback |
| Simah | 30% | 180s | 5 calls | Queue (critical path) |
| SADAD | 50% | 60s | 10 calls | Alternative provider |
| GOSI | 40% | 120s | 5 calls | Manual salary entry |

### Retry Policy
| Target | Max Attempts | Initial Wait | Backoff | Max Wait |
|--------|-------------|-------------|---------|----------|
| Internal REST | 3 | 500ms | 2.0x exponential | 5s |
| Fineract | 3 | 1s | 2.0x exponential | 10s |
| Government APIs | 3 | 2s | 2.0x exponential | 30s |
| Kafka publish | 5 | 500ms | 2.0x exponential | 30s |
| Temporal activity | 3 | 1s | 2.0x exponential | 60s |

### Bulkhead
| Service | Max Concurrent | Max Wait |
|---------|---------------|----------|
| Default | 25 | 0ms (reject) |
| Fineract | 10 | 500ms |
| Government APIs | 5 | 1s |

## Rate Limiting (Government APIs)

| API | Rate Limit | Window | Strategy |
|-----|-----------|--------|----------|
| Nafath | 100/min | Sliding | Token bucket |
| Yakeen | 50/min | Sliding | Token bucket |
| Simah | 30/min | Sliding | Token bucket + queue |
| SADAD | 200/min | Sliding | Token bucket |
| GOSI | 50/min | Sliding | Token bucket |
| ZATCA | 100/min | Sliding | Token bucket |

## Temporal Activity Timeouts

| Activity Type | Start-to-Close | Schedule-to-Close | Heartbeat |
|--------------|----------------|-------------------|-----------|
| Quick (internal DB) | 30s | 1min | N/A |
| Standard (REST call) | 5min | 10min | 30s |
| External API | 10min | 30min | 1min |
| Long-running (batch) | 30min | 1h | 5min |
| Human-in-loop | 72h | 7 days | 1h |

## Idempotency

### Key Generation
```
SHA256(operation_type:entity_type:entity_id:amount:timestamp_bucket)
```

### Storage
- Redis with TTL (24 hours for financial, 1 hour for non-financial)
- Backup in PostgreSQL `idempotency_keys` table

### Workflow
```
1. Receive request with X-Idempotency-Key header
2. Check Redis for existing key
3. If found: return cached response
4. If not found: acquire distributed lock
5. Execute operation
6. Store result with key in Redis + PostgreSQL
7. Release lock
8. Return response
```

## SAGA Compensation

### Rules
1. Every step that modifies external state MUST have a compensation
2. Compensations execute in REVERSE order
3. Compensations MUST be idempotent (safe to run multiple times)
4. Failed compensations go to Dead Letter Queue (DLQ)
5. DLQ items require manual resolution via admin UI
6. All SAGA state stored in Temporal workflow history

### Pattern
```java
SagaOrchestrator.begin()
    .step("step-name",
        () -> action(),           // Forward action
        (result) -> compensate()) // Compensation
    .step(...)
    .execute();
```

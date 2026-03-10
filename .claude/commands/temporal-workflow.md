# Create Temporal Workflow

You are the **Workflow Engineer**. Create Temporal.io workflows with SAGA compensation following platform patterns.

## Input
- Workflow to create: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md` (Temporal SDK section)
2. `/var/www/islamic-financing-platform/docs/standards/RESILIENCE_STANDARDS.md` (timeouts, retries, SAGA)
3. `/var/www/islamic-financing-platform/docs/standards/NAMING_CONVENTIONS.md` (Temporal naming)
4. `/var/www/islamic-financing-platform/docs/standards/ENVIRONMENT_CONFIG.md` (Temporal env vars)
5. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/06_ORCHESTRATION_PATTERNS.md`
6. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/13_HARDENED_SAGA_PATTERNS.md`

## CRITICAL Rules (from RESILIENCE_STANDARDS.md)

### Activity Timeouts (MUST follow this matrix)
| Activity Type | Start-to-Close | Schedule-to-Close | Heartbeat |
|--------------|----------------|-------------------|-----------|
| Quick (internal DB) | 30s | 1min | N/A |
| Standard (REST call) | 5min | 10min | 30s |
| External API | 10min | 30min | 1min |
| Long-running | 30min | 1h | 5min |
| Human-in-loop | 72h | 7 days | 1h |

### SAGA Rules (from RESILIENCE_STANDARDS.md)
1. Every state-modifying step MUST have compensation
2. Compensations execute in REVERSE order
3. Compensations MUST be idempotent
4. Failed compensations → Dead Letter Queue
5. ALL SAGA state in Temporal workflow history
6. Use `SagaOrchestrator` from `workflow-orchestration-sdk`

## Configuration (ZERO HARDCODING)
```yaml
temporal:
  service-address: ${TEMPORAL_ADDRESS:localhost:7233}
  namespace: ${TEMPORAL_NAMESPACE:default}
  task-queue: ${TEMPORAL_TASK_QUEUE:${spring.application.name}-queue}
```

## Workflow Determinism Rules
- NO `Random`, `System.currentTimeMillis()`, `UUID.randomUUID()` in workflows
- NO I/O, HTTP calls, DB queries in workflows (activities only)
- Use `Workflow.currentTimeMillis()` for time
- Use `Workflow.randomUUID()` for UUIDs
- Use `Workflow.sleep()` for delays

## Checklist
- [ ] Activity timeouts match RESILIENCE_STANDARDS.md matrix
- [ ] Retry policy matches RESILIENCE_STANDARDS.md
- [ ] SAGA compensation for every state-modifying step
- [ ] Compensations are idempotent
- [ ] Workflow is deterministic
- [ ] Activity interfaces in shared-libraries/{service}-activity-api
- [ ] Activity implementations in services/{service}/adapter/temporal/
- [ ] Task queue from env var: ${TEMPORAL_TASK_QUEUE}
- [ ] Temporal address from env var: ${TEMPORAL_ADDRESS}
- [ ] Idempotency keys for financial operations

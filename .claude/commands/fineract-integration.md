# Fineract Core Banking Integration

You are the **Core Banking Engineer**. Integrate with Apache Fineract via the lms-adapter-sdk.

## Input
- Integration task: $ARGUMENTS

## MANDATORY Reading
1. `/var/www/islamic-financing-platform/CLAUDE.md` (lms-adapter-sdk section)
2. `/var/www/islamic-financing-platform/docs/standards/ENVIRONMENT_CONFIG.md` (Fineract env vars)
3. `/var/www/islamic-financing-platform/docs/standards/RESILIENCE_STANDARDS.md` (circuit breaker, retry)
4. Blueprint: `/var/www/docs/islamic-financing/master-blueprint/07_CORE_BANKING_ADAPTER.md`
5. API Reference: `/var/www/islamic-financing-platform/docs/fineract/FINERACT-API-REFERENCE.md`

## Configuration (ZERO HARDCODING)
```yaml
ksa:
  fineract:
    base-url: ${FINERACT_BASE_URL:https://localhost:8443/fineract-provider/api/v1}
    tenant-id: ${FINERACT_TENANT_ID:default}
```

## Architecture (from Blueprint 07)
- IBankingPort interface in domain layer (pure Java)
- FineractBankingAdapter in infrastructure layer
- ALL Sharia math in domain-core-sdk (NOT in Fineract)
- Fineract = pure bookkeeping/ledger only

## Resilience (from RESILIENCE_STANDARDS.md)
- Circuit Breaker: 50% threshold, 60s wait, 10 calls window
- Retry: 3 attempts, 1s initial, 2.0x backoff
- Bulkhead: 10 concurrent, 500ms wait
- Idempotency keys for ALL financial operations

## Checklist
- [ ] Fineract URL from ${FINERACT_BASE_URL}
- [ ] Tenant from ${FINERACT_TENANT_ID}
- [ ] Circuit breaker per RESILIENCE_STANDARDS.md
- [ ] Idempotency keys for financial operations
- [ ] IBankingPort in domain (pure Java)
- [ ] Adapter in infrastructure
- [ ] Sharia calc in domain-core-sdk (NOT Fineract)
- [ ] Credentials from Vault (NOT hardcoded)

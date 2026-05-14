-- V18__fineract_proxy_audit_log.sql
-- Audit log for every Fineract proxy call routed through ledger-service.
-- Required for SAMA 7-year audit trail and reconciliation.

CREATE TABLE IF NOT EXISTS fineract_audit_log (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    caller_service          VARCHAR(50) NOT NULL,
    caller_user_id          VARCHAR(100),
    operation               VARCHAR(100) NOT NULL,
    fineract_endpoint       VARCHAR(255) NOT NULL,
    http_method             VARCHAR(10) NOT NULL,
    request_body            JSONB,
    response_body           JSONB,
    response_status         INT,
    fineract_resource_id    VARCHAR(100),
    idempotency_key         VARCHAR(100),
    correlation_id          VARCHAR(100),
    error_message           TEXT,
    duration_ms             INT,
    occurred_at             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fal_tenant_op_time
    ON fineract_audit_log (tenant_id, operation, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_fal_caller_time
    ON fineract_audit_log (caller_service, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_fal_idempotency
    ON fineract_audit_log (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_fal_correlation
    ON fineract_audit_log (correlation_id)
    WHERE correlation_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_fal_resource
    ON fineract_audit_log (fineract_resource_id)
    WHERE fineract_resource_id IS NOT NULL;

COMMENT ON TABLE fineract_audit_log IS
    'Audit trail of all Fineract operations proxied through ledger-service. SAMA 7-year retention.';

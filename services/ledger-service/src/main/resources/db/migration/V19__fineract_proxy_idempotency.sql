-- V19__fineract_proxy_idempotency.sql
-- Idempotency cache for Fineract proxy calls.
-- Same Idempotency-Key + same request hash returns cached response.

CREATE TABLE IF NOT EXISTS fineract_proxy_idempotency (
    idempotency_key     VARCHAR(100) NOT NULL,
    tenant_id           UUID NOT NULL,
    operation           VARCHAR(100) NOT NULL,
    request_hash        VARCHAR(64) NOT NULL,
    cached_response     JSONB NOT NULL,
    cached_status       INT NOT NULL,
    expires_at          TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_fpi_expires
    ON fineract_proxy_idempotency (expires_at);

COMMENT ON TABLE fineract_proxy_idempotency IS
    'Idempotency cache for Fineract proxy. Default TTL 24h. Cleanup cron purges expired rows.';

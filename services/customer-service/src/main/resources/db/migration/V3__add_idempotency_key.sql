-- ============================================================================
-- Add idempotency_key to customers table
-- Enables idempotent customer creation
-- ============================================================================

ALTER TABLE customers ADD COLUMN idempotency_key VARCHAR(100);

-- Partial unique index: tenant-scoped idempotency where key is provided
CREATE UNIQUE INDEX uq_customer_idempotency
    ON customers(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

COMMENT ON COLUMN customers.idempotency_key IS 'Client-provided idempotency key to prevent duplicate customer creation';

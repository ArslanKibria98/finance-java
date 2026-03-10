-- ============================================================================
-- Add idempotency_key to loan_applications table
-- Enables idempotent loan application creation
-- ============================================================================

ALTER TABLE loan_applications ADD COLUMN idempotency_key VARCHAR(100);

-- Partial unique index: tenant-scoped idempotency where key is provided
CREATE UNIQUE INDEX uq_loan_app_idempotency
    ON loan_applications(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

COMMENT ON COLUMN loan_applications.idempotency_key IS 'Client-provided idempotency key to prevent duplicate loan applications';

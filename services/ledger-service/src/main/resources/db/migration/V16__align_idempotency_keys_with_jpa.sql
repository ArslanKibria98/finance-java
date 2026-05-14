-- Align idempotency_keys table with IdempotencyKeyJpaEntity columns.
-- The original V1 schema is missing fields used by the GL posting flow.

ALTER TABLE idempotency_keys
    ADD COLUMN IF NOT EXISTS resource_type            VARCHAR(50),
    ADD COLUMN IF NOT EXISTS resource_id              UUID,
    ADD COLUMN IF NOT EXISTS request_hash             VARCHAR(64),
    ADD COLUMN IF NOT EXISTS fineract_transaction_id  BIGINT,
    ADD COLUMN IF NOT EXISTS status                   VARCHAR(20) DEFAULT 'COMPLETED',
    ADD COLUMN IF NOT EXISTS completed_at             TIMESTAMPTZ;

-- Backfill any rows that pre-date this migration so the NOT NULL tightening below succeeds.
UPDATE idempotency_keys
   SET resource_type = COALESCE(resource_type, operation_type),
       resource_id   = COALESCE(resource_id, reference_id),
       request_hash  = COALESCE(request_hash, '0'),
       status        = COALESCE(status, 'COMPLETED');

ALTER TABLE idempotency_keys
    ALTER COLUMN resource_type SET NOT NULL,
    ALTER COLUMN request_hash  SET NOT NULL,
    ALTER COLUMN status        SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_idempotency_resource
    ON idempotency_keys (resource_type, resource_id);

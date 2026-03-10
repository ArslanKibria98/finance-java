-- ============================================================================
-- V2: Link customer_db via postgres_fdw
-- Replaces stub customers_view with a real cross-database view that reads
-- from customer_db.customers and hashes PII (NID, mobile) at the DB level.
-- ============================================================================

-- 1. Enable postgres_fdw extension
CREATE EXTENSION IF NOT EXISTS postgres_fdw;

-- 2. Create foreign server pointing to customer_db (same Postgres instance)
CREATE SERVER customer_db_server
    FOREIGN DATA WRAPPER postgres_fdw
    OPTIONS (host 'localhost', port '5432', dbname 'customer_db');

-- 3. Create user mapping (postgres superuser bypasses RLS on customer_db)
CREATE USER MAPPING FOR postgres
    SERVER customer_db_server
    OPTIONS (user 'postgres', password 'postgres');

-- 4. Create foreign table mirroring customer_db.customers
--    (lifecycle_stage is an enum on remote side; fdw casts it to text automatically)
CREATE FOREIGN TABLE ft_customers (
    id              UUID,
    national_id     VARCHAR(255),
    mobile_number   VARCHAR(255),
    lifecycle_stage TEXT,
    is_active       BOOLEAN,
    blocked_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ
) SERVER customer_db_server
  OPTIONS (schema_name 'public', table_name 'customers');

-- 5. Drop the old stub table and its indexes
DROP TABLE IF EXISTS customers_view CASCADE;

-- 6. Create a view that:
--    - Hashes NID and mobile using SHA-256 (matches Java's SHA-256 hex output)
--    - Maps lifecycle_stage to risk-compatible status
--    - Excludes soft-deleted records
CREATE OR REPLACE VIEW customers_view AS
SELECT
    id,
    encode(digest(national_id, 'sha256'), 'hex')    AS nid_hash,
    encode(digest(mobile_number, 'sha256'), 'hex')   AS mobile_hash,
    CASE
        WHEN NOT is_active OR blocked_at IS NOT NULL
            THEN 'BLOCKED'
        WHEN lifecycle_stage = 'ACTIVE'
            THEN 'ACTIVE'
        WHEN lifecycle_stage IN ('QUALIFIED', 'LEAD', 'PROSPECT', 'APPLICANT')
            THEN 'QUALIFIED'
        WHEN lifecycle_stage = 'DORMANT'
            THEN 'DORMANT'
        WHEN lifecycle_stage = 'CHURNED'
            THEN 'INACTIVE'
        ELSE 'ACTIVE'
    END AS status,
    created_at
FROM ft_customers
WHERE deleted_at IS NULL;

COMMENT ON VIEW customers_view IS 'Cross-database view reading from customer_db.customers with hashed PII. Lifecycle stages mapped to risk-compatible statuses.';

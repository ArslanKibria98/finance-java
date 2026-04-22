-- V9__create_settlements_table.sql
-- Reconciles the settlements table with the SettlementAggregate JPA entity.
--
-- The V1 initial schema already created `settlements` with PG enum columns
-- (settlement_type, settlement_status). Those don't round-trip cleanly through
-- @Enumerated(EnumType.STRING) + the PG driver, so we:
--   1. Convert settlement_type / status to VARCHAR(50)
--   2. Drop the now-orphaned enum types
--   3. Add the discount_amount column (tracks the EARLY_SETTLEMENT rule discount
--      applied on top of Ibra, separate from ibra_amount)
--
-- Idempotent (IF EXISTS / IF NOT EXISTS) so a rerun against any partial state
-- (fresh cluster with the V1 table already in place, or previously-applied V9)
-- succeeds without manual cleanup.

-- 1. settlement_type enum → VARCHAR(50)
ALTER TABLE settlements
    ALTER COLUMN settlement_type TYPE VARCHAR(50) USING settlement_type::text;

-- 2. status enum → VARCHAR(50) (DROP DEFAULT first — cannot cast away from the enum while the default references it)
ALTER TABLE settlements ALTER COLUMN status DROP DEFAULT;
ALTER TABLE settlements
    ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE settlements ALTER COLUMN status SET DEFAULT 'PENDING';

-- 3. Retire enum types (no other tables reference them)
DROP TYPE IF EXISTS settlement_status;
DROP TYPE IF EXISTS settlement_type;

-- 4. discount_amount column for the EARLY_SETTLEMENT rule discount value (Ibra lives in ibra_amount)
ALTER TABLE settlements
    ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19, 4);

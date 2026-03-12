-- V7__convert_remaining_enums_to_varchar.sql
-- V4 missed several tables. Convert all remaining PostgreSQL enum columns to VARCHAR.
-- Must drop partial indexes, triggers, and defaults BEFORE altering types.

-- Phase 1: Drop partial indexes that reference enum types in WHERE clauses
DROP INDEX IF EXISTS idx_loans_dpd;
DROP INDEX IF EXISTS idx_disbursements_pending;

-- Phase 2: Drop triggers that compare old/new status values
DROP TRIGGER IF EXISTS trigger_loan_status ON loans;
DROP TRIGGER IF EXISTS trigger_disbursement_status ON disbursements;

-- Phase 3: Drop defaults that reference enum types
ALTER TABLE loans ALTER COLUMN status DROP DEFAULT;
ALTER TABLE disbursements ALTER COLUMN status DROP DEFAULT;
ALTER TABLE collateral_registry ALTER COLUMN status DROP DEFAULT;

-- Phase 4: Convert all enum columns to VARCHAR
ALTER TABLE loans ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE loan_status_history ALTER COLUMN from_status TYPE VARCHAR(50) USING from_status::text;
ALTER TABLE loan_status_history ALTER COLUMN to_status TYPE VARCHAR(50) USING to_status::text;
ALTER TABLE disbursements ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE disbursement_status_history ALTER COLUMN from_status TYPE VARCHAR(50) USING from_status::text;
ALTER TABLE disbursement_status_history ALTER COLUMN to_status TYPE VARCHAR(50) USING to_status::text;
ALTER TABLE approval_history ALTER COLUMN decision TYPE VARCHAR(50) USING decision::text;
ALTER TABLE collateral_registry ALTER COLUMN collateral_type TYPE VARCHAR(50) USING collateral_type::text;
ALTER TABLE collateral_registry ALTER COLUMN status TYPE VARCHAR(50) USING status::text;
ALTER TABLE contract_version_history ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

-- Phase 5: Restore defaults
ALTER TABLE loans ALTER COLUMN status SET DEFAULT 'PENDING_DISBURSEMENT';
ALTER TABLE disbursements ALTER COLUMN status SET DEFAULT 'PENDING';
ALTER TABLE collateral_registry ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Phase 6: Recreate partial indexes with varchar comparison
CREATE INDEX idx_loans_dpd ON loans(current_dpd) WHERE (status = 'ACTIVE');
CREATE INDEX idx_disbursements_pending ON disbursements(tenant_id) WHERE (status = 'PENDING');

-- Phase 7: Recreate triggers (only those that existed)
CREATE TRIGGER trigger_loan_status
    AFTER UPDATE ON loans
    FOR EACH ROW EXECUTE FUNCTION track_loan_status();

-- Phase 8: Drop enum types
DROP TYPE IF EXISTS loan_status CASCADE;
DROP TYPE IF EXISTS disbursement_status CASCADE;
DROP TYPE IF EXISTS approval_decision CASCADE;
DROP TYPE IF EXISTS collateral_type CASCADE;
DROP TYPE IF EXISTS collateral_status CASCADE;
DROP TYPE IF EXISTS contract_status CASCADE;

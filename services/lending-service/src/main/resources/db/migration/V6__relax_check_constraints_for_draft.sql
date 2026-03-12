-- V6__relax_check_constraints_for_draft.sql
-- At DRAFT stage, amount and tenure are not yet submitted.
-- Relax constraints to allow NULL (checked at application level).

ALTER TABLE loan_applications DROP CONSTRAINT IF EXISTS chk_requested_amount;
ALTER TABLE loan_applications DROP CONSTRAINT IF EXISTS chk_tenure;

-- Re-add with NULL-safe version: only enforce when value is NOT NULL
ALTER TABLE loan_applications ADD CONSTRAINT chk_requested_amount
    CHECK (requested_amount IS NULL OR requested_amount >= 0);

ALTER TABLE loan_applications ADD CONSTRAINT chk_tenure
    CHECK (requested_tenure_months IS NULL OR requested_tenure_months >= 0);

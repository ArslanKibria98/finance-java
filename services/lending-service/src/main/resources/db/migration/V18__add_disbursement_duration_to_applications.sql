-- Snapshots product.disbursementDurationHours onto the loan application at apply time.
-- Value 0 = immediate disbursement (current behavior, backwards compatible).
-- Workflow uses Workflow.sleep() of this many hours between CONTRACT_SIGNED and disbursement.
ALTER TABLE loan_applications
    ADD COLUMN disbursement_duration_hours INT NOT NULL DEFAULT 0,
    ADD COLUMN disbursement_scheduled_at   TIMESTAMPTZ,
    ADD CONSTRAINT chk_disbursement_duration_non_negative
        CHECK (disbursement_duration_hours >= 0);

COMMENT ON COLUMN loan_applications.disbursement_duration_hours IS
    'Hours to wait after contract signing before disbursement. Snapshot from product at apply time. 0 = immediate.';
COMMENT ON COLUMN loan_applications.disbursement_scheduled_at IS
    'Moment after which the workflow may proceed to disbursement (set when contract is signed).';

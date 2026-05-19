-- Captures the real wall-clock moment the application transitions into AWAIT_DISBURSED.
-- Set once inside LoanApplicationAggregate.moveToAwaitDisbursed() and never overwritten,
-- so updatedAt drift from later mutations cannot wipe the transition timestamp.
ALTER TABLE loan_applications
    ADD COLUMN await_disbursed_at TIMESTAMPTZ;

COMMENT ON COLUMN loan_applications.await_disbursed_at IS
    'Wall-clock timestamp captured the moment status entered AWAIT_DISBURSED. Surfaced as disbursed_time while the application is in AWAIT_DISBURSED; null otherwise in the API.';

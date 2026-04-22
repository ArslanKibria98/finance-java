-- V17: Add payment_status and is_skipped to amortization_schedules
-- Required for skip payment tracking

ALTER TABLE amortization_schedules
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS is_skipped BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN amortization_schedules.payment_status IS 'PENDING, PAID, SKIPPED, DEFERRED';
COMMENT ON COLUMN amortization_schedules.is_skipped      IS 'TRUE when installment is deferred via skip payment';

CREATE INDEX IF NOT EXISTS idx_amortization_payment_status
    ON amortization_schedules(loan_id, payment_status) WHERE is_active = TRUE;

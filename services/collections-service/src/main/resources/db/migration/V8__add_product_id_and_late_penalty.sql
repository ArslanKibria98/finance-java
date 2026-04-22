-- V8__add_product_id_and_late_penalty.sql
-- Enables per-product delinquency rule enforcement + late-payment penalty accrual.
--
-- Two orthogonal changes in one migration:
--   1. repayment_schedules.product_id — so the engine can resolve product-specific
--      DelinquencyRule rows at payment time (was always null before → engine
--      falls back to tenant-default only).
--   2. installments.late_penalty_amount — a separate accumulator for LATE_PAYMENT
--      rule penalties. Kept distinct from fee_amount (original schedule fee) so
--      overwriting the penalty each tick is idempotent and doesn't destroy any
--      origination fee the schedule was created with.
--
-- outstanding_amount stays generated = (total_amount - paid_total). The domain
-- now keeps total_amount in sync (= principal + profit + fee + late_penalty)
-- on every penalty tick, so the generated outstanding col remains correct.

ALTER TABLE repayment_schedules
    ADD COLUMN IF NOT EXISTS product_id UUID;

CREATE INDEX IF NOT EXISTS idx_schedules_tenant_product
    ON repayment_schedules(tenant_id, product_id)
    WHERE product_id IS NOT NULL;

ALTER TABLE installments
    ADD COLUMN IF NOT EXISTS late_penalty_amount NUMERIC(19, 4) NOT NULL DEFAULT 0;

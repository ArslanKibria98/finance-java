-- V21: Add fee_amount to loans and fee_component to amortization_schedules
-- Author: Antigravity
-- Date: 2026-05-07

ALTER TABLE loans ADD COLUMN fee_amount NUMERIC(20, 6) NOT NULL DEFAULT 0;
ALTER TABLE amortization_schedules ADD COLUMN fee_component NUMERIC(20, 6) NOT NULL DEFAULT 0;

COMMENT ON COLUMN loans.fee_amount IS 'Total processing and administrative fees at loan inception';
COMMENT ON COLUMN amortization_schedules.fee_component IS 'Fee portion of this installment';

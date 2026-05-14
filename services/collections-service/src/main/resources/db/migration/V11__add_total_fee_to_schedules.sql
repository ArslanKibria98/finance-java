-- V3: Add total_fee to repayment_schedules
-- Author: Antigravity
-- Date: 2026-05-07

ALTER TABLE repayment_schedules ADD COLUMN total_fee NUMERIC(19, 4) NOT NULL DEFAULT 0;

COMMENT ON COLUMN repayment_schedules.total_fee IS 'Sum of all processing and administrative fees across all installments';

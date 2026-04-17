-- V26: Add maxDti, minAge, maxAge, gdbrPercentage to product_fee_settings
-- These fields define eligibility thresholds per product

ALTER TABLE product_fee_settings
    ADD COLUMN IF NOT EXISTS max_dti        NUMERIC(5, 2),
    ADD COLUMN IF NOT EXISTS min_age        INTEGER,
    ADD COLUMN IF NOT EXISTS max_age        INTEGER,
    ADD COLUMN IF NOT EXISTS gdbr_percentage NUMERIC(5, 2);

COMMENT ON COLUMN product_fee_settings.max_dti         IS 'Maximum Debt-to-Income ratio allowed (%)';
COMMENT ON COLUMN product_fee_settings.min_age         IS 'Minimum customer age (years)';
COMMENT ON COLUMN product_fee_settings.max_age         IS 'Maximum customer age (years)';
COMMENT ON COLUMN product_fee_settings.gdbr_percentage IS 'Global DBR percentage override (%)';

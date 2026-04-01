-- V19: Remove min/max financing amount and VAT percentage from fee settings
-- These values are now derived from admin fee slabs (min/max across all slabs)

-- Drop CHECK constraint on financing amount range
ALTER TABLE product_fee_settings DROP CONSTRAINT IF EXISTS chk_financing_amount_range;

-- Drop the columns
ALTER TABLE product_fee_settings DROP COLUMN IF EXISTS min_financing_amount;
ALTER TABLE product_fee_settings DROP COLUMN IF EXISTS max_financing_amount;
ALTER TABLE product_fee_settings DROP COLUMN IF EXISTS vat_percentage;

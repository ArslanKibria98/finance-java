-- V18: Add global_dbr_percentage column to product_fee_settings
-- BRD: Each product must define both a product-specific DBR (max_dbr_percentage)
-- and a global DBR percentage (SAMA regulation, typically 45-65%)

ALTER TABLE product_fee_settings
    ADD COLUMN global_dbr_percentage NUMERIC(5,2);

COMMENT ON COLUMN product_fee_settings.global_dbr_percentage IS 'Global DBR percentage per SAMA regulations (e.g., 45.00 or 65.00)';

-- Seed existing BNPL product with SAMA default 65%
UPDATE product_fee_settings SET global_dbr_percentage = 65.00 WHERE global_dbr_percentage IS NULL;

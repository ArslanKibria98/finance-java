-- V27: Drop global_dbr_percentage column from product_fee_settings
-- Replaced by gdbr_percentage (added in V26) which serves the same purpose.

ALTER TABLE product_fee_settings
    DROP COLUMN IF EXISTS global_dbr_percentage;

-- V32__add_missing_fee_settings_fields.sql
-- Add missing fields to product_fee_settings table as requested by the user.

ALTER TABLE product_fee_settings
    ADD COLUMN penalty_waiver_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN max_penalty_waivers_allowed INTEGER DEFAULT 1,
    ADD COLUMN min_financing_amount NUMERIC(19, 4),
    ADD COLUMN max_financing_amount NUMERIC(19, 4),
    ADD COLUMN vat_percentage NUMERIC(5, 2);

-- Note: These fields might exist in the 'products' table as well, 
-- but we are adding them here to support the Fee Settings tab in the UI.

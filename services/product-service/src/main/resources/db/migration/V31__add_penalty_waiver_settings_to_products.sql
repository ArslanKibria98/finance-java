-- V31__add_penalty_waiver_settings_to_products.sql
-- Add configuration for penalty waivers at product level.

ALTER TABLE products
    ADD COLUMN penalty_waiver_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN max_penalty_waivers_allowed INTEGER DEFAULT 1;

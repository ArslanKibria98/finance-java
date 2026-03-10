-- V5: Add min_tenure and max_tenure to product_admin_fee_slabs
-- Allows each fee slab to define its own tenure range.

ALTER TABLE product_admin_fee_slabs ADD COLUMN min_tenure INT;
ALTER TABLE product_admin_fee_slabs ADD COLUMN max_tenure INT;

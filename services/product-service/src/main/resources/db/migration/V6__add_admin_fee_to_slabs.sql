-- V6: Add admin_fee column to product_admin_fee_slabs
-- Matches admin portal's processing_fee_slabs table which has both processing_fee and admin_fee.

ALTER TABLE product_admin_fee_slabs ADD COLUMN admin_fee NUMERIC(19, 4);

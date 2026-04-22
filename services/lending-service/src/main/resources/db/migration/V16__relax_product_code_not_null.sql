-- V16__relax_product_code_not_null.sql
-- Allow product_code to be NULL in loans table
-- Product-service products may not always have a product_code set
ALTER TABLE loans ALTER COLUMN product_code DROP NOT NULL;

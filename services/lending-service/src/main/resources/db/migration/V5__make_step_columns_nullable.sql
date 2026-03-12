-- V5__make_step_columns_nullable.sql
-- At DRAFT stage only customer_id and tenant_id are known.
-- Product and amount details are submitted at basic-info step.

ALTER TABLE loan_applications ALTER COLUMN product_id DROP NOT NULL;
ALTER TABLE loan_applications ALTER COLUMN product_code DROP NOT NULL;
ALTER TABLE loan_applications ALTER COLUMN requested_amount DROP NOT NULL;
ALTER TABLE loan_applications ALTER COLUMN requested_tenure_months DROP NOT NULL;

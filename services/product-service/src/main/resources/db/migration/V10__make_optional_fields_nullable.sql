-- V10__make_optional_fields_nullable.sql
-- Make product fields nullable that are not required during initial creation

ALTER TABLE products ALTER COLUMN product_code DROP NOT NULL;
ALTER TABLE products ALTER COLUMN sharia_structure DROP NOT NULL;
ALTER TABLE products ALTER COLUMN min_amount DROP NOT NULL;
ALTER TABLE products ALTER COLUMN max_amount DROP NOT NULL;
ALTER TABLE products ALTER COLUMN base_profit_rate DROP NOT NULL;
ALTER TABLE products ALTER COLUMN min_tenure_months DROP NOT NULL;
ALTER TABLE products ALTER COLUMN max_tenure_months DROP NOT NULL;

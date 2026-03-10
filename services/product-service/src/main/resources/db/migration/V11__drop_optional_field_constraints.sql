-- V11__drop_optional_field_constraints.sql
-- Drop check constraints that prevent nullable amount/tenure fields

ALTER TABLE products DROP CONSTRAINT IF EXISTS chk_amount_range;
ALTER TABLE products DROP CONSTRAINT IF EXISTS chk_tenure_range;

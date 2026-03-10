-- V7: Convert JSONB columns to VARCHAR where JPA sends plain strings
-- product_approval_conditions.value is JSONB but JPA entity sends String -> type mismatch

ALTER TABLE product_approval_conditions ALTER COLUMN value TYPE VARCHAR(1000) USING value::text;

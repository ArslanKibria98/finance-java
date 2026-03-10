-- V4: Convert PostgreSQL ENUM columns to VARCHAR
-- Reason: JPA entity stores these as plain String fields.
-- Hibernate sends character varying which PostgreSQL rejects when the column is an ENUM type.

-- Step 1: Drop partial indexes that reference ENUM columns in WHERE clauses
DROP INDEX IF EXISTS idx_products_active;
DROP INDEX IF EXISTS idx_products_segment;
DROP INDEX IF EXISTS idx_user_aff_active;

-- Step 2: Drop defaults that reference ENUM literals
ALTER TABLE products ALTER COLUMN product_type DROP DEFAULT;
ALTER TABLE products ALTER COLUMN target_segment DROP DEFAULT;
ALTER TABLE products ALTER COLUMN status DROP DEFAULT;

-- Step 3: Convert ENUM columns to VARCHAR
ALTER TABLE products ALTER COLUMN product_type TYPE VARCHAR(50) USING product_type::text;
ALTER TABLE products ALTER COLUMN target_segment TYPE VARCHAR(50) USING target_segment::text;
ALTER TABLE products ALTER COLUMN status TYPE VARCHAR(20) USING status::text;
ALTER TABLE fees ALTER COLUMN calculation_type TYPE VARCHAR(20) USING calculation_type::text;
ALTER TABLE fees ALTER COLUMN timing TYPE VARCHAR(30) USING timing::text;
ALTER TABLE eligibility_rules ALTER COLUMN rule_type TYPE VARCHAR(30) USING rule_type::text;
ALTER TABLE eligibility_rules ALTER COLUMN operator TYPE VARCHAR(20) USING operator::text;

-- Step 4: Restore defaults as plain strings
ALTER TABLE products ALTER COLUMN target_segment SET DEFAULT 'INDIVIDUAL';
ALTER TABLE products ALTER COLUMN status SET DEFAULT 'DRAFT';

-- Step 5: Recreate partial indexes with VARCHAR comparisons
CREATE INDEX idx_products_active ON products(tenant_id) WHERE status = 'ACTIVE' AND deleted_at IS NULL;
CREATE INDEX idx_products_segment ON products(tenant_id, target_segment) WHERE status = 'ACTIVE';

-- Step 6: Drop the ENUM types now that no columns reference them
DROP TYPE IF EXISTS product_type;
DROP TYPE IF EXISTS product_status;
DROP TYPE IF EXISTS target_segment;
DROP TYPE IF EXISTS fee_calculation_type;
DROP TYPE IF EXISTS fee_timing;
DROP TYPE IF EXISTS rule_type;
DROP TYPE IF EXISTS rule_operator;

-- V16: Fix unique constraint on customer_block_codes
-- The previous constraint UNIQUE (customer_id, block_code_id, is_active) prevented 
-- unblocking a code if it had been unblocked before (duplicate is_active=false rows).
-- We now replace it with a partial unique index that only enforces uniqueness for ACTIVE blocks.

ALTER TABLE customer_block_codes DROP CONSTRAINT IF EXISTS customer_block_codes_customer_id_block_code_id_is_active_key;

-- Drop any existing indices that might conflict or be redundant
DROP INDEX IF EXISTS idx_customer_block_codes_active_unique;

-- Create a partial unique index: A customer can have a specific block code only ONCE as ACTIVE.
-- They can have multiple INACTIVE rows for the same block code (history).
CREATE UNIQUE INDEX idx_customer_block_codes_active_unique 
ON customer_block_codes (customer_id, block_code_id) 
WHERE is_active = TRUE;

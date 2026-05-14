-- V13: Add missing columns to block_codes table
ALTER TABLE block_codes ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE block_codes ADD COLUMN IF NOT EXISTS type VARCHAR(20);
ALTER TABLE block_codes ADD COLUMN IF NOT EXISTS category VARCHAR(50);

-- Update existing rows if any (though there shouldn't be yet)
UPDATE block_codes SET type = 'HARD_BLOCK' WHERE type IS NULL;
UPDATE block_codes SET category = 'FRAUD' WHERE category IS NULL;
UPDATE block_codes SET tenant_id = '00000000-0000-0000-0000-000000000001' WHERE tenant_id IS NULL;

-- Make them not null after setting defaults
ALTER TABLE block_codes ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE block_codes ALTER COLUMN type SET NOT NULL;

-- Fix assigned_by type in customer_block_codes
ALTER TABLE customer_block_codes ALTER COLUMN assigned_by TYPE UUID USING assigned_by::UUID;

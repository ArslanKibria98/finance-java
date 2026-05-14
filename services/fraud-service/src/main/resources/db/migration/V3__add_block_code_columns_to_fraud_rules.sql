-- Migration to add block_code support to fraud rules
ALTER TABLE fraud_rules ADD COLUMN IF NOT EXISTS block_code_id UUID;
ALTER TABLE fraud_rules ADD COLUMN IF NOT EXISTS block_code VARCHAR(50);

COMMENT ON COLUMN fraud_rules.block_code_id IS 'Linked block code ID from risk-service registry';
COMMENT ON COLUMN fraud_rules.block_code IS 'Human readable block code (e.g., FRAUD001)';

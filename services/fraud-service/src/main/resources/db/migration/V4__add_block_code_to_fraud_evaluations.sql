-- Migration to add block_code support to fraud evaluations
ALTER TABLE fraud_evaluations ADD COLUMN IF NOT EXISTS block_code_id UUID;
ALTER TABLE fraud_evaluations ADD COLUMN IF NOT EXISTS block_code VARCHAR(50);

COMMENT ON COLUMN fraud_evaluations.block_code_id IS 'Linked block code ID for the overall decision';
COMMENT ON COLUMN fraud_evaluations.block_code IS 'Human readable block code for the overall decision';

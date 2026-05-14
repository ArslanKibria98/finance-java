-- V20: Add block_code_id (UUID FK) to blacklist and device_registry tables
-- This enables standardized block code assignment when creating/updating entries

-- NID Blacklist
ALTER TABLE nid_blacklist
    ADD COLUMN block_code_id UUID REFERENCES block_codes(id);
COMMENT ON COLUMN nid_blacklist.block_code_id IS 'FK to block_codes for standardized reason code';

-- Mobile Blacklist
ALTER TABLE mobile_blacklist
    ADD COLUMN block_code_id UUID REFERENCES block_codes(id);
COMMENT ON COLUMN mobile_blacklist.block_code_id IS 'FK to block_codes for standardized reason code';

-- Device Registry (already has block_code VARCHAR from V19, now add FK reference)
ALTER TABLE device_registry
    ADD COLUMN block_code_id UUID REFERENCES block_codes(id);
COMMENT ON COLUMN device_registry.block_code_id IS 'FK to block_codes for standardized reason code';

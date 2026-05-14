-- V19: Add block_code to device_registry
-- For standardized error reporting in blocked devices list

ALTER TABLE device_registry
    ADD COLUMN block_code VARCHAR(50);

COMMENT ON COLUMN device_registry.block_code IS 'Standardized block code (e.g., FRAUD002, VEL001)';

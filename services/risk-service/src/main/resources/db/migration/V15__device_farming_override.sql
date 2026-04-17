-- V15: Add nid_farming_override column to device_registry
-- Allows admins to explicitly override identity-farming blocks
-- without deleting audit history (NID association rows)

ALTER TABLE device_registry
    ADD COLUMN nid_farming_override BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN device_registry.nid_farming_override IS
    'Admin override for identity-farming block (nid_count > 3). TRUE = exempt from farming detection.';

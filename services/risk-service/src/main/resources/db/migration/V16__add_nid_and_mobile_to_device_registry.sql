-- V16: Add nid and mobile_number to device_registry
-- For administrative visibility and traceability

ALTER TABLE device_registry
    ADD COLUMN nid VARCHAR(255),
    ADD COLUMN mobile_number VARCHAR(20);

COMMENT ON COLUMN device_registry.nid IS 'Actual National ID (NID) of the customer';
COMMENT ON COLUMN device_registry.mobile_number IS 'Actual Mobile Number of the customer';

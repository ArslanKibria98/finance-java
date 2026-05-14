CREATE TABLE internal_check_configs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    check_name VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    block_code_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_internal_check_block_code FOREIGN KEY (block_code_id) REFERENCES block_codes(id),
    CONSTRAINT uk_internal_check_name_tenant UNIQUE (tenant_id, check_name)
);

-- Seed initial 10 checks
INSERT INTO internal_check_configs (id, tenant_id, check_name, display_name, description, active, block_code_id, created_at, updated_at)
VALUES 
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'NID_FORMAT', 'NID Format Validation', 'Validates the structure of the National ID', true, NULL, now(), now()),
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'BLACKLIST_WATCHLIST', 'Blacklist/Watchlist Check', 'Checks against internal blacklist and PEP/Sanction watchlist', true, NULL, now(), now()),
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'FRAUD_HISTORY', 'Fraud History Check', 'Checks previous fraud incidents in the system', true, NULL, now(), now()),
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'DEVICE_FINGERPRINT', 'Device Fingerprint Check', 'Analyzes device reputation and associations', true, NULL, now(), now()),
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'ACCOUNT_LOCK', 'Account Lock History', 'Checks for existing administrative locks', true, NULL, now(), now()),
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'INTERNAL_SANCTIONS', 'Internal Sanctions Check', 'Checks against internal sanctions database', true, NULL, now(), now()),
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'VELOCITY_CHECK', 'Velocity Anomaly Check', 'Checks for rapid repetitive requests', true, NULL, now(), now()),
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'RISK_SCORE', 'Aggregate Risk Scoring', 'Calculates and validates total risk score', true, NULL, now(), now()),
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'CIF_LOOKUP', 'CIF Registry Check', 'Checks existence and status in Core Banking', true, NULL, now(), now()),
(gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'DUPLICATE_MOBILE', 'Duplicate Mobile Check', 'Checks if mobile number is already used by another NID', true, NULL, now(), now());

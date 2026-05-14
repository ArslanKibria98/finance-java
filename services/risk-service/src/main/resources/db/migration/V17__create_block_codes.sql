-- V17__create_block_codes.sql
-- Implementation of Block Code Registry as per Admin Portal UI

CREATE TYPE block_code_type AS ENUM ('COMPLIANCE', 'AML', 'ANTI_FRAUD', 'SANCTION');

CREATE TABLE block_codes (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    code            VARCHAR(20) NOT NULL,
    description     TEXT,
    type            block_code_type NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_block_code_tenant UNIQUE (tenant_id, code)
);

CREATE INDEX idx_block_codes_tenant ON block_codes(tenant_id);
CREATE INDEX idx_block_codes_active ON block_codes(tenant_id) WHERE is_active = TRUE;

-- Link Block Codes to Fraud Rules
ALTER TABLE fraud_rules ADD COLUMN block_code_id UUID REFERENCES block_codes(id);

-- Persist triggering block code in assessments for audit
ALTER TABLE risk_assessments ADD COLUMN block_code VARCHAR(20);

-- Seed some initial standard codes
INSERT INTO block_codes (tenant_id, code, description, type, is_active) VALUES
('00000000-0000-0000-0000-000000000001', 'AML001', 'Money Laundering Indicator - Transaction Pattern', 'AML', TRUE),
('00000000-0000-0000-0000-000000000001', 'COMP001', 'Compliance Review Required - Document Mismatch', 'COMPLIANCE', TRUE),
('00000000-0000-0000-0000-000000000001', 'FRAUD001', 'High Probability of Identity Theft', 'ANTI_FRAUD', TRUE),
('00000000-0000-0000-0000-000000000001', 'SANCT001', 'Sanctions Match - Block Transaction', 'SANCTION', TRUE);

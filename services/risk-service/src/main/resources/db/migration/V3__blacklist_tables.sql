-- ============================================================================
-- NID BLACKLIST & MOBILE BLACKLIST TABLES
-- Simple admin-managed blacklist for blocking national IDs and mobile numbers
-- ============================================================================

CREATE TABLE nid_blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    national_id     VARCHAR(20) NOT NULL,
    reason          TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'BLACKLISTED',
    added_by        VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_nid_blacklist UNIQUE (national_id)
);

CREATE TABLE mobile_blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mobile_number   VARCHAR(20) NOT NULL,
    reason          TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'BLACKLISTED',
    added_by        VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_mobile_blacklist UNIQUE (mobile_number)
);

CREATE INDEX idx_nid_blacklist_status ON nid_blacklist(national_id, status);
CREATE INDEX idx_mobile_blacklist_status ON mobile_blacklist(mobile_number, status);

COMMENT ON TABLE nid_blacklist IS 'Admin-managed NID blacklist — blocks national IDs from registration';
COMMENT ON TABLE mobile_blacklist IS 'Admin-managed mobile blacklist — blocks mobile numbers from registration';

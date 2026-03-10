-- ============================================================================
-- GLOBAL PROFILE INDEX DATABASE SCHEMA
-- PostgreSQL 16+ Production Schema
-- Multi-Jurisdictional Customer Orchestration
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE customer_type AS ENUM (
    'INDIVIDUAL',
    'BUSINESS'
);

CREATE TYPE kyc_status AS ENUM (
    'NOT_STARTED',
    'IN_PROGRESS',
    'PENDING_REVIEW',
    'VERIFIED',
    'EXPIRED',
    'REJECTED',
    'BLOCKED'
);

CREATE TYPE global_kyc_aggregate_status AS ENUM (
    'NONE',
    'PARTIAL',
    'VERIFIED',
    'EXPIRED',
    'BLOCKED'
);

CREATE TYPE signatory_role AS ENUM (
    'CEO',
    'CFO',
    'DIRECTOR',
    'MANAGER',
    'UBO',
    'AUTHORIZED_SIGNATORY'
);

CREATE TYPE signatory_authority AS ENUM (
    'SOLE',
    'JOINT'
);

-- ============================================================================
-- GLOBAL CUSTOMERS (INDIVIDUAL)
-- ============================================================================

CREATE TABLE global_customers (
    global_uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_type customer_type NOT NULL DEFAULT 'INDIVIDUAL',
    global_email_hash VARCHAR(64) NOT NULL,
    global_mobile_hash VARCHAR(64) NOT NULL,
    primary_country_code VARCHAR(3) NOT NULL,
    global_kyc_status global_kyc_aggregate_status NOT NULL DEFAULT 'NONE',
    kyc_last_verified_at TIMESTAMPTZ,
    global_risk_grade VARCHAR(10),
    global_risk_updated_at TIMESTAMPTZ,
    pep_flag BOOLEAN NOT NULL DEFAULT FALSE,
    sanctions_flag BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_flag BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    blocked_at TIMESTAMPTZ,
    blocked_reason VARCHAR(500),
    acquisition_source VARCHAR(100),
    customer_segment VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_global_email UNIQUE (global_email_hash),
    CONSTRAINT uq_global_mobile UNIQUE (global_mobile_hash),
    CHECK (customer_type = 'INDIVIDUAL')
);

CREATE INDEX idx_global_customers_primary_country ON global_customers(primary_country_code);
CREATE INDEX idx_global_customers_kyc_status ON global_customers(global_kyc_status) WHERE is_active = TRUE;
CREATE INDEX idx_global_customers_risk ON global_customers(global_risk_grade) WHERE is_active = TRUE;
CREATE INDEX idx_global_customers_flags ON global_customers(global_uid)
    WHERE pep_flag = TRUE OR sanctions_flag = TRUE OR fraud_flag = TRUE;
CREATE INDEX idx_global_customers_active ON global_customers(is_active) WHERE deleted_at IS NULL;

-- ============================================================================
-- REGIONAL PROFILES
-- ============================================================================

CREATE TABLE regional_profiles (
    regional_profile_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    global_uid UUID NOT NULL REFERENCES global_customers(global_uid) ON DELETE CASCADE,
    country_code VARCHAR(3) NOT NULL,
    regional_cif_number VARCHAR(20) NOT NULL,
    regional_kyc_status kyc_status NOT NULL DEFAULT 'NOT_STARTED',
    kyc_verified_at TIMESTAMPTZ,
    kyc_expiry_date DATE,
    kyc_provider VARCHAR(50),
    pii_vault_region VARCHAR(10) NOT NULL,
    pii_vault_record_id UUID NOT NULL,
    regional_risk_grade VARCHAR(10),
    risk_grade_updated_at TIMESTAMPTZ,
    regional_pep_flag BOOLEAN NOT NULL DEFAULT FALSE,
    regional_sanctions_flag BOOLEAN NOT NULL DEFAULT FALSE,
    keycloak_user_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    activation_date DATE,
    deactivation_date DATE,
    deactivation_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_regional_cif UNIQUE (country_code, regional_cif_number),
    CONSTRAINT uq_global_uid_country UNIQUE (global_uid, country_code),
    CHECK (pii_vault_region = country_code)
);

CREATE INDEX idx_regional_profiles_global_uid ON regional_profiles(global_uid);
CREATE INDEX idx_regional_profiles_country ON regional_profiles(country_code) WHERE is_active = TRUE;
CREATE INDEX idx_regional_profiles_kyc ON regional_profiles(regional_kyc_status);
CREATE INDEX idx_regional_profiles_keycloak ON regional_profiles(keycloak_user_id) WHERE keycloak_user_id IS NOT NULL;
CREATE INDEX idx_regional_profiles_cif ON regional_profiles(regional_cif_number);

-- ============================================================================
-- REGIONAL PROFILE HISTORY
-- ============================================================================

CREATE TABLE regional_profile_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    regional_profile_id UUID NOT NULL REFERENCES regional_profiles(regional_profile_id),
    field_changed VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT NOT NULL,
    changed_by UUID,
    change_reason VARCHAR(500),
    changed_via VARCHAR(50),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_history_profile ON regional_profile_history(regional_profile_id);
CREATE INDEX idx_profile_history_timestamp ON regional_profile_history(changed_at);

-- ============================================================================
-- PII ACCESS TOKENS
-- ============================================================================

CREATE TABLE pii_access_tokens (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    access_token TEXT NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    global_uid UUID NOT NULL REFERENCES global_customers(global_uid),
    regional_profile_id UUID REFERENCES regional_profiles(regional_profile_id),
    pii_vault_region VARCHAR(10) NOT NULL,
    allowed_fields VARCHAR(100)[] NOT NULL,
    requester_id UUID NOT NULL,
    requester_role VARCHAR(50) NOT NULL,
    requester_ip VARCHAR(45),
    access_purpose VARCHAR(100) NOT NULL,
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMPTZ,
    revoked_reason VARCHAR(500),
    used_count INT NOT NULL DEFAULT 0,
    last_used_at TIMESTAMPTZ,
    CHECK (expires_at > issued_at),
    CHECK (EXTRACT(EPOCH FROM (expires_at - issued_at)) <= 600)
);

CREATE INDEX idx_pii_tokens_global_uid ON pii_access_tokens(global_uid);
CREATE INDEX idx_pii_tokens_active ON pii_access_tokens(expires_at) WHERE is_revoked = FALSE;
CREATE INDEX idx_pii_tokens_requester ON pii_access_tokens(requester_id);
CREATE INDEX idx_pii_tokens_hash ON pii_access_tokens(token_hash);

-- ============================================================================
-- GLOBAL BUSINESS CUSTOMERS (SME)
-- ============================================================================

CREATE TABLE global_business_customers (
    global_business_uid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_type customer_type NOT NULL DEFAULT 'BUSINESS',
    company_name_hash VARCHAR(64) NOT NULL,
    primary_country_code VARCHAR(3) NOT NULL,
    global_kyb_status global_kyc_aggregate_status NOT NULL DEFAULT 'NONE',
    kyb_last_verified_at TIMESTAMPTZ,
    global_risk_grade VARCHAR(10),
    global_risk_updated_at TIMESTAMPTZ,
    sanctions_flag BOOLEAN NOT NULL DEFAULT FALSE,
    fraud_flag BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    blocked_at TIMESTAMPTZ,
    blocked_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_global_business_name UNIQUE (company_name_hash, primary_country_code),
    CHECK (customer_type = 'BUSINESS')
);

CREATE INDEX idx_global_business_primary_country ON global_business_customers(primary_country_code);
CREATE INDEX idx_global_business_kyb_status ON global_business_customers(global_kyb_status) WHERE is_active = TRUE;

-- ============================================================================
-- REGIONAL BUSINESS PROFILES
-- ============================================================================

CREATE TABLE regional_business_profiles (
    regional_business_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    global_business_uid UUID NOT NULL
        REFERENCES global_business_customers(global_business_uid) ON DELETE CASCADE,
    country_code VARCHAR(3) NOT NULL,
    regional_bif_number VARCHAR(20) NOT NULL,
    regional_kyb_status kyc_status NOT NULL DEFAULT 'NOT_STARTED',
    kyb_verified_at TIMESTAMPTZ,
    pii_vault_region VARCHAR(10) NOT NULL,
    pii_vault_record_id UUID NOT NULL,
    regional_risk_grade VARCHAR(10),
    sector_risk VARCHAR(20),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_regional_bif UNIQUE (country_code, regional_bif_number),
    CONSTRAINT uq_global_business_country UNIQUE (global_business_uid, country_code),
    CHECK (pii_vault_region = country_code)
);

CREATE INDEX idx_regional_business_global_uid ON regional_business_profiles(global_business_uid);
CREATE INDEX idx_regional_business_country ON regional_business_profiles(country_code);
CREATE INDEX idx_regional_business_bif ON regional_business_profiles(regional_bif_number);

-- ============================================================================
-- BUSINESS AUTHORIZED SIGNATORIES
-- ============================================================================

CREATE TABLE business_authorized_signatories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    global_business_uid UUID NOT NULL
        REFERENCES global_business_customers(global_business_uid) ON DELETE CASCADE,
    individual_global_uid UUID NOT NULL
        REFERENCES global_customers(global_uid) ON DELETE CASCADE,
    role signatory_role NOT NULL,
    is_ubo BOOLEAN NOT NULL DEFAULT FALSE,
    ownership_percentage NUMERIC(5, 2),
    signing_authority signatory_authority NOT NULL,
    signing_limit_usd NUMERIC(19, 4),
    valid_from DATE NOT NULL,
    valid_until DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_business_signatory UNIQUE (global_business_uid, individual_global_uid),
    CHECK (
        (is_ubo = TRUE AND ownership_percentage IS NOT NULL AND ownership_percentage > 0) OR
        (is_ubo = FALSE)
    )
);

CREATE INDEX idx_signatories_business ON business_authorized_signatories(global_business_uid);
CREATE INDEX idx_signatories_individual ON business_authorized_signatories(individual_global_uid);
CREATE INDEX idx_signatories_ubo ON business_authorized_signatories(global_business_uid)
    WHERE is_ubo = TRUE AND is_active = TRUE;

-- ============================================================================
-- GLOBAL CUSTOMER EVENTS (Event Sourcing / Outbox Pattern)
-- ============================================================================

CREATE TABLE global_customer_events (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INT NOT NULL DEFAULT 1,
    event_payload JSONB NOT NULL,
    correlation_id UUID,
    causation_id UUID,
    user_id UUID,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (aggregate_type IN ('GLOBAL_CUSTOMER', 'REGIONAL_PROFILE', 'BUSINESS_CUSTOMER', 'SIGNATORY'))
);

CREATE INDEX idx_events_aggregate ON global_customer_events(aggregate_type, aggregate_id);
CREATE INDEX idx_events_type ON global_customer_events(event_type);
CREATE INDEX idx_events_occurred ON global_customer_events(occurred_at DESC);
CREATE INDEX idx_events_unpublished ON global_customer_events(occurred_at) WHERE published = FALSE;

-- ============================================================================
-- 360-DEGREE VIEW (MATERIALIZED VIEW)
-- ============================================================================

CREATE MATERIALIZED VIEW customer_360_view AS
SELECT
    gc.global_uid,
    gc.customer_type,
    gc.primary_country_code,
    gc.global_kyc_status,
    gc.global_risk_grade,
    gc.pep_flag,
    gc.sanctions_flag,
    gc.fraud_flag,
    gc.is_active,
    jsonb_agg(
        jsonb_build_object(
            'country_code', rp.country_code,
            'cif_number', rp.regional_cif_number,
            'kyc_status', rp.regional_kyc_status,
            'risk_grade', rp.regional_risk_grade,
            'is_active', rp.is_active,
            'created_at', rp.created_at
        )
        ORDER BY rp.created_at
    ) FILTER (WHERE rp.regional_profile_id IS NOT NULL) AS regional_profiles,
    COUNT(rp.regional_profile_id) FILTER (WHERE rp.is_active = TRUE) AS active_regions_count,
    COUNT(rp.regional_profile_id) FILTER (WHERE rp.regional_kyc_status = 'VERIFIED') AS verified_regions_count,
    gc.created_at AS customer_created_at,
    gc.updated_at AS customer_updated_at
FROM global_customers gc
LEFT JOIN regional_profiles rp ON gc.global_uid = rp.global_uid AND rp.deleted_at IS NULL
WHERE gc.deleted_at IS NULL
GROUP BY gc.global_uid;

CREATE UNIQUE INDEX idx_360_view_global_uid ON customer_360_view(global_uid);
CREATE INDEX idx_360_view_primary_country ON customer_360_view(primary_country_code);
CREATE INDEX idx_360_view_kyc_status ON customer_360_view(global_kyc_status);

-- ============================================================================
-- TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_global_customers_updated
    BEFORE UPDATE ON global_customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_regional_profiles_updated
    BEFORE UPDATE ON regional_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_global_business_updated
    BEFORE UPDATE ON global_business_customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_regional_business_updated
    BEFORE UPDATE ON regional_business_profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Auto-compute Global KYC Status from Regional Profiles
CREATE OR REPLACE FUNCTION compute_global_kyc_status()
RETURNS TRIGGER AS $$
DECLARE
    v_verified_count INT;
    v_blocked_count INT;
    v_expired_count INT;
    v_total_count INT;
BEGIN
    SELECT
        COUNT(*) FILTER (WHERE regional_kyc_status = 'VERIFIED') AS verified,
        COUNT(*) FILTER (WHERE regional_kyc_status = 'BLOCKED') AS blocked,
        COUNT(*) FILTER (WHERE regional_kyc_status = 'EXPIRED') AS expired,
        COUNT(*) AS total
    INTO v_verified_count, v_blocked_count, v_expired_count, v_total_count
    FROM regional_profiles
    WHERE global_uid = NEW.global_uid AND is_active = TRUE;

    IF v_blocked_count > 0 THEN
        UPDATE global_customers SET global_kyc_status = 'BLOCKED' WHERE global_uid = NEW.global_uid;
    ELSIF v_expired_count > 0 THEN
        UPDATE global_customers SET global_kyc_status = 'EXPIRED' WHERE global_uid = NEW.global_uid;
    ELSIF v_verified_count = v_total_count AND v_total_count > 0 THEN
        UPDATE global_customers SET global_kyc_status = 'VERIFIED' WHERE global_uid = NEW.global_uid;
    ELSIF v_verified_count > 0 THEN
        UPDATE global_customers SET global_kyc_status = 'PARTIAL' WHERE global_uid = NEW.global_uid;
    ELSE
        UPDATE global_customers SET global_kyc_status = 'NONE' WHERE global_uid = NEW.global_uid;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_compute_global_kyc
    AFTER INSERT OR UPDATE OF regional_kyc_status ON regional_profiles
    FOR EACH ROW EXECUTE FUNCTION compute_global_kyc_status();

-- Refresh materialized view function
CREATE OR REPLACE FUNCTION refresh_customer_360_view()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY customer_360_view;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- HELPER FUNCTIONS
-- ============================================================================

CREATE OR REPLACE FUNCTION create_global_customer(
    p_email VARCHAR,
    p_mobile VARCHAR,
    p_primary_country VARCHAR
) RETURNS UUID AS $$
DECLARE
    v_global_uid UUID;
    v_email_hash VARCHAR;
    v_mobile_hash VARCHAR;
BEGIN
    v_email_hash := encode(sha256(lower(p_email)::bytea), 'hex');
    v_mobile_hash := encode(sha256(p_mobile::bytea), 'hex');

    INSERT INTO global_customers (
        global_email_hash,
        global_mobile_hash,
        primary_country_code
    ) VALUES (
        v_email_hash,
        v_mobile_hash,
        p_primary_country
    ) RETURNING global_uid INTO v_global_uid;

    INSERT INTO global_customer_events (
        aggregate_type,
        aggregate_id,
        event_type,
        event_payload
    ) VALUES (
        'GLOBAL_CUSTOMER',
        v_global_uid,
        'GlobalCustomerCreated',
        jsonb_build_object(
            'global_uid', v_global_uid,
            'email_hash', v_email_hash,
            'mobile_hash', v_mobile_hash,
            'primary_country', p_primary_country
        )
    );

    RETURN v_global_uid;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION find_global_customer_by_email(p_email VARCHAR)
RETURNS UUID AS $$
DECLARE
    v_global_uid UUID;
BEGIN
    SELECT global_uid INTO v_global_uid
    FROM global_customers
    WHERE global_email_hash = encode(sha256(lower(p_email)::bytea), 'hex')
    AND deleted_at IS NULL
    LIMIT 1;

    RETURN v_global_uid;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE global_customers IS 'Global customer orchestration - stores ONLY hashes, NO raw PII';
COMMENT ON TABLE regional_profiles IS 'Regional CIF linking with PII vault references';
COMMENT ON TABLE pii_access_tokens IS 'Time-limited tokens for PII vault access (max 10 min)';
COMMENT ON TABLE business_authorized_signatories IS 'UBO and signatory linking for SME';
COMMENT ON MATERIALIZED VIEW customer_360_view IS 'Aggregated 360-degree view (refresh every 5 min)';
COMMENT ON COLUMN global_customers.global_uid IS 'Anonymized global customer identifier';
COMMENT ON COLUMN global_customers.global_email_hash IS 'SHA-256 hash for deduplication (NOT the actual email)';
COMMENT ON COLUMN regional_profiles.pii_vault_record_id IS 'Logical pointer to PII vault record in regional database';
COMMENT ON COLUMN pii_access_tokens.expires_at IS 'Token expiry - max 10 minutes from issuance';

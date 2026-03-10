-- ============================================================================
-- KYC ADAPTER DATABASE SCHEMA
-- PostgreSQL 16+ Production Schema
-- KSA Islamic Financing Platform
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE kyc_provider AS ENUM (
    'YAKEEN',
    'NAFATH',
    'ABSHER',
    'NIC',
    'UAE_PASS',
    'UAE_ICA',
    'NADRA',
    'MANUAL'
);

CREATE TYPE verification_type AS ENUM (
    'NATIONAL_ID',
    'IQAMA',
    'COMMERCIAL_REGISTRATION',
    'PASSPORT',
    'LIVENESS',
    'ADDRESS',
    'EMPLOYMENT'
);

CREATE TYPE session_status AS ENUM (
    'INITIATED',
    'PENDING_USER_ACTION',
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED',
    'EXPIRED',
    'CANCELLED'
);

CREATE TYPE verification_result AS ENUM (
    'VERIFIED',
    'NOT_VERIFIED',
    'PARTIAL_MATCH',
    'DATA_MISMATCH',
    'EXPIRED_DOCUMENT',
    'PROVIDER_ERROR'
);

-- ============================================================================
-- VERIFICATION TABLES
-- ============================================================================

CREATE TABLE verification_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    session_number VARCHAR(50) NOT NULL,
    customer_id UUID,
    global_uid UUID,
    loan_application_id UUID,
    country_code VARCHAR(3) NOT NULL DEFAULT 'SAU',
    verification_type verification_type NOT NULL,
    provider kyc_provider NOT NULL,
    national_id VARCHAR(20),
    iqama_number VARCHAR(20),
    commercial_registration VARCHAR(50),
    date_of_birth DATE,
    full_name_ar VARCHAR(255),
    full_name_en VARCHAR(255),
    status session_status NOT NULL DEFAULT 'INITIATED',
    provider_session_id VARCHAR(100),
    provider_request_id VARCHAR(100),
    result verification_result,
    confidence_score NUMERIC(5, 4),
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_action_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    attempt_count INT NOT NULL DEFAULT 1,
    max_attempts INT NOT NULL DEFAULT 3,
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_session_number UNIQUE (tenant_id, session_number),
    CONSTRAINT uq_session_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE session_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    session_id UUID NOT NULL REFERENCES verification_sessions(id),
    from_status session_status,
    to_status session_status NOT NULL,
    changed_by UUID,
    change_reason VARCHAR(500),
    provider_callback JSONB,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE provider_response_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    cache_key VARCHAR(100) NOT NULL,
    provider kyc_provider NOT NULL,
    verification_type verification_type NOT NULL,
    subject_id VARCHAR(50) NOT NULL,
    subject_id_type VARCHAR(20) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    request_payload JSONB,
    response_payload JSONB NOT NULL,
    response_code VARCHAR(20),
    result verification_result NOT NULL,
    extracted_data JSONB,
    fetched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_until TIMESTAMPTZ NOT NULL,
    is_stale BOOLEAN NOT NULL DEFAULT FALSE,
    hit_count INT NOT NULL DEFAULT 0,
    last_hit_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_cache_key UNIQUE (tenant_id, cache_key)
);

CREATE TABLE identity_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    internal_customer_id UUID NOT NULL,
    external_id_type VARCHAR(20) NOT NULL,
    external_id_value VARCHAR(50) NOT NULL,
    issuing_country VARCHAR(3) NOT NULL DEFAULT 'SA',
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    verified_by_session_id UUID REFERENCES verification_sessions(id),
    verification_provider kyc_provider,
    document_expiry DATE,
    document_issue_date DATE,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_external_id UNIQUE (tenant_id, external_id_type, external_id_value)
);

CREATE TABLE provider_configuration (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    provider kyc_provider NOT NULL,
    base_url VARCHAR(500) NOT NULL,
    auth_endpoint VARCHAR(500),
    credentials_encrypted BYTEA NOT NULL,
    timeout_seconds INT NOT NULL DEFAULT 30,
    retry_attempts INT NOT NULL DEFAULT 3,
    cache_ttl_hours INT NOT NULL DEFAULT 24,
    rate_limit_per_minute INT,
    daily_quota INT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    priority INT NOT NULL DEFAULT 0,
    last_health_check_at TIMESTAMPTZ,
    health_status VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_provider_config UNIQUE (tenant_id, provider)
);

CREATE TABLE provider_api_logs (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    session_id UUID,
    provider kyc_provider NOT NULL,
    request_method VARCHAR(10) NOT NULL,
    request_endpoint VARCHAR(500) NOT NULL,
    request_headers JSONB,
    request_body JSONB,
    request_hash VARCHAR(64),
    response_status INT,
    response_headers JSONB,
    response_body JSONB,
    response_time_ms INT,
    error_code VARCHAR(50),
    error_message TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMPTZ,
    PRIMARY KEY (id, requested_at)
) PARTITION BY RANGE (requested_at);

CREATE TABLE provider_api_logs_2026_01 PARTITION OF provider_api_logs
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE provider_api_logs_2026_02 PARTITION OF provider_api_logs
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
CREATE TABLE provider_api_logs_2026_03 PARTITION OF provider_api_logs
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');
CREATE TABLE provider_api_logs_2026_04 PARTITION OF provider_api_logs
    FOR VALUES FROM ('2026-04-01') TO ('2026-05-01');
CREATE TABLE provider_api_logs_2026_05 PARTITION OF provider_api_logs
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');
CREATE TABLE provider_api_logs_2026_06 PARTITION OF provider_api_logs
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE change_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by UUID,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(100),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_sessions_customer ON verification_sessions(customer_id) WHERE customer_id IS NOT NULL;
CREATE INDEX idx_sessions_global_uid ON verification_sessions(global_uid) WHERE global_uid IS NOT NULL;
CREATE INDEX idx_sessions_application ON verification_sessions(loan_application_id) WHERE loan_application_id IS NOT NULL;
CREATE INDEX idx_sessions_status ON verification_sessions(tenant_id, status);
CREATE INDEX idx_sessions_provider ON verification_sessions(provider, status);
CREATE INDEX idx_sessions_country ON verification_sessions(country_code, status);

CREATE INDEX idx_cache_subject ON provider_response_cache(subject_id, subject_id_type);
CREATE INDEX idx_cache_valid ON provider_response_cache(tenant_id, valid_until) WHERE is_stale = FALSE;

CREATE INDEX idx_identity_customer ON identity_mapping(internal_customer_id);
CREATE INDEX idx_identity_external ON identity_mapping(external_id_value);

CREATE INDEX idx_api_logs_session ON provider_api_logs(session_id) WHERE session_id IS NOT NULL;

CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published = FALSE;

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE verification_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE provider_response_cache ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity_mapping ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON verification_sessions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON provider_response_cache
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON identity_mapping
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE verification_sessions IS 'KYC verification sessions with multi-provider support';
COMMENT ON COLUMN verification_sessions.global_uid IS 'Links to Global Profile Index for cross-border KYC coordination';
COMMENT ON COLUMN verification_sessions.country_code IS 'ISO 3166-1 alpha-3 country code determines KYC provider routing';
COMMENT ON TABLE provider_response_cache IS 'Cached provider responses with TTL for cost optimization';
COMMENT ON TABLE identity_mapping IS 'Maps external IDs (NID/Iqama/CR) to internal customer UUID';
COMMENT ON TABLE provider_api_logs IS 'Detailed API call logs for debugging and compliance (partitioned by month)';

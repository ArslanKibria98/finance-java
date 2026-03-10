-- ============================================================================
-- RISK SERVICE DATABASE SCHEMA
-- PostgreSQL 16+ Production Schema
-- KSA Islamic Financing Platform - Risk Service
-- ============================================================================
-- Fraud detection, AML/CFT screening, scoring models, signal logging,
-- device registry, account locks, and customer view.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE risk_assessment_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'COMPLETED',
    'FAILED'
);

CREATE TYPE screening_status AS ENUM (
    'PENDING',
    'CLEAR',
    'POTENTIAL_MATCH',
    'CONFIRMED_MATCH',
    'FALSE_POSITIVE',
    'ESCALATED'
);

CREATE TYPE fraud_action AS ENUM (
    'APPROVE',
    'REVIEW',
    'DECLINE',
    'BLOCK'
);

CREATE TYPE watchlist_type AS ENUM (
    'SANCTIONS',
    'PEP',
    'ADVERSE_MEDIA',
    'INTERNAL_BLACKLIST',
    'INTERNAL_GREYLIST'
);

CREATE TYPE signal_severity AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
);

-- ============================================================================
-- RISK ASSESSMENT TABLES
-- ============================================================================

CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    assessment_number VARCHAR(50) NOT NULL,
    customer_id UUID NOT NULL,
    loan_application_id UUID,
    assessment_type VARCHAR(50) NOT NULL,

    overall_risk_score INT NOT NULL,
    risk_grade VARCHAR(10) NOT NULL,

    fraud_score INT,
    aml_score INT,
    credit_risk_score INT,

    recommended_action fraud_action NOT NULL,

    status risk_assessment_status NOT NULL DEFAULT 'PENDING',

    assessment_data JSONB NOT NULL,

    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_assessment UNIQUE (tenant_id, assessment_number)
);

-- ============================================================================
-- SCORING MODEL RESULTS
-- ============================================================================

CREATE TABLE scoring_model_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    risk_assessment_id UUID NOT NULL REFERENCES risk_assessments(id),

    model_name VARCHAR(100) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    model_type VARCHAR(50) NOT NULL,

    input_features JSONB NOT NULL,

    raw_score NUMERIC(10, 6) NOT NULL,
    normalized_score INT NOT NULL,

    threshold_applied INT NOT NULL,
    decision fraud_action NOT NULL,

    feature_importance JSONB,
    explanation TEXT,

    execution_time_ms INT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- FRAUD SIGNAL LOGS
-- ============================================================================

CREATE TABLE fraud_signal_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    risk_assessment_id UUID REFERENCES risk_assessments(id),
    customer_id UUID NOT NULL,

    signal_type VARCHAR(100) NOT NULL,
    signal_source VARCHAR(50) NOT NULL,
    severity signal_severity NOT NULL,

    signal_value VARCHAR(500) NOT NULL,
    signal_details JSONB,

    weight NUMERIC(5, 4) NOT NULL,
    score_contribution INT NOT NULL,

    is_triggered BOOLEAN NOT NULL,

    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- SCREENING RESULTS (AML/Sanctions)
-- ============================================================================

CREATE TABLE screening_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    risk_assessment_id UUID REFERENCES risk_assessments(id),
    customer_id UUID NOT NULL,

    screening_type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,

    search_terms JSONB NOT NULL,

    status screening_status NOT NULL DEFAULT 'PENDING',

    match_count INT NOT NULL DEFAULT 0,
    matches JSONB,

    highest_match_score NUMERIC(5, 4),

    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    review_notes TEXT,

    screened_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

-- ============================================================================
-- WATCHLIST ENTRIES
-- ============================================================================

CREATE TABLE watchlist_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    list_type watchlist_type NOT NULL,

    identifier_type VARCHAR(50) NOT NULL,
    identifier_value VARCHAR(255) NOT NULL,

    name VARCHAR(255),
    reason TEXT,

    source VARCHAR(100),
    source_reference VARCHAR(100),

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    added_by UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_watchlist UNIQUE (tenant_id, list_type, identifier_type, identifier_value)
);

-- ============================================================================
-- VELOCITY CHECKS
-- ============================================================================

CREATE TABLE velocity_checks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    customer_id UUID NOT NULL,
    check_type VARCHAR(50) NOT NULL,

    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,

    count_in_window INT NOT NULL,
    sum_in_window NUMERIC(20, 6),

    threshold_count INT,
    threshold_sum NUMERIC(20, 6),

    is_exceeded BOOLEAN NOT NULL,

    checked_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- DEVICE REGISTRY (for Check 6: Device Fingerprint)
-- ============================================================================

CREATE TABLE device_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    device_id VARCHAR(255) NOT NULL,
    device_fingerprint VARCHAR(512),
    nid_hash VARCHAR(255) NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    block_reason TEXT,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    attempt_count INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- ACCOUNT LOCKS (for Check 8: Account Lock History)
-- ============================================================================

CREATE TABLE account_locks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nid_hash VARCHAR(255) NOT NULL,
    lock_type VARCHAR(50) NOT NULL,
    lock_reason TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    locked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unlocked_at TIMESTAMPTZ,
    locked_by VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- CUSTOMERS VIEW (read-only stub for CIF/mobile lookups)
-- In production: read-replica or materialized view from customer-service DB
-- ============================================================================

CREATE TABLE customers_view (
    id UUID PRIMARY KEY,
    nid_hash VARCHAR(255) NOT NULL,
    mobile_hash VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- CHANGE LOG & OUTBOX
-- ============================================================================

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

CREATE INDEX idx_assessments_customer ON risk_assessments(customer_id);
CREATE INDEX idx_assessments_application ON risk_assessments(loan_application_id) WHERE loan_application_id IS NOT NULL;
CREATE INDEX idx_scoring_assessment ON scoring_model_results(risk_assessment_id);
CREATE INDEX idx_signals_customer ON fraud_signal_logs(customer_id);
CREATE INDEX idx_signals_triggered ON fraud_signal_logs(tenant_id) WHERE is_triggered = TRUE;
CREATE INDEX idx_screening_customer ON screening_results(customer_id);
CREATE INDEX idx_screening_pending ON screening_results(tenant_id) WHERE status = 'POTENTIAL_MATCH';
CREATE INDEX idx_watchlist_active ON watchlist_entries(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_watchlist_lookup ON watchlist_entries(list_type, identifier_type, identifier_value) WHERE is_active = TRUE;
CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published = FALSE;

-- Device registry indexes
CREATE INDEX idx_device_registry_device_id ON device_registry(device_id);
CREATE INDEX idx_device_registry_nid_hash ON device_registry(nid_hash);
CREATE UNIQUE INDEX idx_device_nid_unique ON device_registry(device_id, nid_hash);

-- Account locks indexes
CREATE INDEX idx_account_locks_nid ON account_locks(nid_hash);
CREATE INDEX idx_account_locks_active ON account_locks(nid_hash) WHERE is_active = TRUE;

-- Customers view indexes
CREATE INDEX idx_customers_view_nid ON customers_view(nid_hash);
CREATE INDEX idx_customers_view_mobile ON customers_view(mobile_hash);

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE risk_assessments ENABLE ROW LEVEL SECURITY;
ALTER TABLE screening_results ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_signal_logs ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE risk_assessments IS 'Overall risk assessment records';
COMMENT ON TABLE scoring_model_results IS 'ML model execution results with explainability';
COMMENT ON TABLE fraud_signal_logs IS 'Individual fraud signals with weights and severity';
COMMENT ON TABLE screening_results IS 'AML/CFT screening results from providers';
COMMENT ON TABLE watchlist_entries IS 'Internal blacklist, greylist, and sanctions entries';
COMMENT ON TABLE velocity_checks IS 'Rate limiting and velocity check audit records';
COMMENT ON TABLE device_registry IS 'Device fingerprint registry for identity farming detection';
COMMENT ON TABLE account_locks IS 'Account lock history for compliance and fraud blocks';
COMMENT ON TABLE customers_view IS 'Read-only customer data stub for CIF/mobile lookups';

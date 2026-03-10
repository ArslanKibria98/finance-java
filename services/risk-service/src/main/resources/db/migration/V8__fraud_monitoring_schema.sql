-- ============================================================================
-- FRAUD MONITORING SYSTEM (Finova Fraud HLD v1)
-- KSA Islamic Financing Platform - Risk Service
-- ============================================================================

-- ENUMS
CREATE TYPE fraud_event_type AS ENUM (
    'ONBOARDING', 'LOGIN', 'LOAN_APPLICATION', 'DISBURSEMENT',
    'REPAYMENT', 'EARLY_REPAYMENT', 'IBAN_UPDATE', 'PROOF_OF_INDEBTEDNESS',
    'ACCOUNT_UPDATE'
);

CREATE TYPE fraud_decision_type AS ENUM ('ALLOW', 'ALERT', 'HOLD', 'BLOCK');

CREATE TYPE fraud_block_type AS ENUM (
    'TEMPORARY', 'PERMANENT', 'SESSION',
    'APPLICATION_LEVEL', 'DISBURSEMENT_LEVEL', 'PAYMENT_LEVEL'
);

CREATE TYPE fraud_alert_status AS ENUM (
    'OPEN', 'ASSIGNED', 'INVESTIGATING', 'RESOLVED', 'ESCALATED', 'DISMISSED'
);

CREATE TYPE fraud_alert_priority AS ENUM ('CRITICAL', 'HIGH', 'MEDIUM', 'LOW');

CREATE TYPE fraud_case_status AS ENUM (
    'OPEN', 'INVESTIGATING', 'CONFIRMED_FRAUD', 'FALSE_POSITIVE', 'CLOSED'
);

CREATE TYPE fraud_rule_status AS ENUM ('ACTIVE', 'DISABLED', 'TESTING');

CREATE TYPE fraud_rule_category AS ENUM (
    'LOCATION', 'DEVICE', 'GEOGRAPHIC_ACCESS',
    'FINANCIAL', 'PAYMENT_CARD', 'TRANSACTION_MONITORING'
);

CREATE TYPE device_type_enum AS ENUM ('MOBILE', 'TABLET', 'DESKTOP');
CREATE TYPE device_os_enum AS ENUM ('IOS', 'ANDROID', 'WINDOWS', 'MACOS', 'OTHER');
CREATE TYPE device_integrity_enum AS ENUM ('CLEAN', 'JAILBROKEN', 'ROOTED', 'UNKNOWN');

-- ============================================================================
-- FRAUD RULES (configurable per tenant — HLD Section 5)
-- ============================================================================

CREATE TABLE fraud_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    rule_id         VARCHAR(20) NOT NULL,
    scenario_name   VARCHAR(200) NOT NULL,
    scenario_name_ar VARCHAR(200),
    category        fraud_rule_category NOT NULL,
    detection_logic TEXT,
    default_action  fraud_decision_type NOT NULL,
    block_type      fraud_block_type,
    status          fraud_rule_status NOT NULL DEFAULT 'ACTIVE',
    parameters      JSONB NOT NULL DEFAULT '[]',
    priority        INT NOT NULL DEFAULT 100,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_fraud_rule_tenant UNIQUE (tenant_id, rule_id)
);

CREATE INDEX idx_fraud_rules_tenant ON fraud_rules(tenant_id);
CREATE INDEX idx_fraud_rules_active ON fraud_rules(tenant_id) WHERE status = 'ACTIVE';

-- ============================================================================
-- FRAUD EVENTS (ingested from LOS/LMS — HLD Section 4)
-- ============================================================================

CREATE TABLE fraud_events (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    event_id            VARCHAR(100) NOT NULL,
    event_type          fraud_event_type NOT NULL,
    customer_id         VARCHAR(255) NOT NULL,
    national_id_hash    VARCHAR(255),
    -- Device Data (HLD Table 3)
    device_id           VARCHAR(255),
    device_type         device_type_enum,
    device_os           device_os_enum,
    os_version          VARCHAR(50),
    device_fingerprint  VARCHAR(512),
    device_integrity    device_integrity_enum,
    -- Location Data (HLD Table 4)
    latitude            NUMERIC(10, 7),
    longitude           NUMERIC(10, 7),
    ip_address          VARCHAR(45),
    ip_country          VARCHAR(3),
    ip_city             VARCHAR(200),
    gps_country         VARCHAR(3),
    gps_city            VARCHAR(200),
    vpn_detected        BOOLEAN DEFAULT FALSE,
    proxy_detected      BOOLEAN DEFAULT FALSE,
    -- Session Data (HLD Table 5)
    session_id          VARCHAR(255),
    event_timestamp     TIMESTAMPTZ NOT NULL,
    -- Transaction Data (HLD Table 7)
    transaction_type    VARCHAR(50),
    transaction_amount  NUMERIC(20, 4),
    currency            VARCHAR(3) DEFAULT 'SAR',
    -- Loan-specific
    loan_application_id VARCHAR(100),
    loan_product_type   VARCHAR(100),
    approved_loan_amount NUMERIC(20, 4),
    -- IBAN-specific
    disbursement_iban   VARCHAR(34),
    iban_verification_status VARCHAR(20),
    iban_holder_name    VARCHAR(255),
    -- Payment-specific
    payment_iban        VARCHAR(34),
    card_last4          VARCHAR(4),
    card_country        VARCHAR(3),
    card_holder_name    VARCHAR(255),
    is_third_party_payment BOOLEAN DEFAULT FALSE,
    -- Enrichment (populated by enrichment pipeline)
    resolved_country    VARCHAR(3),
    resolved_city       VARCHAR(200),
    distance_from_last_km NUMERIC(10, 2),
    time_since_last_login_minutes INT,
    -- Metadata
    correlation_id      VARCHAR(100),
    received_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fraud_event UNIQUE (tenant_id, event_id)
);

CREATE INDEX idx_fraud_events_tenant ON fraud_events(tenant_id);
CREATE INDEX idx_fraud_events_customer ON fraud_events(tenant_id, customer_id);
CREATE INDEX idx_fraud_events_device ON fraud_events(device_id);
CREATE INDEX idx_fraud_events_type ON fraud_events(tenant_id, event_type);
CREATE INDEX idx_fraud_events_iban ON fraud_events(disbursement_iban) WHERE disbursement_iban IS NOT NULL;
CREATE INDEX idx_fraud_events_timestamp ON fraud_events(tenant_id, event_timestamp);

-- ============================================================================
-- FRAUD EVALUATIONS (rule engine output — HLD Section 3.3 Step 4-5)
-- ============================================================================

CREATE TABLE fraud_evaluations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    event_id            VARCHAR(100) NOT NULL,
    fraud_event_id      UUID NOT NULL REFERENCES fraud_events(id),
    customer_id         VARCHAR(255) NOT NULL,
    decision            fraud_decision_type NOT NULL,
    block_type          fraud_block_type,
    composite_risk_score INT NOT NULL,
    risk_level          VARCHAR(20) NOT NULL,
    triggered_rules     JSONB NOT NULL DEFAULT '[]',
    block_reason        TEXT,
    block_duration_hours INT,
    customer_message    TEXT,
    evaluation_time_ms  INT NOT NULL,
    evaluated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fraud_evaluation UNIQUE (tenant_id, event_id)
);

CREATE INDEX idx_fraud_eval_tenant ON fraud_evaluations(tenant_id);
CREATE INDEX idx_fraud_eval_customer ON fraud_evaluations(tenant_id, customer_id);
CREATE INDEX idx_fraud_eval_decision ON fraud_evaluations(tenant_id, decision);

-- ============================================================================
-- FRAUD ALERTS (HLD Section 3.3 Step 5-8)
-- ============================================================================

CREATE TABLE fraud_alerts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    evaluation_id   UUID NOT NULL REFERENCES fraud_evaluations(id),
    customer_id     VARCHAR(255) NOT NULL,
    triggering_rule_id VARCHAR(20) NOT NULL,
    priority        fraud_alert_priority NOT NULL,
    status          fraud_alert_status NOT NULL DEFAULT 'OPEN',
    decision        fraud_decision_type NOT NULL,
    summary         TEXT NOT NULL,
    summary_ar      TEXT,
    details         JSONB,
    assigned_to     UUID,
    assigned_at     TIMESTAMPTZ,
    resolved_by     UUID,
    resolved_at     TIMESTAMPTZ,
    resolution_note TEXT,
    sla_deadline    TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_fraud_alerts_tenant ON fraud_alerts(tenant_id);
CREATE INDEX idx_fraud_alerts_status ON fraud_alerts(tenant_id, status);
CREATE INDEX idx_fraud_alerts_priority ON fraud_alerts(tenant_id, priority);
CREATE INDEX idx_fraud_alerts_assigned ON fraud_alerts(assigned_to) WHERE assigned_to IS NOT NULL;
CREATE INDEX idx_fraud_alerts_open ON fraud_alerts(tenant_id) WHERE status = 'OPEN';
CREATE INDEX idx_fraud_alerts_sla ON fraud_alerts(sla_deadline) WHERE status IN ('OPEN', 'ASSIGNED');

-- ============================================================================
-- FRAUD CASES (HLD Section 3.2 — Case Management)
-- ============================================================================

CREATE TABLE fraud_cases (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    case_number     VARCHAR(50) NOT NULL,
    customer_id     VARCHAR(255) NOT NULL,
    status          fraud_case_status NOT NULL DEFAULT 'OPEN',
    assigned_to     UUID,
    summary         TEXT,
    estimated_loss  NUMERIC(20, 4),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at       TIMESTAMPTZ,
    closure_note    TEXT,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_fraud_case_number UNIQUE (tenant_id, case_number)
);

CREATE TABLE fraud_case_alerts (
    case_id         UUID NOT NULL REFERENCES fraud_cases(id),
    alert_id        UUID NOT NULL REFERENCES fraud_alerts(id),
    linked_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (case_id, alert_id)
);

CREATE TABLE fraud_case_actions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    case_id         UUID NOT NULL REFERENCES fraud_cases(id),
    action          VARCHAR(100) NOT NULL,
    note            TEXT,
    performed_by    UUID NOT NULL,
    performed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_cases_tenant ON fraud_cases(tenant_id);
CREATE INDEX idx_fraud_cases_status ON fraud_cases(tenant_id, status);
CREATE INDEX idx_fraud_cases_customer ON fraud_cases(tenant_id, customer_id);

-- ============================================================================
-- SESSION HISTORY (HLD Section 4.3 — Login & Session Timing)
-- ============================================================================

CREATE TABLE session_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    customer_id     VARCHAR(255) NOT NULL,
    session_id      VARCHAR(255),
    device_id       VARCHAR(255),
    ip_address      VARCHAR(45),
    latitude        NUMERIC(10, 7),
    longitude       NUMERIC(10, 7),
    country         VARCHAR(3),
    city            VARCHAR(200),
    login_at        TIMESTAMPTZ NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_session_customer ON session_events(tenant_id, customer_id);
CREATE INDEX idx_session_device ON session_events(device_id);
CREATE INDEX idx_session_time ON session_events(tenant_id, customer_id, login_at);

-- ============================================================================
-- FRAUD USER PROFILES (HLD Section 4.4 — Mirrored KYC data)
-- ============================================================================

CREATE TABLE fraud_user_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    customer_id         VARCHAR(255) NOT NULL,
    national_id_hash    VARCHAR(255) NOT NULL,
    full_name_hash      VARCHAR(255),
    nationality         VARCHAR(3),
    mobile_hash         VARCHAR(255),
    email_hash          VARCHAR(255),
    registered_address_city VARCHAR(200),
    registered_address_country VARCHAR(3),
    registered_iban     VARCHAR(34),
    iban_verified       BOOLEAN DEFAULT FALSE,
    iban_holder_name    VARCHAR(255),
    aml_risk_level      VARCHAR(20),
    last_login_at       TIMESTAMPTZ,
    last_login_latitude NUMERIC(10, 7),
    last_login_longitude NUMERIC(10, 7),
    last_login_country  VARCHAR(3),
    last_login_city     VARCHAR(200),
    last_login_device_id VARCHAR(255),
    account_created_at  TIMESTAMPTZ,
    last_activity_at    TIMESTAMPTZ,
    total_loan_applications INT DEFAULT 0,
    total_disbursements INT DEFAULT 0,
    total_repayments    INT DEFAULT 0,
    is_dormant          BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fraud_user_profile UNIQUE (tenant_id, customer_id)
);

CREATE INDEX idx_fraud_user_customer ON fraud_user_profiles(tenant_id, customer_id);
CREATE INDEX idx_fraud_user_nid ON fraud_user_profiles(national_id_hash);
CREATE INDEX idx_fraud_user_iban ON fraud_user_profiles(registered_iban) WHERE registered_iban IS NOT NULL;
CREATE INDEX idx_fraud_user_dormant ON fraud_user_profiles(tenant_id) WHERE is_dormant = TRUE;

-- ============================================================================
-- EXTENDED BLACKLISTS (HLD Sections 6.2, 6.3, 8)
-- ============================================================================

CREATE TABLE device_blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    device_id       VARCHAR(255) NOT NULL,
    reason          TEXT NOT NULL,
    block_type      fraud_block_type NOT NULL DEFAULT 'TEMPORARY',
    escalation_count INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    added_by        UUID,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_blacklist UNIQUE (tenant_id, device_id)
);

CREATE TABLE country_blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    country_code    VARCHAR(3) NOT NULL,
    country_name    VARCHAR(200) NOT NULL,
    country_name_ar VARCHAR(200),
    reason          TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    added_by        UUID,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_country_blacklist UNIQUE (tenant_id, country_code)
);

CREATE TABLE iban_blacklist (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    iban_hash       VARCHAR(255) NOT NULL,
    reason          TEXT NOT NULL,
    linked_account_count INT DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    added_by        UUID,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_iban_blacklist UNIQUE (tenant_id, iban_hash)
);

CREATE INDEX idx_device_bl_active ON device_blacklist(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_country_bl_active ON country_blacklist(tenant_id) WHERE is_active = TRUE;
CREATE INDEX idx_iban_bl_active ON iban_blacklist(tenant_id) WHERE is_active = TRUE;

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE fraud_rules ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_evaluations ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_alerts ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_cases ENABLE ROW LEVEL SECURITY;
ALTER TABLE fraud_user_profiles ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE fraud_rules IS 'Configurable fraud detection rules per tenant (30 rules from HLD)';
COMMENT ON TABLE fraud_events IS 'Ingested events from LOS/LMS for fraud evaluation';
COMMENT ON TABLE fraud_evaluations IS 'Rule engine evaluation results with triggered rules and decision';
COMMENT ON TABLE fraud_alerts IS 'Generated fraud alerts with SLA tracking and assignment';
COMMENT ON TABLE fraud_cases IS 'Fraud investigation cases created from alerts';
COMMENT ON TABLE session_events IS 'Login/session history for location and velocity analysis';
COMMENT ON TABLE fraud_user_profiles IS 'Mirrored KYC data for fraud rule evaluation';
COMMENT ON TABLE device_blacklist IS 'Blocked devices associated with confirmed fraud';
COMMENT ON TABLE country_blacklist IS 'Blocked countries for geographic access control';
COMMENT ON TABLE iban_blacklist IS 'Blocked IBANs associated with fraud patterns';

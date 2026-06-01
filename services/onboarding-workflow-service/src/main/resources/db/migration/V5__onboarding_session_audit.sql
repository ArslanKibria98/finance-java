-- Phase 2A — Onboarding session audit tables.
-- Tracks every workflow execution across all flows (KSA, Canada, Foreign, Guest)
-- for SAMA compliance + email→workflowId lookup + external ID indexing.

-- =============================================================================
-- onboarding_sessions — one row per workflow execution
-- =============================================================================
CREATE TABLE onboarding_sessions (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id          VARCHAR(200) NOT NULL UNIQUE,
    flow_type            VARCHAR(20)  NOT NULL,
    tenant_id            UUID         NOT NULL,
    email_hash           VARCHAR(64),
    mobile_hash          VARCHAR(64),
    national_id_hash     VARCHAR(64),
    masked_email         VARCHAR(120),
    masked_mobile        VARCHAR(60),
    keycloak_user_id     VARCHAR(100),
    customer_id          VARCHAR(100),
    wallet_id            VARCHAR(100),
    global_uid           VARCHAR(100),
    current_step         VARCHAR(50),
    onboarding_complete  BOOLEAN      NOT NULL DEFAULT FALSE,
    started_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at         TIMESTAMPTZ,
    failed_at            TIMESTAMPTZ,
    failure_reason       TEXT,
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_flow_type CHECK (flow_type IN ('KSA','CANADA','FOREIGN','GUEST'))
);

CREATE INDEX idx_onb_sessions_tenant    ON onboarding_sessions(tenant_id);
CREATE INDEX idx_onb_sessions_flow      ON onboarding_sessions(flow_type);
CREATE INDEX idx_onb_sessions_email     ON onboarding_sessions(email_hash);
CREATE INDEX idx_onb_sessions_mobile    ON onboarding_sessions(mobile_hash);
CREATE INDEX idx_onb_sessions_kc_user   ON onboarding_sessions(keycloak_user_id);
CREATE INDEX idx_onb_sessions_customer  ON onboarding_sessions(customer_id);

-- =============================================================================
-- onboarding_step_audit — one row per step transition (full state machine trail)
-- =============================================================================
CREATE TABLE onboarding_step_audit (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID         NOT NULL REFERENCES onboarding_sessions(id) ON DELETE CASCADE,
    workflow_id     VARCHAR(200) NOT NULL,
    step            VARCHAR(50)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OK',
    payload         JSONB,
    failure_reason  TEXT,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_step_status CHECK (status IN ('OK','FAILED','SKIPPED'))
);

CREATE INDEX idx_step_audit_session     ON onboarding_step_audit(session_id);
CREATE INDEX idx_step_audit_workflow    ON onboarding_step_audit(workflow_id);
CREATE INDEX idx_step_audit_step        ON onboarding_step_audit(step);

-- =============================================================================
-- onboarding_external_refs — external system reference IDs (Facia, Keycloak,
-- customer-service, wallet-service, OTP requests, risk-service, etc.)
-- =============================================================================
CREATE TABLE onboarding_external_refs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id      UUID         NOT NULL REFERENCES onboarding_sessions(id) ON DELETE CASCADE,
    workflow_id     VARCHAR(200) NOT NULL,
    ref_type        VARCHAR(30)  NOT NULL,
    ref_id          VARCHAR(200) NOT NULL,
    metadata        JSONB,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_ref_type CHECK (ref_type IN
        ('KEYCLOAK','FACIA_DOC','FACIA_SELFIE','CUSTOMER','WALLET',
         'OTP_MOBILE','OTP_EMAIL','RISK','PII_VAULT','GLOBAL_PROFILE'))
);

CREATE INDEX idx_ext_refs_session       ON onboarding_external_refs(session_id);
CREATE INDEX idx_ext_refs_workflow      ON onboarding_external_refs(workflow_id);
CREATE INDEX idx_ext_refs_type          ON onboarding_external_refs(ref_type);
CREATE INDEX idx_ext_refs_ref_id        ON onboarding_external_refs(ref_id);

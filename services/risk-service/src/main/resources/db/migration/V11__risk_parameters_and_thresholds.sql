-- V11: Risk Parameters and Scoring Thresholds
-- HLD: FV-HLD-RISK-002 v1.0 — Modules 3 & 4

-- ===== RISK PARAMETERS =====
CREATE TABLE risk_parameters (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL,
    risk_type             VARCHAR(20) NOT NULL,
    flow                  VARCHAR(100),
    category              VARCHAR(200) NOT NULL,
    sub_category          VARCHAR(200),
    question_en           VARCHAR(500) NOT NULL,
    question_ar           VARCHAR(500),
    input_type            VARCHAR(20) NOT NULL,
    lov_set_id            UUID REFERENCES lov_sets(id),
    parent_parameter_id   UUID REFERENCES risk_parameters(id),
    parent_trigger_value  VARCHAR(200),
    category_weight       NUMERIC(10,4) NOT NULL DEFAULT 0,
    operator              VARCHAR(20),
    expected_value        VARCHAR(200),
    flag_type             VARCHAR(10) NOT NULL DEFAULT 'NONE',
    filled_by             VARCHAR(20) NOT NULL DEFAULT 'SYSTEM_USER',
    active                BOOLEAN NOT NULL DEFAULT true,
    display_order         INT NOT NULL DEFAULT 0,
    language              VARCHAR(10) DEFAULT 'en',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version               INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_risk_params_tenant_type ON risk_parameters(tenant_id, risk_type);
CREATE INDEX idx_risk_params_active ON risk_parameters(tenant_id, risk_type, active);
CREATE INDEX idx_risk_params_category ON risk_parameters(tenant_id, risk_type, category);
CREATE INDEX idx_risk_params_parent ON risk_parameters(tenant_id, parent_parameter_id);

-- ===== SCORING THRESHOLDS =====
CREATE TABLE scoring_thresholds (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    risk_type       VARCHAR(20) NOT NULL,
    risk_level      VARCHAR(20) NOT NULL,
    min_score       NUMERIC(10,2) NOT NULL,
    max_score       NUMERIC(10,2) NOT NULL,
    description_en  VARCHAR(300),
    description_ar  VARCHAR(300),
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_scoring_thresh_tenant ON scoring_thresholds(tenant_id, risk_type);
CREATE INDEX idx_scoring_thresh_active ON scoring_thresholds(tenant_id, risk_type, active);

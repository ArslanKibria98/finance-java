-- V12: Assessment Sessions and Answers
-- HLD: FV-HLD-RISK-002 v1.0 — Module 5

-- ===== ASSESSMENT SESSIONS =====
CREATE TABLE assessment_sessions (
    id                            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                     UUID NOT NULL,
    risk_type                     VARCHAR(20) NOT NULL,
    entity_reference              VARCHAR(200) NOT NULL,
    parent_session_id             UUID REFERENCES assessment_sessions(id),
    status                        VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    total_score                   NUMERIC(10,2) DEFAULT 0,
    risk_level                    VARCHAR(20),
    pep_flag                      BOOLEAN NOT NULL DEFAULT false,
    edd_flag                      BOOLEAN NOT NULL DEFAULT false,
    kyc_flag                      BOOLEAN NOT NULL DEFAULT false,
    dominant_override             BOOLEAN NOT NULL DEFAULT false,
    third_party_aml_result        VARCHAR(50),
    third_party_sanctions_result  VARCHAR(50),
    third_party_blocklist_result  VARCHAR(50),
    parameter_version_snapshot    INT,
    lov_version_snapshot          INT,
    idempotency_key               VARCHAR(100),
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                       INT NOT NULL DEFAULT 1,
    UNIQUE(tenant_id, idempotency_key)
);

CREATE INDEX idx_assess_session_tenant ON assessment_sessions(tenant_id);
CREATE INDEX idx_assess_session_entity ON assessment_sessions(tenant_id, entity_reference);
CREATE INDEX idx_assess_session_status ON assessment_sessions(tenant_id, status);
CREATE INDEX idx_assess_session_idem ON assessment_sessions(tenant_id, idempotency_key);

-- ===== ASSESSMENT ANSWERS =====
CREATE TABLE assessment_answers (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id              UUID NOT NULL REFERENCES assessment_sessions(id),
    tenant_id               UUID NOT NULL,
    parameter_id            UUID NOT NULL REFERENCES risk_parameters(id),
    answer_version          INT NOT NULL DEFAULT 1,
    version_status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    answer_value            VARCHAR(500),
    answer_type             VARCHAR(20),
    language_code           VARCHAR(10),
    weight_contribution     NUMERIC(10,4),
    risk_score_at_submission NUMERIC(10,2),
    change_reason           VARCHAR(30),
    score_delta             NUMERIC(10,4),
    level_changed           BOOLEAN NOT NULL DEFAULT false,
    previous_risk_level     VARCHAR(20),
    new_risk_level          VARCHAR(20),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_assess_answer_session ON assessment_answers(tenant_id, session_id);
CREATE INDEX idx_assess_answer_active ON assessment_answers(tenant_id, session_id, version_status);
CREATE INDEX idx_assess_answer_param ON assessment_answers(tenant_id, session_id, parameter_id);

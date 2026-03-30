-- V13: Entity Status Records and Scenario Rules
-- HLD: FV-HLD-RISK-002 v1.0 — Modules 6 & 7

-- ===== ENTITY STATUS RECORDS (append-only history) =====
CREATE TABLE entity_status_records (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    session_id          UUID REFERENCES assessment_sessions(id),
    entity_reference    VARCHAR(200) NOT NULL,
    risk_status         VARCHAR(20),
    account_status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    compliance_status   VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    status_reason       VARCHAR(500),
    changed_by          UUID,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_entity_status_entity ON entity_status_records(tenant_id, entity_reference);
CREATE INDEX idx_entity_status_latest ON entity_status_records(tenant_id, entity_reference, created_at DESC);

-- ===== SCENARIO RULES =====
CREATE TABLE scenario_rules (
    id                            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                     UUID NOT NULL,
    scenario_name                 VARCHAR(200) NOT NULL,
    scenario_name_ar              VARCHAR(200),
    trigger_risk_status           VARCHAR(20),
    trigger_pep_flag              BOOLEAN,
    trigger_third_party_check_type VARCHAR(20),
    trigger_third_party_result    VARCHAR(50),
    resulting_account_status      VARCHAR(20) NOT NULL,
    resulting_compliance_status   VARCHAR(30) NOT NULL,
    requires_manual_review        BOOLEAN NOT NULL DEFAULT false,
    notify_role                   VARCHAR(100),
    priority                      INT NOT NULL DEFAULT 0,
    sla_duration_hours            INT NOT NULL DEFAULT 24,
    active                        BOOLEAN NOT NULL DEFAULT true,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                       INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_scenario_rules_tenant ON scenario_rules(tenant_id);
CREATE INDEX idx_scenario_rules_active ON scenario_rules(tenant_id, active);
CREATE INDEX idx_scenario_rules_priority ON scenario_rules(tenant_id, active, priority);

-- V14: Maker-Checker Review Tasks and Audit Trail
-- HLD: FV-HLD-RISK-002 v1.0 — Modules 8 & 9

-- ===== REVIEW TASKS (Maker-Checker Workflow) =====
CREATE TABLE review_tasks (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL,
    session_id                  UUID NOT NULL REFERENCES assessment_sessions(id),
    entity_reference            VARCHAR(200) NOT NULL,
    status                      VARCHAR(30) NOT NULL DEFAULT 'PENDING_MAKER',
    maker_id                    UUID,
    maker_recommendation        VARCHAR(20),
    maker_comment               VARCHAR(1000),
    maker_account_status        VARCHAR(20),
    maker_compliance_status     VARCHAR(30),
    maker_action_at             TIMESTAMPTZ,
    approver_id                 UUID,
    approver_action             VARCHAR(20),
    approver_comment            VARCHAR(1000),
    approver_account_status     VARCHAR(20),
    approver_compliance_status  VARCHAR(30),
    approver_action_at          TIMESTAMPTZ,
    sla_deadline                TIMESTAMPTZ,
    sla_breached                BOOLEAN NOT NULL DEFAULT false,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                     INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_review_tasks_tenant ON review_tasks(tenant_id);
CREATE INDEX idx_review_tasks_session ON review_tasks(tenant_id, session_id);
CREATE INDEX idx_review_tasks_status ON review_tasks(tenant_id, status);
CREATE INDEX idx_review_tasks_sla ON review_tasks(tenant_id, sla_deadline) WHERE sla_breached = false;

-- ===== AUDIT TRAIL (immutable, append-only, 7-year retention) =====
CREATE TABLE audit_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    entity_type     VARCHAR(30) NOT NULL,
    entity_id       UUID NOT NULL,
    action          VARCHAR(50) NOT NULL,
    before_state    TEXT,
    after_state     TEXT,
    actor_id        UUID,
    actor_role      VARCHAR(50),
    ip_address      VARCHAR(50),
    correlation_id  VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Audit table is append-only, no UPDATE or DELETE allowed
CREATE INDEX idx_audit_entity ON audit_entries(tenant_id, entity_type, entity_id);
CREATE INDEX idx_audit_actor ON audit_entries(tenant_id, actor_id);
CREATE INDEX idx_audit_date ON audit_entries(tenant_id, created_at);
CREATE INDEX idx_audit_correlation ON audit_entries(tenant_id, correlation_id);

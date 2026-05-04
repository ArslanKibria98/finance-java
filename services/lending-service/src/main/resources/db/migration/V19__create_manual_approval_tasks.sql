-- V19__create_manual_approval_tasks.sql
-- Pending manual approval tasks for loan applications that exceed product auto-approval threshold.

CREATE TABLE manual_approval_tasks (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,

    application_id          UUID NOT NULL,
    application_number      VARCHAR(50) NOT NULL,
    customer_id             UUID NOT NULL,
    customer_name           VARCHAR(255),
    product_id              UUID NOT NULL,
    product_name            VARCHAR(255),

    requested_amount        NUMERIC(19, 2) NOT NULL,
    tenure_months           INT NOT NULL,
    monthly_installment     NUMERIC(19, 2),
    credit_score            INT,
    dbr_percentage          NUMERIC(5, 2),

    assigned_role           VARCHAR(50) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
        -- PENDING | APPROVED | REJECTED | BREACHED | EXPIRED

    sla_deadline            TIMESTAMPTZ NOT NULL,
    sla_breached            BOOLEAN NOT NULL DEFAULT FALSE,

    decision_by             UUID,
    decision_at             TIMESTAMPTZ,
    decision_notes          TEXT,
    rejection_reason        VARCHAR(100),

    workflow_id             VARCHAR(255) NOT NULL,

    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_manual_approval_application UNIQUE (tenant_id, application_id),
    CONSTRAINT chk_manual_approval_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'BREACHED', 'EXPIRED'))
);

CREATE INDEX idx_manual_approval_tenant_status ON manual_approval_tasks(tenant_id, status);
CREATE INDEX idx_manual_approval_role ON manual_approval_tasks(tenant_id, assigned_role, status);
CREATE INDEX idx_manual_approval_sla ON manual_approval_tasks(tenant_id, sla_deadline) WHERE status = 'PENDING';
CREATE INDEX idx_manual_approval_workflow ON manual_approval_tasks(workflow_id);

COMMENT ON TABLE manual_approval_tasks IS 'Pending underwriter review tasks for loan applications exceeding auto-approval threshold';

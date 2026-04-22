-- ============================================================
-- V15: Loan Rescheduling & Restructuring
-- Supports: SKIP_PAYMENT, TENURE_EXTENSION, PAYMENT_HOLIDAY, RESTRUCTURING
-- Per Blueprint 17 (Loan Servicing & Restructuring)
-- ============================================================

CREATE TABLE loan_reschedules (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    loan_id                 UUID NOT NULL,
    loan_number             VARCHAR(50),
    reschedule_type         VARCHAR(50) NOT NULL,        -- SKIP_PAYMENT | TENURE_EXTENSION | PAYMENT_HOLIDAY | RESTRUCTURING
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING',  -- PENDING | APPROVED | REJECTED | APPLIED | CANCELLED

    -- Request parameters
    requested_by            UUID,                        -- Customer or CSA UUID
    justification           TEXT,                        -- Required for PAYMENT_HOLIDAY, RESTRUCTURING
    requested_skip_month    DATE,                        -- SKIP_PAYMENT: which installment to skip
    extension_months        INT,                         -- TENURE_EXTENSION: how many months
    holiday_months          INT,                         -- PAYMENT_HOLIDAY: how many months (max 3)
    new_profit_rate         NUMERIC(10, 8),              -- RESTRUCTURING: reduced profit rate
    write_off_amount        NUMERIC(19, 6),              -- RESTRUCTURING: principal write-off
    profit_waiver_amount    NUMERIC(19, 6),              -- RESTRUCTURING: unearned profit waiver

    -- Computed outputs
    old_tenure_months       INT,
    new_tenure_months       INT,
    old_installment_amount  NUMERIC(19, 6),
    new_installment_amount  NUMERIC(19, 6),
    old_maturity_date       DATE,
    new_maturity_date       DATE,

    -- Approval
    approver_id             UUID,
    approver_role           VARCHAR(100),                -- operations_head | credit_committee
    approval_notes          TEXT,
    approved_at             TIMESTAMPTZ,
    rejection_reason        TEXT,
    rejected_at             TIMESTAMPTZ,

    -- GL tracking (RESTRUCTURING only, via ledger-service)
    gl_entry_id             UUID,                        -- Journal entry ID in ledger-service
    gl_posted               BOOLEAN NOT NULL DEFAULT FALSE,

    -- Fineract tracking (via ledger-service Fineract proxy)
    fineract_reschedule_id  BIGINT,
    fineract_synced         BOOLEAN NOT NULL DEFAULT FALSE,

    -- Temporal workflow tracking
    workflow_id             VARCHAR(255),
    idempotency_key         VARCHAR(100),

    -- Audit
    applied_at              TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    cancellation_reason     VARCHAR(500),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1
);

-- Tenant isolation
CREATE INDEX idx_loan_reschedules_tenant   ON loan_reschedules(tenant_id);
CREATE INDEX idx_loan_reschedules_loan     ON loan_reschedules(tenant_id, loan_id);
CREATE INDEX idx_loan_reschedules_status   ON loan_reschedules(tenant_id, status);
CREATE INDEX idx_loan_reschedules_workflow ON loan_reschedules(workflow_id);

-- Idempotency: one active reschedule per loan per type at a time
CREATE UNIQUE INDEX idx_loan_reschedules_active
    ON loan_reschedules(tenant_id, loan_id, reschedule_type)
    WHERE status IN ('PENDING', 'APPROVED');

-- Idempotency key uniqueness
CREATE UNIQUE INDEX idx_loan_reschedules_idempotency
    ON loan_reschedules(tenant_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- Updated_at auto-maintenance
CREATE OR REPLACE FUNCTION update_loan_reschedule_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_loan_reschedule_updated_at
    BEFORE UPDATE ON loan_reschedules
    FOR EACH ROW EXECUTE FUNCTION update_loan_reschedule_updated_at();

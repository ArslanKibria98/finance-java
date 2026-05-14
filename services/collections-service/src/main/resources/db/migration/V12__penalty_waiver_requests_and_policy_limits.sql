-- V12__penalty_waiver_requests_and_policy_limits.sql

-- 1. Add limits to dunning_policies
ALTER TABLE dunning_policies
    ADD COLUMN penalty_waiver_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN max_penalty_waivers_allowed INT NOT NULL DEFAULT 1;

-- 2. Create penalty_waiver_requests table
CREATE TABLE penalty_waiver_requests (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL,
    loan_id               UUID NOT NULL,
    application_id        UUID NOT NULL,
    invoice_id            VARCHAR(100) NOT NULL,
    installment_id        UUID NOT NULL,
    requested_amount      NUMERIC(19, 4) NOT NULL,
    reason                VARCHAR(500) NOT NULL,

    -- PENDING | APPROVED | REJECTED
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    requested_by          UUID NOT NULL, -- Customer ID
    requested_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    processed_by          UUID, -- Admin ID
    processed_at          TIMESTAMPTZ,
    rejection_reason      VARCHAR(500),

    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_waiver_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_waiver_request_tenant_loan ON penalty_waiver_requests(tenant_id, loan_id);
CREATE INDEX idx_waiver_request_tenant_app  ON penalty_waiver_requests(tenant_id, application_id);
CREATE INDEX idx_waiver_request_tenant_inv  ON penalty_waiver_requests(tenant_id, invoice_id);
CREATE INDEX idx_waiver_request_status      ON penalty_waiver_requests(tenant_id, status);

COMMENT ON TABLE penalty_waiver_requests IS 'Tracking table for customer-initiated penalty waiver requests';

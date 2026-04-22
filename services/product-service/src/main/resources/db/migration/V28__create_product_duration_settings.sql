-- Recreates product_duration_settings (dropped in V20) to back the Admin UI "Duration Settings" tab.
-- Disbursement is stored in HOURS (UI shows hours). All values allow 0 which means "immediate / no wait".
CREATE TABLE product_duration_settings (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                       UUID NOT NULL,
    product_id                      UUID NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    request_duration_days           INT  NOT NULL DEFAULT 0,
    approval_duration_days          INT  NOT NULL DEFAULT 0,
    disbursement_duration_hours     INT  NOT NULL DEFAULT 0,
    repayment_duration_days         INT  NOT NULL DEFAULT 0,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                         INT  NOT NULL DEFAULT 1,
    CONSTRAINT chk_dur_request_non_negative       CHECK (request_duration_days       >= 0),
    CONSTRAINT chk_dur_approval_non_negative      CHECK (approval_duration_days      >= 0),
    CONSTRAINT chk_dur_disbursement_non_negative  CHECK (disbursement_duration_hours >= 0),
    CONSTRAINT chk_dur_repayment_non_negative     CHECK (repayment_duration_days     >= 0)
);

CREATE INDEX idx_duration_settings_tenant  ON product_duration_settings(tenant_id);
CREATE INDEX idx_duration_settings_product ON product_duration_settings(product_id);

-- ============================================================
-- V4: Dunning Policies — Admin-configurable delinquency rules
-- Per Blueprint 17 & ProdDocs/06 — product-wise DPD thresholds,
-- actions per stage, and late-fee/Simah reporting config.
-- ============================================================

CREATE TABLE dunning_policies (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL,
    policy_name                 VARCHAR(100) NOT NULL,
    product_code                VARCHAR(50),                 -- NULL = tenant-wide default
    description                 TEXT,
    is_active                   BOOLEAN NOT NULL DEFAULT TRUE,
    is_default                  BOOLEAN NOT NULL DEFAULT FALSE, -- fallback when no product match

    -- DPD thresholds (inclusive lower bound)
    pre_due_days_before         INT NOT NULL DEFAULT 3,      -- reminders N days BEFORE due
    grace_period_days           INT NOT NULL DEFAULT 10,     -- grace window after due_date
    soft_collection_dpd         INT NOT NULL DEFAULT 11,     -- >= this DPD -> SOFT
    hard_collection_dpd         INT NOT NULL DEFAULT 31,     -- >= this DPD -> HARD
    legal_dpd                   INT NOT NULL DEFAULT 91,     -- >= this DPD -> LEGAL
    write_off_dpd               INT NOT NULL DEFAULT 181,    -- >= this DPD -> WRITE_OFF

    -- Actions per stage (JSONB for flexibility; see DunningActionConfig VO)
    -- Example: {"channels":["SMS","EMAIL"],"template":"PRE_DUE_TPL","maxAttempts":3}
    pre_due_actions             JSONB,
    due_date_actions            JSONB,
    grace_period_actions        JSONB,
    soft_collection_actions     JSONB,
    hard_collection_actions     JSONB,
    legal_actions               JSONB,
    write_off_actions           JSONB,

    -- Late fee config (Sharia: amount goes to charity fund, NOT revenue)
    late_fee_enabled            BOOLEAN NOT NULL DEFAULT FALSE,
    late_fee_type               VARCHAR(20),                 -- FLAT | PERCENTAGE
    late_fee_amount             NUMERIC(19, 4),              -- for FLAT type
    late_fee_percentage         NUMERIC(5, 2),               -- for PERCENTAGE type (of outstanding)
    late_fee_min_dpd            INT NOT NULL DEFAULT 1,      -- apply fee from this DPD
    late_fee_max_amount         NUMERIC(19, 4),              -- cap per installment
    charity_fund_account        VARCHAR(100),                -- GL account for charity sweep

    -- Simah reporting
    simah_report_enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    simah_report_dpd            INT NOT NULL DEFAULT 60,     -- DPD at which to report
    simah_default_status_dpd    INT NOT NULL DEFAULT 90,     -- DPD at which to mark DEFAULT

    -- Notifications / escalation
    auto_assign_agent           BOOLEAN NOT NULL DEFAULT FALSE,
    agent_assignment_dpd        INT,                         -- auto-assign at this DPD
    wallet_freeze_dpd           INT,                         -- freeze customer wallet at this DPD

    -- Audit
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by                  UUID,
    updated_by                  UUID,
    version                     INT NOT NULL DEFAULT 1,

    CONSTRAINT uq_dunning_policy_name UNIQUE (tenant_id, policy_name),
    CONSTRAINT chk_dunning_thresholds CHECK (
        soft_collection_dpd < hard_collection_dpd
        AND hard_collection_dpd < legal_dpd
        AND legal_dpd < write_off_dpd
    ),
    CONSTRAINT chk_dunning_late_fee_type CHECK (
        late_fee_type IS NULL OR late_fee_type IN ('FLAT', 'PERCENTAGE')
    )
);

-- Only one active default policy per tenant
CREATE UNIQUE INDEX idx_dunning_policies_default
    ON dunning_policies(tenant_id)
    WHERE is_default = TRUE AND is_active = TRUE;

-- Only one active policy per product per tenant
CREATE UNIQUE INDEX idx_dunning_policies_product
    ON dunning_policies(tenant_id, product_code)
    WHERE product_code IS NOT NULL AND is_active = TRUE;

CREATE INDEX idx_dunning_policies_tenant     ON dunning_policies(tenant_id);
CREATE INDEX idx_dunning_policies_active     ON dunning_policies(tenant_id, is_active);
CREATE INDEX idx_dunning_policies_product_lookup ON dunning_policies(tenant_id, product_code, is_active);

-- Audit trigger
CREATE OR REPLACE FUNCTION update_dunning_policy_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_dunning_policy_updated_at
    BEFORE UPDATE ON dunning_policies
    FOR EACH ROW EXECUTE FUNCTION update_dunning_policy_updated_at();

-- ============================================================
-- Seed: sensible tenant-wide default (DPD buckets per BRS §3.2)
-- ============================================================

INSERT INTO dunning_policies (
    tenant_id, policy_name, product_code, description, is_active, is_default,
    pre_due_days_before, grace_period_days,
    soft_collection_dpd, hard_collection_dpd, legal_dpd, write_off_dpd,
    pre_due_actions, due_date_actions, grace_period_actions,
    soft_collection_actions, hard_collection_actions, legal_actions, write_off_actions,
    late_fee_enabled, late_fee_type, late_fee_percentage, late_fee_min_dpd, charity_fund_account,
    simah_report_enabled, simah_report_dpd, simah_default_status_dpd,
    auto_assign_agent, agent_assignment_dpd, wallet_freeze_dpd
) VALUES (
    '00000000-0000-0000-0000-000000000001',
    'DEFAULT_TENANT_POLICY',
    NULL,
    'Tenant-wide default delinquency policy (fallback when no product-specific policy exists).',
    TRUE, TRUE,
    3, 10,
    11, 31, 91, 181,
    '{"channels":["SMS","EMAIL"],"template":"PRE_DUE_REMINDER","daysBeforeDue":[3,1]}'::jsonb,
    '{"channels":["SMS","EMAIL","PUSH"],"template":"DUE_DATE_REMINDER"}'::jsonb,
    '{"channels":["SMS","EMAIL"],"template":"GRACE_PERIOD_WARNING"}'::jsonb,
    '{"channels":["SMS","EMAIL","PUSH"],"template":"SOFT_COLLECTION","callAttempts":3}'::jsonb,
    '{"channels":["SMS","PHONE_CALL"],"template":"HARD_COLLECTION","fieldVisitRequired":true,"assignToAgent":true}'::jsonb,
    '{"channels":["LEGAL_NOTICE","EMAIL"],"template":"LEGAL_NOTICE","reportToSimah":true,"freezeWallet":true}'::jsonb,
    '{"channels":["EMAIL"],"template":"WRITE_OFF_NOTIFICATION","glWriteOff":true}'::jsonb,
    TRUE, 'PERCENTAGE', 1.50, 11, 'CHARITY_FUND_001',
    TRUE, 60, 90,
    TRUE, 31, 91
) ON CONFLICT DO NOTHING;

COMMENT ON TABLE dunning_policies IS 'Admin-configurable delinquency rules — product-wise DPD thresholds, actions, late fees, Simah reporting';

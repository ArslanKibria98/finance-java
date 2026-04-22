-- ============================================================
-- V6: Delinquency Management — Redesigned to match LMS UI model
-- 6 stages (delinquencyType 1..6), per-product configuration,
-- Early Settlement supports Fixed & Custom Frequency (singles + ranges).
--
-- Replaces V4's dunning_policies (DPD-threshold single policy) with
-- per-stage rule rows + early-settlement configs.
-- ============================================================

-- Drop V4 artifacts (dev-branch, safe to recreate)
DROP TRIGGER IF EXISTS trg_dunning_policy_updated_at ON dunning_policies;
DROP FUNCTION IF EXISTS update_dunning_policy_updated_at();
DROP TABLE IF EXISTS dunning_policies;

-- ──────────────────────────────────────────────────────────────
-- 1. delinquency_rules — one row per (tenant, product, delinquencyType)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE delinquency_rules (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id            UUID NOT NULL,
    product_id           UUID NOT NULL,

    -- 1=Early Settlement, 2=Due Loan, 3=Late Payment,
    -- 4=Write-offs, 5=Non-Performing Loan, 6=Broken Promises
    delinquency_type     SMALLINT NOT NULL,

    is_percentage        BOOLEAN NOT NULL DEFAULT FALSE,
    penalty_percentage   NUMERIC(9, 2) NOT NULL DEFAULT 0.00,
    penalty_amount       NUMERIC(19, 4) NOT NULL DEFAULT 0.00,

    from_day             INT NOT NULL DEFAULT 0,
    till_day             INT NOT NULL DEFAULT 0,

    -- 0 = not-applicable (e.g. Broken Promises), 1 = applicable
    penalty_type         SMALLINT NOT NULL DEFAULT 1,

    -- Broken Promises (type=6) only
    promises_per_year    INT NOT NULL DEFAULT 0,
    promises_per_loan    INT NOT NULL DEFAULT 0,

    -- Early Settlement (type=1): true => use early_settlement_configs rows
    is_custom            BOOLEAN NOT NULL DEFAULT FALSE,

    -- Charity fund routing for late fees (Sharia: swept, not revenue)
    charity_fund_account VARCHAR(100),

    channel              VARCHAR(50) NOT NULL DEFAULT 'LMS',
    record_state         SMALLINT NOT NULL DEFAULT 1,  -- 1=Active, 0=Deleted
    version              INT NOT NULL DEFAULT 1,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by           UUID,
    updated_by           UUID,

    CONSTRAINT chk_delinquency_type      CHECK (delinquency_type BETWEEN 1 AND 6),
    CONSTRAINT chk_delinquency_penalty   CHECK (penalty_type IN (0, 1)),
    CONSTRAINT chk_delinquency_state     CHECK (record_state IN (0, 1))
);

-- One active rule per (tenant, product, delinquencyType)
CREATE UNIQUE INDEX idx_delinquency_rules_unique
    ON delinquency_rules(tenant_id, product_id, delinquency_type)
    WHERE record_state = 1;

CREATE INDEX idx_delinquency_rules_product ON delinquency_rules(tenant_id, product_id);
CREATE INDEX idx_delinquency_rules_type    ON delinquency_rules(tenant_id, delinquency_type);

-- ──────────────────────────────────────────────────────────────
-- 2. early_settlement_configs — singles + range configurations
--    (attached to a delinquency_rules row where delinquency_type=1)
-- ──────────────────────────────────────────────────────────────
CREATE TABLE early_settlement_configs (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delinquency_id       UUID NOT NULL REFERENCES delinquency_rules(id) ON DELETE CASCADE,

    invoice_order        INT NOT NULL,             -- installment # (1-based)
    from_day             INT NOT NULL DEFAULT 0,
    till_day             INT NOT NULL DEFAULT 0,

    is_percentage        BOOLEAN NOT NULL DEFAULT FALSE,
    discount_percentage  NUMERIC(9, 2) NOT NULL DEFAULT 0.00,
    discount_amount      NUMERIC(19, 4) NOT NULL DEFAULT 0.00,

    is_range             BOOLEAN NOT NULL DEFAULT FALSE,
    range_no             INT NOT NULL DEFAULT 0,   -- 0 for singles; >0 groups range rows
    min_invoice_order    INT,                      -- set on range rows to keep group metadata
    max_invoice_order    INT,

    channel              VARCHAR(50) NOT NULL DEFAULT 'LMS',
    record_state         SMALLINT NOT NULL DEFAULT 1,
    version              INT NOT NULL DEFAULT 1,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_es_state CHECK (record_state IN (0, 1))
);

CREATE INDEX idx_es_configs_delinquency ON early_settlement_configs(delinquency_id);
CREATE INDEX idx_es_configs_range       ON early_settlement_configs(delinquency_id, is_range, range_no);

-- ──────────────────────────────────────────────────────────────
-- 3. Auto-update trigger for updated_at + version
-- ──────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION touch_delinquency_row()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version   = COALESCE(OLD.version, 0) + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_delinquency_rules_touch
    BEFORE UPDATE ON delinquency_rules
    FOR EACH ROW EXECUTE FUNCTION touch_delinquency_row();

CREATE TRIGGER trg_es_configs_touch
    BEFORE UPDATE ON early_settlement_configs
    FOR EACH ROW EXECUTE FUNCTION touch_delinquency_row();

COMMENT ON TABLE delinquency_rules          IS 'Per-product delinquency rules — 6 lifecycle stages (Early Settlement → Broken Promises)';
COMMENT ON TABLE early_settlement_configs   IS 'Early Settlement custom-frequency configs: singles (isRange=false) + ranges (isRange=true, grouped by range_no)';

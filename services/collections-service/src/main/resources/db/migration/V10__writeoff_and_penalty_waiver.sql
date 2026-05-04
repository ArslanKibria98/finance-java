-- ============================================================
-- V10: Write-Off & Delinquency Management
--   - Installment write-off flags + waived penalty tracking
--   - write_off_records audit table
--   - penalty_waivers audit table
--   - delinquency_rule_audit config-change audit
--
-- Write-off is driven by the WRITE_OFFS DelinquencyRule (type=4)
-- stored in delinquency_rules (V6). When an installment's DPD
-- enters the rule's [fromDay, tillDay] window, the engine flips
-- is_eligible_for_writeoff = true. Admin may then trigger
-- write-off manually or via a batch run.
-- ============================================================

-- ──────────────────────────────────────────────────────────────
-- 1. Extend installments with write-off + waiver tracking
-- ──────────────────────────────────────────────────────────────
ALTER TABLE installments
    ADD COLUMN IF NOT EXISTS is_eligible_for_writeoff BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS eligibility_evaluated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS waived_penalty_amount    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS written_off_principal    NUMERIC(19, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS written_off_profit       NUMERIC(19, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS written_off_fee          NUMERIC(19, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS written_off_penalty      NUMERIC(19, 4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS write_off_date           DATE,
    ADD COLUMN IF NOT EXISTS write_off_reason         VARCHAR(500),
    ADD COLUMN IF NOT EXISTS written_off_by           UUID;

CREATE INDEX IF NOT EXISTS idx_installments_eligible_writeoff
    ON installments(tenant_id, is_eligible_for_writeoff)
    WHERE is_eligible_for_writeoff = TRUE;

CREATE INDEX IF NOT EXISTS idx_installments_write_off_date
    ON installments(tenant_id, write_off_date)
    WHERE write_off_date IS NOT NULL;

-- ──────────────────────────────────────────────────────────────
-- 2. write_off_records — one row per write-off transaction
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS write_off_records (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL,
    loan_id               UUID NOT NULL,
    schedule_id           UUID,
    installment_id        UUID,

    -- Which DelinquencyRule (type=4 WRITE_OFFS) drove the decision.
    -- NULL when the write-off is manual/override.
    delinquency_rule_id   UUID,

    principal_amount      NUMERIC(19, 4) NOT NULL DEFAULT 0,
    profit_amount         NUMERIC(19, 4) NOT NULL DEFAULT 0,
    fee_amount            NUMERIC(19, 4) NOT NULL DEFAULT 0,
    penalty_amount        NUMERIC(19, 4) NOT NULL DEFAULT 0,
    total_amount          NUMERIC(19, 4) NOT NULL,

    dpd_at_write_off      INT NOT NULL DEFAULT 0,

    -- MANUAL | AUTO_RULE | RECOVERY_REVERSAL
    trigger_type          VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    reason                VARCHAR(500) NOT NULL,
    approval_reference    VARCHAR(100),

    -- ACTIVE | REVERSED — reversals happen when a late recovery is booked
    status                VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reversal_reason       VARCHAR(500),
    reversed_at           TIMESTAMPTZ,
    reversed_by           UUID,

    write_off_date        DATE NOT NULL,
    initiated_by          UUID NOT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_writeoff_trigger CHECK (trigger_type IN ('MANUAL', 'AUTO_RULE', 'RECOVERY_REVERSAL')),
    CONSTRAINT chk_writeoff_status  CHECK (status IN ('ACTIVE', 'REVERSED')),
    CONSTRAINT chk_writeoff_totals  CHECK (total_amount = principal_amount + profit_amount + fee_amount + penalty_amount)
);

CREATE INDEX IF NOT EXISTS idx_writeoff_tenant_loan    ON write_off_records(tenant_id, loan_id);
CREATE INDEX IF NOT EXISTS idx_writeoff_installment    ON write_off_records(tenant_id, installment_id);
CREATE INDEX IF NOT EXISTS idx_writeoff_date           ON write_off_records(tenant_id, write_off_date);
CREATE INDEX IF NOT EXISTS idx_writeoff_status         ON write_off_records(tenant_id, status);

-- ──────────────────────────────────────────────────────────────
-- 3. penalty_waivers — one row per admin waiver action
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS penalty_waivers (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL,
    loan_id               UUID NOT NULL,
    installment_id        UUID NOT NULL,

    original_penalty      NUMERIC(19, 4) NOT NULL,
    waived_amount         NUMERIC(19, 4) NOT NULL,
    remaining_penalty     NUMERIC(19, 4) NOT NULL,

    -- FULL | PARTIAL
    waiver_type           VARCHAR(20) NOT NULL,
    reason                VARCHAR(500) NOT NULL,
    approval_reference    VARCHAR(100),

    waived_by             UUID NOT NULL,
    waived_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_waiver_type    CHECK (waiver_type IN ('FULL', 'PARTIAL')),
    CONSTRAINT chk_waiver_amounts CHECK (waived_amount >= 0 AND waived_amount <= original_penalty),
    CONSTRAINT chk_waiver_remaining CHECK (remaining_penalty = original_penalty - waived_amount)
);

CREATE INDEX IF NOT EXISTS idx_waiver_tenant_loan    ON penalty_waivers(tenant_id, loan_id);
CREATE INDEX IF NOT EXISTS idx_waiver_installment    ON penalty_waivers(tenant_id, installment_id);
CREATE INDEX IF NOT EXISTS idx_waiver_waived_at      ON penalty_waivers(tenant_id, waived_at);

-- ──────────────────────────────────────────────────────────────
-- 4. delinquency_rule_audit — config-change log
-- ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS delinquency_rule_audit (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             UUID NOT NULL,
    rule_id               UUID NOT NULL,
    product_id            UUID,
    delinquency_type      SMALLINT NOT NULL,

    -- CREATE | UPDATE | SOFT_DELETE
    action                VARCHAR(20) NOT NULL,
    before_snapshot       JSONB,
    after_snapshot        JSONB,

    actor_id              UUID NOT NULL,
    actor_role            VARCHAR(50),
    correlation_id        VARCHAR(100),

    changed_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_rule_audit_action CHECK (action IN ('CREATE', 'UPDATE', 'SOFT_DELETE'))
);

CREATE INDEX IF NOT EXISTS idx_rule_audit_tenant   ON delinquency_rule_audit(tenant_id, changed_at);
CREATE INDEX IF NOT EXISTS idx_rule_audit_rule     ON delinquency_rule_audit(tenant_id, rule_id);
CREATE INDEX IF NOT EXISTS idx_rule_audit_product  ON delinquency_rule_audit(tenant_id, product_id);

COMMENT ON TABLE write_off_records         IS 'Audit trail of all write-off actions with full amount breakdown + reversal support';
COMMENT ON TABLE penalty_waivers           IS 'Audit trail of admin penalty waivers (full or partial)';
COMMENT ON TABLE delinquency_rule_audit    IS 'Config-change log for delinquency rules (SAMA 7-year audit requirement)';

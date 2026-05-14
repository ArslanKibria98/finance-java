-- ============================================================================
-- WALLET WITHDRAWAL (CASH-OUT) SCHEMA
-- KSA Islamic Financing Platform
--
-- Withdrawal = funds leaving a wallet to an external destination (bank IBAN).
-- Flow: wallet-service -> ledger-service (Fineract withdraw) -> bank-rails adapter (SAMA SARIE mock).
-- SAGA: if bank-rails fails after Fineract debit, compensating deposit is posted.
-- ============================================================================

-- New movement purpose for outbound withdrawals
ALTER TYPE transaction_purpose ADD VALUE IF NOT EXISTS 'WITHDRAWAL_OUT';
ALTER TYPE transaction_purpose ADD VALUE IF NOT EXISTS 'WITHDRAWAL_FEE';
ALTER TYPE transaction_purpose ADD VALUE IF NOT EXISTS 'WITHDRAWAL_REFUND';

-- ============================================================================
-- ENUM TYPES
-- ============================================================================
CREATE TYPE withdrawal_status AS ENUM (
    'PENDING',
    'VALIDATED',
    'DEBITED',
    'BANK_SUBMITTED',
    'COMPLETED',
    'FAILED',
    'COMPENSATED',
    'CANCELLED'
);

CREATE TYPE withdrawal_channel AS ENUM (
    'BANK_IBAN',
    'INSTANT_SARIE',
    'INTERNAL_TRANSFER',
    'OWN_BANK_ACCOUNT'
);

-- ============================================================================
-- WITHDRAWAL LIMITS (extend wallets)
-- ============================================================================
ALTER TABLE wallets
    ADD COLUMN IF NOT EXISTS single_withdrawal_limit  NUMERIC(20,6) NOT NULL DEFAULT 5000,
    ADD COLUMN IF NOT EXISTS daily_withdrawal_limit   NUMERIC(20,6) NOT NULL DEFAULT 25000,
    ADD COLUMN IF NOT EXISTS monthly_withdrawal_limit NUMERIC(20,6) NOT NULL DEFAULT 100000,
    ADD COLUMN IF NOT EXISTS today_withdrawal_amount  NUMERIC(20,6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS month_withdrawal_amount  NUMERIC(20,6) NOT NULL DEFAULT 0;

-- ============================================================================
-- IBAN BENEFICIARIES (saved bank accounts per customer)
-- ============================================================================
CREATE TABLE wallet_iban_beneficiaries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    customer_id         UUID NOT NULL,
    wallet_id           UUID REFERENCES wallets(id),
    nickname            VARCHAR(100) NOT NULL,
    beneficiary_name    VARCHAR(200) NOT NULL,
    iban                VARCHAR(34)  NOT NULL,
    bank_code           VARCHAR(20),
    bank_name           VARCHAR(120),
    is_verified         BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at         TIMESTAMPTZ,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_iban_format CHECK (iban ~ '^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$'),
    CONSTRAINT uq_iban_per_customer UNIQUE (tenant_id, customer_id, iban)
);

CREATE INDEX idx_iban_ben_customer ON wallet_iban_beneficiaries(tenant_id, customer_id) WHERE is_active = TRUE;
CREATE INDEX idx_iban_ben_wallet   ON wallet_iban_beneficiaries(wallet_id) WHERE is_active = TRUE;

-- ============================================================================
-- WALLET WITHDRAWALS
-- ============================================================================
CREATE TABLE wallet_withdrawals (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                   UUID NOT NULL,
    withdrawal_number           VARCHAR(50) NOT NULL,
    source_wallet_id            UUID NOT NULL REFERENCES wallets(id),
    source_customer_id          UUID NOT NULL,
    channel                     withdrawal_channel NOT NULL DEFAULT 'BANK_IBAN',
    -- Destination bank info (snapshot at request time)
    destination_iban            VARCHAR(34)  NOT NULL,
    destination_bank_code       VARCHAR(20),
    destination_bank_name       VARCHAR(120),
    beneficiary_name            VARCHAR(200) NOT NULL,
    beneficiary_id              UUID REFERENCES wallet_iban_beneficiaries(id),
    -- Money
    amount                      NUMERIC(20,6) NOT NULL,
    fee_amount                  NUMERIC(20,6) NOT NULL DEFAULT 0,
    total_debit                 NUMERIC(20,6) NOT NULL,
    currency                    VARCHAR(3)    NOT NULL DEFAULT 'SAR',
    -- State
    status                      withdrawal_status NOT NULL DEFAULT 'PENDING',
    purpose_note                VARCHAR(280),
    purpose_code                VARCHAR(20),
    -- Movement & external refs
    debit_movement_id           UUID REFERENCES wallet_movements(id),
    fee_movement_id             UUID REFERENCES wallet_movements(id),
    refund_movement_id          UUID REFERENCES wallet_movements(id),
    fineract_debit_txn_id       VARCHAR(100),
    fineract_refund_txn_id      VARCHAR(100),
    bank_reference              VARCHAR(100),
    sarie_reference             VARCHAR(100),
    workflow_id                 VARCHAR(100),
    -- Idempotency & audit
    idempotency_key             VARCHAR(100) NOT NULL,
    initiator_user_id           UUID,
    initiator_ip                VARCHAR(45),
    initiator_device_id         VARCHAR(100),
    -- Error tracking
    error_code                  VARCHAR(50),
    error_message               TEXT,
    -- Timestamps
    initiated_at                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    debited_at                  TIMESTAMPTZ,
    bank_submitted_at           TIMESTAMPTZ,
    completed_at                TIMESTAMPTZ,
    failed_at                   TIMESTAMPTZ,
    compensated_at              TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                     INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_wd_amount_pos    CHECK (amount > 0),
    CONSTRAINT chk_wd_total_pos     CHECK (total_debit > 0),
    CONSTRAINT uq_wd_number         UNIQUE (tenant_id, withdrawal_number),
    CONSTRAINT uq_wd_idem           UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_wd_source       ON wallet_withdrawals(source_wallet_id, initiated_at DESC);
CREATE INDEX idx_wd_status       ON wallet_withdrawals(tenant_id, status) WHERE status IN ('PENDING','VALIDATED','DEBITED','BANK_SUBMITTED');
CREATE INDEX idx_wd_initiator    ON wallet_withdrawals(initiator_user_id, initiated_at DESC);
CREATE INDEX idx_wd_workflow     ON wallet_withdrawals(workflow_id) WHERE workflow_id IS NOT NULL;
CREATE INDEX idx_wd_iban         ON wallet_withdrawals(tenant_id, destination_iban);

-- ============================================================================
-- WALLET WITHDRAWAL STATUS HISTORY
-- ============================================================================
CREATE TABLE wallet_withdrawal_status_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    withdrawal_id   UUID NOT NULL REFERENCES wallet_withdrawals(id),
    from_status     withdrawal_status,
    to_status       withdrawal_status NOT NULL,
    changed_by      UUID,
    reason          VARCHAR(500),
    correlation_id  VARCHAR(100),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wd_history_withdrawal ON wallet_withdrawal_status_history(withdrawal_id, changed_at DESC);

-- ============================================================================
-- TRIGGERS
-- ============================================================================
CREATE TRIGGER trigger_withdrawals_updated
    BEFORE UPDATE ON wallet_withdrawals FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_iban_beneficiaries_updated
    BEFORE UPDATE ON wallet_iban_beneficiaries FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE FUNCTION track_withdrawal_status()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') OR (OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO wallet_withdrawal_status_history (
            tenant_id, withdrawal_id, from_status, to_status, reason
        ) VALUES (
            NEW.tenant_id,
            NEW.id,
            CASE WHEN TG_OP = 'INSERT' THEN NULL ELSE OLD.status END,
            NEW.status,
            NEW.error_message
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_withdrawal_status
    AFTER INSERT OR UPDATE ON wallet_withdrawals FOR EACH ROW EXECUTE FUNCTION track_withdrawal_status();

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================
ALTER TABLE wallet_withdrawals          ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallet_iban_beneficiaries   ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_wd ON wallet_withdrawals
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

CREATE POLICY tenant_isolation_iban ON wallet_iban_beneficiaries
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

COMMENT ON TABLE wallet_withdrawals             IS 'Outbound wallet withdrawals to bank IBAN (cash-out)';
COMMENT ON TABLE wallet_withdrawal_status_history IS 'Audit trail of withdrawal SAGA state transitions';
COMMENT ON TABLE wallet_iban_beneficiaries      IS 'Saved IBAN beneficiaries per customer';

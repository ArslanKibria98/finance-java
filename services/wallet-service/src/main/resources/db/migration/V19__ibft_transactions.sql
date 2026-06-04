-- ============================================================================
-- IBFT transactions (Scotia EFT). HOLD-based: funds held on initiate, finalized
-- (debited) on settlement success, released on failure. Reconciled by cron (INQUIRE).
-- ============================================================================
ALTER TYPE transaction_purpose ADD VALUE IF NOT EXISTS 'IBFT_HOLD';
ALTER TYPE transaction_purpose ADD VALUE IF NOT EXISTS 'IBFT_DEBIT';
ALTER TYPE transaction_purpose ADD VALUE IF NOT EXISTS 'IBFT_RELEASE';

CREATE TYPE ibft_status AS ENUM (
    'INITIATED',   -- created, pre-hold
    'HELD',        -- funds held (Fineract hold + reserved)
    'SUBMITTED',   -- Scotia EFT created + submitted
    'PROCESSING',  -- awaiting settlement (cron polls)
    'COMPLETED',   -- settled → final debit
    'FAILED',      -- create/submit failed OR settlement rejected → hold released
    'REJECTED'     -- explicit Scotia rejection
);

CREATE TABLE ibft_transactions (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL,
    ibft_number              VARCHAR(50) NOT NULL,
    customer_id              UUID NOT NULL,
    wallet_id                UUID NOT NULL REFERENCES wallets(id),
    beneficiary_id           UUID NOT NULL REFERENCES ibft_beneficiaries(id),
    debtor_corporate_account VARCHAR(34),
    creditor_account         VARCHAR(40),
    creditor_name            VARCHAR(200),
    amount                   NUMERIC(20,6) NOT NULL,
    fee_amount               NUMERIC(20,6) NOT NULL DEFAULT 0,
    currency                 VARCHAR(3) NOT NULL DEFAULT 'CAD',
    status                   ibft_status NOT NULL DEFAULT 'INITIATED',
    purpose_note             VARCHAR(280),
    end_to_end_id            VARCHAR(40),
    fineract_hold_txn_id     BIGINT,
    hold_movement_id         UUID REFERENCES wallet_movements(id),
    debit_movement_id        UUID REFERENCES wallet_movements(id),
    release_movement_id      UUID REFERENCES wallet_movements(id),
    ledger_entry_id          UUID,
    scotia_submission_id     VARCHAR(60),
    scotia_payment_id        VARCHAR(60),
    scotia_status            VARCHAR(60),
    idempotency_key          VARCHAR(100) NOT NULL,
    initiator_user_id        UUID,
    initiator_ip             VARCHAR(45),
    initiator_device_id      VARCHAR(100),
    inquiry_attempts         INT NOT NULL DEFAULT 0,
    last_inquired_at         TIMESTAMPTZ,
    error_code               VARCHAR(80),
    error_message            TEXT,
    initiated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at             TIMESTAMPTZ,
    settled_at               TIMESTAMPTZ,
    failed_at                TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                  INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_ibft_amount_pos CHECK (amount > 0),
    CONSTRAINT uq_ibft_number      UNIQUE (tenant_id, ibft_number),
    CONSTRAINT uq_ibft_idem        UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_ibft_wallet  ON ibft_transactions(wallet_id, initiated_at DESC);
CREATE INDEX idx_ibft_pending ON ibft_transactions(tenant_id, status) WHERE status IN ('SUBMITTED','PROCESSING');
CREATE INDEX idx_ibft_submission ON ibft_transactions(scotia_submission_id);

CREATE TRIGGER trigger_ibft_updated
    BEFORE UPDATE ON ibft_transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE ibft_transactions ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ibft_transactions
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

COMMENT ON TABLE ibft_transactions IS 'IBFT (Scotia EFT) transfers — HOLD/finalize/release lifecycle';

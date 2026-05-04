-- ============================================================================
-- WALLET-TO-WALLET TRANSFER SCHEMA
-- KSA Islamic Financing Platform
-- ============================================================================

-- New transaction purposes for transfer movements
ALTER TYPE transaction_purpose ADD VALUE IF NOT EXISTS 'TRANSFER_OUT';
ALTER TYPE transaction_purpose ADD VALUE IF NOT EXISTS 'TRANSFER_IN';
ALTER TYPE transaction_purpose ADD VALUE IF NOT EXISTS 'TRANSFER_FEE';

-- Transfer state machine
CREATE TYPE transfer_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'REVERSED',
    'CANCELLED'
);

CREATE TYPE transfer_channel AS ENUM (
    'P2P_WALLET_ID',
    'P2P_WALLET_NUMBER',
    'P2P_MOBILE',
    'P2P_IBAN_INTERNAL'
);

-- ============================================================================
-- WALLET TRANSFER LIMITS (extend wallets)
-- ============================================================================
ALTER TABLE wallets
    ADD COLUMN IF NOT EXISTS single_transfer_limit  NUMERIC(20,6) NOT NULL DEFAULT 5000,
    ADD COLUMN IF NOT EXISTS daily_transfer_limit   NUMERIC(20,6) NOT NULL DEFAULT 25000,
    ADD COLUMN IF NOT EXISTS monthly_transfer_limit NUMERIC(20,6) NOT NULL DEFAULT 100000,
    ADD COLUMN IF NOT EXISTS today_transfer_amount  NUMERIC(20,6) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS month_transfer_amount  NUMERIC(20,6) NOT NULL DEFAULT 0;

-- ============================================================================
-- WALLET TRANSFERS
-- ============================================================================
CREATE TABLE wallet_transfers (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL,
    transfer_number          VARCHAR(50) NOT NULL,
    source_wallet_id         UUID NOT NULL REFERENCES wallets(id),
    destination_wallet_id    UUID NOT NULL REFERENCES wallets(id),
    source_customer_id       UUID NOT NULL,
    destination_customer_id  UUID NOT NULL,
    channel                  transfer_channel NOT NULL DEFAULT 'P2P_WALLET_ID',
    amount                   NUMERIC(20,6) NOT NULL,
    fee_amount               NUMERIC(20,6) NOT NULL DEFAULT 0,
    total_debit              NUMERIC(20,6) NOT NULL,
    currency                 VARCHAR(3)    NOT NULL DEFAULT 'SAR',
    status                   transfer_status NOT NULL DEFAULT 'PENDING',
    purpose_note             VARCHAR(280),
    debit_movement_id        UUID REFERENCES wallet_movements(id),
    credit_movement_id       UUID REFERENCES wallet_movements(id),
    fee_movement_id          UUID REFERENCES wallet_movements(id),
    ledger_entry_id          UUID,
    fineract_transfer_id     VARCHAR(100),
    workflow_id              VARCHAR(100),
    idempotency_key          VARCHAR(100) NOT NULL,
    initiator_user_id        UUID,
    initiator_ip             VARCHAR(45),
    initiator_device_id      VARCHAR(100),
    error_code               VARCHAR(50),
    error_message            TEXT,
    initiated_at             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at             TIMESTAMPTZ,
    reversed_at              TIMESTAMPTZ,
    reversal_of_transfer_id  UUID REFERENCES wallet_transfers(id),
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                  INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_transfer_amount_pos     CHECK (amount > 0),
    CONSTRAINT chk_transfer_total_pos      CHECK (total_debit > 0),
    CONSTRAINT chk_transfer_not_self       CHECK (source_wallet_id <> destination_wallet_id),
    CONSTRAINT uq_transfer_number          UNIQUE (tenant_id, transfer_number),
    CONSTRAINT uq_transfer_idem            UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_transfers_source      ON wallet_transfers(source_wallet_id, initiated_at DESC);
CREATE INDEX idx_transfers_destination ON wallet_transfers(destination_wallet_id, initiated_at DESC);
CREATE INDEX idx_transfers_status      ON wallet_transfers(tenant_id, status) WHERE status IN ('PENDING','PROCESSING');
CREATE INDEX idx_transfers_initiator   ON wallet_transfers(initiator_user_id, initiated_at DESC);
CREATE INDEX idx_transfers_workflow    ON wallet_transfers(workflow_id) WHERE workflow_id IS NOT NULL;

-- ============================================================================
-- WALLET TRANSFER STATUS HISTORY
-- ============================================================================
CREATE TABLE wallet_transfer_status_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    transfer_id     UUID NOT NULL REFERENCES wallet_transfers(id),
    from_status     transfer_status,
    to_status       transfer_status NOT NULL,
    changed_by      UUID,
    reason          VARCHAR(500),
    correlation_id  VARCHAR(100),
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transfer_history_transfer ON wallet_transfer_status_history(transfer_id, changed_at DESC);

-- ============================================================================
-- TRIGGERS
-- ============================================================================
CREATE TRIGGER trigger_transfers_updated
    BEFORE UPDATE ON wallet_transfers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE FUNCTION track_transfer_status()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'INSERT') OR (OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO wallet_transfer_status_history (
            tenant_id, transfer_id, from_status, to_status, reason
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

CREATE TRIGGER trigger_transfer_status
    AFTER INSERT OR UPDATE ON wallet_transfers FOR EACH ROW EXECUTE FUNCTION track_transfer_status();

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================
ALTER TABLE wallet_transfers ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON wallet_transfers
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

COMMENT ON TABLE wallet_transfers IS 'Wallet-to-wallet P2P transfer transactions';
COMMENT ON TABLE wallet_transfer_status_history IS 'Audit trail of transfer state transitions';

-- ============================================================================
-- DEMO SEED DATA (for mock scenario testing)
-- Three customers + wallets in single tenant '11111111-1111-1111-1111-111111111111'
-- ============================================================================
INSERT INTO wallets (
    id, tenant_id, wallet_number, customer_id,
    available_balance, reserved_balance, currency, status,
    daily_top_up_limit, monthly_top_up_limit, single_top_up_limit,
    single_transfer_limit, daily_transfer_limit, monthly_transfer_limit,
    auto_debit_enabled
) VALUES
    -- Customer A: Active, 10,000 SAR
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     '11111111-1111-1111-1111-111111111111',
     'WLT-DEMO-A',
     'a0000000-0000-0000-0000-000000000001',
     10000.000000, 0, 'SAR', 'ACTIVE',
     50000, 200000, 10000,
     5000, 25000, 100000,
     true),
    -- Customer B: Active, 5,000 SAR
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
     '11111111-1111-1111-1111-111111111111',
     'WLT-DEMO-B',
     'b0000000-0000-0000-0000-000000000002',
     5000.000000, 0, 'SAR', 'ACTIVE',
     50000, 200000, 10000,
     5000, 25000, 100000,
     true),
    -- Customer C: FROZEN (for negative test)
    ('cccccccc-cccc-cccc-cccc-cccccccccccc',
     '11111111-1111-1111-1111-111111111111',
     'WLT-DEMO-C',
     'c0000000-0000-0000-0000-000000000003',
     0, 0, 'SAR', 'FROZEN',
     50000, 200000, 10000,
     5000, 25000, 100000,
     true)
ON CONFLICT (tenant_id, wallet_number) DO NOTHING;

-- ============================================================================
-- WALLET SERVICE DATABASE SCHEMA
-- PostgreSQL 16+ Production Schema
-- KSA Islamic Financing Platform
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE wallet_status AS ENUM (
    'PENDING_ACTIVATION',
    'ACTIVE',
    'FROZEN',
    'SUSPENDED',
    'CLOSED'
);

CREATE TYPE movement_type AS ENUM (
    'CREDIT',
    'DEBIT'
);

CREATE TYPE transaction_purpose AS ENUM (
    'TOP_UP',
    'LOAN_PROCEEDS',
    'INSTALLMENT_PAYMENT',
    'EARLY_SETTLEMENT',
    'FEE_DEDUCTION',
    'REFUND',
    'REVERSAL',
    'WITHDRAWAL',
    'ADJUSTMENT'
);

CREATE TYPE top_up_method AS ENUM (
    'MADA',
    'APPLE_PAY',
    'SADAD',
    'BANK_TRANSFER',
    'INTERNAL'
);

CREATE TYPE top_up_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'CANCELLED',
    'REFUNDED'
);

CREATE TYPE reservation_status AS ENUM (
    'ACTIVE',
    'CAPTURED',
    'RELEASED',
    'EXPIRED'
);

CREATE TYPE sync_status AS ENUM (
    'PENDING',
    'SYNCED',
    'FAILED'
);

-- ============================================================================
-- WALLET TABLES
-- ============================================================================

CREATE TABLE wallets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    wallet_number VARCHAR(50) NOT NULL,
    customer_id UUID NOT NULL,
    available_balance NUMERIC(20, 6) NOT NULL DEFAULT 0,
    reserved_balance NUMERIC(20, 6) NOT NULL DEFAULT 0,
    total_balance NUMERIC(20, 6) GENERATED ALWAYS AS (available_balance + reserved_balance) STORED,
    currency VARCHAR(3) NOT NULL DEFAULT 'SAR',
    status wallet_status NOT NULL DEFAULT 'PENDING_ACTIVATION',
    daily_top_up_limit NUMERIC(20, 6) NOT NULL DEFAULT 50000,
    monthly_top_up_limit NUMERIC(20, 6) NOT NULL DEFAULT 200000,
    single_top_up_limit NUMERIC(20, 6) NOT NULL DEFAULT 10000,
    today_top_up_amount NUMERIC(20, 6) NOT NULL DEFAULT 0,
    month_top_up_amount NUMERIC(20, 6) NOT NULL DEFAULT 0,
    fineract_savings_account_id BIGINT,
    ledger_synced BOOLEAN NOT NULL DEFAULT FALSE,
    last_ledger_sync_at TIMESTAMPTZ,
    auto_debit_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT chk_available_balance CHECK (available_balance >= 0),
    CONSTRAINT chk_reserved_balance CHECK (reserved_balance >= 0),
    CONSTRAINT uq_wallet_number UNIQUE (tenant_id, wallet_number),
    CONSTRAINT uq_customer_wallet UNIQUE (tenant_id, customer_id)
);

CREATE TABLE wallet_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    from_status wallet_status,
    to_status wallet_status NOT NULL,
    changed_by UUID,
    change_reason VARCHAR(500),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE wallet_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    movement_number VARCHAR(50) NOT NULL,
    movement_type movement_type NOT NULL,
    purpose transaction_purpose NOT NULL,
    amount NUMERIC(20, 6) NOT NULL,
    balance_before NUMERIC(20, 6) NOT NULL,
    balance_after NUMERIC(20, 6) NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    description VARCHAR(500),
    idempotency_key VARCHAR(100) NOT NULL,
    ledger_sync_status sync_status NOT NULL DEFAULT 'PENDING',
    ledger_entry_id UUID,
    ledger_synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_movement_amount CHECK (amount > 0),
    CONSTRAINT uq_movement_number UNIQUE (tenant_id, movement_number),
    CONSTRAINT uq_movement_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE balance_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    snapshot_date DATE NOT NULL,
    opening_balance NUMERIC(20, 6) NOT NULL,
    closing_balance NUMERIC(20, 6) NOT NULL,
    reserved_balance NUMERIC(20, 6) NOT NULL,
    total_credits NUMERIC(20, 6) NOT NULL DEFAULT 0,
    total_debits NUMERIC(20, 6) NOT NULL DEFAULT 0,
    movement_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wallet_snapshot UNIQUE (wallet_id, snapshot_date)
);

CREATE TABLE reserved_funds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    reservation_number VARCHAR(50) NOT NULL,
    amount NUMERIC(20, 6) NOT NULL,
    reference_type VARCHAR(50) NOT NULL,
    reference_id UUID NOT NULL,
    status reservation_status NOT NULL DEFAULT 'ACTIVE',
    reserved_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    captured_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    movement_id UUID REFERENCES wallet_movements(id),
    idempotency_key VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_reserved_amount CHECK (amount > 0),
    CONSTRAINT uq_reservation_number UNIQUE (tenant_id, reservation_number),
    CONSTRAINT uq_reservation_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE top_up_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    transaction_number VARCHAR(50) NOT NULL,
    method top_up_method NOT NULL,
    amount NUMERIC(20, 6) NOT NULL,
    fee_amount NUMERIC(20, 6) NOT NULL DEFAULT 0,
    net_amount NUMERIC(20, 6) NOT NULL,
    source_card_last_four VARCHAR(4),
    source_card_brand VARCHAR(20),
    source_iban VARCHAR(34),
    provider_transaction_id VARCHAR(100),
    provider_reference VARCHAR(100),
    status top_up_status NOT NULL DEFAULT 'PENDING',
    movement_id UUID REFERENCES wallet_movements(id),
    idempotency_key VARCHAR(100) NOT NULL,
    error_code VARCHAR(50),
    error_message TEXT,
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_top_up_amount CHECK (amount > 0),
    CONSTRAINT uq_top_up_txn UNIQUE (tenant_id, transaction_number),
    CONSTRAINT uq_top_up_idempotency UNIQUE (tenant_id, idempotency_key)
);

CREATE TABLE top_up_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    top_up_id UUID NOT NULL REFERENCES top_up_transactions(id),
    from_status top_up_status,
    to_status top_up_status NOT NULL,
    provider_status VARCHAR(50),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE scheduled_debits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    wallet_id UUID NOT NULL REFERENCES wallets(id),
    loan_id UUID NOT NULL,
    installment_id UUID NOT NULL,
    due_date DATE NOT NULL,
    amount NUMERIC(20, 6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    processed_at TIMESTAMPTZ,
    movement_id UUID REFERENCES wallet_movements(id),
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,
    next_attempt_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_scheduled_debit UNIQUE (wallet_id, installment_id)
);

CREATE TABLE ledger_sync_status (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    movement_id UUID NOT NULL REFERENCES wallet_movements(id),
    sync_status sync_status NOT NULL DEFAULT 'PENDING',
    ledger_entry_id UUID,
    fineract_transaction_id BIGINT,
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ,
    error_code VARCHAR(50),
    error_message TEXT,
    synced_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_movement_sync UNIQUE (movement_id)
);

CREATE TABLE change_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    operation VARCHAR(20) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    changed_by UUID,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(100),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_wallets_customer ON wallets(customer_id);
CREATE INDEX idx_wallets_status ON wallets(tenant_id, status);
CREATE INDEX idx_wallets_fineract ON wallets(fineract_savings_account_id) WHERE fineract_savings_account_id IS NOT NULL;

CREATE INDEX idx_movements_wallet ON wallet_movements(wallet_id);
CREATE INDEX idx_movements_reference ON wallet_movements(reference_type, reference_id);
CREATE INDEX idx_movements_unsynced ON wallet_movements(tenant_id) WHERE ledger_sync_status = 'PENDING';

CREATE INDEX idx_snapshots_wallet ON balance_snapshots(wallet_id);
CREATE INDEX idx_snapshots_date ON balance_snapshots(snapshot_date);

CREATE INDEX idx_reserved_wallet ON reserved_funds(wallet_id);
CREATE INDEX idx_reserved_active ON reserved_funds(tenant_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_reserved_expiry ON reserved_funds(expires_at) WHERE status = 'ACTIVE';

CREATE INDEX idx_top_up_wallet ON top_up_transactions(wallet_id);
CREATE INDEX idx_top_up_status ON top_up_transactions(tenant_id, status);

CREATE INDEX idx_scheduled_pending ON scheduled_debits(due_date) WHERE status = 'PENDING';

CREATE INDEX idx_ledger_sync_pending ON ledger_sync_status(tenant_id) WHERE sync_status = 'PENDING';

CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published = FALSE;

-- ============================================================================
-- TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version = COALESCE(OLD.version, 0) + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_wallets_updated
    BEFORE UPDATE ON wallets FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_reservations_updated
    BEFORE UPDATE ON reserved_funds FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_top_ups_updated
    BEFORE UPDATE ON top_up_transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE OR REPLACE FUNCTION track_wallet_status()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO wallet_status_history (tenant_id, wallet_id, from_status, to_status)
        VALUES (NEW.tenant_id, NEW.id, OLD.status, NEW.status);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_wallet_status
    AFTER UPDATE ON wallets FOR EACH ROW EXECUTE FUNCTION track_wallet_status();

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE wallets ENABLE ROW LEVEL SECURITY;
ALTER TABLE wallet_movements ENABLE ROW LEVEL SECURITY;
ALTER TABLE reserved_funds ENABLE ROW LEVEL SECURITY;
ALTER TABLE top_up_transactions ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON wallets
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON wallet_movements
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON reserved_funds
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE wallets IS 'Closed-loop customer wallets with balance tracking';
COMMENT ON TABLE wallet_movements IS 'Wallet transaction ledger with ledger sync status';
COMMENT ON TABLE balance_snapshots IS 'End-of-day balance snapshots for reconciliation';
COMMENT ON TABLE reserved_funds IS 'Funds reserved for pending auto-debits';
COMMENT ON TABLE ledger_sync_status IS 'Track GL ledger sync per movement';

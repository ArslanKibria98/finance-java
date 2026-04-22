-- ============================================================================
-- LEDGER SERVICE DATABASE SCHEMA
-- PostgreSQL 16+ High-Fidelity Production Schema
-- KSA Islamic Financing Platform
-- ============================================================================
-- Double-entry accounting ledger — bridge between domain services and
-- Apache Fineract. Covers GL accounts, journal entries, Fineract sync,
-- reconciliation, accruals, period closes, and outbox events.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE account_type AS ENUM (
    'ASSET',
    'LIABILITY',
    'EQUITY',
    'INCOME',
    'EXPENSE',
    'OFF_BALANCE'
);

CREATE TYPE account_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'CLOSED',
    'SUSPENDED'
);

CREATE TYPE entry_status AS ENUM (
    'PENDING',
    'POSTED',
    'REVERSED',
    'FAILED'
);

CREATE TYPE sync_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'SYNCED',
    'FAILED',
    'RETRY_SCHEDULED'
);

CREATE TYPE reconciliation_status AS ENUM (
    'PENDING',
    'MATCHED',
    'DISCREPANCY',
    'RESOLVED',
    'WRITE_OFF'
);

CREATE TYPE accrual_status AS ENUM (
    'SCHEDULED',
    'CALCULATED',
    'POSTED',
    'REVERSED'
);

CREATE TYPE accrual_type AS ENUM (
    'PROFIT_INCOME',
    'FEE_INCOME',
    'LATE_FEE',
    'PROVISION'
);

-- ============================================================================
-- TRIGGER FUNCTION: update_updated_at_column
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TABLE 1: fineract_account_mappings
-- Maps internal GL accounts to Fineract GL account codes per tenant
-- ============================================================================

CREATE TABLE fineract_account_mappings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    internal_account_code   VARCHAR(50) NOT NULL,
    fineract_account_id     BIGINT NOT NULL,
    fineract_gl_code        VARCHAR(50) NOT NULL,
    account_type            account_type NOT NULL,
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    last_synced_at          TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_fineract_account_mapping UNIQUE (tenant_id, internal_account_code)
);

CREATE INDEX idx_fam_tenant ON fineract_account_mappings (tenant_id);
CREATE INDEX idx_fam_tenant_active ON fineract_account_mappings (tenant_id, is_active);
CREATE INDEX idx_fam_fineract_id ON fineract_account_mappings (tenant_id, fineract_account_id);

CREATE TRIGGER trg_fam_updated_at
    BEFORE UPDATE ON fineract_account_mappings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 2: fineract_loan_mappings
-- Maps internal loan IDs to Fineract loan account IDs per tenant
-- ============================================================================

CREATE TABLE fineract_loan_mappings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    internal_loan_id        UUID NOT NULL,
    fineract_loan_id        BIGINT NOT NULL,
    fineract_client_id      BIGINT NOT NULL,
    fineract_loan_product_id BIGINT,
    sync_status             sync_status NOT NULL DEFAULT 'PENDING',
    last_synced_at          TIMESTAMPTZ,
    sync_error_message      TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_fineract_loan_mapping UNIQUE (tenant_id, internal_loan_id)
);

CREATE INDEX idx_flm_tenant ON fineract_loan_mappings (tenant_id);
CREATE INDEX idx_flm_internal_loan ON fineract_loan_mappings (tenant_id, internal_loan_id);
CREATE INDEX idx_flm_fineract_loan ON fineract_loan_mappings (tenant_id, fineract_loan_id);
CREATE INDEX idx_flm_sync_status ON fineract_loan_mappings (tenant_id, sync_status);

CREATE TRIGGER trg_flm_updated_at
    BEFORE UPDATE ON fineract_loan_mappings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 3: fineract_customer_mappings
-- Maps internal customer (CIF) IDs to Fineract client IDs per tenant
-- ============================================================================

CREATE TABLE fineract_customer_mappings (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    internal_customer_id    UUID NOT NULL,
    fineract_client_id      BIGINT NOT NULL,
    fineract_account_no     VARCHAR(50),
    sync_status             sync_status NOT NULL DEFAULT 'PENDING',
    last_synced_at          TIMESTAMPTZ,
    sync_error_message      TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_fineract_customer_mapping UNIQUE (tenant_id, internal_customer_id)
);

CREATE INDEX idx_fcm_tenant ON fineract_customer_mappings (tenant_id);
CREATE INDEX idx_fcm_internal_customer ON fineract_customer_mappings (tenant_id, internal_customer_id);
CREATE INDEX idx_fcm_fineract_client ON fineract_customer_mappings (tenant_id, fineract_client_id);
CREATE INDEX idx_fcm_sync_status ON fineract_customer_mappings (tenant_id, sync_status);

CREATE TRIGGER trg_fcm_updated_at
    BEFORE UPDATE ON fineract_customer_mappings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 4: idempotency_keys
-- Prevents duplicate financial operations via idempotency key deduplication
-- ============================================================================

CREATE TABLE idempotency_keys (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    idempotency_key         VARCHAR(100) NOT NULL,
    operation_type          VARCHAR(100) NOT NULL,
    reference_id            UUID,
    response_payload        JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at              TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '24 hours'),
    CONSTRAINT uq_idempotency_key UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_idk_tenant ON idempotency_keys (tenant_id);
CREATE INDEX idx_idk_tenant_key ON idempotency_keys (tenant_id, idempotency_key);
CREATE INDEX idx_idk_expires_at ON idempotency_keys (expires_at);

-- ============================================================================
-- TABLE 5: journal_entry_sync_log
-- Tracks sync attempts of journal entries to Fineract with retry support
-- ============================================================================

CREATE TABLE journal_entry_sync_log (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    journal_entry_id        UUID NOT NULL,
    fineract_transaction_id VARCHAR(100),
    sync_status             sync_status NOT NULL DEFAULT 'PENDING',
    attempt_count           INT NOT NULL DEFAULT 0,
    max_attempts            INT NOT NULL DEFAULT 3,
    last_attempt_at         TIMESTAMPTZ,
    next_retry_at           TIMESTAMPTZ,
    error_message           TEXT,
    error_code              VARCHAR(100),
    request_payload         JSONB,
    response_payload        JSONB,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_jesl_tenant ON journal_entry_sync_log (tenant_id);
CREATE INDEX idx_jesl_journal_entry ON journal_entry_sync_log (tenant_id, journal_entry_id);
CREATE INDEX idx_jesl_sync_status ON journal_entry_sync_log (tenant_id, sync_status);
CREATE INDEX idx_jesl_next_retry ON journal_entry_sync_log (next_retry_at) WHERE sync_status IN ('FAILED', 'RETRY_SCHEDULED');

CREATE TRIGGER trg_jesl_updated_at
    BEFORE UPDATE ON journal_entry_sync_log
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 6: gl_reconciliation_records
-- Reconciliation records between internal ledger and Fineract GL balances
-- ============================================================================

CREATE TABLE gl_reconciliation_records (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    account_id              UUID NOT NULL,
    reconciliation_date     DATE NOT NULL,
    internal_balance        NUMERIC(20, 6) NOT NULL DEFAULT 0,
    fineract_balance        NUMERIC(20, 6) NOT NULL DEFAULT 0,
    discrepancy_amount      NUMERIC(20, 6) NOT NULL DEFAULT 0,
    status                  reconciliation_status NOT NULL DEFAULT 'PENDING',
    resolved_by             UUID,
    resolved_at             TIMESTAMPTZ,
    resolution_notes        TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_gl_reconciliation UNIQUE (tenant_id, account_id, reconciliation_date)
);

CREATE INDEX idx_glrr_tenant ON gl_reconciliation_records (tenant_id);
CREATE INDEX idx_glrr_account ON gl_reconciliation_records (tenant_id, account_id);
CREATE INDEX idx_glrr_date ON gl_reconciliation_records (tenant_id, reconciliation_date);
CREATE INDEX idx_glrr_status ON gl_reconciliation_records (tenant_id, status);

CREATE TRIGGER trg_glrr_updated_at
    BEFORE UPDATE ON gl_reconciliation_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 7: sync_failures
-- Persistent log of all Fineract sync failures for monitoring and alerting
-- ============================================================================

CREATE TABLE sync_failures (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    entity_type             VARCHAR(50) NOT NULL,
    entity_id               UUID NOT NULL,
    failure_reason          TEXT NOT NULL,
    error_code              VARCHAR(100),
    stack_trace             TEXT,
    attempt_number          INT NOT NULL DEFAULT 1,
    retry_count             INT NOT NULL DEFAULT 0,
    is_resolved             BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by             UUID,
    resolved_at             TIMESTAMPTZ,
    resolution_notes        TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_sf_tenant ON sync_failures (tenant_id);
CREATE INDEX idx_sf_entity ON sync_failures (tenant_id, entity_type, entity_id);
CREATE INDEX idx_sf_unresolved ON sync_failures (tenant_id, is_resolved) WHERE is_resolved = FALSE;
CREATE INDEX idx_sf_created_at ON sync_failures (tenant_id, created_at DESC);

CREATE TRIGGER trg_sf_updated_at
    BEFORE UPDATE ON sync_failures
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 8: accounts
-- Chart of accounts — the core GL account registry
-- ============================================================================

CREATE TABLE accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    account_code            VARCHAR(50) NOT NULL,
    account_name            VARCHAR(255) NOT NULL,
    account_name_ar         VARCHAR(255),
    account_type            account_type NOT NULL,
    status                  account_status NOT NULL DEFAULT 'ACTIVE',
    parent_account_id       UUID REFERENCES accounts (id),
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'SAR',
    is_control_account      BOOLEAN NOT NULL DEFAULT FALSE,
    is_reconcilable         BOOLEAN NOT NULL DEFAULT TRUE,
    normal_balance          VARCHAR(6) NOT NULL DEFAULT 'DEBIT'
                                CHECK (normal_balance IN ('DEBIT', 'CREDIT')),
    description             TEXT,
    fineract_gl_account_id  BIGINT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_account_code UNIQUE (tenant_id, account_code)
);

CREATE INDEX idx_acc_tenant ON accounts (tenant_id);
CREATE INDEX idx_acc_tenant_status ON accounts (tenant_id, status);
CREATE INDEX idx_acc_type ON accounts (tenant_id, account_type);
CREATE INDEX idx_acc_parent ON accounts (tenant_id, parent_account_id);
CREATE INDEX idx_acc_fineract ON accounts (tenant_id, fineract_gl_account_id);

CREATE TRIGGER trg_acc_updated_at
    BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 9: journal_entries
-- Immutable journal entry header — the atomic unit of double-entry bookkeeping
-- ============================================================================

CREATE TABLE journal_entries (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    entry_number            VARCHAR(50) NOT NULL,
    entry_date              DATE NOT NULL,
    value_date              DATE NOT NULL,
    posting_date            TIMESTAMPTZ,
    status                  entry_status NOT NULL DEFAULT 'PENDING',
    description             TEXT,
    reference_type          VARCHAR(100),
    reference_id            UUID,
    source_service          VARCHAR(100),
    total_debit             NUMERIC(20, 6) NOT NULL DEFAULT 0,
    total_credit            NUMERIC(20, 6) NOT NULL DEFAULT 0,
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'SAR',
    reversal_of_entry_id    UUID REFERENCES journal_entries (id),
    reversed_by_entry_id    UUID REFERENCES journal_entries (id),
    fineract_transaction_id VARCHAR(100),
    sync_status             sync_status NOT NULL DEFAULT 'PENDING',
    idempotency_key         VARCHAR(100),
    posted_by               UUID,
    reversed_by             UUID,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_journal_entry_number UNIQUE (tenant_id, entry_number),
    CONSTRAINT uq_journal_entry_idempotency UNIQUE (tenant_id, idempotency_key),
    CONSTRAINT chk_journal_entry_balanced CHECK (total_debit = total_credit)
);

CREATE INDEX idx_je_tenant ON journal_entries (tenant_id);
CREATE INDEX idx_je_status ON journal_entries (tenant_id, status);
CREATE INDEX idx_je_entry_date ON journal_entries (tenant_id, entry_date);
CREATE INDEX idx_je_reference ON journal_entries (tenant_id, reference_type, reference_id);
CREATE INDEX idx_je_sync_status ON journal_entries (tenant_id, sync_status);
CREATE INDEX idx_je_fineract_txn ON journal_entries (tenant_id, fineract_transaction_id);
CREATE INDEX idx_je_idempotency ON journal_entries (tenant_id, idempotency_key);

CREATE TRIGGER trg_je_updated_at
    BEFORE UPDATE ON journal_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TRIGGER FUNCTION: track_entry_status_change
-- Automatically records every status transition in entry_status_history
-- ============================================================================

CREATE OR REPLACE FUNCTION track_entry_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'UPDATE' AND OLD.status IS DISTINCT FROM NEW.status) THEN
        INSERT INTO entry_status_history (
            tenant_id,
            journal_entry_id,
            previous_status,
            new_status,
            changed_by,
            changed_at,
            change_reason
        ) VALUES (
            NEW.tenant_id,
            NEW.id,
            OLD.status,
            NEW.status,
            NEW.updated_by,
            NOW(),
            NULL
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TABLE 10: journal_lines
-- Individual debit/credit lines within a journal entry (must balance per entry)
-- ============================================================================

CREATE TABLE journal_lines (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    journal_entry_id        UUID NOT NULL REFERENCES journal_entries (id) ON DELETE CASCADE,
    account_id              UUID NOT NULL REFERENCES accounts (id),
    line_number             INT NOT NULL,
    debit_amount            NUMERIC(20, 6) NOT NULL DEFAULT 0,
    credit_amount           NUMERIC(20, 6) NOT NULL DEFAULT 0,
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'SAR',
    description             TEXT,
    reference_type          VARCHAR(100),
    reference_id            UUID,
    cost_center             VARCHAR(50),
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_journal_line_sign CHECK (
        (debit_amount > 0 AND credit_amount = 0) OR
        (credit_amount > 0 AND debit_amount = 0)
    ),
    CONSTRAINT uq_journal_line_number UNIQUE (journal_entry_id, line_number)
);

CREATE INDEX idx_jl_tenant ON journal_lines (tenant_id);
CREATE INDEX idx_jl_journal_entry ON journal_lines (tenant_id, journal_entry_id);
CREATE INDEX idx_jl_account ON journal_lines (tenant_id, account_id);
CREATE INDEX idx_jl_reference ON journal_lines (tenant_id, reference_type, reference_id);

CREATE TRIGGER trg_jl_updated_at
    BEFORE UPDATE ON journal_lines
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 11: account_balances
-- Running balances per account per day — updated on every posted entry
-- ============================================================================

CREATE TABLE account_balances (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    account_id              UUID NOT NULL REFERENCES accounts (id),
    balance_date            DATE NOT NULL,
    opening_balance         NUMERIC(20, 6) NOT NULL DEFAULT 0,
    total_debits            NUMERIC(20, 6) NOT NULL DEFAULT 0,
    total_credits           NUMERIC(20, 6) NOT NULL DEFAULT 0,
    closing_balance         NUMERIC(20, 6) NOT NULL DEFAULT 0,
    currency_code           VARCHAR(3) NOT NULL DEFAULT 'SAR',
    entry_count             INT NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_account_balance_date UNIQUE (tenant_id, account_id, balance_date)
);

CREATE INDEX idx_ab_tenant ON account_balances (tenant_id);
CREATE INDEX idx_ab_account ON account_balances (tenant_id, account_id);
CREATE INDEX idx_ab_date ON account_balances (tenant_id, balance_date);
CREATE INDEX idx_ab_account_date ON account_balances (tenant_id, account_id, balance_date DESC);

CREATE TRIGGER trg_ab_updated_at
    BEFORE UPDATE ON account_balances
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 12: accrual_schedules
-- Profit/fee accrual schedules for Murabaha and Ijara financing products
-- ============================================================================

CREATE TABLE accrual_schedules (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    loan_id                 UUID NOT NULL,
    accrual_type            accrual_type NOT NULL,
    accrual_date            DATE NOT NULL,
    scheduled_amount        NUMERIC(20, 6) NOT NULL,
    accrued_amount          NUMERIC(20, 6) NOT NULL DEFAULT 0,
    status                  accrual_status NOT NULL DEFAULT 'SCHEDULED',
    debit_account_id        UUID REFERENCES accounts (id),
    credit_account_id       UUID REFERENCES accounts (id),
    journal_entry_id        UUID REFERENCES journal_entries (id),
    posted_at               TIMESTAMPTZ,
    reversal_date           DATE,
    reversed_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_as_tenant ON accrual_schedules (tenant_id);
CREATE INDEX idx_as_loan ON accrual_schedules (tenant_id, loan_id);
CREATE INDEX idx_as_accrual_date ON accrual_schedules (tenant_id, accrual_date);
CREATE INDEX idx_as_status ON accrual_schedules (tenant_id, status);
CREATE INDEX idx_as_type_date ON accrual_schedules (tenant_id, accrual_type, accrual_date);

CREATE TRIGGER trg_as_updated_at
    BEFORE UPDATE ON accrual_schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 13: period_closes
-- Monthly/annual accounting period close management
-- ============================================================================

CREATE TABLE period_closes (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    period_year             SMALLINT NOT NULL,
    period_month            SMALLINT NOT NULL CHECK (period_month BETWEEN 1 AND 12),
    close_date              DATE NOT NULL,
    is_closed               BOOLEAN NOT NULL DEFAULT FALSE,
    closed_by               UUID,
    closed_at               TIMESTAMPTZ,
    reopened_by             UUID,
    reopened_at             TIMESTAMPTZ,
    reopen_reason           TEXT,
    notes                   TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by              UUID,
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_period_close UNIQUE (tenant_id, period_year, period_month)
);

CREATE INDEX idx_pc_tenant ON period_closes (tenant_id);
CREATE INDEX idx_pc_period ON period_closes (tenant_id, period_year, period_month);
CREATE INDEX idx_pc_closed ON period_closes (tenant_id, is_closed);

CREATE TRIGGER trg_pc_updated_at
    BEFORE UPDATE ON period_closes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- TABLE 14: entry_status_history
-- Immutable audit trail of every journal entry status transition
-- ============================================================================

CREATE TABLE entry_status_history (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    journal_entry_id        UUID NOT NULL REFERENCES journal_entries (id),
    previous_status         entry_status,
    new_status              entry_status NOT NULL,
    changed_by              UUID,
    changed_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    change_reason           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_esh_tenant ON entry_status_history (tenant_id);
CREATE INDEX idx_esh_journal_entry ON entry_status_history (tenant_id, journal_entry_id);
CREATE INDEX idx_esh_changed_at ON entry_status_history (tenant_id, changed_at DESC);

-- Attach status-change tracking trigger AFTER entry_status_history exists
CREATE TRIGGER trg_je_status_change
    AFTER UPDATE ON journal_entries
    FOR EACH ROW EXECUTE FUNCTION track_entry_status_change();

-- ============================================================================
-- TABLE 15: change_log
-- Generic change audit log for all ledger entities (SAMA 7-year retention)
-- ============================================================================

CREATE TABLE change_log (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    entity_type             VARCHAR(100) NOT NULL,
    entity_id               UUID NOT NULL,
    operation               VARCHAR(20) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    old_values              JSONB,
    new_values              JSONB,
    changed_fields          TEXT[],
    changed_by              UUID,
    changed_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address              VARCHAR(45),
    user_agent              TEXT,
    correlation_id          VARCHAR(100)
);

CREATE INDEX idx_cl_tenant ON change_log (tenant_id);
CREATE INDEX idx_cl_entity ON change_log (tenant_id, entity_type, entity_id);
CREATE INDEX idx_cl_changed_at ON change_log (tenant_id, changed_at DESC);
CREATE INDEX idx_cl_changed_by ON change_log (tenant_id, changed_by);

-- ============================================================================
-- TABLE 16: outbox_events
-- Transactional outbox for reliable Kafka event publishing (at-least-once)
-- ============================================================================

CREATE TABLE outbox_events (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    aggregate_type          VARCHAR(100) NOT NULL,
    aggregate_id            UUID NOT NULL,
    event_type              VARCHAR(200) NOT NULL,
    payload                 JSONB NOT NULL,
    topic                   VARCHAR(200) NOT NULL,
    partition_key           VARCHAR(200),
    is_published            BOOLEAN NOT NULL DEFAULT FALSE,
    published_at            TIMESTAMPTZ,
    publish_attempts        INT NOT NULL DEFAULT 0,
    last_attempt_at         TIMESTAMPTZ,
    error_message           TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    scheduled_for           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_oe_tenant ON outbox_events (tenant_id);
CREATE INDEX idx_oe_unpublished ON outbox_events (tenant_id, is_published, scheduled_for)
    WHERE is_published = FALSE;
CREATE INDEX idx_oe_aggregate ON outbox_events (tenant_id, aggregate_type, aggregate_id);
CREATE INDEX idx_oe_event_type ON outbox_events (tenant_id, event_type);
CREATE INDEX idx_oe_created_at ON outbox_events (tenant_id, created_at DESC);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS) — Tenant Isolation
-- ============================================================================

ALTER TABLE fineract_account_mappings ENABLE ROW LEVEL SECURITY;
ALTER TABLE fineract_loan_mappings ENABLE ROW LEVEL SECURITY;
ALTER TABLE fineract_customer_mappings ENABLE ROW LEVEL SECURITY;
ALTER TABLE idempotency_keys ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_entry_sync_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE gl_reconciliation_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE sync_failures ENABLE ROW LEVEL SECURITY;
ALTER TABLE accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE journal_lines ENABLE ROW LEVEL SECURITY;
ALTER TABLE account_balances ENABLE ROW LEVEL SECURITY;
ALTER TABLE accrual_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE period_closes ENABLE ROW LEVEL SECURITY;
ALTER TABLE entry_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE change_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE outbox_events ENABLE ROW LEVEL SECURITY;

-- RLS Policies: app role sees only its own tenant rows
CREATE POLICY tenant_isolation_fineract_account_mappings
    ON fineract_account_mappings
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_fineract_loan_mappings
    ON fineract_loan_mappings
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_fineract_customer_mappings
    ON fineract_customer_mappings
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_idempotency_keys
    ON idempotency_keys
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_journal_entry_sync_log
    ON journal_entry_sync_log
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_gl_reconciliation_records
    ON gl_reconciliation_records
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_sync_failures
    ON sync_failures
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_accounts
    ON accounts
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_journal_entries
    ON journal_entries
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_journal_lines
    ON journal_lines
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_account_balances
    ON account_balances
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_accrual_schedules
    ON accrual_schedules
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_period_closes
    ON period_closes
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_entry_status_history
    ON entry_status_history
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_change_log
    ON change_log
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

CREATE POLICY tenant_isolation_outbox_events
    ON outbox_events
    USING (tenant_id = current_setting('app.tenant_id', TRUE)::UUID);

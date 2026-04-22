-- Collections Service Initial Schema
-- Database: collections_db

-- ============================================================
-- ENUMS
-- ============================================================

CREATE TYPE installment_status AS ENUM (
    'SCHEDULED',
    'DUE',
    'GRACE_PERIOD',
    'OVERDUE',
    'PARTIALLY_PAID',
    'PAID',
    'WAIVED',
    'DEFERRED'
);

CREATE TYPE payment_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'REVERSED'
);

CREATE TYPE payment_method AS ENUM (
    'WALLET_AUTO_DEBIT',
    'WALLET_MANUAL',
    'HYPERPAY_MADA',
    'HYPERPAY_VISA',
    'HYPERPAY_MASTERCARD',
    'HYPERPAY_APPLE_PAY',
    'SADAD',
    'BANK_TRANSFER',
    'CHEQUE',
    'CASH'
);

CREATE TYPE dunning_stage AS ENUM (
    'PRE_DUE_REMINDER',
    'DUE_DATE',
    'GRACE_PERIOD',
    'SOFT_COLLECTION',
    'HARD_COLLECTION',
    'LEGAL',
    'WRITE_OFF'
);

CREATE TYPE dunning_action_type AS ENUM (
    'SMS_SENT',
    'EMAIL_SENT',
    'PUSH_NOTIFICATION',
    'CALL_ATTEMPT',
    'PROMISE_TO_PAY',
    'FIELD_VISIT',
    'LEGAL_NOTICE',
    'WRITE_OFF_APPROVED'
);

CREATE TYPE settlement_type AS ENUM (
    'EARLY_SETTLEMENT',
    'PARTIAL_SETTLEMENT',
    'RESTRUCTURE',
    'WRITE_OFF'
);

CREATE TYPE settlement_status AS ENUM (
    'PENDING',
    'PAYMENT_PENDING',
    'COMPLETED',
    'CANCELLED'
);

-- ============================================================
-- REPAYMENT SCHEDULES
-- ============================================================

CREATE TABLE repayment_schedules (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    loan_id             UUID NOT NULL,
    schedule_number     VARCHAR(50) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    total_installments  INT NOT NULL DEFAULT 0,
    total_principal     NUMERIC(19,4) NOT NULL,
    total_profit        NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(19,4) NOT NULL,
    first_due_date      DATE NOT NULL,
    last_due_date       DATE NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by          UUID,
    version             INT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX idx_schedules_tenant_schedule_number ON repayment_schedules(tenant_id, schedule_number);
CREATE INDEX idx_schedules_tenant_loan ON repayment_schedules(tenant_id, loan_id);
CREATE INDEX idx_schedules_tenant ON repayment_schedules(tenant_id);
CREATE INDEX idx_schedules_loan ON repayment_schedules(tenant_id, loan_id);

-- ============================================================
-- INSTALLMENTS
-- ============================================================

CREATE TABLE installments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    schedule_id         UUID NOT NULL REFERENCES repayment_schedules(id),
    loan_id             UUID,
    installment_number  INT NOT NULL,
    due_date            DATE NOT NULL,
    principal_amount    NUMERIC(19,4) NOT NULL DEFAULT 0,
    profit_amount       NUMERIC(19,4) NOT NULL DEFAULT 0,
    fee_amount          NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(19,4) NOT NULL,
    paid_principal      NUMERIC(19,4) NOT NULL DEFAULT 0,
    paid_profit         NUMERIC(19,4) NOT NULL DEFAULT 0,
    paid_fee            NUMERIC(19,4) NOT NULL DEFAULT 0,
    paid_total          NUMERIC(19,4) NOT NULL DEFAULT 0,
    outstanding_amount  NUMERIC(19,4) GENERATED ALWAYS AS (total_amount - paid_total) STORED,
    status              installment_status NOT NULL DEFAULT 'SCHEDULED',
    dpd                 INT NOT NULL DEFAULT 0,
    paid_date           DATE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_installments_schedule ON installments(schedule_id);
CREATE INDEX idx_installments_tenant ON installments(tenant_id);
CREATE INDEX idx_installments_due_date ON installments(tenant_id, due_date, status);
CREATE INDEX idx_installments_status ON installments(tenant_id, status);
CREATE UNIQUE INDEX idx_installments_schedule_number ON installments(schedule_id, installment_number);

-- ============================================================
-- INSTALLMENT STATUS HISTORY
-- ============================================================

CREATE TABLE installment_status_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    installment_id      UUID NOT NULL REFERENCES installments(id),
    from_status         installment_status,
    to_status           installment_status NOT NULL,
    changed_by          UUID,
    change_reason       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inst_status_hist_installment ON installment_status_history(installment_id);
CREATE INDEX idx_inst_status_hist_tenant ON installment_status_history(tenant_id);

-- ============================================================
-- PAYMENTS
-- ============================================================

CREATE TABLE payments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    payment_number          VARCHAR(100) NOT NULL,
    loan_id                 UUID NOT NULL,
    customer_id             UUID,
    amount                  NUMERIC(19,4) NOT NULL,
    currency                VARCHAR(3) NOT NULL DEFAULT 'SAR',
    payment_method          VARCHAR(50) NOT NULL,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    idempotency_key         VARCHAR(100) NOT NULL,
    source_wallet_id        UUID,
    source_reference        VARCHAR(255),
    provider_transaction_id VARCHAR(255),
    value_date              DATE,
    ledger_synced           BOOLEAN NOT NULL DEFAULT FALSE,
    ledger_entry_id         UUID,
    failure_code            VARCHAR(100),
    failure_message         TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 INT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX idx_payments_payment_number ON payments(payment_number);
CREATE UNIQUE INDEX idx_payments_tenant_idempotency ON payments(tenant_id, idempotency_key);
CREATE INDEX idx_payments_tenant ON payments(tenant_id);
CREATE INDEX idx_payments_loan ON payments(tenant_id, loan_id);
CREATE INDEX idx_payments_status ON payments(tenant_id, status);

-- ============================================================
-- PAYMENT STATUS HISTORY
-- ============================================================

CREATE TABLE payment_status_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    payment_id      UUID NOT NULL REFERENCES payments(id),
    from_status     payment_status,
    to_status       payment_status NOT NULL,
    changed_by      UUID,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_status_hist_payment ON payment_status_history(payment_id);
CREATE INDEX idx_payment_status_hist_tenant ON payment_status_history(tenant_id);

-- ============================================================
-- PAYMENT ALLOCATIONS (Waterfall: fees → profit → principal)
-- ============================================================

CREATE TABLE payment_allocations (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    payment_id          UUID NOT NULL REFERENCES payments(id),
    installment_id      UUID NOT NULL REFERENCES installments(id),
    allocation_order    INT NOT NULL,
    principal_allocated NUMERIC(19,4) NOT NULL DEFAULT 0,
    profit_allocated    NUMERIC(19,4) NOT NULL DEFAULT 0,
    fee_allocated       NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_allocated     NUMERIC(19,4) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_allocations_payment ON payment_allocations(payment_id);
CREATE INDEX idx_allocations_installment ON payment_allocations(installment_id);
CREATE INDEX idx_allocations_tenant ON payment_allocations(tenant_id);

-- ============================================================
-- DUNNING CASES
-- ============================================================

CREATE TABLE dunning_cases (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    loan_id             UUID NOT NULL,
    customer_id         UUID,
    schedule_id         UUID NOT NULL REFERENCES repayment_schedules(id),
    current_stage       dunning_stage NOT NULL DEFAULT 'PRE_DUE_REMINDER',
    days_past_due       INT NOT NULL DEFAULT 0,
    outstanding_amount  NUMERIC(19,4) NOT NULL DEFAULT 0,
    opened_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    closed_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_dunning_tenant ON dunning_cases(tenant_id);
CREATE INDEX idx_dunning_loan ON dunning_cases(tenant_id, loan_id);
CREATE INDEX idx_dunning_stage ON dunning_cases(tenant_id, current_stage);
CREATE INDEX idx_dunning_open ON dunning_cases(tenant_id, closed_at) WHERE closed_at IS NULL;

-- ============================================================
-- DUNNING ACTIONS
-- ============================================================

CREATE TABLE dunning_actions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    case_id         UUID NOT NULL REFERENCES dunning_cases(id),
    action_type     dunning_action_type NOT NULL,
    stage           dunning_stage NOT NULL,
    performed_by    UUID,
    notes           TEXT,
    outcome         TEXT,
    performed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dunning_actions_case ON dunning_actions(case_id);
CREATE INDEX idx_dunning_actions_tenant ON dunning_actions(tenant_id);

-- ============================================================
-- SETTLEMENTS (Early/Partial with Ibra)
-- ============================================================

CREATE TABLE settlements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    loan_id             UUID NOT NULL,
    customer_id         UUID,
    settlement_number   VARCHAR(50) NOT NULL,
    settlement_type     settlement_type NOT NULL,
    status              settlement_status NOT NULL DEFAULT 'PENDING',
    settlement_date     DATE,
    outstanding_principal NUMERIC(19,4) NOT NULL DEFAULT 0,
    outstanding_profit  NUMERIC(19,4) NOT NULL DEFAULT 0,
    outstanding_fees    NUMERIC(19,4) NOT NULL DEFAULT 0,
    ibra_amount         NUMERIC(19,4) NOT NULL DEFAULT 0,
    settlement_amount   NUMERIC(19,4) NOT NULL,
    payment_id          UUID REFERENCES payments(id),
    idempotency_key     VARCHAR(100) NOT NULL,
    initiated_by        UUID,
    approved_by         UUID,
    notes               TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1
);

CREATE UNIQUE INDEX idx_settlements_tenant_idempotency ON settlements(tenant_id, idempotency_key);
CREATE UNIQUE INDEX idx_settlements_tenant_number ON settlements(tenant_id, settlement_number);
CREATE INDEX idx_settlements_tenant ON settlements(tenant_id);
CREATE INDEX idx_settlements_loan ON settlements(tenant_id, loan_id);
CREATE INDEX idx_settlements_status ON settlements(tenant_id, status);

-- ============================================================
-- OUTBOX EVENTS (Transactional outbox for reliable Kafka publishing)
-- ============================================================

CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    published       BOOLEAN NOT NULL DEFAULT FALSE,
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published = FALSE;
CREATE INDEX idx_outbox_tenant ON outbox_events(tenant_id);

-- ============================================================
-- CHANGE LOG (Audit trail)
-- ============================================================

CREATE TABLE change_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID NOT NULL,
    action          VARCHAR(50) NOT NULL,
    changed_by      UUID,
    old_values      JSONB,
    new_values      JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_change_log_entity ON change_log(entity_type, entity_id);
CREATE INDEX idx_change_log_tenant ON change_log(tenant_id);
CREATE INDEX idx_change_log_created ON change_log(created_at);

-- ============================================================
-- UPDATE TRIGGERS
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_repayment_schedules_updated_at
    BEFORE UPDATE ON repayment_schedules
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_installments_updated_at
    BEFORE UPDATE ON installments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_payments_updated_at
    BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_dunning_cases_updated_at
    BEFORE UPDATE ON dunning_cases
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_settlements_updated_at
    BEFORE UPDATE ON settlements
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

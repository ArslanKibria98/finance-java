-- ============================================================================
-- LENDING SERVICE DATABASE SCHEMA (SHARIA-COMPLIANT)
-- PostgreSQL 16+ High-Fidelity Production Schema
-- KSA Islamic Financing Platform
-- ============================================================================
-- Core lending with Sharia profit-sharing, amortization, collateral,
-- and full state machine tracking for regulatory compliance.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE application_status AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'DOCUMENTS_PENDING',
    'UNDER_REVIEW',
    'CREDIT_CHECK',
    'SHARIA_VALIDATION',
    'PENDING_APPROVAL',
    'APPROVED',
    'REJECTED',
    'CANCELLED',
    'EXPIRED'
);

CREATE TYPE loan_status AS ENUM (
    'PENDING_DISBURSEMENT',
    'ACTIVE',
    'DELINQUENT',
    'DEFAULT',
    'RESTRUCTURED',
    'SETTLED',
    'WRITTEN_OFF',
    'CLOSED'
);

CREATE TYPE approval_decision AS ENUM (
    'APPROVED',
    'CONDITIONAL_APPROVED',
    'REJECTED',
    'REFERRED'
);

CREATE TYPE decision_source AS ENUM (
    'SYSTEM_AUTO',
    'CREDIT_OFFICER',
    'SENIOR_CREDIT',
    'CREDIT_COMMITTEE',
    'OVERRIDE'
);

CREATE TYPE disbursement_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'COMPLETED',
    'FAILED',
    'REVERSED'
);

CREATE TYPE sharia_structure AS ENUM (
    'TAWARRUQ',
    'MURABAHA',
    'IJARA',
    'MUSHARAKAH'
);

CREATE TYPE collateral_type AS ENUM (
    'SALARY_ASSIGNMENT',
    'PROMISSORY_NOTE',
    'VEHICLE',
    'REAL_ESTATE',
    'BANK_GUARANTEE',
    'CASH_DEPOSIT'
);

CREATE TYPE collateral_status AS ENUM (
    'PENDING',
    'VERIFIED',
    'REGISTERED',
    'RELEASED',
    'ENFORCED'
);

CREATE TYPE contract_status AS ENUM (
    'DRAFT',
    'GENERATED',
    'PENDING_SIGNATURE',
    'SIGNED',
    'ACTIVE',
    'AMENDED',
    'TERMINATED'
);

-- ============================================================================
-- CORE LENDING TABLES
-- ============================================================================

-- -----------------------------------------------------------------------------
-- Loan Applications
-- -----------------------------------------------------------------------------
CREATE TABLE loan_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Application identification
    application_number VARCHAR(50) NOT NULL,

    -- Customer reference
    customer_id UUID NOT NULL,

    -- Product reference
    product_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,

    -- Partner reference
    partner_id UUID,
    lead_id UUID,

    -- Requested terms
    requested_amount NUMERIC(20, 6) NOT NULL,
    requested_tenure_months INT NOT NULL,

    -- Approved terms (populated after approval)
    approved_amount NUMERIC(20, 6),
    approved_tenure_months INT,
    approved_profit_rate NUMERIC(10, 8),

    -- Sharia structure
    sharia_structure sharia_structure NOT NULL,

    -- Calculated amounts (from Domain-Core-SDK)
    total_profit NUMERIC(20, 6),
    total_repayment NUMERIC(20, 6),
    monthly_installment NUMERIC(20, 6),

    -- DBR/DSR
    dbr_before NUMERIC(7, 4),
    dbr_after NUMERIC(7, 4),

    -- Status
    status application_status NOT NULL DEFAULT 'DRAFT',

    -- Workflow
    workflow_id VARCHAR(100),
    current_stage VARCHAR(50),

    -- Timing
    submitted_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_by UUID,
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT chk_requested_amount CHECK (requested_amount > 0),
    CONSTRAINT chk_tenure CHECK (requested_tenure_months > 0),
    CONSTRAINT uq_application_number UNIQUE (tenant_id, application_number)
);

-- -----------------------------------------------------------------------------
-- Application Status History (State Machine)
-- -----------------------------------------------------------------------------
CREATE TABLE application_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Reference
    application_id UUID NOT NULL REFERENCES loan_applications(id),

    -- Status transition
    from_status application_status,
    to_status application_status NOT NULL,

    -- Actor
    changed_by UUID,
    changed_by_type VARCHAR(20),

    -- Context
    change_reason VARCHAR(500),
    metadata JSONB,

    -- Timestamp
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Loans (Active Financing Contracts)
-- -----------------------------------------------------------------------------
CREATE TABLE loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Loan identification
    loan_number VARCHAR(50) NOT NULL,

    -- Application reference
    application_id UUID NOT NULL REFERENCES loan_applications(id),

    -- Customer reference
    customer_id UUID NOT NULL,

    -- Product reference
    product_id UUID NOT NULL,
    product_code VARCHAR(50) NOT NULL,

    -- Sharia structure
    sharia_structure sharia_structure NOT NULL,
    commodity_transaction_id UUID,

    -- Principal and profit
    principal_amount NUMERIC(20, 6) NOT NULL,
    profit_amount NUMERIC(20, 6) NOT NULL,
    total_amount NUMERIC(20, 6) NOT NULL,

    -- Terms
    profit_rate NUMERIC(10, 8) NOT NULL,
    tenure_months INT NOT NULL,
    installment_amount NUMERIC(20, 6) NOT NULL,

    -- Balances
    outstanding_principal NUMERIC(20, 6) NOT NULL,
    outstanding_profit NUMERIC(20, 6) NOT NULL,
    outstanding_fees NUMERIC(20, 6) NOT NULL DEFAULT 0,
    total_outstanding NUMERIC(20, 6) NOT NULL,

    -- Status
    status loan_status NOT NULL DEFAULT 'PENDING_DISBURSEMENT',

    -- Dates
    booking_date DATE NOT NULL,
    disbursement_date DATE,
    first_due_date DATE,
    maturity_date DATE,
    settlement_date DATE,

    -- DPD tracking
    current_dpd INT NOT NULL DEFAULT 0,
    max_dpd INT NOT NULL DEFAULT 0,

    -- IFRS9 staging
    ifrs9_stage INT NOT NULL DEFAULT 1,

    -- Fineract mapping
    fineract_loan_id BIGINT,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,

    -- Constraints
    CONSTRAINT chk_principal CHECK (principal_amount > 0),
    CONSTRAINT chk_profit CHECK (profit_amount >= 0),
    CONSTRAINT uq_loan_number UNIQUE (tenant_id, loan_number),
    CONSTRAINT uq_application UNIQUE (application_id)
);

-- -----------------------------------------------------------------------------
-- Loan Status History (State Machine)
-- -----------------------------------------------------------------------------
CREATE TABLE loan_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Reference
    loan_id UUID NOT NULL REFERENCES loans(id),

    -- Status transition
    from_status loan_status,
    to_status loan_status NOT NULL,

    -- Actor
    changed_by UUID,
    changed_by_type VARCHAR(20),

    -- Context
    change_reason VARCHAR(500),
    dpd_at_change INT,
    metadata JSONB,

    -- Timestamp
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Amortization Schedules (Pre-calculated Installment Breakdown)
-- -----------------------------------------------------------------------------
CREATE TABLE amortization_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Loan reference
    loan_id UUID NOT NULL REFERENCES loans(id),

    -- Schedule version (for restructuring)
    schedule_version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Installment number
    installment_number INT NOT NULL,

    -- Due date
    due_date DATE NOT NULL,

    -- Opening balance
    opening_principal NUMERIC(20, 6) NOT NULL,

    -- Installment breakdown
    principal_component NUMERIC(20, 6) NOT NULL,
    profit_component NUMERIC(20, 6) NOT NULL,
    total_installment NUMERIC(20, 6) NOT NULL,

    -- Closing balance
    closing_principal NUMERIC(20, 6) NOT NULL,

    -- Cumulative
    cumulative_principal NUMERIC(20, 6) NOT NULL,
    cumulative_profit NUMERIC(20, 6) NOT NULL,

    -- Calculation method
    calculation_method VARCHAR(50) NOT NULL DEFAULT 'REDUCING_BALANCE',

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_installment_number CHECK (installment_number > 0),
    CONSTRAINT uq_amortization UNIQUE (loan_id, schedule_version, installment_number)
);

-- -----------------------------------------------------------------------------
-- Profit Calculation Logs (Sharia Profit Audit Trail)
-- -----------------------------------------------------------------------------
CREATE TABLE profit_calculation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Reference
    loan_id UUID REFERENCES loans(id),
    application_id UUID REFERENCES loan_applications(id),

    -- Calculation context
    calculation_type VARCHAR(50) NOT NULL,
    calculation_date DATE NOT NULL,

    -- Inputs
    principal NUMERIC(20, 6) NOT NULL,
    profit_rate NUMERIC(10, 8) NOT NULL,
    tenure_months INT NOT NULL,
    calculation_method VARCHAR(50) NOT NULL,

    -- Outputs
    gross_profit NUMERIC(20, 6) NOT NULL,
    net_profit NUMERIC(20, 6) NOT NULL,
    effective_rate NUMERIC(10, 8) NOT NULL,

    -- Ibra (profit waiver) if applicable
    ibra_amount NUMERIC(20, 6),
    ibra_reason VARCHAR(500),

    -- Full calculation breakdown
    calculation_details JSONB NOT NULL,

    -- SDK version used
    sdk_version VARCHAR(50) NOT NULL,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    calculated_by UUID
);

-- -----------------------------------------------------------------------------
-- Approval History
-- -----------------------------------------------------------------------------
CREATE TABLE approval_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Application reference
    application_id UUID NOT NULL REFERENCES loan_applications(id),

    -- Approval level
    approval_level INT NOT NULL,
    approval_stage VARCHAR(50) NOT NULL,

    -- Decision
    decision approval_decision NOT NULL,
    decision_source decision_source NOT NULL,

    -- Approver
    approver_id UUID,
    approver_role VARCHAR(100),

    -- Comments
    comments TEXT,
    conditions JSONB,

    -- Risk data at decision
    credit_score INT,
    risk_grade VARCHAR(10),
    dbr_ratio NUMERIC(7, 4),

    -- Timing
    decided_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Disbursements
-- -----------------------------------------------------------------------------
CREATE TABLE disbursements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Loan reference
    loan_id UUID NOT NULL REFERENCES loans(id),

    -- Disbursement identification
    disbursement_number VARCHAR(50) NOT NULL,

    -- Amount
    amount NUMERIC(20, 6) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'SAR',

    -- Destination
    destination_type VARCHAR(20) NOT NULL,
    destination_iban VARCHAR(34),
    destination_wallet_id UUID,
    beneficiary_name VARCHAR(255),

    -- Status
    status disbursement_status NOT NULL DEFAULT 'PENDING',

    -- Payment reference
    payment_reference VARCHAR(100),
    fineract_transaction_id BIGINT,

    -- Idempotency
    idempotency_key VARCHAR(100) NOT NULL,

    -- Timing
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    value_date DATE,

    -- Error
    error_code VARCHAR(50),
    error_message TEXT,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    -- Constraints
    CONSTRAINT chk_disbursement_amount CHECK (amount > 0),
    CONSTRAINT uq_disbursement_number UNIQUE (tenant_id, disbursement_number),
    CONSTRAINT uq_disbursement_idempotency UNIQUE (tenant_id, idempotency_key)
);

-- -----------------------------------------------------------------------------
-- Disbursement Status History
-- -----------------------------------------------------------------------------
CREATE TABLE disbursement_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    disbursement_id UUID NOT NULL REFERENCES disbursements(id),
    from_status disbursement_status,
    to_status disbursement_status NOT NULL,
    changed_by UUID,
    change_reason VARCHAR(500),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Collateral Registry
-- -----------------------------------------------------------------------------
CREATE TABLE collateral_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Loan reference
    loan_id UUID NOT NULL REFERENCES loans(id),

    -- Collateral identification
    collateral_number VARCHAR(50) NOT NULL,

    -- Type
    collateral_type collateral_type NOT NULL,

    -- Details
    description TEXT NOT NULL,

    -- Valuation
    estimated_value NUMERIC(20, 6),
    verified_value NUMERIC(20, 6),
    valuation_date DATE,
    valuator VARCHAR(255),

    -- Coverage
    coverage_percentage NUMERIC(7, 4),

    -- For salary assignment
    employer_name VARCHAR(255),
    employer_cr VARCHAR(50),
    monthly_salary NUMERIC(20, 6),
    assignment_percentage NUMERIC(7, 4),

    -- For vehicle/property
    asset_id VARCHAR(100),
    registration_number VARCHAR(100),
    registration_authority VARCHAR(100),

    -- For promissory note
    promissory_note_number VARCHAR(100),
    promissory_note_amount NUMERIC(20, 6),

    -- Documents
    document_ids UUID[],

    -- Status
    status collateral_status NOT NULL DEFAULT 'PENDING',

    -- Dates
    registered_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,

    -- Constraints
    CONSTRAINT uq_collateral_number UNIQUE (tenant_id, collateral_number)
);

-- -----------------------------------------------------------------------------
-- Contract Version History
-- -----------------------------------------------------------------------------
CREATE TABLE contract_version_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- Loan reference
    loan_id UUID NOT NULL REFERENCES loans(id),

    -- Version
    version_number INT NOT NULL,

    -- Contract details
    contract_type VARCHAR(50) NOT NULL,

    -- Document reference
    document_id UUID NOT NULL,

    -- Contract data snapshot
    contract_terms JSONB NOT NULL,

    -- Signature
    customer_signed_at TIMESTAMPTZ,
    company_signed_at TIMESTAMPTZ,
    signature_method VARCHAR(50),

    -- Status
    status contract_status NOT NULL DEFAULT 'DRAFT',

    -- Previous version
    previous_version_id UUID REFERENCES contract_version_history(id),
    amendment_reason TEXT,

    -- Audit columns
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,

    -- Constraints
    CONSTRAINT uq_contract_version UNIQUE (loan_id, version_number)
);

-- -----------------------------------------------------------------------------
-- Change Log (Local Audit Trail)
-- -----------------------------------------------------------------------------
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

-- -----------------------------------------------------------------------------
-- Outbox Events
-- -----------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,

    correlation_id VARCHAR(100),
    causation_id VARCHAR(100),

    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES
-- ============================================================================

-- Applications
CREATE INDEX idx_applications_tenant ON loan_applications(tenant_id);
CREATE INDEX idx_applications_customer ON loan_applications(customer_id);
CREATE INDEX idx_applications_product ON loan_applications(product_id);
CREATE INDEX idx_applications_status ON loan_applications(tenant_id, status);
CREATE INDEX idx_applications_partner ON loan_applications(partner_id) WHERE partner_id IS NOT NULL;
CREATE INDEX idx_applications_active ON loan_applications(tenant_id) WHERE deleted_at IS NULL;

-- Application Status History
CREATE INDEX idx_app_status_history ON application_status_history(application_id);

-- Loans
CREATE INDEX idx_loans_tenant ON loans(tenant_id);
CREATE INDEX idx_loans_customer ON loans(customer_id);
CREATE INDEX idx_loans_status ON loans(tenant_id, status);
CREATE INDEX idx_loans_dpd ON loans(current_dpd) WHERE status = 'ACTIVE';
CREATE INDEX idx_loans_fineract ON loans(fineract_loan_id) WHERE fineract_loan_id IS NOT NULL;
CREATE INDEX idx_loans_active ON loans(tenant_id) WHERE deleted_at IS NULL;

-- Loan Status History
CREATE INDEX idx_loan_status_history ON loan_status_history(loan_id);

-- Amortization
CREATE INDEX idx_amortization_loan ON amortization_schedules(loan_id);
CREATE INDEX idx_amortization_active ON amortization_schedules(loan_id) WHERE is_active = TRUE;
CREATE INDEX idx_amortization_due ON amortization_schedules(due_date);

-- Profit Logs
CREATE INDEX idx_profit_logs_loan ON profit_calculation_logs(loan_id) WHERE loan_id IS NOT NULL;
CREATE INDEX idx_profit_logs_app ON profit_calculation_logs(application_id) WHERE application_id IS NOT NULL;

-- Approvals
CREATE INDEX idx_approvals_application ON approval_history(application_id);

-- Disbursements
CREATE INDEX idx_disbursements_loan ON disbursements(loan_id);
CREATE INDEX idx_disbursements_status ON disbursements(tenant_id, status);
CREATE INDEX idx_disbursements_pending ON disbursements(tenant_id) WHERE status = 'PENDING';

-- Collateral
CREATE INDEX idx_collateral_loan ON collateral_registry(loan_id);
CREATE INDEX idx_collateral_status ON collateral_registry(status);

-- Contracts
CREATE INDEX idx_contracts_loan ON contract_version_history(loan_id);

-- Outbox
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

CREATE TRIGGER trigger_applications_updated
    BEFORE UPDATE ON loan_applications FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_loans_updated
    BEFORE UPDATE ON loans FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_disbursements_updated
    BEFORE UPDATE ON disbursements FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER trigger_collateral_updated
    BEFORE UPDATE ON collateral_registry FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Application status tracking
CREATE OR REPLACE FUNCTION track_application_status()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO application_status_history (tenant_id, application_id, from_status, to_status)
        VALUES (NEW.tenant_id, NEW.id, OLD.status, NEW.status);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_application_status
    AFTER UPDATE ON loan_applications FOR EACH ROW EXECUTE FUNCTION track_application_status();

-- Loan status tracking
CREATE OR REPLACE FUNCTION track_loan_status()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO loan_status_history (tenant_id, loan_id, from_status, to_status, dpd_at_change)
        VALUES (NEW.tenant_id, NEW.id, OLD.status, NEW.status, NEW.current_dpd);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_loan_status
    AFTER UPDATE ON loans FOR EACH ROW EXECUTE FUNCTION track_loan_status();

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE loan_applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE application_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE loans ENABLE ROW LEVEL SECURITY;
ALTER TABLE loan_status_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE amortization_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE profit_calculation_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE approval_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE disbursements ENABLE ROW LEVEL SECURITY;
ALTER TABLE collateral_registry ENABLE ROW LEVEL SECURITY;
ALTER TABLE contract_version_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON loan_applications
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON loans
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON amortization_schedules
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE loan_applications IS 'Loan applications with full lifecycle tracking';
COMMENT ON TABLE application_status_history IS 'State machine: application status transitions';
COMMENT ON TABLE loans IS 'Active financing contracts with Sharia structure';
COMMENT ON TABLE loan_status_history IS 'State machine: loan status transitions';
COMMENT ON TABLE amortization_schedules IS 'Pre-calculated installment breakdown per schedule version';
COMMENT ON TABLE profit_calculation_logs IS 'Sharia profit calculation audit trail with SDK version';
COMMENT ON TABLE collateral_registry IS 'Loan collateral/security tracking';
COMMENT ON TABLE contract_version_history IS 'Contract amendments and version tracking';

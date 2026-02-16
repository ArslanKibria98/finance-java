-- Create schema for read models
CREATE SCHEMA IF NOT EXISTS projection;

-- Projection checkpoint table for tracking processed events
CREATE TABLE IF NOT EXISTS projection_checkpoints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projector_name VARCHAR(255) NOT NULL,
    event_id VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    partition INTEGER NOT NULL,
    offset BIGINT NOT NULL,
    event_timestamp TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_checkpoint_projector_event UNIQUE (projector_name, event_id)
);

CREATE INDEX idx_checkpoint_projector ON projection_checkpoints(projector_name);
CREATE INDEX idx_checkpoint_event ON projection_checkpoints(event_id);
CREATE INDEX idx_checkpoint_timestamp ON projection_checkpoints(event_timestamp);

-- Loan Summary Read Model table (denormalized for fast queries)
CREATE TABLE IF NOT EXISTS loan_summary_read_model (
    loan_id VARCHAR(100) PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    loan_account_number VARCHAR(50) NOT NULL UNIQUE,
    product_type VARCHAR(50) NOT NULL,
    product_name VARCHAR(255),
    status VARCHAR(50) NOT NULL,

    -- Customer data (denormalized)
    customer_id VARCHAR(100) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    national_id VARCHAR(50),
    customer_segment VARCHAR(50),

    -- Financial data
    principal_amount DECIMAL(19,4) NOT NULL,
    outstanding_principal DECIMAL(19,4) NOT NULL,
    interest_rate DECIMAL(5,2) NOT NULL,
    total_interest DECIMAL(19,4),
    paid_principal DECIMAL(19,4) DEFAULT 0,
    paid_interest DECIMAL(19,4) DEFAULT 0,
    late_fees DECIMAL(19,4) DEFAULT 0,

    -- Dates
    application_date DATE,
    approval_date DATE,
    disbursement_date DATE,
    maturity_date DATE,
    last_payment_date DATE,
    next_payment_date DATE,

    -- Islamic finance specific
    sharia_compliance_type VARCHAR(50),
    commodity_type VARCHAR(100),
    profit_rate DECIMAL(5,2),

    -- Payment schedule
    payment_frequency VARCHAR(50),
    installment_amount DECIMAL(19,4),
    number_of_installments INTEGER,
    installments_paid INTEGER DEFAULT 0,

    -- Delinquency tracking
    days_overdue INTEGER DEFAULT 0,
    overdue_amount DECIMAL(19,4) DEFAULT 0,
    overdue_installments INTEGER DEFAULT 0,

    -- Risk metrics
    risk_category VARCHAR(50),
    provision_amount DECIMAL(19,4),
    collateral_value DECIMAL(19,4),
    ltv_ratio DECIMAL(5,2),

    -- Performance metrics
    payment_performance_score INTEGER,
    early_payments INTEGER DEFAULT 0,
    late_payments INTEGER DEFAULT 0,

    -- Additional metadata
    branch_code VARCHAR(50),
    officer_id VARCHAR(100),
    officer_name VARCHAR(255),
    collection_status VARCHAR(50),

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for loan_summary_read_model
CREATE INDEX idx_loan_customer ON loan_summary_read_model(customer_id);
CREATE INDEX idx_loan_status ON loan_summary_read_model(status);
CREATE INDEX idx_loan_product ON loan_summary_read_model(product_type);
CREATE INDEX idx_loan_overdue ON loan_summary_read_model(days_overdue);
CREATE INDEX idx_loan_maturity ON loan_summary_read_model(maturity_date);
CREATE INDEX idx_loan_tenant ON loan_summary_read_model(tenant_id);
CREATE INDEX idx_loan_customer_name ON loan_summary_read_model(customer_name);

-- Customer Portfolio Read Model table
CREATE TABLE IF NOT EXISTS customer_portfolio_read_model (
    id VARCHAR(100) PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    customer_id VARCHAR(100) NOT NULL UNIQUE,
    customer_name VARCHAR(255) NOT NULL,
    national_id VARCHAR(50),
    customer_segment VARCHAR(50),
    customer_type VARCHAR(50),

    -- Portfolio metrics
    total_loans INTEGER DEFAULT 0,
    active_loans INTEGER DEFAULT 0,
    closed_loans INTEGER DEFAULT 0,
    written_off_loans INTEGER DEFAULT 0,

    -- Financial aggregates
    total_disbursed DECIMAL(19,4) DEFAULT 0,
    total_outstanding DECIMAL(19,4) DEFAULT 0,
    total_principal_paid DECIMAL(19,4) DEFAULT 0,
    total_paid_interest DECIMAL(19,4) DEFAULT 0,
    overdue_amount DECIMAL(19,4) DEFAULT 0,
    total_late_fees DECIMAL(19,4) DEFAULT 0,

    -- Current loan details
    largest_loan_amount DECIMAL(19,4),
    current_installment_amount DECIMAL(19,4),
    average_loan_size DECIMAL(19,4),
    average_interest_rate DECIMAL(5,2),

    -- Dates
    first_loan_date DATE,
    last_loan_date DATE,
    last_payment_date DATE,
    next_payment_date DATE,
    customer_since DATE,

    -- First loan details
    first_loan_amount DECIMAL(19,4),
    first_loan_product VARCHAR(100),

    -- Payment behavior
    payment_performance_score INTEGER,
    total_payments_made INTEGER DEFAULT 0,
    on_time_payments INTEGER DEFAULT 0,
    late_payments INTEGER DEFAULT 0,
    bounced_payments INTEGER DEFAULT 0,
    max_days_overdue INTEGER DEFAULT 0,
    current_days_overdue INTEGER DEFAULT 0,

    -- Risk assessment
    risk_category VARCHAR(50),
    risk_score INTEGER,
    probability_of_default DECIMAL(5,2),
    exposure_at_default DECIMAL(19,4),

    -- Collateral
    total_collateral_value DECIMAL(19,4),
    ltv_ratio DECIMAL(5,2),

    -- Customer value metrics
    lifetime_value DECIMAL(19,4),
    profitability_score INTEGER,

    -- Collection status
    collection_status VARCHAR(50),
    last_contact_date DATE,
    promise_to_pay_date DATE,

    -- Islamic finance specific
    preferred_finance_type VARCHAR(50),
    sharia_compliance_verified BOOLEAN DEFAULT FALSE,

    -- Audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for customer_portfolio_read_model
CREATE INDEX idx_customer_portfolio_id ON customer_portfolio_read_model(customer_id);
CREATE INDEX idx_customer_risk ON customer_portfolio_read_model(risk_category);
CREATE INDEX idx_customer_segment ON customer_portfolio_read_model(customer_segment);
CREATE INDEX idx_customer_outstanding ON customer_portfolio_read_model(total_outstanding);
CREATE INDEX idx_customer_tenant ON customer_portfolio_read_model(tenant_id);
CREATE INDEX idx_customer_name_search ON customer_portfolio_read_model(customer_name);

-- Payment History Read Model table
CREATE TABLE IF NOT EXISTS payment_history_read_model (
    payment_id VARCHAR(100) PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    loan_id VARCHAR(100) NOT NULL,
    loan_account_number VARCHAR(50),

    -- Customer reference (denormalized)
    customer_id VARCHAR(100) NOT NULL,
    customer_name VARCHAR(255),
    national_id VARCHAR(50),

    -- Payment details
    payment_date DATE NOT NULL,
    due_date DATE,
    payment_amount DECIMAL(19,4) NOT NULL,
    scheduled_amount DECIMAL(19,4),

    -- Payment breakdown
    principal_portion DECIMAL(19,4),
    interest_portion DECIMAL(19,4),
    late_fee DECIMAL(19,4),
    other_charges DECIMAL(19,4),

    -- Islamic finance specific
    profit_portion DECIMAL(19,4),
    rental_portion DECIMAL(19,4),

    -- Payment method
    payment_channel VARCHAR(50),
    payment_reference VARCHAR(100),
    bank_reference VARCHAR(100),

    -- Status
    status VARCHAR(50) NOT NULL,
    reconciliation_status VARCHAR(50),

    -- Timing
    days_late INTEGER,
    is_prepayment BOOLEAN DEFAULT FALSE,
    is_partial_payment BOOLEAN DEFAULT FALSE,

    -- Installment tracking
    installment_number INTEGER,
    total_installments INTEGER,

    -- Balances after payment
    outstanding_after DECIMAL(19,4),
    overdue_after DECIMAL(19,4),

    -- Collection tracking
    collection_attempt INTEGER,
    collector_id VARCHAR(100),
    collection_notes TEXT,

    -- Receipt details
    receipt_number VARCHAR(100),
    receipt_generated BOOLEAN DEFAULT FALSE,

    -- Branch/Channel info
    branch_code VARCHAR(50),
    teller_id VARCHAR(100),

    -- Reversal info
    reversed BOOLEAN DEFAULT FALSE,
    reversal_date DATE,
    reversal_reason VARCHAR(255),

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- Create indexes for payment_history_read_model
CREATE INDEX idx_payment_loan ON payment_history_read_model(loan_id);
CREATE INDEX idx_payment_customer ON payment_history_read_model(customer_id);
CREATE INDEX idx_payment_date ON payment_history_read_model(payment_date);
CREATE INDEX idx_payment_due_date ON payment_history_read_model(due_date);
CREATE INDEX idx_payment_status ON payment_history_read_model(status);
CREATE INDEX idx_payment_channel ON payment_history_read_model(payment_channel);
CREATE INDEX idx_payment_tenant ON payment_history_read_model(tenant_id);
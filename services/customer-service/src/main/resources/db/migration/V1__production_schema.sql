-- ============================================================================
-- CUSTOMER SERVICE DATABASE SCHEMA
-- PostgreSQL 16+ Production Schema
-- KSA Islamic Financing Platform
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE customer_type AS ENUM (
    'INDIVIDUAL',
    'SME'
);

CREATE TYPE kyc_status AS ENUM (
    'PENDING',
    'IN_PROGRESS',
    'VERIFIED',
    'EXPIRED',
    'REJECTED',
    'BLOCKED'
);

CREATE TYPE risk_grade AS ENUM (
    'A',
    'B',
    'C',
    'D',
    'E'
);

CREATE TYPE employment_type AS ENUM (
    'GOVERNMENT',
    'SEMI_GOVERNMENT',
    'PRIVATE',
    'SELF_EMPLOYED',
    'RETIRED',
    'UNEMPLOYED'
);

CREATE TYPE residency_type AS ENUM (
    'CITIZEN',
    'RESIDENT',
    'GCC_NATIONAL',
    'VISITOR'
);

CREATE TYPE gender_type AS ENUM (
    'MALE',
    'FEMALE'
);

CREATE TYPE consent_type AS ENUM (
    'DATA_PROCESSING',
    'CREDIT_CHECK',
    'MARKETING',
    'THIRD_PARTY_SHARING',
    'ELECTRONIC_COMMUNICATIONS'
);

CREATE TYPE consent_status AS ENUM (
    'GRANTED',
    'WITHDRAWN',
    'EXPIRED'
);

CREATE TYPE bank_account_status AS ENUM (
    'PENDING_VERIFICATION',
    'VERIFIED',
    'FAILED',
    'BLOCKED'
);

CREATE TYPE lifecycle_stage AS ENUM (
    'LEAD',
    'PROSPECT',
    'QUALIFIED',
    'APPLICANT',
    'ACTIVE',
    'DORMANT',
    'CHURNED'
);

CREATE TYPE business_lifecycle_stage AS ENUM (
    'LEAD',
    'PROSPECT',
    'QUALIFIED',
    'APPLICANT',
    'ACTIVE',
    'DORMANT',
    'CHURNED'
);

-- ============================================================================
-- TABLES
-- ============================================================================

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    cif_number VARCHAR(20) NOT NULL,
    customer_type customer_type NOT NULL DEFAULT 'INDIVIDUAL',
    national_id VARCHAR(255) NOT NULL,
    national_id_type VARCHAR(20) NOT NULL DEFAULT 'NID',
    title VARCHAR(20),
    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    first_name_ar VARCHAR(100),
    last_name_ar VARCHAR(100),
    full_name VARCHAR(500) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender gender_type,
    nationality VARCHAR(3) NOT NULL,
    residency_type residency_type NOT NULL,
    mobile_number VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    region VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(3) NOT NULL DEFAULT 'SA',
    kyc_status kyc_status NOT NULL DEFAULT 'PENDING',
    kyc_verified_at TIMESTAMPTZ,
    kyc_expiry_date DATE,
    nafath_verified BOOLEAN NOT NULL DEFAULT FALSE,
    nafath_transaction_id VARCHAR(100),
    risk_grade risk_grade,
    risk_grade_updated_at TIMESTAMPTZ,
    pep_flag BOOLEAN NOT NULL DEFAULT FALSE,
    sanctions_flag BOOLEAN NOT NULL DEFAULT FALSE,
    keycloak_user_id UUID,
    global_uid UUID,
    lifecycle_stage lifecycle_stage NOT NULL DEFAULT 'LEAD',
    lifecycle_stage_changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    customer_segment VARCHAR(50),
    acquisition_channel VARCHAR(50),
    acquisition_partner_id UUID,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    blocked_at TIMESTAMPTZ,
    blocked_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_cif_number UNIQUE (tenant_id, cif_number),
    CONSTRAINT uq_national_id UNIQUE (tenant_id, national_id)
);

CREATE TABLE business_customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    bif_number VARCHAR(20) NOT NULL,
    commercial_registration VARCHAR(20) NOT NULL,
    cr_issue_date DATE,
    cr_expiry_date DATE,
    vat_number VARCHAR(20),
    company_name VARCHAR(255) NOT NULL,
    company_name_ar VARCHAR(255),
    trading_name VARCHAR(255),
    legal_form VARCHAR(50),
    main_activity_code VARCHAR(20),
    main_activity_description VARCHAR(500),
    registered_address VARCHAR(500),
    city VARCHAR(100),
    region VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(3) NOT NULL DEFAULT 'SA',
    phone_number VARCHAR(50),
    email VARCHAR(255),
    website VARCHAR(255),
    kyb_status kyc_status NOT NULL DEFAULT 'PENDING',
    kyb_verified_at TIMESTAMPTZ,
    risk_grade risk_grade,
    sector_risk VARCHAR(20),
    annual_revenue NUMERIC(19, 4),
    employee_count INT,
    years_in_business INT,
    primary_contact_id UUID REFERENCES customers(id),
    global_business_uid UUID,
    lifecycle_stage business_lifecycle_stage NOT NULL DEFAULT 'LEAD',
    lifecycle_stage_changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_bif_number UNIQUE (tenant_id, bif_number),
    CONSTRAINT uq_commercial_registration UNIQUE (tenant_id, commercial_registration)
);

CREATE TABLE authorized_signatories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    business_customer_id UUID NOT NULL REFERENCES business_customers(id),
    individual_customer_id UUID REFERENCES customers(id),
    full_name VARCHAR(255) NOT NULL,
    national_id VARCHAR(50) NOT NULL,
    role VARCHAR(100),
    authorization_type VARCHAR(50) NOT NULL,
    signing_limit NUMERIC(19, 4),
    valid_from DATE NOT NULL,
    valid_until DATE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

CREATE TABLE employment_info (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    employer_name VARCHAR(255) NOT NULL,
    employer_cr_number VARCHAR(20),
    employer_sector VARCHAR(100),
    employment_type employment_type NOT NULL,
    job_title VARCHAR(100),
    start_date DATE,
    basic_salary NUMERIC(19, 4),
    housing_allowance NUMERIC(19, 4),
    other_allowances NUMERIC(19, 4),
    gross_salary NUMERIC(19, 4),
    deductions NUMERIC(19, 4),
    net_salary NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'SAR',
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_via VARCHAR(50),
    verified_at TIMESTAMPTZ,
    salary_bank_name VARCHAR(100),
    salary_iban VARCHAR(34),
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    end_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    bank_name VARCHAR(100) NOT NULL,
    bank_code VARCHAR(20),
    iban VARCHAR(255) NOT NULL,
    account_holder_name VARCHAR(255),
    account_type VARCHAR(50),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_salary_account BOOLEAN NOT NULL DEFAULT FALSE,
    status bank_account_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    verified_at TIMESTAMPTZ,
    verification_method VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    consent_type consent_type NOT NULL,
    consent_version VARCHAR(20) NOT NULL,
    status consent_status NOT NULL DEFAULT 'GRANTED',
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    granted_via VARCHAR(50) NOT NULL,
    withdrawn_at TIMESTAMPTZ,
    expiry_date DATE,
    ip_address VARCHAR(45),
    device_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1
);

CREATE TABLE kyc_verifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers(id),
    verification_type VARCHAR(50) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100) NOT NULL,
    request_payload JSONB,
    response_payload JSONB,
    is_successful BOOLEAN NOT NULL,
    failure_reason VARCHAR(500),
    verified_data JSONB,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE lifecycle_stage_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    from_stage VARCHAR(20),
    to_stage VARCHAR(20) NOT NULL,
    triggered_by VARCHAR(50) NOT NULL,
    triggered_by_user_id UUID,
    trigger_event VARCHAR(100),
    trigger_reference_id UUID,
    notes VARCHAR(500),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- INDEXES
-- ============================================================================

CREATE INDEX idx_customers_tenant ON customers(tenant_id);
CREATE INDEX idx_customers_kyc ON customers(tenant_id, kyc_status);
CREATE INDEX idx_customers_risk ON customers(tenant_id, risk_grade);
CREATE INDEX idx_customers_keycloak ON customers(keycloak_user_id) WHERE keycloak_user_id IS NOT NULL;
CREATE INDEX idx_customers_active ON customers(tenant_id) WHERE is_active = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_customers_global_uid ON customers(global_uid) WHERE global_uid IS NOT NULL;
CREATE INDEX idx_customers_lifecycle ON customers(tenant_id, lifecycle_stage);
CREATE INDEX idx_customers_leads ON customers(tenant_id) WHERE lifecycle_stage = 'LEAD';
CREATE INDEX idx_customers_active_products ON customers(tenant_id) WHERE lifecycle_stage = 'ACTIVE';

CREATE INDEX idx_business_customers_tenant ON business_customers(tenant_id);
CREATE INDEX idx_business_customers_kyb ON business_customers(tenant_id, kyb_status);
CREATE INDEX idx_business_customers_active ON business_customers(tenant_id) WHERE is_active = TRUE AND deleted_at IS NULL;
CREATE INDEX idx_business_customers_global_uid ON business_customers(global_business_uid) WHERE global_business_uid IS NOT NULL;
CREATE INDEX idx_business_customers_lifecycle ON business_customers(tenant_id, lifecycle_stage);

CREATE INDEX idx_employment_customer ON employment_info(customer_id);
CREATE INDEX idx_employment_current ON employment_info(customer_id) WHERE is_current = TRUE;

CREATE INDEX idx_bank_accounts_customer ON bank_accounts(customer_id);
CREATE INDEX idx_bank_accounts_primary ON bank_accounts(customer_id) WHERE is_primary = TRUE AND deleted_at IS NULL;

CREATE INDEX idx_consents_customer ON consents(customer_id);
CREATE INDEX idx_consents_active ON consents(customer_id, consent_type) WHERE status = 'GRANTED';

CREATE INDEX idx_kyc_verifications_customer ON kyc_verifications(customer_id);
CREATE INDEX idx_kyc_verifications_transaction ON kyc_verifications(transaction_id);

CREATE INDEX idx_lifecycle_entity ON lifecycle_stage_history(entity_type, entity_id);
CREATE INDEX idx_lifecycle_tenant ON lifecycle_stage_history(tenant_id, changed_at DESC);

CREATE INDEX idx_outbox_unpublished ON outbox_events(created_at) WHERE published = FALSE;

-- ============================================================================
-- TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_customers_updated
    BEFORE UPDATE ON customers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_business_customers_updated
    BEFORE UPDATE ON business_customers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_employment_updated
    BEFORE UPDATE ON employment_info FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_bank_accounts_updated
    BEFORE UPDATE ON bank_accounts FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trigger_signatories_updated
    BEFORE UPDATE ON authorized_signatories FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Auto-log lifecycle stage transitions for individuals
CREATE OR REPLACE FUNCTION log_customer_lifecycle_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.lifecycle_stage IS DISTINCT FROM NEW.lifecycle_stage THEN
        NEW.lifecycle_stage_changed_at = NOW();
        INSERT INTO lifecycle_stage_history (
            tenant_id, entity_type, entity_id,
            from_stage, to_stage,
            triggered_by, trigger_event
        ) VALUES (
            NEW.tenant_id, 'INDIVIDUAL', NEW.id,
            OLD.lifecycle_stage::TEXT, NEW.lifecycle_stage::TEXT,
            'SYSTEM', TG_ARGV[0]
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_customer_lifecycle
    BEFORE UPDATE ON customers FOR EACH ROW
    EXECUTE FUNCTION log_customer_lifecycle_change('LIFECYCLE_CHANGE');

-- Auto-log lifecycle stage transitions for business customers
CREATE OR REPLACE FUNCTION log_business_lifecycle_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.lifecycle_stage IS DISTINCT FROM NEW.lifecycle_stage THEN
        NEW.lifecycle_stage_changed_at = NOW();
        INSERT INTO lifecycle_stage_history (
            tenant_id, entity_type, entity_id,
            from_stage, to_stage,
            triggered_by, trigger_event
        ) VALUES (
            NEW.tenant_id, 'BUSINESS', NEW.id,
            OLD.lifecycle_stage::TEXT, NEW.lifecycle_stage::TEXT,
            'SYSTEM', TG_ARGV[0]
        );
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_business_lifecycle
    BEFORE UPDATE ON business_customers FOR EACH ROW
    EXECUTE FUNCTION log_business_lifecycle_change('LIFECYCLE_CHANGE');

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE business_customers ENABLE ROW LEVEL SECURITY;
ALTER TABLE authorized_signatories ENABLE ROW LEVEL SECURITY;
ALTER TABLE employment_info ENABLE ROW LEVEL SECURITY;
ALTER TABLE bank_accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE consents ENABLE ROW LEVEL SECURITY;
ALTER TABLE kyc_verifications ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON customers
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON business_customers
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON authorized_signatories
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON employment_info
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON bank_accounts
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON consents
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation ON kyc_verifications
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

ALTER TABLE lifecycle_stage_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON lifecycle_stage_history
    FOR ALL USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE customers IS 'Individual customer CIF records with KYC status';
COMMENT ON TABLE business_customers IS 'SME customer BIF records with KYB status';
COMMENT ON TABLE authorized_signatories IS 'Authorized signatories for business customers';
COMMENT ON TABLE employment_info IS 'Employment and salary information for individuals';
COMMENT ON TABLE bank_accounts IS 'Verified bank accounts for disbursement/collection';
COMMENT ON TABLE consents IS 'GDPR/PDPL consent tracking';
COMMENT ON TABLE kyc_verifications IS 'Audit trail of KYC verification attempts';
COMMENT ON COLUMN customers.national_id IS 'Encrypted at application layer with AES-256';
COMMENT ON COLUMN customers.mobile_number IS 'Encrypted at application layer with AES-256';
COMMENT ON COLUMN bank_accounts.iban IS 'Encrypted at application layer with AES-256';
COMMENT ON COLUMN customers.global_uid IS 'FK to global_profile_db.global_customers.global_uid';
COMMENT ON COLUMN customers.lifecycle_stage IS 'Tracks the customer journey: LEAD → PROSPECT → QUALIFIED → APPLICANT → ACTIVE → DORMANT → CHURNED';
COMMENT ON TABLE lifecycle_stage_history IS 'Immutable audit trail of all lifecycle stage transitions';

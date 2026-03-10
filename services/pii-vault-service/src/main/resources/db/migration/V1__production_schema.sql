-- ============================================================================
-- PII VAULT DATABASE SCHEMA
-- PostgreSQL 16+ Production Schema
-- Regional Encrypted Storage for Personally Identifiable Information
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- VAULT CONFIGURATION
-- ============================================================================

CREATE TABLE vault_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value TEXT NOT NULL,
    description TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO vault_config (config_key, config_value, description) VALUES
    ('vault_region', 'KSA', 'Region code: KSA, UAE, PAK'),
    ('encryption_key_version', '1', 'Current Vault Transit key version'),
    ('compliance_jurisdiction', 'SAMA', 'Regulatory body: SAMA, CBUAE, SBP'),
    ('data_retention_years', '7', 'PII retention period in years'),
    ('max_token_ttl_seconds', '600', 'Maximum PII access token TTL');

-- ============================================================================
-- ENUM TYPES
-- ============================================================================

CREATE TYPE vault_region AS ENUM (
    'KSA',
    'UAE',
    'PAK',
    'EGY',
    'MYS'
);

CREATE TYPE access_purpose AS ENUM (
    'ONBOARDING',
    'LOAN_APPLICATION_REVIEW',
    'COMPLIANCE_CHECK',
    'AML_SCREENING',
    'CUSTOMER_SERVICE',
    'REGULATORY_REPORT',
    'AUDIT',
    'DATA_EXPORT',
    'DATA_DELETION'
);

CREATE TYPE document_type AS ENUM (
    'NATIONAL_ID',
    'PASSPORT',
    'IQAMA',
    'EMIRATES_ID',
    'CNIC',
    'DRIVING_LICENSE',
    'UTILITY_BILL',
    'BANK_STATEMENT',
    'SALARY_CERTIFICATE',
    'COMMERCIAL_REGISTRATION',
    'VAT_CERTIFICATE',
    'MEMORANDUM_OF_ASSOCIATION',
    'BOARD_RESOLUTION',
    'POWER_OF_ATTORNEY'
);

CREATE TYPE biometric_type AS ENUM (
    'FACE_TEMPLATE',
    'FINGERPRINT',
    'IRIS_SCAN',
    'VOICE_PRINT'
);

-- ============================================================================
-- PII INDIVIDUAL (Encrypted Storage)
-- ============================================================================

CREATE TABLE pii_individual (
    pii_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    global_uid UUID NOT NULL,
    vault_region vault_region NOT NULL,
    national_id_encrypted BYTEA NOT NULL,
    national_id_type VARCHAR(20) NOT NULL,
    full_name_encrypted BYTEA NOT NULL,
    first_name_encrypted BYTEA NOT NULL,
    middle_name_encrypted BYTEA,
    last_name_encrypted BYTEA NOT NULL,
    full_name_ar_encrypted BYTEA,
    date_of_birth_encrypted BYTEA NOT NULL,
    gender VARCHAR(10),
    nationality_code VARCHAR(3) NOT NULL,
    mobile_encrypted BYTEA NOT NULL,
    email_encrypted BYTEA,
    alternate_mobile_encrypted BYTEA,
    address_line1_encrypted BYTEA,
    address_line2_encrypted BYTEA,
    city_encrypted BYTEA,
    region_encrypted BYTEA,
    postal_code_encrypted BYTEA,
    country_code VARCHAR(3) NOT NULL,
    national_address_encrypted BYTEA,
    building_number_encrypted BYTEA,
    unit_number_encrypted BYTEA,
    employer_name_encrypted BYTEA,
    employer_cr_encrypted BYTEA,
    monthly_salary_encrypted BYTEA,
    bank_name VARCHAR(100),
    iban_encrypted BYTEA,
    account_holder_name_encrypted BYTEA,
    encryption_key_version INT NOT NULL DEFAULT 1,
    encryption_algorithm VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    encrypted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,
    deletion_reason VARCHAR(500),
    deleted_by UUID,
    CONSTRAINT uq_pii_global_uid_region UNIQUE (global_uid, vault_region)
);

CREATE INDEX idx_pii_individual_global_uid ON pii_individual(global_uid) WHERE deleted_at IS NULL;
CREATE INDEX idx_pii_individual_created ON pii_individual(created_at DESC);
CREATE INDEX idx_pii_individual_region ON pii_individual(vault_region);

-- ============================================================================
-- PII BUSINESS (SME Encrypted Storage)
-- ============================================================================

CREATE TABLE pii_business (
    pii_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    global_business_uid UUID NOT NULL,
    vault_region vault_region NOT NULL,
    company_name_encrypted BYTEA NOT NULL,
    company_name_ar_encrypted BYTEA,
    trading_name_encrypted BYTEA,
    legal_form VARCHAR(50),
    commercial_registration_encrypted BYTEA NOT NULL,
    vat_number_encrypted BYTEA,
    tax_number_encrypted BYTEA,
    registered_address_encrypted BYTEA,
    city_encrypted BYTEA,
    region_encrypted BYTEA,
    postal_code_encrypted BYTEA,
    country_code VARCHAR(3) NOT NULL,
    phone_number_encrypted BYTEA,
    email_encrypted BYTEA,
    website VARCHAR(255),
    main_activity_code VARCHAR(20),
    main_activity_description_encrypted BYTEA,
    establishment_date DATE,
    annual_revenue_encrypted BYTEA,
    employee_count_encrypted BYTEA,
    bank_name VARCHAR(100),
    iban_encrypted BYTEA,
    account_holder_name_encrypted BYTEA,
    encryption_key_version INT NOT NULL DEFAULT 1,
    encryption_algorithm VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    encrypted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 1,
    deleted_at TIMESTAMPTZ,
    deletion_reason VARCHAR(500),
    deleted_by UUID,
    CONSTRAINT uq_pii_business_global_uid_region UNIQUE (global_business_uid, vault_region)
);

CREATE INDEX idx_pii_business_global_uid ON pii_business(global_business_uid) WHERE deleted_at IS NULL;
CREATE INDEX idx_pii_business_region ON pii_business(vault_region);

-- ============================================================================
-- PII BIOMETRICS
-- ============================================================================

CREATE TABLE pii_biometrics (
    biometric_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pii_id UUID NOT NULL REFERENCES pii_individual(pii_id) ON DELETE CASCADE,
    biometric_type biometric_type NOT NULL,
    face_template_encrypted BYTEA,
    fingerprint_template_encrypted BYTEA,
    iris_scan_encrypted BYTEA,
    voice_print_encrypted BYTEA,
    biometric_provider VARCHAR(50) NOT NULL,
    biometric_quality_score NUMERIC(5, 2),
    verified_at TIMESTAMPTZ,
    verification_transaction_id VARCHAR(100),
    encryption_key_version INT NOT NULL DEFAULT 1,
    encryption_algorithm VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    encrypted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_biometrics_pii ON pii_biometrics(pii_id);
CREATE INDEX idx_biometrics_type ON pii_biometrics(biometric_type);

-- ============================================================================
-- PII DOCUMENTS
-- ============================================================================

CREATE TABLE pii_documents (
    document_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pii_individual_id UUID REFERENCES pii_individual(pii_id) ON DELETE CASCADE,
    pii_business_id UUID REFERENCES pii_business(pii_id) ON DELETE CASCADE,
    document_type document_type NOT NULL,
    document_data_encrypted BYTEA,
    storage_backend VARCHAR(20),
    storage_path VARCHAR(500),
    storage_encryption_key_id VARCHAR(100),
    original_filename VARCHAR(255),
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),
    document_hash VARCHAR(64),
    ocr_extracted_data_encrypted BYTEA,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_by UUID,
    verified_at TIMESTAMPTZ,
    document_issue_date DATE,
    document_expiry_date DATE,
    encryption_key_version INT NOT NULL DEFAULT 1,
    encryption_algorithm VARCHAR(50) NOT NULL DEFAULT 'AES-256-GCM',
    encrypted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    uploaded_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    CHECK (
        (pii_individual_id IS NOT NULL AND pii_business_id IS NULL) OR
        (pii_individual_id IS NULL AND pii_business_id IS NOT NULL)
    ),
    CHECK (
        (storage_backend = 'DATABASE' AND document_data_encrypted IS NOT NULL) OR
        (storage_backend = 'MINIO' AND storage_path IS NOT NULL) OR
        (storage_backend IS NULL)
    )
);

CREATE INDEX idx_documents_individual ON pii_documents(pii_individual_id)
    WHERE pii_individual_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_documents_business ON pii_documents(pii_business_id)
    WHERE pii_business_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_documents_type ON pii_documents(document_type);
CREATE INDEX idx_documents_expiry ON pii_documents(document_expiry_date)
    WHERE document_expiry_date IS NOT NULL AND deleted_at IS NULL;

-- ============================================================================
-- PII ACCESS AUDIT (Immutable, Blockchain-Style Chaining)
-- ============================================================================

CREATE TABLE pii_access_audit (
    audit_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pii_individual_id UUID REFERENCES pii_individual(pii_id),
    pii_business_id UUID REFERENCES pii_business(pii_id),
    global_uid UUID,
    accessor_id UUID NOT NULL,
    accessor_role VARCHAR(50) NOT NULL,
    accessor_ip VARCHAR(45) NOT NULL,
    accessor_device_id VARCHAR(100),
    accessor_session_id VARCHAR(100),
    accessed_fields VARCHAR(100)[] NOT NULL,
    access_operation VARCHAR(20) NOT NULL,
    access_purpose access_purpose NOT NULL,
    access_justification TEXT,
    related_entity_type VARCHAR(50),
    related_entity_id UUID,
    access_token_id UUID,
    access_token_expiry TIMESTAMPTZ,
    access_granted BOOLEAN NOT NULL,
    denial_reason VARCHAR(500),
    retrieved_data_hash VARCHAR(64),
    previous_log_hash VARCHAR(64),
    current_log_hash VARCHAR(64) NOT NULL,
    accessed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (pii_individual_id IS NOT NULL OR pii_business_id IS NOT NULL)
);

CREATE INDEX idx_audit_global_uid ON pii_access_audit(global_uid);
CREATE INDEX idx_audit_individual ON pii_access_audit(pii_individual_id) WHERE pii_individual_id IS NOT NULL;
CREATE INDEX idx_audit_business ON pii_access_audit(pii_business_id) WHERE pii_business_id IS NOT NULL;
CREATE INDEX idx_audit_accessor ON pii_access_audit(accessor_id);
CREATE INDEX idx_audit_timestamp ON pii_access_audit(accessed_at DESC);
CREATE INDEX idx_audit_purpose ON pii_access_audit(access_purpose);
CREATE INDEX idx_audit_hash ON pii_access_audit(current_log_hash);

-- ============================================================================
-- PREVENT AUDIT LOG MODIFICATION (WORM Compliance)
-- ============================================================================

CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit log is immutable. Updates and deletes are prohibited for compliance.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_prevent_audit_update
    BEFORE UPDATE ON pii_access_audit
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

CREATE TRIGGER trigger_prevent_audit_delete
    BEFORE DELETE ON pii_access_audit
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();

-- ============================================================================
-- AUDIT LOG HASH CHAINING
-- ============================================================================

CREATE OR REPLACE FUNCTION compute_audit_log_hash(
    p_audit_id UUID,
    p_global_uid UUID,
    p_accessor_id UUID,
    p_accessed_fields VARCHAR[],
    p_accessed_at TIMESTAMPTZ,
    p_previous_hash VARCHAR
) RETURNS VARCHAR AS $$
DECLARE
    hash_input TEXT;
    computed_hash VARCHAR;
BEGIN
    hash_input := p_audit_id::TEXT || '|' ||
                  COALESCE(p_global_uid::TEXT, 'NULL') || '|' ||
                  p_accessor_id::TEXT || '|' ||
                  array_to_string(p_accessed_fields, ',') || '|' ||
                  p_accessed_at::TEXT || '|' ||
                  COALESCE(p_previous_hash, '0');

    computed_hash := encode(sha256(hash_input::bytea), 'hex');

    RETURN computed_hash;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION chain_audit_log()
RETURNS TRIGGER AS $$
DECLARE
    v_previous_hash VARCHAR;
BEGIN
    SELECT current_log_hash INTO v_previous_hash
    FROM pii_access_audit
    WHERE global_uid = NEW.global_uid
    ORDER BY accessed_at DESC
    LIMIT 1;

    NEW.previous_log_hash := v_previous_hash;
    NEW.current_log_hash := compute_audit_log_hash(
        NEW.audit_id,
        NEW.global_uid,
        NEW.accessor_id,
        NEW.accessed_fields,
        NEW.accessed_at,
        v_previous_hash
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_chain_audit_log
    BEFORE INSERT ON pii_access_audit
    FOR EACH ROW EXECUTE FUNCTION chain_audit_log();

-- ============================================================================
-- PII RETENTION POLICY
-- ============================================================================

CREATE TABLE pii_retention_policy (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vault_region vault_region NOT NULL,
    data_category VARCHAR(100) NOT NULL,
    retention_years INT NOT NULL,
    regulatory_requirement VARCHAR(500),
    effective_from DATE NOT NULL,
    effective_until DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO pii_retention_policy (vault_region, data_category, retention_years, regulatory_requirement, effective_from) VALUES
    ('KSA', 'CUSTOMER_PII', 7, 'SAMA Cyber Security Framework', '2024-01-01'),
    ('KSA', 'BIOMETRICS', 5, 'PDPL Article 12', '2024-01-01'),
    ('KSA', 'DOCUMENTS', 10, 'SAMA Financial Records Retention', '2024-01-01'),
    ('KSA', 'AUDIT_LOGS', 7, 'SAMA Compliance', '2024-01-01'),
    ('UAE', 'CUSTOMER_PII', 7, 'CBUAE Regulations', '2024-01-01'),
    ('UAE', 'BIOMETRICS', 5, 'DIFC Data Protection', '2024-01-01'),
    ('PAK', 'CUSTOMER_PII', 7, 'State Bank of Pakistan', '2024-01-01'),
    ('PAK', 'BIOMETRICS', 5, 'DPDPA', '2024-01-01');

-- ============================================================================
-- PII DELETION LOG
-- ============================================================================

CREATE TABLE pii_deletion_log (
    deletion_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    global_uid UUID NOT NULL,
    vault_region vault_region NOT NULL,
    pii_individual_id UUID,
    pii_business_id UUID,
    deletion_reason VARCHAR(100) NOT NULL,
    customer_consent_proof TEXT,
    deleted_by UUID NOT NULL,
    deletion_approved_by UUID,
    records_deleted INT NOT NULL,
    documents_deleted INT NOT NULL,
    biometrics_deleted INT NOT NULL,
    deletion_verification_hash VARCHAR(64) NOT NULL,
    deleted_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deletion_log_global_uid ON pii_deletion_log(global_uid);
CREATE INDEX idx_deletion_log_timestamp ON pii_deletion_log(deleted_at DESC);

-- ============================================================================
-- TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_pii_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    NEW.version = OLD.version + 1;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_pii_individual_updated
    BEFORE UPDATE ON pii_individual
    FOR EACH ROW EXECUTE FUNCTION update_pii_updated_at();

CREATE TRIGGER trigger_pii_business_updated
    BEFORE UPDATE ON pii_business
    FOR EACH ROW EXECUTE FUNCTION update_pii_updated_at();

-- ============================================================================
-- ROW LEVEL SECURITY
-- ============================================================================

ALTER TABLE pii_individual ENABLE ROW LEVEL SECURITY;
ALTER TABLE pii_business ENABLE ROW LEVEL SECURITY;
ALTER TABLE pii_biometrics ENABLE ROW LEVEL SECURITY;
ALTER TABLE pii_documents ENABLE ROW LEVEL SECURITY;

CREATE POLICY vault_service_only ON pii_individual
    FOR ALL USING (current_user = 'pii_vault_service');
CREATE POLICY vault_service_only ON pii_business
    FOR ALL USING (current_user = 'pii_vault_service');
CREATE POLICY vault_service_only ON pii_biometrics
    FOR ALL USING (current_user = 'pii_vault_service');
CREATE POLICY vault_service_only ON pii_documents
    FOR ALL USING (current_user = 'pii_vault_service');

-- ============================================================================
-- HELPER FUNCTIONS
-- ============================================================================

CREATE OR REPLACE FUNCTION verify_audit_chain(p_global_uid UUID)
RETURNS TABLE(
    audit_id UUID,
    accessed_at TIMESTAMPTZ,
    is_valid BOOLEAN,
    error_message TEXT
) AS $$
DECLARE
    r RECORD;
    v_recomputed_hash VARCHAR;
BEGIN
    FOR r IN
        SELECT * FROM pii_access_audit
        WHERE pii_access_audit.global_uid = p_global_uid
        ORDER BY pii_access_audit.accessed_at
    LOOP
        v_recomputed_hash := compute_audit_log_hash(
            r.audit_id,
            r.global_uid,
            r.accessor_id,
            r.accessed_fields,
            r.accessed_at,
            r.previous_log_hash
        );

        IF v_recomputed_hash <> r.current_log_hash THEN
            RETURN QUERY SELECT
                r.audit_id,
                r.accessed_at,
                FALSE,
                'ALERT: Hash mismatch - audit chain integrity compromised!'::TEXT;
        ELSE
            RETURN QUERY SELECT
                r.audit_id,
                r.accessed_at,
                TRUE,
                NULL::TEXT;
        END IF;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE pii_individual IS 'Encrypted PII storage for individual customers - regional deployment';
COMMENT ON TABLE pii_business IS 'Encrypted PII storage for business customers - regional deployment';
COMMENT ON TABLE pii_biometrics IS 'Encrypted biometric templates (face, fingerprint)';
COMMENT ON TABLE pii_documents IS 'Encrypted customer documents (ID, statements)';
COMMENT ON TABLE pii_access_audit IS 'Immutable audit log with blockchain-style hash chaining';
COMMENT ON TABLE vault_config IS 'Regional vault configuration (KSA, UAE, PAK)';
COMMENT ON COLUMN pii_individual.national_id_encrypted IS 'AES-256-GCM encrypted via HashiCorp Vault Transit';
COMMENT ON COLUMN pii_individual.global_uid IS 'Links to global_profile_db.global_customers.global_uid';
COMMENT ON COLUMN pii_access_audit.current_log_hash IS 'SHA-256 hash chained to previous entry - tamper detection';

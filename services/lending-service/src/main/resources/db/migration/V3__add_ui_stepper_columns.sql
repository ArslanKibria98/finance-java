-- ============================================================================
-- V3: Add columns to loan_applications for UI stepper flow
-- Matches: Basic Info → Bank Account → Eligibility → Accept Offer → Sign Contract
-- ============================================================================

-- Update application_status enum with new stepper-matching values
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'BASIC_INFO_SUBMITTED' AFTER 'DRAFT';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'BANK_ACCOUNT_PENDING' AFTER 'BASIC_INFO_SUBMITTED';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'BANK_ACCOUNT_VERIFIED' AFTER 'BANK_ACCOUNT_PENDING';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'SIMAH_CONSENT_GIVEN' AFTER 'BANK_ACCOUNT_VERIFIED';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'ELIGIBILITY_CHECKING' AFTER 'SIMAH_CONSENT_GIVEN';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'ELIGIBILITY_PASSED' AFTER 'ELIGIBILITY_CHECKING';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'OFFER_PRESENTED' AFTER 'ELIGIBILITY_PASSED';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'OFFER_ACCEPTED' AFTER 'OFFER_PRESENTED';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'CONTRACT_PENDING' AFTER 'OFFER_ACCEPTED';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'CONTRACT_SIGNING' AFTER 'CONTRACT_PENDING';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'OTP_VERIFICATION' AFTER 'CONTRACT_SIGNING';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'IVR_VERIFICATION' AFTER 'OTP_VERIFICATION';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'CONTRACT_SIGNED' AFTER 'IVR_VERIFICATION';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'LOAN_CREATING' AFTER 'CONTRACT_SIGNED';
ALTER TYPE application_status ADD VALUE IF NOT EXISTS 'DISBURSING' AFTER 'LOAN_CREATING';

-- Purpose of finance enum
CREATE TYPE purpose_of_finance AS ENUM (
    'HOUSEHOLD',
    'FAMILY_SUPPORT',
    'EDUCATION',
    'PERSONAL',
    'MEDICAL_TREATMENT',
    'OTHER'
);

-- ============================================================================
-- Customer identity (for cross-service lookups)
-- ============================================================================
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS national_id VARCHAR(20);

-- ============================================================================
-- Pre-qualification data (from "Check Eligibility" home screen)
-- ============================================================================
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS monthly_income NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS total_expenses NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS existing_liabilities NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS adult_dependents INT DEFAULT 0;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS child_dependents INT DEFAULT 0;

-- ============================================================================
-- Step 1: Basic Information enhancements
-- ============================================================================
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS product_name VARCHAR(255);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS purpose_of_finance purpose_of_finance;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS profit_rate NUMERIC(10, 8);

-- ============================================================================
-- Step 2: Bank Account (disbursement target)
-- ============================================================================
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS disbursement_bank_code VARCHAR(20);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS disbursement_bank_name VARCHAR(255);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS disbursement_iban VARCHAR(34);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS disbursement_account_holder VARCHAR(255);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS iban_verified BOOLEAN DEFAULT FALSE;

-- ============================================================================
-- Step 3: SIMAH / Eligibility
-- ============================================================================
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS simah_consent BOOLEAN DEFAULT FALSE;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS simah_consent_at TIMESTAMPTZ;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS credit_score INT;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS simah_reference_id VARCHAR(100);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS verified_salary NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS max_eligible_amount NUMERIC(20, 6);

-- ============================================================================
-- Step 4: Offer
-- ============================================================================
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS offered_amount NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS offered_monthly_installment NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS offered_total_profit NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS offered_total_payable NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS processing_fee NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS admin_fee NUMERIC(20, 6);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS accepted_amount NUMERIC(20, 6);

-- ============================================================================
-- Step 5: Contract signing
-- ============================================================================
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS contract_expires_at TIMESTAMPTZ;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS authorize_digital_signature BOOLEAN DEFAULT FALSE;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS authorize_sell_commodity BOOLEAN DEFAULT FALSE;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS want_physical_delivery BOOLEAN DEFAULT FALSE;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS commodity_trade_id VARCHAR(100);
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS otp_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE loan_applications ADD COLUMN IF NOT EXISTS ivr_verified BOOLEAN DEFAULT FALSE;

-- ============================================================================
-- Indexes for new columns
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_applications_national_id
    ON loan_applications(tenant_id, national_id) WHERE national_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_applications_iban
    ON loan_applications(disbursement_iban) WHERE disbursement_iban IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_applications_simah_ref
    ON loan_applications(simah_reference_id) WHERE simah_reference_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_applications_contract_expiry
    ON loan_applications(contract_expires_at) WHERE contract_expires_at IS NOT NULL;

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON COLUMN loan_applications.monthly_income IS 'Pre-qualification: customer monthly salary in SAR';
COMMENT ON COLUMN loan_applications.simah_consent IS 'Legal: customer consented to SIMAH credit check';
COMMENT ON COLUMN loan_applications.simah_consent_at IS 'Legal: timestamp of SIMAH consent (audit requirement)';
COMMENT ON COLUMN loan_applications.credit_score IS 'SIMAH credit score (fetched during eligibility check)';
COMMENT ON COLUMN loan_applications.disbursement_iban IS 'Verified IBAN for loan disbursement';
COMMENT ON COLUMN loan_applications.contract_expires_at IS '24-hour deadline for contract signing';
COMMENT ON COLUMN loan_applications.commodity_trade_id IS 'Reference to commodity trade (Tawarruq structure)';

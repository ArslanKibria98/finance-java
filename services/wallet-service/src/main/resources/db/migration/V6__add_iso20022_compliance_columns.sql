-- ============================================================================
-- V6: ISO 20022 compliance columns for wallet withdrawals
-- Adds: purpose_code (typed), charge_bearer, service_level, end_to_end_id, uetr
-- Plus: backfill existing rows + check constraints
-- ============================================================================

-- Existing purpose_code is VARCHAR(20). Tighten to ISO 4-char + check constraint.
-- Backfill any non-conforming values to OTHR first.
UPDATE wallet_withdrawals
   SET purpose_code = 'OTHR'
 WHERE purpose_code IS NULL
    OR LENGTH(purpose_code) <> 4
    OR purpose_code !~ '^[A-Z]{4}$';

ALTER TABLE wallet_withdrawals
    ALTER COLUMN purpose_code TYPE VARCHAR(4) USING UPPER(LEFT(purpose_code, 4));

ALTER TABLE wallet_withdrawals
    ADD CONSTRAINT chk_wd_purpose_code CHECK (
        purpose_code IS NULL OR purpose_code IN (
            'SALA','BONU','PENS','RENT','UTIL','ELEC','GASB','WTER','PHON',
            'GOVT','TAXS','GDDS','SUPP','TRAD','SCVE','EDUC','HLTI','INSU',
            'LOAN','LOAR','INTC','FAMI','GIFT','PROP','CASH','INVE',
            'ZAKT','SADQ','CHAR','HAJJ','OTHR'
        )
    );

-- ──────────────────────────────────────────────────────────────────────────
-- Charge bearer (ISO 20022 ChargeBearerType1Code)
-- ──────────────────────────────────────────────────────────────────────────
ALTER TABLE wallet_withdrawals
    ADD COLUMN charge_bearer VARCHAR(4) NOT NULL DEFAULT 'DEBT',
    ADD CONSTRAINT chk_wd_charge_bearer CHECK (
        charge_bearer IN ('DEBT','CRED','SHAR','SLEV')
    );

-- ──────────────────────────────────────────────────────────────────────────
-- Service level (ISO 20022 ServiceLevel)
-- ──────────────────────────────────────────────────────────────────────────
ALTER TABLE wallet_withdrawals
    ADD COLUMN service_level VARCHAR(4) NOT NULL DEFAULT 'NURG',
    ADD CONSTRAINT chk_wd_service_level CHECK (
        service_level IN ('NURG','URGP','SDVA','PRPT')
    );

-- ──────────────────────────────────────────────────────────────────────────
-- End-to-end tracking IDs (ISO 20022 mandatory)
-- ──────────────────────────────────────────────────────────────────────────
ALTER TABLE wallet_withdrawals
    ADD COLUMN end_to_end_id  VARCHAR(35),
    ADD COLUMN uetr           UUID,
    ADD COLUMN instruction_id VARCHAR(35);

CREATE INDEX idx_wd_uetr          ON wallet_withdrawals(uetr) WHERE uetr IS NOT NULL;
CREATE INDEX idx_wd_end_to_end_id ON wallet_withdrawals(tenant_id, end_to_end_id) WHERE end_to_end_id IS NOT NULL;

-- ──────────────────────────────────────────────────────────────────────────
-- Beneficiary country (for STR/CTR corridor analysis)
-- ──────────────────────────────────────────────────────────────────────────
ALTER TABLE wallet_withdrawals
    ADD COLUMN destination_country VARCHAR(2);

-- Backfill destination_country from IBAN prefix where possible
UPDATE wallet_withdrawals
   SET destination_country = LEFT(destination_iban, 2)
 WHERE destination_country IS NULL AND destination_iban IS NOT NULL;

CREATE INDEX idx_wd_destination_country ON wallet_withdrawals(tenant_id, destination_country);

COMMENT ON COLUMN wallet_withdrawals.purpose_code     IS 'ISO 20022 ExternalPurpose1Code — required for amounts >= 5000 SAR';
COMMENT ON COLUMN wallet_withdrawals.charge_bearer    IS 'ISO 20022 ChargeBearerType1Code (DEBT/CRED/SHAR/SLEV)';
COMMENT ON COLUMN wallet_withdrawals.service_level    IS 'ISO 20022 ServiceLevel code (NURG/URGP/SDVA/PRPT)';
COMMENT ON COLUMN wallet_withdrawals.end_to_end_id    IS 'ISO 20022 EndToEndId — mandatory cross-system tracker';
COMMENT ON COLUMN wallet_withdrawals.uetr             IS 'Universal End-to-end Transaction Reference (SWIFT GPI)';
COMMENT ON COLUMN wallet_withdrawals.instruction_id   IS 'ISO 20022 InstructionId — sender bank reference';

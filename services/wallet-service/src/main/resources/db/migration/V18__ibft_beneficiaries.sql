-- ============================================================================
-- IBFT (Inter-Bank Funds Transfer) beneficiaries — external Canadian bank accounts.
-- Separate from wallet_iban_beneficiaries (Saudi/IBAN). Canadian format:
-- institution (FI number) + transit + account number.
-- ============================================================================
CREATE TABLE ibft_beneficiaries (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    customer_id         UUID NOT NULL,
    wallet_id           UUID REFERENCES wallets(id),
    nickname            VARCHAR(100),
    beneficiary_name    VARCHAR(200) NOT NULL,
    institution_number  VARCHAR(4)  NOT NULL,   -- Canadian FI number (e.g. 002 = Scotia)
    transit             VARCHAR(5)  NOT NULL,   -- 5-digit branch transit
    account_number      VARCHAR(20) NOT NULL,   -- 5-12 digit account number
    bank_name           VARCHAR(160),
    currency            VARCHAR(3)  NOT NULL DEFAULT 'CAD',
    is_validated        BOOLEAN NOT NULL DEFAULT FALSE,
    validation_ref      VARCHAR(100),
    validation_result   JSONB,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_ibft_ben_institution CHECK (institution_number ~ '^[0-9]{3,4}$'),
    CONSTRAINT chk_ibft_ben_transit     CHECK (transit ~ '^[0-9]{5}$'),
    CONSTRAINT chk_ibft_ben_account     CHECK (account_number ~ '^[0-9]{5,20}$'),
    CONSTRAINT uq_ibft_ben UNIQUE (tenant_id, customer_id, institution_number, transit, account_number)
);

CREATE INDEX idx_ibft_ben_customer ON ibft_beneficiaries(tenant_id, customer_id);

CREATE TRIGGER trigger_ibft_ben_updated
    BEFORE UPDATE ON ibft_beneficiaries FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE ibft_beneficiaries ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON ibft_beneficiaries
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

COMMENT ON TABLE ibft_beneficiaries IS 'IBFT external Canadian bank-account beneficiaries (institution+transit+account)';

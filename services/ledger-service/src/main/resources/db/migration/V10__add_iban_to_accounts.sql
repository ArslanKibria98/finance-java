-- ============================================================================
-- ADD IBAN (International Bank Account Number) to Chart of Accounts
-- ============================================================================

ALTER TABLE accounts
ADD COLUMN iban VARCHAR(34),
ADD CONSTRAINT chk_iban_format CHECK (iban IS NULL OR iban ~ '^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$');

CREATE INDEX idx_accounts_iban ON accounts(tenant_id, iban) WHERE iban IS NOT NULL;

-- Comment for documentation
COMMENT ON COLUMN accounts.iban IS 'International Bank Account Number - ISO 13616 format (2 letters + 2 digits + up to 30 alphanumeric)';

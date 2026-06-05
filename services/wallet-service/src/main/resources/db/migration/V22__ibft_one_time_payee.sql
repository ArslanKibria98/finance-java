-- ============================================================================
-- Allow one-time IBFT transfers (ad-hoc payee, no saved beneficiary).
-- beneficiary_id becomes nullable; creditor details live on the row (creditor_account/name)
-- plus the raw Canadian parts for audit.
-- ============================================================================
ALTER TABLE ibft_transactions ALTER COLUMN beneficiary_id DROP NOT NULL;

ALTER TABLE ibft_transactions
    ADD COLUMN IF NOT EXISTS creditor_institution VARCHAR(4),
    ADD COLUMN IF NOT EXISTS creditor_transit     VARCHAR(5),
    ADD COLUMN IF NOT EXISTS creditor_account_no  VARCHAR(20),
    ADD COLUMN IF NOT EXISTS one_time             BOOLEAN NOT NULL DEFAULT FALSE;

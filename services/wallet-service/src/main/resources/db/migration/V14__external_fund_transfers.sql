-- ============================================================================
-- EXTERNAL FUND TRANSFER (Scotia RTP) SCHEMA
-- KSA / Canada Islamic Financing Platform
--   1. Per-wallet virtual Canadian-format account_number (FI 002 + transit + seq)
--   2. external_fund_transfers ledger of outbound/inbound bank transfers
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Virtual account number on wallets
--    Format: 002-<transit>-<7-digit sequence>  e.g. 002-80150-0000123
--    Same sequence is used by CreateWalletService for new wallets (nextval).
-- ----------------------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS wallet_account_number_seq START 1;

ALTER TABLE wallets
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(34);

-- Backfill existing wallets deterministically from the shared sequence
UPDATE wallets
   SET account_number = '002-80150-' || lpad(nextval('wallet_account_number_seq')::text, 7, '0')
 WHERE account_number IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_wallet_account_number
    ON wallets(tenant_id, account_number);

-- ----------------------------------------------------------------------------
-- 2. External fund transfer state machine + direction
-- ----------------------------------------------------------------------------
CREATE TYPE external_transfer_status AS ENUM (
    'INITIATED',
    'OPTIONS_OK',
    'SUBMITTED',
    'COMPLETED',
    'REJECTED',
    'FAILED'
);

CREATE TYPE external_transfer_direction AS ENUM (
    'OUTBOUND',
    'INBOUND'
);

-- ----------------------------------------------------------------------------
-- 3. External fund transfers
--    Counterparty is an EXTERNAL Canadian bank account (no destination wallet).
--    OUTBOUND: user wallet -> external account (Scotia RTP commit).
--    INBOUND : external account -> user wallet (credit).
-- ----------------------------------------------------------------------------
CREATE TABLE external_fund_transfers (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL,
    transfer_number         VARCHAR(50) NOT NULL,
    direction               external_transfer_direction NOT NULL,
    wallet_id               UUID NOT NULL REFERENCES wallets(id),
    customer_id             UUID NOT NULL,
    account_number          VARCHAR(34),                 -- the user's virtual account this leg moves through
    counterparty_name       VARCHAR(140),
    counterparty_account    VARCHAR(34),                 -- external bank account / e-Transfer id
    counterparty_email      VARCHAR(140),
    counterparty_bank_code  VARCHAR(10),                 -- Canadian FI number (e.g. 003 RBC)
    amount                  NUMERIC(20,6) NOT NULL,
    fee_amount              NUMERIC(20,6) NOT NULL DEFAULT 0,
    currency                VARCHAR(3) NOT NULL DEFAULT 'CAD',
    status                  external_transfer_status NOT NULL DEFAULT 'INITIATED',
    purpose_note            VARCHAR(280),
    movement_id             UUID REFERENCES wallet_movements(id),
    ledger_entry_id         UUID,
    scotia_payment_id       VARCHAR(100),
    scotia_clearing_ref     VARCHAR(100),
    scotia_status           VARCHAR(60),
    idempotency_key         VARCHAR(100) NOT NULL,
    initiator_user_id       UUID,
    initiator_ip            VARCHAR(45),
    initiator_device_id     VARCHAR(100),
    error_code              VARCHAR(80),
    error_message           TEXT,
    initiated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_ext_transfer_amount_pos CHECK (amount > 0),
    CONSTRAINT uq_ext_transfer_number      UNIQUE (tenant_id, transfer_number),
    CONSTRAINT uq_ext_transfer_idem        UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_ext_transfers_wallet  ON external_fund_transfers(wallet_id, initiated_at DESC);
CREATE INDEX idx_ext_transfers_status  ON external_fund_transfers(tenant_id, status);
CREATE INDEX idx_ext_transfers_account ON external_fund_transfers(tenant_id, account_number);

CREATE TRIGGER trigger_ext_transfers_updated
    BEFORE UPDATE ON external_fund_transfers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Row level security (consistent with wallet_transfers)
ALTER TABLE external_fund_transfers ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON external_fund_transfers
    FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

COMMENT ON TABLE external_fund_transfers IS 'Outbound/inbound external bank transfers via Scotia RTP middleware';
COMMENT ON COLUMN wallets.account_number IS 'Virtual Canadian-format account number (002-transit-seq) used as RTP debtor/creditor reference';

-- ============================================================================
-- When an outbound external transfer's counterparty account number belongs to
-- one of our own wallets, the recipient wallet is credited in the SAME operation
-- (Scotia is only the money rail; our system mirrors both debit + credit).
-- These columns trace the internal recipient leg.
-- ============================================================================
ALTER TABLE external_fund_transfers
    ADD COLUMN IF NOT EXISTS counterparty_wallet_id   UUID,
    ADD COLUMN IF NOT EXISTS counterparty_movement_id UUID,
    ADD COLUMN IF NOT EXISTS counterparty_internal    BOOLEAN NOT NULL DEFAULT FALSE;

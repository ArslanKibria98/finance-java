-- ============================================================================
-- Mirror V10 for the sender side: persist the masked sender name captured at
-- transfer time so the recipient's transaction history returns a consistent
-- name without a live identity lookup (which can return null name fields).
-- ============================================================================

ALTER TABLE wallet_transfers
    ADD COLUMN IF NOT EXISTS sender_masked_name VARCHAR(120);

COMMENT ON COLUMN wallet_transfers.sender_masked_name IS
    'Masked sender display name frozen at transfer time. Used when the recipient views this transfer in their history so the counterparty name is stable and never null.';

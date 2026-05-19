-- ============================================================================
-- Persist the masked recipient name captured at transfer time so that
-- "recent recipients" returns the SAME display name that was shown in the
-- recipient-lookup verify step (matches LookupRecipientService output,
-- including its random fallback when the identity service has no record).
-- ============================================================================

ALTER TABLE wallet_transfers
    ADD COLUMN IF NOT EXISTS recipient_masked_name VARCHAR(120);

COMMENT ON COLUMN wallet_transfers.recipient_masked_name IS
    'Display name shown to the sender at the time of transfer (masked or random fallback). Frozen with the transfer row so the same name is returned consistently in recent-recipients and history.';

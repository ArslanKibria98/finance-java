-- Align journal_entries schema with JPA entity fields used by reports.
-- This migration is additive and safe for existing data.

ALTER TABLE journal_entries
    ADD COLUMN IF NOT EXISTS transaction_type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'SAR',
    ADD COLUMN IF NOT EXISTS is_reversal BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS original_entry_id UUID REFERENCES journal_entries (id),
    ADD COLUMN IF NOT EXISTS requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS approved_by UUID,
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS fineract_synced BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill newly added report columns from existing canonical fields where possible.
UPDATE journal_entries
SET transaction_type = COALESCE(NULLIF(transaction_type, ''), COALESCE(source_service, 'GENERAL')),
    currency = COALESCE(NULLIF(currency, ''), currency_code),
    is_reversal = COALESCE(is_reversal, reversal_of_entry_id IS NOT NULL),
    original_entry_id = COALESCE(original_entry_id, reversal_of_entry_id),
    fineract_synced = COALESCE(fineract_synced, sync_status = 'SYNCED');

-- Keep legacy and report columns consistent for future writes.
UPDATE journal_entries
SET source_service = COALESCE(source_service, transaction_type),
    currency_code = COALESCE(currency_code, currency),
    reversal_of_entry_id = COALESCE(reversal_of_entry_id, original_entry_id),
    sync_status = CASE
        WHEN sync_status IS NULL AND fineract_synced THEN 'SYNCED'
        WHEN sync_status IS NULL THEN 'PENDING'
        ELSE sync_status
    END;

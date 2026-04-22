-- Adds missing reversal_reason field expected by JournalEntryJpaEntity/report queries.
ALTER TABLE journal_entries
    ADD COLUMN IF NOT EXISTS reversal_reason TEXT;

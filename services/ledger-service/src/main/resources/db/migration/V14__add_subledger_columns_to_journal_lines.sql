-- Adds missing sub-ledger columns expected by JournalLineJpaEntity/report queries.
ALTER TABLE journal_lines
    ADD COLUMN IF NOT EXISTS sub_ledger_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS sub_ledger_id UUID;

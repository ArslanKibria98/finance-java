-- JournalEntryJpaEntity binds `status` as plain String (not an @Enumerated enum),
-- so the column needs to be VARCHAR instead of the entry_status enum.
-- Same for sync_status to keep inserts consistent when the entity grows.

ALTER TABLE journal_entries
    ALTER COLUMN status      DROP DEFAULT,
    ALTER COLUMN status      TYPE VARCHAR(20) USING status::text,
    ALTER COLUMN status      SET DEFAULT 'PENDING';

ALTER TABLE journal_entries
    ALTER COLUMN sync_status DROP DEFAULT,
    ALTER COLUMN sync_status TYPE VARCHAR(20) USING sync_status::text,
    ALTER COLUMN sync_status SET DEFAULT 'PENDING';

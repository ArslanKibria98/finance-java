-- Align accounts table columns with AccountJpaEntity mapping.
-- This prevents SQL grammar errors in report queries when older schemas
-- are missing fields expected by the JPA entity.

ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS hierarchy_level INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS hierarchy_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS is_header BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_manual_entries_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS fineract_mapping_id UUID;

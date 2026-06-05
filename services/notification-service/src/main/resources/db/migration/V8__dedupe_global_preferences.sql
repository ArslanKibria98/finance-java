-- V8: Fix duplicate GLOBAL notification_preferences rows.
--
-- Root cause: two Kafka consumer threads (customer-profile sync + event orchestration)
-- both call getOrCreateDefault concurrently, both find no row, both insert a GLOBAL row.
-- The existing unique constraint (tenant_id, customer_id, preference_level, category, event_type)
-- does NOT catch this because category/event_type are NULL for GLOBAL rows, and Postgres
-- treats NULLs as distinct in unique constraints. The resulting duplicates make
-- findByTenantIdAndCustomerId throw IncorrectResultSizeDataAccessException, which aborts
-- the whole event → notifications silently dropped for affected customers.

-- 1. Dedupe: keep ONE GLOBAL row per (tenant_id, customer_id) — prefer the row that already
--    carries a currency_code (display currency), otherwise the oldest.
DELETE FROM notification_preferences p
USING (
    SELECT id,
           row_number() OVER (
               PARTITION BY tenant_id, customer_id
               ORDER BY (currency_code IS NOT NULL) DESC, created_at ASC
           ) AS rn
    FROM notification_preferences
    WHERE preference_level = 'GLOBAL'
) d
WHERE p.id = d.id
  AND d.rn > 1;

-- 2. Prevent recurrence: a partial unique index that ignores the NULL category/event_type
--    columns and enforces at most one GLOBAL preference per customer.
CREATE UNIQUE INDEX IF NOT EXISTS uq_prefs_global_customer
    ON notification_preferences (tenant_id, customer_id)
    WHERE preference_level = 'GLOBAL';

-- V12: Add per-API cost tracking with snapshot-at-call-time semantics.
--      - provider_apis.cost_per_call holds the current admin-managed price.
--      - client_request_{test,dev,prod}.api_cost stores the price ACTIVE AT THE TIME
--        the call was made (so historical bills remain immutable when admin
--        edits prices later).
--      - customer_id / application_id / context_type capture the business context
--        so cost can be aggregated per customer, per application, or per
--        onboarding session.

-- ───────────────────────────────────────────────────────────────────
-- 1. Cost catalog columns on provider_apis
-- ───────────────────────────────────────────────────────────────────
ALTER TABLE provider_apis
    ADD COLUMN IF NOT EXISTS cost_per_call DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
    ADD COLUMN IF NOT EXISTS cost_currency VARCHAR(3)    NOT NULL DEFAULT 'SAR';

COMMENT ON COLUMN provider_apis.cost_per_call IS 'Admin-managed price billed per successful call';
COMMENT ON COLUMN provider_apis.cost_currency IS 'ISO-4217 currency code (e.g., SAR)';

-- ───────────────────────────────────────────────────────────────────
-- 2. Snapshot + business-context columns on every client_request_* table
-- ───────────────────────────────────────────────────────────────────
DO $$
DECLARE
    v_table TEXT;
BEGIN
    FOREACH v_table IN ARRAY ARRAY['client_request_test', 'client_request_dev', 'client_request_prod']
    LOOP
        EXECUTE format('ALTER TABLE %I
            ADD COLUMN IF NOT EXISTS api_cost        DECIMAL(12,4),
            ADD COLUMN IF NOT EXISTS cost_currency   VARCHAR(3),
            ADD COLUMN IF NOT EXISTS customer_id     UUID,
            ADD COLUMN IF NOT EXISTS application_id  VARCHAR(100),
            ADD COLUMN IF NOT EXISTS context_type    VARCHAR(20)', v_table);

        -- Cost-report indexes
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_customer
                        ON %I (tenant_id, customer_id, created_at)
                        WHERE customer_id IS NOT NULL',
                        v_table, v_table);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_application
                        ON %I (tenant_id, application_id, created_at)
                        WHERE application_id IS NOT NULL',
                        v_table, v_table);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_context
                        ON %I (tenant_id, context_type, created_at)
                        WHERE context_type IS NOT NULL',
                        v_table, v_table);
    END LOOP;
END $$;

-- ───────────────────────────────────────────────────────────────────
-- 3. Sensible default prices (SAR) for the third-party catalog.
--    Admin can change any of these later via PATCH /admin/provider-apis/{code}/cost.
-- ───────────────────────────────────────────────────────────────────
UPDATE provider_apis SET cost_per_call = 5.0000 WHERE code LIKE 'SIMAH_%';
UPDATE provider_apis SET cost_per_call = 1.5000 WHERE code LIKE 'SAFEWATCH_%';
UPDATE provider_apis SET cost_per_call = 2.0000 WHERE code LIKE 'MASDAR_%';
UPDATE provider_apis SET cost_per_call = 0.5000 WHERE code LIKE 'NAFATH_%';
UPDATE provider_apis SET cost_per_call = 0.5000 WHERE code LIKE 'NABA_%';
UPDATE provider_apis SET cost_per_call = 0.7500 WHERE code LIKE 'TARABUT_%';
UPDATE provider_apis SET cost_per_call = 0.2500 WHERE code LIKE 'UNIFONIC_%' AND code NOT LIKE '%IVR%';
UPDATE provider_apis SET cost_per_call = 1.0000 WHERE code LIKE 'UNIFONIC_%IVR%';
UPDATE provider_apis SET cost_per_call = 3.0000 WHERE code LIKE 'EIGER_%';
UPDATE provider_apis SET cost_per_call = 4.0000 WHERE code LIKE 'NAFITH_%';
UPDATE provider_apis SET cost_per_call = 2.0000 WHERE code LIKE 'EMDHA_%';
UPDATE provider_apis SET cost_per_call = 1.0000 WHERE code LIKE 'PAYMENT_GUARD_%';
UPDATE provider_apis SET cost_per_call = 1.5000 WHERE code LIKE 'TAHAQUQ_%';
UPDATE provider_apis SET cost_per_call = 0.8000 WHERE code LIKE 'ABSHER_%';
UPDATE provider_apis SET cost_per_call = 0.5000 WHERE code LIKE 'ANB_%';
UPDATE provider_apis SET cost_per_call = 0.5000 WHERE code LIKE 'HYPERPAY_%';
UPDATE provider_apis SET cost_per_call = 1.0000 WHERE code LIKE 'LEAN_%';
UPDATE provider_apis SET cost_per_call = 0.5000 WHERE code LIKE 'WATHQ_%';
UPDATE provider_apis SET cost_per_call = 0.5000 WHERE code LIKE 'DAKHLI_%';

-- All providers default to SAR; nothing to update for currency.

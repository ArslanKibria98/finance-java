-- ============================================================================
-- V20: Add general-credit-score summary fields + occupation to customer profile
-- ============================================================================
-- Two changes:
--   1. customers: cached summary of the general (onboarding) credit score
--      (the detailed snapshot history lives in risk-service)
--   2. customer_pep_answers: capture occupation during EDD form submission
--      (the LOV is stored separately in occupation_options)

-- ----------------------------------------------------------------------------
-- 1. Customers: cached general credit score summary
-- ----------------------------------------------------------------------------
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS general_credit_score             NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS general_credit_score_percentage  NUMERIC(10, 4),
    ADD COLUMN IF NOT EXISTS general_credit_decision          VARCHAR(50),
    ADD COLUMN IF NOT EXISTS general_credit_reason_code       VARCHAR(50),
    ADD COLUMN IF NOT EXISTS general_credit_snapshot_stage    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS general_credit_scored_at         TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_customers_general_decision
    ON customers(tenant_id, general_credit_decision);

-- ----------------------------------------------------------------------------
-- 2. EDD answers: capture occupation (LOV code from occupation_options)
-- ----------------------------------------------------------------------------
ALTER TABLE customer_pep_answers
    ADD COLUMN IF NOT EXISTS occupation        VARCHAR(50),
    ADD COLUMN IF NOT EXISTS occupation_details TEXT;

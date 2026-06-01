-- ============================================================================
-- V23: General Credit Scoring (product-agnostic) for onboarding flow
-- ============================================================================
-- Two separate scoring contexts:
--   - PRODUCT scoring (existing): product_credit_scoring_criteria/rules
--     used during loan application against a specific product
--   - GENERAL scoring (NEW): general_credit_scoring_criteria/rules
--     used during customer onboarding; product-agnostic; one tenant-level set
--
-- Plus snapshot history table to track incremental scoring as third-party
-- data flows in (Nafath -> Yakeen -> Salary -> EDD -> AML).

-- ============================================================================
-- 1. GENERAL SCORING CRITERIA (tenant-level, NOT product-linked)
-- ============================================================================

CREATE TABLE general_credit_scoring_criteria (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    field_definition_id UUID REFERENCES credit_scoring_field_definitions(id),
    custom_name         VARCHAR(255),
    is_custom           BOOLEAN NOT NULL DEFAULT false,
    is_enabled          BOOLEAN NOT NULL DEFAULT true,
    sort_order          INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1,
    -- one entry per (tenant, field) for non-custom criteria
    CONSTRAINT uq_gcsc_tenant_field UNIQUE (tenant_id, field_definition_id)
);

CREATE INDEX idx_gcsc_tenant         ON general_credit_scoring_criteria(tenant_id);
CREATE INDEX idx_gcsc_tenant_enabled ON general_credit_scoring_criteria(tenant_id, is_enabled);

-- ============================================================================
-- 2. GENERAL SCORING RULES
-- ============================================================================

CREATE TABLE general_credit_scoring_rules (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID NOT NULL,
    criteria_id  UUID NOT NULL REFERENCES general_credit_scoring_criteria(id) ON DELETE CASCADE,
    operator     VARCHAR(20) NOT NULL,           -- EQ, GT, GTE, LT, LTE, BETWEEN, IN, NOT_IN
    value        VARCHAR(500) NOT NULL,
    weight       NUMERIC(10, 4) NOT NULL DEFAULT 0,
    percentage   NUMERIC(10, 4) NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gcsr_criteria ON general_credit_scoring_rules(criteria_id);
CREATE INDEX idx_gcsr_tenant   ON general_credit_scoring_rules(tenant_id);

-- ============================================================================
-- 3. SCORING THRESHOLDS (tenant-level config: GREEN / AMBER / RED bands)
-- ============================================================================

CREATE TABLE general_credit_scoring_config (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                UUID NOT NULL UNIQUE,
    min_pass_percentage      NUMERIC(5, 2) NOT NULL DEFAULT 50.00,
    green_threshold          NUMERIC(5, 2) NOT NULL DEFAULT 75.00,
    amber_threshold          NUMERIC(5, 2) NOT NULL DEFAULT 50.00,
    is_enabled               BOOLEAN NOT NULL DEFAULT true,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                  INT NOT NULL DEFAULT 1
);

-- Default config row for the default tenant
INSERT INTO general_credit_scoring_config (tenant_id, min_pass_percentage, green_threshold, amber_threshold)
VALUES ('00000000-0000-0000-0000-000000000001', 50.00, 75.00, 50.00)
ON CONFLICT (tenant_id) DO NOTHING;

-- ============================================================================
-- 4. CUSTOMER SCORING HISTORY (snapshots taken at each third-party step)
-- ============================================================================

CREATE TABLE customer_credit_score_snapshots (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    customer_id         UUID NOT NULL,
    workflow_id         VARCHAR(255),
    -- which onboarding step produced this snapshot:
    -- NAFATH_VERIFIED, YAKEEN_VERIFIED, EDD_SUBMITTED, SALARY_FETCHED, AML_SCORED, ONBOARDING_COMPLETE
    snapshot_stage      VARCHAR(50) NOT NULL,
    score_percentage    NUMERIC(10, 4) NOT NULL,
    total_score         NUMERIC(10, 4) NOT NULL,
    max_possible_score  NUMERIC(10, 4) NOT NULL,
    decision            VARCHAR(50) NOT NULL,        -- AUTO_APPROVE / REFER_MANUAL_REVIEW / AUTO_REJECT / INSUFFICIENT_DATA
    reason_code         VARCHAR(50),                  -- GREEN_AUTO_APPROVE / AMBER_MANUAL_REVIEW / RED_AUTO_REJECT
    matched_criteria    INT NOT NULL DEFAULT 0,
    total_criteria      INT NOT NULL DEFAULT 0,
    inputs              JSONB,                        -- the answers map at scoring time
    breakdown           JSONB,                        -- per-criterion details
    summary             TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ccss_customer  ON customer_credit_score_snapshots(customer_id, created_at DESC);
CREATE INDEX idx_ccss_workflow  ON customer_credit_score_snapshots(workflow_id);
CREATE INDEX idx_ccss_tenant    ON customer_credit_score_snapshots(tenant_id, created_at DESC);

-- ============================================================================
-- 5. CUSTOMER CURRENT SCORE (latest snapshot summary per customer)
-- ============================================================================

CREATE TABLE customer_credit_score_current (
    customer_id         UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    score_percentage    NUMERIC(10, 4) NOT NULL,
    total_score         NUMERIC(10, 4) NOT NULL,
    max_possible_score  NUMERIC(10, 4) NOT NULL,
    decision            VARCHAR(50) NOT NULL,
    reason_code         VARCHAR(50),
    snapshot_stage      VARCHAR(50) NOT NULL,         -- last stage that updated it
    last_snapshot_id    UUID REFERENCES customer_credit_score_snapshots(id),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cccsc_tenant ON customer_credit_score_current(tenant_id);
CREATE INDEX idx_cccsc_decision ON customer_credit_score_current(tenant_id, decision);

-- ============================================================================
-- 6. updated_at triggers
-- ============================================================================

CREATE OR REPLACE FUNCTION trg_set_updated_at() RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_gcsc_updated_at BEFORE UPDATE ON general_credit_scoring_criteria
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_gcsr_updated_at BEFORE UPDATE ON general_credit_scoring_rules
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

CREATE TRIGGER trg_gcscfg_updated_at BEFORE UPDATE ON general_credit_scoring_config
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at();

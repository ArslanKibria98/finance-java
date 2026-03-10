-- V4__aml_risk_scoring_schema.sql
-- AML/CFT Risk Scoring Engine — Reference Data & Scoring Tables
-- Based on EastNets AML Risk Schema (Individuals Schema)
-- Supports: Dominant factor override, Mutual-Exclusive weighted scoring

-- =====================================================================
-- ENUM TYPES
-- =====================================================================

CREATE TYPE aml_category_type AS ENUM ('DOMINANT', 'MUTUAL_EXCLUSIVE');
CREATE TYPE aml_risk_level AS ENUM ('HIGH', 'MEDIUM', 'LOW');
CREATE TYPE fatf_category AS ENUM ('HIGH_RISK', 'INCREASED_MONITORING', 'STANDARD');
CREATE TYPE occupation_risk_level AS ENUM ('PEP', 'HIGH', 'MEDIUM', 'LOW');

-- =====================================================================
-- 1. AML Risk Categories — Configurable scoring categories with weights
-- =====================================================================
CREATE TABLE aml_risk_categories (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    category_code   VARCHAR(50) NOT NULL,
    name_en         VARCHAR(255) NOT NULL,
    name_ar         VARCHAR(255),
    category_type   aml_category_type NOT NULL,
    weight          NUMERIC(5, 2) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    UNIQUE (tenant_id, category_code)
);

CREATE INDEX idx_aml_risk_cat_tenant ON aml_risk_categories(tenant_id);

-- =====================================================================
-- 2. AML Risk Category Factors — Factor options per category
-- =====================================================================
CREATE TABLE aml_risk_category_factors (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    category_id         UUID NOT NULL REFERENCES aml_risk_categories(id) ON DELETE CASCADE,
    factor_code         VARCHAR(100) NOT NULL,
    name_en             VARCHAR(255) NOT NULL,
    name_ar             VARCHAR(255),
    factor_weight_pct   NUMERIC(6, 2) NOT NULL,
    computed_rating     NUMERIC(6, 2) NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, category_id, factor_code)
);

CREATE INDEX idx_aml_factor_category ON aml_risk_category_factors(category_id);
CREATE INDEX idx_aml_factor_tenant ON aml_risk_category_factors(tenant_id);

-- =====================================================================
-- 3. AML Risk Thresholds — Configurable HIGH/MEDIUM/LOW cutoffs
-- =====================================================================
CREATE TABLE aml_risk_thresholds (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    risk_level  aml_risk_level NOT NULL,
    min_score   NUMERIC(6, 2) NOT NULL,
    max_score   NUMERIC(6, 2) NOT NULL,
    description VARCHAR(255),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, risk_level)
);

CREATE INDEX idx_aml_threshold_tenant ON aml_risk_thresholds(tenant_id);

-- =====================================================================
-- 4. FATF Country Risk — High-Risk & Increased Monitoring jurisdictions
-- =====================================================================
CREATE TABLE aml_fatf_countries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    country_code    VARCHAR(3) NOT NULL,
    country_name    VARCHAR(255) NOT NULL,
    fatf_category   fatf_category NOT NULL DEFAULT 'STANDARD',
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from  DATE,
    effective_to    DATE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, country_code)
);

CREATE INDEX idx_fatf_country_tenant ON aml_fatf_countries(tenant_id);
CREATE INDEX idx_fatf_country_category ON aml_fatf_countries(tenant_id, fatf_category);

-- =====================================================================
-- 5. City Risk Scores — Saudi cities with risk classification
-- =====================================================================
CREATE TABLE aml_city_risk_scores (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID NOT NULL,
    city_id     VARCHAR(20) NOT NULL,
    region_id   VARCHAR(20) NOT NULL,
    name_ar     VARCHAR(255) NOT NULL,
    name_en     VARCHAR(255),
    risk_level  aml_risk_level NOT NULL DEFAULT 'LOW',
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, city_id)
);

CREATE INDEX idx_city_risk_tenant ON aml_city_risk_scores(tenant_id);
CREATE INDEX idx_city_risk_region ON aml_city_risk_scores(tenant_id, region_id);
CREATE INDEX idx_city_risk_level ON aml_city_risk_scores(tenant_id, risk_level);

-- =====================================================================
-- 6. Occupation Risk Levels — Occupations with risk classification
-- =====================================================================
CREATE TABLE aml_occupation_risk_levels (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    occupation_code     VARCHAR(20) NOT NULL,
    description_ar      VARCHAR(500) NOT NULL,
    description_en      VARCHAR(500),
    risk_level          occupation_risk_level NOT NULL DEFAULT 'LOW',
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, occupation_code)
);

CREATE INDEX idx_occupation_risk_tenant ON aml_occupation_risk_levels(tenant_id);
CREATE INDEX idx_occupation_risk_level ON aml_occupation_risk_levels(tenant_id, risk_level);

-- =====================================================================
-- 7. Income Range Scores — Monthly income ranges with factor weights
-- =====================================================================
CREATE TABLE aml_income_range_scores (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    range_min           NUMERIC(12, 2) NOT NULL,
    range_max           NUMERIC(12, 2),
    label_en            VARCHAR(100) NOT NULL,
    label_ar            VARCHAR(100),
    factor_weight_pct   NUMERIC(6, 2) NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, range_min)
);

CREATE INDEX idx_income_range_tenant ON aml_income_range_scores(tenant_id);

-- =====================================================================
-- 8. Source of Income Scores — Income source options with factor weights
-- =====================================================================
CREATE TABLE aml_source_of_income_scores (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    source_code         VARCHAR(50) NOT NULL,
    name_en             VARCHAR(255) NOT NULL,
    name_ar             VARCHAR(255),
    factor_weight_pct   NUMERIC(6, 2) NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, source_code)
);

CREATE INDEX idx_source_income_tenant ON aml_source_of_income_scores(tenant_id);

-- =====================================================================
-- 9. AML Risk Assessments — Per-customer AML scoring results
-- =====================================================================
CREATE TABLE aml_risk_assessments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID NOT NULL,
    customer_id         VARCHAR(100),
    national_id_hash    VARCHAR(64) NOT NULL,
    total_score         NUMERIC(6, 2) NOT NULL,
    risk_level          aml_risk_level NOT NULL,
    dominant_override   BOOLEAN NOT NULL DEFAULT FALSE,
    dominant_category   VARCHAR(50),
    score_breakdown     JSONB NOT NULL DEFAULT '{}',
    input_data          JSONB NOT NULL DEFAULT '{}',
    assessed_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    assessed_by         VARCHAR(100) DEFAULT 'SYSTEM',
    idempotency_key     VARCHAR(100),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version             INT NOT NULL DEFAULT 1,
    UNIQUE (tenant_id, idempotency_key)
);

CREATE INDEX idx_aml_assessment_tenant ON aml_risk_assessments(tenant_id);
CREATE INDEX idx_aml_assessment_nid ON aml_risk_assessments(tenant_id, national_id_hash);
CREATE INDEX idx_aml_assessment_level ON aml_risk_assessments(tenant_id, risk_level);
CREATE INDEX idx_aml_assessment_date ON aml_risk_assessments(tenant_id, assessed_at DESC);

-- ============================================================================
-- COUNTRIES REFERENCE TABLE
-- Product Service — country reference data for product availability
-- ============================================================================

CREATE TABLE countries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    code            VARCHAR(3) NOT NULL,
    name_en         VARCHAR(100) NOT NULL,
    name_ar         VARCHAR(100),
    dial_code       VARCHAR(10),
    currency_code   VARCHAR(3),
    is_gcc          BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    CONSTRAINT uq_country_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_countries_tenant ON countries(tenant_id);
CREATE INDEX idx_countries_tenant_active ON countries(tenant_id, is_active);
CREATE INDEX idx_countries_gcc ON countries(tenant_id, is_gcc) WHERE is_active = TRUE;

-- V10: Tenant Configuration and LOV (List of Values) Management
-- HLD: FV-HLD-RISK-002 v1.0 — Modules 1 & 2

-- ===== TENANT CONFIGURATION =====
CREATE TABLE tenant_configs (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID NOT NULL UNIQUE,
    tenant_name             VARCHAR(200) NOT NULL,
    tenant_name_ar          VARCHAR(200),
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    customer_risk_enabled   BOOLEAN NOT NULL DEFAULT true,
    business_risk_enabled   BOOLEAN NOT NULL DEFAULT false,
    loan_risk_enabled       BOOLEAN NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version                 INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_tenant_configs_tenant ON tenant_configs(tenant_id);
CREATE INDEX idx_tenant_configs_status ON tenant_configs(tenant_id, status);

-- ===== LOV SETS =====
CREATE TABLE lov_sets (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    code              VARCHAR(100) NOT NULL,
    name_en           VARCHAR(200) NOT NULL,
    name_ar           VARCHAR(200),
    category_type     VARCHAR(30) NOT NULL DEFAULT 'MUTUAL_EXCLUSIVE',
    current_version   INT NOT NULL DEFAULT 1,
    active            BOOLEAN NOT NULL DEFAULT true,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version           INT NOT NULL DEFAULT 1,
    UNIQUE(tenant_id, code)
);

CREATE INDEX idx_lov_sets_tenant ON lov_sets(tenant_id);
CREATE INDEX idx_lov_sets_active ON lov_sets(tenant_id, active);

-- ===== LOV ENTRIES =====
CREATE TABLE lov_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lov_set_id      UUID NOT NULL REFERENCES lov_sets(id),
    tenant_id       UUID NOT NULL,
    factor_code     VARCHAR(100) NOT NULL,
    label_en        VARCHAR(300) NOT NULL,
    label_ar        VARCHAR(300),
    factor_weight   NUMERIC(10,4) NOT NULL DEFAULT 0,
    risk_status     VARCHAR(20),
    lov_version     INT NOT NULL DEFAULT 1,
    active          BOOLEAN NOT NULL DEFAULT true,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version         INT NOT NULL DEFAULT 1,
    UNIQUE(tenant_id, lov_set_id, factor_code)
);

CREATE INDEX idx_lov_entries_set ON lov_entries(tenant_id, lov_set_id);
CREATE INDEX idx_lov_entries_active ON lov_entries(tenant_id, lov_set_id, active);

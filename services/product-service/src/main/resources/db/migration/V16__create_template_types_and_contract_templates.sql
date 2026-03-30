-- V10__create_template_types_and_contract_templates.sql
-- Template Types (contract_type, notification_type, etc.) and Contract Templates

-- ==========================================
-- 1. Template Types
-- ==========================================
CREATE TABLE template_types (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    category        VARCHAR(50) NOT NULL,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    version         INT NOT NULL DEFAULT 1
);

CREATE INDEX idx_template_types_tenant ON template_types(tenant_id);
CREATE INDEX idx_template_types_category ON template_types(tenant_id, category);
CREATE UNIQUE INDEX idx_template_types_unique_name ON template_types(tenant_id, name, category);

-- ==========================================
-- 2. Contract Templates
-- ==========================================
CREATE TABLE contract_templates (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    name            VARCHAR(200) NOT NULL,
    product_id      UUID NOT NULL,
    type_id         UUID NOT NULL,
    language        VARCHAR(10) NOT NULL DEFAULT 'en',
    message         TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by      UUID,
    version         INT NOT NULL DEFAULT 1,

    CONSTRAINT fk_contract_templates_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_contract_templates_type FOREIGN KEY (type_id) REFERENCES template_types(id)
);

CREATE INDEX idx_contract_templates_tenant ON contract_templates(tenant_id);
CREATE INDEX idx_contract_templates_product ON contract_templates(tenant_id, product_id);
CREATE INDEX idx_contract_templates_type ON contract_templates(tenant_id, type_id);

-- ==========================================
-- 3. Seed default template types
-- ==========================================
INSERT INTO template_types (id, tenant_id, name, category, is_active) VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Contract', 'contract_type', true),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Agreement', 'contract_type', true),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Disclosure', 'contract_type', true),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Terms & Conditions', 'contract_type', true),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Approval Letter', 'notification_type', true),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Rejection Letter', 'notification_type', true),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'SMS Notification', 'notification_type', true),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Email Notification', 'notification_type', true);

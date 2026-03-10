-- V5__create_modules_table.sql
-- Create modules table and link permissions to modules

CREATE TABLE modules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    module_code     VARCHAR(50) NOT NULL,
    module_name     VARCHAR(255) NOT NULL,
    description     TEXT,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    display_order   INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, module_code)
);

CREATE INDEX idx_modules_tenant ON modules(tenant_id);

-- Seed modules from existing resource_type values
INSERT INTO modules (tenant_id, module_code, module_name, description, display_order) VALUES
('00000000-0000-0000-0000-000000000001', 'ADMIN', 'Admin', 'Admin dashboard and system operations', 1),
('00000000-0000-0000-0000-000000000001', 'CUSTOMER', 'Customer', 'Customer profile management', 2),
('00000000-0000-0000-0000-000000000001', 'PARTNER', 'Partner', 'Partner management', 3),
('00000000-0000-0000-0000-000000000001', 'PERMISSION', 'Permission', 'Permission catalog management', 4),
('00000000-0000-0000-0000-000000000001', 'POLICY', 'Policy', 'Casbin policy rules management', 5),
('00000000-0000-0000-0000-000000000001', 'PRODUCT', 'Product', 'Product catalog management', 6),
('00000000-0000-0000-0000-000000000001', 'PROFILE', 'Profile', 'Customer/user profile management', 7),
('00000000-0000-0000-0000-000000000001', 'RISK', 'Risk', 'Risk assessment and screening', 8),
('00000000-0000-0000-0000-000000000001', 'ROLE', 'Role', 'Role definitions management', 9),
('00000000-0000-0000-0000-000000000001', 'WALLET', 'Wallet', 'Wallet and transactions management', 10),
('00000000-0000-0000-0000-000000000001', 'WORKFLOW', 'Workflow', 'Workflow orchestration management', 11),
('00000000-0000-0000-0000-000000000001', 'TEST', 'Test', 'Test permissions', 12);

-- Add module_id column to permissions
ALTER TABLE permissions ADD COLUMN module_id UUID;

-- Populate module_id from existing resource_type
UPDATE permissions p
SET module_id = m.id
FROM modules m
WHERE m.module_code = p.resource_type
  AND m.tenant_id = p.tenant_id;

-- Add foreign key constraint
ALTER TABLE permissions ADD CONSTRAINT fk_permissions_module
    FOREIGN KEY (module_id) REFERENCES modules(id);

CREATE INDEX idx_permissions_module ON permissions(module_id);
